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

public class EcgDashboardFinal extends Application {

    enum State { NORMAL, LOW, HIGH }

    private static final String RES = "resources/";

    // 5초마다 NORMAL → LOW → HIGH 순환
    private final State[] cycle = { State.NORMAL, State.LOW, State.HIGH };
    private int idx = 0;

    // ===== 이미지 =====
    // hearts
    private Image heartNormal, heartLow, heartHigh, heartFallback;

    // layout assets
    private Image navRailImg;     // Group42
    private Image ecgLineImg;     // Union
    private Image gradLowImg;     // Rectangle7
    private Image gradHighImg;    // Group45

    // icons
    private Image warnImg;        // Group15
    private Image sirenImg;       // Group24

    // buttons
    private Image btnShelterImg;  // Group43
    private Image btnEmergencyImg;// Group44

    // BPM cards (Group46/47/48)
    private Image bpmLowImg;
    private Image bpmNormalImg;
    private Image bpmHighImg;

    // bpm unit (Group8) optional
    private Image bpmUnitImg;

    // right panel text images
    private Image txtAutoControl; // "5초 후 자동 제어..." (이미지)
    private Image txtLowTitle;    // "맥박 수가 떨어져요!"
    private Image txtHighTitle;   // "맥박 수가 너무 높아져요!"

    // ===== UI 노드 =====
    private StackPane stageRoot;
    private AnchorPane canvas;

    // top/vitals
    private ImageView iconTop;      // warn/siren
    private ImageView bpmCardView;  // bpm_low/normal/high
    private ImageView bpmUnitView;  // bpm_unit (optional)

    // center
    private ImageView ecgView;      // ecg line image
    private ImageView heartView;    // heart image

    // bottom gradient
    private ImageView gradView;

    // right panel
    private Pane rightPanel;
    private ImageView rightTitleImg;
    private ImageView autoControlImg;

