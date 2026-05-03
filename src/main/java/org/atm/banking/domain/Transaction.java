package org.atm.banking.domain;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Transaction {
    private static final DateTimeFormatter STORAGE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String accountNumber;
    private final TransactionType type;
    private final double amount;
    private final LocalDateTime timestamp;

    public Transaction(String accountNumber, TransactionType type, double amount) {
        this(accountNumber, type, amount, LocalDateTime.now());
    }

    public Transaction(String accountNumber, TransactionType type, double amount, LocalDateTime timestamp) {
        this.accountNumber = accountNumber;
        this.type = type;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public TransactionType getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getFormattedDateTime() {
        return timestamp.format(STORAGE_FORMAT);
    }

    public static Transaction fromPersistedCsv(String accountNumber, TransactionType type, double amount,
                                                String persistedTimestamp) {
        LocalDateTime time = persistedTimestamp.isBlank()
                ? LocalDateTime.now()
                : LocalDateTime.parse(persistedTimestamp.trim(), STORAGE_FORMAT);
        return new Transaction(accountNumber, type, amount, time);
    }
}
