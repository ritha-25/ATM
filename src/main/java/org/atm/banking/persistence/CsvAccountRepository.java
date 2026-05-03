package org.atm.banking.persistence;

import org.atm.banking.domain.Account;
import org.atm.banking.domain.Transaction;
import org.atm.banking.domain.TransactionType;
import org.atm.banking.exception.AtmBankingException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CsvAccountRepository {
    private static final DateTimeFormatter DATE_STORAGE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final Map<String, Account> accounts = new LinkedHashMap<>();
    private final Path accountSnapshot;
    private final Path ledgerSnapshot;

    public CsvAccountRepository(Path dataFolder) throws AtmBankingException {
        this.accountSnapshot = dataFolder.resolve("accounts.csv");
        this.ledgerSnapshot = dataFolder.resolve("transactions.csv");
        ensureStorageExists(dataFolder);
    }

    public Optional<Account> findByNumber(String accountNumber) {
        return Optional.ofNullable(accounts.get(normalize(accountNumber)));
    }

    public boolean exists(String accountNumber) {
        return accounts.containsKey(normalize(accountNumber));
    }

    public void save(Account account) {
        accounts.put(normalize(account.getAccountNumber()), account);
    }

    public Collection<Account> allAccounts() {
        return List.copyOf(accounts.values());
    }

    public void loadFromDisk() throws AtmBankingException {
        accounts.clear();
        readBalances();
        readLedgerLines();
    }

    public void flushToDisk() throws AtmBankingException {
        writeBalances();
        writeLedger();
    }

    private static String normalize(String accountNumber) {
        return accountNumber == null ? "" : accountNumber.trim();
    }

    private void ensureStorageExists(Path dataFolder) throws AtmBankingException {
        try {
            Files.createDirectories(dataFolder);
            if (Files.notExists(accountSnapshot)) {
                Files.createFile(accountSnapshot);
            }
            if (Files.notExists(ledgerSnapshot)) {
                Files.createFile(ledgerSnapshot);
            }
        } catch (IOException e) {
            throw new AtmBankingException("Cannot initiate storage folders: " + e.getMessage());
        }
    }

    private void readBalances() throws AtmBankingException {
        try {
            List<String> lines = Files.readAllLines(accountSnapshot);
            for (String line : lines) {
                if (line.isBlank()) {
                    continue;
                }

                ImportedAccount.importFrom(line.strip()).ifPresent(snapshot -> {
                    Account account = new Account(snapshot.accountNumber(), snapshot.pin(), 0);
                    account.clearLedger();
                    account.setBalance(snapshot.balance());
                    account.restoreDailyWithdrawalTotals(snapshot.withdrawalDate(), snapshot.withdrawnSameDaySum());
                    accounts.put(snapshot.accountNumber(), account);
                });
            }
        } catch (IOException | NumberFormatException e) {
            throw new AtmBankingException("Unable to ingest account snapshot: " + e.getMessage());
        }
    }

    private void readLedgerLines() throws AtmBankingException {
        try {
            List<String> lines = Files.readAllLines(ledgerSnapshot);
            List<Transaction> queue = new ArrayList<>();
            for (String raw : lines) {
                if (raw.isBlank()) {
                    continue;
                }
                try {
                    String[] parts = raw.split(",", -1);
                    if (parts.length < 3) {
                        continue;
                    }
                    Account owner = accounts.get(normalize(parts[0]));
                    if (owner == null) {
                        continue;
                    }

                    TransactionType type = resolveTransactionType(parts[1].trim());
                    double amount = Double.parseDouble(parts[2].trim());
                    Transaction transaction =
                            parts.length >= 4
                                    ? Transaction.fromPersistedCsv(
                                            owner.getAccountNumber(), type, amount, parts[3])
                                    : new Transaction(owner.getAccountNumber(), type, amount);

                    queue.add(transaction);
                } catch (RuntimeException malformedEntry) {

                }
            }

            for (Account entry : accounts.values()) {
                entry.clearLedger();
            }
            for (Transaction transaction : queue) {
                Account owner = accounts.get(transaction.getAccountNumber());
                owner.appendTransaction(transaction);
            }

        } catch (IOException | IllegalArgumentException e) {
            throw new AtmBankingException("Unable to ingest ledger snapshot: " + e.getMessage());
        }
    }

    private void writeBalances() throws AtmBankingException {
        List<String> rows = new ArrayList<>();
        for (Account account : accounts.values()) {
            LocalDate limitDate = account.getWithdrawalTotalDateForPersistence();
            String persistedDate = limitDate == null ? "" : limitDate.format(DATE_STORAGE);

            rows.add(String.join(",",
                    account.getAccountNumber(),
                    account.getPin(),
                    Double.toString(account.getBalance()),
                    persistedDate,
                    Double.toString(account.getWithdrawnSameDaySumForPersistence())));
        }

        persistLines(accountSnapshot, rows);
    }

    private void writeLedger() throws AtmBankingException {
        List<String> rows = new ArrayList<>();
        for (Account account : accounts.values()) {
            for (Transaction transaction : account.getTransactionsView()) {
                rows.add(String.join(",",
                        transaction.getAccountNumber(),
                        transaction.getType().name(),
                        Double.toString(transaction.getAmount()),
                        transaction.getFormattedDateTime()));
            }
        }
        persistLines(ledgerSnapshot, rows);
    }

    private void persistLines(Path target, Collection<String> lines) throws AtmBankingException {
        try {
            Files.write(
                    target,
                    lines,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.CREATE);
        } catch (IOException ex) {
            throw new AtmBankingException("Unable to persist " + target.getFileName() + ": "
                    + ex.getMessage());
        }
    }

    private static TransactionType resolveTransactionType(String persistedToken) throws IllegalArgumentException {
        try {
            return TransactionType.valueOf(persistedToken);
        } catch (IllegalArgumentException primary) {
            return switch (persistedToken) {
                case "CREATE_ACCOUNT", "WITHDRAW" ->
                        migratedValue(persistedToken, primary);
                default -> throw primary;
            };
        }
    }

    private static TransactionType migratedValue(String token, IllegalArgumentException seed) {
        TransactionType migrated =
                switch (token) {
                    case "CREATE_ACCOUNT" -> TransactionType.ACCOUNT_OPENED;
                    case "WITHDRAW" -> TransactionType.WITHDRAWAL;
                    default -> null;
                };
        if (migrated != null) {
            return migrated;
        }
        throw seed;
    }

    private record ImportedAccount(
            String accountNumber,
            String pin,
            double balance,
            LocalDate withdrawalDate,
            double withdrawnSameDaySum) {

        private enum Format {
            MODERN_FULL,
            LEGACY_DUAL_COLUMN
        }

        private static Optional<ImportedAccount> importFrom(String line) {
            String[] parts = line.split(",", -1);

            Format format = decodeFormat(parts.length);
            if (format == null) {
                return Optional.empty();
            }

            try {
                if (format == Format.LEGACY_DUAL_COLUMN) {
                    String account = normalize(parts[0]);
                    double balanceAmount = Double.parseDouble(parts[1].trim());

                    String generatedPin = "1111";

                    return Optional.of(new ImportedAccount(
                            account,
                            generatedPin,
                            balanceAmount,
                            null,
                            0d));
                }

                String accountNumber = normalize(parts[0]);
                String pinDigits = parts[1].trim();
                double persistedBalance = Double.parseDouble(parts[2].trim());
                LocalDate limitDate =
                        parts[3].isBlank()
                                ? null
                                : LocalDate.parse(parts[3].trim(), DATE_STORAGE);
                double withdrawnTodaySum =
                        parts[4].isBlank() ? 0d : Double.parseDouble(parts[4].trim());

                return Optional.of(new ImportedAccount(
                        accountNumber,
                        pinDigits,
                        persistedBalance,
                        limitDate,
                        withdrawnTodaySum));
            } catch (Exception ex) {
                return Optional.empty();
            }
        }

        private static Format decodeFormat(int length) {
            if (length >= 5) {
                return Format.MODERN_FULL;
            }
            if (length == 2) {
                return Format.LEGACY_DUAL_COLUMN;
            }
            return null;
        }
    }
}
