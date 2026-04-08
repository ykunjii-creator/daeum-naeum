import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import javafx.stage.Stage;

public class HeartRateMockUI extends Application {

    @Override
    public void start(Stage stage) {
        // Background (gradient)
        StackPane root = new StackPane();
        root.getStyleClass().add("bg");

        HBox phones = new HBox(28, measurePhone(), statsPhone());
        phones.setAlignment(Pos.CENTER);
        phones.setPadding(new Insets(30));

        root.getChildren().add(phones);

        Scene scene = new Scene(root, 900, 650);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        stage.setTitle("HeartRate UI Mock (JavaFX)");
        stage.setScene(scene);
        stage.show();
    }

    private VBox measurePhone() {
        VBox phone = phoneFrame();

        // Top bar
        HBox top = topBar("HeartRate");

        // Tabs
        HBox tabs = new HBox(48,
                tab("Measure", true),
                tab("Statistics", false)
        );
        tabs.setAlignment(Pos.CENTER);
        tabs.setPadding(new Insets(14, 0, 10, 0));

        // Heart graphic (simple layered circles + heart emoji)
        StackPane heart = new StackPane();
        Circle c1 = new Circle(95); c1.getStyleClass().add("heart-glow-1");
        Circle c2 = new Circle(65); c2.getStyleClass().add("heart-glow-2");
        Circle c3 = new Circle(38); c3.getStyleClass().add("heart-glow-3");
        Label heartIcon = new Label("❤");
        heartIcon.getStyleClass().add("heart-icon");
        heart.getChildren().addAll(c1, c2, c3, heartIcon);
        heart.setPadding(new Insets(10, 0, 10, 0));

        // Big number
        Label bpm = new Label("072");
        bpm.getStyleClass().add("bpm-big");
        Label bpmSub = new Label("beats per minute");
        bpmSub.getStyleClass().add("bpm-sub");

        // ECG line (polyline)
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
        ecg.getStyleClass().add("ecg");
        StackPane ecgWrap = new StackPane(ecg);
        ecgWrap.setPadding(new Insets(20, 24, 26, 24));

        VBox content = new VBox(10, tabs, heart, bpm, bpmSub, ecgWrap);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(0, 0, 10, 0));

        phone.getChildren().addAll(top, content);
        return phone;
    }

    private VBox statsPhone() {
        VBox phone = phoneFrame();

        // Top bar
        HBox top = topBar("HeartRate");

        // Tabs (Statistics selected)
        HBox tabs = new HBox(48,
                tab("Measure", false),
                tab("Statistics", true)
        );
        tabs.setAlignment(Pos.CENTER);
        tabs.setPadding(new Insets(14, 0, 10, 0));

        // Segment: Day / Month / Year
        HBox segment = new HBox(14,
                segmentPill("Day", true),
                segmentPill("Month", false),
                segmentPill("Year", false)
        );
        segment.setAlignment(Pos.CENTER);
        segment.setPadding(new Insets(2, 0, 6, 0));

        // Chart (LineChart)
        NumberAxis xAxis = new NumberAxis(0, 24, 3);
        NumberAxis yAxis = new NumberAxis(50, 160, 20);
        xAxis.setTickLabelsVisible(true);
        yAxis.setTickLabelsVisible(true);
        xAxis.setTickMarkVisible(false);
        yAxis.setTickMarkVisible(false);

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setCreateSymbols(false);
        chart.getStyleClass().add("mini-chart");
        chart.setPrefHeight(260);

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

        // Separator line
        Line sep = new Line(0, 0, 1, 0);
        sep.getStyleClass().add("sep");
        sep.setStrokeWidth(1);
        sep.setManaged(true);
        sep.setVisible(true);

        // Bottom stats row (Min/Max/Avg)
        HBox stats = new HBox(24,
                statBlock("Min", "50"),
                statBlock("Max", "157"),
                statBlock("Avg", "81")
        );
        stats.setAlignment(Pos.CENTER);
        stats.setPadding(new Insets(14, 0, 18, 0));

        VBox content = new VBox(10, tabs, segment, chart, divider(), stats);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(0, 14, 0, 14));

        phone.getChildren().addAll(top, content);
        return phone;
    }

    // ---------- UI building blocks ----------

    private VBox phoneFrame() {
        VBox phone = new VBox();
        phone.getStyleClass().add("phone");
        phone.setPrefSize(290, 560);
        return phone;
    }

    private HBox topBar(String titleText) {
        Label title = new Label(titleText);
        title.getStyleClass().add("top-title");

        Label burger = new Label("≡");
        burger.getStyleClass().add("burger");

        Region spacer1 = new Region();
        Region spacer2 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        HBox top = new HBox(10, burger, spacer1, title, spacer2);
        top.setAlignment(Pos.CENTER);
        top.getStyleClass().add("topbar");
        top.setPadding(new Insets(14, 14, 14, 14));
        return top;
    }

    private VBox tab(String text, boolean selected) {
        Label t = new Label(text);
        t.getStyleClass().add("tab-text");
        if (selected) t.getStyleClass().add("tab-selected");

        Line underline = new Line(0, 0, 38, 0);
        underline.getStyleClass().add("tab-underline");
        underline.setVisible(selected);

        VBox tab = new VBox(6, t, underline);
        tab.setAlignment(Pos.CENTER);
        return tab;
    }

    private Label segmentPill(String text, boolean selected) {
        Label pill = new Label(text);
        pill.getStyleClass().add("pill");
        if (selected) pill.getStyleClass().add("pill-selected");
        return pill;
    }

    private VBox statBlock(String label, String value) {
        Label l = new Label(label);
        l.getStyleClass().add("stat-label");
        Label v = new Label(value);
        v.getStyleClass().add("stat-value");
        VBox box = new VBox(6, l, v);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private Region divider() {
        Region r = new Region();
        r.getStyleClass().add("divider");
        return r;
    }

    public static void main(String[] args) {
        launch();
    }
}
