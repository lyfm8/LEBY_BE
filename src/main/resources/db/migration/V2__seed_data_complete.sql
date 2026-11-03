-- =====================================================================================
-- LEBY ADAPTIVE TOEIC LEARNING PLATFORM - COMPLETE SEED DATA SCRIPT
-- Chuẩn hóa toàn diện 27 bảng dữ liệu thực tế: Giáo trình, Đề thi, Media Cloudinary, Lộ trình
-- Thang điểm: TOEIC ETS 10 - 990 | Thuật toán thích ứng 2 tầng (Tier 1 & Tier 2)
-- =====================================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- -------------------------------------------------------------------------------------
-- 1. XÓA SẠCH DỮ LIỆU CŨ TRƯỚC KHI NẠP SEED DATA MỚI
-- -------------------------------------------------------------------------------------
TRUNCATE TABLE module_test_answers;
TRUNCATE TABLE module_test_attempts;
TRUNCATE TABLE learning_path_items;
TRUNCATE TABLE learning_paths;
TRUNCATE TABLE user_abilities;
TRUNCATE TABLE part_diagnostic_results;
TRUNCATE TABLE diagnostic_answers;
TRUNCATE TABLE diagnostic_attempts;
TRUNCATE TABLE diagnostic_test_questions;
TRUNCATE TABLE diagnostic_tests;
TRUNCATE TABLE module_test_questions;
TRUNCATE TABLE practice_questions;
TRUNCATE TABLE question_abilities;
TRUNCATE TABLE questions;
TRUNCATE TABLE video_lessons;
TRUNCATE TABLE practice_lessons;
TRUNCATE TABLE lessons;
TRUNCATE TABLE module_target_thresholds;
TRUNCATE TABLE module_abilities;
TRUNCATE TABLE modules;
TRUNCATE TABLE ability_evaluation_rules;
TRUNCATE TABLE target_part_thresholds;
TRUNCATE TABLE abilities;
TRUNCATE TABLE parts;
TRUNCATE TABLE toeic_score_conversions;
TRUNCATE TABLE users;
TRUNCATE TABLE target_profiles;

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================================
-- PHẦN 1: HỒ SƠ MỤC TIÊU (TARGET PROFILES - AIM TOEIC)
-- =====================================================================================
INSERT INTO target_profiles (id, target_total_score, status, description, created_at, updated_at) VALUES
(1, 450, 1, 'Mục tiêu Sơ cấp 450 TOEIC - Nắm vững từ vựng cơ bản và cấu trúc ngữ pháp thông dụng.', NOW(), NOW()),
(2, 650, 1, 'Mục tiêu Trung cấp 650 TOEIC - Chuẩn đầu ra tốt nghiệp đại học và giao tiếp công sở tự tin.', NOW(), NOW()),
(3, 800, 1, 'Mục tiêu Nâng cao 800 TOEIC - Làm việc chuyên nghiệp trong tập đoàn đa quốc gia.', NOW(), NOW()),
(4, 990, 1, 'Mục tiêu Xuất sắc 990 TOEIC - Đỉnh cao năng lực ngôn ngữ, thành thạo như người bản xứ.', NOW(), NOW());

-- =====================================================================================
-- PHẦN 2: CÁC PART TRONG ĐỀ THI TOEIC (PARTS 1 - 7)
-- =====================================================================================
INSERT INTO parts (id, name, description, sections, created_at, updated_at) VALUES
(1, 'Part 1: Photographs', 'Mô tả hình ảnh: Nghe 4 câu mô tả về một bức tranh và chọn câu mô tả chính xác nhất.', 'LISTENING', NOW(), NOW()),
(2, 'Part 2: Question - Response', 'Hỏi - Đáp: Nghe một câu hỏi hoặc phát biểu và chọn 1 trong 3 câu phản hồi phù hợp.', 'LISTENING', NOW(), NOW()),
(3, 'Part 3: Short Conversations', 'Đoạn hội thoại ngắn: Nghe đoạn trao đổi giữa 2-3 người và trả lời 3 câu hỏi liên quan.', 'LISTENING', NOW(), NOW()),
(4, 'Part 4: Short Talks', 'Bài nói ngắn: Nghe một bài độc thoại (thông báo, tin nhắn thoại, radio) và trả lời 3 câu hỏi.', 'LISTENING', NOW(), NOW()),
(5, 'Part 5: Incomplete Sentences', 'Điền vào câu: Chọn 1 trong 4 từ/cụm từ thích hợp nhất để hoàn thành câu văn.', 'READING', NOW(), NOW()),
(6, 'Part 6: Text Completion', 'Điền vào đoạn văn: Điền từ, cụm từ hoặc cả câu văn vào chỗ trống trong văn bản ngắn.', 'READING', NOW(), NOW()),
(7, 'Part 7: Reading Comprehension', 'Đọc hiểu văn bản: Đọc các đoạn đơn, đôi, ba (email, thông báo, hóa đơn) và trả lời câu hỏi.', 'READING', NOW(), NOW());

-- =====================================================================================
-- PHẦN 3: NĂNG LỰC VI MÔ TOEIC (ABILITIES THEO TỪNG PART)
-- =====================================================================================
INSERT INTO abilities (id, name, description, sections, part_id, created_at, updated_at) VALUES
(1, 'Human Actions Identification', 'Kỹ năng nhận diện chính xác hành động của con người qua động từ và thể hiện tại tiếp diễn.', 'LISTENING', 1, NOW(), NOW()),
(2, 'Object & Scene Recognition', 'Kỹ năng phân tích vị trí đồ vật, trạng thái tĩnh và bối cảnh không gian xung quanh.', 'LISTENING', 1, NOW(), NOW()),
(3, 'Wh-Questions Reflex', 'Phản xạ nhận biết và trả lời đúng trọng tâm câu hỏi thông tin (Who, Where, When, What, Why, How).', 'LISTENING', 2, NOW(), NOW()),
(4, 'Yes/No & Choice Questions', 'Kỹ năng xử lý câu hỏi xác nhận Yes/No, câu hỏi đuôi và câu hỏi lựa chọn dùng liên từ OR.', 'LISTENING', 2, NOW(), NOW()),
(5, 'Indirect & Evasive Responses', 'Kỹ năng phát hiện câu trả lời gián tiếp, né tránh hoặc cung cấp thêm thông tin bối cảnh.', 'LISTENING', 2, NOW(), NOW()),
(6, 'Specific Details & Keywords Tracking', 'Kỹ năng nghe quét bắt từ khóa (thời gian, địa điểm, chi phí) và theo dấu thông tin cụ thể.', 'LISTENING', 3, NOW(), NOW()),
(7, 'Purpose & Speaker Implication', 'Kỹ năng suy luận mục đích cuộc trò chuyện, nghề nghiệp và ngụ ý ẩn ý của người nói.', 'LISTENING', 3, NOW(), NOW()),
(8, 'Public Announcements Comprehension', 'Kỹ năng nghe hiểu các thông báo nơi công cộng (sân bay, nhà ga, trung tâm mua sắm).', 'LISTENING', 4, NOW(), NOW()),
(9, 'Voicemails & News Reports', 'Kỹ năng nắm bắt nội dung tin nhắn hộp thư thoại kinh doanh và bản tin giao thông/thời tiết.', 'LISTENING', 4, NOW(), NOW()),
(10, 'Parts of Speech & Word Formation', 'Kỹ năng phân tích cấu trúc ngữ pháp để xác định đúng vị trí danh từ, động từ, tính từ, trạng từ.', 'READING', 5, NOW(), NOW()),
(11, 'Verb Tenses & Voice Consistency', 'Kỹ năng chia 12 thì tiếng Anh, hòa hợp chủ ngữ - động từ và phân biệt thể chủ động / bị động.', 'READING', 5, NOW(), NOW()),
(12, 'Business Lexicon & Collocations', 'Năng lực vận dụng chuẩn xác từ vựng chuyên ngành công sở, hợp đồng và cụm từ cố định.', 'READING', 5, NOW(), NOW()),
(13, 'Contextual Sentence Insertion', 'Chiến thuật chọn câu văn hoàn chỉnh điền vào chỗ trống sao cho mạch lạc với ý toàn đoạn.', 'READING', 6, NOW(), NOW()),
(14, 'Transitions & Discourse Markers', 'Kỹ năng nhận diện liên từ nối (However, Therefore, Although) biểu thị quan hệ logic trong văn bản.', 'READING', 6, NOW(), NOW()),
(15, 'Business Emails & Memos Reading', 'Kỹ năng đọc lướt và quét thông tin nhanh trong thư từ thương mại, biên bản họp và thông báo nội bộ.', 'READING', 7, NOW(), NOW()),
(16, 'Cross-referencing Multi-passages', 'Kỹ năng xâu chuỗi thông tin chéo giữa văn bản đôi và văn bản ba (Email - Hóa đơn - Lịch trình).', 'READING', 7, NOW(), NOW());

-- =====================================================================================
-- PHẦN 4: THAM SỐ NGƯỠNG ĐIỀU HƯỚNG TẦNG 1 (TARGET PART THRESHOLDS)
-- =====================================================================================
-- Profile 1: AIM 450 (Ngưỡng dễ)
INSERT INTO target_part_thresholds (id, pass_score, confirming_score, full_part_threshold, target_profile_id, part_id, created_at, updated_at) VALUES
(1, 55.0, 40.0, 25.0, 1, 1, NOW(), NOW()),
(2, 50.0, 35.0, 20.0, 1, 2, NOW(), NOW()),
(3, 45.0, 30.0, 20.0, 1, 3, NOW(), NOW()),
(4, 45.0, 30.0, 20.0, 1, 4, NOW(), NOW()),
(5, 50.0, 35.0, 25.0, 1, 5, NOW(), NOW()),
(6, 45.0, 30.0, 20.0, 1, 6, NOW(), NOW()),
(7, 40.0, 25.0, 15.0, 1, 7, NOW(), NOW()),

