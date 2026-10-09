create table if not exists public.shared_ledger_settings (
    ledger_id uuid primary key references public.shared_ledgers(id) on delete cascade,
    money_mode text not null default 'EQUAL_SPLIT'
        check (money_mode in ('EQUAL_SPLIT', 'SHARED_FUND', 'SEPARATE', 'INCOME_OVERVIEW')),
    updated_by uuid references auth.users(id) on delete set null,
    updated_at timestamptz not null default now()
);

alter table public.shared_ledger_settings enable row level security;
revoke all on public.shared_ledger_settings from anon, public;
grant select, insert, update on public.shared_ledger_settings to authenticated;

drop policy if exists "members can read shared ledger settings" on public.shared_ledger_settings;
drop policy if exists "members can add shared ledger settings" on public.shared_ledger_settings;
drop policy if exists "members can update shared ledger settings" on public.shared_ledger_settings;

create policy "members can read shared ledger settings" on public.shared_ledger_settings
    for select to authenticated using (public.is_shared_ledger_member(ledger_id));
create policy "members can add shared ledger settings" on public.shared_ledger_settings
    for insert to authenticated with check (
        updated_by = (select auth.uid()) and public.is_shared_ledger_member(ledger_id));
create policy "members can update shared ledger settings" on public.shared_ledger_settings
    for update to authenticated using (public.is_shared_ledger_member(ledger_id))
    with check (updated_by = (select auth.uid()) and public.is_shared_ledger_member(ledger_id));

create or replace function public.leave_shared_ledger()
returns uuid language plpgsql security definer set search_path = ''
as $$
declare
    caller uuid := auth.uid();
    target_ledger uuid;
begin
    if caller is null then raise exception 'authentication required' using errcode = '28000'; end if;
    select m.ledger_id into target_ledger
    from public.shared_ledger_members m where m.user_id = caller for update;
    if target_ledger is null then return null; end if;

    perform 1 from public.shared_ledgers l where l.id = target_ledger for update;
    update public.shared_ledger_invites
        set expires_at = now()
        where ledger_id = target_ledger and accepted_at is null and expires_at > now();
    delete from public.shared_ledger_members
        where ledger_id = target_ledger and user_id = caller;

    -- Keep the ledger, goals, finance items, and shared transactions for the
    -- remaining member. Leaving only removes this account's membership.
    return target_ledger;
end; $$;

revoke all on function public.leave_shared_ledger() from public, anon;
grant execute on function public.leave_shared_ledger() to authenticated;
