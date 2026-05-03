package org.atm.banking.ui.fx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.atm.banking.application.AtmInteractionService;
import org.atm.banking.persistence.CsvAccountRepository;

import java.nio.file.Path;

public final class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        CsvAccountRepository repo = new CsvAccountRepository(Path.of("data"));
        AtmInteractionService service = new AtmInteractionService(repo);

        AtmView view = new AtmView();
        new AtmController(service, view);

        Scene scene = new Scene(view, 1060, 720);
        scene.getStylesheets().add(
                getClass().getResource("/org/atm/banking/ui/atm.css").toExternalForm());

        stage.setScene(scene);
        stage.setTitle("SecureBank ATM");
        stage.setResizable(false);
        stage.show();
    }

}
