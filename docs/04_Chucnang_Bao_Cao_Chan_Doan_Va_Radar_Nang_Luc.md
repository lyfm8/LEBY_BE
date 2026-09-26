# TODO 04: Chức Năng Báo Cáo Chẩn Đoán Toàn Diện & Radar Năng Lực (Diagnostic Results)

## 1. Mô Tả Chức Năng & Mục Tiêu Nghiệp Vụ
Sau khi nộp bài thi chẩn đoán toàn diện ở bước 3, Backend tổng hợp và cung cấp bức tranh toàn cảnh về trình độ tiếng Anh của học viên:
* **Tính toán điểm TOEIC dự đoán ban đầu**: Ước tính mức điểm tương đương (thang điểm 990) dựa trên phân bổ điểm Listening (Part 1-4) và Reading (Part 5-7).
* **Chi tiết năng lực 7 Phần thi**: Liệt kê điểm số, chỉ thị điều hướng (`PASS`, `CONFIRM`, `WEAK`, `FULL_PART`) và lời khuyên nhận diện thông minh cho từng Part.
* **Biểu đồ đa giác Radar ma trận năng lực (Abilities Radar Matrix)**: Cung cấp điểm số và trạng thái (`STABLE`, `DEVELOPING`, `WEAK`) của 6 nhóm năng lực hạt nhân để Front-End vẽ biểu đồ SVG Radar phát sáng.
* **Tóm tắt lộ trình thích ứng đề xuất**: Đưa ra nhận định AI về rào cản chính và thời gian ước tính hoàn thành lộ trình (ví dụ: 48 ngày).

---

## 2. Hỗ Trợ Cho Giao Diện Người Dùng (Frontend UI Mapping)
* **Trang Báo Cáo Kết Quả Toàn Diện (`DiagnosticResultPage.tsx`)**: Đường dẫn `/diagnostic/results/:attemptId` (hoặc `/diagnostic/results`).
* **Thành phần UI**:
  * Banner Hero tối màu `#0b1120`: Thẻ nổi bật *"ĐIỂM DỰ ĐOÁN BAN ĐẦU: 550"*.
  * Cột trái: 7 Card kết quả từng Part (`PartResultCard.tsx`) với huy hiệu màu sắc tương ứng (`PASS` - xanh, `CONFIRM` - vàng, `WEAK` - đỏ).
  * Cột phải: Biểu đồ SVG Radar đa giác (`RadarChart.tsx`), Khung nhận định AI và Thẻ *"Lộ trình học đã sẵn sàng!"* kèm nút CTA cam *"Bắt đầu lộ trình học thích ứng"* (chuyển sang `/dashboard`).

---

## 3. Đặc Tả Giao Tiếp Dữ Liệu (API Contracts: Request & Response)