-- Profile 2: AIM 650 (Ngưỡng trung cấp chuẩn tốt nghiệp)
(8, 75.0, 60.0, 35.0, 2, 1, NOW(), NOW()),
(9, 70.0, 55.0, 30.0, 2, 2, NOW(), NOW()),
(10, 65.0, 50.0, 30.0, 2, 3, NOW(), NOW()),
(11, 65.0, 50.0, 30.0, 2, 4, NOW(), NOW()),
(12, 70.0, 55.0, 35.0, 2, 5, NOW(), NOW()),
(13, 65.0, 50.0, 30.0, 2, 6, NOW(), NOW()),
(14, 60.0, 45.0, 25.0, 2, 7, NOW(), NOW()),

-- Profile 3: AIM 800 (Ngưỡng nâng cao)
(15, 88.0, 75.0, 50.0, 3, 1, NOW(), NOW()),
(16, 85.0, 70.0, 45.0, 3, 2, NOW(), NOW()),
(17, 80.0, 65.0, 40.0, 3, 3, NOW(), NOW()),
(18, 80.0, 65.0, 40.0, 3, 4, NOW(), NOW()),
(19, 85.0, 70.0, 45.0, 3, 5, NOW(), NOW()),
(20, 80.0, 65.0, 40.0, 3, 6, NOW(), NOW()),
(21, 75.0, 60.0, 35.0, 3, 7, NOW(), NOW()),

-- Profile 4: AIM 990 (Ngưỡng hoàn hảo)
(22, 98.0, 90.0, 70.0, 4, 1, NOW(), NOW()),
(23, 95.0, 88.0, 65.0, 4, 2, NOW(), NOW()),
(24, 92.0, 85.0, 60.0, 4, 3, NOW(), NOW()),
(25, 92.0, 85.0, 60.0, 4, 4, NOW(), NOW()),
(26, 95.0, 88.0, 65.0, 4, 5, NOW(), NOW()),
(27, 92.0, 85.0, 60.0, 4, 6, NOW(), NOW()),
(28, 90.0, 82.0, 55.0, 4, 7, NOW(), NOW());

-- =====================================================================================
-- PHẦN 5: QUY TẮC ĐÁNH GIÁ NĂNG LỰC TẦNG 2 (ABILITY EVALUATION RULES)
-- =====================================================================================
INSERT INTO ability_evaluation_rules (id, stable_threshold, developing_threshold, min_evidence_count, ability_id, created_at, updated_at) VALUES
(1, 75.0, 50.0, 3, 1, NOW(), NOW()),
(2, 75.0, 50.0, 3, 2, NOW(), NOW()),
(3, 75.0, 50.0, 3, 3, NOW(), NOW()),
(4, 75.0, 50.0, 3, 4, NOW(), NOW()),
(5, 70.0, 45.0, 3, 5, NOW(), NOW()),
(6, 75.0, 50.0, 3, 6, NOW(), NOW()),
(7, 70.0, 45.0, 3, 7, NOW(), NOW()),
(8, 75.0, 50.0, 3, 8, NOW(), NOW()),
(9, 75.0, 50.0, 3, 9, NOW(), NOW()),
(10, 80.0, 55.0, 3, 10, NOW(), NOW()),
(11, 80.0, 55.0, 3, 11, NOW(), NOW()),
(12, 75.0, 50.0, 3, 12, NOW(), NOW()),
(13, 70.0, 45.0, 3, 13, NOW(), NOW()),
(14, 75.0, 50.0, 3, 14, NOW(), NOW()),
(15, 75.0, 50.0, 3, 15, NOW(), NOW()),
(16, 70.0, 45.0, 3, 16, NOW(), NOW());

-- =====================================================================================
-- PHẦN 6: NGƯỜI DÙNG HỆ THỐNG (USERS)
-- Mật khẩu mã hóa BCrypt của chuỗi: Demo@1234
-- =====================================================================================
INSERT INTO users (id, email, password, full_name, dob, avatar, is_active, role, learner_type, token_version, target_profile_id, created_at, updated_at) VALUES
(1, 'admin@leby.edu.vn', '$2a$10$EZDf.oMCc0u7iTmj9BTt..LTd75RQLEq1OJUjnW9TicZYYMdyfjg6', 'Võ Hoàng Quân (Admin)', '1990-05-15', 'https://res.cloudinary.com/leby-toeic/image/upload/v1714500100/avatars/admin_avatar.png', 1, 'ADMIN', 'WORKING', 0, 4, NOW(), NOW()),
(2, 'teacher@leby.edu.vn', '$2a$10$EZDf.oMCc0u7iTmj9BTt..LTd75RQLEq1OJUjnW9TicZYYMdyfjg6', 'ThS. Nguyễn Hoàng Lan (Giảng viên)', '1993-11-20', 'https://res.cloudinary.com/leby-toeic/image/upload/v1714500101/avatars/teacher_avatar.png', 1, 'TEACHER', 'WORKING', 0, 4, NOW(), NOW()),
(3, 'demo@leby.edu.vn', '$2a$10$EZDf.oMCc0u7iTmj9BTt..LTd75RQLEq1OJUjnW9TicZYYMdyfjg6', 'Nguyễn Hải Nam (Học viên Demo)', '2003-08-10', 'https://res.cloudinary.com/leby-toeic/image/upload/v1714500102/avatars/student_nam.png', 1, 'STUDENT', 'UNIVERSITY_STUDENT', 0, 2, NOW(), NOW()),
(4, 'lan.tran@leby.edu.vn', '$2a$10$EZDf.oMCc0u7iTmj9BTt..LTd75RQLEq1OJUjnW9TicZYYMdyfjg6', 'Trần Thảo Lan (Học viên Đi làm)', '1999-02-28', 'https://res.cloudinary.com/leby-toeic/image/upload/v1714500103/avatars/student_lan.png', 1, 'STUDENT', 'WORKING', 0, 3, NOW(), NOW());

-- =====================================================================================
-- PHẦN 7: KHÓA HỌC & MODULE CHUẨN SƯ PHẠM (MODULES)
-- =====================================================================================
INSERT INTO modules (id, title, type, sequence, status, default_pass_score, part_id, created_at, updated_at) VALUES
(1, 'Chinh phục bẫy hành động và bối cảnh Part 1', 'THEORY', 1, 'PUBLISHED', 70, 1, NOW(), NOW()),
(2, 'Phản xạ câu hỏi Wh-Questions thần tốc Part 2', 'PRACTICE', 2, 'PUBLISHED', 70, 2, NOW(), NOW()),
(3, 'Phương pháp bắt Keyword & Ngữ điệu Part 3', 'THEORY', 3, 'PUBLISHED', 75, 3, NOW(), NOW()),
(4, 'Bí kíp đọc lướt câu hỏi & biểu đồ Part 4', 'PRACTICE', 4, 'PUBLISHED', 75, 4, NOW(), NOW()),
(5, 'Làm chủ 12 thì và thể bị động Part 5', 'THEORY', 5, 'PUBLISHED', 80, 5, NOW(), NOW()),
(6, 'Chiến thuật từ loại & Collocations công sở Part 5', 'PRACTICE', 6, 'PUBLISHED', 80, 5, NOW(), NOW()),
(7, 'Kỹ năng tư duy điền câu văn bản Part 6', 'PRACTICE', 7, 'PUBLISHED', 75, 6, NOW(), NOW()),
(8, 'Giải mã Email & Memos điều hành Part 7', 'PRACTICE', 8, 'PUBLISHED', 80, 7, NOW(), NOW()),
(9, 'Vượt bẫy văn bản đôi và ba Part 7', 'PRACTICE', 9, 'PUBLISHED', 80, 7, NOW(), NOW()),
(10, 'Tổng ôn thi vượt ải TOEIC toàn diện', 'EXAM', 10, 'PUBLISHED', 85, 5, NOW(), NOW());

-- Liên kết Module trains via Ability (M-M)
INSERT INTO module_abilities (id, module_id, ability_id) VALUES
(1, 1, 1), (2, 1, 2),
(3, 2, 3), (4, 2, 4),
(5, 3, 6), (6, 3, 7),
(7, 4, 8), (8, 4, 9),
(9, 5, 11),
(10, 6, 10), (11, 6, 12),
(12, 7, 13), (13, 7, 14),
(14, 8, 15),
(15, 9, 16),
(16, 10, 10), (17, 10, 11), (18, 10, 12);

-- Điểm đỗ cá nhân hóa từng Module theo AIM (ModuleTargetThresholds)
INSERT INTO module_target_thresholds (id, pass_score, module_id, target_profile_id, created_at, updated_at) VALUES
(1, 60, 1, 1, NOW(), NOW()), (2, 75, 1, 2, NOW(), NOW()), (3, 85, 1, 3, NOW(), NOW()),
(4, 60, 2, 1, NOW(), NOW()), (5, 70, 2, 2, NOW(), NOW()), (6, 85, 2, 3, NOW(), NOW()),
(7, 60, 3, 1, NOW(), NOW()), (8, 70, 3, 2, NOW(), NOW()), (9, 80, 3, 3, NOW(), NOW()),
(8, 65, 5, 1, NOW(), NOW()), (9, 75, 5, 2, NOW(), NOW()), (10, 85, 5, 3, NOW(), NOW()),
(11, 65, 6, 1, NOW(), NOW()), (12, 75, 6, 2, NOW(), NOW()), (13, 85, 6, 3, NOW(), NOW()),
(14, 60, 7, 1, NOW(), NOW()), (15, 75, 7, 2, NOW(), NOW()), (16, 85, 7, 3, NOW(), NOW()),
(17, 65, 8, 1, NOW(), NOW()), (18, 75, 8, 2, NOW(), NOW()), (19, 85, 8, 3, NOW(), NOW());

