package com.youin.now.checkin;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** {@code checkin_signals} 접근. <b>{@code checkin/} 밖에서 부르지 않습니다.</b> */
public interface CheckinSignalRepository extends JpaRepository<CheckinSignal, String> {

    List<CheckinSignal> findByCheckinId(String checkinId);

    void deleteByCheckinId(String checkinId);
}
