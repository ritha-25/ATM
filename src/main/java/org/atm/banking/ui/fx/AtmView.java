package org.atm.banking.ui.fx;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import org.atm.banking.domain.Transaction;

public class AtmView extends BorderPane {

    final TextField     accountInput   = input("Your 10-digit account number");
    final PasswordField pinInput       = pinField("PIN  (4–6 digits)");
    final TextField     amountInput    = input("Amount  e.g.  500.00");
    final TextField     destInput      = input("Destination Account Number");
    final PasswordField newPinInput    = pinField("New PIN  (4–6 digits)");

    final Button btnLogin      = actionBtn("LOGIN");
    final Button btnRegister   = actionBtn("CREATE ACCOUNT");
    final Button btnDeposit    = actionBtn("DEPOSIT");
    final Button btnWithdraw   = actionBtn("WITHDRAW");
    final Button btnTransfer   = actionBtn("TRANSFER");
    final Button btnBalance    = actionBtn("CHECK BALANCE");
    final Button btnChangePin  = actionBtn("CHANGE PIN");
    final Button btnSave       = actionBtn("SAVE TO FILE");
    final Button btnLoad       = actionBtn("LOAD FROM FILE");
    final Button btnLogout     = actionBtn("LOGOUT");
    final Button navLogin      = navBtn("Login");
    final Button navRegister   = navBtn("Register");
    final Button navDeposit    = navBtn("Deposit");
    final Button navWithdraw   = navBtn("Withdraw");
    final Button navTransfer   = navBtn("Transfer");
    final Button navBalance    = navBtn("Balance");
    final Button navChangePin  = navBtn("Change PIN");
    final Button navHistory    = navBtn("History");
    final Button navSave       = navBtn("Save");
    final Button navLoad       = navBtn("Load");
    final Button navLogout     = navBtn("Logout");

    private final Label statusLabel  = new Label("Welcome to SecureBank ATM");
    private final Label balanceChip  = new Label("Not logged in");
    private final Label accountChip  = new Label("—");
    private final Label timeLabel    = new Label("");
    final Label generatedAccLabel    = new Label("");   // shows auto-generated account number

    private final TableView<Transaction> txTable = buildTxTable();

    private final StackPane screen = new StackPane();

    public AtmView() {
        getStyleClass().add("atm-root");
        setLeft(buildSidebar());
        setCenter(screen);
        setBottom(buildStatusBar());
        startClock();
    }


    private VBox buildSidebar() {

        StackPane icon = new StackPane();
        Rectangle body = new Rectangle(40, 40);
        body.getStyleClass().add("icon-body");
        body.setArcWidth(8); body.setArcHeight(8);
        Rectangle slot = new Rectangle(26, 4);
        slot.getStyleClass().add("icon-slot");
        slot.setTranslateY(7);
        Circle dot = new Circle(3);
        dot.getStyleClass().add("icon-dot");
        dot.setTranslateY(-6);
        icon.getChildren().addAll(body, slot, dot);

        Label bankName = new Label("SecureBank");
        bankName.getStyleClass().add("bank-name");
        Label atmTag = new Label("ATM");
        atmTag.getStyleClass().add("atm-tag");

        VBox logoBox = new VBox(5, icon, bankName, atmTag);
        logoBox.getStyleClass().add("logo-box");
        logoBox.setAlignment(Pos.CENTER);

        accountChip.getStyleClass().add("chip-account");
        balanceChip.getStyleClass().add("chip-balance");
        VBox chipBox = new VBox(3, accountChip, balanceChip);
        chipBox.getStyleClass().add("chip-box");

        VBox nav = new VBox(1,
            section("ACCOUNT"),   navLogin, navRegister,
            section("TRANSACTIONS"), navDeposit, navWithdraw, navTransfer, navBalance,
            section("SETTINGS"),  navChangePin, navHistory,
            section("DATA"),      navSave, navLoad,
            section("SESSION"),   navLogout
        );
        nav.getStyleClass().add("nav-box");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        timeLabel.getStyleClass().add("time-label");

        VBox sidebar = new VBox(logoBox, chipBox, nav, spacer, timeLabel);
        sidebar.getStyleClass().add("sidebar");
        return sidebar;
    }

    private Label section(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("nav-section");
        return l;
    }

