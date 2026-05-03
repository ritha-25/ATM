package org.atm.banking.application;

import org.atm.banking.domain.Account;
import org.atm.banking.domain.BankingPolicies;
import org.atm.banking.domain.Transaction;
import org.atm.banking.domain.TransactionType;
import org.atm.banking.exception.AccountNotFoundException;
import org.atm.banking.exception.AtmBankingException;
import org.atm.banking.exception.DailyWithdrawalLimitException;
import org.atm.banking.exception.DuplicateAccountException;
import org.atm.banking.exception.InsufficientFundsException;
import org.atm.banking.exception.InvalidCredentialsException;
import org.atm.banking.exception.NotAuthenticatedException;
import org.atm.banking.persistence.CsvAccountRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

public final class AtmInteractionService {

    private static final int MAX_ATTEMPTS = 3;

    private final CsvAccountRepository repository;
    private final Random random = new Random();
    private final Map<String, Integer> failedAttempts = new HashMap<>();
    private final Map<String, Boolean> lockedAccounts = new HashMap<>();
    private String activeAccountNumber;

    public AtmInteractionService(CsvAccountRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    /** Auto-generates a unique 10-digit account number. */
    public String generateAccountNumber() {
        String candidate;
        do {
            long num = 1_000_000_000L + (long) (random.nextDouble() * 9_000_000_000L);
            candidate = Long.toString(num);
        } while (repository.exists(candidate));
        return candidate;
    }

    public void registerNewAccount(String accountNumber, String pin, double openingBalance)
            throws AtmBankingException {
        String acc = requireNonBlank(accountNumber, "Account number is required.");
        if (!BankingPolicies.isRecognizedAccountFormat(acc))
            throw new AtmBankingException("Invalid account number format.");
        if (!BankingPolicies.isValidPinFormat(pin))
            throw new AtmBankingException("PIN must be 4-6 digits.");
        if (openingBalance < 0)
            throw new AtmBankingException("Opening balance cannot be negative.");
        if (repository.exists(acc))
            throw new DuplicateAccountException("Account already exists.");
        repository.save(new Account(acc, pin, openingBalance));
    }

    public void login(String accountNumber, String pin) throws AtmBankingException {
        String acc = requireNonBlank(accountNumber, "Account number is required.");

        if (lockedAccounts.getOrDefault(acc, false))
            throw new AtmBankingException("Account is locked. Contact your bank.");

        Optional<Account> found = repository.findByNumber(acc);
        if (found.isEmpty())
            throw new AccountNotFoundException("Account not found.");

        Account account = found.get();

        if (!account.getPin().equals(pin)) {
            int attempts = failedAttempts.getOrDefault(acc, 0) + 1;
            failedAttempts.put(acc, attempts);
            if (attempts >= MAX_ATTEMPTS) {
                lockedAccounts.put(acc, true);
                throw new AtmBankingException("Account locked after 3 failed PIN attempts.");
            }
            int left = MAX_ATTEMPTS - attempts;
            throw new InvalidCredentialsException("Incorrect PIN. " + left + " attempt(s) remaining.");
        }

        failedAttempts.remove(acc);
        activeAccountNumber = acc;
    }

    public boolean isAccountLocked(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) return false;
        return lockedAccounts.getOrDefault(accountNumber.trim(), false);
    }

    public void logout() { activeAccountNumber = null; }

    public boolean isAuthenticated() { return activeAccountNumber != null; }

    public String currentAccountNumber() { return activeAccountNumber; }

    public double deposit(double amount) throws AtmBankingException {
        Account a = requireActive();
        requirePositive(amount);
        a.setBalance(a.getBalance() + amount);
        a.appendTransaction(new Transaction(a.getAccountNumber(), TransactionType.DEPOSIT, amount));
        return a.getBalance();
    }

