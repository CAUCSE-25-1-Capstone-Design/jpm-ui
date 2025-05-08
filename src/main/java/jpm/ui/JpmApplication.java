package jpm.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import jpm.ui.view.MainView;

/**
 * JPM UI 애플리케이션의 주 진입점 클래스
 * 투명한 전체화면 UI를 구성하고 메인 뷰를 초기화합니다.
 */
public class JpmApplication extends Application {

    // 윈도우 드래그를 위한 좌표 저장 변수
    private double xOffset = 0;
    private double yOffset = 0;
    // 최소화 시 윈도우 크기
    private double windowWidth;
    private double windowHeight;

    @Override
    public void start(Stage primaryStage) {
        try {
            // 화면 크기 계산
            Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
            windowWidth = screenBounds.getWidth() * 0.7;   // 화면 너비의 70%
            windowHeight = screenBounds.getHeight() * 0.5; // 화면 높이의 50%

            // 투명한 배경의 루트 컨테이너 생성
            StackPane root = new StackPane();
            root.setPadding(new Insets(10));

            // 메인 뷰 생성 및 설정
            MainView mainView = new MainView();

            // 윈도우 컨트롤 생성 (닫기, 최소화, 최대화 버튼)
            HBox windowControls = createWindowControls(primaryStage);
            windowControls.setPickOnBounds(false); // 영역 크기만큼 이벤트 감지 방지

            // 윈도우 컨트롤 위치 조정 (오른쪽 상단에 고정)
//            StackPane.setMargin(windowControls, new Insets(10, 10, 0, 0));
//            StackPane.setAlignment(windowControls, javafx.geometry.Pos.TOP_RIGHT);

            // 루트에 컴포넌트 추가
            root.getChildren().addAll(mainView, windowControls);

            // 반투명한 검은색 배경의 씬 생성
            Scene scene = new Scene(root);
            scene.setFill(Color.rgb(0, 0, 0, 0.7)); // 70% 투명도의 검은색

            // 스타일시트 적용
            scene.getStylesheets().add(getClass().getClassLoader().getResource("css/styles.css").toExternalForm());

            // 투명한 스타일의 스테이지 설정
            primaryStage.initStyle(StageStyle.TRANSPARENT);
            primaryStage.setFullScreen(true);
            primaryStage.setFullScreenExitHint(""); // 전체화면 힌트 제거
            primaryStage.setScene(scene);

            // 윈도우 드래그 기능 구현
            implementWindowDrag(root, primaryStage);

            // 종료 시 모든 프로세스 정리
            primaryStage.setOnCloseRequest(e -> {
                mainView.shutdown();
                Platform.exit();
            });

            primaryStage.show();

            // 시작 시 입력 필드에 포커스
            mainView.focusInputField();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 윈도우 컨트롤 버튼 (닫기, 최소화, 최대화) 생성
     * 배경 없이 흰색 텍스트만 표시합니다.
     */
    private HBox createWindowControls(Stage stage) {
        HBox controls = new HBox(10); // 버튼 간격 확대
        controls.setAlignment(Pos.TOP_RIGHT);
        controls.setPadding(new Insets(0));

        // 최소화 버튼
        Button minimizeBtn = createWindowButton("—", "최소화");
        minimizeBtn.setOnAction(e -> stage.setIconified(true));

        // 최대화/복원 버튼
        Button maximizeBtn = createWindowButton("❐", "최대화");
        maximizeBtn.setOnAction(e -> {
            if (stage.isFullScreen()) {
                // 전체화면 모드 해제 시 지정된 크기로 설정
                stage.setFullScreen(false);
                stage.setWidth(windowWidth);
                stage.setHeight(windowHeight);
                stage.centerOnScreen(); // 화면 중앙에 배치

                maximizeBtn.setTooltip(new Tooltip("최대화"));
            } else {
                stage.setFullScreen(true);
                maximizeBtn.setTooltip(new Tooltip("복원"));
            }
        });

        // 닫기 버튼
        Button closeBtn = createWindowButton("✕", "닫기");
        closeBtn.getStyleClass().add("close-button");
        closeBtn.setOnAction(e -> {
            stage.close();
        });

        controls.getChildren().addAll(minimizeBtn, maximizeBtn, closeBtn);
        return controls;
    }

    /**
     * 윈도우 컨트롤용 버튼 생성 헬퍼 메서드
     * 배경 없는 투명한 버튼으로 생성합니다.
     */
    private Button createWindowButton(String text, String tooltipText) {
        Button button = new Button(text);
        button.getStyleClass().add("window-button");
        button.setTooltip(new Tooltip(tooltipText));

        // 배경 없는 투명한 버튼으로 설정
        button.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;"
        );

        // 호버 효과
        button.setOnMouseEntered(e ->
                button.setStyle(
                        "-fx-background-color: rgba(255, 255, 255, 0.2);" +
                                "-fx-text-fill: white;" +
                                "-fx-font-size: 14px;" +
                                "-fx-font-weight: bold;" +
                                "-fx-cursor: hand;"
                )
        );

        button.setOnMouseExited(e ->
                button.setStyle(
                        "-fx-background-color: transparent;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-size: 14px;" +
                                "-fx-font-weight: bold;" +
                                "-fx-cursor: hand;"
                )
        );

        button.setMinSize(25, 25);
        return button;
    }

    /**
     * 윈도우 드래그 기능 구현
     */
    private void implementWindowDrag(Region root, Stage stage) {
        root.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        root.setOnMouseDragged(event -> {
            // 전체화면이 아닐 때만 드래그 허용
            if (!stage.isFullScreen()) {
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            }
        });

        // 더블 클릭으로 전체화면 전환
        root.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                stage.setFullScreen(!stage.isFullScreen());
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}