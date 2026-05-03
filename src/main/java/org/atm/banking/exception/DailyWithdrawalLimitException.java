package org.atm.banking.exception;

public class DailyWithdrawalLimitException extends AtmBankingException {
    public DailyWithdrawalLimitException(String message) {
        super(message);
    }
}
