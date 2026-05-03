package org.atm.banking.exception;

public class DuplicateAccountException extends AtmBankingException {
    public DuplicateAccountException(String message) {
        super(message);
    }
}
