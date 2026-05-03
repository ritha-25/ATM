package org.atm.banking.exception;

public class InsufficientFundsException extends AtmBankingException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
