# TODO 07: Chức Năng Phòng Học Bài Giảng & Luyện Tập Tức Thì (Lesson Room)

## 1. Mô Tả Chức Năng & Mục Tiêu Nghiệp Vụ
Không gian học tập tương tác cốt lõi của học viên trong từng bài học con:
* **Truy xuất nội dung đa phương tiện**: Cung cấp link video bài giảng (YouTube, S3/Cloud Storage), thời lượng, tóm tắt lý thuyết.
* **Luyện tập tương tác tức thì (Instant Practice Feedback)**: Cung cấp câu hỏi củng cố ngay sau video. Khi học viên chọn đáp án, Backend chấm điểm tức thì và trả về lời giải thích chi tiết AI phân tích từ loại, ngữ pháp, ngữ cảnh.
* **Đánh dấu hoàn thành bài học (Lesson Completion)**: Cập nhật tiến độ hoàn thành bài học, tính lại tỷ lệ phần trăm của module, mở khóa bài giảng kế tiếp hoặc kích hoạt điều kiện làm bài thi vượt ải module.

---

## 2. Hỗ Trợ Cho Giao Diện Người Dùng (Frontend UI Mapping)
* **Phòng Học Bài Giảng (`LessonRoomPage.tsx`)**: Đường dẫn `/modules/:moduleId/lessons/:lessonId`.
* **Thành phần UI**:
  * Trình phát video tùy biến `VideoPlayer.tsx`.
  * Khu vực đa Tab: Tab 1 Luyện tập nhanh (`PracticeQuestionView.tsx`), Tab 2 Ghi chú, Tab 3 Tài liệu đính kèm.
  * Hộp phản hồi AI hiển thị lời giải chi tiết khi bấm *"Kiểm tra"*.
  * Nút hành động: *"Hoàn thành bài giảng"* và nút cam nổi bật *"Làm bài kiểm tra vượt ải Module →"* (dẫn sang `/modules/:moduleId/test`).

---

## 3. Đặc Tả Giao Tiếp Dữ Liệu (API Contracts: Request & Response)

### 3.1. Lấy chi tiết bài học
* **Endpoint**: `GET /api/v1/modules/{moduleId}/lessons/{lessonId}`
* **Response (200 OK)**:
```json
{
  "success": true,
  "message": "Lấy nội dung bài học thành công",
  "data": {
    "id": 3,
    "moduleId": 2,
    "moduleTitle": "Part 5: Từ vựng & Ngữ pháp nâng cao",
    "title": "Bài 3: Collocations & Cụm từ thường gặp",
    "videoUrl": "https://www.w3schools.com/html/mov_bbb.mp4",
    "durationMinutes": 24,
    "summary": "Nắm vững các cặp từ thường xuyên đi cùng nhau trong các đề thi TOEIC ETS.",
    "isCompleted": false,
    "nextLessonId": 4,
    "practiceQuestion": {
      "questionId": 301,
      "title": "The marketing director suggested ______ a new campaign next month.",
      "options": [
        { "key": "A", "text": "launching" },
        { "key": "B", "text": "to launch" },
        { "key": "C", "text": "launch" },
        { "key": "D", "text": "launched" }
      ],
      "correctOption": "A",
      "explanation": "Động từ 'suggest' theo sau bởi V-ing khi không có mệnh đề that (suggest doing something). Do đó 'launching' là đáp án chính xác."
    }
  }
}
```

### 3.2. Nộp câu trả lời bài luyện tập nhanh
* **Endpoint**: `POST /api/v1/modules/{moduleId}/lessons/{lessonId}/practice`
* **Request Body**:
```json
{
  "questionId": 301,
  "selectedOption": "A"
}
```
* **Response (200 OK)**:
```json
{
  "success": true,
  "message": "Kiểm tra đáp án thành công",
  "data": {
    "isCorrect": true,
    "correctOption": "A",
    "explanation": "Động từ 'suggest' theo sau bởi V-ing khi không có mệnh đề that (suggest doing something). Do đó 'launching' là đáp án chính xác."
  }
}
```

### 3.3. Đánh dấu hoàn thành bài học
* **Endpoint**: `POST /api/v1/modules/{moduleId}/lessons/{lessonId}/complete`
* **Response (200 OK)**:
```json
{
  "success": true,
  "message": "Hoàn thành bài học thành công",
  "data": {
    "lessonId": 3,
    "moduleId": 2,
    "isModuleCompleted": false,
    "moduleProgressPercent": 75,
    "nextLessonId": 4
  }
}
```

---

## 4. Logic Nghiệp Vụ Backend Cần Xử Lý (Business Logic)
1. **Truy vấn đa hình Lesson (Inheritance Mapping)**:
   * Thực thể `Lesson` là abstract class với `@Inheritance(strategy = InheritanceType.JOINED)`.
   * Nếu là `VideoLesson` ➔ Đọc `uri` (link video) và `duration`.
   * Nếu là `PracticeLesson` ➔ Đọc `practice_questions` để lấy câu hỏi tương tác.
2. **Xử lý nộp câu hỏi luyện tập**:
   * Đọc `questions.correct_answer` để đối chiếu với `selectedOption`.
   * Trả về kết quả `isCorrect` cùng phần giải thích `explanation` trích xuất từ cột JSON của câu hỏi.
3. **Đánh dấu hoàn thành bài học**:
   * Cập nhật trạng thái bài học của học viên (bảng theo dõi bài học của user).
   * Tính lại tiến độ module: `progressPercent = (số bài đã học / tổng số bài) * 100`.
   * Nếu `progressPercent >= 80%` ➔ Cho phép mở khóa bài thi vượt ải module.

---

## 5. Bảng Cơ Sở Dữ Liệu Liên Quan
* **`lessons`**, **`video_lessons`**, **`practice_lessons`**: Đọc nội dung bài học.
* **`practice_questions`** & **`questions`**: Đọc đề và đáp án bài luyện tập.
* **`learning_path_items`**: Cập nhật tiến độ `progress` của module.

---

## 6. Danh Sách Công Việc Backend Cần Làm (Checklist)
- [ ] Tạo `LessonRepository`, `VideoLessonRepository`, `PracticeQuestionRepository`.
- [ ] Tạo DTO `LessonDetailResponse`, `PracticeAnswerRequest`, `PracticeAnswerResponse`.
- [ ] Viết `LessonService` & `LessonServiceImpl`.
- [ ] Thêm các endpoint vào `LessonController.java`.