-- =====================================================================================
-- PHẦN 8: BÀI HỌC (LESSONS, VIDEO_LESSONS, PRACTICE_LESSONS)
-- Sử dụng JPA JOINED Table Inheritance Strategy
-- =====================================================================================
-- Bảng cha: lessons
INSERT INTO lessons (id, title, order_no, descriptions, is_free, module_id, ability_id, created_at, updated_at) VALUES
(1, 'Video 1: Nhận diện bẫy thì hiện tại tiếp diễn với chủ ngữ đồ vật', 1, 'Học cách tránh các bẫy mô tả người thực hiện hành động khi trong hình chỉ có đồ vật tĩnh.', 1, 1, 1, NOW(), NOW()),
(2, 'Thực hành: Luyện phản xạ 10 câu hình ảnh người và vật', 2, 'Bài tập tương tác phân biệt âm tương tự và bẫy thì thường gặp trong Part 1.', 1, 1, 2, NOW(), NOW()),
(3, 'Video 2: Chiến lược xử lý câu hỏi Wh-Questions trọng tâm', 1, 'Phân loại 6 dạng Wh-questions và cách bắt trọn 3 từ đầu tiên của câu hỏi.', 1, 2, 3, NOW(), NOW()),
(4, 'Thực hành: Nhận diện phản xạ hỏi đáp thời gian & địa điểm', 2, 'Bài luyện tập phản hồi tức thì với 15 câu Part 2 đa dạng ngữ điệu.', 1, 2, 3, NOW(), NOW()),
(5, 'Video 3: Kỹ năng đọc trước câu hỏi và dự đoán ngữ cảnh Part 3', 1, 'Tận dụng 8 giây giữa các đoạn hội thoại để quét từ khóa và khoanh vùng thông tin.', 1, 3, 6, NOW(), NOW()),
(6, 'Video 4: Tổng quan 12 thì tiếng Anh và các thì 90% xuất hiện trong TOEIC', 1, 'Bản đồ tư duy 12 thì, dấu hiệu nhận biết trạng từ thời gian (already, recently, since, by the time).', 1, 5, 11, NOW(), NOW()),
(7, 'Thực hành: Bài tập chia động từ và phân biệt chủ động - bị động', 2, 'Luyện kỹ năng nhận diện ngoại động từ có tân ngữ và nội động từ không có bị động.', 1, 5, 11, NOW(), NOW()),
(8, 'Video 5: Độc chiêu nhìn đuôi đoán từ loại trong 5 giây', 1, 'Mẹo phân biệt đuôi danh từ (-tion, -ment, -ance), tính từ (-ive, -able, -al) và trạng từ (-ly).', 1, 6, 10, NOW(), NOW()),
(9, 'Thực hành: 20 câu Collocations công sở hay gặp nhất', 2, 'Luyện tập các cụm từ ăn điểm tuyệt đối: conduct a survey, adhere to policies, highly qualified.', 1, 6, 12, NOW(), NOW()),
(10, 'Video 6: Phương pháp skimming và scanning thư từ thương mại Part 7', 1, 'Cách nắm bắt mục tiêu thư từ trường Subject, người gửi, người nhận và đoạn mở đầu.', 1, 8, 15, NOW(), NOW());

-- Bảng con: video_lessons (FK -> lessons.id)
INSERT INTO video_lessons (id, uri, duration) VALUES
(1, 'https://res.cloudinary.com/leby-toeic/video/upload/v1714500001/lessons/part1-action-traps.mp4', 720),
(3, 'https://res.cloudinary.com/leby-toeic/video/upload/v1714500002/lessons/part2-wh-reflex.mp4', 850),
(5, 'https://res.cloudinary.com/leby-toeic/video/upload/v1714500003/lessons/part3-keyword-tracking.mp4', 960),
(6, 'https://res.cloudinary.com/leby-toeic/video/upload/v1714500005/lessons/part5-tenses-passive.mp4', 1100),
(8, 'https://res.cloudinary.com/leby-toeic/video/upload/v1714500006/lessons/part5-word-formation.mp4', 780),
(10, 'https://res.cloudinary.com/leby-toeic/video/upload/v1714500008/lessons/part7-business-emails.mp4', 1020);

-- Bảng con: practice_lessons (FK -> lessons.id)
INSERT INTO practice_lessons (id, instructions) VALUES
(2, 'Lắng nghe từng audio một lần duy nhất. Quan sát bức tranh thật kỹ và click chọn đáp án chính xác nhất trước khi hết 5 giây suy nghĩ.'),
(4, 'Tập trung cao độ vào 3 từ đầu tiên của câu hỏi. Loại bỏ ngay các đáp án có từ lặp lại hoặc từ đồng âm dễ gây nhầm lẫn.'),
(7, 'Đọc toàn bộ câu để xác định chủ ngữ chính và trạng từ chỉ thời gian. Chú ý xét quan hệ chủ động hay bị động trước khi chọn thì.'),
(9, 'Quan sát từ đứng ngay trước và ngay sau chỗ trống để xác định loại từ hoặc cụm từ collocations tương thích.');

