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

  private static final String H2_URL_PREFIX = "jdbc:h2:mem:";
  private static final String H2_URL_OPTIONS = ";MODE=PostgreSQL;DATABASE_TO_UPPER=false";
  private static final Long SEEDED_ID = 100L;
  private static final Long USD_CURRENCY_ID = 101L;
  private static final Long INSERTED_RECORD_ID = 1L;
  private static final Long CURRENCY_ID = 1L;
  private static final String KZT_CURRENCY_NAME = "KZT";
  private static final String USD_CURRENCY_NAME = "USD";
  private static final String SEEDED_NAME = "Aruzhan";
  private static final String SEEDED_SURNAME = "Sadyk";
  private static final String SEEDED_PHONE = "+77001112233";
  private static final String SEEDED_EMAIL = "aru@example.com";
  private static final String CLIENT_ROLE = "CLIENT";
  private static final String PASSWORD_HASH = "hash";
  private static final String SEEDED_CARD_NUMBER = "4000000000000002";
  private static final String NEW_CARD_NUMBER = "4000000000000003";
  private static final String SEEDED_CVV = "123";
  private static final String NEW_CVV = "456";
  private static final String SEEDED_ACCOUNT_NAME = "Main";
  private static final String NEW_ACCOUNT_NAME = "New";
  private static final String SEEDED_DEPOSIT_TYPE_NAME = "Стратегия";
  private static final String NEW_DEPOSIT_TYPE_NAME = "Копилка";
  private static final String SEEDED_LOAN_TYPE_NAME = "Ипотека";
  private static final String NEW_LOAN_TYPE_NAME = "На любые цели";
  private static final String TRANSACTION_MESSAGE = "Test";
  private static final String CARD_TRANSFER_TYPE = "CARD_TRANSFER";
  private static final String NEW_USER_NAME = "Dana";
  private static final String NEW_USER_SURNAME = "Kim";
  private static final String NEW_USER_PHONE = "+77009998877";
  private static final String UPDATED_USER_PHONE = "+77001110000";
  private static final String NEW_USER_EMAIL = "dana@example.com";
  private static final String UPDATED_USER_EMAIL = "new@example.com";
  private static final String UPDATED_PASSWORD_HASH = "new-hash";
  private static final String SEEDED_KZT_RATE_TO_KZT_SQL = "1.00";
  private static final String SEEDED_DEPOSIT_RATE_SQL = "8.00";
  private static final String SEEDED_ACCOUNT_EXPIRY_DATE_SQL = "2030-01-01";
  private static final BigDecimal USD_RATE_TO_KZT = new BigDecimal("500.00");
  private static final BigDecimal UPDATED_USD_RATE_TO_KZT = new BigDecimal("510.00");
  private static final BigDecimal ACCOUNT_OPEN_LIMIT = new BigDecimal("1000");
  private static final BigDecimal UPDATED_ACCOUNT_LIMIT = new BigDecimal("2000");
  private static final BigDecimal WITHDRAW_AMOUNT = new BigDecimal("100");
  private static final BigDecimal TOP_UP_AMOUNT = new BigDecimal("50");
  private static final BigDecimal EXPECTED_ACCOUNT_BALANCE = new BigDecimal("950.00");
  private static final BigDecimal EXPECTED_ACCOUNT_LIMIT = new BigDecimal("2000.00");
  private static final BigDecimal NEW_DEPOSIT_RATE = new BigDecimal("10");
  private static final BigDecimal UPDATED_DEPOSIT_RATE = new BigDecimal("9.5");
  private static final BigDecimal DEPOSIT_AMOUNT = new BigDecimal("1000");
  private static final BigDecimal EXPECTED_DEPOSIT_AMOUNT = new BigDecimal("1050.00");
  private static final BigDecimal NEW_LOAN_RATE = new BigDecimal("15");
  private static final BigDecimal UPDATED_LOAN_RATE = new BigDecimal("14");
  private static final BigDecimal LOAN_AMOUNT = new BigDecimal("10000");
  private static final BigDecimal LOAN_OFFER_RATE = new BigDecimal("12");
  private static final BigDecimal MONTHLY_PAYMENT = new BigDecimal("888");
  private static final BigDecimal MAXIMUM_LOAN_AMOUNT = new BigDecimal("1000000");
  private static final LocalDate NEW_ACCOUNT_EXPIRY_DATE = LocalDate.of(2031, 1, 1);
  private static final LocalDate OPERATION_DATE = LocalDate.of(2026, 1, 1);
  private static final LocalDate DEPOSIT_UPDATED_START_DATE = LocalDate.of(2026, 2, 1);
  private static final LocalDateTime TRANSACTION_DATE = LocalDateTime.of(2026, 1, 1, 12, 0);
  private static final int EXPECTED_TWO_RECORDS = 2;
  private static final int EXPECTED_ONE_RECORD = 1;
  private static final int RECENT_TRANSACTION_LIMIT = 10;
  private static final int PRODUCT_DURATION = 12;

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
    connection = DriverManager.getConnection(H2_URL_PREFIX + System.nanoTime() + H2_URL_OPTIONS);
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
    assertEquals(KZT_CURRENCY_NAME, currencyDao.getCurrencyNameById(SEEDED_ID));
    assertEquals(USD_RATE_TO_KZT, currencyDao.getCurrencyRateToKztById(USD_CURRENCY_ID));
    assertEquals(EXPECTED_TWO_RECORDS, currencyDao.getAllCurrencies().size());

    assertTrue(currencyDao.updateCurrencyRate(USD_CURRENCY_ID, UPDATED_USD_RATE_TO_KZT));

    assertEquals(UPDATED_USD_RATE_TO_KZT, currencyDao.getCurrencyRateToKztById(USD_CURRENCY_ID));
  }

  @Test
  void accountDaoCreatesReadsAndUpdatesAccounts() {
    assertTrue(accountDao.createNewAccount(SEEDED_ID, NEW_CARD_NUMBER, NEW_CVV, NEW_ACCOUNT_EXPIRY_DATE, BigDecimal.ZERO, CURRENCY_ID, AccountStatus.PENDING, ACCOUNT_OPEN_LIMIT, NEW_ACCOUNT_NAME, false));
    assertTrue(accountDao.getAccountByCardNumber(NEW_CARD_NUMBER).isPresent());
    assertEquals(EXPECTED_TWO_RECORDS, accountDao.getAccountsByUserId(SEEDED_ID).size());
    assertEquals(EXPECTED_ONE_RECORD, accountDao.getAccountsByStatus(AccountStatus.PENDING).size());
    assertEquals(EXPECTED_ONE_RECORD, accountDao.countAccountsByUserIdAndStatus(SEEDED_ID, AccountStatus.ACTIVE));

    assertTrue(accountDao.updateTransactionLimit(SEEDED_ID, UPDATED_ACCOUNT_LIMIT));
    assertTrue(accountDao.withdraw(SEEDED_ID, WITHDRAW_AMOUNT));
    assertTrue(accountDao.topUp(SEEDED_ID, TOP_UP_AMOUNT));
    assertTrue(accountDao.clearMainAccount(SEEDED_ID));
    assertTrue(accountDao.setMainAccount(SEEDED_ID));

    Account account = accountDao.getAccountById(SEEDED_ID).orElseThrow();
    assertEquals(EXPECTED_ACCOUNT_BALANCE, account.getBalance());
    assertEquals(EXPECTED_ACCOUNT_LIMIT, account.getTransactionLimit());
    assertTrue(account.getMain());
  }

  @Test
  void depositDaosCreateReadAndUpdateDeposits() {
    assertTrue(depositTypeDao.createNewDepositType(NEW_DEPOSIT_TYPE_NAME, NEW_DEPOSIT_RATE, PRODUCT_DURATION, true, WITHDRAW_AMOUNT, CURRENCY_ID));
    assertEquals(EXPECTED_TWO_RECORDS, depositTypeDao.getAllDepositTypes().size());
    assertTrue(depositTypeDao.changeRateOfDepositType(SEEDED_ID, UPDATED_DEPOSIT_RATE));

    assertTrue(depositDao.createDeposit(SEEDED_ID, SEEDED_ID, true, false, DepositStatus.PENDING, OPERATION_DATE, DEPOSIT_AMOUNT));
    assertEquals(EXPECTED_ONE_RECORD, depositDao.getPendingDeposits().size());
    assertEquals(EXPECTED_ONE_RECORD, depositDao.getDepositsByUserId(SEEDED_ID).size());
    assertTrue(depositDao.acceptDeposit(INSERTED_RECORD_ID));
    assertTrue(depositDao.topUpDeposit(connection, INSERTED_RECORD_ID, WITHDRAW_AMOUNT));
    assertTrue(depositDao.withdrawFromDeposit(connection, INSERTED_RECORD_ID, TOP_UP_AMOUNT));
    assertTrue(depositDao.updateStartDate(connection, INSERTED_RECORD_ID, DEPOSIT_UPDATED_START_DATE));

    Deposit deposit = depositDao.getDepositById(INSERTED_RECORD_ID).orElseThrow();
    assertEquals(DepositStatus.ACTIVE.name(), deposit.getStatus());
    assertEquals(EXPECTED_DEPOSIT_AMOUNT, deposit.getCurrentAmount());
  }

  @Test
  void loanDaosCreateOffersAndUpdateStatuses() {
    assertTrue(loanTypeDao.createNewTypeOfLoan(NEW_LOAN_TYPE_NAME, NEW_LOAN_RATE, PRODUCT_DURATION, WITHDRAW_AMOUNT, MAXIMUM_LOAN_AMOUNT, CURRENCY_ID));
    assertEquals(EXPECTED_TWO_RECORDS, loanTypeDao.getAllLoanTypes().size());
    assertTrue(loanTypeDao.changeRateOfLoanType(SEEDED_ID, UPDATED_LOAN_RATE));

    assertTrue(loanDao.createPendingLoan(SEEDED_ID, SEEDED_ID, SEEDED_ID, LOAN_AMOUNT));
    assertEquals(EXPECTED_ONE_RECORD, loanDao.getPendingLoans().size());
    Loan pendingLoan = loanDao.getLoansByUserId(SEEDED_ID).getFirst();
    assertTrue(loanDao.createOffer(pendingLoan.getLoanId(), SEEDED_ID, SEEDED_ID, SEEDED_ID, LOAN_AMOUNT, LOAN_OFFER_RATE, PRODUCT_DURATION, MONTHLY_PAYMENT));
    assertEquals(EXPECTED_ONE_RECORD, loanDao.getOffers(SEEDED_ID).size());
    assertTrue(loanDao.rejectPendingLoan(pendingLoan.getLoanId()));
  }

  @Test
  void transactionDaoCreatesAndReadsHistory() {
    assertTrue(transactionDao.createNewTransaction(SEEDED_ID, null, TRANSACTION_DATE, WITHDRAW_AMOUNT, CURRENCY_ID, BigDecimal.ZERO, TRANSACTION_MESSAGE, CARD_TRANSFER_TYPE));

    List<Transaction> accountTransactions = transactionDao.getTransactionsByAccountId(SEEDED_ID);
    List<Transaction> userTransactions = transactionDao.getRecentTransactionsByUserId(SEEDED_ID, RECENT_TRANSACTION_LIMIT);

    assertEquals(EXPECTED_ONE_RECORD, accountTransactions.size());
    assertEquals(EXPECTED_ONE_RECORD, userTransactions.size());
    assertEquals(CARD_TRANSFER_TYPE, accountTransactions.getFirst().getTransactionType());
  }

  @Test
  void userDaoCreatesReadsAndUpdatesUsers() {
    User user = new User(null, NEW_USER_NAME, NEW_USER_SURNAME, NEW_USER_PHONE, NEW_USER_EMAIL, CLIENT_ROLE, UserStatus.ACTIVE.name(), OPERATION_DATE, null, PASSWORD_HASH);

    assertTrue(userDao.createNewUser(user));
    assertTrue(userDao.existsByPhoneNumber(NEW_USER_PHONE));
    assertTrue(userDao.existsByEmailAddress(NEW_USER_EMAIL));

    User saved = userDao.getUserByEmailAddress(NEW_USER_EMAIL).orElseThrow();
    assertTrue(userDao.changePhoneNumberOfUserById(saved.getUserId(), UPDATED_USER_PHONE));
    assertTrue(userDao.changeEmailAddressOfUserById(saved.getUserId(), UPDATED_USER_EMAIL));
    assertTrue(userDao.changePasswordHashOfUserById(saved.getUserId(), UPDATED_PASSWORD_HASH));
    assertTrue(userDao.changeStatusOfUserById(saved.getUserId(), UserStatus.DEACTIVATED.name()));

    assertFalse(userDao.getUsersByRoleAndStatus(CLIENT_ROLE, UserStatus.DEACTIVATED.name()).isEmpty());
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
            account_id bigint,
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
      statement.execute("insert into currencies(currency_id, name, rate_to_kzt) values (%d, '%s', %s), (%d, '%s', %s)".formatted(SEEDED_ID, KZT_CURRENCY_NAME, SEEDED_KZT_RATE_TO_KZT_SQL, USD_CURRENCY_ID, USD_CURRENCY_NAME, USD_RATE_TO_KZT));
      statement.execute("insert into users(user_id, name, surname, phone_number, email_address, role, status, date_created, password_hash) values (%d, '%s', '%s', '%s', '%s', '%s', '%s', current_date, '%s')".formatted(SEEDED_ID, SEEDED_NAME, SEEDED_SURNAME, SEEDED_PHONE, SEEDED_EMAIL, CLIENT_ROLE, UserStatus.ACTIVE.name(), PASSWORD_HASH));
      statement.execute("insert into accounts(account_id, user_id, card_number, cvv, expiry_date, balance, currency_id, status, transaction_limit, name, is_main) values (%d, %d, '%s', '%s', date '%s', %s, %d, '%s', %s, '%s', true)".formatted(SEEDED_ID, SEEDED_ID, SEEDED_CARD_NUMBER, SEEDED_CVV, SEEDED_ACCOUNT_EXPIRY_DATE_SQL, ACCOUNT_OPEN_LIMIT, CURRENCY_ID, AccountStatus.ACTIVE.name(), ACCOUNT_OPEN_LIMIT, SEEDED_ACCOUNT_NAME));
      statement.execute("insert into deposit_types(deposit_type_id, name, rate, duration, withdrawal, minimum_amount, currency_id) values (%d, '%s', %s, %d, false, %s, %d)".formatted(SEEDED_ID, SEEDED_DEPOSIT_TYPE_NAME, SEEDED_DEPOSIT_RATE_SQL, PRODUCT_DURATION, WITHDRAW_AMOUNT, CURRENCY_ID));
      statement.execute("insert into loan_types(loan_type_id, name, rate, duration, minimum_amount, maximum_amount, currency_id) values (%d, '%s', %s, %d, %s, %s, %d)".formatted(SEEDED_ID, SEEDED_LOAN_TYPE_NAME, LOAN_OFFER_RATE, PRODUCT_DURATION, WITHDRAW_AMOUNT, MAXIMUM_LOAN_AMOUNT, CURRENCY_ID));
    }
  }
}