    public double withdraw(double amount) throws AtmBankingException {
        Account a = requireActive();
        requirePositive(amount);
        checkLimits(a, amount);
        if (amount > a.getBalance())
            throw new InsufficientFundsException("Insufficient funds. Balance: $" + a.getBalance());
        a.setBalance(a.getBalance() - amount);
        a.appendTransaction(new Transaction(a.getAccountNumber(), TransactionType.WITHDRAWAL, amount));
        a.registerWithdrawalForDailyLimit(LocalDate.now(), amount);
        return a.getBalance();
    }

    public double checkBalance() throws AtmBankingException {
        return requireActive().getBalance();
    }

    public void transfer(String destination, double amount) throws AtmBankingException {
        Account sender = requireActive();
        requirePositive(amount);
        String dest = requireNonBlank(destination, "Destination account is required.");
        if (!BankingPolicies.isRecognizedAccountFormat(dest))
            throw new AtmBankingException("Invalid destination account number.");
        if (dest.equals(sender.getAccountNumber()))
            throw new AtmBankingException("Cannot transfer to the same account.");

        Optional<Account> receiverOpt = repository.findByNumber(dest);
        if (receiverOpt.isEmpty())
            throw new AccountNotFoundException("Destination account not found.");
        Account receiver = receiverOpt.get();

        checkLimits(sender, amount);
        if (amount > sender.getBalance())
            throw new InsufficientFundsException("Insufficient funds. Balance: $" + sender.getBalance());

        sender.setBalance(sender.getBalance() - amount);
        receiver.setBalance(receiver.getBalance() + amount);
        sender.appendTransaction(new Transaction(sender.getAccountNumber(), TransactionType.TRANSFER_OUT, amount));
        receiver.appendTransaction(new Transaction(receiver.getAccountNumber(), TransactionType.TRANSFER_IN, amount));
        sender.registerWithdrawalForDailyLimit(LocalDate.now(), amount);
    }

    public void changePin(String oldPin, String newPin) throws AtmBankingException {
        Account a = requireActive();
        if (!a.getPin().equals(oldPin))
            throw new InvalidCredentialsException("Current PIN is incorrect.");
        if (!BankingPolicies.isValidPinFormat(newPin))
            throw new AtmBankingException("New PIN must be 4-6 digits.");
        a.setPin(newPin);
        a.appendTransaction(new Transaction(a.getAccountNumber(), TransactionType.PIN_CHANGED, 0));
    }

    public List<Transaction> activeAccountLedger() throws AtmBankingException {
        return new ArrayList<>(requireActive().getTransactionsView());
    }

    public void loadFromDisk() throws AtmBankingException {
        repository.loadFromDisk();
        logout();
    }

    public void flushToDisk() throws AtmBankingException {
        repository.flushToDisk();
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private Account requireActive() throws AtmBankingException {
        if (activeAccountNumber == null)
            throw new NotAuthenticatedException("Please login first.");
        Optional<Account> opt = repository.findByNumber(activeAccountNumber);
        if (opt.isEmpty())
            throw new AccountNotFoundException("Session account not found.");
        return opt.get();
    }

    private static String requireNonBlank(String s, String msg) throws AtmBankingException {
        if (s == null || s.isBlank()) throw new AtmBankingException(msg);
        return s.trim();
    }

    private static void requirePositive(double amount) throws AtmBankingException {
        if (amount <= 0) throw new AtmBankingException("Amount must be greater than zero.");
    }

    private void checkLimits(Account a, double amount) throws AtmBankingException {
        if (amount > BankingPolicies.MAX_SINGLE_WITHDRAWAL)
            throw new AtmBankingException("Single transaction limit is $" + BankingPolicies.MAX_SINGLE_WITHDRAWAL);
        double projected = a.amountWithdrawnSameDay(LocalDate.now()) + amount;
        if (projected > BankingPolicies.MAX_DAILY_WITHDRAWALS + 1e-6)
            throw new DailyWithdrawalLimitException("Daily limit of $" + BankingPolicies.MAX_DAILY_WITHDRAWALS + " exceeded.");
    }
}
