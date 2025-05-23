package jpm.ui.view;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.text.TextAlignment;
import jpm.ui.model.ChatMessage;
import jpm.ui.model.ProcessManager;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * 채팅 인터페이스를 제공하는 메인 뷰 컴포넌트
 * 사용자 입력 처리와 JPM 프로세스 관리를 담당합니다.
 */
public class MainView extends BorderPane {

    private final ListView<ChatMessage> chatListView;
    private final TextField inputField; // 메시지 입력 창
    private final Button sendButton; // 메시지 전송 버튼
    private final ObservableList<ChatMessage> messages; // 채팅 메시지가 쌓이는 리스트
    private final ProgressIndicator progressIndicator; // Python 프로세스 실행 중에 보여질 원형 로딩 컴포넌트
    private final ProcessManager processManager;
    private HBox typingIndicator;
    private List<Circle> dots;
    private int activeDotIndex = 0;
    private Timeline animation;
    private boolean isTyping = false;
    ImageView icon = new ImageView(new Image(getClass().getResourceAsStream("/image/up-arrow3.png")));

    // 클래스 필드로 추가
    private Text typingStatusText;

    public MainView() {
        // 기본 스타일 및 패딩 설정
        setId("main-view");
        setPadding(new Insets(10));

        // 배경 투명 설정
        setStyle("-fx-background-color: transparent;");

        // 채팅 메시지 목록 초기화
        messages = FXCollections.observableArrayList();

        // 채팅 메시지 표시 영역 구성
        chatListView = new ListView<>(messages);
        chatListView.setCellFactory(this::createChatCell);
        chatListView.setId("chat-list-view");

        // 리스트뷰 투명 배경 설정
        chatListView.setStyle("-fx-background-color: transparent;");

        // 스크롤 정책 설정 - 필요할 때만 스크롤바 표시
        ScrollPane scrollPane = new ScrollPane(chatListView);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        // 스크롤팬 투명 배경 설정
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        // 입력 필드 구성
        inputField = new TextField();
        inputField.setPromptText("메시지를 입력하세요...");
        inputField.setId("input-field");
        HBox.setHgrow(inputField, Priority.ALWAYS);

        // 전송 버튼 구성

        icon.setFitWidth(25);
        icon.setFitHeight(25);

        sendButton = new Button();
        sendButton.setId("send-button");
        sendButton.setGraphic(icon);
        sendButton.setPrefSize(35, 35);
        sendButton.setMaxSize(35, 35);
        sendButton.setMinSize(35, 35);


        // ProgressIndicator 초기화
        progressIndicator = new ProgressIndicator();
        progressIndicator.setMinSize(20, 20);
        progressIndicator.setMaxSize(20, 20);
        progressIndicator.setVisible(false);   // 초기에는 숨김. Python 프로세스 실행 시 보여질 예정
        progressIndicator.setStyle("-fx-progress-color: #000000;");

        // 전송 버튼에 ProgressIndicator 추가하기 위한 StackPane 사용
        StackPane buttonPane = new StackPane();
        buttonPane.getChildren().addAll(sendButton, progressIndicator);

        // 입력 영역 레이아웃 구성
        HBox inputBox = new HBox(10, inputField, buttonPane);
        inputBox.setPadding(new Insets(10, 0, 0, 0));
        inputBox.setAlignment(Pos.CENTER);
        inputBox.setStyle("-fx-background-color: transparent;");

        // 레이아웃 배치
        setCenter(scrollPane);
        setBottom(inputBox);

        // 프로세스 매니저 초기화 - 응답 처리 콜백 등록
        processManager = new ProcessManager(
                this::handleJpmResponse,
                exitCode -> setProcessingState(false)
        );

        // 타이핑 인디케이터 생성 메서드 호출 추가
        createTypingIndicator();

        // 애니메이션 설정 메서드 호출 추가
        setupAnimation();


        // 이벤트 핸들러 등록
        setupEventHandlers();

        // 시작 메시지 추가
        addSystemMessage("JPM에 오신 것을 환영합니다! 프로젝트 관리와 관련된 질문이나 명령을 입력해주세요.");
    }

    /**
     * 이벤트 핸들러 설정
     */
    private void setupEventHandlers() {
        // 전송 버튼 클릭 이벤트
        sendButton.setOnAction(e -> sendMessage());

        // 입력 필드 엔터키 이벤트
        inputField.setOnAction(e -> sendMessage());

        // 메시지 목록 변경 시 자동 스크롤 처리
        messages.addListener((javafx.collections.ListChangeListener.Change<? extends ChatMessage> c) -> {
            if (c.next() && c.wasAdded()) {
                Platform.runLater(() -> {
                    chatListView.scrollTo(messages.size() - 1);
                });
            }
        });
    }

