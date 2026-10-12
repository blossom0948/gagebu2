alter table public.shared_ledger_settings
    add column if not exists featured_memory_photo_id uuid;
