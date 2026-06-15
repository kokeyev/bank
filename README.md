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
2. Run the SQL files from `src/main/sql_scripts/createTablesScripts` in this order:
   - `currencies.sql`
   - `users.sql`
   - `accounts.sql`
   - `deposit_types.sql`
   - `deposits.sql`
   - `loan_types.sql`
   - `loans.sql`
   - `transactions.sql`
3. Check database settings in `src/main/resources/application.properties`.

Default local settings:

```properties
db.url=jdbc:postgresql://localhost:5432/bank?charSet=UTF-8
db.username=postgres
db.password=1234
db.pool.size=10
```

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

- **Strategy**: deposit and loan products have different rules for amount, duration, rates, top-up, withdrawal, and offers. The service layer applies these rules depending on the selected product.
- **Command-style transaction runner**: `DatabaseTransactionRunner` receives a database action and runs it inside one JDBC transaction. This keeps commit, rollback, and connection cleanup in one place.
- **Interceptor**: `LoginRequiredInterceptor` checks access before protected pages are opened.

## Notes

- Passwords are stored as salted PBKDF2 hashes.
- SQL queries use `PreparedStatement`.
- The app supports Russian, Kazakh, and English interface texts.
- Tests cover Service and DAO logic.
