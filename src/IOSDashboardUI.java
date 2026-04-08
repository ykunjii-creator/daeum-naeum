import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class IOSDashboardUI extends Application {

    @Override
    public void start(Stage stage) {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(16);
        grid.setVgap(16);
        grid.setBackground(new Background(new BackgroundFill(Color.web("#F2F2F7"), CornerRadii.EMPTY, Insets.EMPTY)));

        grid.add(simpleCard("Blood Count", "80–90", "bpm", false), 0, 0);
        grid.add(simpleCard("Heart Rate", "120 bpm", "Live", true), 1, 0);
        grid.add(simpleCard("Pressure", "200", "110 – 200", false), 2, 0);

        grid.add(simpleCard("Medications", "Metformin", "150 mg", false), 0, 1);
        grid.add(simpleCard("Blood Status", "116 / 70", "mmHg", false), 1, 1);

        Scene scene = new Scene(grid, 900, 420);

        stage.setTitle("iOS Health Dashboard (JavaFX)");
        stage.setScene(scene);
        stage.show();
    }

    private VBox simpleCard(String title, String value, String sub, boolean highlight) {
        VBox card = new VBox(6);
        card.setPrefSize(200, 120);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(16));

        // iOS card look
        card.setBackground(new Background(new BackgroundFill(
                highlight ? Color.web("#007AFF") : Color.WHITE,
                new CornerRadii(18),
                Insets.EMPTY
        )));

        card.setBorder(new Border(new BorderStroke(
                Color.web("#000000", 0.06),
                BorderStrokeStyle.SOLID,
                new CornerRadii(18),
                new BorderWidths(1)
        )));

        Label t = new Label(title);
        t.setFont(Font.font("System", 13));
        t.setTextFill(highlight ? Color.WHITE : Color.web("#3C3C43", 0.85));

        Label v = new Label(value);
        v.setFont(Font.font("System", 20));
        v.setStyle("-fx-font-weight: 700;");
        v.setTextFill(highlight ? Color.WHITE : Color.web("#111111"));

        Label s = new Label(sub);
        s.setFont(Font.font("System", 12));
        s.setTextFill(highlight ? Color.web("#FFFFFF", 0.92) : Color.web("#3C3C43", 0.70));

        card.getChildren().addAll(t, v, s);
        return card;
    }

    public static void main(String[] args) {
        launch();
    }
}
