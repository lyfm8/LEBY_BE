package com.example.be.repository.content;

import com.example.be.entity.content.Part;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PartRepository extends JpaRepository<Part, Long> {
    Optional<Part> findByName(String name);
    boolean existsByName(String name);
}
