# TODO 08: Chức Năng Phòng Thi Vượt Ải Module Test (Module Test Room)

## 1. Mô Tả Chức Năng & Mục Tiêu Nghiệp Vụ
Chốt chặn kiểm soát chất lượng đào tạo của từng Module:
* **Cung cấp đề thi vượt ải 25 câu tính giờ**: Đề kiểm tra tập trung đánh giá đúng những kỹ năng mà module đó chịu trách nhiệm rèn luyện (thời gian làm bài: 25 phút).
* **Bộ máy chấm thi chốt chặn (Passing Gate Engine)**:
  * Điểm chuẩn qua ải được đọc từ `modules.pass_score` hoặc `module_target_thresholds` (mặc định 65/100 điểm).
  * Nếu điểm >= 65: Học viên được công nhận **VƯỢT ẢI THÀNH CÔNG (PASS)**!
    * Tự động cập nhật `learning_path_items` của module này sang `COMPLETED`.
    * Tự động mở khóa module tiếp theo trong lộ trình (`status = 'IN_PROGRESS'`).
    * Tự động nâng cấp trạng thái kỹ năng trong `user_abilities` từ `WEAK` lên `DEVELOPING` hoặc `STABLE`.
  * Nếu điểm < 65: Học viên bị xếp loại `FAIL` ➔ Khuyến khích ôn tập lại hoặc làm lại bài kiểm tra.

---

## 2. Hỗ Trợ Cho Giao Diện Người Dùng (Frontend UI Mapping)
* **Phòng Thi Vượt Ải Module (`ModuleTestRoomPage.tsx`)**: Đường dẫn `/modules/:moduleId/test`.
* **Thành phần UI**:
  * Header thi `ModuleTestHeader.tsx`: Tiêu đề bài thi, bộ đếm ngược 25 phút, nút *"Nộp bài thi"*.
  * Thẻ câu hỏi trắc nghiệm `ModuleQuestionCard.tsx`.
  * Bản đồ câu hỏi `ModuleQuestionPalette.tsx` (lưới 25 ô câu hỏi).
  * Modal cảnh báo câu chưa làm trước khi nộp bài.

---

## 3. Đặc Tả Giao Tiếp Dữ Liệu (API Contracts: Request & Response)

### 3.1. Lấy đề thi vượt ải module
* **Endpoint**: `GET /api/v1/modules/{moduleId}/test`
* **Response (200 OK)**:
```json
{
  "success": true,
  "message": "Lấy đề thi vượt ải thành công",
  "data": {
    "moduleId": 2,
    "moduleTitle": "Kiểm tra Module: Part 5 — Vocabulary",
    "topic": "Từ loại & Cụm từ cố định",
    "currentAIM": 650,
    "passScore": 65,
    "durationMinutes": 25,
    "totalQuestions": 25,
    "questions": [
      {
        "questionId": 1,
        "orderNo": 1,
        "partNo": 5,
        "title": "The accounting team needs to verify all financial statements ______ before filing.",
        "options": [
          { "key": "A", "text": "careful" },
          { "key": "B", "text": "carefully" },
          { "key": "C", "text": "caring" },
          { "key": "D", "text": "carefulness" }
        ]
      }
    ]
  }
}
```

### 3.2. Nộp bài thi vượt ải
* **Endpoint**: `POST /api/v1/modules/{moduleId}/test/submit`
* **Request Body**:
```json
{
  "moduleId": 2,
  "durationSeconds": 960,
  "answers": [
    { "questionId": 1, "selectedOption": "B" },
    { "questionId": 8, "selectedOption": "A" }
  ]
}
```
* **Response (200 OK)**:
```json
{
  "success": true,
  "message": "Chấm bài thi vượt ải thành công",
  "data": {
    "attemptId": 901,
    "score": 78,
    "passScore": 65,
    "isPassed": true,
    "redirectUrl": "/modules/2/test/result/901"
  }
}
```

---

## 4. Logic Nghiệp Vụ Backend Cần Xử Lý (Business Logic)
Bọc toàn bộ trong `@Transactional`:
1. **Chấm điểm bài thi**:
   * Duyệt qua 25 câu hỏi, so khớp `selectedOption` với `questions.correct_answer`.
   * Tính số câu đúng `correctCount`, quy đổi ra thang điểm 100: `score = Math.round((correctCount / 25) * 100)`.
   * So sánh với `passScore`: `isPassed = score >= passScore`.
2. **Lưu lịch sử thi**:
   * Đếm số lần thi trước đó của học viên ở module này để gán `attempt_number = count + 1`.
   * Tạo bản ghi mới trong `module_test_attempts` (`score`, `is_passed`, `duration_seconds`, `attempt_number`, `completed_at = NOW()`).
   * Lưu chi tiết từng câu vào `module_test_answers` (`is_correct`, `user_answer`).
3. **Mở khóa chặng học tiếp theo (Nếu `isPassed = true`)**:
   * Cập nhật `learning_path_items` của module hiện tại: `status = 'COMPLETED'`.
   * Tìm `learning_path_items` của module kế tiếp (`order_no = current.order_no + 1`): Cập nhật `status = 'IN_PROGRESS'`.
   * Cập nhật năng lực học viên trong `user_abilities`: Tăng `evidence_count`, tính lại `accuracy_rate`, chuyển trạng thái `status = 'STABLE'` nếu đủ điều kiện.

---

## 5. Bảng Cơ Sở Dữ Liệu Liên Quan
* **`module_test_questions`** & **`questions`**: Đọc đề và đáp án đúng.
* **`module_test_attempts`**: Ghi điểm, trạng thái qua ải, thời gian làm bài.
* **`module_test_answers`**: Ghi chi tiết câu trả lời của học viên.
* **`learning_path_items`**: Cập nhật trạng thái module hiện tại và mở khóa module sau.
* **`user_abilities`**: Cập nhật nâng hạng năng lực.

---

## 6. Danh Sách Công Việc Backend Cần Làm (Checklist)
- [ ] Tạo `ModuleTestAttemptRepository`, `ModuleTestAnswerRepository`.
- [ ] Tạo DTO Request: `SubmitModuleTestRequest`.
- [ ] Tạo DTO Response: `ModuleTestDataResponse`, `ModuleTestSubmitResponse`.
- [ ] Viết `ModuleTestService` & `ModuleTestServiceImpl` xử lý thuật toán chấm điểm và mở khóa module kế tiếp.
- [ ] Tạo `ModuleTestController.java` với 2 endpoint: `GET /test` và `POST /test/submit`.
