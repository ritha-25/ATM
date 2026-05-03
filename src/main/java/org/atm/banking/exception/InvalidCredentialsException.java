package org.atm.banking.exception;

public class InvalidCredentialsException extends AtmBankingException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
