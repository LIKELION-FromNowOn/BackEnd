package com.youin.now.checkin;

import java.util.List;
import java.util.Set;

/**
 * 이상 징후 마스터를 읽는 창구. <b>{@code master/} 가 제공합니다 — 김민정 님 패키지입니다.</b>
 *
 * <p>남의 {@code Repository} 를 직접 부르지 않는다는 규칙({@code docs/04-ports.md:3})에 따라
 * 창구를 하나 둡니다. {@code checkin/} 은 이 인터페이스만 압니다.
 *
 * <p><b>{@code docs/04-ports.md} 의 다섯 개에는 없습니다.</b> 상태 API 를 만들면서 생긴
 * 여섯 번째이고, 만드는 쪽과 쓰는 쪽이 문서의 넷과 같은 규약을 따릅니다.
 *
 * <p><b>2026-08-20 가중치만 넘기던 것을 이름까지 넘기도록 넓혔습니다.</b>
 * 명세서 {@code NOW-STATE-001} 의 {@code reasons} 가 <b>징후 이름 목록</b>인데,
 * 이름이 없어서 빈 배열을 내보내고 있었습니다. 그러면 화면에 <b>근거 없는 전환 제안</b>이 뜹니다.
 */
public interface SignalWeightPort {

    /**
     * @param signalIds 사용자가 고른 마스터 징후 번호들
     * @return <b>없는 번호는 결과에서 빠집니다.</b> 예외를 던지지 않습니다 —
     *         프론트가 옛 번호를 들고 있어도 체크인 자체는 성공해야 합니다.
     *         <b>가중치 큰 것부터</b> 정렬해 돌려주십시오. 같으면 {@code sortOrder} 순입니다.
     *         명세서 처리 규칙 5번 「같은 입력에는 항상 같은 결과」를 위해 순서가 정해져 있어야 합니다
     */
    List<SignalInfo> find(Set<String> signalIds);

    /** @param weight 14건을 다 더하면 25 입니다. 임계값 5 는 그 합 위에서 정해졌습니다 */
    record SignalInfo(String id, String name, int weight) {}
}
