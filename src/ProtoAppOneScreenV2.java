import java.io.File;
import java.util.Random;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ProtoAppOneScreenV2 extends Application {

    // ====== 화면 비율(6.1 느낌) ======
    private static final double PHONE_W = 390;
    private static final double PHONE_H = 844;
    private static final double RADIUS  = 28;

    // ✅ 바깥 검정 여백(원하는 만큼 조절)
    private static final double OUTER_PAD = 14;

    // ====== 리소스 폴더 ======
    private static final String RES = "./resources/";

    // ====== 상태(5초마다 순환) ======
    enum State { LOW, NORMAL, HIGH }
    private final State[] cycle = { State.NORMAL, State.LOW, State.HIGH };
    private int cycleIdx = 0;

    private final Random rng = new Random();

    // ====== 이미지 ======
    private Image heartNormal, heartLow, heartHigh;
    private Image gradLow, gradHigh;
    private Image iconWarn, iconSiren;
    private Image ecgLine;

    // ====== UI 노드 ======
    private AnchorPane canvas;

    private ImageView gradientView;
    private ImageView ecgView;
    private ImageView heartView;
    private ImageView topIconView;

    private Text bpmNumberText;
    private Text bpmUnitText;

    private HBox tabBar;
    private Button tabHome, tabGuide, tabReport, tabProfile;

    @Override
    public void start(Stage stage) {
        loadImages();

        // 바깥(검정 배경)
        StackPane root = new StackPane();
        root.setPadding(new Insets(OUTER_PAD));
        root.setBackground(new Background(
                new BackgroundFill(Color.web("#1C1C1E"), CornerRadii.EMPTY, Insets.EMPTY)));

        // 앱 캔버스
        canvas = new AnchorPane();
        canvas.setPrefSize(PHONE_W, PHONE_H);
        canvas.setBackground(new Background(
                new BackgroundFill(Color.web("#ECECEC"), new CornerRadii(RADIUS), Insets.EMPTY)));
        canvas.setEffect(new DropShadow(28, Color.web("#000000", 0.35)));

        // ✅ 캔버스 라운드 클립(밖으로 절대 안 삐져나감)
        Rectangle clip = new Rectangle(PHONE_W, PHONE_H);
        clip.setArcWidth(RADIUS * 2);
        clip.setArcHeight(RADIUS * 2);
        canvas.setClip(clip);

        // (1) 하단 그라데이션
        gradientView = new ImageView();
        gradientView.setPreserveRatio(false);
        gradientView.setFitWidth(PHONE_W + 160);
        gradientView.setFitHeight(260);
        AnchorPane.setLeftAnchor(gradientView, -80.0);
        AnchorPane.setBottomAnchor(gradientView, 0.0);
        gradientView.setVisible(false);

        // (2) ECG 라인
        ecgView = new ImageView();
        if (ecgLine != null) ecgView.setImage(ecgLine);
        ecgView.setPreserveRatio(true);
        ecgView.setFitWidth(PHONE_W + 80);
        AnchorPane.setLeftAnchor(ecgView, -40.0);
        AnchorPane.setTopAnchor(ecgView, 308.0);
        ecgView.setOpacity(0.75);

        // (3) 상단 아이콘(주의/경고)
        topIconView = new ImageView();
        topIconView.setPreserveRatio(true);
        topIconView.setFitWidth(70);
        AnchorPane.setTopAnchor(topIconView, 90.0);
        centerX(topIconView, 70);

        // (4) BPM 텍스트
        bpmNumberText = new Text("90");
        bpmNumberText.setFont(Font.font("System", FontWeight.BOLD, 64));
        bpmNumberText.setFill(Color.BLACK);
        AnchorPane.setTopAnchor(bpmNumberText, 155.0);

        bpmUnitText = new Text("bpm");
        bpmUnitText.setFont(Font.font("System", FontWeight.MEDIUM, 46));
        bpmUnitText.setFill(Color.web("#9A9A9A"));
        AnchorPane.setTopAnchor(bpmUnitText, 235.0);

        // (5) 하트
        heartView = new ImageView();
        heartView.setPreserveRatio(true);
        heartView.setFitWidth(260);
        AnchorPane.setTopAnchor(heartView, 365.0);
        centerX(heartView, 260);

        // (6) 하단 탭바
        tabBar = buildTabBar(stage);
        AnchorPane.setLeftAnchor(tabBar, 14.0);
        AnchorPane.setRightAnchor(tabBar, 14.0);
        AnchorPane.setBottomAnchor(tabBar, 26.0);
        tabBar.setTranslateX(-8);

        canvas.getChildren().addAll(
                gradientView, ecgView, topIconView,
                bpmNumberText, bpmUnitText,
                heartView, tabBar
        );

        root.getChildren().add(canvas);
        StackPane.setAlignment(canvas, Pos.CENTER_LEFT); // 왼쪽으로 이동


        // ✅ Scene 크기를 "폰 + 패딩*2"로 딱 맞추기 (검정/흰 비율 정상화)
        double sceneW = PHONE_W + OUTER_PAD * 2;
        double sceneH = PHONE_H + OUTER_PAD * 2;
        Scene scene = new Scene(root, sceneW, sceneH);

        stage.setTitle("Prototype (One Screen) - V2");
        stage.setScene(scene);
        stage.setResizable(false); // ✅ 비율 틀어지는 것 방지
        stage.sizeToScene();
        stage.show();

        // 초기 탭/상태
        setActiveTab(tabHome);
        applyState(cycle[cycleIdx]);

        // 5초마다 상태 순환
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            cycleIdx = (cycleIdx + 1) % cycle.length;
            applyState(cycle[cycleIdx]);
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void applyState(State state) {
        int bpm = generateBpmByState(state);

        bpmNumberText.setText(String.valueOf(bpm));
        bpmUnitText.setText("bpm");

        boolean isNormalRange = (bpm >= 80 && bpm <= 100);
        if (isNormalRange) {
            bpmNumberText.setFill(Color.BLACK);
            bpmUnitText.setFill(Color.web("#9A9A9A"));
        } else {
            bpmNumberText.setFill(Color.web("#E02020"));
            bpmUnitText.setFill(Color.web("#FF6B6B"));
        }

        // 아이콘 규칙
        topIconView.setImage(null);

        if (bpm >= 140) {
            if (iconSiren != null) {
                topIconView.setImage(iconSiren);

                // ✅ 요청: 고심박 경고등 0.7배 정도로 줄이기
                double w = 72 * 0.7;   // 원래 72였음
                topIconView.setFitWidth(w);
                centerX(topIconView, w);
            }
        } else if (bpm <= 50) {
            if (iconWarn != null) {
                topIconView.setImage(iconWarn);
                topIconView.setFitWidth(86);
                centerX(topIconView, 86);
            }
        }

        switch (state) {
            case NORMAL -> {
            if (heartNormal != null) heartView.setImage(heartNormal);

            double w = 260;            // ✅ NORMAL만 1.2배
            heartView.setFitWidth(w);
            centerX(heartView, w);           // ✅ 가운데 유지
            heartView.setTranslateX(7); // ✅ normal만 오른쪽으로 10px (원하는 만큼 조절)


            // (선택) 커지면 약간 위로 올리고 싶을 때만
            // AnchorPane.setTopAnchor(heartView, 355.0);

            gradientView.setVisible(false);
            ecgView.setOpacity(0.65);
        }

            case LOW -> {
            if (heartLow != null) heartView.setImage(heartLow);

            double w = 260 * 0.95;           // ✅ 0.8배
            heartView.setFitWidth(w);
            centerX(heartView, w);

            if (gradLow != null) {
                gradientView.setImage(gradLow);
                gradientView.setVisible(true);
            } else gradientView.setVisible(false);

            ecgView.setOpacity(0.80);
        }

            case HIGH -> {
            if (heartHigh != null) heartView.setImage(heartHigh);

            double w = 260 * 0.95;           // ✅ 0.8배
            heartView.setFitWidth(w);
            centerX(heartView, w);

            if (gradHigh != null) {
                gradientView.setImage(gradHigh);
                gradientView.setVisible(true);
            } else gradientView.setVisible(false);

            ecgView.setOpacity(0.85);
        }

    }

        // ✅ 텍스트 중앙 정렬(매번 갱신)
        centerXText(bpmNumberText);
        centerXText(bpmUnitText);
    }

    private int generateBpmByState(State state) {
        return switch (state) {
            case NORMAL -> rand(80, 100);
            case LOW    -> rand(30, 80);
            case HIGH   -> rand(101, 170);
        };
    }

    private int rand(int min, int maxInclusive) {
        return min + rng.nextInt(maxInclusive - min + 1);
    }

    private HBox buildTabBar(Stage owner) {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(14));
        bar.setPrefHeight(86);

        bar.setBackground(new Background(
                new BackgroundFill(Color.web("#F7F7F7"), new CornerRadii(26), Insets.EMPTY)));
        bar.setEffect(new DropShadow(18, Color.web("#000000", 0.14)));

        tabHome = tabButton("Home");
        tabGuide = tabButton("Guide");
        tabReport = tabButton("Report");
        tabProfile = tabButton("Profile");

        tabHome.setOnAction(e -> setActiveTab(tabHome));
        tabGuide.setOnAction(e -> setActiveTab(tabGuide));
        tabReport.setOnAction(e -> setActiveTab(tabReport));
        tabProfile.setOnAction(e -> setActiveTab(tabProfile));

        HBox.setHgrow(tabHome, Priority.ALWAYS);
        HBox.setHgrow(tabGuide, Priority.ALWAYS);
        HBox.setHgrow(tabReport, Priority.ALWAYS);
        HBox.setHgrow(tabProfile, Priority.ALWAYS);

        bar.getChildren().addAll(tabHome, tabGuide, tabReport, tabProfile);
        return bar;
    }

    private Button tabButton(String text) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setPrefHeight(56);
        b.setFocusTraversable(false);
        b.setStyle("""
            -fx-background-color: transparent;
            -fx-text-fill: #8B8B8B;
            -fx-font-size: 15px;
            -fx-font-weight: 700;
            -fx-background-radius: 18px;
        """);
        return b;
    }

    private void setActiveTab(Button active) {
        for (Button b : new Button[]{tabHome, tabGuide, tabReport, tabProfile}) {
            b.setStyle("""
                -fx-background-color: transparent;
                -fx-text-fill: #8B8B8B;
                -fx-font-size: 15px;
                -fx-font-weight: 700;
                -fx-background-radius: 18px;
            """);
        }
        active.setStyle("""
            -fx-background-color: #111111;
            -fx-text-fill: white;
            -fx-font-size: 15px;
            -fx-font-weight: 800;
            -fx-background-radius: 18px;
        """);
    }

    private void loadImages() {
        heartNormal = safeLoad(RES + "heart_normal.png");
        heartLow    = safeLoad(RES + "heart_low.png");
        heartHigh   = safeLoad(RES + "heart_high.png");

        iconWarn    = safeLoad(RES + "icon_warning.png");
        iconSiren   = safeLoad(RES + "icon_siren.png");

        gradLow     = safeLoad(RES + "grad_low.png");
        gradHigh    = safeLoad(RES + "grad_high.png");

        ecgLine     = safeLoad(RES + "ecg_line.png");
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

    private void centerX(ImageView v, double fitWidth) {
        AnchorPane.setLeftAnchor(v, (PHONE_W - fitWidth) / 2.0);
    }

    private void centerXText(Text t) {
        double w = t.getLayoutBounds().getWidth();
        AnchorPane.setLeftAnchor(t, (PHONE_W - w) / 2.0);
    }

    public static void main(String[] args) {
        launch();
    }
}
