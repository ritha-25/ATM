package org.atm.banking.application;

import org.atm.banking.domain.BankingPolicies;
import org.atm.banking.domain.TransactionType;
import org.atm.banking.exception.AccountNotFoundException;
import org.atm.banking.exception.AtmBankingException;
import org.atm.banking.exception.DuplicateAccountException;
import org.atm.banking.exception.InsufficientFundsException;
import org.atm.banking.exception.InvalidCredentialsException;
import org.atm.banking.exception.NotAuthenticatedException;
import org.atm.banking.persistence.CsvAccountRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

class AtmInteractionServiceTest {

    @Test
    void happyPathFundsMovement(@TempDir Path workspace) throws Exception {
        CsvAccountRepository repository = bootstrapRepository(workspace);
        AtmInteractionService service = new AtmInteractionService(repository);

        service.registerNewAccount("5432109876", "1122", 200);
        service.registerNewAccount("1122334455", "5544", 50);

        service.login("5432109876", "1122");
        service.deposit(100);
        service.transfer("1122334455", 75);
        service.logout();

        service.login("1122334455", "5544");
        Assertions.assertEquals(125, service.checkBalance(), 1e-6);
        Assertions.assertTrue(
                service.activeAccountLedger().stream()
                        .anyMatch(
                                transaction ->
                                        transaction.getType() == TransactionType.TRANSFER_IN
                                                && Double.compare(transaction.getAmount(), 75d) == 0));
        service.logout();
    }

    @Test
    void rejectsDuplicateEnrollment(@TempDir Path workspace) throws Exception {
        CsvAccountRepository repository = bootstrapRepository(workspace);
        AtmInteractionService service = new AtmInteractionService(repository);

        service.registerNewAccount("1000005000", "1000", 0);
        Assertions.assertThrows(
                DuplicateAccountException.class,
                () -> service.registerNewAccount("1000005000", "2000", 0));
    }

    @Test
    void operationsRequireAuthenticatedSession(@TempDir Path workspace) throws Exception {
        CsvAccountRepository repository = bootstrapRepository(workspace);
        AtmInteractionService service = new AtmInteractionService(repository);

        service.registerNewAccount("2000000200", "2020", 50);
        Assertions.assertThrows(NotAuthenticatedException.class, () -> service.deposit(15));
        Assertions.assertThrows(
                AccountNotFoundException.class, () -> service.login("0000000011", "1000"));

        Assertions.assertThrows(
                InvalidCredentialsException.class,
                () -> service.login("2000000200", "999888"));
        Assertions.assertFalse(service.isAuthenticated());
        Assertions.assertThrows(NotAuthenticatedException.class, () -> service.deposit(5));
    }

    @Test
    void deniesWithdrawalsBeyondBalance(@TempDir Path workspace) throws Exception {
        CsvAccountRepository repository = bootstrapRepository(workspace);
        AtmInteractionService service = new AtmInteractionService(repository);

        service.registerNewAccount("3000000700", "7070", 40);
        service.login("3000000700", "7070");

        Assertions.assertThrows(
                InsufficientFundsException.class, () -> service.withdraw(100));
        Assertions.assertEquals(40, service.checkBalance(), 1e-6);
        service.logout();
    }

    @Test
    void enforcesPerTransactionWithdrawalCeiling(@TempDir Path workspace) throws Exception {
        CsvAccountRepository repository = bootstrapRepository(workspace);
        AtmInteractionService service = new AtmInteractionService(repository);

        service.registerNewAccount("4000000800", "8080", 5_000);
        service.login("4000000800", "8080");

        Assertions.assertThrows(
                AtmBankingException.class,
                () -> service.withdraw(BankingPolicies.MAX_SINGLE_WITHDRAWAL + 250));
        service.logout();
    }

    @Test
    void changePinFlows(@TempDir Path workspace) throws Exception {
        CsvAccountRepository repository = bootstrapRepository(workspace);
        AtmInteractionService service = new AtmInteractionService(repository);

        service.registerNewAccount("5000000990", "1010", 10);
        service.login("5000000990", "1010");

        Assertions.assertThrows(
                InvalidCredentialsException.class, () -> service.changePin("0000", "2020"));

        service.changePin("1010", "3030");
        service.logout();

        Assertions.assertThrows(
                InvalidCredentialsException.class, () -> service.login("5000000990", "1010"));
        service.login("5000000990", "3030");
        Assertions.assertEquals(10, service.checkBalance(), 1e-6);
        service.logout();
    }

    private static CsvAccountRepository bootstrapRepository(Path workspace) throws Exception {
        Files.createDirectories(workspace.resolve("sandbox"));
        return new CsvAccountRepository(workspace.resolve("sandbox"));
    }
}
