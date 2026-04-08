import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class EmergencyDashboard_NoCSS extends Application {

    enum Mode { LOW_BPM, HIGH_BPM }

    @Override
    public void start(Stage stage) {
        // 한 화면만 띄우고 싶으면 mode 바꿔!
        Mode mode = Mode.LOW_BPM;   // <- HIGH_BPM 으로 바꾸면 아래 그림 느낌

        Pane root = buildScreen(mode);

        Scene scene = new Scene(root, 980, 560);
        stage.setTitle("Emergency Dashboard (No CSS, JavaFX)");
        stage.setScene(scene);
        stage.show();
    }

    private Pane buildScreen(Mode mode) {
        StackPane root = new StackPane();
        root.setPadding(new Insets(24));
        root.setBackground(new Background(new BackgroundFill(
                Color.web("#1C1C1E"), CornerRadii.EMPTY, Insets.EMPTY
        )));

        // 메인 캔버스(가운데 큰 카드 영역)
        StackPane canvas = new StackPane();
        canvas.setPrefSize(900, 470);
        canvas.setBackground(new Background(new BackgroundFill(
                Color.web("#ECECEC"), new CornerRadii(8), Insets.EMPTY
        )));
        canvas.setEffect(new DropShadow(22, Color.web("#000000", 0.35)));

        // 아래쪽 오렌지 그라데이션(배경)
        Rectangle warmGrad = new Rectangle(900, 180);
        warmGrad.setFill(new LinearGradient(
                0, 1, 0, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#F08A2C")),
                new Stop(1, Color.web("#ECECEC"))
        ));
        warmGrad.setOpacity(0.85);
        StackPane.setAlignment(warmGrad, Pos.BOTTOM_CENTER);

        // 왼쪽 네비
        VBox nav = leftNav();
        StackPane.setAlignment(nav, Pos.CENTER_LEFT);
        StackPane.setMargin(nav, new Insets(0, 0, 0, 22));

        // 중앙 bpm + 아이콘 + ECG
        VBox center = centerVitals(mode);
        StackPane.setAlignment(center, Pos.TOP_CENTER);
        StackPane.setMargin(center, new Insets(70, 0, 0, 0));

        // ECG 라인(가로)
        Polyline ecg = ecgLine();
        StackPane.setAlignment(ecg, Pos.CENTER);
        ecg.setTranslateY(20);

        // 오른쪽 경고 패널
        VBox alertPanel = rightAlertPanel(mode);
        StackPane.setAlignment(alertPanel, Pos.CENTER_RIGHT);
        StackPane.setMargin(alertPanel, new Insets(0, 22, 0, 0));

        // 하단 하트 캐릭터(간단 도형 버전)
        StackPane heart = heartCharacter(mode);
        StackPane.setAlignment(heart, Pos.BOTTOM_CENTER);
        StackPane.setMargin(heart, new Insets(0, 0, 40, 0));

        canvas.getChildren().addAll(warmGrad, ecg, center, nav, alertPanel, heart);
        root.getChildren().add(canvas);
        return root;
    }

    // ---------------- Left Nav ----------------
    private VBox leftNav() {
        VBox nav = new VBox(22);
        nav.setAlignment(Pos.TOP_CENTER);
        nav.setPadding(new Insets(18, 14, 18, 14));
        nav.setPrefSize(86, 360);

        nav.setBackground(new Background(new BackgroundFill(
                Color.web("#F7F7F7"), new CornerRadii(40), Insets.EMPTY
        )));
        nav.setBorder(new Border(new BorderStroke(
                Color.web("#FFFFFF", 0.85), BorderStrokeStyle.SOLID,
                new CornerRadii(40), new BorderWidths(2)
        )));
        nav.setEffect(new DropShadow(12, Color.web("#000000", 0.10)));

        nav.getChildren().addAll(
                navItem("⌂", "home", true),
                navItem("❓", "guide", false),
                navItem("〰", "report", false),
                navItem("👤", "profile", false)
        );
        return nav;
    }

    private VBox navItem(String icon, String text, boolean selected) {
        VBox item = new VBox(6);
        item.setAlignment(Pos.CENTER);

        Label i = new Label(icon);
        i.setFont(Font.font(18));
        i.setTextFill(selected ? Color.web("#111111") : Color.web("#6B6B6B"));

        Label t = new Label(text);
        t.setFont(Font.font(10));
        t.setTextFill(Color.web("#8B8B8B"));

        item.getChildren().addAll(i, t);
        return item;
    }

    // ---------------- Center ----------------
    private VBox centerVitals(Mode mode) {
        VBox box = new VBox(8);
        box.setAlignment(Pos.TOP_CENTER);

        Label icon = new Label(mode == Mode.LOW_BPM ? "⚠" : "🚨");
        icon.setFont(Font.font(26));
        icon.setTextFill(mode == Mode.LOW_BPM ? Color.web("#F6B300") : Color.web("#E53935"));

        Label bpm = new Label("50");
        bpm.setFont(Font.font(64));
        bpm.setStyle("-fx-font-weight: 800;");
        bpm.setTextFill(Color.web("#E53935"));

        Label unit = new Label("bpm");
        unit.setFont(Font.font(22));
        unit.setTextFill(Color.web("#E53935", 0.85));

        box.getChildren().addAll(icon, bpm, unit);
        return box;
    }

    // ---------------- ECG ----------------
    private Polyline ecgLine() {
        Polyline ecg = new Polyline(
                0, 120, 60, 120, 75, 110, 90, 140, 110, 120,
                160, 120, 175, 108, 190, 146, 210, 120,
                280, 120, 295, 110, 310, 140, 330, 120,
                420, 120, 435, 108, 450, 146, 470, 120,
                560, 120, 575, 110, 590, 140, 610, 120,
                700, 120, 715, 108, 730, 146, 750, 120,
                840, 120, 900, 120
        );
        ecg.setStroke(Color.web("#FF6B6B", 0.70));
        ecg.setStrokeWidth(2.2);
        ecg.setFill(Color.TRANSPARENT);
        return ecg;
    }

    // ---------------- Right Panel ----------------
    private VBox rightAlertPanel(Mode mode) {
        VBox panel = new VBox(14);
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setPadding(new Insets(18));
        panel.setPrefSize(230, 370);

        LinearGradient redGrad = new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#FF1F1F")),
                new Stop(1, Color.web("#C60000"))
        );
        panel.setBackground(new Background(new BackgroundFill(redGrad, new CornerRadii(34), Insets.EMPTY)));
        panel.setBorder(new Border(new BorderStroke(
                Color.web("#FFFFFF", 0.75),
                BorderStrokeStyle.SOLID,
                new CornerRadii(34),
                new BorderWidths(2)
        )));
        panel.setEffect(new DropShadow(18, Color.web("#000000", 0.22)));

        String title = (mode == Mode.LOW_BPM) ? "맥박 수가\n떨어져요!" : "맥박 수가\n너무 높아져요";
        Label t = new Label(title);
        t.setFont(Font.font(18));
        t.setStyle("-fx-font-weight: 800;");
        t.setTextFill(Color.WHITE);
        t.setAlignment(Pos.CENTER);
        t.setWrapText(true);

        Label sub = new Label("5초 후 자동 제어가\n시작됩니다.");
        sub.setFont(Font.font(12));
        sub.setTextFill(Color.web("#FFFFFF", 0.88));
        sub.setAlignment(Pos.CENTER);
        sub.setWrapText(true);

        Button b1 = pillButton("인근 쉼터 안내");
        Button b2 = pillButton("응급 대응");

        panel.getChildren().addAll(t, spacer(10), sub, spacer(16), b1, b2);
        return panel;
    }

    private Button pillButton(String text) {
        Button b = new Button(text);
        b.setPrefWidth(175);
        b.setPrefHeight(48);
        b.setFont(Font.font(13));
        b.setStyle("-fx-font-weight: 700;");
        b.setTextFill(Color.WHITE);

        b.setBackground(new Background(new BackgroundFill(
                new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#FF3A3A")),
                        new Stop(1, Color.web("#B80000"))
                ),
                new CornerRadii(18), Insets.EMPTY
        )));
        b.setBorder(new Border(new BorderStroke(
                Color.web("#FFFFFF", 0.35),
                BorderStrokeStyle.SOLID,
                new CornerRadii(18),
                new BorderWidths(1)
        )));
        b.setEffect(new DropShadow(10, Color.web("#000000", 0.18)));

        // 클릭 이벤트 자리(나중에 네비/응급콜 연결하면 됨)
        b.setOnAction(e -> System.out.println("Clicked: " + text));
        return b;
    }

    // ---------------- Heart character ----------------
    private StackPane heartCharacter(Mode mode) {
        StackPane heart = new StackPane();
        heart.setPrefSize(140, 110);

        // 간단 하트 느낌: 원 2개 + 삼각형
        Circle left = new Circle(36, Color.web("#FF6B6B"));
        Circle right = new Circle(36, Color.web("#FF6B6B"));
        left.setTranslateX(-22);
        right.setTranslateX(22);
        left.setTranslateY(-6);
        right.setTranslateY(-6);

        Polygon bottom = new Polygon(
                0, 70,
                -55, 10,
                55, 10
        );
        bottom.setFill(Color.web("#FF6B6B"));

        // 얼굴
        Circle face = new Circle(42, Color.web("#FFD1D1"));
        face.setTranslateY(14);

        Circle eye1 = new Circle(3.2, Color.web("#222222"));
        Circle eye2 = new Circle(3.2, Color.web("#222222"));
        eye1.setTranslateX(-12); eye1.setTranslateY(6);
        eye2.setTranslateX(12);  eye2.setTranslateY(6);

        Circle mouth = new Circle(10, Color.web("#FF3B30"));
        mouth.setTranslateY(24);

        // 상태 표시(빈맥은 볼 터치 느낌 원)
        if (mode == Mode.HIGH_BPM) {
            Circle cheek = new Circle(16, Color.web("#FF9AA2"));
            cheek.setTranslateX(42);
            cheek.setTranslateY(20);
            heart.getChildren().addAll(left, right, bottom, face, eye1, eye2, mouth, cheek);
        } else {
            heart.getChildren().addAll(left, right, bottom, face, eye1, eye2, mouth);
        }

        return heart;
    }

    private Region spacer(double h) {
        Region r = new Region();
        r.setPrefHeight(h);
        return r;
    }

    public static void main(String[] args) {
        launch();
    }
}
