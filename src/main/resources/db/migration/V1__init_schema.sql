-- =====================================================================================
-- LEBY ADAPTIVE TOEIC LEARNING PLATFORM - V1 INITIAL SCHEMA (TiDB / MySQL 8)
-- Chuẩn hóa sạch 28 bảng khớp 100% với toàn bộ JPA Entity trên nhánh dev
-- Kèm đầy đủ ràng buộc UNIQUE nghiệp vụ và độ dài cột an toàn cho nội dung Tiếng Việt
-- =====================================================================================

-- 1. Hồ sơ mục tiêu điểm TOEIC (TargetProfile)
CREATE TABLE target_profiles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    target_total_score INT NOT NULL,
    status BIT(1) NOT NULL DEFAULT b'1',
    description VARCHAR(500) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_target_profiles_score (target_total_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Các phần thi TOEIC Part 1 - Part 7 (Part)
CREATE TABLE parts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT NULL,
    sections VARCHAR(255) NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Năng lực vi mô TOEIC theo từng Part (Ability)
CREATE TABLE abilities (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    sections VARCHAR(255) NULL,
    part_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    KEY idx_abilities_part_id (part_id),
    CONSTRAINT fk_abilities_part FOREIGN KEY (part_id) REFERENCES parts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Bảng quy đổi điểm chuẩn ETS TOEIC (ToeicScoreConversion)
CREATE TABLE toeic_score_conversions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    correct_count INT NOT NULL,
    listening_score INT NOT NULL,
    reading_score INT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_toeic_score_conversions_correct_count (correct_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Người dùng hệ thống (User: ADMIN, TEACHER, STUDENT)
CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(150) NOT NULL,
    full_name VARCHAR(150) NULL,
    dob DATE NULL,
    avatar VARCHAR(500) NULL,
    is_active BIT(1) NOT NULL DEFAULT b'0',
    role VARCHAR(255) NOT NULL,
    learner_type VARCHAR(255) NULL,
    token_version INT NOT NULL DEFAULT 0,
    target_profile_id BIGINT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    KEY idx_users_role (role),
    KEY idx_users_target_profile_id (target_profile_id),
    CONSTRAINT fk_users_target_profile FOREIGN KEY (target_profile_id) REFERENCES target_profiles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Quản lý tệp tin đa phương tiện trên Cloudinary (Media)
CREATE TABLE medias (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    public_id VARCHAR(255) NOT NULL,
    secure_url TEXT NOT NULL,
    resource_type VARCHAR(255) NOT NULL,
    format VARCHAR(50) NOT NULL,
    bytes BIGINT NOT NULL,
    width INT NULL,
    height INT NULL,
    original_filename VARCHAR(255) NULL,
    duration DOUBLE NULL,
    status VARCHAR(255) NOT NULL,
    pending_expires_at DATETIME(6) NULL,
    uploaded_by BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_medias_public_id (public_id),
    KEY idx_medias_status (status),
    KEY idx_medias_uploaded_by (uploaded_by),
    CONSTRAINT fk_medias_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Ngưỡng điều hướng Tầng 1 theo AIM và Part (TargetPartThreshold)
CREATE TABLE target_part_thresholds (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    pass_score FLOAT NOT NULL,
    confirming_score FLOAT NULL,
    full_part_threshold FLOAT NULL,
    target_profile_id BIGINT NOT NULL,
    part_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_target_part_threshold (target_profile_id, part_id),
    KEY idx_target_part_thresholds_part (part_id),
    CONSTRAINT fk_target_part_thresholds_profile FOREIGN KEY (target_profile_id) REFERENCES target_profiles (id),
    CONSTRAINT fk_target_part_thresholds_part FOREIGN KEY (part_id) REFERENCES parts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Quy tắc đánh giá năng lực nhị phân Tầng 2 (AbilityEvaluationRule)
CREATE TABLE ability_evaluation_rules (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    stable_threshold FLOAT NOT NULL,
    developing_threshold FLOAT NOT NULL,
    min_evidence_count INT NOT NULL,
    ability_id BIGINT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ability_evaluation_rules_ability_id (ability_id),
    CONSTRAINT fk_ability_evaluation_rules_ability FOREIGN KEY (ability_id) REFERENCES abilities (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Mô-đun học tập chuẩn sư phạm (Module)
CREATE TABLE modules (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    title VARCHAR(200) NOT NULL,
    type VARCHAR(100) NOT NULL,
    sequence INT NOT NULL,
    status VARCHAR(255) NOT NULL,
    default_pass_score INT NOT NULL DEFAULT 65,
    required_completion_rate INT NOT NULL DEFAULT 80,
    descriptions TEXT NULL,
    part_id BIGINT NULL,
    PRIMARY KEY (id),
    KEY idx_modules_part_id (part_id),
    KEY idx_modules_sequence (sequence),
    KEY idx_modules_status (status),
    CONSTRAINT fk_modules_part FOREIGN KEY (part_id) REFERENCES parts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. Liên kết Nhiều-Nhiều giữa Module và Ability (ModuleAbility)
CREATE TABLE module_abilities (
    id BIGINT NOT NULL AUTO_INCREMENT,
    module_id BIGINT NOT NULL,
    ability_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_module_ability (module_id, ability_id),
    KEY idx_module_abilities_ability_id (ability_id),
    CONSTRAINT fk_module_abilities_module FOREIGN KEY (module_id) REFERENCES modules (id),
    CONSTRAINT fk_module_abilities_ability FOREIGN KEY (ability_id) REFERENCES abilities (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. Ngưỡng điểm đỗ vượt ải Module theo từng AIM (ModuleTargetThreshold)
CREATE TABLE module_target_thresholds (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    pass_score INT NOT NULL,
    module_id BIGINT NOT NULL,
    target_profile_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_module_target_threshold (module_id, target_profile_id),
    KEY idx_module_target_thresholds_profile_id (target_profile_id),
    CONSTRAINT fk_module_target_thresholds_module FOREIGN KEY (module_id) REFERENCES modules (id),
    CONSTRAINT fk_module_target_thresholds_profile FOREIGN KEY (target_profile_id) REFERENCES target_profiles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. Bài học trong Module: VIDEO hoặc PRACTICE (Lesson)
CREATE TABLE lessons (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    title VARCHAR(200) NOT NULL,
    order_no INT NOT NULL,
    status VARCHAR(255) NOT NULL,
    descriptions TEXT NULL,
    audio_url VARCHAR(500) NULL,
    video_url VARCHAR(500) NULL,
    image_url VARCHAR(500) NULL,
    is_free BOOLEAN NOT NULL DEFAULT TRUE,
    lesson_type VARCHAR(30) NOT NULL,
    duration_seconds INT NOT NULL DEFAULT 0,
    module_id BIGINT NOT NULL,
    ability_id BIGINT NULL,
    PRIMARY KEY (id),
    KEY idx_lessons_module_id (module_id),
    KEY idx_lessons_ability_id (ability_id),
    KEY idx_lessons_order_no (order_no),
    CONSTRAINT fk_lessons_module FOREIGN KEY (module_id) REFERENCES modules (id),
    CONSTRAINT fk_lessons_ability FOREIGN KEY (ability_id) REFERENCES abilities (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 13. Kịch bản lời thoại phụ đề theo timestamp của bài học (LessonScript)
CREATE TABLE lesson_scripts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_no INT NOT NULL,
    start_time VARCHAR(20) NOT NULL,
    end_time VARCHAR(20) NOT NULL,
    speaker VARCHAR(100) NULL,
    content_en TEXT NOT NULL,
    content_vi TEXT NULL,
    lesson_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    KEY idx_lesson_scripts_lesson_id (lesson_id),
    CONSTRAINT fk_lesson_scripts_lesson FOREIGN KEY (lesson_id) REFERENCES lessons (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 14. Ngân hàng câu hỏi TOEIC đơn lập & chùm ngữ cảnh cha-con (Question)
CREATE TABLE questions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    type VARCHAR(255) NOT NULL,
    name VARCHAR(200) NOT NULL,
    order_no INT NULL,
    question_data JSON NULL,
    correct_answer JSON NULL,
    difficulty INT NULL,
    sections VARCHAR(255) NULL,
    descriptions TEXT NULL,
    part_id BIGINT NULL,
    explanation JSON NULL,
    parent_id BIGINT NULL,
    PRIMARY KEY (id),
    KEY idx_questions_part_id (part_id),
    KEY idx_questions_parent_id (parent_id),
    KEY idx_questions_type (type),
    KEY idx_questions_difficulty (difficulty),
    CONSTRAINT fk_questions_part FOREIGN KEY (part_id) REFERENCES parts (id),
    CONSTRAINT fk_questions_parent FOREIGN KEY (parent_id) REFERENCES questions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 15. Liên kết tệp Media gắn cho Câu hỏi (QuestionMedia)
CREATE TABLE question_medias (
    id BIGINT NOT NULL AUTO_INCREMENT,
    question_id BIGINT NOT NULL,
    media_id BIGINT NOT NULL,
    order_no INT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_question_media (question_id, media_id),
    KEY idx_question_medias_media_id (media_id),
    CONSTRAINT fk_question_medias_question FOREIGN KEY (question_id) REFERENCES questions (id),
    CONSTRAINT fk_question_medias_media FOREIGN KEY (media_id) REFERENCES medias (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 16. Liên kết Câu hỏi đo lường Năng lực Ability (QuestionAbility)
CREATE TABLE question_abilities (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_no INT NULL,
    points INT NULL,
    question_id BIGINT NOT NULL,
    ability_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_question_ability (question_id, ability_id),
    KEY idx_question_abilities_ability_id (ability_id),
    CONSTRAINT fk_question_abilities_question FOREIGN KEY (question_id) REFERENCES questions (id),
    CONSTRAINT fk_question_abilities_ability FOREIGN KEY (ability_id) REFERENCES abilities (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 17. Liên kết Câu hỏi luyện tập trong bài học PRACTICE (PracticeQuestion - extends AbstractEntity)
CREATE TABLE practice_questions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    order_no INT NOT NULL,
    points INT NOT NULL,
    lesson_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_practice_lesson_question (lesson_id, question_id),
    KEY idx_practice_questions_lesson_id (lesson_id),
    KEY idx_practice_questions_question_id (question_id),
    CONSTRAINT fk_practice_questions_lesson FOREIGN KEY (lesson_id) REFERENCES lessons (id),
    CONSTRAINT fk_practice_questions_question FOREIGN KEY (question_id) REFERENCES questions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 18. Liên kết Câu hỏi cho bài thi vượt ải Module (ModuleTestQuestion)
CREATE TABLE module_test_questions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_no INT NOT NULL,
    points INT NOT NULL,
    module_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_module_test_question (module_id, question_id),
    KEY idx_module_test_questions_question_id (question_id),
    CONSTRAINT fk_module_test_questions_module FOREIGN KEY (module_id) REFERENCES modules (id),
    CONSTRAINT fk_module_test_questions_question FOREIGN KEY (question_id) REFERENCES questions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 19. Khuôn mẫu Đề thi chẩn đoán / Đề thi thử 200 câu (DiagnosticTest)
CREATE TABLE diagnostic_tests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NULL,
    exam_status VARCHAR(20) NOT NULL,
    test_type VARCHAR(30) NOT NULL,
    duration_minutes INT NOT NULL,
    updated_by VARCHAR(100) NULL,
    PRIMARY KEY (id),
    KEY idx_diagnostic_tests_status_type (exam_status, test_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 20. Liên kết Câu hỏi trong Đề thi chẩn đoán / Mock Test (DiagnosticTestQuestion)
CREATE TABLE diagnostic_test_questions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_no INT NOT NULL,
    points INT NOT NULL,
    diagnostic_test_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_diagnostic_test_question (diagnostic_test_id, question_id),
    KEY idx_diagnostic_test_questions_question_id (question_id),
    CONSTRAINT fk_diagnostic_test_questions_test FOREIGN KEY (diagnostic_test_id) REFERENCES diagnostic_tests (id),
    CONSTRAINT fk_diagnostic_test_questions_question FOREIGN KEY (question_id) REFERENCES questions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 21. Lượt làm bài thi chẩn đoán / thi thử của học viên (DiagnosticAttempt)
CREATE TABLE diagnostic_attempts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    started_at DATETIME(6) NULL,
    completed_at DATETIME(6) NULL,
    status VARCHAR(20) NOT NULL,
    duration_seconds INT NULL,
    user_id BIGINT NOT NULL,
    diagnostic_test_id BIGINT NOT NULL,
    target_profile_id BIGINT NULL,
    PRIMARY KEY (id),
    KEY idx_diagnostic_attempts_user_id (user_id),
    KEY idx_diagnostic_attempts_test_id (diagnostic_test_id),
    KEY idx_diagnostic_attempts_profile_id (target_profile_id),
    CONSTRAINT fk_diagnostic_attempts_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_diagnostic_attempts_test FOREIGN KEY (diagnostic_test_id) REFERENCES diagnostic_tests (id),
    CONSTRAINT fk_diagnostic_attempts_profile FOREIGN KEY (target_profile_id) REFERENCES target_profiles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 22. Chi tiết câu trả lời của học viên trong lượt thi chẩn đoán (DiagnosticAnswer)
CREATE TABLE diagnostic_answers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    user_answer JSON NULL,
    is_correct BIT(1) NOT NULL,
    is_flagged BIT(1) NOT NULL DEFAULT b'0',
    diagnostic_attempt_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_diagnostic_answer (diagnostic_attempt_id, question_id),
    KEY idx_diagnostic_answers_question_id (question_id),
    CONSTRAINT fk_diagnostic_answers_attempt FOREIGN KEY (diagnostic_attempt_id) REFERENCES diagnostic_attempts (id),
    CONSTRAINT fk_diagnostic_answers_question FOREIGN KEY (question_id) REFERENCES questions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 23. Kết quả chẩn đoán 7 Part TOEIC (PartDiagnosticResult)
CREATE TABLE part_diagnostic_results (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    score FLOAT NOT NULL,
    directive VARCHAR(255) NOT NULL,
    diagnostic_attempt_id BIGINT NOT NULL,
    part_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_part_diagnostic_result (diagnostic_attempt_id, part_id),
    KEY idx_part_diagnostic_results_part_id (part_id),
    CONSTRAINT fk_part_diagnostic_results_attempt FOREIGN KEY (diagnostic_attempt_id) REFERENCES diagnostic_attempts (id),
    CONSTRAINT fk_part_diagnostic_results_part FOREIGN KEY (part_id) REFERENCES parts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 24. Trạng thái năng lực hiện tại của học viên (UserAbility)
CREATE TABLE user_abilities (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    status VARCHAR(255) NOT NULL,
    evidence_count INT NOT NULL DEFAULT 0,
    accuracy_rate FLOAT NULL,
    user_id BIGINT NOT NULL,
    ability_id BIGINT NOT NULL,
    part_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_ability_part (user_id, ability_id, part_id),
    KEY idx_user_abilities_ability_id (ability_id),
    KEY idx_user_abilities_part_id (part_id),
    CONSTRAINT fk_user_abilities_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_abilities_ability FOREIGN KEY (ability_id) REFERENCES abilities (id),
    CONSTRAINT fk_user_abilities_part FOREIGN KEY (part_id) REFERENCES parts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 25. Lộ trình học cá nhân hóa (LearningPath)
-- Lưu ý BR02: diagnostic_attempt_id dùng INDEX thường (ManyToOne), KHÔNG đặt UNIQUE để tái sử dụng khi đổi AIM
CREATE TABLE learning_paths (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    version INT NULL,
    status VARCHAR(20) NOT NULL,
    user_id BIGINT NOT NULL,
    target_profile_id BIGINT NOT NULL,
    diagnostic_attempt_id BIGINT NULL,
    PRIMARY KEY (id),
    KEY idx_learning_paths_user_status (user_id, status),
    KEY idx_learning_paths_target_profile_id (target_profile_id),
    KEY idx_learning_paths_diagnostic_attempt_id (diagnostic_attempt_id),
    CONSTRAINT fk_learning_paths_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_learning_paths_target_profile FOREIGN KEY (target_profile_id) REFERENCES target_profiles (id),
    CONSTRAINT fk_learning_paths_diagnostic_attempt FOREIGN KEY (diagnostic_attempt_id) REFERENCES diagnostic_attempts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 26. Các Module nằm trong lộ trình học của học viên (LearningPathItem)
CREATE TABLE learning_path_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    order_no INT NOT NULL,
    is_correct BIT(1) NULL,
    progress_percent INT NOT NULL DEFAULT 0,
    reason VARCHAR(500) NULL,
    status VARCHAR(255) NOT NULL,
    learning_path_id BIGINT NOT NULL,
    module_id BIGINT NOT NULL,
    ability_id BIGINT NULL,
    PRIMARY KEY (id),
    KEY idx_learning_path_items_path_id (learning_path_id),
    KEY idx_learning_path_items_module_id (module_id),
    KEY idx_learning_path_items_ability_id (ability_id),
    CONSTRAINT fk_learning_path_items_path FOREIGN KEY (learning_path_id) REFERENCES learning_paths (id),
    CONSTRAINT fk_learning_path_items_module FOREIGN KEY (module_id) REFERENCES modules (id),
    CONSTRAINT fk_learning_path_items_ability FOREIGN KEY (ability_id) REFERENCES abilities (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 27. Lượt làm bài thi vượt ải Module (ModuleTestAttempt)
CREATE TABLE module_test_attempts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    score INT NULL,
    result VARCHAR(255) NULL,
    is_submitted BIT(1) NOT NULL DEFAULT b'0',
    submitted_at DATETIME(6) NULL,
    module_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    KEY idx_module_test_attempts_module_id (module_id),
    KEY idx_module_test_attempts_user_id (user_id),
    CONSTRAINT fk_module_test_attempts_module FOREIGN KEY (module_id) REFERENCES modules (id),
    CONSTRAINT fk_module_test_attempts_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 28. Chi tiết câu trả lời bài thi vượt ải Module (ModuleTestAnswer)
CREATE TABLE module_test_answers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    is_correct BIT(1) NOT NULL,
    is_flagged BIT(1) NOT NULL DEFAULT b'0',
    user_answer JSON NULL,
    module_test_attempt_id BIGINT NOT NULL,
    module_test_question_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_module_test_answer (module_test_attempt_id, module_test_question_id),
    KEY idx_module_test_answers_question_id (module_test_question_id),
    CONSTRAINT fk_module_test_answers_attempt FOREIGN KEY (module_test_attempt_id) REFERENCES module_test_attempts (id),
    CONSTRAINT fk_module_test_answers_question FOREIGN KEY (module_test_question_id) REFERENCES module_test_questions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
