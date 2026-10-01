# UC-05: Module & Lesson

## Mục tiêu
Quản lý Module và Lesson (Video / Practice). UI accordion: click Module → mở rộng xem Lesson bên trong.

---

## Backend

**Endpoints:**
```
GET    /api/admin/modules                              ← danh sách modules
GET    /api/admin/modules/{id}/lessons                 ← lessons của 1 module (lazy khi accordion mở)
POST   /api/admin/modules                              ← tạo module
PUT    /api/admin/modules/{id}                         ← cập nhật module
DELETE /api/admin/modules/{id}                         ← xóa module
POST   /api/admin/modules/{moduleId}/lessons/video     ← tạo video lesson
POST   /api/admin/modules/{moduleId}/lessons/practice  ← tạo practice lesson
PUT    /api/admin/lessons/{id}                         ← cập nhật lesson (tự detect loại)
DELETE /api/admin/lessons/{id}                         ← xóa lesson
PATCH  /api/admin/modules/{id}/reorder-lessons         ← đổi thứ tự lessons
```

**Response item GET modules — `data: ModuleResponse[]`:**
```json
{
  "id": 1,
  "title": "Module 1: Describe an Image — Basic Strategies",
  "type": "THEORY",
  "sequence": 1,
  "status": "PUBLISHED",
  "partId": 1,
  "partName": "Part 1: Photos",
  "totalLessons": 4,
  "totalVideoLessons": 2,
  "totalPracticeLessons": 2
}
```

**Response item GET lessons — `data: LessonResponse[]`:**
```json
{
  "id": 10,
  "lessonType": "VIDEO",
  "title": "Overview of Part 1 TOEIC",
  "orderNo": 1,
  "status": "PUBLISHED",
  "abilityId": 1,
  "abilityName": "Identifying Actions",
  "uri": "https://...",
  "durationSeconds": 920,
  "durationDisplay": "15:20 phút",
  "totalQuestions": null
}
```
> 1 DTO `LessonResponse` dùng chung, field không áp dụng = `null`.  
> `lessonType`: `'VIDEO' | 'PRACTICE'`

**Request POST video lesson:**
```json
{ "title": "...", "orderNo": 1, "descriptions": "...", "uri": "https://...", "durationSeconds": 920, "abilityId": 1 }
```

**Request POST practice lesson:**
```json
{ "title": "...", "orderNo": 2, "descriptions": "...", "instructions": "...", "abilityId": 2, "questionIds": [1042, 1043, 1044] }
```

**Request PATCH reorder:**
```json
{ "orderedLessonIds": [10, 12, 11, 13] }
```
> BE update `orderNo` theo vị trí index trong array.

**Service guard:**
- DELETE module: kiểm tra module không có trong `learning_path_items` → `BadRequestException`
- Không auto-publish: module mới mặc định `DRAFT`, admin phải chủ động set `PUBLISHED`

---

## Frontend

**Feature folder:** `src/features/admin-module/`

**Luồng:**
1. Page mount → fetch danh sách modules (không phân trang nếu tổng nhỏ, hoặc phân trang nếu nhiều)
2. State `openModuleIds: Set<number>` — track module nào đang mở accordion
3. Khi click mở module lần đầu → fetch lessons của module đó, cache vào state `lessonsByModuleId: Map<number, LessonResponse[]>`
4. Modal form Lesson: tab chọn `VIDEO | PRACTICE` → render form tương ứng
5. Reorder lesson: drag-drop (optional, có thể làm nút up/down đơn giản hơn)

**Service pattern:**
```typescript
export const adminModuleService = {
    getModules(params?: ModuleFilterParams): Promise<ApiResponse<ModuleResponse[]>> {
        return apiClient.get('/api/admin/modules', { params });
    },
    getLessons(moduleId: number): Promise<ApiResponse<LessonResponse[]>> {
        return apiClient.get(`/api/admin/modules/${moduleId}/lessons`);
    },
    createModule(data: CreateModuleRequest): Promise<ApiResponse<ModuleResponse>> {
        return apiClient.post('/api/admin/modules', data);
    },
    createVideoLesson(moduleId: number, data: CreateVideoLessonRequest): Promise<ApiResponse<LessonResponse>> {
        return apiClient.post(`/api/admin/modules/${moduleId}/lessons/video`, data);
    },
    createPracticeLesson(moduleId: number, data: CreatePracticeLessonRequest): Promise<ApiResponse<LessonResponse>> {
        return apiClient.post(`/api/admin/modules/${moduleId}/lessons/practice`, data);
    },
    // ... update, delete, reorder tương tự
};
```

**Lưu ý giao tiếp BE–FE:**
- `durationDisplay` BE tính sẵn dạng "15:20 phút" (tránh FE phải format)
- `questionIds` trong practice lesson = câu hỏi từ ngân hàng → FE cần question picker với filter Part/Ability
- Sau create/update/delete lesson → refetch `lessonsByModuleId[moduleId]` (không refetch toàn bộ)
- `lessonType` là discriminator để FE hiển thị đúng badge và form edit
