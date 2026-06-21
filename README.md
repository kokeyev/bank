# Online Bank

Online Bank is a Spring MVC web application for a Java Web Development capstone project. The application lets clients open bank products, managers review requests, and admins update bank settings.

## Main Features

### Client

- Register, log in, log out, and deactivate an account.
- Open bank accounts in different currencies.
- Open deposits and loans.
- Transfer money between own accounts.
- Transfer money by phone number or card number.
- Exchange currencies.
- Change language, phone number, email, and password.

### Manager

- Review account opening requests.
- Review deposit opening requests.
- Review loan requests.
- Create loan offers for clients.
- Approve or reject client requests.

### Admin

- Review manager registration requests.
- Update transfer fees.
- Update currency exchange rates.
- Update deposit and loan rates.

## Tech Stack

- Java 21
- Spring Core
- Spring MVC
- Jakarta Servlet API
- Thymeleaf
- JDBC
- PostgreSQL
- Maven
- JUnit 5
- Mockito
- H2 for tests
- SLF4J and Logback

## Project Structure

- `controller` - web endpoints and page flow.
- `service` - business rules.
- `dao` - database access with JDBC.
- `model` - main domain objects.
- `dto` - form and view objects.
- `config` - Spring and database configuration.
- `resources/templates` - Thymeleaf pages.
- `resources/static` - CSS files.
- `sql_scripts` - database table scripts.

## Database Setup

The application uses PostgreSQL by default.

1. Create a database named `bank`.
2. Run the full init script:

```bash
cd src/main/sql_scripts
psql -d bank -f init_database.sql
```

The init script creates the schema and loads seed data for currencies, deposit products, loan products, an active demo client, an active demo manager, and one demo client account.

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

- Currency names are unique in the schema.
- Seed scripts are idempotent for reference data and demo records.
- CVV is stored only because this course project simulates issued cards and displays demo card details inside the bank cabinet. The app has no merchant acquiring, online purchase, or external card authorization flow. In a production banking system, CVV must not be stored after authorization/issuance; it should be removed from persistent storage or replaced with a compliant card-provider/tokenization design.

## How To Run

Build the project:

```bash
mvn clean package
```

Run tests:

```bash
mvn test
```

Deploy the generated WAR file from `target/` to a Jakarta Servlet container such as Tomcat.

## Useful Routes

- `/register` - client registration.
- `/login` - client login.
- `/accounts` - client dashboard.
- `/accounts/open` - open a new account.
- `/transfers` - transfer menu.
- `/deposits` - deposit products.
- `/loans` - loan products.
- `/exchange` - currency exchange.
- `/settings` - language and account settings.
- `/manager/accounts` - account requests for managers.
- `/manager/deposits` - deposit requests for managers.
- `/manager/loans` - loan requests for managers.
- `/admin` - admin panel.

## Design Patterns


## Notes

- Passwords are stored as salted hashes.
- SQL queries use `PreparedStatement`.
- The app supports Russian, Kazakh, and English interface texts.
- Tests cover Service and DAO logic.
- POST forms include a session CSRF token checked by `CsrfInterceptor`.
