package com.example.be.repository.ability;

import com.example.be.entity.ability.Ability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AbilityRepository extends JpaRepository<Ability, Long> {
    List<Ability> findByPartIdOrderByIdAsc(Long partId);
    long countByPartId(Long partId);
    boolean existsByPartIdAndName(Long partId, String name);
}
