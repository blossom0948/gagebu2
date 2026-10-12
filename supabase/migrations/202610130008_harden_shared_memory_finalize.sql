-- Only the server may mark a shared photo ready, after verifying its Storage object.
drop policy if exists "owners can finish own memory uploads" on public.shared_memory_photos;
drop policy if exists "members can add own shared memories" on public.shared_memory_photos;

create policy "members can add own unfinalized memories" on public.shared_memory_photos
    for insert to authenticated
    with check (
        owner_id = (select auth.uid())
        and uploaded_at is null
        and public.is_shared_ledger_member(ledger_id)
    );

revoke update (uploaded_at) on public.shared_memory_photos from authenticated;

create or replace function public.finalize_shared_memory_photo(p_photo_id uuid)
returns boolean
language plpgsql
security definer
set search_path = ''
as $$
declare
    photo_row public.shared_memory_photos%rowtype;
begin
    if auth.uid() is null then
        raise exception 'authentication required' using errcode = '42501';
    end if;

    select * into photo_row
      from public.shared_memory_photos
     where id = p_photo_id
     for update;
    if not found then
        raise exception 'shared photo not found' using errcode = 'P0002';
    end if;
    if photo_row.owner_id is distinct from auth.uid()
        or not public.is_shared_ledger_member(photo_row.ledger_id) then
        raise exception 'shared ledger membership required' using errcode = '42501';
    end if;

    if photo_row.uploaded_at is not null then
        return true;
    end if;

    if not exists (
        select 1 from storage.objects o
         where o.bucket_id = 'couple_memory'
           and o.name = photo_row.object_path
           and case
               when coalesce(o.metadata ->> 'size', '') ~ '^[0-9]{1,7}$'
               then (o.metadata ->> 'size')::bigint = photo_row.size_bytes
               else false
           end
    ) then
        raise exception 'shared photo object missing or size mismatch' using errcode = '23514';
    end if;

    update public.shared_memory_photos
       set uploaded_at = now()
     where id = p_photo_id;
    return true;
end;
$$;

revoke all on function public.finalize_shared_memory_photo(uuid) from public, anon;
grant execute on function public.finalize_shared_memory_photo(uuid) to authenticated;
