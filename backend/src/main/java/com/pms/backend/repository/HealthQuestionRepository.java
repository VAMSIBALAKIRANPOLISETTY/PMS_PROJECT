package com.pms.backend.repository;

import com.pms.backend.model.HealthQuestion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthQuestionRepository extends JpaRepository<HealthQuestion, Long> {
    List<HealthQuestion> findByActiveTrueOrderBySymptomKeyAsc();
}
