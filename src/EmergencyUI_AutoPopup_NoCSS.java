import java.util.Random;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

public class EmergencyUI_AutoPopup_NoCSS extends Application {

    // ---------- thresholds ----------
    private static final int LOW_TH = 60;
    private static final int HIGH_TH = 140;
    private static final int NORMAL_MIN = 80;
    private static final int NORMAL_MAX = 100;

    enum State { LOW, NORMAL, HIGH }

    private final IntegerProperty bpm = new SimpleIntegerProperty(50);
    private final Random rnd = new Random();

    // UI refs that change with state
    private Label topIcon;
    private Label bpmMain;
    private Label bpmUnit;
    private Polyline ecgLine;

    private VBox rightPanel;
    private Label rightTitle;
    private Label rightSub;

    private Rectangle bottomGrad;
    private StackPane heartCharacter;

    // popup overlay
    private StackPane popupOverlay;

    @Override
    public void start(Stage stage) {
        StackPane root = new StackPane();
        root.setPadding(new Insets(22));
        root.setBackground(new Background(new BackgroundFill(Color.web("#1C1C1E"), CornerRadii.EMPTY, Insets.EMPTY)));

        StackPane canvas = new StackPane();
        canvas.setPrefSize(1040, 620);
        canvas.setBackground(new Background(new BackgroundFill(Color.web("#ECECEC"), new CornerRadii(8), Insets.EMPTY)));
        canvas.setEffect(new DropShadow(24, Color.web("#000000", 0.35)));

        // bottom warm gradient (changes by state)
        bottomGrad = new Rectangle(1040, 220);
        bottomGrad.setFill(new LinearGradient(
                0, 1, 0, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#F08A2C")),
                new Stop(1, Color.web("#ECECEC"))
        ));
        bottomGrad.setOpacity(0.85);
        StackPane.setAlignment(bottomGrad, Pos.BOTTOM_CENTER);

        // left nav
        VBox nav = leftNav();
        StackPane.setAlignment(nav, Pos.CENTER_LEFT);
        StackPane.setMargin(nav, new Insets(0, 0, 0, 22));

        // ECG line
        ecgLine = ecgLine();
        StackPane.setAlignment(ecgLine, Pos.CENTER);
        ecgLine.setTranslateY(15);

        // Center vitals (icon + bpm)
        VBox center = centerVitals();
        StackPane.setAlignment(center, Pos.TOP_CENTER);
        StackPane.setMargin(center, new Insets(72, 0, 0, 0));

        // Right alert panel (shows only LOW/HIGH)
        rightPanel = rightAlertPanel();
        StackPane.setAlignment(rightPanel, Pos.CENTER_RIGHT);
        StackPane.setMargin(rightPanel, new Insets(0, 22, 0, 0));

        // Heart character (changes face by state)
        heartCharacter = new StackPane();
        heartCharacter.setPrefSize(180, 150);
        StackPane.setAlignment(heartCharacter, Pos.BOTTOM_CENTER);
        StackPane.setMargin(heartCharacter, new Insets(0, 0, 42, 0));

        // Popup overlay (hidden by default)
        popupOverlay = buildPopupOverlay();
        popupOverlay.setVisible(false);

        // (Optional) demo control: slider to test thresholds quickly
        HBox demo = demoControls();
        StackPane.setAlignment(demo, Pos.BOTTOM_LEFT);
        StackPane.setMargin(demo, new Insets(0, 0, 18, 18));

        canvas.getChildren().addAll(bottomGrad, ecgLine, center, nav, rightPanel, heartCharacter, popupOverlay, demo);
        root.getChildren().add(canvas);

        Scene scene = new Scene(root, 1080, 700);
        stage.setTitle("Emergency UI (Auto state + Popup) - No CSS");
        stage.setScene(scene);
        stage.show();

        // Update when bpm changes
        bpm.addListener((obs, oldV, newV) -> applyState(currentState()));

        // Initial state
        applyState(currentState());

        // (Optional) Auto simulation: randomly drift BPM every 1.2s
        Timeline auto = new Timeline(new KeyFrame(Duration.millis(1200), e -> {
            int v = bpm.get();
            // drift towards random target
            int delta = rnd.nextInt(21) - 10; // -10..+10
            int next = clamp(v + delta, 40, 170);
            bpm.set(next);
        }));
        auto.setCycleCount(Animation.INDEFINITE);
        auto.play();
    }

