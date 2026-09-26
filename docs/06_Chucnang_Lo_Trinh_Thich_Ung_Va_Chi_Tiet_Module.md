# TODO 06: Chức Năng Lộ Trình Thích Ứng & Chi Tiết Module (Adaptive Roadmap)

## 1. Mô Tả Chức Năng & Mục Tiêu Nghiệp Vụ
Phân hệ biến kết quả chẩn đoán thành hành động học tập cụ thể:
* **Sinh lộ trình thích ứng cá nhân hóa (Adaptive Roadmap Generation)**: Tự động ghép nối các `modules` tương ứng với những kỹ năng bị `WEAK` trong bảng `ability_aim_plan_items` thành một chuỗi học tập có thứ tự logic.
* **Quản lý trạng thái từng chặng (Module Status Lifecycle)**:
  * `COMPLETED`: Đã học xong và vượt qua bài thi Module Test.
  * `IN_PROGRESS`: Module hiện tại đang học.
  * `LOCKED`: Module chưa mở khóa (chỉ mở khi vượt qua module trước).
* **Cung cấp chi tiết Module**: Cung cấp danh sách các bài học video/luyện tập và trạng thái bài thi vượt ải module.

---

## 2. Hỗ Trợ Cho Giao Diện Người Dùng (Frontend UI Mapping)
* **Trang Lộ Trình Học Thích Ứng (`LearningPathPage.tsx`)**: Đường dẫn `/learning-path`.
  * Trục timeline dọc trực quan (`ModuleTimelineItem.tsx`) kết nối các module với trạng thái và huy hiệu năng lực (`abilities tags`).
* **Trang Chi Tiết Module (`ModuleDetailPage.tsx`)**: Đường dẫn `/modules/:moduleId`.
  * Danh sách bài giảng, bài luyện tập và Thẻ bài thi vượt ải cuối module.

---

## 3. Đặc Tả Giao Tiếp Dữ Liệu (API Contracts: Request & Response)

### 3.1. Lấy toàn bộ lộ trình học hiện tại của học viên
* **Endpoint**: `GET /api/v1/learning-paths/current` hoặc `GET /api/v1/roadmap`
* **Response (200 OK)**:
```json
{
  "success": true,
  "message": "Lấy lộ trình học thích ứng thành công",
  "data": {
    "planId": 101,
    "targetScore": 650,
    "targetLevel": "NÂNG CAO",
    "overallProgress": 42,
    "totalModules": 4,
    "completedModules": 1,
    "modules": [
      {
        "id": 1,
        "title": "Khởi động & Nền tảng phát âm Part 1-2",
        "description": "Luyện nghe tranh và câu hỏi phản xạ với bẫy phát âm phổ biến.",
        "status": "COMPLETED",
        "progressPercent": 100,
        "lessonCount": 6,
        "completedLessonCount": 6,
        "estimatedHours": 8,
        "abilities": ["P1_P2_BASIC"]
      },
      {
        "id": 2,
        "title": "Part 5: Từ vựng & Ngữ pháp nâng cao",
        "description": "Tập trung giải quyết các câu hỏi từ loại, liên từ và cấu trúc đặc biệt.",
        "status": "IN_PROGRESS",
        "progressPercent": 65,
        "lessonCount": 8,
        "completedLessonCount": 5,
        "estimatedHours": 12,
        "abilities": ["P5_VOCAB", "COLLOCATION"]
      },
      {
        "id": 3,
        "title": "Part 7: Kỹ thuật đọc quét Skimming & Scanning",
        "description": "Nâng cao tốc độ đọc hiểu văn bản kép và bài viết thương mại.",
        "status": "LOCKED",
        "progressPercent": 0,
        "lessonCount": 10,
        "completedLessonCount": 0,
        "estimatedHours": 15,
        "abilities": ["P7_SCANNING", "P7_INFERENCE"]
      },
      {
        "id": 4,
        "title": "Luyện đề tổng hợp & Tối ưu tốc độ",
        "description": "Thực chiến đề thi ETS hoàn chỉnh dưới áp lực thời gian thực.",
        "status": "LOCKED",
        "progressPercent": 0,
        "lessonCount": 5,
        "completedLessonCount": 0,
        "estimatedHours": 10,
        "abilities": ["TIME_MANAGEMENT"]
      }
    ]
  }
}
```