### 3.1. Lấy báo cáo chi tiết kết quả thi chẩn đoán
* **Endpoint**: `GET /api/v1/diagnostic/results/{attemptId}`
* **Headers**: `Accept: application/json`
* **Response (200 OK)**:
```json
{
  "success": true,
  "message": "Lấy kết quả chẩn đoán thành công",
  "data": {
    "attemptId": 501,
    "testTitle": "Bài đánh giá tổng hợp — Chẩn đoán toàn diện",
    "completedAt": "2026-09-12T09:15:00Z",
    "predictedScore": 550,
    "targetScore": 650,
    "totalQuestions": 100,
    "correctAnswers": 58,
    "sectionScores": {
      "listening": 305,
      "reading": 245,
      "listeningPercent": 45,
      "readingPercent": 61
    },
    "partResults": [
      {
        "partNo": 1,
        "name": "Part 1: Photographs",
        "score": 90,
        "maxScore": 100,
        "directive": "PASS",
        "directiveLabel": "PASS",
        "aiFeedback": "Kỹ năng nghe miêu tả tranh xuất sắc, tiếp tục duy trì phát huy."
      },
      {
        "partNo": 2,
        "name": "Part 2: Question-Response",
        "score": 75,
        "maxScore": 100,
        "directive": "PASS",
        "directiveLabel": "PASS",
        "aiFeedback": "Khá ổn, cần lưu ý thêm một số bẫy thông tin đồng âm."
      },
      {
        "partNo": 3,
        "name": "Part 3: Conversations",
        "score": 55,
        "maxScore": 100,
        "directive": "CONFIRM",
        "directiveLabel": "CONFIRM",
        "aiFeedback": "Khả năng bắt keyword còn chậm. Cần cải thiện chiến thuật đọc đề trước."
      },
      {
        "partNo": 4,
        "name": "Part 4: Short Talks",
        "score": 35,
        "maxScore": 100,
        "directive": "WEAK",
        "directiveLabel": "WEAK",
        "aiFeedback": "Từ vựng độc thoại còn mỏng. Hệ thống sẽ tối ưu hóa lộ trình nghe cho bạn."
      },
      {
        "partNo": 5,
        "name": "Part 5: Incomplete Sentences",
        "score": 80,
        "maxScore": 100,
        "directive": "PASS",
        "directiveLabel": "PASS",
        "aiFeedback": "Ngữ pháp nền tảng tốt. Tập trung tối ưu thời gian làm bài dưới 15 giây/câu."
      },
      {
        "partNo": 6,
        "name": "Part 6: Text Completion",
        "score": 62,
        "maxScore": 100,
        "directive": "CONFIRM",
        "directiveLabel": "CONFIRM",
        "aiFeedback": "Cần cải thiện kỹ năng đọc hiểu văn cảnh và liên kết từ loại."
      },
      {
        "partNo": 7,
        "name": "Part 7: Reading Comprehension",
        "score": 40,
        "maxScore": 100,
        "directive": "FULL_PART",
        "directiveLabel": "FULL PART",
        "aiFeedback": "Yếu nhất phần đọc hiểu dài. Cần tăng tốc độ đọc quét (scanning) & từ vựng."
      }
    ],
    "abilitiesRadar": [
      { "code": "P1_P2_BASIC", "name": "Nghe miêu tả tranh & hỏi đáp", "score": 85, "status": "STABLE" },
      { "code": "P3_P4_DETAIL", "name": "Nghe thông tin chi tiết", "score": 42, "status": "WEAK" },
      { "code": "P5_GRAMMAR", "name": "Ngữ pháp & Cấu trúc", "score": 80, "status": "STABLE" },
      { "code": "P5_P6_VOCAB", "name": "Từ vựng ngữ cảnh", "score": 58, "status": "DEVELOPING" },
      { "code": "P7_SCANNING", "name": "Đọc quét & Bắt ý chính", "score": 38, "status": "WEAK" },
      { "code": "P7_INFERENCE", "name": "Đọc hiểu suy luận logic", "score": 45, "status": "WEAK" }
    ],
    "aiRoadmapSummary": {
      "radarInsight": "Phân tích AI cho thấy khối lượng kỹ năng nghe hiểu nhóm (Part 3-4) đang là rào cản chính hạn chế điểm mục tiêu của bạn.",
      "roadmapDays": 48,
      "focusParts": [4, 7],
      "recommendedAction": "Bắt đầu lộ trình học thích ứng",
      "secondaryAction": "Làm bài đánh giá chuyên sâu Part 7"
    }
  }
}
```

---

## 4. Logic Nghiệp Vụ Backend Cần Xử Lý (Business Logic)
1. **Kiểm tra quyền sở hữu**:
   * Kiểm tra `attemptId` có thuộc về `currentUserId` đang đăng nhập không (chống IDOR). Nếu không khớp ➔ Ném `403 Forbidden`.
2. **Tổng hợp dữ liệu báo cáo**:
   * Đọc `diagnostic_attempts`: Lấy ngày giờ hoàn thành, điểm số dự đoán tổng thể.
   * Đọc `part_diagnostic_results`: Lấy điểm của 7 Part và directive tương ứng. Dựa trên directive để sinh feedback tự động phù hợp.
   * Đọc `user_abilities` join với `abilities`: Lấy danh sách 6 nhóm kỹ năng chính hiển thị trên Radar Chart.
   * Đọc `ability_aim_plans` join với `target_profiles`: Lấy `target_total_score` (ví dụ AIM 650).
3. **Sinh nhận định AI tổng quan (Summary Insight)**:
   * Tìm các Ability có tỷ lệ chính xác thấp nhất hoặc Part bị xếp `WEAK`/`FULL_PART` để tạo câu nhận xét: *"Phân tích cho thấy khối lượng kỹ năng nghe hiểu nhóm (Part 3-4) đang là rào cản chính..."*.
   * Tính toán số ngày ước tính dựa trên số lượng Ability bị yếu (mỗi Ability tương đương 8 - 12 ngày học).

---

## 5. Bảng Cơ Sở Dữ Liệu Liên Quan
* **`diagnostic_attempts`**: Đọc `score`, `completed_at`, `user_id`.
* **`part_diagnostic_results`**: Đọc `score`, `directive`, `part_id`.
* **`parts`**: Đọc tên Part (`Part 1: Photographs`...).
* **`user_abilities`**: Đọc `accuracy_rate`, `status`.
* **`abilities`**: Đọc `code`, `name`.
* **`ability_aim_plans`** & **`target_profiles`**: Đọc mốc điểm AIM mục tiêu.

---

## 6. Danh Sách Công Việc Backend Cần Làm (Checklist)
- [ ] Tạo DTO Response: `DiagnosticResultReportResponse`, `PartResultDto`, `AbilityRadarDto`, `AiRoadmapSummaryDto`.
- [ ] Viết `getDiagnosticResult(Long attemptId)` trong `DiagnosticService`.
- [ ] Thêm endpoint `GET /api/v1/diagnostic/results/{attemptId}` trong `DiagnosticController`.