-- Chèn danh sách 21 câu hỏi mẫu chuẩn thực tế:
INSERT INTO questions (id, name, type, difficulty, sections, descriptions, part_id, parent_id, question_data, correct_answer, explanation, created_at, updated_at) VALUES
(1, 'Part 1: Construction workers reviewing architectural plans', 'SINGLE_CHOICE', 2, 'LISTENING', 'Câu hỏi hình ảnh Part 1: Nhóm kỹ sư công trình tại công trường.', 1, NULL, '{"text":"Look at the photograph and choose the statement that best describes what you see.","imageUrl":"https://res.cloudinary.com/leby-toeic/image/upload/v1714500020/questions/part1_construction_site.jpg","audioUrl":"https://res.cloudinary.com/leby-toeic/video/upload/v1714500021/audio/part1_q01_workers.mp3","options":[{"key":"A","text":"(A) Some workers are inspecting a building blueprint."},{"key":"B","text":"(B) The men are climbing up a ladder."},{"key":"C","text":"(C) Tools are being packed into cardboard boxes."},{"key":"D","text":"(D) Concrete is being poured onto the roadway."}]}', '{"selected":"A","key":"A"}', '{"transcript":"(A) Some workers are inspecting a building blueprint.\n(B) The men are climbing up a ladder.\n(C) Tools are being packed into cardboard boxes.\n(D) Concrete is being poured onto the roadway.","translation":"(A) Một số công nhân đang kiểm tra bản thiết kế công trình.\n(B) Những người đàn ông đang leo lên thang.\n(C) Dụng cụ đang được đóng vào các hộp các-tông.\n(D) Bê tông đang được đổ xuống mặt đường.","clue":"Trong ảnh, hai người đàn ông đội mũ bảo hộ đang cùng trải một bản vẽ kỹ thuật lớn trên bàn.","analysis":"Đáp án (A) dùng động từ \"inspecting a building blueprint\" phản ánh chuẩn xác hành động. Các đáp án B, C, D miêu tả hành động không có thật trong hình.","vocabulary":[{"word":"inspect","type":"v","meaning":"kiểm tra, giám định"},{"word":"blueprint","type":"n","meaning":"bản vẽ thiết kế kiến trúc"}]}', NOW(), NOW());
INSERT INTO questions (id, name, type, difficulty, sections, descriptions, part_id, parent_id, question_data, correct_answer, explanation, created_at, updated_at) VALUES
(2, 'Part 1: Office meeting room with presentation screen', 'SINGLE_CHOICE', 3, 'LISTENING', 'Câu hỏi hình ảnh đồ vật: Phòng họp không có người.', 1, NULL, '{"text":"Look at the photograph and choose the statement that best describes what you see.","imageUrl":"https://res.cloudinary.com/leby-toeic/image/upload/v1714500022/questions/part1_conference_room.jpg","audioUrl":"https://res.cloudinary.com/leby-toeic/video/upload/v1714500023/audio/part1_q02_room.mp3","options":[{"key":"A","text":"(A) A presentation is being delivered by a manager."},{"key":"B","text":"(B) Chairs are arranged around a conference table."},{"key":"C","text":"(C) Documents are being passed around the room."},{"key":"D","text":"(D) The windows are currently being cleaned."}]}', '{"selected":"B","key":"B"}', '{"transcript":"(A) A presentation is being delivered by a manager.\n(B) Chairs are arranged around a conference table.\n(C) Documents are being passed around the room.\n(D) The windows are currently being cleaned.","translation":"(A) Một bài thuyết trình đang được một quản lý trình bày.\n(B) Những chiếc ghế được sắp xếp xung quanh bàn hội nghị.\n(C) Tài liệu đang được phát quanh phòng.\n(D) Cửa sổ hiện đang được lau chùi.","clue":"Bức tranh chụp phòng họp tĩnh không có người, ghế xếp ngay ngắn quanh bàn dài.","analysis":"Bẫy cực phổ biến: Tranh không người không chọn các câu có thì Hiện tại tiếp diễn bị động (is/are being + V3) như A, C, D vì không có chủ thể tác động. Chọn (B) chỉ trạng thái.","vocabulary":[{"word":"be arranged","type":"v","meaning":"được sắp xếp ngăn nắp"},{"word":"conference table","type":"n","meaning":"bàn họp hội nghị"}]}', NOW(), NOW());
INSERT INTO questions (id, name, type, difficulty, sections, descriptions, part_id, parent_id, question_data, correct_answer, explanation, created_at, updated_at) VALUES
(3, 'Part 2: Wh-Question - Where is the annual shareholder meeting?', 'SINGLE_CHOICE', 2, 'LISTENING', 'Câu hỏi Wh-question hỏi địa điểm với Where.', 2, NULL, '{"text":"Where will the annual shareholder meeting be held this year?","audioUrl":"https://res.cloudinary.com/leby-toeic/video/upload/v1714500030/audio/part2_q03_where.mp3","options":[{"key":"A","text":"(A) At the Grand Plaza Hotel in Chicago."},{"key":"B","text":"(B) Yes, it was very informative."},{"key":"C","text":"(C) Next Thursday at 9:00 A.M."}]}', '{"selected":"A","key":"A"}', '{"transcript":"Where will the annual shareholder meeting be held this year?\n(A) At the Grand Plaza Hotel in Chicago.\n(B) Yes, it was very informative.\n(C) Next Thursday at 9:00 A.M.","translation":"Đại hội đồng cổ đông thường niên năm nay sẽ được tổ chức ở đâu?\n(A) Tại Khách sạn Grand Plaza ở Chicago.\n(B) Vâng, nó đã rất bổ ích.\n(C) Thứ Năm tới lúc 9:00 sáng.","clue":"Từ để hỏi \"Where\" yêu cầu câu trả lời chỉ nơi chốn cụ thể.","analysis":"(A) là địa điểm chính xác. (B) sai vì câu hỏi Wh-question không thể trả lời bằng Yes/No. (C) bẫy câu hỏi thời gian (When).","vocabulary":[{"word":"shareholder","type":"n","meaning":"cổ đông"},{"word":"annual meeting","type":"n","meaning":"cuộc họp thường niên"}]}', NOW(), NOW());
INSERT INTO questions (id, name, type, difficulty, sections, descriptions, part_id, parent_id, question_data, correct_answer, explanation, created_at, updated_at) VALUES
(4, 'Part 2: Wh-Question - Who approved the budget increase?', 'SINGLE_CHOICE', 3, 'LISTENING', 'Câu hỏi Who hỏi người phê duyệt ngân sách.', 2, NULL, '{"text":"Who approved the proposed budget increase for marketing?","audioUrl":"https://res.cloudinary.com/leby-toeic/video/upload/v1714500031/audio/part2_q04_who.mp3","options":[{"key":"A","text":"(A) About fifteen percent more than last quarter."},{"key":"B","text":"(B) Ms. Davis signed off on it yesterday."},{"key":"C","text":"(C) To attract more international clients."}]}', '{"selected":"B","key":"B"}', '{"transcript":"Who approved the proposed budget increase for marketing?\n(A) About fifteen percent more than last quarter.\n(B) Ms. Davis signed off on it yesterday.\n(C) To attract more international clients.","translation":"Ai đã phê duyệt mức tăng ngân sách tiếp thị đề xuất?\n(A) Nhiều hơn khoảng 15% so với quý trước.\n(B) Bà Davis đã ký duyệt nó vào hôm qua.\n(C) Để thu hút thêm khách hàng quốc tế.","clue":"Từ để hỏi \"Who\" cần câu trả lời chỉ người hoặc chức danh.","analysis":"Cụm từ \"Ms. Davis signed off on it\" (Bà Davis đã ký duyệt) trả lời trực tiếp cho \"Who approved\". (A) trả lời câu hỏi \"How much\", (C) trả lời câu hỏi \"Why\".","vocabulary":[{"word":"sign off on","type":"phr v","meaning":"chính thức phê duyệt, ký duyệt"},{"word":"budget increase","type":"n","meaning":"sự gia tăng ngân sách"}]}', NOW(), NOW());
INSERT INTO questions (id, name, type, difficulty, sections, descriptions, part_id, parent_id, question_data, correct_answer, explanation, created_at, updated_at) VALUES
(5, 'Part 2: Indirect Response - Are we getting a new photocopier?', 'SINGLE_CHOICE', 4, 'LISTENING', 'Câu hỏi Yes/No với phản hồi gián tiếp.', 2, NULL, '{"text":"Aren\'t we getting a replacement for the broken photocopier soon?","audioUrl":"https://res.cloudinary.com/leby-toeic/video/upload/v1714500032/audio/part2_q05_indirect.mp3","options":[{"key":"A","text":"(A) The procurement team is still waiting for quotes."},{"key":"B","text":"(B) Make fifty double-sided copies, please."},{"key":"C","text":"(C) Yes, the photo looks great."}]}', '{"selected":"A","key":"A"}', '{"transcript":"Aren\'t we getting a replacement for the broken photocopier soon?\n(A) The procurement team is still waiting for quotes.\n(B) Make fifty double-sided copies, please.\n(C) Yes, the photo looks great.","translation":"Chẳng phải chúng ta sắp có máy photocopy thay thế máy hỏng rồi sao?\n(A) Bộ phận thu mua vẫn đang chờ bảng báo giá.\n(B) Làm ơn photo 50 bản hai mặt.\n(C) Vâng, bức ảnh trông rất tuyệt.","clue":"Câu hỏi phủ định xác nhận trạng thái máy photocopy mới.","analysis":"(A) là câu trả lời gián tiếp xuất sắc: \"chưa có vì bộ phận thu mua còn đang chờ báo giá\". (B) bẫy từ liên quan \"copies\". (C) bẫy đồng âm \"photo\" trong \"photocopier\".","vocabulary":[{"word":"procurement","type":"n","meaning":"hoạt động thu mua vật tư"},{"word":"quote","type":"n","meaning":"bảng báo giá"}]}', NOW(), NOW());
INSERT INTO questions (id, name, type, difficulty, sections, descriptions, part_id, parent_id, question_data, correct_answer, explanation, created_at, updated_at) VALUES
(6, 'Part 3 (Stimulus): Conversation between HR manager and job applicant', 'MULTIPLE_CHOICE', 3, 'LISTENING', 'Đoạn thoại Part 3: Phỏng vấn nhân sự và thỏa thuận lịch bắt đầu công việc.', 3, NULL, '{"text":"Questions 7 through 9 refer to the following conversation.","audioUrl":"https://res.cloudinary.com/leby-toeic/video/upload/v1714500040/audio/part3_conv01_hr.mp3","context":"Listen to a conversation between an HR director and a prospective candidate."}', '{"info":"Parent conversation container"}', '{"transcript":"W: Good morning, Mr. Campbell. Thank you for coming in today for the final interview for our Senior Financial Analyst role. We were very impressed by your portfolio.\nM: Thank you, Ms. Lee. I\'ve been following your company\'s sustainable investment initiatives for years, so I\'m thrilled to be considered.\nW: Wonderful. The board has already authorized the hiring decision. Could you potentially start at our Singapore branch by the beginning of next month?\nM: That shouldn\'t be a problem at all. My current contract concludes in two weeks, which gives me ample time to relocate.","translation":"Nữ: Chào buổi sáng, anh Campbell. Cảm ơn anh đã đến hôm nay tham dự vòng phỏng vấn cuối cùng cho vị trí Chuyên viên phân tích tài chính cao cấp. Chúng tôi rất ấn tượng với hồ sơ năng lực của anh.\nNam: Cảm ơn cô Lee. Tôi đã theo dõi các sáng kiến đầu tư bền vững của công ty cô nhiều năm nay, nên tôi vô cùng hào hứng khi được cân nhắc.\nNữ: Tuyệt vời. Ban giám đốc đã chính thức ủy quyền quyết định tuyển dụng. Liệu anh có thể bắt đầu làm việc tại chi nhánh Singapore vào đầu tháng tới không?\nNam: Điều đó hoàn toàn không thành vấn đề. Hợp đồng hiện tại của tôi sẽ kết thúc sau hai tuần nữa, đủ để tôi có thời gian thu xếp chuyển chỗ ở.","analysis":"Đoạn hội thoại về buổi phỏng vấn vị trí Senior Financial Analyst, địa điểm làm việc ở Singapore và thời điểm bắt đầu vào đầu tháng sau.","vocabulary":[{"word":"portfolio","type":"n","meaning":"hồ sơ năng lực, danh mục đầu tư"},{"word":"relocate","type":"v","meaning":"chuyển địa điểm sinh sống / công tác"}]}', NOW(), NOW());
INSERT INTO questions (id, name, type, difficulty, sections, descriptions, part_id, parent_id, question_data, correct_answer, explanation, created_at, updated_at) VALUES
(7, 'Part 3 Q1: What position is being discussed?', 'SINGLE_CHOICE', 2, 'LISTENING', 'Câu hỏi vị trí ứng tuyển.', 3, 6, '{"text":"What position is the man interviewing for?","options":[{"key":"A","text":"(A) Sustainable Energy Consultant"},{"key":"B","text":"(B) Senior Financial Analyst"},{"key":"C","text":"(C) Human Resources Director"},{"key":"D","text":"(D) Regional Branch Manager"}]}', '{"selected":"B","key":"B"}', '{"clue":"Người phụ nữ nói: \"interview for our Senior Financial Analyst role\".","analysis":"Thông tin nêu trực tiếp ngay ở câu đầu tiên của người phụ nữ.","vocabulary":[{"word":"analyst","type":"n","meaning":"chuyên viên phân tích"}]}', NOW(), NOW());
INSERT INTO questions (id, name, type, difficulty, sections, descriptions, part_id, parent_id, question_data, correct_answer, explanation, created_at, updated_at) VALUES
(8, 'Part 3 Q2: Where will the man be based?', 'SINGLE_CHOICE', 3, 'LISTENING', 'Câu hỏi địa điểm chi nhánh làm việc.', 3, 6, '{"text":"Where is the job located?","options":[{"key":"A","text":"(A) In Tokyo"},{"key":"B","text":"(B) In Singapore"},{"key":"C","text":"(C) In Chicago"},{"key":"D","text":"(D) In London"}]}', '{"selected":"B","key":"B"}', '{"clue":"Người phụ nữ hỏi: \"Could you potentially start at our Singapore branch...?\"","analysis":"Địa điểm làm việc cụ thể được đề cập là chi nhánh Singapore.","vocabulary":[{"word":"branch","type":"n","meaning":"chi nhánh văn phòng"}]}', NOW(), NOW());
INSERT INTO questions (id, name, type, difficulty, sections, descriptions, part_id, parent_id, question_data, correct_answer, explanation, created_at, updated_at) VALUES
(9, 'Part 3 Q3: When can the man start work?', 'SINGLE_CHOICE', 3, 'LISTENING', 'Câu hỏi thời gian bắt đầu công việc.', 3, 6, '{"text":"When is the man expected to begin working?","options":[{"key":"A","text":"(A) Tomorrow morning"},{"key":"B","text":"(B) In two weeks"},{"key":"C","text":"(C) At the beginning of next month"},{"key":"D","text":"(D) At the end of the year"}]}', '{"selected":"C","key":"C"}', '{"clue":"Người phụ nữ đề xuất \"by the beginning of next month\" và người đàn ông xác nhận \"That shouldn\'t be a problem at all\".","analysis":"Bẫy đáp án (B) vì người đàn ông chỉ nhắc hợp đồng cũ hết hạn sau 2 tuần, thời điểm bắt đầu việc mới là đầu tháng sau.","vocabulary":[{"word":"conclude","type":"v","meaning":"kết thúc, chấm dứt hợp đồng"}]}', NOW(), NOW());
INSERT INTO questions (id, name, type, difficulty, sections, descriptions, part_id, parent_id, question_data, correct_answer, explanation, created_at, updated_at) VALUES
(10, 'Part 4 (Stimulus): Airport gate departure delay announcement', 'MULTIPLE_CHOICE', 3, 'LISTENING', 'Bài nói ngắn Part 4: Thông báo hoãn chuyến bay tại cổng sân bay.', 4, NULL, '{"text":"Questions 11 and 12 refer to the following airport announcement.","audioUrl":"https://res.cloudinary.com/leby-toeic/video/upload/v1714500050/audio/part4_airport_delay.mp3","context":"Listen to an announcement made over an airport public address system."}', '{"info":"Parent announcement container"}', '{"transcript":"Attention all passengers booked on Pacific Sky Flight 402 departing for Seattle. Due to severe thunderstorms along our scheduled flight path, departure has been pushed back from 3:15 P.M. to approximately 5:30 P.M. We apologize for the inconvenience. Passengers traveling with young children or requiring special assistance may approach Gate 18 immediately to receive complimentary meal vouchers valid at any terminal dining outlet.","translation":"Xin quý hành khách trên chuyến bay Pacific Sky 402 đi Seattle chú ý. Do cơn giông bão nghiêm trọng dọc theo đường bay dự kiến, giờ khởi hành đã được lùi từ 3:15 chiều sang khoảng 5:30 chiều. Chúng tôi thành thật xin lỗi vì sự bất tiện này. Hành khách đi cùng trẻ nhỏ hoặc cần hỗ trợ đặc biệt xin vui lòng đến Cổng 18 ngay bây giờ để nhận phiếu ăn miễn phí có giá trị tại bất kỳ nhà hàng nào trong nhà ga.","analysis":"Thông báo delay chuyến bay Pacific Sky 402 do thời tiết giông bão, hoãn đến 5:30 chiều, phát phiếu ăn tại Cổng 18.","vocabulary":[{"word":"severe thunderstorm","type":"n","meaning":"cơn giông bão dữ dội"},{"word":"complimentary voucher","type":"n","meaning":"phiếu quà tặng miễn phí"}]}', NOW(), NOW());
INSERT INTO questions (id, name, type, difficulty, sections, descriptions, part_id, parent_id, question_data, correct_answer, explanation, created_at, updated_at) VALUES
(11, 'Part 4 Q1: Why has the flight been delayed?', 'SINGLE_CHOICE', 2, 'LISTENING', 'Nguyên nhân hoãn chuyến bay.', 4, 10, '{"text":"What is the reason for the flight delay?","options":[{"key":"A","text":"(A) Mechanical engine problems"},{"key":"B","text":"(B) Severe weather conditions"},{"key":"C","text":"(C) Air traffic controller strikes"},{"key":"D","text":"(D) Security inspection at the gate"}]}', '{"selected":"B","key":"B"}', '{"clue":"Người phát thanh nêu rõ: \"Due to severe thunderstorms along our scheduled flight path\".","analysis":"Giông bão là điều kiện thời tiết xấu (Severe weather conditions).","vocabulary":[{"word":"departure","type":"n","meaning":"sự khởi hành"}]}', NOW(), NOW());
INSERT INTO questions (id, name, type, difficulty, sections, descriptions, part_id, parent_id, question_data, correct_answer, explanation, created_at, updated_at) VALUES
(12, 'Part 4 Q2: What can passengers collect at Gate 18?', 'SINGLE_CHOICE', 3, 'LISTENING', 'Đồ vật/phiếu được nhận tại cổng 18.', 4, 10, '{"text":"What can eligible passengers receive at Gate 18?","options":[{"key":"A","text":"(A) Free overnight hotel rooms"},{"key":"B","text":"(B) Replacement boarding passes"},{"key":"C","text":"(C) Meal vouchers"},{"key":"D","text":"(D) Upgraded business class tickets"}]}', '{"selected":"C","key":"C"}', '{"clue":"\"approach Gate 18 immediately to receive complimentary meal vouchers\".","analysis":"Hành khách đủ điều kiện sẽ được phát meal vouchers.","vocabulary":[{"word":"voucher","type":"n","meaning":"phiếu mua hàng / ăn uống"}]}', NOW(), NOW());
INSERT INTO questions (id, name, type, difficulty, sections, descriptions, part_id, parent_id, question_data, correct_answer, explanation, created_at, updated_at) VALUES
(13, 'Part 5: Word Formation - Adverb modifying participle adjective', 'SINGLE_CHOICE', 3, 'READING', 'Ngữ pháp từ loại: Trạng từ bổ nghĩa cho tính từ phân từ.', 5, NULL, '{"text":"The newly appointed director was _______ praised by the board for her visionary restructuring plan.","options":[{"key":"A","text":"high"},{"key":"B","text":"highly"},{"key":"C","text":"height"},{"key":"D","text":"heighten"}]}', '{"selected":"B","key":"B"}', '{"transcript":"The newly appointed director was highly praised by the board for her visionary restructuring plan.","translation":"Giám đốc mới được bổ nhiệm đã được ban giám đốc khen ngợi hết lời vì kế hoạch tái cấu trúc mang tầm nhìn xa.","clue":"Chỗ trống đứng giữa trợ động từ \"was\" và quá khứ phân từ \"praised\" (bị động).","analysis":"Vị trí này chỉ có thể là một Trạng từ (Adverb) để bổ nghĩa cho động từ \"praised\". \"Highly praised\" là một collocation kinh điển có nghĩa là \"được đánh giá/khen ngợi rất cao\".","vocabulary":[{"word":"highly praised","type":"adj phr","meaning":"được khen ngợi nhiệt liệt"},{"word":"restructuring","type":"n","meaning":"sự tái cấu trúc"}]}', NOW(), NOW());
INSERT INTO questions (id, name, type, difficulty, sections, descriptions, part_id, parent_id, question_data, correct_answer, explanation, created_at, updated_at) VALUES
(14, 'Part 5: Verb Tense - Condition with "By the time"', 'SINGLE_CHOICE', 4, 'READING', 'Ngữ pháp thì: Cấu trúc By the time với Tương lai hoàn thành.', 5, NULL, '{"text":"By the time the new logistics facility opens next spring, our software engineers _______ the inventory database.","options":[{"key":"A","text":"will have migrated"},{"key":"B","text":"have migrated"},{"key":"C","text":"migrated"},{"key":"D","text":"are migrating"}]}', '{"selected":"A","key":"A"}', '{"transcript":"By the time the new logistics facility opens next spring, our software engineers will have migrated the inventory database.","translation":"Trước thời điểm cơ sở hậu cần mới mở cửa vào mùa xuân tới, các kỹ sư phần mềm của chúng tôi sẽ hoàn tất việc chuyển đổi cơ sở dữ liệu hàng tồn kho.","clue":"Cấu trúc cố định: \"By the time + S + V(hiện tại đơn), S + will have + V3/ed (Tương lai hoàn thành)\".","analysis":"Hành động chuyển đổi dữ liệu sẽ được hoàn tất trước một mốc thời gian trong tương lai (next spring), nên bắt buộc dùng thì Tương lai hoàn thành \"will have migrated\".","vocabulary":[{"word":"migrate","type":"v","meaning":"di chuyển, chuyển đổi hệ thống"},{"word":"inventory","type":"n","meaning":"hàng hóa tồn kho"}]}', NOW(), NOW());
INSERT INTO questions (id, name, type, difficulty, sections, descriptions, part_id, parent_id, question_data, correct_answer, explanation, created_at, updated_at) VALUES
(15, 'Part 5: Business Collocation - "Adhere to safety regulations"', 'SINGLE_CHOICE', 3, 'READING', 'Từ vựng: Cụm giới từ công sở đi kèm adhere.', 5, NULL, '{"text":"All factory employees are strictly mandated to _______ to the updated safety protocols while operating heavy machinery.","options":[{"key":"A","text":"adhere"},{"key":"B","text":"comply"},{"key":"C","text":"follow"},{"key":"D","text":"observe"}]}', '{"selected":"A","key":"A"}', '{"transcript":"All factory employees are strictly mandated to adhere to the updated safety protocols while operating heavy machinery.","translation":"Tất cả nhân viên nhà máy được yêu cầu nghiêm ngặt phải tuân thủ các quy định an toàn cập nhật trong khi vận hành máy móc hạng nặng.","clue":"Quan sát giới từ \"to\" ngay sau chỗ trống.","analysis":"Chỉ có \"adhere + to\" mang nghĩa tuân thủ. \"Comply\" phải đi với \"with\" (comply with). \"Follow\" và \"observe\" là ngoại động từ đi trực tiếp với danh từ, không có giới từ \"to\".","vocabulary":[{"word":"adhere to","type":"v phr","meaning":"tuân thủ nghiêm ngặt quy định"},{"word":"protocol","type":"n","meaning":"quy trình, quy định an toàn"}]}', NOW(), NOW());
INSERT INTO questions (id, name, type, difficulty, sections, descriptions, part_id, parent_id, question_data, correct_answer, explanation, created_at, updated_at) VALUES
(16, 'Part 6 (Stimulus): Internal memo regarding cybersecurity update', 'MULTIPLE_CHOICE', 3, 'READING', 'Đoạn văn điền từ Part 6: Thông báo bảo mật hệ thống nội bộ.', 6, NULL, '{"text":"Questions 17 and 18 refer to the following internal company memorandum.","content":"MEMORANDUM\\nTo: All Staff Members\\nFrom: IT Security Department\\nDate: October 14\\nSubject: Mandatory Two-Factor Authentication\\n\\nIn light of recent phishing attempts targeting our regional servers, the IT department is implementing a mandatory two-factor authentication (2FA) protocol across all employee accounts. Beginning next Monday, staff will be prompted to verify their identity via a mobile authenticator application. ----[17]----, access to corporate email and client portals will be temporarily restricted.\\n\\n----[18]----. Our technical support specialists will be hosting walk-in assistance sessions in Room 204 throughout the week to help anyone experiencing configuration difficulties."}', '{"info":"Parent memo container"}', '{"translation":"BẢN GHI NHỚ NỘI BỘ\\nGửi: Toàn thể nhân viên\\nTừ: Bộ phận An ninh CNTT\\nNgày: 14 tháng 10\\nTiêu đề: Xác thực hai yếu tố bắt buộc\\n\\nTrước các nỗ lực lừa đảo gần đây nhắm vào máy chủ khu vực của chúng ta, phòng CNTT đang triển khai quy trình xác thực hai yếu tố (2FA) bắt buộc trên mọi tài khoản nhân viên. Bắt đầu từ thứ Hai tới, nhân viên sẽ được nhắc xác minh danh tính qua ứng dụng xác thực trên điện thoại. ----[17]----, quyền truy cập vào email công ty và cổng thông tin khách hàng sẽ tạm thời bị giới hạn.\\n\\n----[18]----. Các chuyên viên hỗ trợ kỹ thuật sẽ tổ chức các buổi hỗ trợ trực tiếp tại Phòng 204 trong suốt tuần để trợ giúp bất kỳ ai gặp khó khăn trong việc cài đặt.","analysis":"Bản tin thông báo bắt buộc cài đặt bảo mật 2 lớp 2FA, giải thích hậu quả nếu không cài đặt và hỗ trợ tại phòng 204.","vocabulary":[{"word":"two-factor authentication","type":"n","meaning":"xác thực 2 yếu tố (2FA)"},{"word":"mandatory","type":"adj","meaning":"mang tính bắt buộc"}]}', NOW(), NOW());
INSERT INTO questions (id, name, type, difficulty, sections, descriptions, part_id, parent_id, question_data, correct_answer, explanation, created_at, updated_at) VALUES
(17, 'Part 6 Q1: Transitional word selection', 'SINGLE_CHOICE', 3, 'READING', 'Chọn từ nối chuyển tiếp chỉ điều kiện/hậu quả.', 6, 16, '{"text":"Select the best word or phrase to fill in blank [17].","options":[{"key":"A","text":"Otherwise"},{"key":"B","text":"Furthermore"},{"key":"C","text":"Similarly"},{"key":"D","text":"In conclusion"}]}', '{"selected":"A","key":"A"}', '{"clue":"Ý trước yêu cầu xác minh danh tính; ý sau cảnh báo quyền truy cập sẽ bị giới hạn.","analysis":"\"Otherwise\" (Nếu không thì) thể hiện hậu quả tiêu cực nếu hành động ở câu trước không được thực hiện.","vocabulary":[{"word":"otherwise","type":"adv","meaning":"nếu không thì"}]}', NOW(), NOW());
INSERT INTO questions (id, name, type, difficulty, sections, descriptions, part_id, parent_id, question_data, correct_answer, explanation, created_at, updated_at) VALUES
(18, 'Part 6 Q2: Sentence insertion', 'SINGLE_CHOICE', 4, 'READING', 'Chọn câu văn hoàn chỉnh điền vào vị trí [18].', 6, 16, '{"text":"Which sentence best fits in blank [18]?","options":[{"key":"A","text":"Please ensure that you set up your mobile device prior to the deadline."},{"key":"B","text":"The servers will be shut down permanently next weekend."},{"key":"C","text":"We recommend that you change your office chair immediately."},{"key":"D","text":"All paper files must be shredded before Friday."}]}', '{"selected":"A","key":"A"}', '{"clue":"Câu sau nói về việc phòng IT hỗ trợ kỹ thuật cài đặt trong suốt tuần.","analysis":"Câu (A) \"Hãy đảm bảo bạn thiết lập thiết bị di động trước hạn chót\" liên kết trực tiếp với dịch vụ hỗ trợ walk-in sessions của phòng IT ở câu kế tiếp.","vocabulary":[{"word":"prior to","type":"prep","meaning":"trước một thời điểm nào đó"}]}', NOW(), NOW());
INSERT INTO questions (id, name, type, difficulty, sections, descriptions, part_id, parent_id, question_data, correct_answer, explanation, created_at, updated_at) VALUES
(19, 'Part 7 (Stimulus): Job posting for Logistics Operations Director', 'MULTIPLE_CHOICE', 4, 'READING', 'Văn bản Part 7: Bản tin tuyển dụng vị trí Giám đốc Vận hành Hậu cần.', 7, NULL, '{"text":"Questions 20 and 21 refer to the following job vacancy notice.","content":"GLOBAL TRANSIT LOGISTICS CORP.\\nPosition: Director of Regional Logistics\\nLocation: Da Nang Harbor Complex, Vietnam\\nEmployment Type: Full-Time\\n\\nAbout the Role:\\nGlobal Transit Logistics is seeking an experienced Director of Regional Logistics to oversee maritime and overland freight supply chains across Southeast Asia. The successful candidate will manage a team of over 60 dispatch coordinators, optimize warehouse fulfillment cycles, and negotiate carrier contracts with international maritime shipping lines.\\n\\nKey Qualifications:\\n- Bachelor degree in Supply Chain Management, Business Administration, or related discipline.\\n- Minimum of 8 years of progressive management experience in freight forwarding or warehouse logistics.\\n- Demonstrated proficiency with SAP S/4HANA enterprise resource software.\\n- Fluent written and spoken proficiency in both English and Vietnamese.\\n\\nBenefits & Remuneration:\\n- Competitive base salary with biannual performance bonuses.\\n- Comprehensive health insurance for candidate and direct dependents.\\n- Generous relocation package provided for non-local hires.\\n\\nTo apply, please forward your curriculum vitae and three professional references to recruitment@globaltransit.com by November 30."}', '{"info":"Parent reading passage container"}', '{"translation":"TẬP ĐOÀN HẬU CẦN GLOBAL TRANSIT\\nVị trí: Giám đốc Vận hành Hậu cần Khu vực\\nĐịa điểm: Khu phức hợp Cảng Đà Nẵng, Việt Nam\\nHình thức: Toàn thời gian\\n\\nVề vị trí công việc:\\nGlobal Transit Logistics đang tìm kiếm một Giám đốc Vận hành Hậu cần giàu kinh nghiệm để giám sát chuỗi cung ứng vận tải đường biển và đường bộ trên khắp Đông Nam Á. Ứng viên được chọn sẽ quản lý đội ngũ hơn 60 điều phối viên, tối ưu hóa chu kỳ xuất kho và đàm phán hợp đồng với các hãng tàu quốc tế.\\n\\nYêu cầu chính:\\n- Bằng cử nhân chuyên ngành Quản lý chuỗi cung ứng, Quản trị kinh doanh hoặc liên quan.\\n- Tối thiểu 8 năm kinh nghiệm quản lý trong lĩnh vực giao nhận vận tải hoặc kho vận.\\n- Thành thạo phần mềm quản trị doanh nghiệp SAP S/4HANA.\\n- Trôi chảy tiếng Anh và tiếng Việt cả nói lẫn viết.\\n\\nQuyền lợi & Đãi ngộ:\\n- Lương cơ bản cạnh tranh cùng thưởng hiệu suất 2 lần/năm.\\n- Bảo hiểm y tế toàn diện cho ứng viên và người phụ thuộc.\\n- Gói hỗ trợ chuyển nơi ở hào phóng cho ứng viên ngoại tỉnh.\\n\\nHạn chót nộp hồ sơ: 30 tháng 11 qua email recruitment@globaltransit.com.","analysis":"Bản tin tuyển dụng Giám đốc Hậu cần tại Đà Nẵng, yêu cầu tối thiểu 8 năm kinh nghiệm, kỹ năng SAP S/4HANA và song ngữ Anh - Việt.","vocabulary":[{"word":"freight forwarding","type":"n","meaning":"giao nhận vận tải hàng hóa"},{"word":"relocation package","type":"n","meaning":"gói hỗ trợ chi phí chuyển nơi ở"}]}', NOW(), NOW());
INSERT INTO questions (id, name, type, difficulty, sections, descriptions, part_id, parent_id, question_data, correct_answer, explanation, created_at, updated_at) VALUES
(20, 'Part 7 Q1: Responsibility identification', 'SINGLE_CHOICE', 3, 'READING', 'Câu hỏi chi tiết trách nhiệm công việc.', 7, 19, '{"text":"According to the notice, what is one of the primary duties of the position?","options":[{"key":"A","text":"Designing corporate software systems"},{"key":"B","text":"Negotiating carrier contracts with shipping lines"},{"key":"C","text":"Conducting customs inspections at the border"},{"key":"D","text":"Purchasing new cargo transport vessels"}]}', '{"selected":"B","key":"B"}', '{"clue":"Đoạn văn nêu: \"negotiate carrier contracts with international maritime shipping lines\".","analysis":"Đáp án (B) phản ánh đúng nguyên văn nhiệm vụ trong bản mô tả công việc.","vocabulary":[{"word":"negotiate","type":"v","meaning":"đàm phán, thương lượng hợp đồng"}]}', NOW(), NOW());
INSERT INTO questions (id, name, type, difficulty, sections, descriptions, part_id, parent_id, question_data, correct_answer, explanation, created_at, updated_at) VALUES
(21, 'Part 7 Q2: Qualification check', 'SINGLE_CHOICE', 3, 'READING', 'Câu hỏi kiểm tra tiêu chuẩn kinh nghiệm.', 7, 19, '{"text":"What is a mandatory requirement for applicants?","options":[{"key":"A","text":"At least 8 years of logistics management experience"},{"key":"B","text":"A Master\'s degree in Maritime Law"},{"key":"C","text":"Current residency in Da Nang"},{"key":"D","text":"Fluency in three Asian languages"}]}', '{"selected":"A","key":"A"}', '{"clue":"Mục Key Qualifications: \"Minimum of 8 years of progressive management experience in freight forwarding\".","analysis":"Tối thiểu 8 năm kinh nghiệm quản lý hậu cần là yêu cầu bắt buộc (A). Đáp án (C) sai vì công ty có gói hỗ trợ cho ứng viên ngoại tỉnh.","vocabulary":[{"word":"progressive experience","type":"n","meaning":"kinh nghiệm thăng tiến theo thời gian"}]}', NOW(), NOW());

