package com.pms.backend.repository;

import com.pms.backend.model.AppUser;
import com.pms.backend.model.HealthTimelineRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthTimelineRecordRepository extends JpaRepository<HealthTimelineRecord, Long> {
    List<HealthTimelineRecord> findByUserOrderByObservedAtDescCreatedAtDesc(AppUser user);
}
