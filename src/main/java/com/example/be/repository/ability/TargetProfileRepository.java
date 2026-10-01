package com.example.be.repository.ability;

import com.example.be.entity.ability.TargetProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TargetProfileRepository extends JpaRepository<TargetProfile, Long> {
}
