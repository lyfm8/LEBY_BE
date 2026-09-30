# UC-04: Ngân hàng câu hỏi

## Mục tiêu
Quản lý toàn bộ câu hỏi (dùng chung cho Diagnostic Test và Practice Lesson). Admin có thể lọc, tìm kiếm, và tạo mới câu hỏi trực tiếp.

---

## Backend

**Endpoints:**
```
GET    /api/v1/admin/questions          ← danh sách + filter + phân trang
GET    /api/v1/admin/questions/{id}     ← chi tiết đầy đủ
POST   /api/v1/admin/questions          ← tạo mới
PUT    /api/v1/admin/questions/{id}     ← cập nhật
DELETE /api/v1/admin/questions/{id}     ← xóa
```

**Query params GET:**
```
?page=0&pageSize=10&partId=1&abilityId=2&type=SINGLE_CHOICE&difficulty=3&keyword=background
```

**Response item danh sách — `data: QuestionListItemResponse[]`:**
```json
{
  "id": 1042,
  "name": "Q-1042",
  "questionSummary": "What is the man doing in the background...",
  "partName": "Part 1",
  "abilityName": "Background Details",
  "type": "SINGLE_CHOICE",
  "difficulty": 3,
  "section": "LISTENING"
}
```

**Request POST/PUT:**
```json
{
  "name": "Q-New",
  "type": "SINGLE_CHOICE",
  "difficulty": 2,
  "section": "LISTENING",
  "partId": 1,
  "abilityIds": [1, 4],
  "descriptions": "...",
  "questionData": { "text": "...", "options": ["A.", "B.", "C.", "D."] },
  "correctAnswer": { "answer": "A" }
}
```

**Service logic:**
- Validate `abilityIds` phải thuộc cùng `partId` → throw `BadRequestException` nếu sai
- Khi PUT: xóa toàn bộ `QuestionAbility` cũ → insert lại từ `abilityIds` mới
- Guard DELETE: kiểm tra câu hỏi không được dùng trong bảng khác

---

## Frontend

**Feature folder:** `src/features/admin-question/`

**Luồng:**
1. Danh sách có bộ lọc (Part, Ability, Độ khó, Search).
2. Form tạo mới có giao diện nhập dạng JSON object cấu trúc sẵn cho `questionData` và `correctAnswer`.
3. Dropdown Ability phụ thuộc vào giá trị Part đã chọn.

**Service pattern:**
```typescript
export const adminQuestionService = {
    getQuestions(params: QuestionFilterParams): Promise<ApiResponse<QuestionListItemResponse[]>> {
        return apiClient.get('/api/v1/admin/questions', { params });
    },
    // ... CRUD methods
};
```
