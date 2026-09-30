// package com.example.backend.exception;
package com.esb.icrm.exception;

/**
 * 業務例外 (HTTP 200，前端依 ApiResponse.success=false 判斷)
 * <p>
 * 唯·自訂業務例外，由 GlobalExceptionHandler 統一攔截並回傳 HTTP 200
 * </p>
 *
 */
public class DomainException extends RuntimeException {

    /**
     * 建構子
     *
     * @param message 錯誤訊息
     */
    public DomainException(String message) {
        super(message);
    }
}