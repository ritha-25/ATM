package org.atm.banking.ui.fx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import org.atm.banking.domain.Transaction;

public class AtmOperatorView extends VBox {
    private final TextField accountField;
    private final PasswordField pinField;
    private final TextField amountField;
    private final TextField transferTargetField;
    private final PasswordField newPinField;
    private final Button loginButton;
    private final Button logoutButton;
    private final Button registerButton;
    private final Button depositButton;
    private final Button withdrawButton;
    private final Button balanceButton;
    private final Button transferButton;
    private final Button changePinButton;
    private final Button loadButton;
    private final Button saveButton;
    private final Label noticeLabel;
    private final TableView<Transaction> transactionTable;

    public AtmOperatorView() {
        setSpacing(12);
        setPadding(new Insets(22));
        setAlignment(Pos.TOP_CENTER);

        Label heading = BankingUiKit.title("ATM banking console (temporary UI)");

        accountField = BankingUiKit.textField("Account number (5-14 digits)");
        pinField = new PasswordField();
        pinField.setPromptText("PIN (4-6 digits)");
        pinField.setPrefWidth(200);

        amountField = BankingUiKit.textField("Amount / opening balance");

        transferTargetField = BankingUiKit.textField("Transfer destination account");

        newPinField = new PasswordField();
        newPinField.setPromptText("New PIN confirmation");
        newPinField.setPrefWidth(240);

        loginButton = BankingUiKit.actionButton("Login");
        logoutButton = BankingUiKit.actionButton("Logout");
        registerButton = BankingUiKit.actionButton("Register");
        depositButton = BankingUiKit.actionButton("Deposit");
        withdrawButton = BankingUiKit.actionButton("Withdraw");
        balanceButton = BankingUiKit.actionButton("Balance");
        transferButton = BankingUiKit.actionButton("Transfer");
        changePinButton = BankingUiKit.actionButton("Change PIN");
        loadButton = BankingUiKit.actionButton("Load data");
        saveButton = BankingUiKit.actionButton("Save data");

        noticeLabel = new Label("Register or load data, then authenticate to operate the session.");
        noticeLabel.setPrefWidth(760);
        noticeLabel.setWrapText(true);

        transactionTable = buildLedgerTable();

        var credentialsRow =
                BankingUiKit.row(
                        10,
                        accountField,
                        pinField,
                        transferTargetField);
        credentialsRow.setAlignment(Pos.CENTER);

        var sizingRow =
                BankingUiKit.row(
                        10,
                        amountField,
                        newPinField);
        sizingRow.setAlignment(Pos.CENTER);

        var authenticationRow =
                BankingUiKit.row(10, loginButton, logoutButton, registerButton, loadButton, saveButton);
        authenticationRow.setAlignment(Pos.CENTER);

        var operationsRow =
                BankingUiKit.row(
                        10,
                        depositButton,
                        withdrawButton,
                        balanceButton,
                        transferButton,
                        changePinButton);
        operationsRow.setAlignment(Pos.CENTER);

        getChildren()
                .addAll(
                        heading,
                        credentialsRow,
                        sizingRow,
                        authenticationRow,
                        operationsRow,
                        transactionTable,
                        noticeLabel);
    }

    private TableView<Transaction> buildLedgerTable() {
        TableView<Transaction> table = new TableView<>();

        TableColumn<Transaction, String> accountColumn = new TableColumn<>("Account");
        accountColumn.setCellValueFactory(new PropertyValueFactory<>("accountNumber"));
        accountColumn.setPrefWidth(170);

        TableColumn<Transaction, String> typeColumn = new TableColumn<>("Transaction");
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeColumn.setPrefWidth(170);

        TableColumn<Transaction, Double> amountColumn = new TableColumn<>("Amount");
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amountColumn.setPrefWidth(120);

        TableColumn<Transaction, String> dateColumn = new TableColumn<>("Timestamp");
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("formattedDateTime"));
        dateColumn.setPrefWidth(220);

        table.getColumns().addAll(accountColumn, typeColumn, amountColumn, dateColumn);
        table.setPrefHeight(320);
        return table;
    }

    public String getAccountInput() {
        return accountField.getText().trim();
    }

    public String getPinInput() {
        return pinField.getText();
    }

    public String readAmountLiteral() {
        return amountField.getText().trim();
    }

    public void clearNewCredentialInputs() {
        newPinField.clear();
    }

    public String getTransferTarget() {
        return transferTargetField.getText().trim();
    }

    public String getNewPinInput() {
        return newPinField.getText();
    }

    public Button getLoginButton() {
        return loginButton;
    }

    public Button getLogoutButton() {
        return logoutButton;
    }

    public Button getRegisterButton() {
        return registerButton;
    }

    public Button getDepositButton() {
        return depositButton;
    }

    public Button getWithdrawButton() {
        return withdrawButton;
    }

    public Button getBalanceButton() {
        return balanceButton;
    }

    public Button getTransferButton() {
        return transferButton;
    }

    public Button getChangePinButton() {
        return changePinButton;
    }

    public Button getLoadButton() {
        return loadButton;
    }

    public Button getSaveButton() {
        return saveButton;
    }

    public Label getNoticeLabel() {
        return noticeLabel;
    }

    public TableView<Transaction> getTransactionTable() {
        return transactionTable;
    }
}
