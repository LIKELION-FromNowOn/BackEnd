package com.youin.now.common.error;

import com.youin.now.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    /** 날짜·숫자 쿼리 파라미터 형식이 맞지 않을 때 마지막 예외 처리로 500이 되는 것을 막습니다. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handle(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.status())
                .body(ApiResponse.fail(ErrorCode.VALIDATION_FAILED.name(),
                        ErrorCode.VALIDATION_FAILED.defaultMessage()));
    }

    /**
     * 없는 경로. <b>스프링 자체 예외라 마지막 그물이 먼저 잡으면 500 으로 나갑니다.</b>
     *
     * <p>두 가지를 함께 잡습니다. 스프링 6.0 부터 정적 리소스 핸들러가 모든 경로에 매핑돼 있어
     * {@link NoHandlerFoundException} 대신 {@link NoResourceFoundException} 이 던져지는데,
     * <b>어느 쪽이 오든 404 로 나가야 합니다.</b>
     *
     * <p>2026-08-20 이전에는 없는 경로가 {@code 500 INTERNAL_ERROR} 로 나갔습니다.
     * 프론트가 <b>URL 오타를 서버 장애로 오인</b>했습니다.
     */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleNotFound(Exception e) {
        return ResponseEntity.status(ErrorCode.NOT_FOUND.status())
                .body(ApiResponse.fail(ErrorCode.NOT_FOUND.name(),
                        ErrorCode.NOT_FOUND.defaultMessage()));
    }

    /**
     * 본문을 읽을 수 없는 경우 — 깨진 JSON · 잘못된 문자 인코딩 · 타입 불일치.
     *
     * <p>이것도 이전에는 500 이었습니다. <b>보낸 쪽 잘못인데 서버 장애로 보였습니다.</b>
     *
     * <p>2026-08-20 에 상태 API 를 시험하다 발견했습니다. 콘솔이 한글을 MS949 로 보내
     * {@code Invalid UTF-8 middle byte} 가 났는데 응답이 {@code 500 INTERNAL_ERROR} 였습니다.
     * <b>프론트가 인코딩 문제를 서버 장애로 오인합니다.</b>
     *
     * <p><b>원인 메시지를 사용자에게 보여 주지 않습니다.</b> 내부 구조가 드러납니다.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handle(HttpMessageNotReadableException e) {
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.status())
                .body(ApiResponse.fail(ErrorCode.VALIDATION_FAILED.name(),
                        "요청 본문을 읽을 수 없습니다. JSON 형식과 UTF-8 인코딩을 확인해 주십시오"));
    }

    /** 경로는 맞고 메서드가 틀린 경우. 이것도 이전에는 500 이었습니다. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handle(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(ErrorCode.METHOD_NOT_ALLOWED.status())
                .body(ApiResponse.fail(ErrorCode.METHOD_NOT_ALLOWED.name(),
                        ErrorCode.METHOD_NOT_ALLOWED.defaultMessage()));
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
