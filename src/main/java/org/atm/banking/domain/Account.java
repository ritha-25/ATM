package org.atm.banking.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Account {
    private final String accountNumber;
    private String pin;
    private double balance;
    private final List<Transaction> transactions;
    private LocalDate withdrawalTotalDate;
    private double withdrawnSameDaySum;

    public Account(String accountNumber, String pin, double openingBalance) {
        this.accountNumber = accountNumber;
        this.pin = pin;
        this.balance = round(openingBalance);
        this.transactions = new ArrayList<>();
        transactions.add(new Transaction(accountNumber, TransactionType.ACCOUNT_OPENED, openingBalance));
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = round(balance);
    }

    public List<Transaction> getTransactionsView() {
        return Collections.unmodifiableList(transactions);
    }

    public void appendTransaction(Transaction transaction) {
        transactions.add(transaction);
    }

    public void clearLedger() {
        transactions.clear();
    }


    public double amountWithdrawnSameDay(LocalDate today) {
        if (withdrawalTotalDate == null || !withdrawalTotalDate.equals(today)) {
            withdrawnSameDaySum = 0;
            withdrawalTotalDate = today;
        }
        return withdrawnSameDaySum;
    }

    public void registerWithdrawalForDailyLimit(LocalDate today, double amount) {
        amountWithdrawnSameDay(today);
        withdrawnSameDaySum = round(withdrawnSameDaySum + amount);
    }

    public LocalDate getWithdrawalTotalDateForPersistence() {
        return withdrawalTotalDate;
    }

    public double getWithdrawnSameDaySumForPersistence() {
        return withdrawnSameDaySum;
    }


    public void restoreDailyWithdrawalTotals(LocalDate date, double amount) {
        this.withdrawalTotalDate = date;
        this.withdrawnSameDaySum = round(amount);
    }

    private static double round(double amount) {
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
