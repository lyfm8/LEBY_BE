# UC-09: Diagnostic Test (Đề kiểm tra đầu vào)

## Mục tiêu
Quản lý các Đề thi chẩn đoán (Diagnostic Test) do Admin soạn sẵn. Học viên mới vào sẽ phải làm 1 trong các đề này để đánh giá năng lực ban đầu.

---

## Backend

**Endpoints:**
```
GET    /api/v1/admin/diagnostic-tests          ← danh sách + filter
GET    /api/v1/admin/diagnostic-tests/{id}     ← chi tiết + danh sách câu hỏi
POST   /api/v1/admin/diagnostic-tests          ← tạo mới
PUT    /api/v1/admin/diagnostic-tests/{id}     ← cập nhật
DELETE /api/v1/admin/diagnostic-tests/{id}     ← xóa
```

**Response `data: DiagnosticTestListItemResponse[]`:**
```json
{
  "id": 1,
  "title": "Đề Chẩn Đoán Đầu Vào Số 1 (Full 7 Parts)",
  "description": "Đề chuẩn đánh giá toàn diện...",
  "status": true,
  "totalQuestions": 100
}
```

**Request POST/PUT:**
```json
{
  "title": "Đề Chẩn Đoán Đầu Vào Số 1",
  "description": "...",
  "status": true,
  "questionIds": [1042, 1043, 1044, 2055]
}
```

**Service logic:**
- POST: Lưu `DiagnosticTest` + Insert các `DiagnosticTestQuestion` từ `questionIds`.
- PUT: Xóa các `DiagnosticTestQuestion` cũ → Insert lại mảng mới.
- DELETE: Guard - Không cho phép xóa nếu đã có học viên làm bài (`DiagnosticAttempt`).

---

## Frontend

**Feature folder:** `src/features/admin-diagnostic-test/`

**Luồng:**
1. Danh sách Đề thi (có nút Thêm, Sửa, Xóa).
2. Khi Thêm/Sửa:
   - Form cơ bản: `title`, `description`, `status` (toggle Active/Inactive).
   - Component **Question Picker** (dùng chung với Module Test UC-06): Hiển thị bảng Ngân hàng câu hỏi, có filter Part/Ability, để Admin tick chọn những câu sẽ đưa vào đề.
3. Khi click lưu → Submit JSON về BE.

**Lưu ý:** 
- Đề Diagnostic thường khá dài (ví dụ 100 câu). Giao diện cần đếm rõ "Đã chọn X câu hỏi" để Admin dễ kiểm soát.
