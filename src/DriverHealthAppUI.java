import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class DriverHealthAppUI extends Application {

    @Override
    public void start(Stage stage) {

        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #ECECEC;");

        HBox topRow = new HBox(20,
                homeCard(),
                reportCard(),
                settingsCard()
        );

        HBox bottomRow = new HBox(20, guideCard());

        root.getChildren().addAll(topRow, bottomRow);

        Scene scene = new Scene(root, 1200, 700);
        stage.setTitle("Driver Health Monitoring UI");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }

    // ---------------- HOME ----------------
    private VBox homeCard() {
        VBox box = cardBase("홈");

        Label status = new Label("⚠ 위험 감지!");
        status.setFont(Font.font(22));
        status.setTextFill(Color.WHITE);

        Label msg = new Label("집중하세요! 위험 심장 신호 감지됨");
        msg.setTextFill(Color.WHITE);

        Label rest = new Label("졸음 쉼터까지 2.4km 남았습니다");
        rest.setTextFill(Color.WHITE);

        VBox alertBox = new VBox(10, status, msg, rest);
        alertBox.setAlignment(Pos.CENTER);
        alertBox.setPadding(new Insets(20));
        alertBox.setStyle("""
            -fx-background-color: linear-gradient(#FF3B3B, #B00000);
            -fx-background-radius: 15;
        """);

        Label device = new Label("🔋 배터리 80% | 🔗 Bluetooth 연결 | 📶 신호 양호");
        device.setTextFill(Color.WHITE);

        box.getChildren().addAll(alertBox, device);
        return box;
    }

    // ---------------- REPORT ----------------
    private VBox reportCard() {
        VBox box = cardBase("리포트");

        box.getChildren().addAll(
                infoRow("주행 시간", "3시간 25분"),
                infoRow("평균 심박수", "78 bpm"),
                infoRow("위험 감지", "3회"),
                infoRow("휴식 횟수", "2회")
        );

        Label footer = new Label("📍 가까운 졸음쉼터: 2.4km | 5분 후");
        footer.setTextFill(Color.DARKRED);

        box.getChildren().add(footer);
        return box;
    }

    private HBox infoRow(String left, String right) {
        Label l = new Label(left);
        Label r = new Label(right);
        r.setTextFill(Color.DARKRED);
        r.setFont(Font.font(16));

        HBox row = new HBox(10, l, r);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // ---------------- SETTINGS ----------------
    private VBox settingsCard() {
        VBox box = cardBase("설정");

        CheckBox sound = new CheckBox("Sound 알림");
        sound.setSelected(true);

        CheckBox haptic = new CheckBox("Haptic 알림");
        haptic.setSelected(true);

        box.getChildren().addAll(
                new Label("내 정보"),
                new Label("보호자 등록 (위험 시 위치 전송)"),
                sound,
                haptic
        );
        return box;
    }

    // ---------------- GUIDE ----------------
    private VBox guideCard() {
        VBox box = cardBase("가이드");

        box.getChildren().addAll(
                new Label("📌 벨트 착용 가이드"),
                new Label("❤️ 심장 건강 상식"),
                new Label("🚗 안전 운전 팁")
        );
        return box;
    }

    // ---------------- CARD BASE ----------------
    private VBox cardBase(String title) {
        Label header = new Label(title);
        header.setFont(Font.font(18));
        header.setTextFill(Color.WHITE);

        VBox box = new VBox(15);
        box.setPadding(new Insets(15));
        box.setPrefWidth(300);
        box.setStyle("""
            -fx-background-color: linear-gradient(#C62828, #8E0000);
            -fx-background-radius: 20;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 5);
        """);

        box.getChildren().add(header);
        return box;
    }
}
