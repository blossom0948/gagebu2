-- Data enters this ledger only after explicit selection in the app.
create extension if not exists pgcrypto with schema extensions;

create table if not exists public.shared_ledgers (
    id uuid primary key default gen_random_uuid(),
    created_at timestamptz not null default now()
);
create table if not exists public.shared_ledger_members (
    ledger_id uuid not null references public.shared_ledgers(id) on delete cascade,
    user_id uuid not null references auth.users(id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (ledger_id, user_id), unique (user_id)
);
create table if not exists public.shared_ledger_invites (
    id uuid primary key default gen_random_uuid(),
    ledger_id uuid not null references public.shared_ledgers(id) on delete cascade,
    code_hash bytea not null unique,
    created_by uuid not null references auth.users(id) on delete cascade,
    created_at timestamptz not null default now(), expires_at timestamptz not null,
    accepted_at timestamptz, accepted_by uuid references auth.users(id) on delete set null
);
create table if not exists public.shared_transactions (
    id uuid primary key default gen_random_uuid(),
    ledger_id uuid not null references public.shared_ledgers(id) on delete cascade,
    transaction_id uuid not null,
    owner_id uuid not null references auth.users(id) on delete cascade,
    type text not null check (type in ('EXPENSE', 'INCOME')),
    amount bigint not null check (amount between 1 and 1000000000000),
    occurred_at timestamptz not null,
    timezone text not null check (length(timezone) between 1 and 80),
    category_key text not null check (length(category_key) between 1 and 40),
    merchant text not null check (length(merchant) between 1 and 300),
    memo text not null default '' check (length(memo) <= 1000),
    payment_method text not null default '카드' check (length(payment_method) <= 80),
    updated_at timestamptz not null default now(), deleted_at timestamptz,
    unique (ledger_id, transaction_id)
);
create index if not exists shared_transactions_ledger_updated_idx
    on public.shared_transactions (ledger_id, updated_at);
create table if not exists public.shared_goals (
    id uuid primary key default gen_random_uuid(),
    ledger_id uuid not null references public.shared_ledgers(id) on delete cascade,
    owner_id uuid not null references auth.users(id) on delete cascade,
    title text not null check (length(title) between 1 and 80),
    target_amount bigint not null check (target_amount between 1 and 1000000000000),
    month_key text not null check (month_key ~ '^[0-9]{4}-(0[1-9]|1[0-2])$'),
    created_at timestamptz not null default now(), updated_at timestamptz not null default now(),
    unique (ledger_id, month_key, title)
);

create or replace function public.is_shared_ledger_member(target_ledger uuid)
returns boolean language sql stable security definer set search_path = ''
as $$ select exists (
    select 1 from public.shared_ledger_members m
    where m.ledger_id = target_ledger and m.user_id = auth.uid()
); $$;
revoke all on function public.is_shared_ledger_member(uuid) from public, anon;
grant execute on function public.is_shared_ledger_member(uuid) to authenticated;

alter table public.shared_ledgers enable row level security;
alter table public.shared_ledger_members enable row level security;
alter table public.shared_ledger_invites enable row level security;
alter table public.shared_transactions enable row level security;
alter table public.shared_goals enable row level security;

drop policy if exists "ledger members can read their ledger" on public.shared_ledgers;
drop policy if exists "members can see ledger membership" on public.shared_ledger_members;
drop policy if exists "members can read shared transactions" on public.shared_transactions;
drop policy if exists "members can add their own shared transactions" on public.shared_transactions;
drop policy if exists "owners can update their shared transactions" on public.shared_transactions;
drop policy if exists "owners can remove their shared transactions" on public.shared_transactions;
drop policy if exists "members can read shared goals" on public.shared_goals;
drop policy if exists "members can create shared goals" on public.shared_goals;
drop policy if exists "goal owners can update their goals" on public.shared_goals;
drop policy if exists "goal owners can delete their goals" on public.shared_goals;

create policy "ledger members can read their ledger" on public.shared_ledgers
    for select to authenticated using (public.is_shared_ledger_member(id));
create policy "members can see ledger membership" on public.shared_ledger_members
    for select to authenticated using (public.is_shared_ledger_member(ledger_id));
create policy "members can read shared transactions" on public.shared_transactions
    for select to authenticated using (public.is_shared_ledger_member(ledger_id));
create policy "members can add their own shared transactions" on public.shared_transactions
    for insert to authenticated with check (
        owner_id = (select auth.uid()) and public.is_shared_ledger_member(ledger_id));
create policy "owners can update their shared transactions" on public.shared_transactions
    for update to authenticated using (
        owner_id = (select auth.uid()) and public.is_shared_ledger_member(ledger_id))
    with check (owner_id = (select auth.uid()) and public.is_shared_ledger_member(ledger_id));
create policy "owners can remove their shared transactions" on public.shared_transactions
    for delete to authenticated using (
        owner_id = (select auth.uid()) and public.is_shared_ledger_member(ledger_id));
create policy "members can read shared goals" on public.shared_goals
    for select to authenticated using (public.is_shared_ledger_member(ledger_id));
create policy "members can create shared goals" on public.shared_goals
    for insert to authenticated with check (
        owner_id = (select auth.uid()) and public.is_shared_ledger_member(ledger_id));
create policy "goal owners can update their goals" on public.shared_goals
    for update to authenticated using (
        owner_id = (select auth.uid()) and public.is_shared_ledger_member(ledger_id))
    with check (owner_id = (select auth.uid()) and public.is_shared_ledger_member(ledger_id));
create policy "goal owners can delete their goals" on public.shared_goals
    for delete to authenticated using (
        owner_id = (select auth.uid()) and public.is_shared_ledger_member(ledger_id));

revoke all on public.shared_ledgers, public.shared_ledger_members,
    public.shared_ledger_invites, public.shared_transactions, public.shared_goals from anon, public;
grant select on public.shared_ledgers, public.shared_ledger_members to authenticated;
grant select, insert, update, delete on public.shared_transactions, public.shared_goals to authenticated;

create or replace function public.create_shared_ledger_invite()
returns jsonb language plpgsql security definer set search_path = ''
as $$
declare
    caller uuid := auth.uid(); target_ledger uuid; member_total integer; invite_code text;
begin
    if caller is null then raise exception 'authentication required' using errcode = '28000'; end if;
    select m.ledger_id into target_ledger from public.shared_ledger_members m where m.user_id = caller;
    if target_ledger is null then
        insert into public.shared_ledgers default values returning id into target_ledger;
        insert into public.shared_ledger_members (ledger_id, user_id) values (target_ledger, caller);
    end if;
    perform 1 from public.shared_ledgers l where l.id = target_ledger for update;
    select count(*) into member_total from public.shared_ledger_members m where m.ledger_id = target_ledger;
    if member_total >= 2 then raise exception 'ledger already has two members' using errcode = '23514'; end if;
    update public.shared_ledger_invites set expires_at = now()
    where ledger_id = target_ledger and accepted_at is null and expires_at > now();
    invite_code := upper(encode(extensions.gen_random_bytes(10), 'hex'));
    insert into public.shared_ledger_invites (ledger_id, code_hash, created_by, expires_at)
    values (target_ledger, extensions.digest(invite_code, 'sha256'), caller, now() + interval '24 hours');
    return jsonb_build_object('ledger_id', target_ledger, 'invite_code', invite_code,
        'expires_at', now() + interval '24 hours');
end; $$;

create or replace function public.join_shared_ledger(invite_code text)
returns jsonb language plpgsql security definer set search_path = ''
as $$
declare
    caller uuid := auth.uid(); invite public.shared_ledger_invites%rowtype; member_total integer;
begin
    if caller is null then raise exception 'authentication required' using errcode = '28000'; end if;
    if invite_code is null or invite_code !~ '^[A-F0-9]{20}$' then
        raise exception 'invalid invitation code' using errcode = '22023'; end if;
    if exists (select 1 from public.shared_ledger_members m where m.user_id = caller) then
        raise exception 'account already belongs to a ledger' using errcode = '23505'; end if;
    select * into invite from public.shared_ledger_invites i
    where i.code_hash = extensions.digest(upper(invite_code), 'sha256') for update;
    if not found or invite.accepted_at is not null or invite.expires_at <= now() then
        raise exception 'invitation is invalid, used, or expired' using errcode = '22023'; end if;
    perform 1 from public.shared_ledgers l where l.id = invite.ledger_id for update;
    select count(*) into member_total from public.shared_ledger_members m where m.ledger_id = invite.ledger_id;
    if member_total >= 2 then raise exception 'ledger already has two members' using errcode = '23514'; end if;
    insert into public.shared_ledger_members (ledger_id, user_id) values (invite.ledger_id, caller);
    update public.shared_ledger_invites set accepted_at = now(), accepted_by = caller where id = invite.id;
    return jsonb_build_object('ledger_id', invite.ledger_id);
end; $$;

revoke all on function public.create_shared_ledger_invite() from public, anon;
revoke all on function public.join_shared_ledger(text) from public, anon;
grant execute on function public.create_shared_ledger_invite() to authenticated;
grant execute on function public.join_shared_ledger(text) to authenticated;
