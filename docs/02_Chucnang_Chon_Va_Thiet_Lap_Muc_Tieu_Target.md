# TODO 02: Chức Năng Chọn & Thiết Lập Mục Tiêu TOEIC (Target Selection)

## 1. Mô Tả Chức Năng & Mục Tiêu Nghiệp Vụ
Đây là bước đầu tiên trong chu trình học tập cá nhân hóa sau khi người dùng đăng ký tài khoản thành công:
* **Cung cấp danh mục mục tiêu TOEIC**: Trả về danh sách các gói mục tiêu điểm số chuẩn mực (AIM 450, AIM 550, AIM 650, AIM 750, AIM 850+) cùng mô tả chi tiết và gợi ý mục tiêu khuyến nghị.
* **Lưu mốc điểm mục tiêu học viên chọn**: Kích hoạt bản ghi kế hoạch mục tiêu (`ability_aim_plans`) cho học viên trong cơ sở dữ liệu để làm mốc so sánh cho bài thi chẩn đoán tiếp theo.

---

## 2. Hỗ Trợ Cho Giao Diện Người Dùng (Frontend UI Mapping)
* **Trang Chọn Mục Tiêu (`TargetSelectionPage.tsx`)**: Đường dẫn `/target-selection` hoặc `/onboarding/target`.
* **Thành phần UI**:
  * 5 Card mục tiêu tương tác (`TargetCard.tsx`): Card 650 có viền cam 2px và nhãn "Khuyên chọn".
  * Nút hành động chính: *"Xác nhận mục tiêu"* (chuyển tiếp sang `/diagnostic/room`).
  * Dropdown thay đổi mục tiêu trong trang Profile (`ProfileInfoTab.tsx`).

---

## 3. Đặc Tả Giao Tiếp Dữ Liệu (API Contracts: Request & Response)

### 3.1. Lấy danh sách các mốc điểm mục tiêu TOEIC
* **Endpoint**: `GET /api/v1/targets`
* **Headers**: `Accept: application/json`
* **Response (200 OK)**:
```json
{
  "success": true,
  "message": "Lấy danh sách mục tiêu thành công",
  "data": [
    {
      "id": 1,
      "targetTotalScore": 450,
      "level": "CƠ BẢN",
      "description": "Đạt chuẩn tốt nghiệp Đại học hoặc phục vụ công việc căn bản.",
      "status": true,
      "isRecommended": false
    },
    {
      "id": 2,
      "targetTotalScore": 550,
      "level": "TRUNG CẤP",
      "description": "Đủ điều kiện làm việc trong môi trường đa quốc gia.",
      "status": true,
      "isRecommended": false
    },
    {
      "id": 3,
      "targetTotalScore": 650,
      "level": "NÂNG CAO",
      "description": "Mục tiêu vàng cho quản lý và nhân sự công nghệ.",
      "status": true,
      "isRecommended": true
    },
    {
      "id": 4,
      "targetTotalScore": 750,
      "level": "CHUYÊN SÂU",
      "description": "Thành thạo giao tiếp thuyết trình chuyên sâu.",
      "status": true,
      "isRecommended": false
    },
    {
      "id": 5,
      "targetTotalScore": 850,
      "level": "THÀNH THẠO",
      "description": "Làm chủ tiếng Anh học thuật và đàm phán quốc tế.",
      "status": true,
      "isRecommended": false
    }
  ]
}
```

### 3.2. Lưu lựa chọn mục tiêu của học viên
* **Endpoint**: `POST /api/v1/targets/select`
* **Yêu cầu xác thực**: Bắt buộc có phiên đăng nhập (Cookie JWT của User).
* **Request Body**:
```json
{
  "targetProfileId": 3
}
```
* **Response (200 OK)**:
```json
{
  "success": true,
  "message": "Thiết lập mục tiêu thành công",
  "data": {
    "planId": 101,
    "userId": 1,
    "targetProfileId": 3,
    "targetTotalScore": 650,
    "level": "NÂNG CAO",
    "createdAt": "2026-09-12T08:30:00Z"
  }
}
```

---

## 4. Logic Nghiệp Vụ Backend Cần Xử Lý (Business Logic)
1. **Lấy danh sách Targets**:
   * Truy vấn từ bảng `target_profiles` với điều kiện `status = true`, sắp xếp tăng dần theo `target_total_score ASC`.
   * Gán thêm trường tính toán `level` (dựa trên mốc điểm: <500: CƠ BẢN, 500-600: TRUNG CẤP, 600-700: NÂNG CAO, 700-800: CHUYÊN SÂU, >=800: THÀNH THẠO).
   * Đánh dấu `isRecommended = true` cho gói 650 điểm (mục tiêu phổ biến nhất).
2. **Lưu lựa chọn mục tiêu (`POST /select`)**:
   * Trích xuất `currentUserId` từ `SecurityContextHolder`.
   * Kiểm tra `targetProfileId` có tồn tại và đang kích hoạt không ➔ Nếu không: Ném `404 Not Found`.
   * Kiểm tra học viên đã có bản ghi trong `ability_aim_plans` chưa:
     * Nếu chưa có: Tạo mới một bản ghi `AbilityAimPlan` với `user_id`, `target_profile_id`, `status = true`, `created_at = NOW()`.
     * Nếu đã có: Cập nhật `target_profile_id` mới (trường hợp học viên đổi mục tiêu ở trang Profile).
   * Trả về DTO chứa `planId` vừa tạo/cập nhật.

---

## 5. Bảng Cơ Sở Dữ Liệu Liên Quan
* **`target_profiles`**:
  * Đọc: `id`, `target_total_score`, `status`, `description`.
* **`ability_aim_plans`**:
  * Ghi (Insert/Update): `id`, `user_id`, `target_profile_id`, `status`, `created_at`, `updated_at`.
* **`users`**:
  * Đọc: Xác thực `user_id`.

---

## 6. Các Lưu Ý Quan Trọng
* **Tái sử dụng**: API `POST /api/v1/targets/select` được dùng ở cả 2 nơi: Màn hình Onboarding ban đầu (`/target-selection`) và Màn hình Cập nhật mục tiêu trong Profile (`/profile`).
* **Không làm mất lịch sử**: Khi người dùng đổi mục tiêu, nếu đã có các bài làm cũ, giữ nguyên kết quả các lần thi trước, chỉ cập nhật lại mốc so sánh cho các bài thi và lộ trình kế tiếp.

---

## 7. Danh Sách Công Việc Backend Cần Làm (Checklist)
- [ ] Tạo `TargetProfileRepository` kế thừa `JpaRepository<TargetProfile, Long>`.
- [ ] Tạo `AbilityAimPlanRepository` kế thừa `JpaRepository<AbilityAimPlan, Long>`.
- [ ] Tạo DTO `TargetProfileResponse` và `SelectTargetRequest`.
- [ ] Viết `TargetService` & `TargetServiceImpl` xử lý logic lấy danh sách và lưu kế hoạch mục tiêu.
- [ ] Tạo `TargetController.java` tại `com.example.be.features.ability.controller` với 2 endpoint: `GET /api/v1/targets` và `POST /api/v1/targets/select`.
