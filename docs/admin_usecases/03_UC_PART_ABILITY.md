# UC-03: Part & Ability

## Mục tiêu
UI dạng master-detail: chọn Part bên trái → xem/thêm/sửa/xóa Ability bên phải.  
7 Part TOEIC là dữ liệu seed sẵn (read-only). Ability mới do Admin tạo.

---

## Backend

**Endpoints:**
```
GET    /api/v1/admin/parts                          ← 7 Parts (không phân trang)
GET    /api/v1/admin/parts/{partId}/abilities       ← Abilities của 1 Part
POST   /api/v1/admin/parts/{partId}/abilities       ← Tạo Ability
PUT    /api/v1/admin/abilities/{id}                 ← Cập nhật Ability
DELETE /api/v1/admin/abilities/{id}                 ← Xóa Ability
```

**Response GET parts — `data: PartResponse[]`:**
```json
{
  "id": 1,
  "name": "Part 1: Photographs",
  "section": "LISTENING",
  "totalQuestions": 120
}
```
> `totalQuestions` = COUNT từ `questions` WHERE `part_id = ?`

**Response GET abilities — `data: AbilityResponse[]`:**
```json
{
  "id": 1,
  "name": "Identifying Actions",
  "description": "Nhận diện hành động trong ảnh",
  "status": "PUBLISHED",
  "totalQuestions": 45
}
```

**Request POST/PUT ability:**
```json
{
  "name": "Background Details",
  "description": "Quan sát chi tiết mờ trong nền ảnh",
  "status": "PUBLISHED"
}
```
> Status khi tạo mới: mặc định `DRAFT`.

**Service guard DELETE:**
- Kiểm tra `totalQuestions > 0` → throw `BadRequestException` ("Ability còn câu hỏi liên kết")

---

## Frontend

**Feature folder:** `src/features/admin-part/`

**Luồng:**
1. Page mount → fetch 7 Parts, lưu vào state, auto-select Part đầu tiên
2. Khi `selectedPartId` thay đổi → fetch Abilities của Part đó
3. Nút "Thêm Ability" → mở modal form (react-hook-form + zod)
4. Edit/Delete → gọi service tương ứng, sau thành công refetch abilities

**Service pattern:**
```typescript
export const adminPartService = {
    getParts(): Promise<ApiResponse<PartResponse[]>> {
        return apiClient.get('/api/v1/admin/parts');
    },
    getAbilities(partId: number): Promise<ApiResponse<AbilityResponse[]>> {
        return apiClient.get(`/api/v1/admin/parts/${partId}/abilities`);
    },
    createAbility(partId: number, data: AbilityFormData): Promise<ApiResponse<AbilityResponse>> {
        return apiClient.post(`/api/v1/admin/parts/${partId}/abilities`, data);
    },
    updateAbility(id: number, data: AbilityFormData): Promise<ApiResponse<AbilityResponse>> {
        return apiClient.put(`/api/v1/admin/abilities/${id}`, data);
    },
    deleteAbility(id: number): Promise<ApiResponse<null>> {
        return apiClient.delete(`/api/v1/admin/abilities/${id}`);
    },
};
```

**Lưu ý giao tiếp BE–FE:**
- GET parts không phân trang → `pagination` = undefined trong response
- GET abilities cũng không phân trang (số lượng nhỏ ~5-10 per Part)
- Status Ability enum: `'PUBLISHED' | 'DRAFT' | 'ARCHIVED'` — FE cần render đúng badge màu tương ứng
- Section Part enum: `'LISTENING' | 'READING'` — badge màu khác nhau theo Figma
