package com.example.be.repository.question;

import com.example.be.entity.question.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    long countByPartId(Long partId);
}
