package com.example.be.config;

import com.example.be.entity.content.Part;
import com.example.be.enums.ability.ESection;
import com.example.be.repository.content.PartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PartDataInitializer implements CommandLineRunner {

    private final PartRepository partRepository;

    @Override
    public void run(String... args) {
        if (partRepository.count() == 0) {
            log.info("Seeding initial 7 TOEIC Parts from ETS standard...");

            List<Part> initialParts = List.of(
                    createPart("Part 1: Photographs", "Nghe và chọn câu mô tả đúng nhất cho bức ảnh", ESection.LISTENING),
                    createPart("Part 2: Question-Response", "Nghe 1 câu hỏi/phát biểu và chọn câu phản hồi phù hợp nhất", ESection.LISTENING),
                    createPart("Part 3: Conversations", "Nghe đoạn hội thoại giữa 2-3 người và trả lời câu hỏi", ESection.LISTENING),
                    createPart("Part 4: Talks", "Nghe bài nói độc thoại ngắn và trả lời câu hỏi", ESection.LISTENING),
                    createPart("Part 5: Incomplete Sentences", "Chọn từ/cụm từ đúng để hoàn thành câu", ESection.READING),
                    createPart("Part 6: Text Completion", "Điền từ/câu thích hợp vào chỗ trống trong đoạn văn bản", ESection.READING),
                    createPart("Part 7: Reading Comprehension", "Đọc hiểu đoạn đơn hoặc đa đoạn văn bản và trả lời câu hỏi", ESection.READING)
            );

            partRepository.saveAll(initialParts);
            log.info("Successfully seeded 7 TOEIC Parts.");
        }
    }

    private Part createPart(String name, String description, ESection section) {
        Part part = new Part();
        part.setName(name);
        part.setDescription(description);
        part.setSections(section);
        return part;
    }
}
