-- Shared savings goals are jointly editable by ledger members.
-- Track contributions as a value entered by the couple, not inferred from
-- unrelated transaction memos or categories.
alter table public.shared_goals
    add column if not exists current_amount bigint not null default 0
        check (current_amount between 0 and 1000000000000);

-- Both ledger members can edit/delete shared goals, but goal ownership remains
-- immutable from the client via the column-level update grant below.
drop policy if exists "goal owners can update their goals" on public.shared_goals;
drop policy if exists "members can update shared goals" on public.shared_goals;
create policy "members can update shared goals" on public.shared_goals
    for update to authenticated
    using (public.is_shared_ledger_member(ledger_id))
    with check (public.is_shared_ledger_member(ledger_id));

drop policy if exists "goal owners can delete their goals" on public.shared_goals;
drop policy if exists "members can delete shared goals" on public.shared_goals;
create policy "members can delete shared goals" on public.shared_goals
    for delete to authenticated
    using (public.is_shared_ledger_member(ledger_id));

revoke update on public.shared_goals from authenticated;
grant update (title, target_amount, current_amount, month_key, updated_at)
    on public.shared_goals to authenticated;
