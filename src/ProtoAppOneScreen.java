// 하트캐릭터 이미지 크기 맞추기
// 경고등이랑 주의표지판 크기 맞추기
// bpm 카드 이미지 조금 아래로
// bpm 위 숫자는 랜덤으로
// 저심박 고심박일땐 빨강, 정상심박일땐 검정
// 하단 그라데이션 오른쪽 여전히 약간 잘림 수정
// 뒤에 심박 라인 이미지 나오도록

//guide report profile 누르면 다른 창 뜨도록 (데모 이미지 없어서 당장 구현 불가)

import java.io.File;
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
import javafx.stage.Stage;
import javafx.util.Duration;

public class ProtoAppOneScreen extends Application {

    // ====== 상태(5초마다 자동 변경) ======
    enum State { NORMAL, LOW, HIGH }
    private final State[] cycle = { State.NORMAL, State.LOW, State.HIGH };
    private int cycleIdx = 0;

    // ====== 리소스 폴더 ======
    private static final String RES = "./resources/"; // 필요하면 "resources/"로 변경

    // ====== 이미지들(네가 이미 만들었던 이름 기준) ======
    private Image heartNormal, heartLow, heartHigh, heartFallback;
    private Image bpmNormal, bpmLow, bpmHigh;
    private Image gradLow, gradHigh;         // 선택(없어도 됨)
    private Image iconWarn, iconSiren;       // 선택(없어도 됨)

    // ====== UI 노드 ======
    private ImageView heartView;
    private ImageView bpmView;
    private ImageView topIconView;
    private ImageView gradientView;

    private HBox tabBar;
    private Button tabHome, tabGuide, tabReport, tabProfile;

    @Override
    public void start(Stage stage) {
        loadImages();

        // 바깥(검정 배경)
        StackPane root = new StackPane();
        root.setPadding(new Insets(18));
        root.setBackground(new Background(new BackgroundFill(Color.web("#1C1C1E"), CornerRadii.EMPTY, Insets.EMPTY)));

        // 앱 캔버스(한 장 화면)
        AnchorPane canvas = new AnchorPane();
        canvas.setPrefSize(420, 820); // 모바일 느낌(세로)
        canvas.setBackground(new Background(new BackgroundFill(Color.web("#ECECEC"), new CornerRadii(20), Insets.EMPTY)));
        canvas.setEffect(new DropShadow(24, Color.web("#000000", 0.35)));

        // (선택) 하단 그라데이션 이미지
        gradientView = new ImageView();
        gradientView.setPreserveRatio(false);
        gradientView.setFitWidth(420);
        gradientView.setFitHeight(260);
        gradientView.setVisible(false);
        AnchorPane.setLeftAnchor(gradientView, 0.0);
        AnchorPane.setBottomAnchor(gradientView, 0.0);

        // 상단 아이콘(경고/사이렌)
        topIconView = new ImageView();
        topIconView.setPreserveRatio(true);
        topIconView.setFitWidth(52);
        AnchorPane.setLeftAnchor(topIconView, (420 - 52) / 2.0);
        AnchorPane.setTopAnchor(topIconView, 70.0);

        // BPM 카드(이미지)
        bpmView = new ImageView();
        bpmView.setPreserveRatio(true);
        bpmView.setFitWidth(240);
        AnchorPane.setLeftAnchor(bpmView, (420 - 240) / 2.0);
        AnchorPane.setTopAnchor(bpmView, 120.0);

        // 하트(캐릭터 대신 단색 하트 이미지든 뭐든 OK)
        heartView = new ImageView();
        heartView.setPreserveRatio(true);
        heartView.setFitWidth(220);
        AnchorPane.setLeftAnchor(heartView, (420 - 220) / 2.0);
        AnchorPane.setTopAnchor(heartView, 330.0);

        // 하단 탭바 (모양만 + 누르면 하이라이트)
        tabBar = buildTabBar();
        AnchorPane.setLeftAnchor(tabBar, 20.0);
        AnchorPane.setRightAnchor(tabBar, 20.0);
        AnchorPane.setBottomAnchor(tabBar, 26.0);

        // 앱 화면 구성
        canvas.getChildren().addAll(gradientView, topIconView, bpmView, heartView, tabBar);
        root.getChildren().add(canvas);

        Scene scene = new Scene(root, 480, 900);
        stage.setTitle("Prototype (One Screen)");
        stage.setScene(scene);
        stage.show();

        // 초기 상태
        applyState(State.NORMAL);
        setActiveTab(tabHome);

        // 5초마다 상태 자동 변화
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            cycleIdx = (cycleIdx + 1) % cycle.length;
            applyState(cycle[cycleIdx]);
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    // ====== 탭바 생성 ======
    private HBox buildTabBar() {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(12));
        bar.setPrefHeight(72);

        // iOS 느낌 비슷하게: 흰 바탕 + 둥근 모서리 + 살짝 그림자
        bar.setBackground(new Background(new BackgroundFill(Color.web("#F7F7F7"), new CornerRadii(22), Insets.EMPTY)));
        bar.setEffect(new DropShadow(14, Color.web("#000000", 0.14)));

        tabHome = tabButton("Home");
        tabGuide = tabButton("Guide");
        tabReport = tabButton("Report");
        tabProfile = tabButton("Profile");

        // 탭 누르면 "활성 표시"만 바뀌고, 화면은 한 장 유지
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
        b.setPrefHeight(48);
        b.setFocusTraversable(false);
        // 기본 스타일(비활성)
        b.setStyle("""
            -fx-background-color: transparent;
            -fx-text-fill: #8B8B8B;
            -fx-font-size: 13px;
            -fx-font-weight: 700;
            -fx-background-radius: 16px;
        """);
        return b;
    }

