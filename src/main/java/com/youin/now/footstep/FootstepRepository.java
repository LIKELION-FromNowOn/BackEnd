package com.youin.now.footstep;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FootstepRepository extends JpaRepository<FootstepEntity, String> {

    /** {@code footsteps} 에 {@code sort_order} 가 없어 id 순입니다. ULID 라 생성 순서와 같습니다 */
    List<FootstepEntity> findAllByOrderByIdAsc();
}