    private HBox buildStatusBar() {
        statusLabel.getStyleClass().add("status-text");
        HBox bar = new HBox(statusLabel);
        bar.getStyleClass().add("status-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    void showWelcome() {
        Label title = title("Welcome to SecureBank");
        Label sub   = sub("Select an option from the sidebar to get started");

        VBox loginTile    = tile("Login",    "Access your account");
        VBox registerTile = tile("Register", "Open a new account");
        VBox loadTile     = tile("Load",     "Restore saved data");

        loginTile.setOnMouseClicked(e    -> navLogin.fire());
        registerTile.setOnMouseClicked(e -> navRegister.fire());
        loadTile.setOnMouseClicked(e     -> navLoad.fire());

        HBox tiles = new HBox(16, loginTile, registerTile, loadTile);
        tiles.setAlignment(Pos.CENTER);

        VBox box = new VBox(20, title, sub, tiles);
        box.setAlignment(Pos.CENTER);
        show(wrap(box));
    }

    void showLogin() {
        accountInput.clear();
        pinInput.clear();
        Label title = title("Account Login");
        Label sub   = sub("Enter your account number and PIN");

        VBox form = form(
            row("Account Number", accountInput),
            row("PIN", pinInput)
        );

        HBox actions = new HBox(12, link("New here? Register →", navRegister), grow(), btnLogin);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(22, title, sub, form, actions);
        box.setAlignment(Pos.TOP_CENTER);
        box.setMaxWidth(460);
        show(wrap(box));
    }

    void showRegister() {
        pinInput.clear();
        amountInput.clear();
        generatedAccLabel.setText("Generating...");

        Label title = title("Create Account");
        Label sub   = sub("Your account number is auto-generated. Choose a PIN to continue.");

        Label accNumLabel = new Label("Your Account Number:");
        accNumLabel.getStyleClass().add("form-label");
        generatedAccLabel.getStyleClass().add("generated-acc");

        HBox accRow = new HBox(12, accNumLabel, generatedAccLabel);
        accRow.setAlignment(Pos.CENTER_LEFT);

        VBox form = form(
            accRow,
            row("PIN  (4–6 digits)", pinInput),
            row("Opening Balance  (optional)", amountInput)
        );

        HBox actions = new HBox(12, link("Already registered? Login →", navLogin), grow(), btnRegister);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(22, title, sub, form, actions);
        box.setAlignment(Pos.TOP_CENTER);
        box.setMaxWidth(460);
        show(wrap(box));
    }

    void showDeposit() {
        amountInput.clear();
        Label title = title("Deposit Funds");
        Label sub   = sub("Enter the amount you wish to deposit");
        VBox form = form(row("Amount ($)", amountInput));
        show(wrap(card(title, sub, form, right(btnDeposit))));
    }

    void showWithdraw() {
        amountInput.clear();
        Label title = title("Withdraw Funds");
        Label sub   = sub("Max per transaction: $1,000  |  Max per day: $3,000");
        VBox form = form(row("Amount ($)", amountInput));
        show(wrap(card(title, sub, form, right(btnWithdraw))));
    }

    void showTransfer() {
        amountInput.clear();
        destInput.clear();
        Label title = title("Transfer Funds");
        Label sub   = sub("Send money to another SecureBank account");
        VBox form = form(
            row("Destination Account", destInput),
            row("Amount ($)", amountInput)
        );
        show(wrap(card(title, sub, form, right(btnTransfer))));
    }

    void showBalance(double balance) {
        Label title  = title("Account Balance");
        Label amount = new Label(String.format("$%.2f", balance));
        amount.getStyleClass().add("balance-big");
        Label acct = new Label("Account  " + accountChip.getText());
        acct.getStyleClass().add("balance-acct");
        VBox box = new VBox(14, title, amount, acct, right(btnBalance));
        box.setAlignment(Pos.CENTER);
        box.setMaxWidth(460);
        show(wrap(box));
    }

    void showChangePin() {
        pinInput.clear();
        newPinInput.clear();
        Label title = title("Change PIN");
        Label sub   = sub("Enter your current PIN, then choose a new one");
        VBox form = form(
            row("Current PIN", pinInput),
            row("New PIN",     newPinInput)
        );
        show(wrap(card(title, sub, form, right(btnChangePin))));
    }

    void showHistory(ObservableList<Transaction> items) {
        txTable.setItems(items);
        txTable.setPrefHeight(400);
        Label title = title("Transaction History");
        VBox box = new VBox(16, title, txTable);
        box.setAlignment(Pos.TOP_CENTER);
        box.setMaxWidth(720);
        show(wrap(box));
    }

    void showSave() {
        Label title = title("Save Data");
        Label sub   = sub("Writes all accounts and transactions to data/accounts.csv and data/transactions.csv");
        sub.setWrapText(true); sub.setMaxWidth(380);
        show(wrap(card(title, sub, right(btnSave))));
    }

    void showLoad() {
        Label title = title("Load Data");
        Label sub   = sub("Reloads all accounts and transactions from the CSV files in the data/ folder");
        sub.setWrapText(true); sub.setMaxWidth(380);
        show(wrap(card(title, sub, right(btnLoad))));
    }

    void showLocked(String accountNumber) {
        Label title = title("Account Locked");
        title.getStyleClass().add("locked-title");
        Label msg = sub("Account " + accountNumber + " has been locked after 3 failed PIN attempts.\nPlease contact your bank to unlock.");
        msg.setWrapText(true);
        msg.setMaxWidth(380);
        VBox box = new VBox(16, title, msg);
        box.setAlignment(Pos.CENTER);
        show(wrap(box));
    }


    private void show(Node panel) { screen.getChildren().setAll(panel); }

    private StackPane wrap(Node content) {
        StackPane p = new StackPane(content);
        p.getStyleClass().add("panel-wrap");
        return p;
    }

    private VBox card(Node... children) {
        VBox c = new VBox(18);
        c.getStyleClass().add("card");
        c.setMaxWidth(460);
        c.getChildren().addAll(children);
        return c;
    }

    private Label title(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("screen-title");
        return l;
    }

    private Label sub(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("screen-sub");
        return l;
    }

    private VBox form(Node... rows) {
        VBox f = new VBox(14);
        f.getStyleClass().add("form-box");
        f.getChildren().addAll(rows);
        return f;
    }

    private HBox row(String labelText, Control field) {
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("form-label");
        lbl.setPrefWidth(220);
        field.setPrefWidth(220);
        field.getStyleClass().add("form-input");
        HBox r = new HBox(14, lbl, field);
        r.setAlignment(Pos.CENTER_LEFT);
        return r;
    }

    private HBox right(Button btn) {
        HBox h = new HBox(btn);
        h.setAlignment(Pos.CENTER_RIGHT);
        h.setPadding(new Insets(6, 0, 0, 0));
        return h;
    }

    private VBox tile(String titleText, String subText) {
        Label t = new Label(titleText);
        t.getStyleClass().add("tile-title");
        Label s = new Label(subText);
        s.getStyleClass().add("tile-sub");
        VBox tile = new VBox(5, t, s);
        tile.getStyleClass().add("tile");
        tile.setAlignment(Pos.CENTER);
        tile.setPrefSize(155, 80);
        return tile;
    }

    private Button link(String text, Button target) {
        Button b = new Button(text);
        b.getStyleClass().add("link-btn");
        b.setOnAction(e -> target.fire());
        return b;
    }

    private Region grow() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    private TableView<Transaction> buildTxTable() {
        TableView<Transaction> t = new TableView<>();
        t.getStyleClass().add("tx-table");
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        t.getColumns().addAll(
            col("Account",     "accountNumber",    130),
            col("Type",        "type",             120),
            col("Amount ($)",  "amount",           100),
            col("Date & Time", "formattedDateTime", 180)
        );
        return t;
    }

    private <T> TableColumn<Transaction, T> col(String title, String prop, double w) {
        TableColumn<Transaction, T> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(w);
        return c;
    }


    private static Button actionBtn(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("action-btn");
        return b;
    }

    private static Button navBtn(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("nav-btn");
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }

    private static TextField input(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        return f;
    }

    private static PasswordField pinField(String prompt) {
        PasswordField f = new PasswordField();
        f.setPromptText(prompt);
        return f;
    }

    private void startClock() {
        javafx.animation.Timeline clock = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e ->
                timeLabel.setText(java.time.LocalTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))))
        );
        clock.setCycleCount(javafx.animation.Animation.INDEFINITE);
        clock.play();
    }

    void setStatus(String msg)       { statusLabel.setText(msg); }
    void setBalanceChip(String text) { balanceChip.setText(text); }
    void setAccountChip(String text) { accountChip.setText(text); }

    String getAccount() { return accountInput.getText().trim(); }
    String getPin()     { return pinInput.getText(); }
    String getAmount()  { return amountInput.getText().trim(); }
    String getDest()    { return destInput.getText().trim(); }
    String getNewPin()  { return newPinInput.getText(); }

    TableView<Transaction> getTxTable() { return txTable; }

    void setNavLoggedIn(boolean in) {
        navDeposit.setDisable(!in);
        navWithdraw.setDisable(!in);
        navTransfer.setDisable(!in);
        navBalance.setDisable(!in);
        navChangePin.setDisable(!in);
        navHistory.setDisable(!in);
        navLogout.setDisable(!in);
    }
}
