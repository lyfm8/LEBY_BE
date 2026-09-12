# TODO 01: Chức Năng Xác Thực & Phân Quyền (Authentication & Authorization)

## 1. Mô Tả Chức Năng & Mục Tiêu Nghiệp Vụ
Phân hệ quản lý vòng đời tài khoản người dùng, xác minh danh tính và cấp quyền truy cập vào hệ sinh thái học tập LEBY:
* **Gửi mã OTP qua Email**: Xác thực email người dùng trước khi tạo tài khoản chính thức.
* **Đăng ký tài khoản**: Tạo mới học viên kèm mã hóa mật khẩu một chiều (BCrypt) và kiểm tra OTP còn hiệu lực.
* **Đăng nhập hệ thống**: Đăng nhập bằng `username` và `password`, kiểm tra trạng thái kích hoạt tài khoản (`status = true`), thiết lập phiên làm việc qua **HTTP-Only Cookie** (chống rò rỉ XSS).
* **Lấy thông tin phiên hiện tại (`/me`)**: Cho phép Front-End khôi phục trạng thái đăng nhập khi người dùng tải lại trang (reload / F5).
* **Đăng xuất an toàn**: Xóa bỏ Cookie và tăng `tokenVersion` trong cơ sở dữ liệu để vô hiệu hóa token ngay lập tức trên mọi thiết bị.

---

## 2. Hỗ Trợ Cho Giao Diện Người Dùng (Frontend UI Mapping)
* **Trang Đăng nhập (`LoginPage.tsx`)**: Đường dẫn `/login`
* **Trang Đăng ký (`RegisterPage.tsx`)**: Đường dẫn `/register`
* **Trang Xác minh OTP (`OtpVerifyPage.tsx`)**: Đường dẫn `/verify-otp`
* **Bộ bảo vệ đường dẫn (`AuthGuard.tsx`)**: Kiểm tra session trước khi cho phép vào các trang nội bộ (`/dashboard`, `/learning-path`, `/modules`, `/profile`).

---

## 3. Đặc Tả Giao Tiếp Dữ Liệu (API Contracts: Request & Response)

### 3.1. Gửi mã OTP xác minh Email
* **Endpoint**: `POST /api/auth/register/send-otp`
* **Headers**: `Content-Type: application/json`
* **Request Body**:
```json
{
  "email": "nam.nguyen@leby.edu.vn"
}
```
* **Response (200 OK)**:
```json
{
  "success": true,
  "message": "Mã OTP đã được gửi đến email của bạn",
  "data": null
}
```

### 3.2. Đăng ký tài khoản học viên mới
* **Endpoint**: `POST /api/auth/register`
* **Request Body**:
```json
{
  "username": "student_nam",
  "email": "nam.nguyen@leby.edu.vn",
  "password": "Password123@",
  "fullName": "Nguyễn Nam",
  "otp": "123456"
}
```
* **Response (200 OK) + Header `Set-Cookie`**:
```json
{
  "success": true,
  "message": "Đăng ký tài khoản thành công",
  "data": {
    "id": 1,
    "username": "student_nam",
    "email": "nam.nguyen@leby.edu.vn",
    "fullName": "Nguyễn Nam",
    "role": "USER",
    "dob": null,
    "avatar": "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150"
  }
}
```

### 3.3. Đăng nhập hệ thống
* **Endpoint**: `POST /api/auth/login`
* **Request Body**:
```json
{
  "username": "student_nam",
  "password": "Password123@"
}
```
* **Response (200 OK) + Header `Set-Cookie`**:
```json
{
  "success": true,
  "message": "Đăng nhập thành công",
  "data": {
    "id": 1,
    "username": "student_nam",
    "email": "nam.nguyen@leby.edu.vn",
    "fullName": "Nguyễn Nam",
    "role": "USER",
    "dob": "2001-08-15",
    "avatar": "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150"
  }
}
```

### 3.4. Lấy thông tin phiên hiện tại (Restore Session)
* **Endpoint**: `GET /api/auth/me` hoặc `GET /api/v1/auth/me`
* **Cookie**: Tự động đính kèm `access_token`
* **Response (200 OK)**:
```json
{
  "success": true,
  "message": "OK",
  "data": {
    "id": 1,
    "username": "student_nam",
    "email": "nam.nguyen@leby.edu.vn",
    "fullName": "Nguyễn Nam",
    "role": "USER",
    "dob": "2001-08-15",
    "avatar": "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150"
  }
}
```

