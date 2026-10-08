-- Shared household settings are uploaded only when a member saves them in the
-- Together screen. Personal transactions and local preferences stay private.
create table if not exists public.shared_finance_items (
    id uuid primary key default gen_random_uuid(),
    ledger_id uuid not null references public.shared_ledgers(id) on delete cascade,
    owner_id uuid not null references auth.users(id) on delete cascade,
    kind text not null check (kind in ('MONTHLY_INCOME', 'FIXED_EXPENSE', 'LIVING_BUDGET', 'ALLOWANCE', 'ANNIVERSARY')),
    title text not null check (length(trim(title)) between 1 and 80),
    amount bigint,
    due_day integer,
    month_key text,
    date_key text,
    memo text not null default '' check (length(memo) <= 300),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint shared_finance_item_shape check (
        (kind = 'ANNIVERSARY' and amount is null and due_day is null and month_key is null
            and date_key is not null
            and date_key ~ '^(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])$'
            and to_char(to_date('2000-' || date_key, 'YYYY-MM-DD'), 'MM-DD') = date_key)
        or (kind = 'LIVING_BUDGET' and amount is not null and amount between 1 and 1000000000000
            and due_day is null and month_key is not null and month_key ~ '^[0-9]{4}-(0[1-9]|1[0-2])$' and date_key is null)
        or (kind in ('MONTHLY_INCOME', 'FIXED_EXPENSE', 'ALLOWANCE')
            and amount is not null and amount between 1 and 1000000000000
            and due_day is not null and due_day between 1 and 31
            and month_key is null and date_key is null)
    )
);

create index if not exists shared_finance_items_ledger_kind_idx
    on public.shared_finance_items (ledger_id, kind, title);

alter table public.shared_finance_items enable row level security;
revoke all on public.shared_finance_items from anon, public;
grant select, insert, update, delete on public.shared_finance_items to authenticated;

drop policy if exists "members can read shared finance items" on public.shared_finance_items;
drop policy if exists "members can add shared finance items" on public.shared_finance_items;
drop policy if exists "members can edit shared finance items" on public.shared_finance_items;
drop policy if exists "members can remove shared finance items" on public.shared_finance_items;

create policy "members can read shared finance items" on public.shared_finance_items
    for select to authenticated using (public.is_shared_ledger_member(ledger_id));
create policy "members can add shared finance items" on public.shared_finance_items
    for insert to authenticated with check (
        owner_id = (select auth.uid()) and public.is_shared_ledger_member(ledger_id));
create policy "members can edit shared finance items" on public.shared_finance_items
    for update to authenticated using (public.is_shared_ledger_member(ledger_id))
    with check (public.is_shared_ledger_member(ledger_id));
create policy "members can remove shared finance items" on public.shared_finance_items
    for delete to authenticated using (public.is_shared_ledger_member(ledger_id));

-- The creator and ledger are immutable on update; both members may manage the
-- shared value itself.
revoke update on public.shared_finance_items from authenticated;
grant update (kind, title, amount, due_day, month_key, date_key, memo, updated_at)
    on public.shared_finance_items to authenticated;
