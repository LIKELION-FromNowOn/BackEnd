package com.youin.now.footstep;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class FootstepService {

    private final FootstepRepository footstepRepository;

    public FootstepService(FootstepRepository footstepRepository) {
        this.footstepRepository = footstepRepository;
    }

    public List<FootstepRes> getFootsteps() {
        return footstepRepository.findAllByOrderByIdAsc()
                .stream()
                .map(FootstepRes::from)
                .toList();
    }
}