-- =====================================================================================
-- PHẦN 10: LIÊN KẾT CÂU HỎI VỚI NĂNG LỰC & BÀI THỰC HÀNH
-- =====================================================================================
-- Question trains Ability (question_abilities)
INSERT INTO question_abilities (id, order_no, points, question_id, ability_id) VALUES
(1, 1, 5, 1, 1),
(2, 2, 5, 2, 2),
(3, 1, 5, 3, 3),
(4, 2, 5, 4, 3),
(5, 3, 5, 5, 5),
(6, 1, 5, 7, 6),
(7, 2, 5, 8, 6),
(8, 3, 5, 9, 7),
(9, 1, 5, 11, 8),
(10, 2, 5, 12, 8),
(11, 1, 5, 13, 10),
(12, 2, 5, 14, 11),
(13, 3, 5, 15, 12),
(14, 1, 5, 17, 14),
(15, 2, 5, 18, 13),
(16, 1, 5, 20, 15),
(17, 2, 5, 21, 16);

-- Practice Questions (practice_questions) gắn vào các bài tập PracticeLesson
INSERT INTO practice_questions (id, order_no, points, practice_lesson_id, question_id) VALUES
(1, 1, 10, 2, 1),
(2, 2, 10, 2, 2),
(3, 1, 10, 4, 3),
(4, 2, 10, 4, 4),
(5, 3, 10, 4, 5),
(6, 1, 10, 7, 13),
(7, 2, 10, 7, 14),
(8, 1, 10, 9, 15);

