import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        Label label = new Label("Welcome to 40dash Cashier System");
        StackPane root = new StackPane(label);
        Scene scene = new Scene(root, 600, 400);
        primaryStage.setScene(scene);
        primaryStage.setTitle("40dash - Cashier");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}