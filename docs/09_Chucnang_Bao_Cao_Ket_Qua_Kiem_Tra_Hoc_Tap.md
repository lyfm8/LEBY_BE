# TODO 09: Chức Năng Báo Cáo Kết Quả Kiểm Tra Học Tập (Module Test Results Report)

## 1. Mô Tả Chức Năng & Mục Tiêu Nghiệp Vụ
Bám sát 100% hình ảnh thiết kế tham khảo `module-test-results.png`:
* **Báo cáo kết quả học tập trực quan**: Hiển thị điểm số cực đại (**78 /100**), huy hiệu **HOÀN THÀNH (PASS)**, dòng so sánh ngưỡng tối thiểu (65 điểm) và mức vượt trội (+13 điểm).
* **Nhận diện & Lời khuyên AI (AI Adaptive Advice)**: Tự động phân tích điểm mạnh đã khắc phục (mảng từ loại) và nhắc nhở ôn thêm phần còn thiếu (trạng từ chỉ mức độ).
* **3 Thẻ phân tích năng lực chi tiết**:
  1. TỶ LỆ CHÍNH XÁC: 78% (Đúng 19/25 câu hỏi).
  2. THỜI GIAN LÀM BÀI: 09:15 (Nhanh hơn 35% học viên khác).
  3. KHỐI KIẾN THỨC MỤC TIÊU: ĐẠT CHUẨN (Đã bù đắp 80% lỗ hổng).
* **Bảng lịch sử nỗ lực luyện tập**: Thể hiện sự tiến bộ qua các lần thi (Lần 1: 52 điểm - FAIL ➔ Lần 2: 78 điểm - PASS).
* **Phúc khảo đáp án chi tiết**: Cung cấp API xem lại từng câu hỏi, đáp án đã chọn, đáp án đúng và giải thích chuyên sâu.

---

## 2. Hỗ Trợ Cho Giao Diện Người Dùng (Frontend UI Mapping)
* **Trang Báo Cáo Kết Quả Kiểm Tra (`ModuleTestResultPage.tsx`)**: Đường dẫn `/results` hoặc `/modules/:moduleId/test/result/:attemptId`.
* **Trang Phúc Khảo Chi Tiết (`ModuleTestReviewPage.tsx`)**: Đường dẫn `/modules/:moduleId/test/review/:attemptId`.
* **Thành phần UI**:
  * Sử dụng khung giao diện thống nhất `SidebarNav.tsx` (Menu "Kết quả" active).
  * Thẻ điểm lớn `ResultScoreCard.tsx` kèm khung nhận diện AI.
  * Lưới 3 thẻ năng lực `ResultMetricsGrid.tsx`.
  * Bảng các lần thi `ResultHistoryTable.tsx`.
  * Nút CTA: *"Tiếp tục lộ trình học"* (sang `/learning-path`), *"Làm lại bài kiểm tra"* và *"Xem lại đáp án chi tiết & giải thích"*.

---

## 3. Đặc Tả Giao Tiếp Dữ Liệu (API Contracts: Request & Response)

### 3.1. Lấy báo cáo kết quả kiểm tra học tập
* **Endpoint**: `GET /api/v1/modules/{moduleId}/test/results/{attemptId}` hoặc `GET /api/v1/learning-results/latest`
* **Response (200 OK)**:
```json
{
  "success": true,
  "message": "Lấy báo cáo kết quả kiểm tra thành công",
  "data": {
    "attemptId": 901,
    "moduleId": 2,
    "moduleTitle": "Part 5 — Vocabulary",
    "completedAt": "14:32 - 12/10/2024",
    "score": 78,
    "maxScore": 100,
    "passScore": 65,
    "isPassed": true,
    "diffFromPass": 13,
    "aiFeedback": {
      "title": "Chúc mừng! Bạn đã hoàn thành xuất sắc và mở khóa Module tiếp theo.",
      "content": "AI nhận diện bạn đã khắc phục tốt mảng từ loại (Part 5), tuy nhiên nên chú ý ôn thêm về trạng từ chỉ mức độ ở Module 2."
    },
    "metrics": {
      "accuracyRate": 78,
      "correctAnswers": 19,
      "totalQuestions": 25,
      "durationFormatted": "09:15",
      "speedComparison": "Nhanh hơn 35% học viên khác",
      "competencyStatus": "ĐẠT CHUẨN",
      "gapResolvedPercent": 80
    },
    "history": [
      {
        "attemptNumber": 2,
        "isLatest": true,
        "completedAt": "12/10/2024",
        "score": 78,
        "maxScore": 100,
        "isPassed": true,
        "durationFormatted": "09:15"
      },
      {
        "attemptNumber": 1,
        "isLatest": false,
        "completedAt": "10/10/2024",
        "score": 52,
        "maxScore": 100,
        "isPassed": false,
        "durationFormatted": "14:20"
      }
    ],
    "nextModuleId": 3
  }
}
```

