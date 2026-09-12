# TODO 03: Chức Năng Phòng Thi Chẩn Đoán Toàn Diện & Chấm Điểm Ban Đầu

## 1. Mô Tả Chức Năng & Mục Tiêu Nghiệp Vụ
Đây là "trái tim" của hệ thống học tập thích ứng LEBY:
* **Gộp 1 tầng toàn diện (Comprehensive Diagnostic Test)**: Thay vì bắt học viên làm 2 tầng test rời rạc, hệ thống cung cấp 1 bài kiểm tra chẩn đoán toàn diện duy nhất (gồm khoảng 35 - 45 câu hỏi chất lượng cao hoặc đề tổng hợp 100 câu) bao phủ trọn vẹn cả **7 Part** và gắn nhãn các **Abilities hạt nhân**.
* **Đầy đủ đa phương tiện (Multimedia)**: Cung cấp file nghe MP3 (Part 1 - 4), hình ảnh tranh chụp, đoạn văn đọc hiểu (Part 5 - 7).
* **Bộ máy chấm điểm thích ứng (Adaptive Scoring Engine)**:
  1. Chấm điểm từng câu hỏi dựa trên đáp án đúng.
  2. So sánh điểm số từng Part với bảng ngưỡng `target_part_thresholds` của mốc AIM học viên đã chọn để sinh ra chỉ thị điều hướng: `PASS`, `CONFIRM`, `WEAK`, `FULL_PART`.
  3. Dựa trên bảng trung gian `question_abilities`, tính tỷ lệ chính xác cho từng kỹ năng `abilities`.
  4. Áp dụng quy tắc `ability_evaluation_rules` để tự động phân loại kỹ năng của học viên thành `STABLE`, `DEVELOPING`, hoặc `WEAK`.
  5. Tự động sinh danh sách các kỹ năng bị `WEAK` vào bảng `ability_aim_plan_items` để làm nguyên liệu sinh lộ trình học thích ứng sau này.

---

## 2. Hỗ Trợ Cho Giao Diện Người Dùng (Frontend UI Mapping)
* **Phòng Thi Chẩn Đoán (`DiagnosticRoomPage.tsx`)**: Đường dẫn `/diagnostic/room`.
* **Thành phần UI**:
  * Trình phát âm thanh `AudioPlayer.tsx` (Part 1 - Part 4).
  * Thẻ câu hỏi `QuestionCard.tsx` (hiển thị đề bài, hình ảnh, 4 lựa chọn A, B, C, D).
  * Bản đồ câu hỏi `QuestionPalette.tsx` (lưới câu hỏi theo dõi trạng thái: đã làm, chưa làm, đánh dấu cờ).
  * Đồng hồ đếm lùi thời gian thực (45 phút).
  * Hộp thoại xác nhận nộp bài `SubmitConfirmModal.tsx`.

---

## 3. Đặc Tả Giao Tiếp Dữ Liệu (API Contracts: Request & Response)

### 3.1. Lấy đề thi chẩn đoán toàn diện
* **Endpoint**: `GET /api/v1/diagnostic/comprehensive-test`
* **Yêu cầu xác thực**: Bắt buộc đăng nhập.
* **Backend gửi xuống (Response 200 OK)**:
```json
{
  "success": true,
  "message": "Lấy đề thi chẩn đoán thành công",
  "data": {
    "testId": 1,
    "attemptId": 501,
    "title": "Bài đánh giá tổng hợp — Chẩn đoán toàn diện",
    "description": "Bài kiểm tra giúp xác định chính xác năng lực TOEIC ban đầu của bạn.",
    "durationMinutes": 45,
    "totalQuestions": 100,
    "questions": [
      {
        "questionId": 15,
        "orderNo": 15,
        "partId": 1,
        "partNo": 1,
        "partName": "Photographs",
        "section": "LISTENING",
        "title": "Câu hỏi 15: Look at the picture and select the best statement that describes what you see.",
        "audioUrl": "https://assets.leby.edu.vn/audio/q15.mp3",
        "imageUrl": "https://images.unsplash.com/photo-1556761175-5973dc0f32e7?w=1000",
        "passage": null,
        "options": [
          { "key": "A", "text": "The team is having a presentation in the hall." },
          { "key": "B", "text": "Some documents are being printed by the assistant." },
          { "key": "C", "text": "They are discussing some plans around a monitor." },
          { "key": "D", "text": "Most desks are empty at the moment." }
        ]
      }
    ]
  }
}
```

### 3.2. Nộp bài làm chẩn đoán & Kích hoạt chấm điểm
* **Endpoint**: `POST /api/v1/diagnostic/submit`
* **Request Body**:
```json
{
  "attemptId": 501,
  "durationSeconds": 2100,
  "answers": [
    { "questionId": 15, "selectedOption": "C", "isFlagged": false },
    { "questionId": 16, "selectedOption": "A", "isFlagged": true }
  ]
}
```
* **Response (200 OK)**:
```json
{
  "success": true,
  "message": "Chấm bài chẩn đoán thành công",
  "data": {
    "attemptId": 501,
    "redirectUrl": "/diagnostic/results/501"
  }
}
```

---

## 4. Logic Nghiệp Vụ Backend Cần Xử Lý (Business Logic)

