package jpm.ui.view;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.text.TextAlignment;
import jpm.ui.model.ChatMessage;
import jpm.ui.model.ProcessManager;

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
        sendButton = new Button("전송");
        sendButton.setId("send-button");
        sendButton.setPrefWidth(60);
        sendButton.setMinWidth(60);


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
     * 메시지 전송 처리
     */
    private void sendMessage() {
        String input = inputField.getText().trim();
        if (!input.isEmpty()) {
            // 사용자 메시지 추가
            addUserMessage(input);
            inputField.clear();

            // 진행 상태 표시
            setProcessingState(true);

            // 프로세스에 입력 전달
            processManager.processUserInput(input);
        }
    }

    /**
     * 진행 상태 표시 여부 설정
     * */
    private void setProcessingState(boolean isProcessing) {
        Platform.runLater(() -> {
            progressIndicator.setVisible(isProcessing);
            if (isProcessing) {
                progressIndicator.toFront();
            }
            sendButton.setText(isProcessing ? "" : "전송");
        });
    }

    /**
     * 사용자 메시지 추가
     */
    private void addUserMessage(String content) {
        messages.add(new ChatMessage(content, ChatMessage.MessageType.USER));
    }

    /**
     * JPM 응답 처리
     */
    private void handleJpmResponse(String response) {
        // JPM 메시지 추가 (UI 스레드에서 실행)
        Platform.runLater(() -> {
            messages.add(new ChatMessage(response, ChatMessage.MessageType.JPM));
        });
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

                // 텍스트 생성
                Text text = new Text(message.getContent());

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
                        TextFlow textFlow = new TextFlow(text);
                        textFlow.setTextAlignment(TextAlignment.RIGHT);
                        textFlow.setPrefWidth(Control.USE_COMPUTED_SIZE);
                        textFlow.setMaxWidth(500); // 최대 너비 제한

                        // 텍스트 스타일 적용
                        text.getStyleClass().add("user-text");

                        bubble.getChildren().add(textFlow);
                        bubble.getStyleClass().addAll("message-bubble", "user-bubble");
                        bubble.setPadding(new Insets(7.5, 12.5, 7.5, 12.5));

                        container.getChildren().add(bubble);
                        break;

                    case JPM:
                        // JPM 메시지: 말풍선 없는 단순 텍스트, 왼쪽 정렬
                        container.setAlignment(Pos.CENTER_LEFT);

                        // 텍스트 설정
                        text.setWrappingWidth(600); // 긴 텍스트를 위한 충분한 너비
                        text.getStyleClass().add("jpm-text");

                        container.getChildren().add(text);
                        break;

                    case SYSTEM:
                        // 시스템 메시지: 가운데 정렬
                        container.setAlignment(Pos.CENTER);

                        text.setWrappingWidth(500);
                        text.getStyleClass().add("system-text");

                        container.getChildren().add(text);
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
}