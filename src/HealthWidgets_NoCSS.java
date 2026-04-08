import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class HealthWidgets_NoCSS extends Application {

    private static final Color BG = Color.web("#E9E9EF");      // light gray background
    private static final Color CARD = Color.WHITE;             // card background
    private static final Color TEXT = Color.web("#222222");
    private static final Color SUBT = Color.web("#777777");
    private static final Color BLUE = Color.web("#0A6CFF");
    private static final Color LIGHT_GRAY = Color.web("#DADCE2");

    @Override
    public void start(Stage stage) {
        // Root background
        Pane root = new Pane();
        root.setBackground(new Background(new BackgroundFill(BG, CornerRadii.EMPTY, Insets.EMPTY)));

        // Layout (manual but simple)
        // Canvas size similar to screenshot
        double W = 820, H = 520;

        // positions
        double margin = 55;
        double gapX = 28;
        double gapY = 28;

        double topCardW = 220;
        double topCardH = 150;

        double bottomCardW = 310;
        double bottomCardH = 150;

        // Top row (3 cards)
        Region c1 = bloodCountCard(topCardW, topCardH);
        c1.relocate(margin, 70);

        Region c2 = heartRateCard(topCardW, topCardH);
        c2.relocate(margin + topCardW + gapX, 70);

        Region c3 = pressureCard(topCardW, topCardH);
        c3.relocate(margin + (topCardW + gapX) * 2, 70);

        // Bottom row (2 cards)
        Region c4 = medicationsCard(bottomCardW, bottomCardH);
        c4.relocate(margin + 60, 70 + topCardH + gapY);

        Region c5 = bloodStatusCard(bottomCardW, bottomCardH);
        c5.relocate(margin + 60 + bottomCardW + gapX, 70 + topCardH + gapY);

        root.getChildren().addAll(c1, c2, c3, c4, c5);

        Scene scene = new Scene(root, W, H);
        stage.setTitle("Health Widgets (No CSS)");
        stage.setScene(scene);
        stage.show();
    }

    // ----------------- Card Base -----------------
    private StackPane cardBase(double w, double h) {
        StackPane card = new StackPane();
        card.setPrefSize(w, h);

        BackgroundFill fill = new BackgroundFill(CARD, new CornerRadii(26), Insets.EMPTY);
        card.setBackground(new Background(fill));

        card.setEffect(new DropShadow(18, Color.web("#000000", 0.12)));
        card.setPadding(new Insets(16));
        return card;
    }

    // ----------------- Card 1: Blood Count -----------------
    private Region bloodCountCard(double w, double h) {
        StackPane base = cardBase(w, h);

        VBox wrap = new VBox(10);
        wrap.setAlignment(Pos.TOP_LEFT);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane icon = smallIcon("≋");
        Label title = label("Blood Count", 14, TEXT, true);

        Label value = label("80–90", 14, TEXT, true);
        VBox titleBox = new VBox(2, title, value);

        header.getChildren().addAll(icon, titleBox);

        // mini waveform area
        StackPane mini = new StackPane();
        mini.setPrefHeight(70);
        mini.setBackground(new Background(new BackgroundFill(Color.web("#F3F4F7"), new CornerRadii(18), Insets.EMPTY)));
        mini.setPadding(new Insets(10));

        Polyline wave = new Polyline(
                0, 30, 18, 26, 36, 34, 54, 22, 72, 30,
                90, 28, 108, 35, 126, 20, 144, 30
        );
        wave.setStroke(Color.web("#B8BBC4"));
        wave.setStrokeWidth(2);
        wave.setFill(Color.TRANSPARENT);

        Circle dot = new Circle(2.6, BLUE);
        dot.setTranslateX(62);
        dot.setTranslateY(2);

        VBox right = new VBox(2,
                label("120", 16, TEXT, true),
                label("bpm", 11, SUBT, false)
        );
        right.setAlignment(Pos.CENTER_LEFT);

        HBox miniRow = new HBox(10);
        miniRow.setAlignment(Pos.CENTER_LEFT);
        miniRow.getChildren().addAll(new StackPane(wave, dot), right);

        mini.getChildren().add(miniRow);

        wrap.getChildren().addAll(header, mini);
        base.getChildren().add(wrap);
        StackPane.setAlignment(wrap, Pos.TOP_LEFT);
        return base;
    }

    // ----------------- Card 2: Heart Rate (highlight) -----------------
    private Region heartRateCard(double w, double h) {
        StackPane base = cardBase(w, h);

        VBox wrap = new VBox(10);
        wrap.setAlignment(Pos.TOP_LEFT);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane icon = smallBlueIcon("♡");
        VBox titleBox = new VBox(2,
                label("Heart Rate", 14, TEXT, true),
                label("120 bpm", 14, TEXT, true)
        );
        header.getChildren().addAll(icon, titleBox);

        HBox body = new HBox(10);
        body.setAlignment(Pos.CENTER_LEFT);

        // ECG line
        StackPane ecgArea = new StackPane();
        ecgArea.setPrefSize(120, 70);
        ecgArea.setBackground(new Background(new BackgroundFill(Color.web("#F3F4F7"), new CornerRadii(18), Insets.EMPTY)));
        ecgArea.setPadding(new Insets(10));

        Polyline ecg = new Polyline(
                0, 30, 12, 30, 18, 22, 24, 45, 32, 30, 48, 30,
                54, 20, 60, 46, 70, 30, 90, 30,
                96, 22, 102, 45, 112, 30
        );
        ecg.setStroke(BLUE);
        ecg.setStrokeWidth(2);
        ecg.setFill(Color.TRANSPARENT);

        ecgArea.getChildren().add(ecg);

        // Blue pill at right
        StackPane pill = new StackPane();
        pill.setPrefSize(62, 70);
        pill.setBackground(new Background(new BackgroundFill(BLUE, new CornerRadii(18), Insets.EMPTY)));

        VBox pillText = new VBox(
                label("120", 16, Color.WHITE, true),
                label("bpm", 11, Color.web("#FFFFFF", 0.92), false)
        );
        pillText.setAlignment(Pos.CENTER_LEFT);
        pillText.setPadding(new Insets(0, 0, 0, 10));
        pill.getChildren().add(pillText);

        body.getChildren().addAll(ecgArea, pill);

        wrap.getChildren().addAll(header, body);
        base.getChildren().add(wrap);
        StackPane.setAlignment(wrap, Pos.TOP_LEFT);
        return base;
    }

    // ----------------- Card 3: Pressure -----------------
    private Region pressureCard(double w, double h) {
        StackPane base = cardBase(w, h);

        VBox wrap = new VBox(10);
        wrap.setAlignment(Pos.TOP_LEFT);

        Label title = label("Pressure", 14, TEXT, true);

        HBox body = new HBox(10);
        body.setAlignment(Pos.CENTER_LEFT);

        VBox labels = new VBox(18,
                label("200", 14, TEXT, true),
                label("110", 14, TEXT, true)
        );
        labels.setAlignment(Pos.CENTER_LEFT);

        StackPane chart = new StackPane();
        chart.setPrefSize(140, 90);
        chart.setBackground(new Background(new BackgroundFill(Color.web("#F3F4F7"), new CornerRadii(18), Insets.EMPTY)));

        // little “mountain” areas
        Polygon topArea = new Polygon(
                10, 55, 30, 40, 55, 46, 75, 32, 95, 44, 120, 36, 132, 48, 132, 55, 10, 55
        );
        topArea.setFill(Color.web("#F0D8A0"));

        Polygon midArea = new Polygon(
                10, 55, 25, 62, 45, 58, 65, 70, 85, 60, 110, 72, 132, 62, 132, 80, 10, 80
        );
        midArea.setFill(Color.web("#CFE0FF"));

        Line axis = new Line(55, 20, 55, 78);
        axis.setStroke(Color.web("#333333"));
        axis.setStrokeWidth(2);

        Circle dotTop = new Circle(4, Color.web("#222222"));
        dotTop.setCenterX(55); dotTop.setCenterY(25);

        Circle dotBot = new Circle(4, Color.web("#222222"));
        dotBot.setCenterX(55); dotBot.setCenterY(75);

        chart.getChildren().addAll(topArea, midArea, axis, dotTop, dotBot);

        body.getChildren().addAll(labels, chart);

        wrap.getChildren().addAll(title, body);
        base.getChildren().add(wrap);
        StackPane.setAlignment(wrap, Pos.TOP_LEFT);
        return base;
    }

    // ----------------- Card 4: Medications -----------------
    private Region medicationsCard(double w, double h) {
        StackPane base = cardBase(w, h);

        VBox wrap = new VBox(10);
        wrap.setAlignment(Pos.TOP_LEFT);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = label("Medications", 14, TEXT, true);

        // small badge "3"
        StackPane badge = new StackPane(label("3", 12, TEXT, true));
        badge.setPadding(new Insets(4, 8, 4, 8));
        badge.setBackground(new Background(new BackgroundFill(Color.web("#E9ECF4"), new CornerRadii(10), Insets.EMPTY)));

        header.getChildren().addAll(title, badge);

        // icon area
        StackPane icon = new StackPane();
        icon.setPrefSize(44, 44);
        icon.setBackground(new Background(new BackgroundFill(Color.web("#E9F0FF"), new CornerRadii(14), Insets.EMPTY)));

        Label i = new Label("⟡");
        i.setFont(Font.font(18));
        i.setTextFill(BLUE);
        icon.getChildren().add(i);

        VBox text = new VBox(4,
                label("Metformin", 16, TEXT, true),
                label("150 mg", 12, SUBT, false)
        );

        wrap.getChildren().addAll(header, icon, text);
        base.getChildren().add(wrap);
        StackPane.setAlignment(wrap, Pos.TOP_LEFT);
        return base;
    }

    // ----------------- Card 5: Blood Status -----------------
    private Region bloodStatusCard(double w, double h) {
        StackPane base = cardBase(w, h);

        VBox wrap = new VBox(10);
        wrap.setAlignment(Pos.TOP_LEFT);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane icon = smallIcon("⟳");
        VBox titleBox = new VBox(2,
                label("Blood Status", 14, SUBT, true),
                label("116/70", 14, TEXT, true)
        );
        header.getChildren().addAll(icon, titleBox);

        // mini bars area
        StackPane area = new StackPane();
        area.setPrefSize(260, 80);
        area.setBackground(new Background(new BackgroundFill(Color.web("#F3F4F7"), new CornerRadii(18), Insets.EMPTY)));
        area.setPadding(new Insets(10));

        Pane bars = new Pane();
        bars.setPrefSize(180, 60);

        for (int k = 0; k < 10; k++) {
            double x = 10 + k * 16;
            double y1 = 10 + (k % 3) * 8;
            double y2 = 50 - (k % 4) * 6;

            Line l = new Line(x, y1, x, y2);
            l.setStroke(Color.web("#B8BBC4"));
            l.setStrokeWidth(2);
            bars.getChildren().add(l);
        }

        Line blueLine = new Line(150, 10, 150, 50);
        blueLine.setStroke(BLUE);
        blueLine.setStrokeWidth(3);
        bars.getChildren().add(blueLine);

        VBox right = new VBox(0,
                label("116", 16, TEXT, true),
                label("/70", 12, SUBT, false)
        );
        right.setAlignment(Pos.CENTER_LEFT);

        HBox row = new HBox(12, bars, right);
        row.setAlignment(Pos.CENTER_LEFT);

        area.getChildren().add(row);

        wrap.getChildren().addAll(header, area);
        base.getChildren().add(wrap);
        StackPane.setAlignment(wrap, Pos.TOP_LEFT);
        return base;
    }

    // ----------------- Helpers -----------------
    private Label label(String text, int size, Color color, boolean bold) {
        Label l = new Label(text);
        l.setFont(Font.font("System", size));
        l.setTextFill(color);
        if (bold) l.setStyle("-fx-font-weight: 700;");
        return l;
    }

    private StackPane smallIcon(String glyph) {
        StackPane p = new StackPane();
        p.setPrefSize(34, 34);
        p.setBackground(new Background(new BackgroundFill(Color.web("#EFF1F6"), new CornerRadii(12), Insets.EMPTY)));

        Label l = new Label(glyph);
        l.setFont(Font.font(14));
        l.setTextFill(Color.web("#666666"));
        p.getChildren().add(l);
        return p;
    }

    private StackPane smallBlueIcon(String glyph) {
        StackPane p = new StackPane();
        p.setPrefSize(34, 34);
        p.setBackground(new Background(new BackgroundFill(BLUE, new CornerRadii(12), Insets.EMPTY)));

        Label l = new Label(glyph);
        l.setFont(Font.font(14));
        l.setTextFill(Color.WHITE);
        p.getChildren().add(l);
        return p;
    }

    public static void main(String[] args) {
        launch();
    }
}
