package com.pms.backend.repository;

import com.pms.backend.model.AdminRule;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRuleRepository extends JpaRepository<AdminRule, Long> {
    List<AdminRule> findByActiveTrueOrderByConditionLabelAsc();
    boolean existsByConditionLabelIgnoreCase(String conditionLabel);
    Optional<AdminRule> findByConditionLabelIgnoreCase(String conditionLabel);
}
