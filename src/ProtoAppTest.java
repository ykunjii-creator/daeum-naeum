import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;


public class ProtoAppTest extends Application {

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

    // ===== 화면 전환용 =====
    private StackPane contentHolder;
    private Pane homePane, guidePane, reportPane, profilePane;


    // ====== "손 제외, 빨간 하트만" 기준으로 맞추기 위한 메트릭 ======
    static class HeartMetrics {
        final double imgW, imgH;
        final double heartMinX, heartMaxX, heartMinY, heartMaxY;

        HeartMetrics(double imgW, double imgH, double minX, double maxX, double minY, double maxY) {
            this.imgW = imgW; this.imgH = imgH;
            this.heartMinX = minX; this.heartMaxX = maxX;
            this.heartMinY = minY; this.heartMaxY = maxY;
        }

        double heartW() { return heartMaxX - heartMinX; }
        double heartCenterX() { return (heartMinX + heartMaxX) / 2.0; }
    }

    private final Map<Image, HeartMetrics> heartMetrics = new HashMap<>();

    // ✅ NORMAL의 "하트(빨강)" 폭이 화면에서 이 정도로 보이도록 맞춤
    //    (너가 NORMAL에서 fitWidth=260이 기준이라 했으니, 그 기준으로 자동 계산됨)
    private double targetHeartDisplayW = -1;

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
        // fitWidth/center/translate는 상태별로 applyHeartImage()에서 자동 처리
        AnchorPane.setTopAnchor(heartView, 365.0);

        // (6) 하단 탭바
        tabBar = buildTabBar(stage);
        AnchorPane.setBottomAnchor(tabBar, 36.0);
        centerXNode(tabBar, 320);
        // (6) 하단 탭바

        // 🔥 여기부터 추가 (④번)
        // ===============================
        contentHolder = new StackPane();
        contentHolder.setPrefSize(PHONE_W, PHONE_H);
        AnchorPane.setTopAnchor(contentHolder, 0.0);
        AnchorPane.setLeftAnchor(contentHolder, 0.0);
        AnchorPane.setRightAnchor(contentHolder, 0.0);
        AnchorPane.setBottomAnchor(contentHolder, 0.0);

        homePane = buildHomePane();
        reportPane = buildReportPane();
        guidePane = simplePlaceholder("Guide screen");
        profilePane = simplePlaceholder("Profile screen");

        contentHolder.getChildren().addAll(
            homePane, reportPane, guidePane, profilePane
        );

        showScreen(homePane);

        // ❗ canvas에는 이제 contentHolder + tabBar만
        canvas.getChildren().addAll(contentHolder, tabBar);
        // ===============================
        // 🔥 여기까지
        // ===============================

        root.getChildren().add(canvas);
        StackPane.setAlignment(canvas, Pos.CENTER_LEFT);
        
        // ✅ Scene 크기를 "폰 + 패딩*2"로 딱 맞추기 (검정/흰 비율 정상화)
        double sceneW = PHONE_W + OUTER_PAD * 2;
        double sceneH = PHONE_H + OUTER_PAD * 2;
        Scene scene = new Scene(root, sceneW, sceneH);

        stage.setTitle("Prototype (Heart Mid) - V2");
        stage.setScene(scene);
        stage.setResizable(false);
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

