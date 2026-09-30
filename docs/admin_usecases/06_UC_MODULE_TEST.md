# UC-06: Module Test

## Mục tiêu
Quản lý bài kiểm tra cuối Module (tập hợp các câu hỏi). Sử dụng cấu trúc DB hiện tại: Quản lý danh sách `ModuleTestQuestion` gắn trực tiếp vào `Module`. KHÔNG tạo entity `ModuleTest` dư thừa.

---

## Backend

**Endpoints:**
```
GET    /api/v1/admin/modules/{moduleId}/test-questions          ← lấy danh sách câu hỏi test của module
POST   /api/v1/admin/modules/{moduleId}/test-questions/batch    ← cập nhật toàn bộ danh sách câu hỏi
```

**Response GET — `data: QuestionListItemResponse[]`:**
```json
[
  {
    "id": 1042,
    "name": "Q-1042",
    "questionSummary": "...",
    "difficulty": 1,
    "type": "SINGLE_CHOICE"
  }
]
```

**Request POST batch:**
```json
{
  "questionIds": [1042, 1045, 1050]
}
```

**Service logic (AdminModuleTestService):**
- **GET**: SELECT các Question JOIN với `ModuleTestQuestion` WHERE `module_id = ?`.
- **POST (Cập nhật)**: 
  - Validate: Không cho phép đổi nếu đã có `ModuleTestAttempt` cho Module này.
  - DELETE toàn bộ `ModuleTestQuestion` cũ của Module.
  - INSERT mới các `ModuleTestQuestion` từ mảng `questionIds`.

> Nếu cần quản lý thời gian test (`durationMinutes`), thêm field `testDurationMinutes` trực tiếp vào entity `Module`. Hiện tại cứ quản lý danh sách câu hỏi trước.

---

## Frontend

**Feature folder:** Có thể gộp vào `src/features/admin-module/` (nằm trong màn hình chi tiết/chỉnh sửa Module).

**Luồng:**
1. Tại trang danh sách Module (UC-05), thêm một nút "Cấu hình Bài Test".
2. Nhấn vào mở ra màn hình/modal quản lý Test của Module đó.
3. Bên trái/trên: Hiển thị danh sách câu hỏi hiện tại.
4. Bên phải/dưới: Bảng Ngân hàng câu hỏi (có filter) để tick chọn thêm vào Test.
5. Nhấn Lưu → Gửi mảng `questionIds` về BE.

**Service pattern:**
```typescript
export const adminModuleTestService = {
    getTestQuestions(moduleId: number): Promise<ApiResponse<QuestionListItemResponse[]>> {
        return apiClient.get(`/api/v1/admin/modules/${moduleId}/test-questions`);
    },
    updateTestQuestions(moduleId: number, data: { questionIds: number[] }): Promise<ApiResponse<null>> {
        return apiClient.post(`/api/v1/admin/modules/${moduleId}/test-questions/batch`, data);
    },
};
```
