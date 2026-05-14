package com.pms.backend.repository;

import com.pms.backend.model.AppUser;
import com.pms.backend.model.Assessment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {
    List<Assessment> findByUserOrderByCreatedAtDesc(AppUser user);
    List<Assessment> findAllByOrderByCreatedAtDesc();
}
