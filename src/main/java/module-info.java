module com.tictactoe.tictactoemanager {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.tictactoe.tictactoemanager to javafx.fxml;
    exports com.tictactoe.tictactoemanager;
}