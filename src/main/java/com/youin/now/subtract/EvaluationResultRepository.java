package com.youin.now.subtract;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** {@code evaluation_results} 접근. <b>{@code subtract/} 밖에서 부르지 않습니다.</b> */
public interface EvaluationResultRepository extends JpaRepository<EvaluationResult, String> {

    List<EvaluationResult> findByEvaluationId(String evaluationId);

    Optional<EvaluationResult> findByEvaluationIdAndUserItemId(String evaluationId, String userItemId);

    void deleteByEvaluationId(String evaluationId);
}
