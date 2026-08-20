package com.youin.now.note;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareNoteRuleRepository extends JpaRepository<CareNoteRule, String> {

    List<CareNoteRule> findByCareNoteIdOrderBySentNoAsc(String careNoteId);
}
