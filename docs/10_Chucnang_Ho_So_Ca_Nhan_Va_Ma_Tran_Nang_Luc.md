# TODO 10: Chức Năng Trang Cá Nhân & Ma Trận Năng Lực (User Profile)

## 1. Mô Tả Chức Năng & Mục Tiêu Nghiệp Vụ
Quản lý thông tin học viên và theo dõi sự tiến bộ của bộ năng lực thích ứng:
* **Hồ sơ học viên (Profile Hero & Info)**: Cung cấp họ tên, email, ngày sinh, phân loại người học (Learner Type: *"Học thích ứng cấp tốc"*), hạng thành viên (*"Học viên Premium"*).
* **Ma trận năng lực TOEIC (Abilities Matrix)**: Trực quan hóa toàn bộ các kỹ năng đã được hệ thống đo lường và theo dõi qua các bài kiểm tra chẩn đoán và thi vượt ải:
  * Tỷ lệ chính xác (`accuracy_rate`).
  * Số lượng bằng chứng tích lũy (`evidence_count`).
  * Trạng thái phân loại chuẩn ETS: `STABLE` (Xanh lá - Đạt chuẩn), `DEVELOPING` (Vàng - Đang rèn luyện), `WEAK` (Đỏ - Cần cải thiện).
* **Cập nhật thông tin & Thay đổi mục tiêu**: Cho phép học viên cập nhật họ tên, ngày sinh, số điện thoại và đổi mốc điểm mục tiêu TOEIC (AIM).
* **Bảo mật & Đổi mật khẩu**: Kiểm tra mật khẩu cũ, mã hóa mật khẩu mới bằng BCrypt và cập nhật `tokenVersion` để bảo vệ tài khoản.

---

## 2. Hỗ Trợ Cho Giao Diện Người Dùng (Frontend UI Mapping)
* **Trang Cá Nhân (`ProfilePage.tsx`)**: Đường dẫn `/profile`.
* **Thành phần UI**:
  * Sử dụng khung giao diện thống nhất `SidebarNav.tsx` (Menu "Cài đặt" active).
  * Banner học viên `ProfileHero.tsx`.
  * Hệ thống 3 Tab điều hướng:
    * Tab 1: `ProfileInfoTab.tsx` (Form thông tin cá nhân & Chọn lại mục tiêu).
    * Tab 2: `ProfileAbilityTab.tsx` (Bảng tổng hợp năng lực TOEIC với thanh tiến độ % và huy hiệu trạng thái).
    * Tab 3: `ProfileSecurityTab.tsx` (Form đổi mật khẩu & Tùy chọn thông báo).

---

## 3. Đặc Tả Giao Tiếp Dữ Liệu (API Contracts: Request & Response)

### 3.1. Lấy thông tin hồ sơ và ma trận năng lực học viên
* **Endpoint**: `GET /api/v1/users/profile` hoặc `GET /api/v1/profile`
* **Response (200 OK)**:
```json
{
  "success": true,
  "message": "Lấy thông tin cá nhân thành công",
  "data": {
    "user": {
      "id": 1,
      "username": "student_nam",
      "email": "nam.nguyen@leby.edu.vn",
      "fullName": "Nguyễn Nam",
      "dob": "2001-08-15",
      "avatar": "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
      "learnerType": "Học thích ứng cấp tốc",
      "role": "Học viên Premium",
      "createdAt": "2026-08-01"
    },
    "target": {
      "planId": 101,
      "targetTotalScore": 650,
      "level": "NÂNG CAO",
      "createdAt": "2026-08-01"
    },
    "skills": [
      { "code": "P1_P2_BASIC", "name": "Nghe miêu tả tranh & hỏi đáp", "partNo": 1, "accuracyRate": 0.85, "evidenceCount": 45, "status": "STABLE" },
      { "code": "P3_P4_DETAIL", "name": "Nghe thông tin chi tiết", "partNo": 3, "accuracyRate": 0.42, "evidenceCount": 60, "status": "WEAK" },
      { "code": "P5_GRAMMAR", "name": "Ngữ pháp & Cấu trúc", "partNo": 5, "accuracyRate": 0.80, "evidenceCount": 70, "status": "STABLE" },
      { "code": "P5_COLLOCATION", "name": "Từ vựng & Cụm từ", "partNo": 5, "accuracyRate": 0.55, "evidenceCount": 50, "status": "DEVELOPING" },
      { "code": "P7_SCANNING", "name": "Đọc quét & Bắt ý chính", "partNo": 7, "accuracyRate": 0.38, "evidenceCount": 40, "status": "WEAK" }
    ]
  }
}
```

