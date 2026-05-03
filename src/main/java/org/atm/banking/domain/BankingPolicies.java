package org.atm.banking.domain;

public final class BankingPolicies {
    public static final double MAX_SINGLE_WITHDRAWAL = 1_000.00;
    public static final double MAX_DAILY_WITHDRAWALS = 3_000.00;
    private static final int MIN_PIN_DIGITS = 4;
    private static final int MAX_PIN_DIGITS = 6;

    private BankingPolicies() {
    }

    public static boolean isValidPinFormat(String pin) {
        return pin != null && pin.matches("\\d{%d,%d}".formatted(MIN_PIN_DIGITS, MAX_PIN_DIGITS));
    }

    public static boolean isRecognizedAccountFormat(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return false;
        }
        return accountNumber.matches("\\d{5,14}");
    }
}
