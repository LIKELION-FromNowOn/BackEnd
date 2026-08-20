package com.youin.now.common.error;

import org.springframework.http.HttpStatus;

/**
 * 실패 코드. <b>문자열을 코드에 직접 쓰지 말고 여기에 넣으십시오.</b>
 *
 * <p>화면 문구는 프론트가 이 {@code code} 로 분기합니다. 메시지를 바꿔도 분기는 안 깨집니다.
 * <b>없는 코드가 필요하면 여기에 추가하고 슬랙 {@code backend} 에 한 줄 올려 주십시오.</b>
 */
public enum ErrorCode {

    // 공통
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST,        "요청 형식이 올바르지 않습니다"),
    UNAUTHORIZED     (HttpStatus.UNAUTHORIZED,       "토큰이 없거나 만료되었습니다"),
    FORBIDDEN        (HttpStatus.FORBIDDEN,          "본인의 자원이 아닙니다"),
    NOT_FOUND        (HttpStatus.NOT_FOUND,          "찾을 수 없습니다"),
    /** 경로는 맞고 메서드가 틀린 경우. <b>2026-08-20 신설 — 그전에는 500 으로 나갔습니다.</b> */
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 방식입니다"),
    RATE_LIMITED     (HttpStatus.TOO_MANY_REQUESTS,  "호출 한도를 초과했습니다"),
    INTERNAL_ERROR   (HttpStatus.INTERNAL_SERVER_ERROR, "잠시 후 다시 시도해 주십시오"),

    // 인증
    EMAIL_ALREADY_EXISTS (HttpStatus.CONFLICT, "이미 등록된 이메일입니다"),
    INVALID_CREDENTIALS  (HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다"),
    USER_NOT_FOUND       (HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),
    NO_FIELDS            (HttpStatus.BAD_REQUEST, "수정할 값이 없습니다"),
    GUEST_FORBIDDEN      (HttpStatus.FORBIDDEN, "게스트는 프로필을 수정할 수 없습니다"),
    CHECKIN_NOT_FOUND    (HttpStatus.NOT_FOUND, "체크인을 찾을 수 없습니다"),
    NO_PROPOSAL          (HttpStatus.CONFLICT, "제안된 전환이 없습니다"),

    // 관리 항목
    MIN_ITEMS_REQUIRED(HttpStatus.BAD_REQUEST, "관리 항목을 3개 이상 선택해 주세요"),
    ITEM_NOT_FOUND    (HttpStatus.NOT_FOUND,   "항목을 찾을 수 없습니다"),
    FREQUENCY_REQUIRED(HttpStatus.BAD_REQUEST, "선택한 항목의 빈도를 골라 주십시오"),

    // 덜어내기
    /**
     * 오늘 상태 체크 전에 판정을 요청한 경우. 메시지는 명세서 {@code NOW-SUB-001} 실패 코드 표와 글자까지 같습니다.
     * <b>2026-08-20 신설 — 명세서에는 있었고 코드에만 없었습니다.</b>
     */
    NO_CHECKIN               (HttpStatus.CONFLICT,  "상태 체크를 먼저 진행해 주세요"),
    EVALUATION_NOT_FOUND     (HttpStatus.NOT_FOUND, "판정 결과를 찾을 수 없습니다"),
    ALREADY_REVERTED         (HttpStatus.CONFLICT,  "이미 되돌린 항목입니다"),
    /** 클리닉 안내로 제외된 항목은 되돌릴 수 없습니다. <b>앱이 클리닉 안내를 덮어쓰면 안 됩니다.</b> */
    CANNOT_REVERT_EXCLUDED   (HttpStatus.CONFLICT,  "이 항목은 되돌릴 수 없습니다"),
    RECOMMENDATION_PAUSED    (HttpStatus.CONFLICT,  "지금은 추천을 멈춘 상태입니다"),

    // 오늘의 행동
    ACTION_NOT_FOUND  (HttpStatus.NOT_FOUND,        "행동을 찾을 수 없습니다"),
    REROLL_LIMIT      (HttpStatus.TOO_MANY_REQUESTS,"오늘은 더 바꿀 수 없습니다"),
    ALREADY_COMPLETED (HttpStatus.CONFLICT,         "이미 완료한 행동은 바꿀 수 없습니다"),
    NO_EVALUATION     (HttpStatus.CONFLICT,         "덜어내기를 먼저 진행해 주세요"),

    // 기록
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "조회 기간이 올바르지 않습니다"),
    INVALID_PERIOD    (HttpStatus.BAD_REQUEST, "조회 단위가 올바르지 않습니다"),

    // AI
    LLM_UNAVAILABLE (HttpStatus.SERVICE_UNAVAILABLE, "기본값으로 처리했습니다"),
    TEXT_REJECTED   (HttpStatus.BAD_REQUEST,         "응답을 다시 생성했습니다");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status()        { return status; }
    public String defaultMessage()    { return defaultMessage; }
}
