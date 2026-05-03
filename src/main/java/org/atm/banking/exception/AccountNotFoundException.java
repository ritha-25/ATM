package org.atm.banking.exception;

public class AccountNotFoundException extends AtmBankingException {
    public AccountNotFoundException(String message) {
        super(message);
    }
}