-- Module Test Questions (module_test_questions) cho bài thi vượt ải
INSERT INTO module_test_questions (id, order_no, points, module_id, question_id) VALUES
(1, 1, 10, 1, 1),
(2, 2, 10, 1, 2),
(3, 1, 10, 2, 3),
(4, 2, 10, 2, 4),
(5, 3, 10, 2, 5),
(6, 1, 10, 5, 13),
(7, 2, 10, 5, 14),
(8, 1, 10, 6, 15),
(9, 1, 10, 10, 1),
(10, 2, 10, 10, 3),
(11, 3, 10, 10, 13),
(12, 4, 10, 10, 15),
(13, 5, 10, 10, 20);

-- =====================================================================================
-- PHẦN 11: ĐỀ THI CHẨN ĐOÁN (DIAGNOSTIC TESTS)
-- =====================================================================================
INSERT INTO diagnostic_tests (id, title, description, status, test_type, duration_minutes, created_at, updated_at) VALUES
(1, 'Bài thi chẩn đoán năng lực TOEIC đầu vào (Diagnostic Test 45P)', 'Đề thi chẩn đoán thích ứng 45 phút quét toàn diện 7 Part, làm căn cứ sinh lộ trình học cá nhân hóa.', 1, 'DIAGNOSTIC', 45, NOW(), NOW()),
(2, 'Đề thi thử toàn diện ETS TOEIC Mock Test 2026 (120P - 200 câu)', 'Bộ đề thi thử chuẩn cấu trúc đề thi thật ETS gồm 100 câu Listening và 100 câu Reading.', 1, 'FULL_MOCK_TEST', 120, NOW(), NOW());