### Bước 1: Khởi tạo lần thi (`GET /comprehensive-test`)
1. Lấy đề thi chẩn đoán có `test_type = 'COMPREHENSIVE'` và `status = true` từ `diagnostic_tests`.
2. Tạo ngay một bản ghi mới trong bảng `diagnostic_attempts`:
   * `user_id = currentUserId`, `diagnostic_test_id = test.getId()`, `started_at = NOW()`.
3. Lấy danh sách câu hỏi kèm theo từ `diagnostic_test_questions` join với `questions`:
   * Trích xuất `questionData` (JSON chứa media, options) để ánh xạ sang DTO trả về cho Front-End.
   * **Bảo mật tuyệt đối**: Tuyệt đối KHÔNG gửi trường `correct_answer` về cho Front-End ở bước này.

### Bước 2: Xử lý nộp bài & Bộ máy chấm điểm (`POST /submit`)
Bọc toàn bộ trong `@Transactional`:
1. **Tính điểm từng Part**:
   * Duyệt qua từng câu trả lời trong `request.answers`.
   * So khớp với `questions.correct_answer`: Nếu trùng khớp ➔ Tính điểm câu.
   * Gom điểm theo từng `part_id` (từ Part 1 đến Part 7).
   * Lấy `target_profile_id` từ `ability_aim_plans` của user để đọc ngưỡng trong `target_part_thresholds`:
     * Điểm >= `pass_threshold` ➔ Gán nhãn `directive = 'PASS'`.
     * Điểm >= `confirm_threshold` ➔ Gán nhãn `directive = 'CONFIRM'`.
     * Điểm thấp ➔ Gán nhãn `directive = 'WEAK'`.
     * Điểm quá thấp hoặc phần đọc dài Part 7 ➔ Gán nhãn `directive = 'FULL_PART'`.
   * Lưu 7 bản ghi vào bảng `part_diagnostic_results`.

2. **Chấm điểm Năng lực hạt nhân (Abilities)**:
   * Truy vấn bảng trung gian `question_abilities`: Biết từng câu hỏi đo lường những Ability nào.
   * Với mỗi Ability, cộng dồn `totalQuestions` và `correctQuestions` của các câu có liên kết.
   * Tính `accuracy_rate = correctCount / totalCount`.
   * Đọc quy tắc từ `ability_evaluation_rules`:
     * Nếu `accuracy_rate >= stable_min` ➔ `status = 'STABLE'`.
     * Nếu `accuracy_rate <= weak_max` ➔ `status = 'WEAK'`.
     * Còn lại ➔ `status = 'DEVELOPING'`.
   * Lưu hoặc cập nhật vào bảng `user_abilities` (lưu `accuracy_rate`, `evidence_count`, `status`).

3. **Tạo Backlog Lộ trình (`ability_aim_plan_items`)**:
   * Lọc toàn bộ các Ability bị xếp loại `WEAK` hoặc `DEVELOPING`.
   * Thêm vào bảng `ability_aim_plan_items` gắn với `plan_id` hiện tại của học viên để chuẩn bị nguyên liệu cho việc sinh lộ trình học thích ứng sau này.
4. Cập nhật `diagnostic_attempts.completed_at = NOW()`, `score = tổng điểm dự đoán`.

---

## 5. Bảng Cơ Sở Dữ Liệu Liên Quan
* **`diagnostic_tests`**: Đọc đề thi.
* **`diagnostic_test_questions`**: Đọc danh sách câu hỏi trong đề thi.
* **`questions`**: Đọc nội dung câu hỏi, media và đáp án đúng.
* **`question_abilities`**: Đọc ánh xạ câu hỏi với kỹ năng.
* **`target_part_thresholds`**: Đọc ngưỡng so sánh 7 Part theo mục tiêu AIM.
* **`ability_evaluation_rules`**: Đọc quy tắc phân loại `WEAK` / `STABLE`.
* **`diagnostic_attempts`**: Ghi bản ghi phiên thi (`started_at`, `completed_at`, `score`).
* **`part_diagnostic_results`**: Ghi kết quả điểm và chỉ thị cho 7 Part.
* **`user_abilities`**: Ghi hồ sơ năng lực của học viên.
* **`ability_aim_plan_items`**: Ghi các kỹ năng bị yếu cần đào tạo.

---

## 6. Các Lưu Ý Quan Trọng
* **Cột JSON trong Postgres**: Thực thể `Question` dùng trường `questionData` và `correctAnswer` dạng JSON. Sử dụng `JsonNode` của Jackson để bóc tách trường linh hoạt.
* **Không làm gián đoạn bài làm**: Nếu học viên bị rớt mạng và nộp lại cùng 1 `attemptId`, kiểm tra nếu `completed_at != null` thì trả về kết quả đã chấm trước đó, không chấm lại hai lần.

---

## 7. Danh Sách Công Việc Backend Cần Làm (Checklist)
- [ ] Tạo `DiagnosticTestRepository`, `DiagnosticAttemptRepository`, `PartDiagnosticResultRepository`.
- [ ] Tạo DTO Request: `SubmitDiagnosticRequest`, `DiagnosticAnswerItem`.
- [ ] Tạo DTO Response: `ComprehensiveTestResponse`, `DiagnosticQuestionDto`.
- [ ] Viết `DiagnosticScoringService` chịu trách nhiệm chấm điểm, ánh xạ ngưỡng Part, tính Ability và sinh Plan Items.
- [ ] Viết `DiagnosticController.java` tại `com.example.be.features.diagnostic.controller` với 2 endpoint: `GET /comprehensive-test` và `POST /submit`.
