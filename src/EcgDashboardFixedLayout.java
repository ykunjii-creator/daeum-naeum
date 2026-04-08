import java.io.File;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

public class EcgDashboardFixedLayout extends Application {

    enum State { NORMAL, LOW, HIGH }

    // 5초마다 이 순서로 바뀜
    private final State[] cycle = { State.NORMAL, State.LOW, State.HIGH };
    private int stateIndex = 0;

    // resources 폴더(프로젝트 루트 기준)
    private static final String RES_DIR = "resources/";

    // 상태에 따라 바뀌는 UI
    private Label topIcon;
    private Label bpmValue;
    private Label bpmUnit;
    private Polyline ecg;
    private Rectangle bottomGlow;
    private VBox rightPanel;
    private Label rightTitle;

    private ImageView heartView;
    private Image heartNormal, heartLow, heartHigh, heartFallback;

    @Override
    public void start(Stage stage) {
        loadHeartImages();

        // ====== 바깥 배경 ======
        StackPane root = new StackPane();
        root.setPadding(new Insets(18));
        root.setBackground(new Background(new BackgroundFill(Color.web("#1C1C1E"), CornerRadii.EMPTY, Insets.EMPTY)));

        // ====== 앱 화면(캔버스) ======
        // 너 이미지 비율이 대략 3:2 느낌이라 1040x620 유지
        AnchorPane canvas = new AnchorPane();
        canvas.setPrefSize(1040, 620);
        canvas.setBackground(new Background(new BackgroundFill(Color.web("#ECECEC"), new CornerRadii(8), Insets.EMPTY)));
        canvas.setEffect(new DropShadow(24, Color.web("#000000", 0.35)));

        // ====== 하단 그라데이션(위험일 때만 보이게) ======
        bottomGlow = new Rectangle(1040, 220);
        bottomGlow.setFill(new LinearGradient(
                0, 1, 0, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#F29A2E")),
                new Stop(1, Color.web("#ECECEC"))
        ));
        bottomGlow.setOpacity(0.0);
        AnchorPane.setLeftAnchor(bottomGlow, 0.0);
        AnchorPane.setBottomAnchor(bottomGlow, 0.0);

        // ====== ECG 라인 (중앙 가로선 위치 고정) ======
        ecg = buildEcgLine();
        ecg.setVisible(false);
        AnchorPane.setLeftAnchor(ecg, 0.0);
        AnchorPane.setTopAnchor(ecg, 250.0); // ← 이 값이 "그림처럼" 중앙선 위치

        // ====== 왼쪽 네비(탭) ======
        VBox nav = buildLeftNav();
        AnchorPane.setLeftAnchor(nav, 26.0);
        AnchorPane.setTopAnchor(nav, 86.0);

        // ====== 상단 아이콘 + BPM (중앙 위쪽) ======
        VBox vitals = buildVitals();
        AnchorPane.setLeftAnchor(vitals, (1040 - 240) / 2.0); // 가운데 정렬 느낌
        AnchorPane.setTopAnchor(vitals, 70.0);

        // ====== 가운데 하트 이미지 (하단 중앙) ======
        heartView = new ImageView();
        heartView.setPreserveRatio(true);
        heartView.setFitWidth(190);

        StackPane heartBox = new StackPane(heartView);
        heartBox.setPrefSize(240, 220);
        heartBox.setAlignment(Pos.CENTER);
        AnchorPane.setLeftAnchor(heartBox, (1040 - 240) / 2.0);
        AnchorPane.setTopAnchor(heartBox, 350.0); // ← 하단 중앙 위치

        // ====== 오른쪽 경고 패널 (고정) ======
        rightPanel = buildRightPanel();
        rightPanel.setVisible(false);
        AnchorPane.setRightAnchor(rightPanel, 28.0);
        AnchorPane.setTopAnchor(rightPanel, 60.0);

        // 올리기
        canvas.getChildren().addAll(bottomGlow, ecg, nav, vitals, heartBox, rightPanel);
        root.getChildren().add(canvas);

        Scene scene = new Scene(root, 1080, 700);
        stage.setTitle("ECG Dashboard (Fixed Layout)");
        stage.setScene(scene);
        stage.show();

        // 초기 상태
        applyState(State.LOW); // 처음 화면을 저심박(50)로 보고 싶으면 LOW로 시작
        // applyState(State.NORMAL);

        // 5초마다 상태 변화
        Timeline t = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            stateIndex = (stateIndex + 1) % cycle.length;
            applyState(cycle[stateIndex]);
        }));
        t.setCycleCount(Timeline.INDEFINITE);
        t.play();
    }

    // =================== Vitals ===================
    private VBox buildVitals() {
        VBox box = new VBox(6);
        box.setAlignment(Pos.TOP_CENTER);
        box.setPrefWidth(240);

        topIcon = new Label("⚠");
        topIcon.setFont(Font.font(30));

        bpmValue = new Label("50");
        bpmValue.setFont(Font.font(72));
        bpmValue.setStyle("-fx-font-weight: 900;");

        bpmUnit = new Label("bpm");
        bpmUnit.setFont(Font.font(26));

        box.getChildren().addAll(topIcon, bpmValue, bpmUnit);
        return box;
    }

    // =================== Left Nav ===================
    private VBox buildLeftNav() {
        VBox nav = new VBox(18);
        nav.setAlignment(Pos.TOP_CENTER);
        nav.setPadding(new Insets(18, 14, 18, 14));
        nav.setPrefSize(92, 400);

        nav.setBackground(new Background(new BackgroundFill(Color.web("#F7F7F7"), new CornerRadii(46), Insets.EMPTY)));
        nav.setBorder(new Border(new BorderStroke(
                Color.web("#FFFFFF", 0.90),
                BorderStrokeStyle.SOLID,
                new CornerRadii(46),
                new BorderWidths(2)
        )));
        nav.setEffect(new DropShadow(12, Color.web("#000000", 0.10)));

        nav.getChildren().addAll(
                navItem("⌂", "home"),
                navItem("❓", "guide"),
                navItem("〰", "report"),
                navItem("👤", "profile")
        );
        return nav;
    }

    private VBox navItem(String icon, String text) {
        Button b = new Button(icon);
        b.setFont(Font.font(18));
        b.setPrefSize(46, 46);
        b.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, new CornerRadii(12), Insets.EMPTY)));
        b.setTextFill(Color.web("#3A3A3A"));
        // 지금은 “모양만” — 클릭 동작은 나중에 연결 가능
        b.setOnAction(e -> System.out.println("Clicked: " + text));

        Label t = new Label(text);
        t.setFont(Font.font(10));
        t.setTextFill(Color.web("#8B8B8B"));

        VBox box = new VBox(6, b, t);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    // =================== Right Panel ===================
    private VBox buildRightPanel() {
        VBox panel = new VBox(16);
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setPadding(new Insets(22));
        panel.setPrefSize(255, 500);

        LinearGradient redGrad = new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#FF1F1F")),
                new Stop(1, Color.web("#B80000"))
        );
        panel.setBackground(new Background(new BackgroundFill(redGrad, new CornerRadii(44), Insets.EMPTY)));
        panel.setBorder(new Border(new BorderStroke(
                Color.web("#FFFFFF", 0.75),
                BorderStrokeStyle.SOLID,
                new CornerRadii(44),
                new BorderWidths(2)
        )));
        panel.setEffect(new DropShadow(18, Color.web("#000000", 0.22)));

        rightTitle = new Label("맥박 수가\n떨어져요!");
        rightTitle.setFont(Font.font(20));
        rightTitle.setStyle("-fx-font-weight: 900;");
        rightTitle.setTextFill(Color.WHITE);
        rightTitle.setAlignment(Pos.CENTER);
        rightTitle.setWrapText(true);

        Label sub = new Label("5초 후 자동 제어가\n시작됩니다.");
        sub.setFont(Font.font(12));
        sub.setTextFill(Color.web("#FFFFFF", 0.88));
        sub.setAlignment(Pos.CENTER);
        sub.setWrapText(true);

        Button b1 = pillButton("인근 쉼터 안내");
        b1.setOnAction(e -> System.out.println("Popup: shelter (fake)"));

        Button b2 = pillButton("응급 대응");
        b2.setOnAction(e -> System.out.println("Popup: emergency (fake)"));

        panel.getChildren().addAll(rightTitle, spacer(18), sub, spacer(24), b1, b2);
        return panel;
    }

    private Button pillButton(String text) {
        Button b = new Button(text);
        b.setPrefWidth(190);
        b.setPrefHeight(56);
        b.setFont(Font.font(14));
        b.setStyle("-fx-font-weight: 800;");
        b.setTextFill(Color.WHITE);

        b.setBackground(new Background(new BackgroundFill(
                new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#FF3A3A")),
                        new Stop(1, Color.web("#B80000"))
                ),
                new CornerRadii(22), Insets.EMPTY
        )));
        b.setEffect(new DropShadow(10, Color.web("#000000", 0.18)));
        return b;
    }

    // =================== ECG ===================
    private Polyline buildEcgLine() {
        Polyline p = new Polyline(
                0, 90, 70, 90, 88, 75, 105, 118, 128, 90,
                200, 90, 218, 75, 235, 120, 258, 90,
                340, 90, 358, 75, 375, 118, 398, 90,
                490, 90, 508, 75, 525, 120, 548, 90,
                640, 90, 658, 75, 675, 118, 698, 90,
                790, 90, 808, 75, 825, 120, 848, 90,
                940, 90, 1040, 90
        );
        p.setStroke(Color.web("#FF6B6B", 0.75));
        p.setStrokeWidth(2.2);
        p.setFill(Color.TRANSPARENT);
        return p;
    }

    // =================== State 적용 ===================
    private void applyState(State s) {
        if (s == State.NORMAL) {
            topIcon.setText("");
            bpmValue.setText("80-100");
            bpmValue.setTextFill(Color.web("#111111"));
            bpmUnit.setTextFill(Color.web("#8A8A8A"));

            bottomGlow.setOpacity(0.0);
            ecg.setVisible(false);
            rightPanel.setVisible(false);

            heartView.setImage(heartNormal != null ? heartNormal : heartFallback);

        } else if (s == State.LOW) {
            topIcon.setText("⚠");
            topIcon.setTextFill(Color.web("#F6B300"));

            bpmValue.setText("50");
            bpmValue.setTextFill(Color.web("#E53935"));
            bpmUnit.setTextFill(Color.web("#E53935", 0.85));

            bottomGlow.setOpacity(0.85);
            ecg.setVisible(true);
            rightPanel.setVisible(true);
            rightTitle.setText("맥박 수가\n떨어져요!");

            heartView.setImage(heartLow != null ? heartLow : heartFallback);

        } else { // HIGH
            topIcon.setText("🚨");
            topIcon.setTextFill(Color.web("#E53935"));

            bpmValue.setText("140-150");
            bpmValue.setTextFill(Color.web("#E53935"));
            bpmUnit.setTextFill(Color.web("#E53935", 0.85));

            bottomGlow.setOpacity(0.70);
            ecg.setVisible(true);
            rightPanel.setVisible(true);
            rightTitle.setText("맥박 수가\n너무 높아져요");

            heartView.setImage(heartHigh != null ? heartHigh : heartFallback);
        }
    }

    // =================== 이미지 로드 ===================
    private void loadHeartImages() {
        heartFallback = safeLoad(RES_DIR + "heart.png");
        heartNormal   = safeLoad(RES_DIR + "heart_normal.png");
        heartLow      = safeLoad(RES_DIR + "heart_low.png");
        heartHigh     = safeLoad(RES_DIR + "heart_high.png");

        if (heartNormal == null) heartNormal = heartFallback;
        if (heartLow == null)    heartLow = heartFallback;
        if (heartHigh == null)   heartHigh = heartFallback;
    }

    private Image safeLoad(String path) {
        try {
            File f = new File(path);
            if (!f.exists()) {
                System.out.println("[WARN] File not found: " + path);
                return null;
            }
            return new Image(f.toURI().toString());
        } catch (Exception e) {
            System.out.println("[WARN] Failed to load: " + path + " / " + e.getMessage());
            return null;
        }
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
