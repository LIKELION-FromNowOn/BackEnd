package com.youin.now.subtract;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;

/**
 * LLM 이 돌려주는 것. <b>근거 문장뿐입니다.</b>
 *
 * <p>{@code verdict} 는 코드가 이미 정했습니다. <b>받는 자리에 칸이 없으니
 * 모델이 넣어도 들어올 데가 없습니다.</b>
 *
 * <p>필드 이름을 조금 다르게 쓰는 경우가 있어 별칭을 뒀습니다 —
 * 2026-08-20 코치에서 {@code answer} 대신 {@code message} 로 온 적이 있습니다.
 */
public record SubtractReasons(
        @JsonAlias({"results", "items"}) List<Reason> reasons) {

    public record Reason(@JsonAlias({"id"}) String itemId,
                         @JsonAlias({"text", "sentence"}) String reason) {}
}
