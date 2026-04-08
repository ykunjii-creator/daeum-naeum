import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class IOSDashboardUICSS extends Application {

    @Override
    public void start(Stage stage) {

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(16);
        grid.setVgap(16);

        grid.add(heartRateCard(), 0, 0);
        grid.add(highlightHeartRateCard(), 1, 0);
        grid.add(pressureCard(), 2, 0);

        grid.add(medicationCard(), 0, 1);
        grid.add(bloodStatusCard(), 1, 1);

        Scene scene = new Scene(grid, 900, 420);
        scene.getStylesheets().add(
                getClass().getResource("style.css").toExternalForm()
        );

        stage.setTitle("iOS Health Dashboard (JavaFX)");
        stage.setScene(scene);
        stage.show();
    }

    // ---------------- Cards ----------------

    private VBox heartRateCard() {
        VBox card = baseCard();
        card.getChildren().addAll(
                title("Blood Count"),
                value("80–90"),
                sub("bpm")
        );
        return card;
    }

    private VBox highlightHeartRateCard() {
        VBox card = baseCard();
        card.getStyleClass().add("widget-highlight");

        card.getChildren().addAll(
                title("Heart Rate"),
                value("120 bpm"),
                sub("Live")
        );
        return card;
    }

    private VBox pressureCard() {
        VBox card = baseCard();
        card.getChildren().addAll(
                title("Pressure"),
                value("200"),
                sub("110 – 200")
        );
        return card;
    }

    private VBox medicationCard() {
        VBox card = baseCard();
        card.getChildren().addAll(
                title("Medications"),
                value("Metformin"),
                sub("150 mg")
        );
        return card;
    }

    private VBox bloodStatusCard() {
        VBox card = baseCard();
        card.getChildren().addAll(
                title("Blood Status"),
                value("116 / 70"),
                sub("mmHg")
        );
        return card;
    }

    // ---------------- UI Helpers ----------------

    private VBox baseCard() {
        VBox box = new VBox(6);
        box.setPrefSize(200, 120);
        box.setAlignment(Pos.TOP_LEFT);
        box.getStyleClass().add("widget");
        return box;
    }

    private Label title(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("widget-title");
        return l;
    }

    private Label value(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("widget-value");
        return l;
    }

    private Label sub(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("widget-sub");
        return l;
    }

    public static void main(String[] args) {
        launch();
    }
}
