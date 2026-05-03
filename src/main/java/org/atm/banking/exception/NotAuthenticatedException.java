package org.atm.banking.exception;

public class NotAuthenticatedException extends AtmBankingException {
    public NotAuthenticatedException(String message) {
        super(message);
    }
}
