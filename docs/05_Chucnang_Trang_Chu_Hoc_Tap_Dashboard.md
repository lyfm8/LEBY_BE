# TODO 05: Chức Năng Trang Chủ Học Tập Cá Nhân Hóa (Student Dashboard)

## 1. Mô Tả Chức Năng & Mục Tiêu Nghiệp Vụ
Sau khi có kết quả chẩn đoán, học viên bước vào trang chủ học tập hàng ngày:
* **Trung tâm điều phối học tập**: Cung cấp bức tranh tổng thể về tiến độ rèn luyện của học viên.
* **Theo dõi mục tiêu & điểm hiện tại**: So sánh điểm dự đoán hiện tại (ví dụ 520) với điểm mục tiêu (650), cùng mức tăng điểm (+30 điểm).
* **Đo lường kỷ luật & chuyên cần**: Quản lý chuỗi ngày học liên tục (Streak) và số giờ tích lũy trong tuần.
* **Bảng mini đánh giá 7 Part**: Giúp học viên luôn nhìn thấy Part nào đã vững (`PASS`), Part nào đang yếu (`WEAK`).
* **Đề xuất chặng học tiếp theo (Next Roadmap Milestone)**: Chỉ định rõ ràng Module tiếp theo học viên cần hoàn thành kèm nút hành động *"Học ngay"* dẫn thẳng tới bài học.

---

## 2. Hỗ Trợ Cho Giao Diện Người Dùng (Frontend UI Mapping)
* **Trang Chủ Học Tập (`DashboardPage.tsx`)**: Đường dẫn `/dashboard` hoặc `/home`.
* **Thành phần UI**:
  * Sử dụng khung giao diện thống nhất `SidebarNav.tsx` bên trái.
  * Header phụ `db-sub-header`: Lời chào cá nhân hóa, nút huy hiệu mục tiêu (nhấp để đổi mục tiêu).
  * 4 Thẻ chỉ số `StatCard.tsx`: Điểm dự đoán, Streak ngày học, Thời gian học tuần, Tiến độ Modules.
  * Cột trái: `PartEvaluationCard.tsx` (mini grid 7 Part) và `RecentActivityCard.tsx` (nhật ký làm bài gần đây).
  * Cột phải: `NextRoadmapCard.tsx` (Module ưu tiên hàng đầu cần học kèm nút *"Học ngay"*).

---

## 3. Đặc Tả Giao Tiếp Dữ Liệu (API Contracts: Request & Response)

### 3.1. Lấy thông tin tổng quan Dashboard
* **Endpoint**: `GET /api/v1/dashboard/summary`
* **Yêu cầu xác thực**: Bắt buộc đăng nhập.
* **Response (200 OK)**:
```json
{
  "success": true,
  "message": "Lấy dữ liệu trang chủ thành công",
  "data": {
    "greeting": "Chào mừng trở lại, Nguyễn Nam!",
    "user": {
      "id": 1,
      "fullName": "Nguyễn Nam",
      "avatar": "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
      "targetScore": 650,
      "targetLevel": "NÂNG CAO"
    },
    "stats": {
      "predictedScore": 520,
      "scoreDiff": 30,
      "streakDays": 5,
      "weeklyHours": 12.5,
      "completedModules": 1,
      "totalModules": 5
    },
    "partEvaluations": [
      { "partNo": 1, "name": "Part 1: Photographs", "score": 90, "directive": "PASS", "directiveLabel": "PASS" },
      { "partNo": 2, "name": "Part 2: Question-Response", "score": 75, "directive": "PASS", "directiveLabel": "PASS" },
      { "partNo": 3, "name": "Part 3: Conversations", "score": 55, "directive": "CONFIRM", "directiveLabel": "CONFIRM" },
      { "partNo": 4, "name": "Part 4: Short Talks", "score": 35, "directive": "WEAK", "directiveLabel": "WEAK" },
      { "partNo": 5, "name": "Part 5: Incomplete Sentences", "score": 80, "directive": "PASS", "directiveLabel": "PASS" },
      { "partNo": 6, "name": "Part 6: Text Completion", "score": 62, "directive": "CONFIRM", "directiveLabel": "CONFIRM" },
      { "partNo": 7, "name": "Part 7: Reading", "score": 40, "directive": "FULL_PART", "directiveLabel": "FULL PART" }
    ],
    "nextModules": [
      {
        "id": 101,
        "moduleId": 2,
        "title": "Part 5: Từ vựng & Ngữ pháp nâng cao",
        "description": "Tập trung cải thiện các câu hỏi từ loại và cấu trúc đặc biệt.",
        "section": "READING",
        "status": "IN_PROGRESS",
        "statusLabel": "Đang học (65%)"
      }
    ],
    "recentActivities": [
      {
        "id": 1,
        "title": "Hoàn thành bài thi Chẩn đoán Tier 1",
        "timestamp": "Hôm nay, 10:30",
        "scoreText": "520 / 650",
        "type": "DIAGNOSTIC"
      },
      {
        "id": 2,
        "title": "Hoàn thành Bài 2: Ngữ pháp thì kết hợp",
        "timestamp": "Hôm qua, 20:15",
        "scoreText": "Đã xong",
        "type": "LESSON"
      }
    ]
  }
}
```

---

## 4. Logic Nghiệp Vụ Backend Cần Xử Lý (Business Logic)
1. **Tính toán Streak ngày học**:
   * Dựa trên các bản ghi `diagnostic_attempts`, `module_test_attempts` hoặc nhật ký truy cập học tập.
   * Nếu có hoạt động học trong ngày hôm nay hoặc hôm qua ➔ Tính số ngày liên tục. Nếu cách quá 1 ngày ➔ Reset về 1.
2. **Lấy điểm dự đoán hiện tại**:
   * Ưu tiên lấy điểm từ bài kiểm tra gần nhất (bài thi chẩn đoán hoặc module test gần nhất).
3. **Xác định chặng tiếp theo (`nextModules`)**:
   * Đọc từ bảng `learning_paths` của học viên: Tìm `learning_path_items` có `status = 'IN_PROGRESS'` (hoặc item đầu tiên có `status = 'NOT_STARTED'`).
   * Trả về thông tin module đó để FE hiển thị card "Lộ trình học tiếp theo" với nút "Học ngay".
4. **Nhật ký hoạt động (`recentActivities`)**:
   * Gom và sắp xếp giảm dần theo thời gian (`ORDER BY created_at DESC LIMIT 5`) từ các bảng bài tập, bài thi.

---

## 5. Bảng Cơ Sở Dữ Liệu Liên Quan
* **`users`**: Đọc `full_name`, `avatar`.
* **`ability_aim_plans`** & **`target_profiles`**: Đọc mục tiêu AIM.
* **`part_diagnostic_results`**: Đọc điểm và chỉ thị 7 Part.
* **`learning_paths`** & **`learning_path_items`**: Đọc tiến độ lộ trình, module đang học.
* **`modules`**: Đọc tên module, mô tả.
* **`module_test_attempts`**: Đọc lịch sử thi gần đây.

---

## 6. Danh Sách Công Việc Backend Cần Làm (Checklist)
- [ ] Tạo DTO `DashboardSummaryResponse`, `StatCardSummaryDto`, `NextModuleItemDto`, `RecentActivityDto`.
- [ ] Tạo `DashboardService` & `DashboardServiceImpl`.
- [ ] Tạo `DashboardController.java` tại `com.example.be.features.content.controller` với endpoint `GET /api/v1/dashboard/summary`.
