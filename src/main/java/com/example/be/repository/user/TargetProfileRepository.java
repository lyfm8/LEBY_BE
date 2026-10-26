package com.example.be.repository.user;

import com.example.be.entity.user.TargetProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TargetProfileRepository extends JpaRepository<TargetProfile, Long> {
}
