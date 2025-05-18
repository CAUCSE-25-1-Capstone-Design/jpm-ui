module jpm.ui {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.logging;


    opens jpm.ui to javafx.fxml;
    exports jpm.ui;
    exports jpm.ui.constants;
    opens jpm.ui.constants to javafx.fxml;
}