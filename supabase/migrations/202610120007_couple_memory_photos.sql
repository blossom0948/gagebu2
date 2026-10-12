-- Couple memories are opt-in: clients reserve one row only after the user picks
-- a photo. The app strips EXIF and stores a JPEG capped at 1 MiB per image.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('couple_memory', 'couple_memory', false, 1048576, array['image/jpeg'])
on conflict (id) do update
set name = excluded.name,
    public = false,
    file_size_limit = excluded.file_size_limit,
    allowed_mime_types = excluded.allowed_mime_types;

create table if not exists public.shared_memory_photos (
    id uuid primary key,
    ledger_id uuid not null references public.shared_ledgers(id) on delete cascade,
    owner_id uuid references auth.users(id) on delete set null,
    title text not null check (length(btrim(title)) between 1 and 60),
    memo text not null default '' check (length(memo) <= 300),
    photo_date date not null,
    object_path text not null unique,
    size_bytes bigint not null check (size_bytes between 1 and 1048576),
    uploaded_at timestamptz,
    created_at timestamptz not null default now(),
    constraint shared_memory_photos_path check (object_path = ledger_id::text || '/' || id::text || '.jpg')
);
create index if not exists shared_memory_photos_ledger_date_idx
    on public.shared_memory_photos (ledger_id, photo_date desc, created_at desc);

create or replace function public.enforce_shared_memory_quota()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    current_count integer;
    current_bytes bigint;
begin
    if auth.uid() is null or new.owner_id is distinct from auth.uid()
        or not public.is_shared_ledger_member(new.ledger_id) then
        raise exception 'shared ledger membership required' using errcode = '42501';
    end if;

    -- Serialize reservations per couple so parallel uploads cannot exceed caps.
    perform 1 from public.shared_ledgers l where l.id = new.ledger_id for update;
    if not found then raise exception 'shared ledger not found' using errcode = 'P0002'; end if;
    select count(*), coalesce(sum(p.size_bytes), 0)
      into current_count, current_bytes
      from public.shared_memory_photos p
     where p.ledger_id = new.ledger_id;
    if current_count >= 30 or current_bytes + new.size_bytes > 15728640 then
        raise exception 'shared photo quota exceeded' using errcode = '23514';
    end if;
    return new;
end;
$$;
revoke all on function public.enforce_shared_memory_quota() from public, anon, authenticated;

drop trigger if exists shared_memory_quota_before_insert on public.shared_memory_photos;
create trigger shared_memory_quota_before_insert
before insert on public.shared_memory_photos
for each row execute function public.enforce_shared_memory_quota();

alter table public.shared_memory_photos enable row level security;
revoke all on public.shared_memory_photos from public, anon;
grant select, insert, delete on public.shared_memory_photos to authenticated;
grant update (title, memo, photo_date, uploaded_at) on public.shared_memory_photos to authenticated;

drop policy if exists "members can read shared memories" on public.shared_memory_photos;
drop policy if exists "members can add own shared memories" on public.shared_memory_photos;
drop policy if exists "owners can finish own memory uploads" on public.shared_memory_photos;
drop policy if exists "members can delete shared memories" on public.shared_memory_photos;
create policy "members can read shared memories" on public.shared_memory_photos
    for select to authenticated
    using (public.is_shared_ledger_member(ledger_id));
create policy "members can add own shared memories" on public.shared_memory_photos
    for insert to authenticated
    with check (owner_id = (select auth.uid()) and public.is_shared_ledger_member(ledger_id));
create policy "owners can finish own memory uploads" on public.shared_memory_photos
    for update to authenticated
    using (owner_id = (select auth.uid()) and public.is_shared_ledger_member(ledger_id))
    with check (owner_id = (select auth.uid()) and public.is_shared_ledger_member(ledger_id));
create policy "members can delete shared memories" on public.shared_memory_photos
    for delete to authenticated
    using (public.is_shared_ledger_member(ledger_id));

drop policy if exists "shared ledger members can view couple memories" on storage.objects;
drop policy if exists "owners can upload couple memories" on storage.objects;
drop policy if exists "members can delete couple memories" on storage.objects;
create policy "shared ledger members can view couple memories" on storage.objects
    for select to authenticated
    using (
        bucket_id = 'couple_memory'
        and exists (
            select 1 from public.shared_memory_photos p
             where p.object_path = name
               and p.uploaded_at is not null
               and public.is_shared_ledger_member(p.ledger_id)
        )
    );
create policy "owners can upload couple memories" on storage.objects
    for insert to authenticated
    with check (
        bucket_id = 'couple_memory'
        and exists (
            select 1 from public.shared_memory_photos p
             where p.object_path = name
               and p.owner_id = (select auth.uid())
               and p.uploaded_at is null
               and public.is_shared_ledger_member(p.ledger_id)
               and case
                   when coalesce(metadata ->> 'size', '') ~ '^[0-9]{1,7}$'
                   then (metadata ->> 'size')::bigint = p.size_bytes
                   else false
               end
        )
    );
create policy "members can delete couple memories" on storage.objects
    for delete to authenticated
    using (
        bucket_id = 'couple_memory'
        and exists (
            select 1 from public.shared_memory_photos p
             where p.object_path = name
               and public.is_shared_ledger_member(p.ledger_id)
        )
    );
