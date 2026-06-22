package org.openbank.dao;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openbank.dao.impl.AccountDaoImpl;
import org.openbank.dao.impl.CurrencyDaoImpl;
import org.openbank.dao.impl.DepositDaoImpl;
import org.openbank.dao.impl.DepositTypeDaoImpl;
import org.openbank.dao.impl.LoanDaoImpl;
import org.openbank.dao.impl.LoanTypeDaoImpl;
import org.openbank.dao.impl.TransactionDaoImpl;
import org.openbank.dao.impl.UserDaoImpl;
import org.openbank.db.ConnectionPool;
import org.openbank.model.Account;
import org.openbank.model.Deposit;
import org.openbank.model.Loan;
import org.openbank.model.Transaction;
import org.openbank.model.User;
import org.openbank.model.status.AccountStatus;
import org.openbank.model.status.DepositStatus;
import org.openbank.model.status.UserStatus;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DaoIntegrationTest {

  private Connection connection;
  private CurrencyDaoImpl currencyDao;
  private AccountDaoImpl accountDao;
  private DepositTypeDaoImpl depositTypeDao;
  private DepositDaoImpl depositDao;
  private LoanTypeDaoImpl loanTypeDao;
  private LoanDaoImpl loanDao;
  private TransactionDaoImpl transactionDao;
  private UserDaoImpl userDao;

  @BeforeEach
  void setUp() throws Exception {
    connection = DriverManager.getConnection("jdbc:h2:mem:" + System.nanoTime() + ";MODE=PostgreSQL;DATABASE_TO_UPPER=false");
    createSchema();
    seedData();

    ConnectionPool connectionPool = mock(ConnectionPool.class);
    when(connectionPool.getConnection()).thenReturn(connection);
    currencyDao = new CurrencyDaoImpl(connectionPool);
    accountDao = new AccountDaoImpl(connectionPool);
    depositTypeDao = new DepositTypeDaoImpl(connectionPool);
    depositDao = new DepositDaoImpl(connectionPool);
    loanTypeDao = new LoanTypeDaoImpl(connectionPool);
    loanDao = new LoanDaoImpl(connectionPool);
    transactionDao = new TransactionDaoImpl(connectionPool);
    userDao = new UserDaoImpl(connectionPool);
  }

  @AfterEach
  void tearDown() throws Exception {
    connection.close();
  }

  @Test
  void currencyDaoReadsAndUpdatesCurrencies() {
    assertEquals("KZT", currencyDao.getCurrencyNameById(100L));
    assertEquals(new BigDecimal("500.00"), currencyDao.getCurrencyRateToKztById(101L));
    assertEquals(2, currencyDao.getAllCurrencies().size());

    assertTrue(currencyDao.updateCurrencyRate(101L, new BigDecimal("510.00")));

    assertEquals(new BigDecimal("510.00"), currencyDao.getCurrencyRateToKztById(101L));
  }

  @Test
  void accountDaoCreatesReadsAndUpdatesAccounts() {
    assertTrue(accountDao.createNewAccount(100L, "4000000000000003", "456", LocalDate.of(2031, 1, 1), BigDecimal.ZERO, 1L, AccountStatus.PENDING, new BigDecimal("1000"), "New", false));
    assertTrue(accountDao.getAccountByCardNumber("4000000000000003").isPresent());
    assertEquals(2, accountDao.getAccountsByUserId(100L).size());
    assertEquals(1, accountDao.getAccountsByStatus(AccountStatus.PENDING).size());
    assertEquals(1, accountDao.countAccountsByUserIdAndStatus(100L, AccountStatus.ACTIVE));

    assertTrue(accountDao.updateTransactionLimit(100L, new BigDecimal("2000")));
    assertTrue(accountDao.withdraw(100L, new BigDecimal("100")));
    assertTrue(accountDao.topUp(100L, new BigDecimal("50")));
    assertTrue(accountDao.clearMainAccount(100L));
    assertTrue(accountDao.setMainAccount(100L));

    Account account = accountDao.getAccountById(100L).orElseThrow();
    assertEquals(new BigDecimal("950.00"), account.getBalance());
    assertEquals(new BigDecimal("2000.00"), account.getTransactionLimit());
    assertTrue(account.getMain());
  }

  @Test
  void depositDaosCreateReadAndUpdateDeposits() {
    assertTrue(depositTypeDao.createNewDepositType("Копилка", new BigDecimal("10"), 12, true, new BigDecimal("100"), 1L));
    assertEquals(2, depositTypeDao.getAllDepositTypes().size());
    assertTrue(depositTypeDao.changeRateOfDepositType(100L, new BigDecimal("9.5")));

    assertTrue(depositDao.createDeposit(100L, 100L, true, false, DepositStatus.PENDING, LocalDate.of(2026, 1, 1), new BigDecimal("1000")));
    assertEquals(1, depositDao.getPendingDeposits().size());
    assertEquals(1, depositDao.getDepositsByUserId(100L).size());
    assertTrue(depositDao.acceptDeposit(1L));
    assertTrue(depositDao.topUpDeposit(connection, 1L, new BigDecimal("100")));
    assertTrue(depositDao.withdrawFromDeposit(connection, 1L, new BigDecimal("50")));
    assertTrue(depositDao.updateStartDate(connection, 1L, LocalDate.of(2026, 2, 1)));

    Deposit deposit = depositDao.getDepositById(1L).orElseThrow();
    assertEquals(DepositStatus.ACTIVE.name(), deposit.getStatus());
    assertEquals(new BigDecimal("1050.00"), deposit.getCurrentAmount());
  }

  @Test
  void loanDaosCreateOffersAndUpdateStatuses() {
    assertTrue(loanTypeDao.createNewTypeOfLoan("На любые цели", new BigDecimal("15"), 12, new BigDecimal("100"), new BigDecimal("1000000"), 1L));
    assertEquals(2, loanTypeDao.getAllLoanTypes().size());
    assertTrue(loanTypeDao.changeRateOfLoanType(100L, new BigDecimal("14")));

    assertTrue(loanDao.createPendingLoan(100L, 100L, new BigDecimal("10000")));
    assertEquals(1, loanDao.getPendingLoans().size());
    Loan pendingLoan = loanDao.getLoansByUserId(100L).getFirst();
    assertTrue(loanDao.createOffer(pendingLoan.getLoanId(), 100L, 100L, new BigDecimal("10000"), new BigDecimal("12"), 12, new BigDecimal("888")));
    assertEquals(1, loanDao.getOffers(100L).size());
    assertTrue(loanDao.rejectPendingLoan(pendingLoan.getLoanId()));
  }

  @Test
  void transactionDaoCreatesAndReadsHistory() {
    assertTrue(transactionDao.createNewTransaction(100L, null, LocalDateTime.of(2026, 1, 1, 12, 0), new BigDecimal("100"), 1L, BigDecimal.ZERO, "Test", "CARD_TRANSFER"));

    List<Transaction> accountTransactions = transactionDao.getTransactionsByAccountId(100L);
    List<Transaction> userTransactions = transactionDao.getRecentTransactionsByUserId(100L, 10);

    assertEquals(1, accountTransactions.size());
    assertEquals(1, userTransactions.size());
    assertEquals("CARD_TRANSFER", accountTransactions.getFirst().getTransactionType());
  }

  @Test
  void userDaoCreatesReadsAndUpdatesUsers() {
    User user = new User(null, "Dana", "Kim", "+77009998877", "dana@example.com", "CLIENT", UserStatus.ACTIVE.name(), LocalDate.of(2026, 1, 1), null, "hash");

    assertTrue(userDao.createNewUser(user));
    assertTrue(userDao.existsByPhoneNumber("+77009998877"));
    assertTrue(userDao.existsByEmailAddress("dana@example.com"));

    User saved = userDao.getUserByEmailAddress("dana@example.com").orElseThrow();
    assertTrue(userDao.changePhoneNumberOfUserById(saved.getUserId(), "+77001110000"));
    assertTrue(userDao.changeEmailAddressOfUserById(saved.getUserId(), "new@example.com"));
    assertTrue(userDao.changePasswordHashOfUserById(saved.getUserId(), "new-hash"));
    assertTrue(userDao.changeStatusOfUserById(saved.getUserId(), UserStatus.DEACTIVATED.name()));

    assertFalse(userDao.getUsersByRoleAndStatus("CLIENT", UserStatus.DEACTIVATED.name()).isEmpty());
  }

  private void createSchema() throws Exception {
    try (Statement statement = connection.createStatement()) {
      statement.execute("""
          create table currencies (
            currency_id bigint generated by default as identity primary key,
            name varchar(20),
            rate_to_kzt numeric(19, 2)
          )
          """);
      statement.execute("""
          create table users (
            user_id bigint generated by default as identity primary key,
            name varchar(100),
            surname varchar(100),
            phone_number varchar(30),
            email_address varchar(100),
            role varchar(30),
            status varchar(30),
            date_created date,
            date_modified date,
            password_hash varchar(255)
          )
          """);
      statement.execute("""
          create table accounts (
            account_id bigint generated by default as identity primary key,
            user_id bigint,
            card_number varchar(30),
            cvv varchar(3),
            expiry_date date,
            balance numeric(19, 2),
            currency_id bigint,
            status varchar(30),
            transaction_limit numeric(19, 2),
            name varchar(100),
            is_main boolean
          )
          """);
      statement.execute("""
          create table deposit_types (
            deposit_type_id bigint generated by default as identity primary key,
            name varchar(100),
            rate numeric(19, 2),
            duration integer,
            withdrawal boolean,
            minimum_amount numeric(19, 2),
            currency_id bigint
          )
          """);
      statement.execute("""
          create table deposits (
            deposit_id bigint generated by default as identity primary key,
            user_id bigint,
            deposit_type_id bigint,
            reinvest_interest boolean,
            auto_renewal boolean,
            status varchar(30),
            start_date date,
            current_amount numeric(19, 2)
          )
          """);
      statement.execute("""
          create table loan_types (
            loan_type_id bigint generated by default as identity primary key,
            name varchar(100),
            rate numeric(19, 2),
            duration integer,
            minimum_amount numeric(19, 2),
            maximum_amount numeric(19, 2),
            currency_id bigint
          )
          """);
      statement.execute("""
          create table loans (
            loan_id bigint generated by default as identity primary key,
            user_id bigint,
            loan_type_id bigint,
            parent_loan_id bigint,
            remaining_amount numeric(19, 2),
            rate numeric(19, 2),
            duration integer,
            status varchar(30),
            start_date date,
            monthly_payment numeric(19, 2)
          )
          """);
      statement.execute("""
          create table transactions (
            transaction_id bigint generated by default as identity primary key,
            sender_account_id bigint,
            receiver_account_id bigint,
            transaction_date timestamp,
            amount numeric(19, 2),
            currency_id bigint,
            fee numeric(19, 2),
            message varchar(255),
            transaction_type varchar(50)
          )
          """);
    }
  }

  private void seedData() throws Exception {
    try (Statement statement = connection.createStatement()) {
      statement.execute("insert into currencies(currency_id, name, rate_to_kzt) values (100, 'KZT', 1.00), (101, 'USD', 500.00)");
      statement.execute("insert into users(user_id, name, surname, phone_number, email_address, role, status, date_created, password_hash) values (100, 'Aruzhan', 'Sadyk', '+77001112233', 'aru@example.com', 'CLIENT', 'ACTIVE', current_date, 'hash')");
      statement.execute("insert into accounts(account_id, user_id, card_number, cvv, expiry_date, balance, currency_id, status, transaction_limit, name, is_main) values (100, 100, '4000000000000002', '123', date '2030-01-01', 1000.00, 1, 'ACTIVE', 1000.00, 'Main', true)");
      statement.execute("insert into deposit_types(deposit_type_id, name, rate, duration, withdrawal, minimum_amount, currency_id) values (100, 'Стратегия', 8.00, 12, false, 100.00, 1)");
      statement.execute("insert into loan_types(loan_type_id, name, rate, duration, minimum_amount, maximum_amount, currency_id) values (100, 'Ипотека', 12.00, 12, 100.00, 1000000.00, 1)");
    }
  }
}
