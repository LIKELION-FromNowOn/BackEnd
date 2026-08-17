package com.youin.now.common.error;

import com.youin.now.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 예외를 {@link ApiResponse} 로 포장하는 유일한 자리.
 *
 * <p><b>컨트롤러에서 try-catch 로 응답을 만들지 마십시오.</b> 모양이 갈라집니다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handle(ApiException e) {
        return ResponseEntity.status(e.code().status())
                .body(ApiResponse.fail(e.code().name(), e.getMessage()));
    }

    /** {@code @Valid} 실패 — 어느 필드가 왜 틀렸는지까지 내려줍니다. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handle(MethodArgumentNotValidException e) {
        FieldError f = e.getBindingResult().getFieldError();
        String msg = (f == null)
                ? ErrorCode.VALIDATION_FAILED.defaultMessage()
                : f.getField() + " — " + f.getDefaultMessage();
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.status())
                .body(ApiResponse.fail(ErrorCode.VALIDATION_FAILED.name(), msg));
    }

    /**
     * 마지막 그물. <b>여기까지 온 것은 전부 우리 잘못입니다.</b>
     * 사용자에게 내부 메시지를 보여 주지 않습니다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handle(Exception e) {
        // TODO 로그 연결 — 발표 전까지는 콘솔로 충분합니다
        e.printStackTrace();
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.status())
                .body(ApiResponse.fail(ErrorCode.INTERNAL_ERROR.name(),
                        ErrorCode.INTERNAL_ERROR.defaultMessage()));
    }
}
