module org.jpm.jpmui {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.jpm.jpmui to javafx.fxml;
    exports org.jpm.jpmui;
}