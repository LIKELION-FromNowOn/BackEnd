package com.youin.now.common.error;

/**
 * 업무 예외. <b>{@code throw new ApiException(ErrorCode.XXX)} 한 줄이면 됩니다.</b>
 * 응답 포장은 {@link GlobalExceptionHandler} 가 합니다.
 */
public class ApiException extends RuntimeException {

    private final ErrorCode code;

    public ApiException(ErrorCode code) {
        super(code.defaultMessage());
        this.code = code;
    }

    /** 기본 메시지 대신 상황에 맞는 문구를 쓸 때. <b>코드는 바꾸지 마십시오.</b> */
    public ApiException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode code() { return code; }
}
