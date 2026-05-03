package org.atm.banking.ui.fx;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;

final class BankingUiKit {
    private BankingUiKit() {
    }

    static Label title(String text) {
        Label label = new Label(text);
        label.setFont(Font.font(20));
        return label;
    }

    static TextField textField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setPrefWidth(200);
        return field;
    }

    static Button actionButton(String text) {
        Button button = new Button(text);
        button.setPrefWidth(130);
        button.setPrefHeight(34);
        return button;
    }

    static HBox row(double spacing, Region... nodes) {
        HBox row = new HBox(spacing, nodes);
        row.setPadding(new Insets(4, 0, 4, 0));
        return row;
    }
}
