package jpm.ui.model;

import jpm.ui.constants.DevelopmentLevel;
import jpm.ui.constants.JpmConstants;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 외부 프로세스 실행 및 통신을 담당하는 클래스
 * jpm-nlp와 jpm-core 프로세스와의 통신을 관리합니다.
 */
public class ProcessManager {

    // 외부 프로세스 실행 경로 (실제 경로로 변경 필요)
    private final String pythonCommand; // 시스템에 맞는 Python 명령어 ("python" 또는 "python3")
    private static final String NLP_SCRIPT_PATH = JpmConstants.JPM_NLP_PATH;

    private final Consumer<String> outputHandler; // 출력 처리 콜백
    private final Consumer<Integer> processCompletionCallback; // 프로세스 종료 후 콜백
    private final ExecutorService executorService; // 비동기 작업 실행기

    private static final Logger LOGGER = Logger.getLogger(ProcessManager.class.getName());
    // Logger 레벨 초기화
    static {
        if (JpmConstants.DEVELOPMENT_LEVEL == DevelopmentLevel.DEBUG) {
            LOGGER.setLevel(Level.ALL); // 디버그 모드에서는 모든 로그 출력
        } else {
            LOGGER.setLevel(Level.OFF); // 릴리즈 모드에서는 출력 X
        }
    }

    /**
     * 프로세스 매니저 생성자
     *
     * @param outputHandler 프로세스 출력 처리 콜백
     */
    public ProcessManager(Consumer<String> outputHandler, Consumer<Integer> processCompletionCallback) {
        this.outputHandler = outputHandler;
        this.processCompletionCallback = processCompletionCallback;
        this.executorService = Executors.newCachedThreadPool();
        this.pythonCommand = detectPythonCommand(); // Python 명령어 자동 감지
    }

    /**
     * 사용자 입력을 처리하고 NLP 프로세스에 전달
     * 명령줄 인자를 통해 입력을 전달합니다.
     *
     * @param input 사용자 입력
     */
    public void processUserInput(String input) {
        executorService.submit(() -> {
            Integer exitCode = null;
            try {
                // 새 NLP 프로세스 시작 (매 요청마다 새로운 프로세스)
                ProcessBuilder pb = new ProcessBuilder(pythonCommand, NLP_SCRIPT_PATH, input);
                pb.redirectErrorStream(true); // 표준 오류를 표준 출력으로 리다이렉트

                LOGGER.info("NLP 프로세스 시작: " + pythonCommand + " " + NLP_SCRIPT_PATH + " \"" + input + "\"");
                Process process = pb.start();

                // 프로세스 출력 읽기
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // 빈 라인 무시
                        if (!line.trim().isEmpty()) {
                            final String output = line;
                            outputHandler.accept(output);
                        }
                    }
                }

                // 프로세스 종료 대기
                exitCode = process.waitFor();
                LOGGER.info("NLP 프로세스 종료 코드: " + exitCode);

                if (exitCode != 0) {
                    // 비정상 종료 시 오류 메시지
                    outputHandler.accept("프로세스가 비정상 종료되었습니다 (코드: " + exitCode + ")");
                }

            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "프로세스 통신 오류", e);
                outputHandler.accept("프로세스 통신 중 오류가 발생했습니다: " + e.getMessage());
                exitCode = -1; // IO 예외 발생 시 오류코드 (임의)
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "프로세스 실행 중단", e);
                Thread.currentThread().interrupt();
                outputHandler.accept("프로세스 실행이 중단되었습니다.");
                exitCode = -2; // 인터럽트 시 오류코드 (임의)
            } finally {
                // 프로세스 종료 콜백 호출
                if(processCompletionCallback != null && exitCode != null) {
                    processCompletionCallback.accept(exitCode);
                }
            }
        });
    }

    /**
     * 시스템에 맞는 Python 명령어 감지
     *
     * @return 감지된 Python 명령어 ("python" 또는 "python3")
     */
    private String detectPythonCommand() {
        String python = "";
        try {
            // 먼저 'python3' 시도
            if (testCommand("python3")) {
                return python = "python3";
            }

            // 다음으로 'python' 시도
            if (testCommand("python")) {
                return python = "python";
            }

            // 둘 다 실패하면 기본값 반환
            return python = "python";
        } finally {
            LOGGER.info("감지한 Python 명령어: " + python);
        }
    }

    /**
     * 명령어 실행 테스트
     *
     * @param command 테스트할 명령어
     * @return 명령어 실행 성공 여부
     */
    private boolean testCommand(String command) {
        try {
            Process process = new ProcessBuilder(command, "--version").start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    /**
     * 자원 정리 및 프로세스 종료
     */
    public void shutdown() {
        LOGGER.info("프로세스 매니저 종료...");

        // 스레드 풀 종료
        executorService.shutdownNow();
    }
}