-- Gắn câu hỏi vào Đề thi chẩn đoán số 1 (diagnostic_test_questions)
INSERT INTO diagnostic_test_questions (id, order_no, points, diagnostic_test_id, question_id) VALUES
(1, 1, 5, 1, 1),
(2, 2, 5, 1, 2),
(3, 3, 5, 1, 3),
(4, 4, 5, 1, 4),
(5, 5, 5, 1, 5),
(6, 6, 5, 1, 7),
(7, 7, 5, 1, 8),
(8, 8, 5, 1, 9),
(9, 9, 5, 1, 11),
(10, 10, 5, 1, 12),
(11, 11, 5, 1, 13),
(12, 12, 5, 1, 14),
(13, 13, 5, 1, 15),
(14, 14, 5, 1, 17),
(15, 15, 5, 1, 18),
(16, 16, 5, 1, 20),
(17, 17, 5, 1, 21);

-- =====================================================================================
-- PHẦN 12: LỊCH SỬ THI & LỘ TRÌNH THÍCH ỨNG CỦA HỌC VIÊN DEMO (STUDENT NAM - ID 3)
-- Minh họa luồng nghiệp vụ hoàn chỉnh từ thi chẩn đoán -> sinh lộ trình -> học module
-- =====================================================================================

-- 1. Lượt thi chẩn đoán của học viên Nam (User ID 3)
INSERT INTO diagnostic_attempts (id, started_at, completed_at, status, duration_seconds, user_id, diagnostic_test_id, created_at, updated_at) VALUES
(1, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY) + INTERVAL 2580 SECOND, 'SUBMITTED', 2580, 3, 1, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY));

-- 2. Chi tiết từng câu trả lời trong bài thi chẩn đoán (diagnostic_answers)
INSERT INTO diagnostic_answers (id, user_answer, is_correct, is_flagged, diagnostic_attempt_id, question_id, created_at, updated_at) VALUES
(1, '{"selected": "A"}', 1, 0, 1, 1, NOW(), NOW()),
(2, '{"selected": "B"}', 1, 0, 1, 2, NOW(), NOW()),
(3, '{"selected": "A"}', 1, 0, 1, 3, NOW(), NOW()),
(4, '{"selected": "B"}', 1, 0, 1, 4, NOW(), NOW()),
(5, '{"selected": "B"}', 0, 1, 1, 5, NOW(), NOW()), -- Sai câu 5 (bẫy indirect)
(6, '{"selected": "B"}', 1, 0, 1, 7, NOW(), NOW()),
(7, '{"selected": "A"}', 0, 0, 1, 8, NOW(), NOW()), -- Sai câu 8 (địa điểm chi nhánh)
(8, '{"selected": "C"}', 1, 0, 1, 9, NOW(), NOW()),
(9, '{"selected": "B"}', 1, 0, 1, 11, NOW(), NOW()),
(10, '{"selected": "C"}', 1, 0, 1, 12, NOW(), NOW()),
(11, '{"selected": "B"}', 1, 0, 1, 13, NOW(), NOW()),
(12, '{"selected": "B"}', 0, 0, 1, 14, NOW(), NOW()), -- Sai câu 14 (thì by the time)
(13, '{"selected": "A"}', 1, 0, 1, 15, NOW(), NOW()),
(14, '{"selected": "A"}', 1, 0, 1, 17, NOW(), NOW()),
(15, '{"selected": "D"}', 0, 1, 1, 18, NOW(), NOW()), -- Sai câu 18 (sentence insertion Part 6)
(16, '{"selected": "B"}', 1, 0, 1, 20, NOW(), NOW()),
(17, '{"selected": "A"}', 1, 0, 1, 21, NOW(), NOW());

-- 3. Kết quả tổng hợp 7 Part sau khi chấm (part_diagnostic_results)
-- Part 1: 100% -> PASS (Miễn học)
-- Part 2: 66.7% -> CONFIRM (Soi Tầng 2)
-- Part 3: 66.7% -> CONFIRM (Soi Tầng 2)
-- Part 4: 100% -> PASS (Miễn học)
-- Part 5: 66.7% -> CONFIRM (Soi Tầng 2)
-- Part 6: 50.0% -> ABILITY_DIAGNOSTIC (Dưới chuẩn -> Bốc module)
-- Part 7: 100% -> PASS (Miễn học)
INSERT INTO part_diagnostic_results (id, score, directive, diagnostic_attempt_id, part_id, created_at, updated_at) VALUES
(1, 100.0, 'PASS', 1, 1, NOW(), NOW()),
(2, 66.7, 'CONFIRM', 1, 2, NOW(), NOW()),
(3, 66.7, 'CONFIRM', 1, 3, NOW(), NOW()),
(4, 100.0, 'PASS', 1, 4, NOW(), NOW()),
(5, 66.7, 'CONFIRM', 1, 5, NOW(), NOW()),
(6, 50.0, 'ABILITY_DIAGNOSTIC', 1, 6, NOW(), NOW()),
(7, 100.0, 'PASS', 1, 7, NOW(), NOW());

