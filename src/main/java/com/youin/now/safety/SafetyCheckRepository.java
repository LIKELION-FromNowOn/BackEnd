package com.youin.now.safety;

import org.springframework.data.jpa.repository.JpaRepository;

/** {@code safety_checks} 기록. <b>쓰기만 하고 읽는 API 는 없습니다</b> — 검수용입니다. */
public interface SafetyCheckRepository extends JpaRepository<SafetyCheck, String> {
}