    // ---------------- State logic ----------------
    private State currentState() {
        int v = bpm.get();
        if (v < LOW_TH) return State.LOW;
        if (v >= HIGH_TH) return State.HIGH;
        // Normal band: treat 80~100 as NORMAL, others still "normal-ish"
        return State.NORMAL;
    }

    private void applyState(State s) {
        // text
        if (s == State.NORMAL) {
            // show "80-100" like your normal image
            bpmMain.setText(NORMAL_MIN + "-" + NORMAL_MAX);
            bpmMain.setTextFill(Color.web("#111111"));
            bpmUnit.setTextFill(Color.web("#8A8A8A"));
        } else if (s == State.LOW) {
            bpmMain.setText(String.valueOf(bpm.get()));
            bpmMain.setTextFill(Color.web("#E53935"));
            bpmUnit.setTextFill(Color.web("#E53935", 0.85));
        } else { // HIGH
            // show "140-150" like your high image 느낌
            bpmMain.setText("140-150");
            bpmMain.setTextFill(Color.web("#E53935"));
            bpmUnit.setTextFill(Color.web("#E53935", 0.85));
        }

        // top icon
        if (s == State.NORMAL) {
            topIcon.setText(""); // no icon
        } else if (s == State.LOW) {
            topIcon.setText("⚠");
            topIcon.setTextFill(Color.web("#F6B300"));
        } else {
            topIcon.setText("🚨");
            topIcon.setTextFill(Color.web("#E53935"));
        }

        // bottom gradient intensity (normal: 거의 없음)
        if (s == State.NORMAL) {
            bottomGrad.setOpacity(0.0);
        } else if (s == State.LOW) {
            bottomGrad.setOpacity(0.85);
        } else {
            // high: 조금 더 붉은 느낌 섞기
            bottomGrad.setFill(new LinearGradient(
                    0, 1, 0, 0, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#F05A2C")),
                    new Stop(1, Color.web("#ECECEC"))
            ));
            bottomGrad.setOpacity(0.70);
        }

        // ECG visibility (정상에서는 숨겨도 되고 보여도 되는데, 이미지처럼 "정상"은 깔끔 -> 숨김)
        ecgLine.setVisible(s != State.NORMAL);

        // right panel visibility + text
        rightPanel.setVisible(s != State.NORMAL);
        if (s == State.LOW) {
            rightTitle.setText("맥박 수가\n떨어져요!");
            rightSub.setText("5초 후 자동 제어가\n시작됩니다.");
        } else if (s == State.HIGH) {
            rightTitle.setText("맥박 수가\n너무 높아져요");
            rightSub.setText("5초 후 자동 제어가\n시작됩니다.");
        }

        // heart character
        heartCharacter.getChildren().setAll(buildHeartByState(s));
    }

