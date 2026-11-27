package com.example.be.service.exam.util;

import com.example.be.entity.question.Question;
import com.example.be.repository.question.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Component dùng chung nạp hàng loạt câu hỏi kèm Part, Parent và QuestionMedias
 * trong đúng 2 truy vấn tách biệt nhằm triệt tiêu N+1 Query và tránh MultipleBagFetchException.
 */
@Component
@RequiredArgsConstructor
public class QuestionBatchLoader {

    private final QuestionRepository questionRepository;

    /**
     * Nạp đầy đủ thông tin Media của câu hỏi con và câu hỏi cha theo tập ID.
     *
     * @param questionIds Danh sách ID câu hỏi cần nạp
     * @return Map {@code questionId -> Question} đã gắn đầy đủ parent + media
     */
    public Map<Long, Question> loadDetailedQuestionsMap(Collection<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> distinctIds = questionIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (distinctIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Question> detailedQuestions = questionRepository.findAllWithMediasAndParentByIdIn(distinctIds);
        if (detailedQuestions.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> parentIds = detailedQuestions.stream()
                .map(Question::getParent)
                .filter(Objects::nonNull)
                .map(Question::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (!parentIds.isEmpty()) {
            Map<Long, Question> parentMap = questionRepository.findAllParentsWithMediasByIdIn(parentIds).stream()
                    .collect(Collectors.toMap(Question::getId, p -> p, (first, second) -> first));
            for (Question q : detailedQuestions) {
                if (q.getParent() != null && parentMap.containsKey(q.getParent().getId())) {
                    q.setParent(parentMap.get(q.getParent().getId()));
                }
            }
        }

        return detailedQuestions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q, (first, second) -> first));
    }
}
