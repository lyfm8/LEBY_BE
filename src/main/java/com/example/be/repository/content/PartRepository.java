package com.example.be.repository.content;

import com.example.be.entity.content.Part;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartRepository extends JpaRepository<Part, Long> {
    Optional<Part> findByName(String name);
    boolean existsByName(String name);

    /**
     * Lấy toàn bộ 7 phần thi TOEIC sắp xếp theo thứ tự tăng dần của ID (Part 1 đến Part 7).
     *
     * @return Danh sách toàn bộ Part theo đúng trình tự chuẩn TOEIC
     */
    List<Part> findAllByOrderByIdAsc();
}
