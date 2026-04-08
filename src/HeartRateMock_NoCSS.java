import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class HeartRateMock_NoCSS extends Application {

    @Override
    public void start(Stage stage) {
        // Background gradient (like sample)
        StackPane root = new StackPane();
        root.setPadding(new Insets(30));

        BackgroundFill bgFill = new BackgroundFill(
                new LinearGradient(
                        0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#F7E36B")),
                        new Stop(1, Color.web("#F3C27A"))
                ),
                CornerRadii.EMPTY, Insets.EMPTY
        );
        root.setBackground(new Background(bgFill));

        HBox phones = new HBox(28, measurePhone(), statsPhone());
        phones.setAlignment(Pos.CENTER);

        root.getChildren().add(phones);

        Scene scene = new Scene(root, 900, 650);
        stage.setTitle("HeartRate Mock (No CSS)");
        stage.setScene(scene);
        stage.show();
    }

    // -------------------- Measure Phone --------------------
    private VBox measurePhone() {
        VBox phone = phoneFrame();

        HBox top = topBar("HeartRate");

        HBox tabs = new HBox(55, tab("Measure", true), tab("Statistics", false));
        tabs.setAlignment(Pos.CENTER);
        tabs.setPadding(new Insets(14, 0, 10, 0));

        // Heart visual
        StackPane heart = new StackPane();
        Circle c1 = new Circle(95, Color.web("#FF6C6C", 0.10));
        Circle c2 = new Circle(65, Color.web("#FF6C6C", 0.14));
        Circle c3 = new Circle(38, Color.web("#FF6C6C", 0.18));

        Label heartIcon = new Label("❤");
        heartIcon.setFont(Font.font(34));
        heartIcon.setTextFill(Color.web("#FF6C6C"));

        heart.getChildren().addAll(c1, c2, c3, heartIcon);
        heart.setPadding(new Insets(10, 0, 10, 0));

        Label bpm = new Label("072");
        bpm.setFont(Font.font(46));
        bpm.setStyle("-fx-font-weight: 800;");
        bpm.setTextFill(Color.web("#9AA0A6"));

        Label bpmSub = new Label("beats per minute");
        bpmSub.setFont(Font.font(12));
        bpmSub.setTextFill(Color.web("#B0B5BB"));

        // ECG polyline
        Polyline ecg = new Polyline(
                0.0, 30.0,
                30.0, 30.0,
                45.0, 20.0,
                60.0, 45.0,
                80.0, 30.0,
                110.0, 30.0,
                125.0, 18.0,
                145.0, 48.0,
                170.0, 30.0,
                210.0, 30.0,
                230.0, 22.0,
                250.0, 46.0,
                280.0, 30.0,
                320.0, 30.0
        );
        ecg.setStroke(Color.web("#FF6C6C", 0.55));
        ecg.setStrokeWidth(2);
        ecg.setFill(Color.TRANSPARENT);

        StackPane ecgWrap = new StackPane(ecg);
        ecgWrap.setPadding(new Insets(20, 24, 26, 24));

        VBox content = new VBox(10, tabs, heart, bpm, bpmSub, ecgWrap);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(0, 0, 10, 0));

        phone.getChildren().addAll(top, content);
        return phone;
    }

    // -------------------- Stats Phone --------------------
    private VBox statsPhone() {
        VBox phone = phoneFrame();

        HBox top = topBar("HeartRate");

        HBox tabs = new HBox(55, tab("Measure", false), tab("Statistics", true));
        tabs.setAlignment(Pos.CENTER);
        tabs.setPadding(new Insets(14, 0, 10, 0));

        HBox segment = new HBox(14,
                pill("Day", true),
                pill("Month", false),
                pill("Year", false)
        );
        segment.setAlignment(Pos.CENTER);
        segment.setPadding(new Insets(2, 0, 6, 0));

        // Chart
        NumberAxis xAxis = new NumberAxis(0, 24, 3);
        NumberAxis yAxis = new NumberAxis(50, 160, 20);
        xAxis.setTickMarkVisible(false);
        yAxis.setTickMarkVisible(false);

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setCreateSymbols(false);
        chart.setPrefHeight(260);
        chart.setPrefWidth(255);
        chart.setHorizontalGridLinesVisible(true);
        chart.setVerticalGridLinesVisible(true);

        XYChart.Series<Number, Number> s = new XYChart.Series<>();
        s.getData().add(new XYChart.Data<>(0, 80));
        s.getData().add(new XYChart.Data<>(2, 120));
        s.getData().add(new XYChart.Data<>(4, 95));
        s.getData().add(new XYChart.Data<>(6, 70));
        s.getData().add(new XYChart.Data<>(9, 55));
        s.getData().add(new XYChart.Data<>(12, 90));
        s.getData().add(new XYChart.Data<>(15, 85));
        s.getData().add(new XYChart.Data<>(18, 150));
        s.getData().add(new XYChart.Data<>(20, 110));
        s.getData().add(new XYChart.Data<>(24, 80));
        chart.getData().add(s);

        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setMaxHeight(1);
        divider.setBackground(new Background(new BackgroundFill(Color.web("#000000", 0.08), CornerRadii.EMPTY, Insets.EMPTY)));
        divider.setMaxWidth(240);

        HBox stats = new HBox(26,
                statBlock("Min", "50"),
                statBlock("Max", "157"),
                statBlock("Avg", "81")
        );
        stats.setAlignment(Pos.CENTER);
        stats.setPadding(new Insets(14, 0, 18, 0));

        VBox content = new VBox(10, tabs, segment, chart, divider, stats);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(0, 14, 0, 14));

        phone.getChildren().addAll(top, content);
        return phone;
    }

    // -------------------- Building Blocks --------------------

    private VBox phoneFrame() {
        VBox phone = new VBox();
        phone.setPrefSize(290, 560);
        phone.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(8), Insets.EMPTY)));
        phone.setBorder(new Border(new BorderStroke(Color.web("#000000", 0.10),
                BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(1))));
        phone.setEffect(new javafx.scene.effect.DropShadow(22, Color.web("#000000", 0.25)));
        return phone;
    }

    private HBox topBar(String titleText) {
        Label title = new Label(titleText);
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font(16));
        title.setStyle("-fx-font-weight: 700;");

        Label burger = new Label("≡");
        burger.setTextFill(Color.web("#FFFFFF", 0.95));
        burger.setFont(Font.font(18));

        Region spacer1 = new Region();
        Region spacer2 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        HBox top = new HBox(10, burger, spacer1, title, spacer2);
        top.setAlignment(Pos.CENTER);
        top.setPadding(new Insets(14));
        top.setBackground(new Background(new BackgroundFill(Color.web("#FF6C6C"),
                new CornerRadii(8, 8, 0, 0, false), Insets.EMPTY)));
        return top;
    }

    private VBox tab(String text, boolean selected) {
        Label t = new Label(text);
        t.setFont(Font.font(13));
        t.setStyle("-fx-font-weight: 600;");
        t.setTextFill(selected ? Color.web("#6B6F76") : Color.web("#9AA0A6"));

        Line underline = new Line(0, 0, 38, 0);
        underline.setStroke(Color.web("#FF6C6C"));
        underline.setStrokeWidth(2);
        underline.setVisible(selected);

        VBox tab = new VBox(6, t, underline);
        tab.setAlignment(Pos.CENTER);
        return tab;
    }

    private Label pill(String text, boolean selected) {
        Label pill = new Label(text);
        pill.setFont(Font.font(12));
        pill.setStyle("-fx-font-weight: 600;");
        pill.setPadding(new Insets(6, 16, 6, 16));

        if (selected) {
            pill.setTextFill(Color.WHITE);
            pill.setBackground(new Background(new BackgroundFill(Color.web("#FF6C6C"),
                    new CornerRadii(999), Insets.EMPTY)));
        } else {
            pill.setTextFill(Color.web("#9AA0A6"));
            pill.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT,
                    new CornerRadii(999), Insets.EMPTY)));
        }
        return pill;
    }

    private VBox statBlock(String label, String value) {
        Label l = new Label(label);
        l.setTextFill(Color.web("#B0B5BB"));
        l.setFont(Font.font(12));
        l.setStyle("-fx-font-weight: 600;");

        Label v = new Label(value);
        v.setTextFill(Color.web("#8A8F96"));
        v.setFont(Font.font(14));
        v.setStyle("-fx-font-weight: 700;");

        VBox box = new VBox(6, l, v);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    public static void main(String[] args) {
        launch();
    }
}
