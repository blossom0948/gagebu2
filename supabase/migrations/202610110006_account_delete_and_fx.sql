-- Foreign-currency records keep their original amount and the daily reference
-- rate used for the KRW amount. Personal transactions remain on-device.
alter table public.shared_transactions
    add column if not exists currency text not null default 'KRW',
    add column if not exists original_amount numeric(24, 6),
    add column if not exists exchange_rate numeric(24, 10),
    add column if not exists exchange_rate_date date;

do $$
begin
    if not exists (
        select 1 from pg_constraint
        where conrelid = 'public.shared_transactions'::regclass
          and conname = 'shared_transactions_fx_shape'
    ) then
        alter table public.shared_transactions
            add constraint shared_transactions_fx_shape check (
                currency ~ '^[A-Z]{3}$' and (
                    (currency = 'KRW' and original_amount is null and exchange_rate is null and exchange_rate_date is null)
                    or (currency <> 'KRW' and original_amount is not null and original_amount > 0
                        and exchange_rate is not null and exchange_rate > 0 and exchange_rate_date is not null)
                )
            );
    end if;
end $$;

-- A deleted account is removed from auth.users. If it was sharing with a
-- partner, shared rows are reassigned first so the partner keeps the ledger.
-- A solo shared ledger is deleted with its last account. Private finance data
-- is never uploaded and therefore remains on the device.
create or replace function public.delete_my_account()
returns boolean
language plpgsql
security definer
set search_path = ''
as $$
declare
    caller uuid := auth.uid();
    target_ledger uuid;
    remaining_member uuid;
    deleted_rows integer;
begin
    if caller is null then
        raise exception 'authentication required' using errcode = '28000';
    end if;

    select m.ledger_id into target_ledger
    from public.shared_ledger_members m
    where m.user_id = caller
    for update;

    if target_ledger is not null then
        perform 1 from public.shared_ledgers l where l.id = target_ledger for update;
        select m.user_id into remaining_member
        from public.shared_ledger_members m
        where m.ledger_id = target_ledger and m.user_id <> caller
        limit 1
        for update;

        if remaining_member is null then
            delete from public.shared_ledgers where id = target_ledger;
        else
            update public.shared_transactions
                set owner_id = remaining_member
                where ledger_id = target_ledger and owner_id = caller;
            update public.shared_goals
                set owner_id = remaining_member
                where ledger_id = target_ledger and owner_id = caller;
            update public.shared_finance_items
                set owner_id = remaining_member
                where ledger_id = target_ledger and owner_id = caller;
            update public.shared_ledger_settings
                set updated_by = remaining_member
                where ledger_id = target_ledger and updated_by = caller;
            delete from public.shared_ledger_invites
                where ledger_id = target_ledger and created_by = caller;
            delete from public.shared_ledger_members
                where ledger_id = target_ledger and user_id = caller;
        end if;
    end if;

    delete from auth.users where id = caller;
    get diagnostics deleted_rows = row_count;
    if deleted_rows <> 1 then
        raise exception 'account not found' using errcode = 'P0002';
    end if;
    return true;
end;
$$;

revoke all on function public.delete_my_account() from public, anon;
grant execute on function public.delete_my_account() to authenticated;
