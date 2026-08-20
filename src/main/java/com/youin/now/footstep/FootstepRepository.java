package com.youin.now.footstep;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FootstepRepository extends JpaRepository<FootstepEntity, String> {

    List<FootstepEntity> findAllByOrderByIdAsc();
}