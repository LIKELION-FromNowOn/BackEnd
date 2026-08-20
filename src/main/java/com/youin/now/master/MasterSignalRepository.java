package com.youin.now.master;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * {@code signals} 조회. <b>읽기만 합니다.</b>
 *
 * <p>⚠️ {@code master/} 는 김민정 님 폴더입니다. 이 파일의 사연은 {@link MasterSignal} 주석에 있습니다.
 */
public interface MasterSignalRepository extends JpaRepository<MasterSignal, String> {

    List<MasterSignal> findByIdIn(Collection<String> ids);
}
