-- Run with psql from this directory:
-- psql -d bank -f init_database.sql

\ir createTablesScripts/currencies.sql
\ir createTablesScripts/users.sql
\ir createTablesScripts/accounts.sql
\ir createTablesScripts/deposit_types.sql
\ir createTablesScripts/deposits.sql
\ir createTablesScripts/loan_types.sql
\ir createTablesScripts/loans.sql
\ir createTablesScripts/transactions.sql
\ir seedDataScripts/001_seed_reference_data.sql