### 3.2. Lấy chi tiết một Module
* **Endpoint**: `GET /api/v1/modules/{moduleId}`
* **Response (200 OK)**:
```json
{
  "success": true,
  "message": "Lấy chi tiết module thành công",
  "data": {
    "id": 2,
    "title": "Part 5: Từ vựng & Ngữ pháp nâng cao",
    "description": "Tập trung bứt phá Part 5 thông qua phân loại từ vựng và Collocations.",
    "status": "IN_PROGRESS",
    "progressPercent": 65,
    "lessons": [
      {
        "id": 1,
        "orderNo": 1,
        "title": "Bài 1: Tổng quan phương pháp giải Part 5 dưới 20s",
        "duration": "18 phút",
        "isCompleted": true
      },
      {
        "id": 2,
        "orderNo": 2,
        "title": "Bài 2: Các bẫy ngữ pháp thường gặp về thì",
        "duration": "22 phút",
        "isCompleted": true
      },
      {
        "id": 3,
        "orderNo": 3,
        "title": "Bài 3: Collocations & Cụm từ thường gặp",
        "duration": "24 phút",
        "isCompleted": false,
        "isCurrent": true
      }
    ],
    "moduleTest": {
      "testId": 201,
      "title": "Kiểm tra vượt ải Module: Part 5 — Vocabulary",
      "questionCount": 25,
      "durationMinutes": 25,
      "passScore": 65,
      "isUnlocked": true,
      "bestScore": null
    }
  }
}
```

---

## 4. Logic Nghiệp Vụ Backend Cần Xử Lý (Business Logic)
1. **Thuật toán Khởi tạo Lộ trình (`generateLearningPath`)**:
   * Khi học viên hoàn thành chẩn đoán và bấm vào lộ trình:
   * Nếu user chưa có `LearningPath` đang kích hoạt (`status = true`):
     * Tạo mới `LearningPath` (`user_id`, `version = 1`, `started_at = NOW()`).
     * Quét các Ability có trạng thái `WEAK` trong `ability_aim_plan_items`.
     * Tìm các `modules` gắn với những Ability đó (qua bảng `module_abilities`), sắp xếp theo trường `modules.sequence`.
     * Tạo các bản ghi `learning_path_items`: Item đầu tiên gán `status = 'IN_PROGRESS'`, các item sau gán `status = 'LOCKED'`.
2. **Kiểm tra điều kiện mở khóa bài thi Module Test**:
   * Bài thi Module Test chỉ `isUnlocked = true` khi học viên đã hoàn thành tối thiểu 80% số bài giảng trong module đó.

---

## 5. Bảng Cơ Sở Dữ Liệu Liên Quan
* **`learning_paths`**: Quản lý lộ trình tổng thể.
* **`learning_path_items`**: Quản lý từng chặng module trong lộ trình.
* **`modules`** & **`module_abilities`**: Danh mục module và ánh xạ năng lực.
* **`lessons`**: Danh sách bài học video/practice trong module.
* **`module_test_attempts`**: Kiểm tra điểm thi vượt ải cao nhất của module.

---

## 6. Danh Sách Công Việc Backend Cần Làm (Checklist)
- [ ] Tạo `LearningPathRepository`, `LearningPathItemRepository`, `ModuleRepository`, `LessonRepository`.
- [ ] Tạo DTO Response: `RoadmapResponse`, `ModuleTimelineItemDto`, `ModuleDetailResponse`, `LessonSummaryDto`.
- [ ] Viết `RoadmapService` & `RoadmapServiceImpl`.
- [ ] Tạo `RoadmapController.java` với 2 endpoint: `GET /api/v1/learning-paths/current` và `GET /api/v1/modules/{moduleId}`.