### 3.2. Cập nhật thông tin cá nhân & Mục tiêu
* **Endpoint**: `PUT /api/v1/users/profile` hoặc `PUT /api/v1/profile/info`
* **Request Body**:
```json
{
  "fullName": "Nguyễn Nam",
  "dob": "2001-08-15",
  "phone": "0987654321",
  "targetScore": 750
}
```
* **Response (200 OK)**:
```json
{
  "success": true,
  "message": "Cập nhật hồ sơ thành công",
  "data": {
    "fullName": "Nguyễn Nam",
    "dob": "2001-08-15",
    "targetScore": 750
  }
}
```

### 3.3. Đổi mật khẩu tài khoản
* **Endpoint**: `POST /api/v1/users/change-password`
* **Request Body**:
```json
{
  "oldPassword": "Password123@",
  "newPassword": "NewPassword456@"
}
```
* **Response (200 OK)**:
```json
{
  "success": true,
  "message": "Đổi mật khẩu thành công. Vui lòng đăng nhập lại trên các thiết bị khác.",
  "data": null
}
```

---

## 4. Logic Nghiệp Vụ Backend Cần Xử Lý (Business Logic)
1. **Tổng hợp Ma trận Năng lực (`skills`)**:
   * Truy vấn bảng `user_abilities` của `currentUserId`, join với bảng `abilities` để lấy mã kỹ năng, tên hiển thị, part số mấy.
   * Format `accuracyRate` thành số thập phân (ví dụ `0.85`), `status` dạng Enum (`STABLE`, `DEVELOPING`, `WEAK`).
2. **Cập nhật Mục tiêu (Khi đổi `targetScore`)**:
   * Tìm `target_profiles` có `target_total_score = targetScore`.
   * Cập nhật `target_profile_id` trong `ability_aim_plans` của user.
3. **Đổi mật khẩu**:
   * So sánh `oldPassword` với `users.password_hash` bằng `passwordEncoder.matches(...)`. Nếu sai ➔ Ném `400 Bad Request`.
   * Băm `newPassword` bằng BCrypt.
   * Tăng `token_version = token_version + 1` để vô hiệu hóa các token cũ đã cấp trên các trình duyệt khác.

---

## 5. Bảng Cơ Sở Dữ Liệu Liên Quan
* **`users`**: Đọc/Ghi thông tin cá nhân, mật khẩu, tokenVersion.
* **`ability_aim_plans`** & **`target_profiles`**: Đọc/Ghi mục tiêu AIM.
* **`user_abilities`** & **`abilities`**: Đọc ma trận năng lực của học viên.

---

## 6. Danh Sách Công Việc Backend Cần Làm (Checklist)
- [ ] Tạo DTO Request: `UpdateProfileRequest`, `ChangePasswordRequest`.
- [ ] Tạo DTO Response: `ProfileDataResponse`, `UserAbilityDto`.
- [ ] Viết `UserService` & `UserServiceImpl` xử lý thông tin cá nhân, cập nhật profile và đổi mật khẩu.
- [ ] Tạo `UserController.java` tại `com.example.be.features.user.controller` với 3 endpoint: `GET /profile`, `PUT /profile`, `POST /change-password`.