### 3.5. Đăng xuất
* **Endpoint**: `POST /api/auth/logout`
* **Response (200 OK) + Header `Set-Cookie: access_token=; Max-Age=0`**:
```json
{
  "success": true,
  "message": "Đăng xuất thành công",
  "data": null
}
```

---

## 4. Logic Nghiệp Vụ Backend Cần Xử Lý (Business Logic)
1. **Kiểm tra trùng lặp**:
   * Khi gửi OTP / Đăng ký: Kiểm tra `users.username` hoặc `users.email` đã tồn tại chưa. Nếu đã có ➔ Ném lỗi `409 Conflict`.
2. **Xử lý OTP**:
   * Sinh mã ngẫu nhiên 6 chữ số (`SecureRandom`), lưu vào bảng tạm hoặc cache (Redis/Memory) với thời hạn sống (TTL) 5 phút.
   * Gửi qua `JavaMailSender` (template HTML thân thiện).
   * Khi đăng ký, kiểm tra mã OTP khớp và chưa hết hạn; sau khi dùng xong phải xóa hoặc đánh dấu đã sử dụng.
3. **Mã hóa & Phân quyền mặc định**:
   * Mật khẩu phải được băm bằng `BCryptPasswordEncoder` (độ mạnh 10 hoặc 12).
   * Gán Role mặc định: `ROLE_USER` thông qua bảng liên kết `user_roles`.
   * Gán `token_version = 1`.
4. **Cấp phát JWT Cookie**:
   * Payload JWT chứa: `userId`, `username`, `role`, `tokenVersion`.
   * Thời hạn token: 24 giờ.
   * Cookie cấu hình: `HttpOnly = true`, `Secure = false` (ở dev) hoặc `true` (ở prod), `SameSite = Lax`, `Path = /`.
5. **Cơ chế Token Invalidation (Bảo mật cao)**:
   * Khi kiểm tra request, JWT Filter giải mã token lấy `tokenVersion`. Nếu `tokenVersion` trong token nhỏ hơn `user.getTokenVersion()` trong DB ➔ Coi như token bị thu hồi, trả về `401 Unauthorized`.

---

## 5. Bảng Cơ Sở Dữ Liệu Liên Quan
* **`users`**:
  * Đọc/Ghi: `id`, `username`, `email`, `password_hash`, `full_name`, `dob`, `avatar`, `status`, `token_version`, `created_at`.
* **`roles`**:
  * Đọc: `id`, `name` (`ROLE_USER`, `ROLE_ADMIN`, `ROLE_STAFF`).
* **`user_roles`**:
  * Ghi: `user_id`, `role_id`.
* **`otp_tokens`** (hoặc bảng lưu OTP tạm thời):
  * Ghi: `email`, `otp_code`, `expired_at`, `is_used`.

---

## 6. Các Lưu Ý Quan Trọng (Security & Edge Cases)
* **CORS Configuration**: Bắt buộc cấu hình `allowCredentials(true)` và chỉ định rõ domain FE `http://localhost:5173` trong `WebSecurityConfig`. Không được dùng `allowedOrigins("*")` khi có cookie.
* **Xử lý Exception**:
  * Sai mật khẩu / Không tìm thấy user: Trả về chung một thông báo `"Tên đăng nhập hoặc mật khẩu không chính xác"` (tránh lộ thông tin user enumeration).
  * Tài khoản bị khóa (`status = false`): Trả về `403 Forbidden` kèm thông báo `"Tài khoản đã bị tạm khóa, vui lòng liên hệ hỗ trợ"`.
* **Hỗ trợ cả 2 tiền tố URL**: Trong code controller, hỗ trợ cả `@RequestMapping("/api/auth")` và alias `@RequestMapping("/api/v1/auth")` để tránh lệch chuẩn với FE.

---

## 7. Danh Sách Công Việc Backend Cần Làm (Checklist)
- [ ] Kiểm tra và hoàn thiện `AuthController.java` hỗ trợ đầy đủ 5 endpoints trên.
- [ ] Tạo DTO Request: `SendOtpRequest`, `RegisterRequest`, `LoginRequest`.
- [ ] Tạo DTO Response: `AuthUserResponse`.
- [ ] Viết `JwtTokenProvider` hỗ trợ tạo Cookie `ResponseCookie`.
- [ ] Viết `JwtAuthenticationFilter` trích xuất token từ Cookie header.
- [ ] Hoàn thiện `AuthServiceImpl` xử lý logic kiểm tra OTP, mã hóa BCrypt, kiểm tra tokenVersion.
- [ ] Viết cấu hình `SecurityConfig` mở public cho `/api/auth/**`, `/api/v1/auth/**`.