    @Override
    public void start(Stage stage) {
        loadImages();

        // 바깥 배경(검정)
        stageRoot = new StackPane();
        stageRoot.setPadding(new Insets(18));
        stageRoot.setBackground(new Background(new BackgroundFill(Color.web("#1C1C1E"), CornerRadii.EMPTY, Insets.EMPTY)));

        // 앱 캔버스(회색)
        canvas = new AnchorPane();
        canvas.setPrefSize(1040, 620);
        canvas.setBackground(new Background(new BackgroundFill(Color.web("#ECECEC"), new CornerRadii(8), Insets.EMPTY)));
        canvas.setEffect(new DropShadow(24, Color.web("#000000", 0.35)));

        // 하단 그라데이션(LOW/HIGH에서만 보임)
        gradView = new ImageView();
        gradView.setPreserveRatio(false);
        gradView.setFitWidth(1040);
        gradView.setFitHeight(230);
        gradView.setVisible(false);
        AnchorPane.setLeftAnchor(gradView, 0.0);
        AnchorPane.setBottomAnchor(gradView, 0.0);

        // 왼쪽 탭(이미지)
        ImageView navRail = new ImageView(navRailImg);
        navRail.setPreserveRatio(true);
        navRail.setFitHeight(420);
        AnchorPane.setLeftAnchor(navRail, 26.0);
        AnchorPane.setTopAnchor(navRail, 86.0);

        // ECG 라인(이미지)
        ecgView = new ImageView(ecgLineImg);
        ecgView.setPreserveRatio(false);
        ecgView.setFitWidth(1040);
        ecgView.setFitHeight(60);
        ecgView.setOpacity(0.9);
        ecgView.setVisible(false);
        AnchorPane.setLeftAnchor(ecgView, 0.0);
        AnchorPane.setTopAnchor(ecgView, 265.0);

        // 상단: 아이콘 + BPM 이미지 카드
        Pane vitals = buildVitalsImageBased();
        AnchorPane.setLeftAnchor(vitals, (1040 - 240) / 2.0);
        AnchorPane.setTopAnchor(vitals, 65.0);

        // 하트(이미지)
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
        stage.setTitle("ECG Dashboard (Final)");
        stage.setScene(scene);
        stage.show();

        // 초기 상태(원하면 NORMAL로 바꿔도 됨)
        applyState(State.LOW);

        // 5초마다 자동 변화
        Timeline t = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            idx = (idx + 1) % cycle.length;
            applyState(cycle[idx]);
        }));
        t.setCycleCount(Timeline.INDEFINITE);
        t.play();
    }

    // ===== Vitals (이미지 기반) =====
    private Pane buildVitalsImageBased() {
        AnchorPane p = new AnchorPane();
        p.setPrefSize(240, 180);

        iconTop = new ImageView();
        iconTop.setPreserveRatio(true);
        iconTop.setFitWidth(58);
        AnchorPane.setLeftAnchor(iconTop, (240 - 58) / 2.0);
        AnchorPane.setTopAnchor(iconTop, 0.0);

        bpmCardView = new ImageView();
        bpmCardView.setPreserveRatio(true);
        bpmCardView.setFitWidth(220);
        AnchorPane.setLeftAnchor(bpmCardView, (240 - 220) / 2.0);
        AnchorPane.setTopAnchor(bpmCardView, 52.0);

        // bpm 글자 이미지(선택)
        bpmUnitView = new ImageView();
        bpmUnitView.setPreserveRatio(true);
        bpmUnitView.setFitWidth(70);
        bpmUnitView.setImage(bpmUnitImg);
        AnchorPane.setLeftAnchor(bpmUnitView, (240 - 70) / 2.0);
        AnchorPane.setTopAnchor(bpmUnitView, 135.0);
        bpmUnitView.setVisible(bpmUnitImg != null);

        p.getChildren().addAll(iconTop, bpmCardView, bpmUnitView);
        return p;
    }

    // ===== Right Panel (제목/자동제어: 이미지) =====
    private Pane buildRightPanel() {
        StackPane panel = new StackPane();
        panel.setPrefSize(255, 500);
        panel.setBackground(new Background(new BackgroundFill(Color.web("#E30000"), new CornerRadii(44), Insets.EMPTY)));
        panel.setEffect(new DropShadow(18, Color.web("#000000", 0.22)));

        VBox inner = new VBox(14);
        inner.setAlignment(Pos.TOP_CENTER);
        inner.setPadding(new Insets(22));

        // 제목 이미지 (LOW/HIGH에서 바뀜)
        rightTitleImg = new ImageView();
        rightTitleImg.setPreserveRatio(true);
        rightTitleImg.setFitWidth(190);
        rightTitleImg.setImage(txtLowTitle); // 초기값

        Region spacer1 = new Region();
        spacer1.setPrefHeight(14);

        // "5초 후 자동 제어..." 이미지
        autoControlImg = new ImageView();
        autoControlImg.setPreserveRatio(true);
        autoControlImg.setFitWidth(190);
        autoControlImg.setImage(txtAutoControl);
        autoControlImg.setVisible(txtAutoControl != null);

        Region spacer2 = new Region();
        spacer2.setPrefHeight(18);

        // 버튼 이미지 2개
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

        inner.getChildren().addAll(rightTitleImg, spacer1, autoControlImg, spacer2, shelterBtnView, emergencyBtnView);
        panel.getChildren().add(inner);

        return panel;
    }

    // ===== 상태 적용 =====
    private void applyState(State s) {
        if (s == State.NORMAL) {
            // 상단 아이콘 숨김
            iconTop.setImage(null);

            // BPM 카드
            bpmCardView.setImage(bpmNormalImg);

            // 중앙 요소
            heartView.setImage(heartNormal != null ? heartNormal : heartFallback);

            // 경고 화면 요소 숨김
            gradView.setVisible(false);
            ecgView.setVisible(false);
            rightPanel.setVisible(false);

        } else if (s == State.LOW) {
            // 경고 아이콘
            iconTop.setImage(warnImg);

            // BPM 카드
            bpmCardView.setImage(bpmLowImg);

            // 하트
            heartView.setImage(heartLow != null ? heartLow : heartFallback);

            // 그라데이션/ECG/패널 표시
            gradView.setImage(gradLowImg);
            gradView.setVisible(true);

            ecgView.setVisible(true);
            rightPanel.setVisible(true);

            // 오른쪽 제목 이미지
            if (txtLowTitle != null) rightTitleImg.setImage(txtLowTitle);

        } else { // HIGH
            iconTop.setImage(sirenImg);

            bpmCardView.setImage(bpmHighImg);

            heartView.setImage(heartHigh != null ? heartHigh : heartFallback);

            gradView.setImage(gradHighImg);
            gradView.setVisible(true);

            ecgView.setVisible(true);
            rightPanel.setVisible(true);

            if (txtHighTitle != null) rightTitleImg.setImage(txtHighTitle);
        }
    }

    // ===== 팝업 =====
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

    // ===== 이미지 로드 =====
    private void loadImages() {
        // hearts
        heartFallback = safeLoad(RES + "heart.png");
        heartNormal   = safeLoad(RES + "heart_normal.png");
        heartLow      = safeLoad(RES + "heart_low.png");
        heartHigh     = safeLoad(RES + "heart_high.png");

        // nav & line & gradients
        navRailImg  = safeLoad(RES + "nav_rail.png");      // Group42
        ecgLineImg  = safeLoad(RES + "ecg_line.png");      // Union
        gradLowImg  = safeLoad(RES + "grad_low.png");      // Rectangle7
        gradHighImg = safeLoad(RES + "grad_high.png");     // Group45

        // icons
        warnImg  = safeLoad(RES + "icon_warning.png");     // Group15
        sirenImg = safeLoad(RES + "icon_siren.png");       // Group24

        // buttons
        btnShelterImg   = safeLoad(RES + "btn_shelter.png");    // Group43
        btnEmergencyImg = safeLoad(RES + "btn_emergency.png");  // Group44

        // BPM cards
        bpmLowImg    = safeLoad(RES + "bpm_low.png");       // Group46
        bpmNormalImg = safeLoad(RES + "bpm_normal.png");    // Group47
        bpmHighImg   = safeLoad(RES + "bpm_high.png");      // Group48
        

        // right panel texts
        txtAutoControl = safeLoad(RES + "txt_autocontrol.png");
        txtLowTitle    = safeLoad(RES + "txt_low_title.png");
        txtHighTitle   = safeLoad(RES + "txt_high_title.png");

        // fallback
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

    public static void main(String[] args) {
        launch();
    }
}
