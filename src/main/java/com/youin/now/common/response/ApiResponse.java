package com.youin.now.common.response;

/**
 * 모든 응답의 봉투. <b>예외 없이 이 모양으로만 나갑니다.</b>
 *
 * <pre>
 * 성공  { "ok": true,  "data": { ... } }
 * 실패  { "ok": false, "error": { "code": "MIN_ITEMS_REQUIRED", "message": "..." } }
 * </pre>
 *
 * <p>세 사람이 각자 만들면 성공 응답이 세 가지 모양이 되고,
 * 프론트가 API 마다 다르게 파싱해야 합니다. <b>그래서 하나만 씁니다.</b>
 */
public record ApiResponse<T>(boolean ok, T data, ApiError error) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> fail(String code, String message) {
        return new ApiResponse<>(false, null, new ApiError(code, message));
    }

    public record ApiError(String code, String message) {}
}
