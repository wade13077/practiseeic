// package com.example.backend.exception;

package com.esb.icrm.exception;

/**
 * 外部系統呼叫例外
 * <p>
 * 呼叫外部系統 (EIAM、SMS 等) 發生錯誤時拋出，由 {@code GlobalExceptionHandler} 攔截為 HTTP 500
 * </p>
 *
 */
public class ExternalSystemException extends RuntimeException {

    /** 目標系統名稱 */
    private final String targetSystem;

    /**
     * 建構子
     *
     * @param targetSystem 目標系統名稱
     * @param message      錯誤訊息
     */
    public ExternalSystemException(String targetSystem, String message) {
        super("[" + targetSystem + "] " + message);
        this.targetSystem = targetSystem;
    }

    /**
     * 建構子 (含原始例外)
     *
     * @param targetSystem 目標系統名稱
     * @param message      錯誤訊息
     * @param cause        原始例外
     */
    public ExternalSystemException(String targetSystem, String message, Throwable cause) {
        super("[" + targetSystem + "] " + message, cause);
        this.targetSystem = targetSystem;
    }

    /**
     * 取得目標系統名稱
     *
     * @return 目標系統名稱
     */
    public String getTargetSystem() {
        return targetSystem;
    }
}