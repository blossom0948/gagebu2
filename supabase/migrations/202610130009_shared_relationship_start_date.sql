-- Optional shared relationship start date for the couple D+ card.
-- Existing shared settings remain unchanged; a null date hides the counter.
alter table public.shared_ledger_settings
    add column if not exists relationship_start_date date;

alter table public.shared_ledger_settings
    drop constraint if exists shared_ledger_settings_relationship_start_date_check;

alter table public.shared_ledger_settings
    add constraint shared_ledger_settings_relationship_start_date_check
    check (
        relationship_start_date is null
        or relationship_start_date between date '1900-01-01' and date '2100-12-31'
    );
