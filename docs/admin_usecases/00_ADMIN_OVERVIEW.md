# LEBY Admin Portal — Tổng quan UseCase (v2)

> **Đọc file này trước khi đọc bất kỳ UC nào bên dưới.**

---

## Kiến trúc thực tế của dự án

### Backend — Spring Boot
- Phân tầng: `Controller → Service (interface) → ServiceImpl → Repository`
- Tất cả endpoint trả về `ApiResponse<T>` wrapper
- Phân quyền: `@PreAuthorize("hasRole('ADMIN')")` tại method level
- DTO: tách rõ `CreateXRequest`, `UpdateXRequest`, `XResponse`
- Mapping thủ công hoặc MapStruct (không expose Entity ra ngoài)

### Frontend — React + TypeScript + Vite
**Cấu trúc thư mục mỗi feature** (theo pattern hiện có của dự án):
```
src/features/<feature-name>/
├── components/      ← Presentational (Dumb) components, chỉ nhận props
├── pages/           ← Container (Smart) components, fetch data, quản lý state
├── services/        ← Gọi API, pattern: export const xyzService = { method() }
├── types/           ← TypeScript interfaces, không có logic
└── <feature>.css    ← CSS riêng của feature
```

**Pattern gọi API (bắt buộc theo codebase hiện tại):**
```typescript
// services/xyzService.ts
import { apiClient } from '@/core/api/apiClient';
import type { ApiResponse } from '@/core/api/apiResponse';
import type { XyzResponse } from '../types/xyzTypes';

export const xyzService = {
    getList(): Promise<ApiResponse<XyzResponse[]>> {
        return apiClient.get<ApiResponse<XyzResponse[]>>('/api/admin/xyz');
    },
};
```

**ApiResponse interface thực tế (từ `core/api/apiResponse.ts`):**
```typescript
interface ApiResponse<T> {
    success: boolean;
    message: string;
    data: T;
    pagination?: {
        page: number;
        pageSize: number;
        totalItems: number;
        totalPages: number;
    }
}
```
> ⚠️ Pagination nằm trong `ApiResponse.pagination`, KHÔNG nằm trong `data`.  
> Khi API trả danh sách có phân trang: `data = T[]`, `pagination = { page, pageSize, totalItems, totalPages }`.

**Không có TanStack Query trong dependencies hiện tại.**  
→ State management bằng `useState` + `useEffect` hoặc custom hook.  
→ Validation form dùng `react-hook-form` + `zod` (đã có sẵn).

---

## Quy ước chung BE

### URL pattern Admin
```
GET    /api/admin/{resource}          ← danh sách (có phân trang)
GET    /api/admin/{resource}/{id}     ← chi tiết
POST   /api/admin/{resource}          ← tạo mới
PUT    /api/admin/{resource}/{id}     ← cập nhật toàn bộ
PATCH  /api/admin/{resource}/{id}/... ← cập nhật 1 trường
DELETE /api/admin/{resource}/{id}     ← xóa
```

### ApiResponse BE chuẩn
```json
// Thành công - đơn lẻ
{ "success": true, "message": "...", "data": { ... } }

// Thành công - danh sách phân trang
{ "success": true, "message": "...", "data": [...], "pagination": { "page": 0, "pageSize": 10, "totalItems": 2847, "totalPages": 285 } }

// Thất bại
{ "success": false, "message": "Mô tả lỗi thân thiện", "errors": [{ "field": "email", "detail": "..." }] }
```

### Query params phân trang chuẩn
```
?page=0&pageSize=10&sort=createdAt&direction=desc
```

---

## 8 UC cần implement

| # | File | Sidebar |
|---|------|---------|
| 1 | [01_UC_DASHBOARD.md](./01_UC_DASHBOARD.md) | Dashboard |
| 2 | [02_UC_USER_MANAGEMENT.md](./02_UC_USER_MANAGEMENT.md) | Quản lý User |
| 3 | [03_UC_PART_ABILITY.md](./03_UC_PART_ABILITY.md) | Part & Ability |
| 4 | [04_UC_QUESTION_BANK.md](./04_UC_QUESTION_BANK.md) | Ngân hàng câu hỏi |
| 5 | [05_UC_MODULE_LESSON.md](./05_UC_MODULE_LESSON.md) | Module & Lesson |
| 6 | [06_UC_MODULE_TEST.md](./06_UC_MODULE_TEST.md) | Module Test |
| 7 | [07_UC_TARGET_PROFILE.md](./07_UC_TARGET_PROFILE.md) | Target Profile (AIM) |
| 8 | [08_UC_THRESHOLD_RULE.md](./08_UC_THRESHOLD_RULE.md) | Threshold & Evaluation Rule |

## Thứ tự implement
```
Part & Ability → Question Bank → Module & Lesson → Module Test
→ Target Profile → Threshold & Eval Rule → User Mgmt → Dashboard
```
> Lý do: Part & Ability là master data, các UC sau đều phụ thuộc.