    private void centerXNode(Region r, double w) {
    r.setPrefWidth(w);
    AnchorPane.setLeftAnchor(r, (PHONE_W - w) / 2.0);
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
                double w = 72 * 0.7;   // 고심박 경고등 0.7배
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

        // ✅ 핵심: "손 제외(빨간 하트만)" 기준으로 3장 크기/중앙 자동 맞춤
        switch (state) {
            case NORMAL -> {
                applyHeartImage(heartNormal, 365.0, 260.0); // NORMAL은 네가 원한 기준 fitWidth=260에서 타겟 자동 산출
                gradientView.setVisible(false);
                ecgView.setOpacity(0.65);
            }
            case LOW -> {
                applyHeartImage(heartLow, 365.0, null);     // 나머지는 타겟 하트폭에 자동 맞춤
                if (gradLow != null) {
                    gradientView.setImage(gradLow);
                    gradientView.setVisible(true);
                } else gradientView.setVisible(false);
                ecgView.setOpacity(0.80);
            }
            case HIGH -> {
                applyHeartImage(heartHigh, 365.0, null);    // 나머지는 타겟 하트폭에 자동 맞춤
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
        HBox bar = new HBox(6);
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(10));
        bar.setPrefWidth(320);
        bar.setPrefHeight(76);

        bar.setBackground(new Background(
                new BackgroundFill(Color.web("#F7F7F7"), new CornerRadii(24), Insets.EMPTY)));
        bar.setEffect(new DropShadow(18, Color.web("#000000", 0.14)));

        tabHome = tabButton("Home");
        tabGuide = tabButton("Guide");
        tabReport = tabButton("Report");
        tabProfile = tabButton("Profile");

        

        tabHome.setOnAction(e -> {
            setActiveTab(tabHome);
            showScreen(homePane);
        });

        tabGuide.setOnAction(e -> {
            setActiveTab(tabGuide);
            showScreen(guidePane);
        });

        tabReport.setOnAction(e -> {
            setActiveTab(tabReport);
            showScreen(reportPane);
        });

        tabProfile.setOnAction(e -> {
            setActiveTab(tabProfile);
            showScreen(profilePane);
        });


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
        b.setPrefHeight(48);
        b.setMinWidth(0);
        b.setFocusTraversable(false);
        b.setStyle("""
            -fx-background-color: transparent;
            -fx-text-fill: #8B8B8B;
            -fx-font-size: 14px;
            -fx-font-weight: 700;
            -fx-background-radius: 16px;
        """);
        return b;
    }

    private void setActiveTab(Button active) {
        for (Button b : new Button[]{tabHome, tabGuide, tabReport, tabProfile}) {
            b.setStyle("""
                -fx-background-color: transparent;
                -fx-text-fill: #8B8B8B;
                -fx-font-size: 14px;
                -fx-font-weight: 700;
                -fx-background-radius: 18px;
            """);
        }
        active.setStyle("""
            -fx-background-color: #111111;
            -fx-text-fill: white;
            -fx-font-size: 14px;
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

        // ✅ 빨간 하트(손 제외) 영역 메트릭 사전 계산
        if (heartNormal != null) heartMetrics.put(heartNormal, computeHeartMetrics(heartNormal));
        if (heartLow != null)    heartMetrics.put(heartLow, computeHeartMetrics(heartLow));
        if (heartHigh != null)   heartMetrics.put(heartHigh, computeHeartMetrics(heartHigh));

        // ✅ 타겟 하트폭은 NORMAL 첫 적용 시 자동 설정됨
        targetHeartDisplayW = -1;
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

    // ====== 핵심: 빨간 하트 영역(손 제외) 바운딩 계산 ======
    private HeartMetrics computeHeartMetrics(Image img) {
        if (img == null) return null;
        PixelReader pr = img.getPixelReader();
        if (pr == null) return null;

        int w = (int) img.getWidth();
        int h = (int) img.getHeight();

        int minX = w, minY = h, maxX = -1, maxY = -1;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Color c = pr.getColor(x, y);
                if (c.getOpacity() < 0.05) continue;

                // 빨강 판정 (필요하면 임계값만 살짝 조절)
                boolean isRed = c.getRed() > 0.65 && c.getGreen() < 0.35 && c.getBlue() < 0.35;

                if (isRed) {
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }

        if (maxX < 0) return null; // 빨강 못 찾음
        return new HeartMetrics(w, h, minX, maxX, minY, maxY);
    }

    /**
     * 이미지 편집 없이:
     * - "빨간 하트" 폭을 NORMAL 기준으로 동일하게 맞추고
     * - "빨간 하트" 중심이 화면 정중앙이 되도록 자동 translateX 보정
     *
     * @param img                표시할 이미지
     * @param topAnchor          하트 Y 위치
     * @param normalFitWidthHint NORMAL일 때만: 기존 너가 쓰던 fitWidth(예: 260). 타겟 폭 산출용.
     *                           LOW/HIGH에서는 null로 호출하면 됨.
     */
    private void applyHeartImage(Image img, double topAnchor, Double normalFitWidthHint) {
        if (img == null) return;

        heartView.setImage(img);
        HeartMetrics m = heartMetrics.get(img);

        // 메트릭 없으면 기존 방식 fallback
        if (m == null || m.heartW() <= 1) {
            double fallbackW = (normalFitWidthHint != null) ? normalFitWidthHint : 260.0;
            heartView.setFitWidth(fallbackW);
            centerX(heartView, fallbackW);
            heartView.setTranslateX(0);
            AnchorPane.setTopAnchor(heartView, topAnchor);
            return;
        }

        // NORMAL 기준 타겟 "하트(빨강)" 폭 설정
        if (targetHeartDisplayW < 0) {
            double hint = (normalFitWidthHint != null) ? normalFitWidthHint : 260.0;
            double scale = hint / m.imgW;
            targetHeartDisplayW = m.heartW() * scale;
        }

        // 이 이미지가 targetHeartDisplayW로 보이도록 fitWidth 계산
        double fitW = targetHeartDisplayW * (m.imgW / m.heartW());
        heartView.setFitWidth(fitW);

        // 빨간 하트 중심이 정중앙이 되도록 translateX 계산
        double scale = fitW / m.imgW;
        double imgCenterX = m.imgW / 2.0;
        double heartCenterX = m.heartCenterX();
        double dx = (imgCenterX - heartCenterX) * scale;

        centerX(heartView, fitW);
        heartView.setTranslateX(dx);
        AnchorPane.setTopAnchor(heartView, topAnchor);
    }

    // ====== 기존 유틸 ======
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

    private Pane buildHomePane() {
    AnchorPane p = new AnchorPane();
    p.setPrefSize(PHONE_W, PHONE_H);

    // ⚠️ 여기 안으로 ↓↓↓ 기존 start()에서 만들던 UI 코드 전부 이동
    // gradientView = new ImageView();
    // ecgView = new ImageView();
    // topIconView = new ImageView();
    // bpmNumberText = new Text(...);
    // bpmUnitText = new Text(...);
    // heartView = new ImageView();

    p.getChildren().addAll(
        gradientView, ecgView, topIconView,
        bpmNumberText, bpmUnitText,
        heartView
    );

    return p;
    }

    private Pane simplePlaceholder(String text) {
        StackPane p = new StackPane();
        p.setPrefSize(PHONE_W, PHONE_H);
        Text t = new Text(text);
        t.setFill(Color.web("#666666"));
        t.setFont(Font.font("System", FontWeight.BOLD, 18));
        p.getChildren().add(t);
    return p;
    }

    private void showScreen(Pane target) {
    for (javafx.scene.Node n : contentHolder.getChildren()) {
        n.setVisible(false);
    }
    target.setVisible(true);
    target.toFront();
    }

    private Pane buildReportPane() {
        AnchorPane p = new AnchorPane();
        p.setPrefSize(PHONE_W, PHONE_H);
        p.setBackground(new Background(
                new BackgroundFill(Color.web("#ECECEC"), CornerRadii.EMPTY, Insets.EMPTY)));

        // ===== 상단 코랄 헤더 =====
        double headerH = 92;

        Rectangle header = new Rectangle(PHONE_W, headerH);
        header.setFill(Color.web("#FF6B6B"));
        AnchorPane.setTopAnchor(header, 0.0);
        AnchorPane.setLeftAnchor(header, 0.0);

        Text title = new Text("HeartRate");
        title.setFill(Color.WHITE);
        title.setFont(Font.font("System", FontWeight.BOLD, 18));
        AnchorPane.setTopAnchor(title, 40.0);
        AnchorPane.setLeftAnchor(title, (PHONE_W - 85) / 2.0);

        // 햄버거(아이콘 이미지 없으니 텍스트로 대체)
        Text menu = new Text("≡");
        menu.setFill(Color.WHITE);
        menu.setFont(Font.font("System", FontWeight.BOLD, 40));
        AnchorPane.setTopAnchor(menu, 34.0);
        AnchorPane.setLeftAnchor(menu, 18.0);

        // ===== Measure / Statistics 상단 탭 =====
        HBox topTabs = new HBox(0);
        topTabs.setAlignment(Pos.CENTER);
        topTabs.setPrefHeight(54);
        topTabs.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
        topTabs.setTranslateX(-40); // ✅ 왼쪽으로 14px 이동 (원하는 만큼 -10 ~ -20 조절)
        AnchorPane.setTopAnchor(topTabs, headerH);
        AnchorPane.setLeftAnchor(topTabs, 0.0);
        AnchorPane.setRightAnchor(topTabs, 0.0);

        Button tabMeasure = topTabButton("Measure");
        Button tabStats   = topTabButton("Statistics");

        // underline (활성 표시)
        Rectangle underline = new Rectangle(70, 3, Color.web("#FF6B6B"));
        underline.setArcWidth(6);
        underline.setArcHeight(6);

        StackPane measureWrap = new StackPane(tabMeasure);
        StackPane statsWrap   = new StackPane(tabStats);

        // underline 위치용: 각 탭 아래쪽에 붙임
        StackPane measureCell = new StackPane();
        measureCell.setPrefWidth(PHONE_W / 2.0);
        measureCell.getChildren().addAll(measureWrap);
        StackPane.setAlignment(measureWrap, Pos.CENTER);

        StackPane statsCell = new StackPane();
        statsCell.setPrefWidth(PHONE_W / 2.0);
        statsCell.getChildren().addAll(statsWrap);
        StackPane.setAlignment(statsWrap, Pos.CENTER);

        topTabs.getChildren().addAll(measureCell, statsCell);

        // ===== 콘텐츠 스왑(Measure 화면 vs Statistics 화면) =====
        StackPane tabContent = new StackPane();
        AnchorPane.setTopAnchor(tabContent, headerH + 54);
        AnchorPane.setLeftAnchor(tabContent, 0.0);
        AnchorPane.setRightAnchor(tabContent, 0.0);
        // 하단 탭바 공간 피하기
        AnchorPane.setBottomAnchor(tabContent, 130.0);

        Pane measurePane = buildReportMeasurePane();   // 레퍼런스 왼쪽 화면 느낌
        Pane statsPane   = buildReportStatsPane();     // 레퍼런스 오른쪽 화면 느낌

        tabContent.getChildren().addAll(measurePane, statsPane);

        // 기본은 Statistics 탭 활성 (너가 원하는 Report 탭이 통계 느낌이라)
        setTopTabsActive(tabMeasure, tabStats, false);
        measurePane.setVisible(false);
        statsPane.setVisible(true);

        // underline을 Statistics 아래로
        statsCell.getChildren().add(underline);
        StackPane.setAlignment(underline, Pos.BOTTOM_CENTER);
        StackPane.setMargin(underline, new Insets(0, 0, 6, 0));

        tabMeasure.setOnAction(e -> {
            setTopTabsActive(tabMeasure, tabStats, true);
            // underline 이동
            statsCell.getChildren().remove(underline);
            if (!measureCell.getChildren().contains(underline)) measureCell.getChildren().add(underline);
            StackPane.setAlignment(underline, Pos.BOTTOM_CENTER);
            StackPane.setMargin(underline, new Insets(0, 0, 6, 0));

            measurePane.setVisible(true);
            statsPane.setVisible(false);
        });

        tabStats.setOnAction(e -> {
            setTopTabsActive(tabMeasure, tabStats, false);
            // underline 이동
            measureCell.getChildren().remove(underline);
            if (!statsCell.getChildren().contains(underline)) statsCell.getChildren().add(underline);
            StackPane.setAlignment(underline, Pos.BOTTOM_CENTER);
            StackPane.setMargin(underline, new Insets(0, 0, 6, 0));

            measurePane.setVisible(false);
            statsPane.setVisible(true);
        });

        p.getChildren().addAll(header, title, menu, topTabs, tabContent);
        return p;
    }

    private Pane buildReportMeasurePane() {
        AnchorPane p = new AnchorPane();
        p.setPrefSize(PHONE_W, PHONE_H);

        // 큰 하트(이미지 없으니 심플하게 원+하트 느낌)
        StackPane heartBadge = new StackPane();
        heartBadge.setPrefSize(220, 220);

        // 연한 원 3겹
        heartBadge.getChildren().addAll(
                circle(210, "#FFD7D7", 0.35),
                circle(170, "#FFD7D7", 0.55),
                circle(120, "#FF6B6B", 0.90)
        );

        Text heart = new Text("❤");
        heart.setFill(Color.WHITE);
        heart.setFont(Font.font("System", FontWeight.BOLD, 36));
        heart.setTranslateY(7);   // ✅ 아래로 살짝
        heart.setTranslateX(0);   // 필요하면 +1 ~ +2 정도
        heartBadge.getChildren().add(heart);

        AnchorPane.setTopAnchor(heartBadge, 55.0);
        AnchorPane.setLeftAnchor(heartBadge, (PHONE_W - 220) / 2.0);

        Text bpmBig = new Text("072");
        bpmBig.setFill(Color.web("#050505ff"));
        bpmBig.setFont(Font.font("System", FontWeight.BOLD, 56));
        AnchorPane.setTopAnchor(bpmBig, 295.0);
        // AnchorPane.setLeftAnchor(bpmBig, (PHONE_W - 120) / 2.0);

        Text bpmSub = new Text("beats per minute");
        bpmSub.setFill(Color.web("#050505ff"));
        bpmSub.setFont(Font.font("System", FontWeight.MEDIUM, 14));
        AnchorPane.setTopAnchor(bpmSub, 360.0);
        // AnchorPane.setLeftAnchor(bpmSub, (PHONE_W - 140) / 2.0);

        Platform.runLater(() -> {
            centerXText(bpmBig);
            bpmBig.setTranslateX(0);

            centerXText(bpmSub);
            bpmSub.setTranslateX(0);
        });


        // ECG 라인 재사용하면 예쁨
        ImageView ecg = new ImageView();
        if (ecgLine != null) ecg.setImage(ecgLine);

        // ✅ 세로로 더 길게(두껍게)
        ecg.setPreserveRatio(false);
        ecg.setFitWidth(PHONE_W - 80);
        ecg.setFitHeight(90);     // ✅ 기존 대비 체감 3배 느낌 (원하면 80~110 조절)
        ecg.setOpacity(0.55);

        AnchorPane.setTopAnchor(ecg, 420.0);
        AnchorPane.setLeftAnchor(ecg, 40.0);

        p.getChildren().addAll(heartBadge, bpmBig, bpmSub, ecg);
        return p;
    }
    
    private Pane buildReportStatsPane() {
        double contentW = PHONE_W - 70;          // ✅ 아래 카드랑 동일 폭
        double contentLeft = (PHONE_W - contentW) / 2.0;  // ✅ 둘 다 같은 leftAnchor

        AnchorPane p = new AnchorPane();
        p.setPrefSize(PHONE_W, PHONE_H);

       // 세그먼트 컨트롤 (Day/Month/Year)
        HBox seg = new HBox(0);  // ✅ 간격 0 (버튼을 1/3로 딱 나눌거라)
        seg.setAlignment(Pos.CENTER);
        seg.setPadding(new Insets(8, 10, 8, 10));
        seg.setPrefWidth(contentW);   // ✅ 폭 고정 (카드랑 동일)
        seg.setMaxWidth(contentW);

        seg.setBackground(new Background(
                new BackgroundFill(Color.WHITE, new CornerRadii(18), Insets.EMPTY)));
        seg.setEffect(new DropShadow(10, Color.web("#000000", 0.08)));

        Button day = segBtn("Day", true);
        Button month = segBtn("Month", false);
        Button year = segBtn("Year", false);

        // ✅ 3개 버튼을 동일 폭으로 강제 분배
        for (Button b : new Button[]{day, month, year}) {
            b.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(b, Priority.ALWAYS);
        }

        seg.getChildren().addAll(day, month, year);

        // ✅ seg 위치: 카드랑 완전 동일한 left 기준
        AnchorPane.setTopAnchor(seg, 26.0);
        AnchorPane.setLeftAnchor(seg, contentLeft);

        // ❗️여기서 rightAnchor는 절대 주지 말기 (left+right 같이 주면 다시 늘어나/밀려)


        // 차트 카드
        VBox card = new VBox(12);
        card.setPadding(new Insets(14));
        card.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(18), Insets.EMPTY)));
        card.setEffect(new DropShadow(12, Color.web("#000000", 0.10)));



        // LineChart
        NumberAxis yAxis = new NumberAxis(50, 160, 10);
        NumberAxis xAxis = new NumberAxis(0, 24, 3);

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setHorizontalGridLinesVisible(true);
        chart.setVerticalGridLinesVisible(false);
        chart.setAlternativeRowFillVisible(false);
        chart.setAlternativeColumnFillVisible(false);
        chart.setPrefHeight(260);
        chart.setMinHeight(260);

        XYChart.Series<Number, Number> s = new XYChart.Series<>();
        int[] values = {72, 120, 95, 80, 60, 55, 70, 85, 90, 88, 92, 110, 140, 120, 100, 95, 90, 105, 115, 130, 150, 120, 100, 85, 72};
        for (int i = 0; i < values.length; i++) s.getData().add(new XYChart.Data<>(i, values[i]));
        chart.getData().add(s);

        // ✅ 차트 선 색/두께 (기본 파란색 없애기)
        Platform.runLater(() -> {
            // series line
            if (s.getNode() != null) {
                s.getNode().setStyle("-fx-stroke: #FF6B6B; -fx-stroke-width: 2.2px;");
            }
            // gridline/axis 톤 다운
            chart.lookupAll(".chart-horizontal-grid-lines line").forEach(n ->
                    n.setStyle("-fx-stroke: rgba(0,0,0,0.08);"));
            chart.lookupAll(".axis").forEach(n ->
                    n.setStyle("-fx-tick-label-fill: #9A9A9A; -fx-font-size: 10px;"));
        });

        // Min / Max / Avg
        HBox stats = new HBox(50);
        stats.setAlignment(Pos.CENTER);
        stats.setTranslateY(15);
        stats.getChildren().addAll(
                statBox("Min", "50"),
                statBox("Max", "157"),
                statBox("Avg", "81")
        );

        card.getChildren().addAll(chart, stats);
        VBox.setMargin(chart, new Insets(15, 0, 0, -5));


        double cardW = PHONE_W - 70; // ✅ 폭 줄이기(= 320 정도)
        card.setPrefWidth(cardW);
        AnchorPane.setLeftAnchor(card, (PHONE_W - cardW) / 2.0);
        AnchorPane.setRightAnchor(card, null);

        // ✅ 세로로 더 길게 보이게
        AnchorPane.setTopAnchor(card, 90.0);
        AnchorPane.setBottomAnchor(card, 8.0); // 아래쪽 여유 조금

        // AnchorPane.setLeftAnchor(card, 26.0);
        // AnchorPane.setRightAnchor(card, 26.0);
        // AnchorPane.setBottomAnchor(card, 0.0);

        chart.setPrefHeight(320);  // ✅ 260 -> 320
        chart.setMinHeight(320);


        // 세그먼트 클릭시 활성 스타일만 바꿔주기(데이터 스왑은 나중에 붙이면 됨)
        day.setOnAction(e -> setSegActive(day, month, year));
        month.setOnAction(e -> setSegActive(month, day, year));
        year.setOnAction(e -> setSegActive(year, day, month));

        p.getChildren().addAll(seg, card);
        return p;
    }
    private Button topTabButton(String text) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setFocusTraversable(false);
        b.setBackground(Background.EMPTY);
        b.setStyle("""
            -fx-background-color: transparent;
            -fx-text-fill: #8B8B8B;
            -fx-font-size: 14px;
            -fx-font-weight: 700;
        """);
        return b;
    }

    private void setTopTabsActive(Button measure, Button stats, boolean measureActive) {
        if (measureActive) {
            measure.setStyle("""
                -fx-background-color: transparent;
                -fx-text-fill: #333333;
                -fx-font-size: 14px;
                -fx-font-weight: 800;
            """);
            stats.setStyle("""
                -fx-background-color: transparent;
                -fx-text-fill: #8B8B8B;
                -fx-font-size: 14px;
                -fx-font-weight: 700;
            """);
        } else {
            stats.setStyle("""
                -fx-background-color: transparent;
                -fx-text-fill: #333333;
                -fx-font-size: 14px;
                -fx-font-weight: 800;
            """);
            measure.setStyle("""
                -fx-background-color: transparent;
                -fx-text-fill: #8B8B8B;
                -fx-font-size: 14px;
                -fx-font-weight: 700;
            """);
        }
    }

    private Button segBtn(String t, boolean active) {
        Button b = new Button(t);
        b.setFocusTraversable(false);
        b.setPrefHeight(30);
        b.setMinWidth(70);
        b.setStyle(active
                ? "-fx-background-color: #FF6B6B; -fx-text-fill: white; -fx-background-radius: 14px; -fx-font-weight: 800;"
                : "-fx-background-color: transparent; -fx-text-fill: #777777; -fx-background-radius: 14px; -fx-font-weight: 700;"
        );
        return b;
    }

    private void setSegActive(Button active, Button other1, Button other2) {
        active.setStyle("-fx-background-color: #FF6B6B; -fx-text-fill: white; -fx-background-radius: 14px; -fx-font-weight: 800;");
        other1.setStyle("-fx-background-color: transparent; -fx-text-fill: #777777; -fx-background-radius: 14px; -fx-font-weight: 700;");
        other2.setStyle("-fx-background-color: transparent; -fx-text-fill: #777777; -fx-background-radius: 14px; -fx-font-weight: 700;");
    }

    private VBox statBox(String label, String value) {
        Text l = new Text(label);
        l.setFill(Color.web("#888888"));
        l.setFont(Font.font("System", FontWeight.MEDIUM, 16));

        Text v = new Text(value);
        v.setFill(Color.web("#222222"));
        v.setFont(Font.font("System", FontWeight.BOLD, 24));

        VBox box = new VBox(6, l, v);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private StackPane circle(double size, String hex, double opacity) {
        javafx.scene.shape.Circle c = new javafx.scene.shape.Circle(size / 2.0);
        c.setFill(Color.web(hex, opacity));
        StackPane sp = new StackPane(c);
        sp.setPrefSize(size, size);
        return sp;
    }


}