-- 4. Trạng thái năng lực học viên được cập nhật (user_abilities)
INSERT INTO user_abilities (id, status, evidence_count, accuracy_rate, user_id, ability_id, part_id, created_at, updated_at) VALUES
(1, 'STABLE', 3, 100.0, 3, 1, 1, NOW(), NOW()),
(2, 'DEVELOPING', 3, 66.7, 3, 3, 2, NOW(), NOW()),
(3, 'WEAK', 3, 0.0, 3, 5, 2, NOW(), NOW()),
(4, 'DEVELOPING', 3, 66.7, 3, 6, 3, NOW(), NOW()),
(5, 'STABLE', 3, 100.0, 3, 8, 4, NOW(), NOW()),
(6, 'STABLE', 3, 100.0, 3, 10, 5, NOW(), NOW()),
(7, 'WEAK', 3, 0.0, 3, 11, 5, NOW(), NOW()),
(8, 'WEAK', 3, 0.0, 3, 13, 6, NOW(), NOW()),
(9, 'STABLE', 3, 100.0, 3, 15, 7, NOW(), NOW());

-- 5. Lộ trình học thích ứng được tạo tự động (LearningPath)
INSERT INTO learning_paths (id, version, status, user_id, target_profile_id, diagnostic_attempt_id, created_at, updated_at) VALUES
(1, 1, 'ACTIVE', 3, 2, 1, DATE_SUB(NOW(), INTERVAL 5 DAY), NOW());

-- 6. Danh sách các Module được thuật toán bốc vào lộ trình theo chuẩn sư phạm (LearningPathItems)
-- Đã khử trùng lặp và sắp xếp theo Module.sequence (2, 3, 5, 7, 10)
INSERT INTO learning_path_items (id, order_no, is_correct, progress_percent, reason, status, learning_path_id, module_id, ability_id, created_at, updated_at) VALUES
(1, 1, 0, 75, 'Bổ trợ phản hồi gián tiếp Part 2 do tỷ lệ đúng < 50%', 'IN_PROGRESS', 1, 2, 5, NOW(), NOW()),
(2, 2, 0, 0, 'Củng cố khả năng suy luận ngữ cảnh Part 3', 'LOCKED', 1, 3, 7, NOW(), NOW()),
(3, 3, 0, 0, 'Khắc phục lỗ hổng 12 thì và thể bị động Part 5', 'LOCKED', 1, 5, 11, NOW(), NOW()),
(4, 4, 0, 0, 'Rèn luyện kỹ năng tư duy điền câu văn bản Part 6', 'LOCKED', 1, 7, 13, NOW(), NOW()),
(5, 5, 0, 0, 'Bài thi vượt ải tổng ôn hoàn thành lộ trình', 'LOCKED', 1, 10, NULL, NOW(), NOW());

-- 7. Lượt làm bài thi vượt ải thử nghiệm của học viên Nam ở Module 2 (module_test_attempts)
INSERT INTO module_test_attempts (id, score, result, is_submitted, submitted_at, module_id, user_id, created_at, updated_at) VALUES
(1, 80, 'PASS', 1, DATE_SUB(NOW(), INTERVAL 1 DAY), 2, 3, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));

INSERT INTO module_test_answers (id, is_correct, is_flagged, user_answer, module_test_attempt_id, module_test_question_id, created_at, updated_at) VALUES
(1, 1, 0, '{"selected": "A"}', 1, 3, NOW(), NOW()),
(2, 1, 0, '{"selected": "B"}', 1, 4, NOW(), NOW()),
(3, 0, 0, '{"selected": "C"}', 1, 5, NOW(), NOW());

-- =====================================================================================
-- PHẦN 13: BẢNG QUY ĐỔI ĐIỂM TOEIC CHUẨN ETS (TOEIC_SCORE_CONVERSIONS)
-- Nạp đủ 101 mốc điểm (0 đến 100 câu đúng) cho cả Listening và Reading
-- =====================================================================================
INSERT INTO toeic_score_conversions (id, correct_count, listening_score, reading_score, created_at, updated_at) VALUES
(1, 0, 5, 5, NOW(), NOW()),
(2, 1, 9, 8, NOW(), NOW()),
(3, 2, 13, 11, NOW(), NOW()),
(4, 3, 17, 14, NOW(), NOW()),
(5, 4, 21, 17, NOW(), NOW()),
(6, 5, 25, 20, NOW(), NOW()),
(7, 6, 29, 23, NOW(), NOW()),
(8, 7, 33, 26, NOW(), NOW()),
(9, 8, 37, 29, NOW(), NOW()),
(10, 9, 41, 32, NOW(), NOW()),
(11, 10, 45, 35, NOW(), NOW()),
(12, 11, 50, 39, NOW(), NOW()),
(13, 12, 55, 43, NOW(), NOW()),
(14, 13, 60, 47, NOW(), NOW()),
(15, 14, 65, 51, NOW(), NOW()),
(16, 15, 70, 55, NOW(), NOW()),
(17, 16, 75, 59, NOW(), NOW()),
(18, 17, 80, 63, NOW(), NOW()),
(19, 18, 85, 67, NOW(), NOW()),
(20, 19, 90, 71, NOW(), NOW()),
(21, 20, 95, 75, NOW(), NOW()),
(22, 21, 100, 79, NOW(), NOW()),
(23, 22, 105, 83, NOW(), NOW()),
(24, 23, 110, 87, NOW(), NOW()),
(25, 24, 115, 91, NOW(), NOW()),
(26, 25, 120, 95, NOW(), NOW()),
(27, 26, 125, 100, NOW(), NOW()),
(28, 27, 130, 105, NOW(), NOW()),
(29, 28, 135, 110, NOW(), NOW()),
(30, 29, 140, 115, NOW(), NOW()),
(31, 30, 145, 120, NOW(), NOW()),
(32, 31, 150, 125, NOW(), NOW()),
(33, 32, 155, 130, NOW(), NOW()),
(34, 33, 160, 135, NOW(), NOW()),
(35, 34, 165, 140, NOW(), NOW()),
(36, 35, 170, 145, NOW(), NOW()),
(37, 36, 175, 150, NOW(), NOW()),
(38, 37, 180, 155, NOW(), NOW()),
(39, 38, 185, 160, NOW(), NOW()),
(40, 39, 190, 165, NOW(), NOW()),
(41, 40, 195, 170, NOW(), NOW()),
(42, 41, 200, 175, NOW(), NOW()),
(43, 42, 205, 180, NOW(), NOW()),
(44, 43, 210, 185, NOW(), NOW()),
(45, 44, 215, 190, NOW(), NOW()),
(46, 45, 220, 195, NOW(), NOW()),
(47, 46, 226, 200, NOW(), NOW()),
(48, 47, 232, 205, NOW(), NOW()),
(49, 48, 238, 210, NOW(), NOW()),
(50, 49, 244, 215, NOW(), NOW()),
(51, 50, 250, 220, NOW(), NOW()),
(52, 51, 256, 225, NOW(), NOW()),
(53, 52, 262, 230, NOW(), NOW()),
(54, 53, 268, 235, NOW(), NOW()),
(55, 54, 274, 240, NOW(), NOW()),
(56, 55, 280, 245, NOW(), NOW()),
(57, 56, 286, 250, NOW(), NOW()),
(58, 57, 292, 255, NOW(), NOW()),
(59, 58, 298, 260, NOW(), NOW()),
(60, 59, 304, 265, NOW(), NOW()),
(61, 60, 310, 270, NOW(), NOW()),
(62, 61, 316, 275, NOW(), NOW()),
(63, 62, 322, 280, NOW(), NOW()),
(64, 63, 328, 285, NOW(), NOW()),
(65, 64, 334, 290, NOW(), NOW()),
(66, 65, 340, 295, NOW(), NOW()),
(67, 66, 346, 300, NOW(), NOW()),
(68, 67, 352, 305, NOW(), NOW()),
(69, 68, 358, 310, NOW(), NOW()),
(70, 69, 364, 315, NOW(), NOW()),
(71, 70, 370, 320, NOW(), NOW()),
(72, 71, 375, 326, NOW(), NOW()),
(73, 72, 380, 332, NOW(), NOW()),
(74, 73, 385, 338, NOW(), NOW()),
(75, 74, 390, 344, NOW(), NOW()),
(76, 75, 395, 350, NOW(), NOW()),
(77, 76, 400, 356, NOW(), NOW()),
(78, 77, 405, 362, NOW(), NOW()),
(79, 78, 410, 368, NOW(), NOW()),
(80, 79, 415, 374, NOW(), NOW()),
(81, 80, 420, 380, NOW(), NOW()),
(82, 81, 425, 386, NOW(), NOW()),
(83, 82, 430, 392, NOW(), NOW()),
(84, 83, 435, 398, NOW(), NOW()),
(85, 84, 440, 404, NOW(), NOW()),
(86, 85, 445, 410, NOW(), NOW()),
(87, 86, 450, 416, NOW(), NOW()),
(88, 87, 455, 422, NOW(), NOW()),
(89, 88, 460, 428, NOW(), NOW()),
(90, 89, 465, 434, NOW(), NOW()),
(91, 90, 470, 440, NOW(), NOW()),
(92, 91, 473, 446, NOW(), NOW()),
(93, 92, 476, 452, NOW(), NOW()),
(94, 93, 479, 458, NOW(), NOW()),
(95, 94, 482, 464, NOW(), NOW()),
(96, 95, 485, 470, NOW(), NOW()),
(97, 96, 488, 476, NOW(), NOW()),
(98, 97, 495, 495, NOW(), NOW()),
(99, 98, 495, 495, NOW(), NOW()),
(100, 99, 495, 495, NOW(), NOW()),
(101, 100, 495, 495, NOW(), NOW());

-- =====================================================================================
-- HOÀN TẤT NẠP SEED DATA THÀNH CÔNG CHO 27 BẢNG DỮ LIỆU LEBY PLATFORM!
-- =====================================================================================
