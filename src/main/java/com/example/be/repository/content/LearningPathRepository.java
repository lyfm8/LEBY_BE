package com.example.be.repository.content;

import com.example.be.entity.content.LearningPath;
import com.example.be.enums.content.ELearningPathStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LearningPathRepository extends JpaRepository<LearningPath, Long> {

    List<LearningPath> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<LearningPath> findByUserIdAndStatus(Long userId, ELearningPathStatus status);
}
