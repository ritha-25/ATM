package org.atm.banking.ui.fx;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import org.atm.banking.application.AtmInteractionService;
import org.atm.banking.exception.AtmBankingException;

public class AtmController {

    private final AtmInteractionService service;
    private final AtmView view;

    private String pendingAccountNumber = "";

    public AtmController(AtmInteractionService service, AtmView view) {
        this.service = service;
        this.view = view;
        wireNav();
        wireActions();
        loadOnStartup();
        view.setNavLoggedIn(false);
        view.showWelcome();
    }


    private void wireNav() {
        view.navLogin.setOnAction(e     -> view.showLogin());
        view.navRegister.setOnAction(e  -> showRegister());
        view.navDeposit.setOnAction(e   -> { if (needsLogin()) return; view.showDeposit(); });
        view.navWithdraw.setOnAction(e  -> { if (needsLogin()) return; view.showWithdraw(); });
        view.navTransfer.setOnAction(e  -> { if (needsLogin()) return; view.showTransfer(); });
        view.navBalance.setOnAction(e   -> { if (needsLogin()) return; doBalance(); });
        view.navChangePin.setOnAction(e -> { if (needsLogin()) return; view.showChangePin(); });
        view.navHistory.setOnAction(e   -> { if (needsLogin()) return; doHistory(); });
        view.navSave.setOnAction(e      -> view.showSave());
        view.navLoad.setOnAction(e      -> view.showLoad());
        view.navLogout.setOnAction(e    -> doLogout());
    }

    private void wireActions() {
        view.btnLogin.setOnAction(e     -> doLogin());
        view.btnRegister.setOnAction(e  -> doRegister());
        view.btnDeposit.setOnAction(e   -> doDeposit());
        view.btnWithdraw.setOnAction(e  -> doWithdraw());
        view.btnTransfer.setOnAction(e  -> doTransfer());
        view.btnBalance.setOnAction(e   -> doBalance());
        view.btnChangePin.setOnAction(e -> doChangePin());
        view.btnSave.setOnAction(e      -> doSave());
        view.btnLoad.setOnAction(e      -> doLoad());
        view.btnLogout.setOnAction(e    -> doLogout());
    }
    private void showRegister() {
        pendingAccountNumber = service.generateAccountNumber();
        view.generatedAccLabel.setText(pendingAccountNumber);
        view.showRegister();
    }

    private void doLogin() {
        String account = view.getAccount();
        String pin     = view.getPin();

        if (account.isBlank()) { status("Please enter your account number."); return; }
        if (pin.isBlank())     { status("Please enter your PIN."); return; }

        try {
            service.login(account, pin);
            view.setAccountChip(service.currentAccountNumber());
            view.setNavLoggedIn(true);
            doBalance();   // navigate straight to balance screen
            status("Welcome! Logged in as account " + service.currentAccountNumber());
        } catch (AtmBankingException ex) {
            status(ex.getMessage());
            if (service.isAccountLocked(account)) {
                view.showLocked(account);
            }
        }
    }

