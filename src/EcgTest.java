// Ecg.java
// JavaFX Prototype: ECG Alert Demo (RR -> HR threshold -> Alert + Actions + Logs)
//
// What this demo shows (prototype-level):
// 1) Simulated RR interval stream (ms) -> HR(bpm)=60000/RR
// 2) Abnormal detection by thresholds (LOW/HIGH)
// 3) Alert UI + beep + "BLE warning sent" (simulated)
// 4) Actions: Call 119 / Call Guardian1/2 (mock), Open Maps for ER/Rest area (real browser open)
// 5) Data Log + Export CSV (summary + abnormal window metadata)
//
// Requirements:
// - JDK 17+ recommended
// - JavaFX SDK configured (module-path + add-modules javafx.controls,javafx.fxml)
// Notes:
// - Phone call / SMS are mocked (desktop limitations). We log + show dialogs.
// - Maps button opens browser with Google Maps search.
//
// Author: Milo (for Ellie)

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class EcgTest extends Application {

    // ====== CONFIG ======
    private final IntegerProperty lowBpm = new SimpleIntegerProperty(40);
    private final IntegerProperty highBpm = new SimpleIntegerProperty(180);

    // "event window" metadata (for demo: store 10s pre + 10s post)
    private final IntegerProperty preWindowSec = new SimpleIntegerProperty(10);
    private final IntegerProperty postWindowSec = new SimpleIntegerProperty(10);

    // "contacts" (demo)
    private final StringProperty guardian1Name = new SimpleStringProperty("긴급연락1");
    private final StringProperty guardian1Phone = new SimpleStringProperty("010-1234-5678");
    private final StringProperty guardian2Name = new SimpleStringProperty("긴급연락2");
    private final StringProperty guardian2Phone = new SimpleStringProperty("010-8765-4321");

    // ====== SIGNAL STATE ======
    private final IntegerProperty rrMs = new SimpleIntegerProperty(800);    // ~75 bpm
    private final IntegerProperty hrBpm = new SimpleIntegerProperty(75);
    private final BooleanProperty abnormal = new SimpleBooleanProperty(false);
    private final BooleanProperty bleConnected = new SimpleBooleanProperty(true); // simulated
    private final BooleanProperty streaming = new SimpleBooleanProperty(false);

    private Instant abnormalStart = null;
    private boolean bleWarningSent = false;

    // ring buffer (store last N seconds samples, but here we store summary samples)
    private final Deque<EcgSample> ring = new ArrayDeque<>();
    private final List<EcgSample> abnormalClip = new ArrayList<>(); // pre + post samples (demo)

    // logs
    private final ObservableList<LogEvent> logs = FXCollections.observableArrayList();

    // scheduler
    private Timeline timeline;
    private final Random rng = new Random();

    // simple beep (fallback if media not available)
    private void beep() {
        // Toolkit beep is okay for prototype
        try {
            java.awt.Toolkit.getDefaultToolkit().beep();
        } catch (Exception ignored) {}
    }

    // ====== UI helpers ======
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String nowTs() {
        return LocalDateTime.now().format(TS_FMT);
    }

    private void addLog(String type, String msg) {
        logs.add(0, new LogEvent(nowTs(), type, msg));
        if (logs.size() > 400) logs.remove(logs.size() - 1);
    }

    private void openMaps(String query) {
        try {
            String url = "https://www.google.com/maps/search/?api=1&query=" + URI.create("x:" + query).getRawSchemeSpecificPart().substring(2);
            // Above trick avoids manual encoding logic; it URL-encodes via URI parsing-ish.
            // If it looks weird, just hardcode encoding; this works well for demo.
            Desktop.getDesktop().browse(new URI(url));
            addLog("NAV_OPEN", "Open maps search: " + query);
        } catch (Exception e) {
            addLog("ERROR", "Maps open failed: " + e.getMessage());
            showInfo("지도 열기 실패", "브라우저/권한 문제로 지도를 열 수 없어요.\n" + e.getMessage());
        }
    }

    private void showInfo(String title, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.showAndWait();
    }

    private void showWarn(String title, String content) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.showAndWait();
    }

    // ====== STREAM / SIM ======
    private void startStream() {
        if (streaming.get()) return;
        streaming.set(true);
        addLog("STREAM_START", "RR->HR simulation started (20Hz summary)");

        // 20Hz "summary" ticks for prototype (every 50ms)
        timeline = new Timeline(new KeyFrame(Duration.millis(50), e -> tick()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void stopStream() {
        streaming.set(false);
        if (timeline != null) timeline.stop();
        addLog("STREAM_STOP", "Simulation stopped");
    }

    private void forceTachy() {
        rrMs.set(320); // ~188 bpm
        evaluate();
        addLog("FORCE_ABNORMAL", "Forced tachy (RR=320ms)");
    }

    private void forceBrady() {
        rrMs.set(1600); // ~38 bpm
        evaluate();
        addLog("FORCE_ABNORMAL", "Forced brady (RR=1600ms)");
    }

    private void resetNormal() {
        rrMs.set(800);
        abnormal.set(false);
        abnormalStart = null;
        bleWarningSent = false;
        abnormalClip.clear();
        addLog("RESET", "Reset to normal");
    }

    private void tick() {
        // --- simulate RR drift ---
        // small drift
        if (rng.nextDouble() < 0.05) {
            int delta = rng.nextInt(41) - 20; // -20..+20
            rrMs.set(clamp(rrMs.get() + delta, 300, 2000));
        }

        // rare abnormal episode injection
        if (rng.nextDouble() < 0.002) {
            boolean tachy = rng.nextBoolean();
            rrMs.set(tachy ? (300 + rng.nextInt(180)) : (1300 + rng.nextInt(700)));
        }

        // store sample in ring (summary sample)
        ring.addLast(new EcgSample(Instant.now(), rrMs.get(), hrFromRr(rrMs.get())));
        trimRingSeconds(20); // keep last 20 seconds of summary samples

        evaluate();
    }

    private void trimRingSeconds(int keepSec) {
        Instant cutoff = Instant.now().minusSeconds(keepSec);
        while (!ring.isEmpty() && ring.peekFirst().ts.isBefore(cutoff)) {
            ring.removeFirst();
        }
    }

    private int hrFromRr(int rr) {
        return (int)Math.round(60000.0 / rr);
    }

    private int clamp(int x, int lo, int hi) {
        return Math.max(lo, Math.min(hi, x));
    }

    private void evaluate() {
        int rr = rrMs.get();
        int hr = hrFromRr(rr);
        hrBpm.set(hr);

        boolean nowAbnormal = (hr < lowBpm.get()) || (hr > highBpm.get());

        if (nowAbnormal && !abnormal.get()) {
            // ABNORMAL START
            abnormal.set(true);
            abnormalStart = Instant.now();
            bleWarningSent = false;

            // collect "pre" window samples from ring (last preWindowSec)
            abnormalClip.clear();
            abnormalClip.addAll(extractLastSecondsFromRing(preWindowSec.get()));

            addLog("ABNORMAL_START", "HR=" + hr + " bpm, RR=" + rr + "ms (threshold " + lowBpm.get() + "~" + highBpm.get() + ")");
            beep();
        } else if (!nowAbnormal && abnormal.get()) {
            // ABNORMAL END
            abnormal.set(false);

            // collect "post" window samples (just take last postWindowSec from ring again)
            abnormalClip.addAll(extractLastSecondsFromRing(postWindowSec.get()));

            long dur = (abnormalStart == null) ? -1 : (Instant.now().getEpochSecond() - abnormalStart.getEpochSecond());
            addLog("ABNORMAL_END", "duration=" + dur + "s, clipSamples=" + abnormalClip.size());
            abnormalStart = null;
            bleWarningSent = false;
        }

        // Simulated BLE warning
        if (abnormal.get() && !bleWarningSent) {
            bleWarningSent = true;
            if (bleConnected.get()) {
                addLog("BLE_WARNING_TX", "Sent warning payload {hr=" + hr + ", rr=" + rr + "}");
            } else {
                addLog("BLE_WARNING_TX_FAIL", "BLE disconnected. Payload dropped.");
            }
        }
    }

    private List<EcgSample> extractLastSecondsFromRing(int sec) {
        Instant cutoff = Instant.now().minusSeconds(sec);
        List<EcgSample> out = new ArrayList<>();
        for (EcgSample s : ring) {
            if (!s.ts.isBefore(cutoff)) out.add(s);
        }
        return out;
    }

    // ====== EXPORT ======
    private String buildExportJsonLikeText() {
        // quick "report" text (JSON-ish) to copy
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"exportedAt\": \"").append(nowTs()).append("\",\n");
        sb.append("  \"thresholds\": {\"lowBpm\": ").append(lowBpm.get()).append(", \"highBpm\": ").append(highBpm.get()).append("},\n");
        sb.append("  \"latest\": {\"rrMs\": ").append(rrMs.get()).append(", \"hrBpm\": ").append(hrBpm.get()).append("},\n");
        sb.append("  \"eventWindow\": {\"preSec\": ").append(preWindowSec.get()).append(", \"postSec\": ").append(postWindowSec.get()).append("},\n");
        sb.append("  \"abnormalClipSamples\": ").append(abnormalClip.size()).append(",\n");
        sb.append("  \"recentLogs\": [\n");
        int n = Math.min(20, logs.size());
        for (int i = 0; i < n; i++) {
            LogEvent le = logs.get(i);
            sb.append("    {\"ts\":\"").append(le.ts).append("\",\"type\":\"").append(le.type).append("\",\"msg\":\"")
                    .append(le.msg.replace("\"", "\\\"")).append("\"}");
            if (i != n - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    private void copyExportToClipboard() {
        String txt = buildExportJsonLikeText();
        ClipboardContent cc = new ClipboardContent();
        cc.putString(txt);
        Clipboard.getSystemClipboard().setContent(cc);
        addLog("EXPORT_COPY", "Copied report text to clipboard (len=" + txt.length() + ")");
        showInfo("복사 완료", "로그/요약 리포트를 클립보드에 복사했어!\n(발표 때 바로 붙여넣기 가능)");
    }

    private void exportCsv(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export CSV (logs + abnormal clip metadata)");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fc.setInitialFileName("ecg_demo_export.csv");
        File file = fc.showSaveDialog(stage);
        if (file == null) return;

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            // header
            bw.write("exported_at," + nowTs());
            bw.newLine();
            bw.write("threshold_low_bpm," + lowBpm.get());
            bw.newLine();
            bw.write("threshold_high_bpm," + highBpm.get());
            bw.newLine();
            bw.write("event_pre_sec," + preWindowSec.get());
            bw.newLine();
            bw.write("event_post_sec," + postWindowSec.get());
            bw.newLine();
            bw.write("latest_rr_ms," + rrMs.get());
            bw.newLine();
            bw.write("latest_hr_bpm," + hrBpm.get());
            bw.newLine();
            bw.write("abnormal_clip_samples," + abnormalClip.size());
            bw.newLine();
            bw.newLine();

            bw.write("LOGS");
            bw.newLine();
            bw.write("ts,type,msg");
            bw.newLine();
            for (LogEvent le : logs) {
                bw.write(csv(le.ts) + "," + csv(le.type) + "," + csv(le.msg));
                bw.newLine();
            }

            bw.newLine();
            bw.write("ABNORMAL_CLIP_SUMMARY_SAMPLES");
            bw.newLine();
            bw.write("ts,rr_ms,hr_bpm");
            bw.newLine();
            for (EcgSample s : abnormalClip) {
                bw.write(csv(LocalDateTime.ofInstant(s.ts, TimeZone.getDefault().toZoneId()).format(TS_FMT))
                        + "," + s.rr + "," + s.hr);
                bw.newLine();
            }

            addLog("EXPORT_CSV", "Saved CSV to " + file.getAbsolutePath());
            showInfo("저장 완료", "CSV 저장 완료!\n" + file.getAbsolutePath());
        } catch (Exception e) {
            addLog("ERROR", "CSV export failed: " + e.getMessage());
            showWarn("저장 실패", e.getMessage());
        }
    }

    private String csv(String s) {
        if (s == null) return "";
        String t = s.replace("\"", "\"\"");
        return "\"" + t + "\"";
    }

    // ====== APP ======
    @Override
    public void start(Stage stage) {
        // Top: status banner
        Label statusTitle = new Label();
        statusTitle.setFont(Font.font(20));
        Label statusSub = new Label();

        // metrics
        Label hrLabel = new Label();
        hrLabel.setFont(Font.font(42));
        Label rrLabel = new Label();
        rrLabel.setFont(Font.font(18));

        // dynamic binding
        hrLabel.textProperty().bind(hrBpm.asString().concat(" bpm"));
        rrLabel.textProperty().bind(rrMs.asString().concat(" ms (RR interval)"));

        // Status color + text
        abnormal.addListener((obs, oldV, newV) -> {
            if (newV) {
                statusTitle.setText("⚠ 이상 심전도 의심");
                statusSub.setText("기준: " + lowBpm.get() + "~" + highBpm.get() + " bpm (RR→HR 계산)");
            } else {
                statusTitle.setText("✅ 정상");
                statusSub.setText("기준: " + lowBpm.get() + "~" + highBpm.get() + " bpm");
            }
        });
        // initialize
        statusTitle.setText("✅ 정상");
        statusSub.setText("기준: " + lowBpm.get() + "~" + highBpm.get() + " bpm");

        // banner background changes
        HBox banner = new HBox(14);
        banner.setPadding(new Insets(14));
        banner.setAlignment(Pos.CENTER_LEFT);
        VBox bannerText = new VBox(4, statusTitle, statusSub);
        banner.getChildren().addAll(new Label("❤"), bannerText);
        banner.setStyle("-fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: #ddd;");
        updateBannerStyle(banner);
        abnormal.addListener((o, a, b) -> updateBannerStyle(banner));

        // Threshold inputs
        Spinner<Integer> lowSpin = new Spinner<>(20, 120, lowBpm.get(), 1);
        Spinner<Integer> highSpin = new Spinner<>(80, 240, highBpm.get(), 1);
        lowSpin.setEditable(true);
        highSpin.setEditable(true);

        lowSpin.valueProperty().addListener((o, ov, nv) -> {
            lowBpm.set(nv);
            addLog("THRESHOLD_LOW_SET", "low=" + nv);
            evaluate();
            statusSub.setText("기준: " + lowBpm.get() + "~" + highBpm.get() + " bpm");
        });
        highSpin.valueProperty().addListener((o, ov, nv) -> {
            highBpm.set(nv);
            addLog("THRESHOLD_HIGH_SET", "high=" + nv);
            evaluate();
            statusSub.setText("기준: " + lowBpm.get() + "~" + highBpm.get() + " bpm");
        });

        Spinner<Integer> preSpin = new Spinner<>(5, 30, preWindowSec.get(), 1);
        Spinner<Integer> postSpin = new Spinner<>(5, 30, postWindowSec.get(), 1);
        preSpin.setEditable(true);
        postSpin.setEditable(true);

        preSpin.valueProperty().addListener((o, ov, nv) -> {
            preWindowSec.set(nv);
            addLog("WINDOW_PRE_SET", "preSec=" + nv);
        });
        postSpin.valueProperty().addListener((o, ov, nv) -> {
            postWindowSec.set(nv);
            addLog("WINDOW_POST_SET", "postSec=" + nv);
        });

        GridPane config = new GridPane();
        config.setHgap(10);
        config.setVgap(10);
        config.addRow(0, new Label("LOW(bpm)"), lowSpin, new Label("HIGH(bpm)"), highSpin);
        config.addRow(1, new Label("Pre(sec)"), preSpin, new Label("Post(sec)"), postSpin);
        config.setPadding(new Insets(12));
        config.setStyle("-fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: #eee; -fx-background-color: #fafafa;");

        // Buttons (actions)
        Button btnStart = new Button("스트림 시작");
        Button btnStop = new Button("스트림 중지");
        btnStop.setDisable(true);

        btnStart.setOnAction(e -> {
            startStream();
            btnStart.setDisable(true);
            btnStop.setDisable(false);
        });
        btnStop.setOnAction(e -> {
            stopStream();
            btnStart.setDisable(false);
            btnStop.setDisable(true);
        });

        Button btnForceT = new Button("이상(빠름) 강제");
        btnForceT.setOnAction(e -> forceTachy());
        Button btnForceB = new Button("이상(느림) 강제");
        btnForceB.setOnAction(e -> forceBrady());
        Button btnReset = new Button("리셋");
        btnReset.setOnAction(e -> resetNormal());

        ToggleButton btnBle = new ToggleButton("BLE 연결됨(시뮬)");
        btnBle.setSelected(true);
        btnBle.selectedProperty().bindBidirectional(bleConnected);
        bleConnected.addListener((o, ov, nv) -> {
            btnBle.setText(nv ? "BLE 연결됨(시뮬)" : "BLE 끊김(시뮬)");
            addLog("BLE_TOGGLE", "connected=" + nv);
        });

        Button btnCall119 = new Button("119 응급콜");
        btnCall119.setOnAction(e -> {
            addLog("CALL", "119 call tapped (mock)");
            showWarn("응급콜(시연)", "PC 프로토타입이라 실제 통화는 안 걸려!\n하지만 모바일 앱에선 tel/sms API로 연동 가능.\n\n[시연] 119로 연락을 시도합니다.");
        });

        Button btnCallG1 = new Button(guardian1Name.get() + " 연락");
        btnCallG1.setOnAction(e -> {
            addLog("CALL", guardian1Name.get() + " call tapped (mock): " + guardian1Phone.get());
            showInfo("긴급연락(시연)", guardian1Name.get() + " (" + guardian1Phone.get() + ")\n연락을 시도합니다.");
        });

        Button btnCallG2 = new Button(guardian2Name.get() + " 연락");
        btnCallG2.setOnAction(e -> {
            addLog("CALL", guardian2Name.get() + " call tapped (mock): " + guardian2Phone.get());
            showInfo("긴급연락(시연)", guardian2Name.get() + " (" + guardian2Phone.get() + ")\n연락을 시도합니다.");
        });

        Button btnNavEr = new Button("응급실 경로");
        btnNavEr.setOnAction(e -> openMaps("근처 응급실"));
        Button btnNavRest = new Button("졸음쉼터 경로");
        btnNavRest.setOnAction(e -> openMaps("근처 졸음쉼터"));

        Button btnCopy = new Button("리포트 복사");
        btnCopy.setOnAction(e -> copyExportToClipboard());

        Button btnExport = new Button("CSV 저장");
        btnExport.setOnAction(e -> exportCsv(stage));

        HBox actions1 = new HBox(10, btnStart, btnStop, btnBle, btnForceT, btnForceB, btnReset);
        actions1.setAlignment(Pos.CENTER_LEFT);

        HBox actions2 = new HBox(10, btnCall119, btnCallG1, btnCallG2, btnNavEr, btnNavRest, btnCopy, btnExport);
        actions2.setAlignment(Pos.CENTER_LEFT);

        VBox actions = new VBox(10, actions1, actions2);
        actions.setPadding(new Insets(12));
        actions.setStyle("-fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: #eee; -fx-background-color: #ffffff;");

        // Logs table
        TableView<LogEvent> table = new TableView<>(logs);
        TableColumn<LogEvent, String> c1 = new TableColumn<>("시간");
        c1.setCellValueFactory(d -> d.getValue().tsProperty());
        c1.setPrefWidth(160);

        TableColumn<LogEvent, String> c2 = new TableColumn<>("유형");
        c2.setCellValueFactory(d -> d.getValue().typeProperty());
        c2.setPrefWidth(140);

        TableColumn<LogEvent, String> c3 = new TableColumn<>("내용");
        c3.setCellValueFactory(d -> d.getValue().msgProperty());
        c3.setPrefWidth(520);

        table.getColumns().addAll(c1, c2, c3);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPrefHeight(260);

        // Layout: left top metrics + right logs? We'll do vertical for simplicity.
        VBox metrics = new VBox(6, hrLabel, rrLabel);
        metrics.setPadding(new Insets(12));
        metrics.setAlignment(Pos.CENTER_LEFT);
        metrics.setStyle("-fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: #eee; -fx-background-color: #ffffff;");

        Label hint = new Label(
                "프로토타입 시연 포인트:\n" +
                "• RR(ms) → HR(bpm)=60000/RR 계산\n" +
                "• HR이 LOW/HIGH를 벗어나면 '이상' + (시뮬)BLE 경고 전송 + 알림\n" +
                "• 응급실/졸음쉼터는 브라우저 지도 검색으로 연동 시연\n" +
                "• 로그/요약은 CSV/클립보드로 내보내기"
        );
        hint.setTextFill(Color.GRAY);
        hint.setPadding(new Insets(6, 0, 0, 2));

        VBox root = new VBox(12, banner, metrics, config, actions, new Label("이벤트 로그"), table, hint);
        root.setPadding(new Insets(16));
        Scene scene = new Scene(root, 1100, 860);

        stage.setTitle("ECG Alert Prototype (JavaFX) - Ecg.java");
        stage.setScene(scene);
        stage.show();

        // Start immediately (optional)
        startStream();
        btnStart.setDisable(true);
        btnStop.setDisable(false);

        // on close
        stage.setOnCloseRequest(e -> {
            stopStream();
            Platform.exit();
        });
    }

    private void updateBannerStyle(HBox banner) {
        if (abnormal.get()) {
            banner.setStyle("-fx-background-radius: 14; -fx-border-radius: 14; " +
                    "-fx-border-color: rgba(220,53,69,0.35); -fx-background-color: rgba(220,53,69,0.10);");
        } else {
            banner.setStyle("-fx-background-radius: 14; -fx-border-radius: 14; " +
                    "-fx-border-color: rgba(25,135,84,0.35); -fx-background-color: rgba(25,135,84,0.10);");
        }
    }

    // ====== data classes ======
    static class EcgSample {
        final Instant ts;
        final int rr;
        final int hr;

        EcgSample(Instant ts, int rr, int hr) {
            this.ts = ts;
            this.rr = rr;
            this.hr = hr;
        }
    }

    public static class LogEvent {
        private final StringProperty ts = new SimpleStringProperty();
        private final StringProperty type = new SimpleStringProperty();
        private final StringProperty msg = new SimpleStringProperty();

        LogEvent(String ts, String type, String msg) {
            this.ts.set(ts);
            this.type.set(type);
            this.msg.set(msg);
        }

        public StringProperty tsProperty() { return ts; }
        public StringProperty typeProperty() { return type; }
        public StringProperty msgProperty() { return msg; }

        public String getTs() { return ts.get(); }
        public String getType() { return type.get(); }
        public String getMsg() { return msg.get(); }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
