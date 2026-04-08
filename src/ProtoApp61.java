import java.io.File;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ProtoApp61 extends Application {

    // ===== 6.1" 느낌 비율 =====
    private static final double PHONE_W = 390;
    private static final double PHONE_H = 844;

    // PC에서 보기 좋게 바깥 여백 포함
    private static final double WINDOW_W = 520;
    private static final double WINDOW_H = 980;

    private static final String RES = "./resources/";

    enum State { NORMAL, LOW, HIGH }
    private final State[] cycle = { State.NORMAL, State.LOW, State.HIGH };
    private int cycleIdx = 0;

    // images
    private Image heartNormal, heartLow, heartHigh, heartFallback;
    private Image ecgLine;
    private Image iconWarn, iconSiren;

    private Image bpmNormalImg, bpmLowImg, bpmHighImg;

    private Image btnShelterImg, btnEmergencyImg;

    // UI nodes
    private ImageView heartView;
    private ImageView ecgView;
    private ImageView topIconView;
    private ImageView bpmImgView;

    private Pane rightPanel;

    // bottom tabs
    private Button tabHome, tabGuide, tabReport, tabProfile;

    // gradient tint (네가 말한 “조금 오른쪽” 이동)
    private Region bottomTint;

    @Override
    public void start(Stage stage) {
        loadImages();

        StackPane root = new StackPane();
        root.setPadding(new Insets(18));
        root.setBackground(new Background(new BackgroundFill(Color.web("#1C1C1E"), CornerRadii.EMPTY, Insets.EMPTY)));

        AnchorPane phone = new AnchorPane();
        phone.setPrefSize(PHONE_W, PHONE_H);
        phone.setBackground(new Background(new BackgroundFill(Color.web("#ECECEC"), new CornerRadii(22), Insets.EMPTY)));
        phone.setEffect(new DropShadow(26, Color.web("#000000", 0.35)));

        // ===== (A) 아래쪽 그라데이션(코드로만) =====
        bottomTint = new Region();
        bottomTint.setPrefSize(PHONE_W * 0.78, PHONE_H * 0.33); // 덩어리 조금 작게
        bottomTint.setBackground(new Background(new BackgroundFill(
                Color.web("#FFD7A8", 0.55), new CornerRadii(22), Insets.EMPTY
        )));
        bottomTint.setOpacity(0.55);

        // ↓ “조금 더 오른쪽”으로: LeftAnchor 값을 더 크게(오른쪽으로 이동)
        AnchorPane.setLeftAnchor(bottomTint, PHONE_W * 0.12);   // 핵심: 기존 0이었는데 오른쪽으로 민 것
        AnchorPane.setBottomAnchor(bottomTint, 0.0);

        // ===== (B) 상단 아이콘 =====
        topIconView = new ImageView();
        topIconView.setPreserveRatio(true);
        topIconView.setFitWidth(54);
        AnchorPane.setTopAnchor(topIconView, 78.0);
        AnchorPane.setLeftAnchor(topIconView, (PHONE_W - 54) / 2.0);

        // ===== (C) BPM 이미지 (숫자+bpm) ↓ 조금 아래로 =====
        bpmImgView = new ImageView();
        bpmImgView.setPreserveRatio(true);
        bpmImgView.setFitWidth(250);

        // 기존보다 살짝 아래로 내림: 136 -> 156
        AnchorPane.setTopAnchor(bpmImgView, 156.0);
        AnchorPane.setLeftAnchor(bpmImgView, (PHONE_W - 250) / 2.0);

        // ===== (D) ECG =====
        ecgView = new ImageView();
        ecgView.setPreserveRatio(false);
        ecgView.setFitWidth(PHONE_W);
        ecgView.setFitHeight(64);
        ecgView.setOpacity(0.9);
        if (ecgLine != null) ecgView.setImage(ecgLine);

        // BPM 내렸으니 ECG도 살짝 아래로
        AnchorPane.setTopAnchor(ecgView, 290.0);
        AnchorPane.setLeftAnchor(ecgView, 0.0);

        // ===== (E) 하트 =====
        heartView = new ImageView();
        heartView.setPreserveRatio(true);
        heartView.setFitWidth(230);

        // ECG 내렸으니 하트도 살짝 아래로
        AnchorPane.setTopAnchor(heartView, 392.0);
        AnchorPane.setLeftAnchor(heartView, (PHONE_W - 230) / 2.0);

        // ===== (F) 오른쪽 패널 =====
        rightPanel = buildRightPanel();
        AnchorPane.setRightAnchor(rightPanel, 16.0);
        AnchorPane.setTopAnchor(rightPanel, 190.0);

        // ===== (G) 하단 탭바 =====
        Pane tabBar = buildBottomTabBar();
        AnchorPane.setLeftAnchor(tabBar, 18.0);
        AnchorPane.setRightAnchor(tabBar, 18.0);
        AnchorPane.setBottomAnchor(tabBar, 22.0);

        // Assemble
        phone.getChildren().addAll(
                bottomTint,
                topIconView,
                bpmImgView,
                ecgView,
                heartView,
                rightPanel,
                tabBar
        );

        root.getChildren().add(phone);

        Scene scene = new Scene(root, WINDOW_W, WINDOW_H);
        stage.setTitle("ProtoApp (6.1 ratio 390x844)");
        stage.setScene(scene);
        stage.show();

        // init
        applyState(State.NORMAL);
        setActiveTab(tabHome);

        // cycle every 5s
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            cycleIdx = (cycleIdx + 1) % cycle.length;
            applyState(cycle[cycleIdx]);
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    // =================== Bottom Tab Bar ===================
    private Pane buildBottomTabBar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(12));
        bar.setPrefHeight(78);

        bar.setBackground(new Background(new BackgroundFill(
                Color.web("#F7F7F7", 0.96),
                new CornerRadii(22),
                Insets.EMPTY
        )));
        bar.setEffect(new DropShadow(12, Color.web("#000000", 0.14)));

        tabHome = tabButton("Home");
        tabGuide = tabButton("Guide");
        tabReport = tabButton("Report");
        tabProfile = tabButton("Profile");

        tabHome.setOnAction(e -> setActiveTab(tabHome));
        tabGuide.setOnAction(e -> setActiveTab(tabGuide));
        tabReport.setOnAction(e -> setActiveTab(tabReport));
        tabProfile.setOnAction(e -> setActiveTab(tabProfile));

        bar.getChildren().addAll(tabHome, tabGuide, tabReport, tabProfile);
        HBox.setHgrow(tabHome, Priority.ALWAYS);
        HBox.setHgrow(tabGuide, Priority.ALWAYS);
        HBox.setHgrow(tabReport, Priority.ALWAYS);
        HBox.setHgrow(tabProfile, Priority.ALWAYS);

        return bar;
    }

    private Button tabButton(String text) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setMinHeight(52);
        b.setFocusTraversable(false);
        b.setStyle("""
            -fx-background-color: transparent;
            -fx-text-fill: #6B6B6B;
            -fx-font-size: 14px;
            -fx-font-weight: 800;
            -fx-background-radius: 18px;
        """);
        return b;
    }

    private void setActiveTab(Button active) {
        Button[] all = {tabHome, tabGuide, tabReport, tabProfile};
        for (Button b : all) {
            b.setStyle("""
                -fx-background-color: transparent;
                -fx-text-fill: #6B6B6B;
                -fx-font-size: 14px;
                -fx-font-weight: 800;
                -fx-background-radius: 18px;
            """);
        }
        active.setStyle("""
            -fx-background-color: #111111;
            -fx-text-fill: white;
            -fx-font-size: 14px;
            -fx-font-weight: 900;
            -fx-background-radius: 18px;
        """);
    }

    // =================== Right Panel ===================
    private Pane buildRightPanel() {
        StackPane wrapper = new StackPane();
        wrapper.setPrefSize(160, 420);

        Region panel = new Region();
        panel.setPrefSize(160, 420);
        panel.setBackground(new Background(new BackgroundFill(
                Color.web("#FF3B30", 0.92),
                new CornerRadii(30),
                Insets.EMPTY
        )));
        panel.setEffect(new DropShadow(18, Color.web("#000000", 0.18)));

        Rectangle border = new Rectangle(160, 420);
        border.setArcWidth(60);
        border.setArcHeight(60);
        border.setFill(Color.TRANSPARENT);
        border.setStroke(Color.web("#FFFFFF", 0.65));
        border.setStrokeWidth(2);

        VBox content = new VBox(14);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(22, 14, 18, 14));

        javafx.scene.control.Label title = new javafx.scene.control.Label("맥박 수가\n떨어져요!");
        title.setTextFill(Color.WHITE);
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 900;");
        title.setAlignment(Pos.CENTER);

        javafx.scene.control.Label sub = new javafx.scene.control.Label("5초 후 자동 제어가\n시작됩니다.");
        sub.setTextFill(Color.web("#FFFFFF", 0.9));
        sub.setStyle("-fx-font-size: 11px; -fx-font-weight: 700;");
        sub.setAlignment(Pos.CENTER);

        StackPane shelterBtn = pillButton("인근 쉼터 안내");
        shelterBtn.setOnMouseClicked(e -> showShelterPopup());

        StackPane emergencyBtn = pillButton("응급 대응");
        emergencyBtn.setOnMouseClicked(e -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setHeaderText("응급 대응 (데모)");
            a.setContentText("여기서 119 / 보호자 연락 / 위치 전송 등을 연결하면 돼요.");
            a.showAndWait();
        });

        content.getChildren().addAll(title, spacer(8), sub, spacer(18), shelterBtn, emergencyBtn);

        wrapper.getChildren().addAll(panel, border, content);
        return wrapper;
    }

    private StackPane pillButton(String text) {
        StackPane wrap = new StackPane();
        wrap.setPrefSize(130, 64);

        Region bg = new Region();
        bg.setPrefSize(130, 64);
        bg.setBackground(new Background(new BackgroundFill(
                Color.web("#E41F1A", 0.95),
                new CornerRadii(22),
                Insets.EMPTY
        )));
        bg.setEffect(new DropShadow(10, Color.web("#000000", 0.14)));

        javafx.scene.control.Label label = new javafx.scene.control.Label(text);
        label.setTextFill(Color.WHITE);
        label.setStyle("-fx-font-size: 13px; -fx-font-weight: 900;");

        wrap.getChildren().addAll(bg, label);

        wrap.setOnMouseEntered(e -> wrap.setOpacity(0.92));
        wrap.setOnMouseExited(e -> wrap.setOpacity(1.0));
        return wrap;
    }

    private Region spacer(double h) {
        Region r = new Region();
        r.setMinHeight(h);
        return r;
    }

    private void showShelterPopup() {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText("인근 쉼터 안내 (데모)");
        a.setContentText("• 가장 가까운 졸음쉼터: 2.4km\n• 방향: 다음 교차로 우회전 → 직진 1.8km\n• 예상 도착: 약 5분");
        a.showAndWait();
    }

    // =================== State apply ===================
    private void applyState(State s) {
        switch (s) {
            case NORMAL -> {
                topIconView.setImage(null);

                if (bpmNormalImg != null) bpmImgView.setImage(bpmNormalImg);

                if (heartNormal != null) heartView.setImage(heartNormal);
                else if (heartFallback != null) heartView.setImage(heartFallback);

                // NORMAL일 때 아이콘 자리 남는 느낌 싫으면 hidden 처리도 가능
                topIconView.setFitWidth(54);
                AnchorPane.setLeftAnchor(topIconView, (PHONE_W - 54) / 2.0);
            }
            case LOW -> {
                if (iconWarn != null) topIconView.setImage(iconWarn);

                // ✅ 저심박 노란 삼각형 더 크게
                topIconView.setFitWidth(72);
                AnchorPane.setLeftAnchor(topIconView, (PHONE_W - 72) / 2.0);

                if (bpmLowImg != null) bpmImgView.setImage(bpmLowImg);
                else if (bpmNormalImg != null) bpmImgView.setImage(bpmNormalImg);

                if (heartLow != null) heartView.setImage(heartLow);
                else if (heartFallback != null) heartView.setImage(heartFallback);
            }
            case HIGH -> {
                if (iconSiren != null) topIconView.setImage(iconSiren);

                topIconView.setFitWidth(64);
                AnchorPane.setLeftAnchor(topIconView, (PHONE_W - 64) / 2.0);

                if (bpmHighImg != null) bpmImgView.setImage(bpmHighImg);
                else if (bpmNormalImg != null) bpmImgView.setImage(bpmNormalImg);

                if (heartHigh != null) heartView.setImage(heartHigh);
                else if (heartFallback != null) heartView.setImage(heartFallback);
            }
        }
    }

    // =================== Image load ===================
    private void loadImages() {
        heartNormal   = safeLoad(RES + "heart_normal.png");
        heartLow      = safeLoad(RES + "heart_low.png");
        heartHigh     = safeLoad(RES + "heart_high.png");
        heartFallback = safeLoad(RES + "heart.png");

        ecgLine = safeLoad(RES + "ecg_line.png");

        iconWarn  = safeLoad(RES + "icon_warning.png");
        iconSiren = safeLoad(RES + "icon_siren.png");

        bpmLowImg    = safeLoad(RES + "bpm_low.png");
        bpmNormalImg = safeLoad(RES + "bpm_normal.png");
        bpmHighImg   = safeLoad(RES + "bpm_high.png");

        btnShelterImg   = safeLoad(RES + "btn_shelter.png");
        btnEmergencyImg = safeLoad(RES + "btn_emergency.png");
    }

    private Image safeLoad(String path) {
        try {
            File f = new File(path);
            if (!f.exists()) return null;
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
