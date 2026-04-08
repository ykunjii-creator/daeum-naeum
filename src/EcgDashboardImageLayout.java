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
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

public class EcgDashboardImageLayout extends Application {

    enum State { NORMAL, LOW, HIGH }

    private static final String RES = "resources/";

    private final State[] cycle = { State.NORMAL, State.LOW, State.HIGH };
    private int idx = 0;

    // ===== 이미지들 =====
    private Image heartNormal, heartLow, heartHigh, heartFallback;
    private Image navRailImg, ecgLineImg;
    private Image warnImg, sirenImg;
    private Image btnShelterImg, btnEmergencyImg;

    // ✅ 추가: 그라데이션 이미지
    private Image gradLowImg, gradHighImg;

    // ===== UI =====
    private StackPane stageRoot;
    private AnchorPane canvas;

    private ImageView iconTop;
    private Label bpmValue;
    private Label bpmUnit;

    private ImageView heartView;

    // ✅ 변경: Rectangle -> ImageView
    private ImageView gradView;

    private ImageView ecgView;
    private Pane rightPanel;
    private Label rightTitleText;

    @Override
    public void start(Stage stage) {
        loadImages();

        stageRoot = new StackPane();
        stageRoot.setPadding(new Insets(18));
        stageRoot.setBackground(new Background(new BackgroundFill(Color.web("#1C1C1E"), CornerRadii.EMPTY, Insets.EMPTY)));

        canvas = new AnchorPane();
        canvas.setPrefSize(1040, 620);
        canvas.setBackground(new Background(new BackgroundFill(Color.web("#ECECEC"), new CornerRadii(8), Insets.EMPTY)));
        canvas.setEffect(new DropShadow(24, Color.web("#000000", 0.35)));

        // ✅ 하단 그라데이션(ImageView)
        gradView = new ImageView();
        gradView.setPreserveRatio(false);
        gradView.setFitWidth(1040);
        gradView.setFitHeight(230);
        gradView.setOpacity(1.0);
        gradView.setVisible(false);
        AnchorPane.setLeftAnchor(gradView, 0.0);
        AnchorPane.setBottomAnchor(gradView, 0.0);

        // 왼쪽 탭(nav rail)
        ImageView navRail = new ImageView(navRailImg);
        navRail.setPreserveRatio(true);
        navRail.setFitHeight(420);
        AnchorPane.setLeftAnchor(navRail, 26.0);
        AnchorPane.setTopAnchor(navRail, 86.0);

        // ECG 라인(Union)
        ecgView = new ImageView(ecgLineImg);
        ecgView.setPreserveRatio(false);
        ecgView.setFitWidth(1040);
        ecgView.setFitHeight(60);
        ecgView.setOpacity(0.9);
        ecgView.setVisible(false);
        AnchorPane.setLeftAnchor(ecgView, 0.0);
        AnchorPane.setTopAnchor(ecgView, 265.0);

        // 상단 아이콘 + BPM
        VBox vitals = buildVitals();
        AnchorPane.setLeftAnchor(vitals, (1040 - 240) / 2.0);
        AnchorPane.setTopAnchor(vitals, 70.0);

        // 하트
        heartView = new ImageView();
        heartView.setPreserveRatio(true);
        heartView.setFitWidth(200);
        AnchorPane.setLeftAnchor(heartView, (1040 - 200) / 2.0);
        AnchorPane.setTopAnchor(heartView, 360.0);

        // 오른쪽 패널
        rightPanel = buildRightPanel();
        AnchorPane.setRightAnchor(rightPanel, 26.0);
        AnchorPane.setTopAnchor(rightPanel, 60.0);

        canvas.getChildren().addAll(gradView, ecgView, navRail, vitals, heartView, rightPanel);
        stageRoot.getChildren().add(canvas);

        Scene scene = new Scene(stageRoot, 1080, 700);
        stage.setTitle("ECG Dashboard (Gradient Image)");
        stage.setScene(scene);
        stage.show();

        // 초기 상태
        applyState(State.LOW);

        Timeline t = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            idx = (idx + 1) % cycle.length;
            applyState(cycle[idx]);
        }));
        t.setCycleCount(Timeline.INDEFINITE);
        t.play();
    }

    private VBox buildVitals() {
        VBox box = new VBox(6);
        box.setAlignment(Pos.TOP_CENTER);
        box.setPrefWidth(240);

        iconTop = new ImageView();
        iconTop.setPreserveRatio(true);
        iconTop.setFitWidth(52);

        bpmValue = new Label("50");
        bpmValue.setFont(Font.font(72));
        bpmValue.setStyle("-fx-font-weight: 900;");
        bpmValue.setTextFill(Color.web("#E53935"));

        bpmUnit = new Label("bpm");
        bpmUnit.setFont(Font.font(26));
        bpmUnit.setTextFill(Color.web("#E53935", 0.85));

        box.getChildren().addAll(iconTop, bpmValue, bpmUnit);
        return box;
    }

    private Pane buildRightPanel() {
        StackPane panel = new StackPane();
        panel.setPrefSize(255, 500);
        panel.setBackground(new Background(new BackgroundFill(Color.web("#E30000"), new CornerRadii(44), Insets.EMPTY)));
        panel.setEffect(new DropShadow(18, Color.web("#000000", 0.22)));

        VBox inner = new VBox(16);
        inner.setAlignment(Pos.TOP_CENTER);
        inner.setPadding(new Insets(22));

        rightTitleText = new Label("맥박 수가\n떨어져요!");
        rightTitleText.setFont(Font.font(20));
        rightTitleText.setStyle("-fx-font-weight: 900;");
        rightTitleText.setTextFill(Color.WHITE);
        rightTitleText.setAlignment(Pos.CENTER);
        rightTitleText.setWrapText(true);

        Label sub = new Label("5초 후 자동 제어가\n시작됩니다.");
        sub.setFont(Font.font(12));
        sub.setTextFill(Color.web("#FFFFFF", 0.88));
        sub.setAlignment(Pos.CENTER);
        sub.setWrapText(true);

        ImageView shelterBtnView = new ImageView(btnShelterImg);
        shelterBtnView.setPreserveRatio(true);
        shelterBtnView.setFitWidth(200);

        ImageView emergencyBtnView = new ImageView(btnEmergencyImg);
        emergencyBtnView.setPreserveRatio(true);
        emergencyBtnView.setFitWidth(200);

        shelterBtnView.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> showPopup(
                "인근 쉼터 안내",
                "• 가장 가까운 졸음쉼터: 2.4km",
                "• 방향: 다음 교차로에서 우회전 → 직진 1.8km",
                "• 예상 도착: 약 5분"
        ));

        emergencyBtnView.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> showPopup(
                "응급 대응",
                "• 119 자동 연결(가정)",
                "• GPS/탐지 시간/상태 메시지 전송(가정)",
                "• 보호자에게 알림 전송(가정)"
        ));

        Region spacer1 = new Region(); spacer1.setPrefHeight(20);
        Region spacer2 = new Region(); spacer2.setPrefHeight(16);

        inner.getChildren().addAll(rightTitleText, spacer1, sub, spacer2, shelterBtnView, emergencyBtnView);
        panel.getChildren().add(inner);
        return panel;
    }

    private void applyState(State s) {
        if (s == State.NORMAL) {
            iconTop.setImage(null);

            bpmValue.setText("80-100");
            bpmValue.setTextFill(Color.web("#111111"));
            bpmUnit.setTextFill(Color.web("#8A8A8A"));

            gradView.setVisible(false);
            ecgView.setVisible(false);
            rightPanel.setVisible(false);

            heartView.setImage(heartNormal != null ? heartNormal : heartFallback);

        } else if (s == State.LOW) {
            iconTop.setImage(warnImg);

            bpmValue.setText("50");
            bpmValue.setTextFill(Color.web("#E53935"));
            bpmUnit.setTextFill(Color.web("#E53935", 0.85));

            // ✅ 노란 그라데이션
            gradView.setImage(gradLowImg);
            gradView.setVisible(true);

            ecgView.setVisible(true);
            rightPanel.setVisible(true);
            rightTitleText.setText("맥박 수가\n떨어져요!");

            heartView.setImage(heartLow != null ? heartLow : heartFallback);

        } else { // HIGH
            iconTop.setImage(sirenImg);

            bpmValue.setText("140-150");
            bpmValue.setTextFill(Color.web("#E53935"));
            bpmUnit.setTextFill(Color.web("#E53935", 0.85));

            // ✅ 빨간 그라데이션
            gradView.setImage(gradHighImg);
            gradView.setVisible(true);

            ecgView.setVisible(true);
            rightPanel.setVisible(true);
            rightTitleText.setText("맥박 수가\n너무 높아져요");

            heartView.setImage(heartHigh != null ? heartHigh : heartFallback);
        }
    }

    private void showPopup(String title, String a, String b, String c) {
        StackPane overlay = new StackPane();

        Rectangle dim = new Rectangle(1080, 700);
        dim.setFill(Color.web("#000000", 0.35));

        VBox card = new VBox(10);
        card.setPadding(new Insets(16));
        card.setPrefSize(430, 230);
        card.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(18), Insets.EMPTY)));
        card.setEffect(new DropShadow(20, Color.web("#000000", 0.25)));

        Label t = new Label(title);
        t.setFont(Font.font(16));
        t.setStyle("-fx-font-weight: 900;");

        Label l1 = new Label(a);
        Label l2 = new Label(b); l2.setWrapText(true);
        Label l3 = new Label(c);

        Button close = new Button("닫기");
        close.setPrefSize(90, 34);
        close.setTextFill(Color.WHITE);
        close.setFont(Font.font(12));
        close.setStyle("-fx-font-weight: 800;");
        close.setBackground(new Background(new BackgroundFill(Color.web("#0A6CFF"), new CornerRadii(12), Insets.EMPTY)));

        HBox row = new HBox(close);
        row.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(t, spacer(6), l1, l2, l3, spacer(10), row);

        overlay.getChildren().addAll(dim, card);
        StackPane.setAlignment(card, Pos.CENTER);

        close.setOnAction(e -> stageRoot.getChildren().remove(overlay));
        dim.setOnMouseClicked(e -> stageRoot.getChildren().remove(overlay));

        stageRoot.getChildren().add(overlay);
    }

    private Region spacer(double h) {
        Region r = new Region();
        r.setPrefHeight(h);
        return r;
    }

    private void loadImages() {
        heartFallback = safeLoad(RES + "heart.png");
        heartNormal   = safeLoad(RES + "heart_normal.png");
        heartLow      = safeLoad(RES + "heart_low.png");
        heartHigh     = safeLoad(RES + "heart_high.png");

        navRailImg = safeLoad(RES + "nav_rail.png");       // Group42
        ecgLineImg = safeLoad(RES + "ecg_line.png");       // Union

        warnImg  = safeLoad(RES + "icon_warning.png");     // Group15
        sirenImg = safeLoad(RES + "icon_siren.png");       // Group24

        btnShelterImg   = safeLoad(RES + "btn_shelter.png");   // Group43
        btnEmergencyImg = safeLoad(RES + "btn_emergency.png"); // Group44

        // ✅ 추가: 그라데이션
        gradLowImg  = safeLoad(RES + "grad_low.png");      // Rectangle7
        gradHighImg = safeLoad(RES + "grad_high.png");     // Group45

        if (heartNormal == null) heartNormal = heartFallback;
        if (heartLow == null)    heartLow = heartFallback;
        if (heartHigh == null)   heartHigh = heartFallback;
    }

    private javafx.scene.image.Image safeLoad(String path) {
        try {
            File f = new File(path);
            if (!f.exists()) {
                System.out.println("[WARN] File not found: " + path);
                return null;
            }
            return new javafx.scene.image.Image(f.toURI().toString());
        } catch (Exception e) {
            System.out.println("[WARN] Failed to load: " + path + " / " + e.getMessage());
            return null;
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