    private void doRegister() {
        String pin     = view.getPin();
        String amtText = view.getAmount();
        double opening = amtText.isBlank() ? 0.0 : toDouble(amtText, -1);

        if (pendingAccountNumber.isBlank()) {
            status("No account number generated. Please go back and try again.");
            return;
        }
        if (pin.isBlank()) { status("Please enter a PIN."); return; }
        if (opening < 0)   { status("Opening balance must be a valid number."); return; }

        try {
            service.registerNewAccount(pendingAccountNumber, pin, opening);
            autoSave();
            String acc = pendingAccountNumber;
            pendingAccountNumber = "";
            status("Account " + acc + " created! Save this number: " + acc + "  — then login.");

            Platform.runLater(() -> {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.INFORMATION);
                alert.setTitle("Account Created");
                alert.setHeaderText("Your account has been created!");
                alert.setContentText(
                    "Account Number: " + acc + "\n\n" +
                    "IMPORTANT: Save this number — you need it to login.\n" +
                    "Opening Balance: $" + (amtText.isBlank() ? "0.00" : amtText));
                alert.showAndWait();
                view.showLogin();
            });
        } catch (AtmBankingException ex) {
            status("Registration failed: " + ex.getMessage());
        }
    }

    private void doDeposit() {
        Double amount = requireAmount();
        if (amount == null) return;
        try {
            double balance = service.deposit(amount);
            autoSave();
            view.amountInput.clear();
            refreshBalance(balance);
            status("Deposit successful. New balance: $" + fmt(balance));
            view.showBalance(balance);
        } catch (AtmBankingException ex) {
            status("Deposit failed: " + ex.getMessage());
        }
    }

    private void doWithdraw() {
        Double amount = requireAmount();
        if (amount == null) return;
        try {
            double balance = service.withdraw(amount);
            autoSave();
            view.amountInput.clear();
            refreshBalance(balance);
            status("Withdrawal successful. New balance: $" + fmt(balance));
            view.showBalance(balance);
        } catch (AtmBankingException ex) {
            status("Withdrawal failed: " + ex.getMessage());
        }
    }

    private void doTransfer() {
        Double amount = requireAmount();
        if (amount == null) return;
        String dest = view.getDest();
        if (dest.isBlank()) { status("Please enter a destination account number."); return; }
        try {
            service.transfer(dest, amount);
            double balance = service.checkBalance();
            autoSave();
            view.amountInput.clear();
            view.destInput.clear();
            refreshBalance(balance);
            status("Transfer to " + dest + " successful. New balance: $" + fmt(balance));
            view.showBalance(balance);
        } catch (AtmBankingException ex) {
            status("Transfer failed: " + ex.getMessage());
        }
    }

    private void doBalance() {
        try {
            double balance = service.checkBalance();
            refreshBalance(balance);
            status("Balance: $" + fmt(balance));
            view.showBalance(balance);
        } catch (AtmBankingException ex) {
            status("Error: " + ex.getMessage());
        }
    }

    private void doChangePin() {
        String oldPin = view.getPin();
        String newPin = view.getNewPin();
        if (oldPin.isBlank() || newPin.isBlank()) { status("Both PIN fields are required."); return; }
        try {
            service.changePin(oldPin, newPin);
            autoSave();
            view.pinInput.clear();
            view.newPinInput.clear();
            status("PIN changed successfully.");
        } catch (AtmBankingException ex) {
            status("PIN change failed: " + ex.getMessage());
        }
    }

    private void doHistory() {
        try {
            var list = service.activeAccountLedger();
            Platform.runLater(() ->
                view.showHistory(FXCollections.observableArrayList(list)));
        } catch (AtmBankingException ex) {
            status("Error: " + ex.getMessage());
        }
    }

    private void doSave() {
        try {
            service.flushToDisk();
            status("Data saved to file successfully.");
        } catch (AtmBankingException ex) {
            status("Save failed: " + ex.getMessage());
        }
    }

    private void doLoad() {
        try {
            service.loadFromDisk();
            view.setNavLoggedIn(false);
            view.setAccountChip("—");
            view.setBalanceChip("Not logged in");
            status("Data loaded. Please login.");
            view.showLogin();
        } catch (AtmBankingException ex) {
            status("Load failed: " + ex.getMessage());
        }
    }

    private void doLogout() {
        service.logout();
        view.setNavLoggedIn(false);
        view.setAccountChip("—");
        view.setBalanceChip("Not logged in");
        view.showLogin();
        status("Logged out. Goodbye!");
    }

    private void autoSave() {
        try { service.flushToDisk(); }
        catch (AtmBankingException ignored) {}
    }

    private void loadOnStartup() {
        try { service.loadFromDisk(); }
        catch (AtmBankingException ignored) {}
    }

    private boolean needsLogin() {
        if (!service.isAuthenticated()) {
            status("Please login first.");
            view.showLogin();
            return true;
        }
        return false;
    }

    private void refreshBalance(double balance) {
        view.setBalanceChip("$" + fmt(balance));
    }

    private void status(String msg) {
        Platform.runLater(() -> view.setStatus(msg));
    }

    private Double requireAmount() {
        String text = view.getAmount();
        if (text.isBlank()) { status("Please enter an amount."); return null; }
        try { return Double.parseDouble(text); }
        catch (NumberFormatException e) { status("Amount must be a number, e.g. 250.00"); return null; }
    }

    private double toDouble(String text, double fallback) {
        try { return Double.parseDouble(text); }
        catch (NumberFormatException e) { return fallback; }
    }

    private static String fmt(double v) { return String.format("%.2f", v); }
}