    private void setActiveTab(Button active) {
        // 모두 비활성 스타일
        for (Button b : new Button[]{tabHome, tabGuide, tabReport, tabProfile}) {
            b.setStyle("""
                -fx-background-color: transparent;
                -fx-text-fill: #8B8B8B;
                -fx-font-size: 13px;
                -fx-font-weight: 700;
                -fx-background-radius: 16px;
            """);
        }
        // 활성 탭만 강조
        active.setStyle("""
            -fx-background-color: #111111;
            -fx-text-fill: white;
            -fx-font-size: 13px;
            -fx-font-weight: 800;
            -fx-background-radius: 16px;
        """);
    }

    // ====== 상태 적용(하트 + BPM + (선택)그라데이션 + (선택)아이콘) ======
    private void applyState(State s) {
        switch (s) {
            case NORMAL -> {
                bpmView.setImage(bpmNormal);
                heartView.setImage(heartNormal != null ? heartNormal : heartFallback);

                // 정상은 아이콘/그라데이션 숨김
                topIconView.setImage(null);
                gradientView.setVisible(false);
            }
            case LOW -> {
                bpmView.setImage(bpmLow != null ? bpmLow : bpmNormal);
                heartView.setImage(heartLow != null ? heartLow : heartFallback);

                // 선택 요소(있으면 보여주기)
                if (iconWarn != null) topIconView.setImage(iconWarn);
                if (gradLow != null) {
                    gradientView.setImage(gradLow);
                    gradientView.setVisible(true);
                } else {
                    gradientView.setVisible(false);
                }
            }
            case HIGH -> {
                bpmView.setImage(bpmHigh != null ? bpmHigh : bpmNormal);
                heartView.setImage(heartHigh != null ? heartHigh : heartFallback);

                if (iconSiren != null) topIconView.setImage(iconSiren);
                if (gradHigh != null) {
                    gradientView.setImage(gradHigh);
                    gradientView.setVisible(true);
                } else {
                    gradientView.setVisible(false);
                }
            }
        }
    }

    // ====== 이미지 로드 ======
    private void loadImages() {
        // 하트 3종
        heartNormal = safeLoad(RES + "heart_normal.png");
        heartLow    = safeLoad(RES + "heart_low.png");
        heartHigh   = safeLoad(RES + "heart_high.png");
        heartFallback = safeLoad(RES + "heart.png");

        // BPM 카드 3종
        bpmNormal = safeLoad(RES + "bpm_normal.png");
        bpmLow    = safeLoad(RES + "bpm_low.png");
        bpmHigh   = safeLoad(RES + "bpm_high.png");

        // 선택: 그라데이션/아이콘 (없어도 실행됨)
        gradLow  = safeLoad(RES + "grad_low.png");
        gradHigh = safeLoad(RES + "grad_high.png");
        iconWarn = safeLoad(RES + "icon_warning.png");
        iconSiren= safeLoad(RES + "icon_siren.png");

        // 최소 필수 이미지 없을 때 대비
        if (bpmNormal == null) {
            // 없으면 아주 단순한 “빈 화면”이 될 수 있으니 콘솔로 알려주기
            System.out.println("[WARN] bpm_normal.png 가 없으면 BPM 영역이 비어 보일 수 있어요.");
        }
        if (heartFallback == null && heartNormal == null) {
            System.out.println("[WARN] heart_normal.png 또는 heart.png 가 필요해요.");
        }
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