    /**
     * 진행 상태 표시 여부 설정
     * */
//    private void setProcessingState(boolean isProcessing) {
//        Platform.runLater(() -> {
//            progressIndicator.setVisible(isProcessing);
//            if (isProcessing) {
//                progressIndicator.toFront();
//            }
//            sendButton.setText(isProcessing ? "" : "전송");
//        });
//      * 메시지 전송 처리
//      */
//     private void sendMessage() {
//         String input = inputField.getText().trim();
//         if (!input.isEmpty()) {
//             // 사용자 메시지 추가
//             addUserMessage(input);
//             inputField.clear();

//             // 진행 상태 표시
//             setProcessingState(true);

//             // 프로세스에 입력 전달
//             processManager.processUserInput(input);
//         }
//    }

    /**
     * 진행 상태 표시 여부 설정
     * */
    private void setProcessingState(boolean isProcessing) {
        Platform.runLater(() -> {
            // 진행 상태에 따라 입력 필드와 전송 버튼 활성화/비활성화
            inputField.setDisable(isProcessing);
            sendButton.setDisable(isProcessing);

            // 진행 상태에 따라 프로그레스 인디케이터 표시/숨김
            progressIndicator.setVisible(isProcessing);
            if (isProcessing) {
                progressIndicator.toFront();
            }
            sendButton.setGraphic(isProcessing ? null : icon);

            // 처리가 완료되면 입력 필드에 포커스 설정
            if (!isProcessing) {
                inputField.requestFocus();
            }
        });
    }

    /**
     * 사용자 메시지 추가
     */
    private void addUserMessage(String content) {
        messages.add(new ChatMessage(content, ChatMessage.MessageType.USER));
    }

    /**
     * 시스템 메시지 추가 (환영 메시지 등)
     */
    private void addSystemMessage(String content) {
        messages.add(new ChatMessage(content, ChatMessage.MessageType.SYSTEM));
    }

