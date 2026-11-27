package com.example.be.mapper.content;

import com.example.be.dto.response.content.LessonProgressResponse;
import com.example.be.dto.response.content.UserLessonDetailResponse;
import com.example.be.dto.response.content.UserLessonNavItemResponse;
import com.example.be.dto.response.content.UserLessonReviewQuestionResponse;
import com.example.be.dto.response.content.UserLessonReviewQuestionResponse.ReviewExplanationItem;
import com.example.be.dto.response.content.UserLessonReviewQuestionResponse.ReviewOptionItem;
import com.example.be.dto.response.content.UserLessonReviewQuestionResponse.ReviewVocabularyItem;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.content.Lesson;
import com.example.be.entity.content.LessonProgress;
import com.example.be.entity.content.Module;
import com.example.be.entity.content.Part;
import com.example.be.entity.media.Media;
import com.example.be.entity.question.PracticeQuestion;
import com.example.be.entity.question.Question;
import com.example.be.entity.question.QuestionMedia;
import com.example.be.enums.ability.ESection;
import com.example.be.enums.content.ELessonType;
import com.example.be.enums.media.EMediaResourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Mapper chuyên biệt chuyển đổi Entity sang DTO cho màn hình Phòng học bài giảng Video (UC11)
 * và kết quả đồng bộ tiến độ xem bài giảng (LessonProgress).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserLessonMapper {

    private static final String DEFAULT_PART_NAME = "Chuyên đề tổng hợp";
    private static final int FULL_PERCENT = 100;
    private static final int ZERO_VALUE = 0;
    private static final Set<String> AUDIO_FORMATS = Set.of("mp3", "wav", "ogg", "m4a", "aac", "flac");

    private final ObjectMapper objectMapper;

    /**
     * Chuyển đổi một bài học cùng tiến độ của học viên sang mục hiển thị trong sidebar điều hướng.
     *
     * @param lesson              Bài học đã xuất bản trong Module
     * @param progress            Bản ghi tiến độ của học viên (có thể null nếu chưa từng xem)
     * @param inheritedCompleted  Cờ xác nhận toàn bộ bài học trong Module đã được bảo lưu hoàn thành (BR06)
     * @return {@link UserLessonNavItemResponse}
     */
    public UserLessonNavItemResponse toNavItemResponse(
            Lesson lesson,
            LessonProgress progress,
            boolean inheritedCompleted
    ) {
        boolean isCompleted = inheritedCompleted
                || (progress != null && Boolean.TRUE.equals(progress.getIsCompleted()));
        int watchPercent = isCompleted
                ? FULL_PERCENT
                : (progress != null ? clampPercent(progress.getWatchPercent()) : ZERO_VALUE);

        return UserLessonNavItemResponse.builder()
                .id(lesson.getId())
                .orderNo(lesson.getOrderNo())
                .title(lesson.getTitle())
                .lessonType(lesson.getLessonType())
                .durationSeconds(safeNonNegative(lesson.getDurationSeconds()))
                .isCompleted(isCompleted)
                .watchPercent(watchPercent)
                .build();
    }

    /**
     * Tổng hợp trọn vẹn dữ liệu Phòng học bài giảng Video (UC11) cho học viên.
     *
     * @param lesson                 Thực thể bài học (đã nạp Module, Part, Ability)
     * @param progress               Tiến độ xem hiện tại của học viên (có thể null)
     * @param inheritedCompleted     Cờ bảo lưu hoàn thành bài học từ lộ trình trước (BR06)
     * @param requiredWatchPercent   Ngưỡng % thời lượng xem cần đạt (đọc động theo BR09)
     * @param moduleProgressPercent  Tỷ lệ hoàn thành hiện tại của Module (%)
     * @param completedVideoLessons  Số bài giảng Video đã hoàn thành trong Module
     * @param totalVideoLessons      Tổng số bài giảng Video đã xuất bản trong Module
     * @param requiredCompletionRate Tỷ lệ hoàn thành bài giảng tối thiểu mở khóa Module Test (BR03)
     * @param moduleTestUnlocked     Cờ xác nhận bài thi vượt ải đã mở khóa hay chưa
     * @param prevLessonId           ID bài học liền trước trong Module (có thể null)
     * @param nextLesson             Bài học liền sau trong Module (có thể null)
     * @param moduleLessons          Danh sách điều hướng toàn bộ bài học trong Module
     * @param reviewQuestions        Danh sách câu hỏi ôn tập củng cố đã bóc tách JSON
     * @return {@link UserLessonDetailResponse}
     */
    public UserLessonDetailResponse toUserLessonDetailResponse(
            Lesson lesson,
            LessonProgress progress,
            boolean inheritedCompleted,
            int requiredWatchPercent,
            int moduleProgressPercent,
            int completedVideoLessons,
            int totalVideoLessons,
            int requiredCompletionRate,
            boolean moduleTestUnlocked,
            Long prevLessonId,
            Lesson nextLesson,
            List<UserLessonNavItemResponse> moduleLessons,
            List<UserLessonReviewQuestionResponse> reviewQuestions
    ) {
        Module module = lesson.getModule();
        Part part = module != null ? module.getPart() : null;
        Ability ability = lesson.getAbility();

        boolean isCompleted = inheritedCompleted
                || (progress != null && Boolean.TRUE.equals(progress.getIsCompleted()));
        int watchedSeconds = progress != null ? safeNonNegative(progress.getWatchedSeconds()) : ZERO_VALUE;
        int lastPositionSeconds = progress != null ? safeNonNegative(progress.getLastPositionSeconds()) : ZERO_VALUE;
        int watchPercent = progress != null
                ? clampPercent(progress.getWatchPercent())
                : (isCompleted ? FULL_PERCENT : ZERO_VALUE);

        return UserLessonDetailResponse.builder()
                .id(lesson.getId())
                .moduleId(module != null ? module.getId() : null)
                .moduleTitle(module != null ? module.getTitle() : "")
                .partName(part != null ? part.getName() : DEFAULT_PART_NAME)
                .section(resolveSection(part))
                .title(lesson.getTitle())
                .orderNo(lesson.getOrderNo())
                .lessonType(lesson.getLessonType())
                .descriptions(lesson.getDescriptions())
                .videoUrl(lesson.getVideoUrl())
                .audioUrl(lesson.getAudioUrl())
                .imageUrl(lesson.getImageUrl())
                .durationSeconds(safeNonNegative(lesson.getDurationSeconds()))
                .isFree(Boolean.TRUE.equals(lesson.getIsFree()))
                .abilityName(ability != null ? ability.getName() : null)
                .watchedSeconds(watchedSeconds)
                .lastPositionSeconds(lastPositionSeconds)
                .watchPercent(watchPercent)
                .requiredWatchPercent(requiredWatchPercent)
                .isCompleted(isCompleted)
                .completedAt(progress != null ? progress.getCompletedAt() : null)
                .moduleProgressPercent(clampPercent(moduleProgressPercent))
                .completedVideoLessons(Math.max(ZERO_VALUE, completedVideoLessons))
                .totalVideoLessons(Math.max(ZERO_VALUE, totalVideoLessons))
                .requiredCompletionRate(requiredCompletionRate)
                .moduleTestUnlocked(moduleTestUnlocked)
                .prevLessonId(prevLessonId)
                .nextLessonId(nextLesson != null ? nextLesson.getId() : null)
                .nextLessonType(nextLesson != null ? nextLesson.getLessonType() : null)
                .moduleLessons(moduleLessons != null ? moduleLessons : Collections.emptyList())
                .reviewQuestions(reviewQuestions != null ? reviewQuestions : Collections.emptyList())
                .build();
    }

    /**
     * Chuyển đổi kết quả sau khi lưu tiến độ bài giảng và tính lại tiến độ Module sang DTO phản hồi.
     *
     * @param progress               Bản ghi LessonProgress vừa được lưu
     * @param effectiveDuration      Thời lượng video chuẩn dùng để tính tỷ lệ
     * @param requiredWatchPercent   Ngưỡng % hoàn thành video theo BR09
     * @param justCompleted          true nếu bài học vừa đạt trạng thái hoàn thành ở đúng lần đồng bộ này
     * @param moduleProgressPercent  Tỷ lệ hoàn thành mới của Module (%)
     * @param completedVideoLessons  Số bài giảng Video đã hoàn thành trong Module
     * @param totalVideoLessons      Tổng số bài giảng Video của Module
     * @param requiredCompletionRate Ngưỡng mở khóa bài thi vượt ải (BR03)
     * @param moduleTestUnlocked     Trạng thái mở khóa bài thi vượt ải sau cập nhật
     * @param moduleTestJustUnlocked true nếu bài thi vượt ải vừa được mở khóa nhờ lần đồng bộ này
     * @return {@link LessonProgressResponse}
     */
    public LessonProgressResponse toLessonProgressResponse(
            LessonProgress progress,
            int effectiveDuration,
            int requiredWatchPercent,
            boolean justCompleted,
            int moduleProgressPercent,
            int completedVideoLessons,
            int totalVideoLessons,
            int requiredCompletionRate,
            boolean moduleTestUnlocked,
            boolean moduleTestJustUnlocked
    ) {
        return LessonProgressResponse.builder()
                .lessonId(progress.getLesson() != null ? progress.getLesson().getId() : null)
                .watchedSeconds(safeNonNegative(progress.getWatchedSeconds()))
                .lastPositionSeconds(safeNonNegative(progress.getLastPositionSeconds()))
                .durationSeconds(Math.max(ZERO_VALUE, effectiveDuration))
                .watchPercent(clampPercent(progress.getWatchPercent()))
                .requiredWatchPercent(requiredWatchPercent)
                .isCompleted(Boolean.TRUE.equals(progress.getIsCompleted()))
                .justCompleted(justCompleted)
                .completedAt(progress.getCompletedAt())
                .moduleProgressPercent(clampPercent(moduleProgressPercent))
                .completedVideoLessons(Math.max(ZERO_VALUE, completedVideoLessons))
                .totalVideoLessons(Math.max(ZERO_VALUE, totalVideoLessons))
                .requiredCompletionRate(requiredCompletionRate)
                .moduleTestUnlocked(moduleTestUnlocked)
                .moduleTestJustUnlocked(moduleTestJustUnlocked)
                .build();
    }

    /**
     * Chuyển đổi danh sách PracticeQuestion gắn trên bài giảng Video sang danh sách câu hỏi ôn tập cho FE.
     * Bóc tách an toàn các trường JSON (questionData, correctAnswer, explanation) và kết hợp media từ QuestionMedia.
     *
     * @param practiceQuestions Danh sách PracticeQuestion (đã JOIN FETCH question và parent)
     * @param mediaByQuestionId Map danh sách QuestionMedia theo questionId (đã nạp theo lô 1 query IN)
     * @return Danh sách {@link UserLessonReviewQuestionResponse}
     */
    public List<UserLessonReviewQuestionResponse> toReviewQuestionResponses(
            List<PracticeQuestion> practiceQuestions,
            Map<Long, List<QuestionMedia>> mediaByQuestionId
    ) {
        if (practiceQuestions == null || practiceQuestions.isEmpty()) {
            return Collections.emptyList();
        }

        List<UserLessonReviewQuestionResponse> results = new ArrayList<>(practiceQuestions.size());
        for (PracticeQuestion pq : practiceQuestions) {
            if (pq == null || pq.getQuestion() == null) {
                continue;
            }
            results.add(mapSingleReviewQuestion(pq, mediaByQuestionId));
        }
        return results;
    }

    /**
     * Chuyển đổi 1 bản ghi PracticeQuestion sang DTO câu hỏi ôn tập kèm ngữ cảnh cha (nếu là câu hỏi chùm).
     */
    private UserLessonReviewQuestionResponse mapSingleReviewQuestion(
            PracticeQuestion pq,
            Map<Long, List<QuestionMedia>> mediaByQuestionId
    ) {
        Question question = pq.getQuestion();
        Question parent = question.getParent();

        // Bước 1: Bóc tách nội dung câu hỏi, phương án lựa chọn và media dự phòng từ chuỗi JSON question_data
        JsonNode qData = parseJsonSafely(question.getQuestionData(), "questionData", question.getId());
        String questionText = extractFirstText(qData, "text", "question", "prompt", "content");
        if (questionText.isBlank()) {
            questionText = question.getName() != null ? question.getName() : "";
        }

        List<ReviewOptionItem> options = extractOptions(qData);
        MediaPair questionMediaPair = resolveMediaUrls(
                mediaByQuestionId.get(question.getId()),
                extractFirstText(qData, "imageUrl", "image_url"),
                extractFirstText(qData, "audioUrl", "audio_url")
        );

        // Bước 2: Nếu là câu hỏi con thuộc chùm (Part 3, 4, 6, 7), bóc tách thêm đoạn văn & media của câu hỏi cha
        String parentContextText = null;
        String parentImageUrl = null;
        String parentAudioUrl = null;
        if (parent != null) {
            JsonNode parentData = parseJsonSafely(parent.getQuestionData(), "parent.questionData", parent.getId());
            String extractedParentText = extractFirstText(parentData, "passage", "context", "text", "content");
            parentContextText = extractedParentText.isBlank() ? null : extractedParentText;

            MediaPair parentMediaPair = resolveMediaUrls(
                    mediaByQuestionId.get(parent.getId()),
                    extractFirstText(parentData, "imageUrl", "image_url"),
                    extractFirstText(parentData, "audioUrl", "audio_url")
            );
            parentImageUrl = parentMediaPair.imageUrl;
            parentAudioUrl = parentMediaPair.audioUrl;
        }

        // Bước 3: Bóc tách đáp án đúng và lời giải chi tiết từ JSON
        String correctAnswer = extractCorrectAnswer(question.getCorrectAnswer(), question.getId());
        ReviewExplanationItem explanation = extractExplanation(question.getExplanation(), question.getId());

        return UserLessonReviewQuestionResponse.builder()
                .id(pq.getId())
                .questionId(question.getId())
                .orderNo(pq.getOrderNo())
                .points(pq.getPoints())
                .questionText(questionText)
                .imageUrl(questionMediaPair.imageUrl)
                .audioUrl(questionMediaPair.audioUrl)
                .parentContextText(parentContextText)
                .parentImageUrl(parentImageUrl)
                .parentAudioUrl(parentAudioUrl)
                .options(options)
                .correctAnswer(correctAnswer)
                .explanation(explanation)
                .build();
    }

    /**
     * Trích xuất danh sách phương án A/B/C/D từ nút JSON question_data (hỗ trợ cả dạng mảng object lẫn map key-value).
     */
    private List<ReviewOptionItem> extractOptions(JsonNode qData) {
        if (qData == null || !qData.has("options")) {
            return Collections.emptyList();
        }
        JsonNode optionsNode = qData.get("options");
        List<ReviewOptionItem> items = new ArrayList<>();

        if (optionsNode.isArray()) {
            for (JsonNode optNode : optionsNode) {
                String key = optNode.hasNonNull("key") ? optNode.get("key").asText("").trim() : "";
                String text = optNode.hasNonNull("text") ? optNode.get("text").asText("").trim() : "";
                if (!key.isBlank()) {
                    items.add(ReviewOptionItem.builder().key(key).text(text).build());
                }
            }
        } else if (optionsNode.isObject()) {
            optionsNode.properties().forEach(entry -> {
                String key = entry.getKey() != null ? entry.getKey().trim() : "";
                String text = entry.getValue() != null ? entry.getValue().asText("").trim() : "";
                if (!key.isBlank()) {
                    items.add(ReviewOptionItem.builder().key(key).text(text).build());
                }
            });
        }

        return items;
    }

    /**
     * Bóc tách chữ cái đáp án đúng (A/B/C/D) từ chuỗi JSON correct_answer (hỗ trợ {"key":"A"} hoặc {"selected":"A"}).
     */
    private String extractCorrectAnswer(String rawCorrectAnswer, Long questionId) {
        if (rawCorrectAnswer == null || rawCorrectAnswer.isBlank()) {
            return "";
        }
        JsonNode node = parseJsonSafely(rawCorrectAnswer, "correctAnswer", questionId);
        if (node == null) {
            return rawCorrectAnswer.trim();
        }
        if (node.isTextual()) {
            return node.asText("").trim();
        }
        String key = extractFirstText(node, "key", "selected", "answer");
        return key.trim();
    }

    /**
     * Bóc tách đối tượng giải thích chi tiết (transcript, translation, clue, analysis, vocabulary) từ JSON explanation.
     */
    private ReviewExplanationItem extractExplanation(String rawExplanation, Long questionId) {
        if (rawExplanation == null || rawExplanation.isBlank()) {
            return null;
        }
        JsonNode node = parseJsonSafely(rawExplanation, "explanation", questionId);
        if (node == null) {
            return ReviewExplanationItem.builder()
                    .analysis(rawExplanation.trim())
                    .vocabulary(Collections.emptyList())
                    .build();
        }

        List<ReviewVocabularyItem> vocabList = new ArrayList<>();
        if (node.has("vocabulary") && node.get("vocabulary").isArray()) {
            for (JsonNode vNode : node.get("vocabulary")) {
                String word = vNode.hasNonNull("word") ? vNode.get("word").asText("").trim() : "";
                String type = vNode.hasNonNull("type") ? vNode.get("type").asText("").trim() : "";
                String meaning = vNode.hasNonNull("meaning") ? vNode.get("meaning").asText("").trim() : "";
                if (!word.isBlank()) {
                    vocabList.add(ReviewVocabularyItem.builder()
                            .word(word)
                            .type(type)
                            .meaning(meaning)
                            .build());
                }
            }
        }

        String transcript = blankToNull(extractFirstText(node, "transcript"));
        String translation = blankToNull(extractFirstText(node, "translation"));
        String clue = blankToNull(extractFirstText(node, "clue"));
        String analysis = blankToNull(extractFirstText(node, "analysis", "text", "detail"));

        if (transcript == null && translation == null && clue == null && analysis == null && vocabList.isEmpty()) {
            return null;
        }

        return ReviewExplanationItem.builder()
                .transcript(transcript)
                .translation(translation)
                .clue(clue)
                .analysis(analysis)
                .vocabulary(vocabList)
                .build();
    }

    /**
     * Hợp nhất liên kết ảnh và âm thanh từ bảng question_medias (ưu tiên) và fallback từ trường JSON cũ.
     */
    private MediaPair resolveMediaUrls(List<QuestionMedia> mediaList, String jsonImageUrl, String jsonAudioUrl) {
        String resolvedImage = blankToNull(jsonImageUrl);
        String resolvedAudio = blankToNull(jsonAudioUrl);

        if (mediaList != null) {
            for (QuestionMedia qm : mediaList) {
                Media media = qm != null ? qm.getMedia() : null;
                if (media == null || media.getSecureUrl() == null || media.getSecureUrl().isBlank()) {
                    continue;
                }
                String format = media.getFormat() != null ? media.getFormat().toLowerCase(Locale.ROOT) : "";
                if (media.getResourceType() == EMediaResourceType.IMAGE && resolvedImage == null) {
                    resolvedImage = media.getSecureUrl();
                } else if (AUDIO_FORMATS.contains(format) && resolvedAudio == null) {
                    resolvedAudio = media.getSecureUrl();
                } else if (media.getResourceType() == EMediaResourceType.VIDEO && resolvedAudio == null) {
                    // Trên Cloudinary, tệp âm thanh MP3/WAV thường được gắn resource_type = VIDEO
                    resolvedAudio = media.getSecureUrl();
                }
            }
        }
        return new MediaPair(resolvedImage, resolvedAudio);
    }

    /**
     * Phân tích chuỗi JSON an toàn; trả null và ghi log cảnh báo nếu JSON lỗi định dạng để không sập API.
     */
    private JsonNode parseJsonSafely(String rawJson, String fieldName, Long questionId) {
        if (rawJson == null || rawJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(rawJson);
        } catch (Exception ex) {
            log.warn("Không thể phân tích JSON trường {} của questionId={}: {}", fieldName, questionId, ex.getMessage());
            return null;
        }
    }

    private String extractFirstText(JsonNode node, String... keys) {
        if (node == null || !node.isObject()) {
            return "";
        }
        for (String key : keys) {
            if (node.hasNonNull(key)) {
                String value = node.get(key).asText("");
                if (!value.isBlank()) {
                    return value.trim();
                }
            }
        }
        return "";
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private ESection resolveSection(Part part) {
        return (part != null && part.getSections() != null) ? part.getSections() : ESection.READING;
    }

    private int safeNonNegative(Integer value) {
        return value != null && value > ZERO_VALUE ? value : ZERO_VALUE;
    }

    private int clampPercent(Integer value) {
        if (value == null || value < ZERO_VALUE) {
            return ZERO_VALUE;
        }
        return Math.min(value, FULL_PERCENT);
    }

    private record MediaPair(String imageUrl, String audioUrl) {
    }
}
