module jpm.ui {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.logging;


    opens jpm.ui to javafx.fxml;
    exports jpm.ui;
}