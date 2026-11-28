package com.example.be.service.exam.implement;

import com.example.be.dto.response.exam.PracticeRoomResponse;
import com.example.be.entity.content.Lesson;
import com.example.be.entity.question.PracticeQuestion;
import com.example.be.entity.question.Question;
import com.example.be.enums.content.ELessonType;
import com.example.be.enums.content.EModuleStatus;
import com.example.be.exception.BaseException;
import com.example.be.exception.ErrorCode;
import com.example.be.mapper.exam.PracticeRoomMapper;
import com.example.be.repository.content.LessonRepository;
import com.example.be.repository.question.PracticeQuestionRepository;
import com.example.be.service.exam.service.UserPracticeService;
import com.example.be.service.exam.util.QuestionBatchLoader;
import com.example.be.service.exam.util.UserExamAccessHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Triển khai nghiệp vụ Phòng luyện tập bài thực hành (UC12) cho học viên.
 * Đọc câu hỏi từ bảng {@code practice_questions}, tự động nạp ngữ cảnh bài đọc/audio câu hỏi cha (Part 3, 4, 6, 7),
 * và kiểm soát mở khóa tuần tự BR10 trước khi cho phép truy cập bài luyện tập.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserPracticeServiceImpl implements UserPracticeService {

    private final LessonRepository lessonRepository;
    private final PracticeQuestionRepository practiceQuestionRepository;
    private final QuestionBatchLoader questionBatchLoader;
    private final UserExamAccessHelper userExamAccessHelper;
    private final PracticeRoomMapper practiceRoomMapper;

    @Override
    @Transactional(readOnly = true)
    public PracticeRoomResponse getPracticeRoom(Long userId, Long lessonId) {
        // Bước 1: Nạp bài học kèm Module, Part và Ability trong 1 query
        Lesson lesson = lessonRepository.findByIdWithModulePartAndAbility(lessonId)
                .orElseThrow(() -> new BaseException(ErrorCode.PRACTICE_LESSON_NOT_FOUND));

        if (lesson.getStatus() != EModuleStatus.PUBLISHED || lesson.getModule() == null) {
            throw new BaseException(ErrorCode.PRACTICE_LESSON_NOT_FOUND);
        }
        if (lesson.getLessonType() != ELessonType.PRACTICE) {
            throw new BaseException(ErrorCode.INVALID_LESSON_TYPE);
        }

        // Bước 2: Kiểm tra quyền truy cập mô-đun (chặn LOCKED theo BR10 nếu trong lộ trình; mở Học tự do nếu PUBLISHED ngoài lộ trình)
        userExamAccessHelper.requireAccessibleModuleForStudy(userId, lesson.getModule());

        // Bước 3: Nạp danh sách câu hỏi thực hành theo thứ tự orderNo tăng dần
        List<PracticeQuestion> practiceQuestions = practiceQuestionRepository
                .findAllWithQuestionAndPartByLessonId(lessonId);

        if (practiceQuestions.isEmpty()) {
            log.info("[UC12] Bài luyện tập lessonId={} chưa có câu hỏi (Ngoại lệ 2a)", lessonId);
            return practiceRoomMapper.toRoomResponse(lesson, Collections.emptyList(), Collections.emptyMap());
        }

        // Bước 4: Nạp hàng loạt Media của câu hỏi con và câu hỏi cha trong 2 truy vấn (tránh N+1 & MultipleBagFetchException)
        List<Long> questionIds = practiceQuestions.stream()
                .map(PracticeQuestion::getQuestion)
                .filter(Objects::nonNull)
                .map(Question::getId)
                .toList();
        Map<Long, Question> detailedQuestionMap = questionBatchLoader.loadDetailedQuestionsMap(questionIds);

        // Bước 5: Chuyển đổi sang DTO Phòng luyện tập thông qua PracticeRoomMapper
        return practiceRoomMapper.toRoomResponse(lesson, practiceQuestions, detailedQuestionMap);
    }
}