    /**
     * 채팅 셀 팩토리 - 메시지 타입에 따라 다른 스타일 적용
     */
    private ListCell<ChatMessage> createChatCell(ListView<ChatMessage> listView) {
        return new ListCell<>() {
            @Override
            protected void updateItem(ChatMessage message, boolean empty) {
                super.updateItem(message, empty);

                if (empty || message == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                // 컨테이너 준비
                HBox container = new HBox();
                container.setPadding(new Insets(5, 10, 5, 10));

                switch (message.getType()) {
                    case USER:
                        // 사용자 메시지: 말풍선 형태의 컨테이너, 오른쪽 정렬
                        container.setAlignment(Pos.CENTER_RIGHT);

                        // 동적 크기의 말풍선 생성
                        StackPane bubble = new StackPane();

                        // TextFlow를 사용하여 텍스트 우측 정렬 구현
                        Text userText = new Text(message.getContent());
                        TextFlow textFlow = new TextFlow(userText);
                        textFlow.setTextAlignment(TextAlignment.RIGHT);
                        textFlow.setPrefWidth(Control.USE_COMPUTED_SIZE);
                        textFlow.setMaxWidth(500); // 최대 너비 제한

                        // 텍스트 스타일 적용
                        userText.getStyleClass().add("user-text");

                        bubble.getChildren().add(textFlow);
                        bubble.getStyleClass().addAll("message-bubble", "user-bubble");
                        bubble.setPadding(new Insets(7.5, 12.5, 7.5, 12.5));

                        container.getChildren().add(bubble);
                        break;

                    case JPM:
                        // JPM 메시지: 말풍선 없는 단순 텍스트, 왼쪽 정렬
                        container.setAlignment(Pos.CENTER_LEFT);

                        // 텍스트 설정
                        Text jpmText = new Text(message.getContent());
                        jpmText.setWrappingWidth(600); // 긴 텍스트를 위한 충분한 너비
                        jpmText.getStyleClass().add("typing-text");

                        container.getChildren().add(jpmText);
                        break;

                    case SYSTEM:
                        // 시스템 메시지: 가운데 정렬
                        container.setAlignment(Pos.CENTER);

                        Text systemText = new Text(message.getContent());
                        systemText.setWrappingWidth(500);
                        systemText.getStyleClass().add("system-text");

                        container.getChildren().add(systemText);
                        break;

                    case TYPING:
                        // 타이핑 인디케이터 표시
                        container.setAlignment(Pos.CENTER_LEFT);
                        container.getChildren().add(typingIndicator);
                        typingIndicator.setVisible(true);
                        typingIndicator.setManaged(true);
                        break;
                }

                setGraphic(container);
                setStyle("-fx-background-color: transparent;"); // 셀 배경을 투명하게 설정
            }
        };
    }

    /**
     * 입력 필드에 포커스 설정
     */
    public void focusInputField() {
        Platform.runLater(() -> inputField.requestFocus());
    }

    /**
     * 종료 시 리소스 정리
     */
    public void shutdown() {
        if (processManager != null) {
            processManager.shutdown();
        }
    }

    /**
     * 타이핑 인디케이터 생성 메서드
     */
    private void createTypingIndicator() {
        // 타이핑 인디케이터 컨테이너
        typingIndicator = new HBox(8);
        typingIndicator.setPadding(new Insets(5, 10, 5, 10));
        typingIndicator.setAlignment(Pos.CENTER_LEFT);

        // 말풍선 배경
        StackPane bubbleContainer = new StackPane();
        bubbleContainer.getStyleClass().add("message-bubble");
        bubbleContainer.setPadding(new Insets(7.5, 12.5, 7.5, 12.5));

        // 말풍선 내용을 담을 HBox
        HBox contentBox = new HBox(10); // 10픽셀 간격
        contentBox.setAlignment(Pos.CENTER_LEFT);

        // "~~ 하는중" 텍스트 추가
        typingStatusText = new Text("응답 준비 중");
        typingStatusText.getStyleClass().add("typing-text");
        contentBox.getChildren().add(typingStatusText);

        // 3개의 점 생성
        dots = new ArrayList<>();
        HBox dotsContainer = new HBox(8);

        for (int i = 0; i < 3; i++) {
            Circle dot = new Circle(4);
            // 모든 점의 중심을 동일하게 설정하여 애니메이션 중에도 정렬이 유지되도록 함
            dot.setCenterX(0);
            dot.setCenterY(0);
            dot.setFill(i == 0 ? Color.GRAY : Color.LIGHTGRAY);

            // 각 점을 StackPane으로 감싸서 애니메이션 중에도 레이아웃이 안정적으로 유지되도록 함
            StackPane dotWrapper = new StackPane(dot);
            dotWrapper.setMinSize(10, 10);  // 최소 크기 설정
            dotWrapper.setPrefSize(10, 10);  // 선호 크기 설정

            dots.add(dot);
            dotsContainer.getChildren().add(dotWrapper);
        }

        // 텍스트와 점 컨테이너를 contentBox에 추가
        contentBox.getChildren().add(dotsContainer);

        // contentBox를 말풍선 컨테이너에 추가
        bubbleContainer.getChildren().add(contentBox);

        // 타이핑 인디케이터에 말풍선 추가
        HBox bubble = new HBox();
        bubble.getChildren().addAll(bubbleContainer);
        bubble.setAlignment(Pos.CENTER_LEFT);

        typingIndicator.getChildren().add(bubble);

        // 처음에는 보이지 않게 설정
        typingIndicator.setVisible(false);
        typingIndicator.setManaged(false);
    }

    /**
     * 타이핑 인디케이터 텍스트 설정 메서드
     * @param text 표시할 텍스트
     */
    public void setTypingIndicatorText(String text) {
        Platform.runLater(() -> {
            if (typingStatusText != null) {
                typingStatusText.setText(text);
            }
        });
    }

    /**
     * 애니메이션 설정 메서드
     */
    private void setupAnimation() {
        // 0.33초마다 활성화된 점 변경
        animation = new Timeline(
                new KeyFrame(Duration.millis(330), e -> {
                    // 이전 점 비활성화
                    dots.get(activeDotIndex).setFill(Color.LIGHTGRAY);

                    // 다음 점 활성화
                    activeDotIndex = (activeDotIndex + 1) % 3;
                    Circle activeCircle = dots.get(activeDotIndex);
                    activeCircle.setFill(Color.GRAY);

                    // 활성화된 점에 크기 애니메이션 적용
                    ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(330), activeCircle);
                    scaleTransition.setFromX(1.0);
                    scaleTransition.setFromY(1.0);
                    scaleTransition.setToX(1.5);  // 1.5배로 커짐
                    scaleTransition.setToY(1.5);  // 1.5배로 커짐
                    scaleTransition.setCycleCount(2);  // 커졌다가 작아지는 사이클
                    scaleTransition.setAutoReverse(true);  // 자동으로 역방향 애니메이션 실행
                    scaleTransition.play();
                })
        );

        animation.setCycleCount(Timeline.INDEFINITE);
    }

