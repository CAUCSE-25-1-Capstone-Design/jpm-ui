package jpm.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Screen;
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
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

            windowWidth = screenBounds.getWidth() * 0.5;
            windowHeight = screenBounds.getHeight() * 0.7;

            // 외부 컨테이너 생성 (투명 배경 + 그림자용)
            StackPane shadowContainer = new StackPane();
            shadowContainer.setPadding(new Insets(20)); // 그림자를 위한 여백 확보

            shadowContainer.setStyle("-fx-background-color: transparent;"); // 완전 투명 배경

            // 중간 컨테이너 생성 (그림자 효과가 적용될 투명 레이어)
            StackPane effectContainer = new StackPane();
            effectContainer.setStyle("-fx-background-color: transparent;"); // 투명 배경
            effectContainer.setMaxWidth(windowWidth - 30); // 여백 고려한 크기
            effectContainer.setMaxHeight(windowHeight - 30);

            // 그림자 효과 생성

            DropShadow dropShadow = new DropShadow();
            dropShadow.setRadius(15);
            dropShadow.setOffsetX(0);  // 수평 오프셋 (0 = 중앙)
            dropShadow.setOffsetY(8);  // 수직 오프셋 (양수 = 아래쪽)
            dropShadow.setSpread(0.2); // 그림자 확산 (약간 줄임)
            dropShadow.setColor(Color.rgb(0, 0, 0, 0.45)); // 그림자 색상 및 투명도 조정

            // 중간 컨테이너에 그림자 적용
            effectContainer.setEffect(dropShadow);

            // 내부 컨테이너 생성 (실제 UI 콘텐츠용)
            StackPane contentContainer = new StackPane();
            contentContainer.setPadding(new Insets(10));
            contentContainer.setStyle("-fx-background-color: rgba(242, 242, 247, 0.9);"
                    + "-fx-background-radius: 20;");

            // 메인 뷰 생성
            MainView mainView = new MainView();

            // 윈도우 컨트롤
            HBox windowControls = createWindowControls(primaryStage);
            windowControls.setPickOnBounds(false);

            // 컴포넌트 추가
            contentContainer.getChildren().addAll(mainView, windowControls);
            effectContainer.getChildren().add(contentContainer);
            shadowContainer.getChildren().add(effectContainer);

            // Scene 생성 - 그림자 컨테이너를 루트로 사용
            Scene scene = new Scene(shadowContainer, windowWidth, windowHeight);
            scene.setFill(Color.TRANSPARENT);  // 완전 투명 배경

            // 폰트 적용
            Font.loadFont(getClass().getClassLoader().getResource("fonts/Pretendard-Regular.otf").toString(), 16);

            // CSS 적용
            scene.getStylesheets().add(getClass().getClassLoader().getResource("css/styles.css").toExternalForm());

            // Stage 설정
            primaryStage.initStyle(StageStyle.TRANSPARENT);  // 테두리 제거
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            primaryStage.setResizable(false);

            // 전체화면 진입 후 ESC로 빠져나오면 창 크기 조정
            primaryStage.fullScreenProperty().addListener((obs, wasFullScreen, isFullScreen) -> {
                if (wasFullScreen && !isFullScreen) {
                    primaryStage.setWidth(windowWidth);
                    primaryStage.setHeight(windowHeight);
                    primaryStage.centerOnScreen();
                }
            });

            // 드래그로 창 이동 - contentContainer에 적용
            implementWindowDrag(contentContainer, primaryStage);

            // 닫기 이벤트 처리
            primaryStage.setOnCloseRequest(e -> {
                mainView.shutdown();
                Platform.exit();
            });

            // 실행
            primaryStage.show();
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