package com.youin.now.subtract;

/**
 * {@code NOW-SUB-003} 되돌리기 응답.
 *
 * <p><b>판정 전체가 아니라 넷만 돌려줍니다.</b> 2026-08-20 이전에는 {@link SubtractRes}
 * 를 통째로 돌려주고 있었는데 명세와 달랐습니다.
 *
 * @param persisted <b>{@code true} 면 다음 판정에서도 {@code keep} 으로 고정됩니다.</b>
 *                  되돌리기는 한 번으로 끝난다는 뜻입니다 — 사용자가 같은 항목을
 *                  매번 되돌릴 일이 없습니다
 * @param summary   되돌린 뒤로 <b>갱신된</b> 건수
 */
public record SubtractRevertRes(String itemId, String verdict,
                                boolean persisted, SubtractRes.Summary summary) {}
