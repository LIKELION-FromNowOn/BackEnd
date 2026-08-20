package com.youin.now.coach;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;

/**
 * LLM 이 돌려주는 것. <b>이 둘뿐입니다.</b>
 *
 * <p>{@code level} · {@code basis} · {@code generatedBy} 는 <b>서버가 실어 보냅니다.</b>
 * LLM 출력에 넣지 못하게 프롬프트에 못 박아 두었고, 여기에도 자리가 없습니다 —
 * <b>모델이 넣어도 들어올 데가 없습니다.</b>
 */
public record CoachAnswer(
        /*
         * 모델이 answer 대신 message 를 쓸 때가 있습니다.
         * 2026-08-20 실서버에서 [message, chips] 로 온 것을 봤고,
         * 그때 answer 가 null 이라 답이 통째로 버려졌습니다.
         * 프롬프트에 answer 로 적어 두었지만 모델은 가끔 다르게 씁니다.
         */
        @JsonAlias({"message", "text", "content", "reply"}) String answer,
        @JsonAlias({"suggestions", "followups"}) List<String> chips) {
}