    // ---------------- UI Parts ----------------
    private VBox leftNav() {
        VBox nav = new VBox(22);
        nav.setAlignment(Pos.TOP_CENTER);
        nav.setPadding(new Insets(18, 14, 18, 14));
        nav.setPrefSize(90, 380);

        nav.setBackground(new Background(new BackgroundFill(
                Color.web("#F7F7F7"), new CornerRadii(45), Insets.EMPTY
        )));
        nav.setBorder(new Border(new BorderStroke(
                Color.web("#FFFFFF", 0.90),
                BorderStrokeStyle.SOLID,
                new CornerRadii(45),
                new BorderWidths(2)
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

    private VBox centerVitals() {
        VBox box = new VBox(8);
        box.setAlignment(Pos.TOP_CENTER);

        topIcon = new Label("⚠");
        topIcon.setFont(Font.font(28));
        topIcon.setTextFill(Color.web("#F6B300"));

        bpmMain = new Label("50");
        bpmMain.setFont(Font.font(66));
        bpmMain.setStyle("-fx-font-weight: 900;");
        bpmMain.setTextFill(Color.web("#E53935"));

        bpmUnit = new Label("bpm");
        bpmUnit.setFont(Font.font(24));
        bpmUnit.setTextFill(Color.web("#E53935", 0.85));

        box.getChildren().addAll(topIcon, bpmMain, bpmUnit);
        return box;
    }

    private Polyline ecgLine() {
        Polyline ecg = new Polyline(
                0, 150, 70, 150, 88, 135, 105, 175, 128, 150,
                200, 150, 218, 135, 235, 178, 258, 150,
                340, 150, 358, 135, 375, 175, 398, 150,
                490, 150, 508, 135, 525, 178, 548, 150,
                640, 150, 658, 135, 675, 175, 698, 150,
                790, 150, 808, 135, 825, 178, 848, 150,
                940, 150, 1040, 150
        );
        ecg.setStroke(Color.web("#FF6B6B", 0.75));
        ecg.setStrokeWidth(2.2);
        ecg.setFill(Color.TRANSPARENT);
        return ecg;
    }

    private VBox rightAlertPanel() {
        VBox panel = new VBox(16);
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setPadding(new Insets(20));
        panel.setPrefSize(250, 420);

        LinearGradient redGrad = new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#FF1F1F")),
                new Stop(1, Color.web("#B80000"))
        );
        panel.setBackground(new Background(new BackgroundFill(redGrad, new CornerRadii(40), Insets.EMPTY)));
        panel.setBorder(new Border(new BorderStroke(
                Color.web("#FFFFFF", 0.75),
                BorderStrokeStyle.SOLID,
                new CornerRadii(40),
                new BorderWidths(2)
        )));
        panel.setEffect(new DropShadow(18, Color.web("#000000", 0.22)));

        rightTitle = new Label("맥박 수가\n떨어져요!");
        rightTitle.setFont(Font.font(20));
        rightTitle.setStyle("-fx-font-weight: 900;");
        rightTitle.setTextFill(Color.WHITE);
        rightTitle.setAlignment(Pos.CENTER);
        rightTitle.setWrapText(true);

        rightSub = new Label("5초 후 자동 제어가\n시작됩니다.");
        rightSub.setFont(Font.font(12));
        rightSub.setTextFill(Color.web("#FFFFFF", 0.88));
        rightSub.setAlignment(Pos.CENTER);
        rightSub.setWrapText(true);

        Button b1 = pillButton("인근 쉼터 안내");
        b1.setOnAction(e -> showShelterPopup());

        Button b2 = pillButton("응급 대응");
        b2.setOnAction(e -> showEmergencyPopup());

        panel.getChildren().addAll(rightTitle, spacer(14), rightSub, spacer(18), b1, b2);
        return panel;
    }

    private Button pillButton(String text) {
        Button b = new Button(text);
        b.setPrefWidth(190);
        b.setPrefHeight(54);
        b.setFont(Font.font(14));
        b.setStyle("-fx-font-weight: 800;");
        b.setTextFill(Color.WHITE);

        b.setBackground(new Background(new BackgroundFill(
                new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#FF3A3A")),
                        new Stop(1, Color.web("#B80000"))
                ),
                new CornerRadii(20), Insets.EMPTY
        )));
        b.setBorder(new Border(new BorderStroke(
                Color.web("#FFFFFF", 0.35),
                BorderStrokeStyle.SOLID,
                new CornerRadii(20),
                new BorderWidths(1)
        )));
        b.setEffect(new DropShadow(10, Color.web("#000000", 0.18)));
        return b;
    }

    // ---------------- Heart by state ----------------
    private StackPane buildHeartByState(State s) {
        StackPane heart = new StackPane();
        heart.setPrefSize(180, 150);

        // heart base (two circles + triangle)
        Circle left = new Circle(44, Color.web("#E91414"));
        Circle right = new Circle(44, Color.web("#E91414"));
        left.setTranslateX(-28);
        right.setTranslateX(28);
        left.setTranslateY(-10);
        right.setTranslateY(-10);

        Polygon bottom = new Polygon(0, 92, -72, 18, 72, 18);
        bottom.setFill(Color.web("#E91414"));

        // face area (pink)
        Circle face = new Circle(54, Color.web("#FFB3B3"));
        face.setTranslateY(20);

        Circle eye1 = new Circle(4, Color.web("#222222"));
        Circle eye2 = new Circle(4, Color.web("#222222"));
        eye1.setTranslateX(-16); eye1.setTranslateY(14);
        eye2.setTranslateX(16);  eye2.setTranslateY(14);

        Shape mouth;
        if (s == State.NORMAL) {
            // smile
            Arc smile = new Arc(0, 34, 16, 10, 200, 140);
            smile.setType(ArcType.OPEN);
            smile.setStroke(Color.web("#E91414"));
            smile.setStrokeWidth(4);
            smile.setFill(Color.TRANSPARENT);
            mouth = smile;

            // small hand/OK 느낌 원
            Circle hand = new Circle(14, Color.web("#FFB3B3"));
            hand.setStroke(Color.web("#E91414"));
            hand.setStrokeWidth(5);
            hand.setTranslateX(55);
            hand.setTranslateY(-5);

            heart.getChildren().addAll(left, right, bottom, face, eye1, eye2, mouth, hand);
            return heart;

        } else if (s == State.HIGH) {
            // shocked O mouth + cheek/hand
            Circle o = new Circle(14, Color.web("#FF3B30"));
            o.setTranslateY(42);
            mouth = o;

            Circle hand = new Circle(18, Color.web("#FFB3B3"));
            hand.setStroke(Color.web("#E91414"));
            hand.setStrokeWidth(6);
            hand.setTranslateX(55);
            hand.setTranslateY(35);

            heart.getChildren().addAll(left, right, bottom, face, eye1, eye2, mouth, hand);
            return heart;

        } else {
            // LOW: shocked bigger mouth
            Circle o = new Circle(16, Color.web("#FF3B30"));
            o.setTranslateY(42);
            mouth = o;

            heart.getChildren().addAll(left, right, bottom, face, eye1, eye2, mouth);
            return heart;
        }
    }

    // ---------------- Popup overlay ----------------
    private StackPane buildPopupOverlay() {
        StackPane overlay = new StackPane();
        overlay.setPickOnBounds(true);

        Rectangle dim = new Rectangle(1040, 620);
        dim.setFill(Color.web("#000000", 0.35));

        VBox card = new VBox(10);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(16));
        card.setPrefSize(360, 200);
        card.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(18), Insets.EMPTY)));
        card.setEffect(new DropShadow(20, Color.web("#000000", 0.25)));

        Label title = new Label("인근 쉼터 안내");
        title.setFont(Font.font(16));
        title.setStyle("-fx-font-weight: 900;");
        title.setTextFill(Color.web("#111111"));

        Label info1 = new Label("• 가장 가까운 졸음쉼터: 2.4km");
        info1.setFont(Font.font(13));
        info1.setTextFill(Color.web("#333333"));

        Label info2 = new Label("• 방향: 다음 교차로에서 우회전 → 직진 1.8km");
        info2.setFont(Font.font(13));
        info2.setTextFill(Color.web("#333333"));
        info2.setWrapText(true);

        Label info3 = new Label("• 예상 도착: 약 5분");
        info3.setFont(Font.font(13));
        info3.setTextFill(Color.web("#333333"));

        Button close = new Button("닫기");
        close.setPrefWidth(90);
        close.setPrefHeight(34);
        close.setTextFill(Color.WHITE);
        close.setFont(Font.font(12));
        close.setStyle("-fx-font-weight: 800;");
        close.setBackground(new Background(new BackgroundFill(Color.web("#0A6CFF"), new CornerRadii(12), Insets.EMPTY)));
        close.setOnAction(e -> overlay.setVisible(false));

        HBox btnRow = new HBox(close);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(title, spacer(6), info1, info2, info3, spacer(10), btnRow);

        overlay.getChildren().addAll(dim, card);
        StackPane.setAlignment(card, Pos.CENTER);

        // click dim to close
        dim.setOnMouseClicked(e -> overlay.setVisible(false));

        return overlay;
    }

    private void showShelterPopup() {
        popupOverlay.setVisible(true);
    }

    private void showEmergencyPopup() {
        popupOverlay.setVisible(true);
        // 팝업 내용을 응급 대응으로 바꾸고 싶으면: buildPopupOverlay를 더 일반화하면 됨.
        // 지금은 과제 시연용이라 같은 팝업으로 처리.
    }

    // ---------------- Demo controls ----------------
    private HBox demoControls() {
        // 작은 슬라이더: bpm을 임계치 넘겨보며 “자동 변화” 확인용
        Label l = new Label("BPM");
        l.setTextFill(Color.web("#FFFFFF", 0.9));
        l.setFont(Font.font(12));

        Slider s = new Slider(40, 170, bpm.get());
        s.setPrefWidth(220);
        s.valueProperty().addListener((obs, ov, nv) -> bpm.set(nv.intValue()));

        Label v = new Label();
        v.setTextFill(Color.web("#FFFFFF", 0.9));
        v.setFont(Font.font(12));
        v.textProperty().bind(bpm.asString());

        HBox box = new HBox(10, l, s, v);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(10));
        box.setBackground(new Background(new BackgroundFill(Color.web("#000000", 0.25), new CornerRadii(14), Insets.EMPTY)));
        box.setEffect(new DropShadow(10, Color.web("#000000", 0.25)));
        return box;
    }

    // ---------------- utils ----------------
    private Region spacer(double h) {
        Region r = new Region();
        r.setPrefHeight(h);
        return r;
    }

    private int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public static void main(String[] args) {
        launch();
    }
}
