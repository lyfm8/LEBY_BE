package com.example.be.repository.setting;

import com.example.be.entity.setting.ToeicScoreConversion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ToeicScoreConversionRepository extends JpaRepository<ToeicScoreConversion, Long> {

    Optional<ToeicScoreConversion> findByCorrectCount(Integer correctCount);
}