### 3.2. Lấy chi tiết phúc khảo đáp án từng câu
* **Endpoint**: `GET /api/v1/modules/{moduleId}/test/review/{attemptId}`
* **Response (200 OK)**:
```json
{
  "success": true,
  "message": "Lấy chi tiết bài làm thành công",
  "data": {
    "attemptId": 901,
    "moduleId": 2,
    "moduleTitle": "Kiểm tra Module: Part 5 — Vocabulary",
    "score": 78,
    "passScore": 65,
    "isPassed": true,
    "correctCount": 19,
    "totalQuestions": 25,
    "completedAt": "2026-09-12T14:32:00Z",
    "nextModuleId": 3,
    "detailedAnswers": [
      {
        "questionId": 8,
        "orderNo": 8,
        "questionTitle": "Question 8: The supervisor requested a ______ analysis of the marketing budget before submitting the proposal to the board of directors.",
        "selectedOption": "A",
        "correctOption": "A",
        "isCorrect": true,
        "explanation": "Trước danh từ 'analysis' cần một tính từ bổ nghĩa. 'comprehensive' (toàn diện, sâu rộng) là tính từ chính xác. Các phương án khác: comprehend (v), comprehensively (adv), comprehensiveness (n).",
        "options": [
          { "key": "A", "text": "The supervisor requested a comprehensive analysis of the marketing budget." },
          { "key": "B", "text": "The supervisor requested a comprehend analysis of the marketing budget." },
          { "key": "C", "text": "The supervisor requested a comprehensively analysis of the marketing budget." },
          { "key": "D", "text": "The supervisor requested a comprehensiveness analysis of the marketing budget." }
        ]
      }
    ]
  }
}
```

---

## 4. Logic Nghiệp Vụ Backend Cần Xử Lý (Business Logic)
1. **Tổng hợp dữ liệu Performance Report**:
   * Truy vấn `module_test_attempts` theo `attemptId`.
   * Tính `diffFromPass = score - passScore`.
   * Định dạng thời gian làm bài: `mm:ss` (ví dụ `555s` ➔ `"09:15"`).
   * Lấy lịch sử tất cả các lần thi của học viên ở module này:
     `SELECT * FROM module_test_attempts WHERE user_id = ? AND module_id = ? ORDER BY attempt_number DESC`.
2. **Sinh nội dung AI Feedback thích ứng**:
   * Nếu `isPassed = true`: Khen ngợi và chỉ ra năng lực còn sót lại cần chú ý ở module tiếp theo.
   * Nếu `isPassed = false`: Động viên và chỉ ra nhóm câu sai nhiều nhất để ôn tập.

---

## 5. Bảng Cơ Sở Dữ Liệu Liên Quan
* **`module_test_attempts`**: Đọc điểm, thời gian, số lần thi.
* **`module_test_answers`**: Đọc đáp án chọn, so khớp đúng sai.
* **`modules`**: Đọc tên module, ngưỡng pass_score.
* **`questions`**: Đọc đề câu hỏi, 4 đáp án và lời giải thích chi tiết.

---

## 6. Danh Sách Công Việc Backend Cần Làm (Checklist)
- [ ] Tạo DTO `ModuleTestPerformanceReportResponse`, `AttemptHistoryItemDto`, `MetricSummaryDto`.
- [ ] Tạo DTO `ModuleTestReviewResponse`, `DetailedAnswerReviewDto`.
- [ ] Viết hàm `getPerformanceReport(attemptId)` và `getTestReview(attemptId)` trong `ModuleTestService`.
- [ ] Bổ sung các endpoint vào `ModuleTestController.java`.
