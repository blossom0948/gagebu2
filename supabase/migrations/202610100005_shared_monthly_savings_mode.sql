-- Add the monthly-savings presentation mode for the shared-ledger settings.
-- Existing settings and all ledger data remain unchanged.
alter table public.shared_ledger_settings
    drop constraint if exists shared_ledger_settings_money_mode_check;

alter table public.shared_ledger_settings
    add constraint shared_ledger_settings_money_mode_check
    check (money_mode in (
        'EQUAL_SPLIT',
        'SHARED_FUND',
        'SEPARATE',
        'INCOME_OVERVIEW',
        'MONTHLY_SAVINGS'
    ));