    /**
     * 타이핑 인디케이터 표시 메서드
     */
    public void showTypingIndicator() {
        if (!isTyping) {
            isTyping = true;

            // 타이핑 인디케이터를 메시지 목록에 추가
            Platform.runLater(() -> {
                // 타이핑 인디케이터 메시지 생성
                ChatMessage typingMessage = new ChatMessage("", ChatMessage.MessageType.TYPING);
                messages.add(typingMessage);

                // 애니메이션 시작
                animation.play();
            });
        }
    }

    /**
     * 타이핑 인디케이터 숨김 메서드
     */
    public void hideTypingIndicator() {
        if (isTyping) {
            isTyping = false;

            // 타이핑 인디케이터 메시지 제거
            Platform.runLater(() -> {
                // 타이핑 메시지 찾아서 제거
                messages.removeIf(msg -> msg.getType() == ChatMessage.MessageType.TYPING);

                // 애니메이션 중지
                animation.stop();
            });
        }
    }

    ///

    // 메시지 전송 처리 메서드 수정
    private void sendMessage() {
        String input = inputField.getText().trim();
        if (!input.isEmpty()) {
            // 사용자 메시지 추가
            addUserMessage(input);
            inputField.clear();

            // 타이핑 인디케이터 표시
            showTypingIndicator();

            // 진행 상태 표시
            setProcessingState(true);

            // 프로세스에 입력 전달
            processManager.processUserInput(input);
        }
    }

    // JPM 응답 처리 메서드 수정
    private void handleJpmResponse(String response) {

        // JPM 메시지 추가 (UI 스레드에서 실행)
        Platform.runLater(() -> {
            String[] command = response.split(";");
            if(command.length != 0 && command[0].equals("PROGRESS")) {
                // 타이핑 인디케이터 숨기기
                hideTypingIndicator();

                if(command[1].equals("JPM")) {
                    String method = command[2];
                    switch (method) {
                        case "init":
                            setTypingIndicatorText("jpm 프로젝트 초기화 중");
                            showTypingIndicator();
                            break;
                        case "install":
                            setTypingIndicatorText("설치 중: " + command[3].split(":")[1]);
                            showTypingIndicator();
                            break;
                        case "update":
                            setTypingIndicatorText("업데이트 중: " + command[3].split(":")[1]);
                            showTypingIndicator();
                            break;
                        case "list":
                            setTypingIndicatorText("리스트업 중");
                            showTypingIndicator();
                            break;
                        case "delete":
                            setTypingIndicatorText("삭제 중: " + command[3].split(":")[1]);
                            showTypingIndicator();
                            break;
                        case "build":
                            setTypingIndicatorText("빌드 중");
                            showTypingIndicator();
                            break;
                        case "test":
                            setTypingIndicatorText("프로젝트 테스트 중");
                            showTypingIndicator();
                            break;
                        case "run":
                            setTypingIndicatorText("프로젝트 실행 중");
                            showTypingIndicator();
                            break;
                        case "clean":
                            setTypingIndicatorText("프로젝트 빌드 초기화 중");
                            showTypingIndicator();
                            break;
                        case "version":
                            setTypingIndicatorText("프로젝트 버전 읽어오는 중");
                            showTypingIndicator();
                            break;
                        case "set":
                            setTypingIndicatorText("메인 클래스 설정 중");
                            showTypingIndicator();
                            break;
                        case "getMetadata":
                            setTypingIndicatorText("프로젝트 메타데이터 읽어오는 중");
                            showTypingIndicator();
                            break;
                        case "refresh":
                            setTypingIndicatorText("프로젝트 설정 적용 중");
                            showTypingIndicator();
                            break;
                    }
                }
                else if(command[1].equals("GPT")) {
                    if(command[2].equals("generate")) {
                        setTypingIndicatorText("응답 생성 중");
                        showTypingIndicator();
                    }
                }
            }
            else {
                if(command[0].equals("OUTPUT") && command[1].equals("START")) hideTypingIndicator();
                else if(command[0].equals("OUTPUT") && command[1].equals("END")) setProcessingState(false);
                else messages.add(new ChatMessage(response, ChatMessage.MessageType.JPM));
            }
        });
    }
}
