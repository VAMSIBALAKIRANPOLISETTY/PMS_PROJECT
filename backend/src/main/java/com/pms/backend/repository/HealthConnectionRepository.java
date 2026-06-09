package com.pms.backend.repository;

import com.pms.backend.model.AppUser;
import com.pms.backend.model.HealthConnection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthConnectionRepository extends JpaRepository<HealthConnection, Long> {
    List<HealthConnection> findByUserOrderByCreatedAtDesc(AppUser user);
}
