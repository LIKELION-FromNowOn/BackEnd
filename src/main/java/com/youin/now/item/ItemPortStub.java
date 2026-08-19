package com.youin.now.item;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * <b>임시 구현입니다. 관리 항목 API 가 들어오면 이 파일을 지우십시오.</b>
 *
 * <p>왜 인터페이스만 올리지 않았는가 —
 * <b>인터페이스만 있으면 주입하는 순간 앱이 안 뜹니다.</b>
 * {@code NoSuchBeanDefinitionException} 이 나서 판정 API 를 만들 수가 없습니다.
 *
 * <p>항상 <b>빈 목록</b>을 돌려줍니다. 부르는 쪽에서는
 * <b>「이 사람이 아직 관리 항목을 안 골랐다」</b>로 읽으면 됩니다.
 * 지어낸 항목을 돌려주지 않습니다 — 가짜 데이터로 만든 판정은 검증이 안 됩니다.
 *
 * <p><b>판정 결과가 비는 것이 정상입니다.</b> 마스터 시드와 항목 저장이 들어와야 실데이터가 흐릅니다.
 *
 * <hr>
 *
 * <p><b>실제 구현을 만드실 때</b> — 이 파일을 <b>지우고</b> 같은 인터페이스를 구현한
 * {@code @Component} 를 하나만 두십시오. 둘이 동시에 있으면 스프링이
 * 「빈이 둘」이라고 기동을 거부합니다. <b>그때 이 주석을 떠올리시면 원인을 바로 찾습니다.</b>
 *
 * <p>작성 2026-08-20 · 송원석 · 근거 {@code .agent/REQUESTS.md} #19
 */
@Component
public class ItemPortStub implements ItemPort {

    @Override
    public List<SelectedItem> selected(String userId) {
        return List.of();
    }
}
