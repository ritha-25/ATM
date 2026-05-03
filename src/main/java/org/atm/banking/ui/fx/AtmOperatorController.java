package org.atm.banking.ui.fx;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import org.atm.banking.application.AtmInteractionService;
import org.atm.banking.exception.AtmBankingException;

import java.util.Objects;

public class AtmOperatorController {
    private final AtmInteractionService orchestrator;
    private final AtmOperatorView view;

    public AtmOperatorController(AtmInteractionService orchestrator, AtmOperatorView view) {
        this.orchestrator = Objects.requireNonNull(orchestrator);
        this.view = Objects.requireNonNull(view);
        wireInteractions();
        refreshLedgerTable();
        renderSessionBanner();
    }

    private void wireInteractions() {
        view.getLoginButton().setOnAction(event -> authenticate());
        view.getLogoutButton().setOnAction(event -> terminateSession());

        view.getRegisterButton().setOnAction(event -> registerProfile());
        view.getDepositButton().setOnAction(event -> depositFunds());
        view.getWithdrawButton().setOnAction(event -> withdrawFunds());
        view.getBalanceButton().setOnAction(event -> printBalanceSnapshot());
        view.getTransferButton().setOnAction(event -> initiateTransfer());

        view.getChangePinButton().setOnAction(event -> rotatePinSecrets());
        view.getLoadButton().setOnAction(event -> reloadSnapshots());
        view.getSaveButton().setOnAction(event -> persistSnapshots());
    }

    private void authenticate() {
        try {
            orchestrator.login(view.getAccountInput(), view.getPinInput());
            publishNotice("Authenticated as " + orchestrator.currentAccountNumber());
            refreshLedgerTable();
        } catch (AtmBankingException ex) {
            publishNotice(ex.getMessage());
        }
    }

    private void terminateSession() {
        orchestrator.logout();
        clearLedgerIfNeeded();
        publishNotice("Session cleared.");
    }

    private void registerProfile() {
        Double seedBalanceOpt = parseOptionalOpeningBalance();
        if (seedBalanceOpt == null) {
            return;
        }
        try {
            orchestrator.registerNewAccount(
                    view.getAccountInput(),
                    view.getPinInput(),
                    seedBalanceOpt);
            publishNotice(
                    "Account "
                            + view.getAccountInput()
                            + " registered. Deposit additional funds after authenticating.");
        } catch (AtmBankingException ex) {
            publishNotice(explanation(ex));
        }
    }

    private void depositFunds() {
        Double amount = coerceAmount(view.readAmountLiteral());
        if (amount == null) {
            return;
        }

        try {
            double refreshedBalance = orchestrator.deposit(amount);
            publishNotice("Deposit posted. Balance: " + refreshedBalance);
            refreshLedgerTable();
        } catch (AtmBankingException ex) {
            publishNotice(ex.getMessage());
        }
    }

    private void withdrawFunds() {
        Double amount = coerceAmount(view.readAmountLiteral());
        if (amount == null) {
            return;
        }
        try {
            double refreshedBalance = orchestrator.withdraw(amount);
            publishNotice("Funds dispensed. Balance: " + refreshedBalance);
            refreshLedgerTable();
        } catch (AtmBankingException ex) {
            publishNotice(ex.getMessage());
        }
    }

    private void printBalanceSnapshot() {
        try {
            double balance = orchestrator.checkBalance();
            publishNotice("Reported balance: " + balance);
            refreshLedgerTable();
        } catch (AtmBankingException ex) {
            publishNotice(ex.getMessage());
        }
    }

    private void initiateTransfer() {
        Double amount = coerceAmount(view.readAmountLiteral());
        if (amount == null) {
            return;
        }
        try {
            orchestrator.transfer(view.getTransferTarget(), amount);
            publishNotice(
                    "Transfer completed toward "
                            + view.getTransferTarget()
                            + " for "
                            + amount);
            refreshLedgerTable();
        } catch (AtmBankingException ex) {
            publishNotice(ex.getMessage());
        }
    }

    private void rotatePinSecrets() {
        try {
            orchestrator.changePin(view.getPinInput(), view.getNewPinInput());
            publishNotice("PIN updated. Memorize your new credential.");
            view.clearNewCredentialInputs();
        } catch (AtmBankingException ex) {
            publishNotice(ex.getMessage());
        }
    }

    private void reloadSnapshots() {
        try {
            orchestrator.loadFromDisk();
            terminateSessionQuietlyForReload();
            publishNotice("Repository rehydrated from disk.");
        } catch (AtmBankingException ex) {
            publishNotice(ex.getMessage());
        }
    }

    private void terminateSessionQuietlyForReload() {
        orchestrator.logout();
        clearLedgerIfNeeded();
    }

    private void persistSnapshots() {
        try {
            orchestrator.flushToDisk();
            publishNotice("Snapshots saved locally.");
        } catch (AtmBankingException ex) {
            publishNotice(ex.getMessage());
        }
    }

    private void refreshLedgerTable() {
        Platform.runLater(
                () -> {
                    try {
                        if (!orchestrator.isAuthenticated()) {
                            view.getTransactionTable().setItems(FXCollections.observableArrayList());
                            return;
                        }
                        view.getTransactionTable()
                                .setItems(
                                        FXCollections.observableArrayList(
                                                orchestrator.activeAccountLedger()));
                    } catch (AtmBankingException ex) {
                        view.getTransactionTable().setItems(FXCollections.observableArrayList());
                    }
                    renderSessionBanner();
                });
    }

    private void renderSessionBanner() {
        Platform.runLater(
                () -> {
                    boolean activeSession = orchestrator.isAuthenticated();
                    view.getDepositButton().setDisable(!activeSession);
                    view.getWithdrawButton().setDisable(!activeSession);
                    view.getBalanceButton().setDisable(!activeSession);
                    view.getTransferButton().setDisable(!activeSession);
                    view.getChangePinButton().setDisable(!activeSession);
                });
    }

    private void clearLedgerIfNeeded() {
        refreshLedgerTable();
    }

    private Double coerceAmount(String literal) {
        try {
            return Double.parseDouble(literal);
        } catch (NumberFormatException malformed) {
            publishNotice("Amount field must contain a numeric value.");
            return null;
        }
    }

    private Double parseOptionalOpeningBalance() {
        String literal = view.readAmountLiteral();
        if (literal.isBlank()) {
            return 0d;
        }
        return coerceAmount(literal);
    }

    private void publishNotice(String text) {
        Platform.runLater(() -> view.getNoticeLabel().setText(text));
    }

    private static String explanation(Throwable throwable) {
        if (throwable instanceof AtmBankingException bankingIssue) {
            return bankingIssue.getMessage();
        }
        return throwable.getMessage();
    }
}
