package jpm.ui.model;

import java.io.*;
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

    private static final Logger LOGGER = Logger.getLogger(ProcessManager.class.getName());

    // 외부 프로세스 실행 경로 (실제 경로로 변경 필요)
    private static final String NLP_PROCESS_PATH = "python";
    private static final String NLP_SCRIPT_PATH = "src/main/resources/python/jpm_nlp.py";

    private Process nlpProcess;              // NLP 프로세스
    private BufferedWriter nlpInputWriter;   // NLP 프로세스 입력 스트림
    private BufferedReader nlpOutputReader;  // NLP 프로세스 출력 스트림
    private final Consumer<String> outputHandler; // 출력 처리 콜백
    private final ExecutorService executorService; // 비동기 작업 실행기
    private volatile boolean isRunning;      // 프로세스 실행 상태

    /**
     * 프로세스 매니저 생성자
     *
     * @param outputHandler 프로세스 출력 처리 콜백
     */
    public ProcessManager(Consumer<String> outputHandler) {
        this.outputHandler = outputHandler;
        this.executorService = Executors.newCachedThreadPool();
        this.isRunning = false;
    }

    /**
     * NLP 프로세스 시작
     * 필요할 때 프로세스를 시작하고 연결을 설정합니다.
     */
    private void ensureNlpProcessStarted() {
        if (nlpProcess != null && nlpProcess.isAlive()) {
            return; // 이미 실행 중이면 재사용
        }

        try {
            LOGGER.info("NLP 프로세스 시작...");

            // NLP 프로세스 시작 (Python 스크립트 실행)
            ProcessBuilder pb = new ProcessBuilder(NLP_PROCESS_PATH, NLP_SCRIPT_PATH);
            pb.redirectErrorStream(true); // 표준 오류를 표준 출력으로 리다이렉트

            nlpProcess = pb.start();

            // 입출력 스트림 설정
            nlpInputWriter = new BufferedWriter(new OutputStreamWriter(nlpProcess.getOutputStream()));
            nlpOutputReader = new BufferedReader(new InputStreamReader(nlpProcess.getInputStream()));

            isRunning = true;

            // 별도 스레드에서 프로세스 종료 감지
            executorService.submit(() -> {
                try {
                    int exitCode = nlpProcess.waitFor();
                    LOGGER.info("NLP 프로세스 종료: " + exitCode);
                    isRunning = false;
                } catch (InterruptedException e) {
                    LOGGER.log(Level.WARNING, "NLP 프로세스 감시 중단", e);
                    Thread.currentThread().interrupt();
                }
            });

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "NLP 프로세스 시작 실패", e);
            outputHandler.accept("오류: NLP 프로세스를 시작할 수 없습니다. " + e.getMessage());
        }
    }

    /**
     * 사용자 입력을 처리하고 NLP 프로세스에 전달
     *
     * @param input 사용자 입력
     */
    public void processUserInput(String input) {
        // NLP 프로세스 시작 확인
        ensureNlpProcessStarted();

        if (!isRunning) {
            outputHandler.accept("오류: NLP 프로세스가 실행 중이 아닙니다.");
            return;
        }

        // 비동기적으로 입력 전송 및 응답 처리
        executorService.submit(() -> {
            try {
                // 입력을 NLP 프로세스에 전송
                nlpInputWriter.write(input);
                nlpInputWriter.newLine();
                nlpInputWriter.flush();

                LOGGER.info("사용자 입력 전송: " + input);

                // 응답 읽기 - 라인 단위로 실시간 처리
                String line;
                while ((line = nlpOutputReader.readLine()) != null) {
                    // 응답 끝 마커 확인 (jpm-nlp에서 정의 필요)
                    if (line.equals("JPM_RESPONSE_END")) {
                        break;
                    }

                    // 빈 라인 무시
                    if (!line.trim().isEmpty()) {
                        final String output = line;
                        outputHandler.accept(output);
                    }
                }

            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "프로세스 통신 오류", e);
                outputHandler.accept("프로세스 통신 중 오류가 발생했습니다: " + e.getMessage());

                // 연결 재설정 시도
                resetConnection();
            }
        });
    }

    /**
     * 프로세스 연결 재설정
     * 오류 발생 시 프로세스를 재시작합니다.
     */
    private void resetConnection() {
        shutdown();
        ensureNlpProcessStarted();
    }

    /**
     * 자원 정리 및 프로세스 종료
     */
    public void shutdown() {
        LOGGER.info("프로세스 매니저 종료...");

        isRunning = false;

        // 스트림 닫기
        try {
            if (nlpInputWriter != null) {
                nlpInputWriter.close();
            }
            if (nlpOutputReader != null) {
                nlpOutputReader.close();
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "스트림 닫기 오류", e);
        }

        // 프로세스 종료
        if (nlpProcess != null && nlpProcess.isAlive()) {
            nlpProcess.destroy();
            try {
                // 일정 시간 후에도 종료되지 않으면 강제 종료
                if (nlpProcess.waitFor() != 0) {
                    nlpProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "프로세스 종료 대기 중단", e);
                Thread.currentThread().interrupt();
                nlpProcess.destroyForcibly();
            }
        }

        // 스레드 풀 종료
        executorService.shutdownNow();
    }

    /**
     * 프로세스가 실행 중인지 확인
     *
     * @return 실행 중이면 true
     */
    public boolean isRunning() {
        return isRunning;
    }
}