# Online Bank

Online Bank is a Spring MVC web application for a Java Web Development capstone project. The application allows clients to manage bank accounts, deposits, loans, transfers, and currency exchange. Managers can review client requests, and admins can update bank settings such as fees, rates, and limits.

## Main Features

### Client

- Register, log in, log out, and deactivate an account
- Open bank accounts in different currencies
- View accounts, deposits, loans, and transaction history
- Transfer money between own accounts
- Transfer money to other clients by phone number or card number
- Transfer money to external cards
- Open deposits and apply for loans
- Exchange currencies between accounts
- Update phone number, email, password, and language

### Manager

- Review account opening requests
- Approve or reject account requests
- Review deposit opening requests
- Approve or reject deposit requests
- Review loan applications
- Create loan offers for clients
- Approve or reject loan requests
- Process expired deposits and accrue deposit interest

### Admin

- Review manager registration requests
- Approve or reject managers
- Update transfer fees
- Update currency exchange rates
- Update deposit rates
- Update loan rates
- Update account limits

## Tech Stack

- Java 21
- Spring Core
- Spring MVC
- Jakarta Servlet API
- Thymeleaf
- JDBC
- PostgreSQL
- Maven
- SLF4J and Logback
- JUnit 5
- Mockito
- H2 Database for tests
- JaCoCo for test coverage

## Project Structure

- `controller` - Spring MVC controllers
- `service` - business logic and validation rules
- `dao` - database access layer implemented with JDBC
- `db` - custom database connection pool
- `model` - main domain objects
- `dto` - request, form, and view data objects
- `config` - Spring MVC, application, database, i18n, and web configuration
- `exception` - custom application exceptions
- `view` - helpers for preparing data for Thymeleaf pages
- `resources/templates` - Thymeleaf HTML templates
- `resources/static/css` - application styles
- `sql_scripts` - database initialization, table creation, and seed data scripts
- `webapp/WEB-INF` - web application configuration
- `test` - unit and integration tests

## Database Setup

The application uses PostgreSQL by default.

1. Create a database named `bank`.
2. Run the full init script:

```bash
cd src/main/sql_scripts
psql -d bank -f init_database.sql
```

The init script creates all tables and loads seed data for currencies, deposit products, loan products, demo users, and one demo client account.

If you want to run only the schema manually, run the SQL files from `src/main/sql_scripts/createTablesScripts` in this order:
   - `currencies.sql`
   - `users.sql`
   - `accounts.sql`
   - `deposit_types.sql`
   - `deposits.sql`
   - `loan_types.sql`
   - `loans.sql`
   - `transactions.sql`
3. For reference data only, run `src/main/sql_scripts/seedDataScripts/001_seed_reference_data.sql`.
4. Check database settings in `src/main/resources/application.properties`.

Default local settings:

```properties
db.url=jdbc:postgresql://localhost:5432/bank?charSet=UTF-8
db.username=postgres
db.password=1234
db.pool.size=10
```

Demo credentials after running the seed script:

- Client: `client@openbank.kz` / `client12345`
- Manager: `manager@openbank.kz` / `manager12345`
- Admin panel: `admin@openbank.kz` / `admin12345`

Database notes:

- Seed scripts can be run again for reference data and demo records.
- CVV is stored only for this course project to simulate demo bank cards. In a real banking system, CVV must not be stored in the database.

## How To Run

1. Set up the PostgreSQL database using the instructions from the Database Setup section.

2. Build the project:

```bash
mvn clean package
```

This command also runs tests and generates the WAR file in the `target/` directory:

```text
target/bank-1.0-SNAPSHOT.war
```

3. Deploy the WAR file to a Jakarta Servlet container, for example Apache Tomcat.

To run only tests, use:

```bash
mvn test
```

To run tests with the JaCoCo coverage report, use:

```bash
mvn verify
```

## Useful Routes

### Client

- `/register` - client registration
- `/login` - client login
- `/accounts` - client dashboard
- `/accounts/open` - open a new account
- `/transfers` - transfer menu
- `/transfers/between-accounts` - transfer between own accounts
- `/transfers/account-top-up` - top up an account
- `/transfers/by-phone` - transfer by phone number
- `/transfers/by-card` - transfer by card number
- `/transfers/deposit-top-up` - top up a deposit
- `/transfers/loan-payment` - pay a loan
- `/transfers/currency-exchange` - exchange money between accounts
- `/deposits` - deposit products
- `/deposits/kopilka` - Kopilka deposit page
- `/deposits/strategy` - Strategy deposit page
- `/deposits/capital` - Capital deposit page
- `/loans` - loan products
- `/loans/purpose` - personal loan page
- `/loans/auto` - car loan page
- `/loans/mortgage` - mortgage loan page
- `/exchange` - currency exchange rates and calculator
- `/settings` - language, contact, password, and account settings

### Manager

- `/manager` - manager login and registration page
- `/manager/accounts` - account requests
- `/manager/deposits` - deposit requests
- `/manager/loans` - loan requests

### Admin

- `/admin` - admin login and dashboard

## Design Patterns

### 1. Strategy Pattern

The project uses the Strategy pattern for deposit products.

Different deposit types have different business rules, for example top-up, withdrawal, auto-renewal, and interest reinvestment. These rules are implemented through the `DepositProductStrategy` interface and separate strategy classes:

- `KopilkaDepositStrategy`
- `StrategyDepositStrategy`
- `CapitalDepositStrategy`

The correct strategy is selected by `DepositProductStrategyResolver`. This makes it easier to add a new deposit product without changing the main deposit service logic.

### 2. Interceptor Pattern

The project uses Spring MVC interceptors for request checks before the request reaches a controller.

Implemented interceptors:

- `LoginRequiredInterceptor` checks access to protected client, manager, and admin pages.
- `CsrfInterceptor` checks CSRF tokens for POST forms.
- `LocaleChangeInterceptor` changes the application language.

This keeps security and request validation logic outside controllers.

### 3. Template Callback Pattern

The project uses a template callback approach for JDBC transactions.

`DatabaseTransactionRunner` defines the common transaction flow:

- get database connection
- disable auto-commit
- execute business logic
- commit transaction
- rollback if an error happens
- release connection

The real business logic is passed as a callback. This avoids repeating transaction management code in every service method.

## Notes

- Passwords are stored as salted SHA-256 hashes.
- SQL queries use `PreparedStatement` to reduce SQL injection risks.
- Forms are validated with Jakarta Validation annotations.
- POST forms include a session CSRF token checked by `CsrfInterceptor`.
- The app supports Russian, Kazakh, and English interface texts.
- JDBC transactions are handled with explicit commit and rollback logic.
- Tests cover service logic, DAO logic, database integration, and i18n configuration.