package com.youin.now.coach;

import java.util.List;

/**
 * LLM 이 돌려주는 것. <b>이 둘뿐입니다.</b>
 *
 * <p>{@code level} · {@code basis} · {@code generatedBy} 는 <b>서버가 실어 보냅니다.</b>
 * LLM 출력에 넣지 못하게 프롬프트에 못 박아 두었고, 여기에도 자리가 없습니다 —
 * <b>모델이 넣어도 들어올 데가 없습니다.</b>
 */
public record CoachAnswer(String answer, List<String> chips) {
}
