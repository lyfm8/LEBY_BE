# TO-DO BE: ADMIN FEATURES (UC01 - UC03)

Tài liệu này tổng hợp các yêu cầu API từ Frontend cho các tính năng Admin.

---

## UC-01: ADMIN DASHBOARD

### 1. Endpoint cần thiết
- `GET /api/v1/admin/dashboard/summary` (Role: ADMIN)

### 2. Cấu trúc Response
```json
{
  "success": true,
  "data": {
    "stats": {
      "totalUsers": 2847,
      "totalUsersGrowthPercent": 12.5,
      "activeUsersThisMonth": 1420,
      "activeUsersGrowthPercent": 8.3, 
      "totalModules": 16,
      "totalTestsCompleted": 12405
    },
    "usersByMonth": [ { "month": "T5", "count": 180 } ],
    "aimDistribution": [ { "aimName": "AIM 450", "count": 512, "percent": 18.0 } ],
    "moduleTestPassRate": { "passPercent": 75.0, "failPercent": 25.0 },
    "recentActivities": [
      {
        "userId": 101,
        "fullName": "Nguyễn Nam",
        "action": "Đã hoàn thành Module Test 4",
        "aimTarget": "AIM 650",
        "timeAgo": "5 phút trước",
        "resultBadge": "PASS" // "PASS", "NEW", "SCORE"
      }
    ]
  }
}
```

### 3. Lưu ý
- Tối ưu N+1 query, dùng cache nếu cần.
- Không phân trang.

---

## UC-02: USER MANAGEMENT

### 1. Các Endpoints Cần Thiết
Tất cả yêu cầu Role: ADMIN.
- `GET /api/v1/admin/users` (Query params: `page`, `pageSize`, `keyword`, `isActive`, `role`)
- `GET /api/v1/admin/users/{id}`
- `POST /api/v1/admin/users` (Body cần `password`)
- `PUT /api/v1/admin/users/{id}` (Body KHÔNG `password`)
- `PATCH /api/v1/admin/users/{id}/toggle-active`

### 2. Cấu trúc Response (GET List)
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "fullName": "Nguyễn Văn A",
      "email": "a@gmail.com",
      "role": "STUDENT",
      "isActive": true,
      "learnerType": "FREE",
      "aimTarget": "AIM 650",
      "createdAt": "2024-10-12T00:00:00"
    }
  ],
  "pagination": { "page": 0, "pageSize": 10, "totalItems": 150, "totalPages": 15 }
}
```

### 3. Lưu ý
- Lật `isActive = false` BẮT BUỘC phải tăng `tokenVersion` / invalidate JWT.
- Handle DuplicateResourceException (username/email trùng).

---

## UC-03: PART & ABILITY MANAGEMENT

### 1. Các Endpoints Cần Thiết
- `GET /api/v1/admin/parts` (Lấy 7 parts cố định, tính tổng câu hỏi)
- `GET /api/v1/admin/parts/{partId}/abilities` (Lấy các ability của 1 part cụ thể)
- `POST /api/v1/admin/parts/{partId}/abilities` (Tạo ability mới, mặc định DRAFT nếu client không truyền)
- `PUT /api/v1/admin/abilities/{id}` (Cập nhật ability)
- `DELETE /api/v1/admin/abilities/{id}` (Xóa ability)

### 2. Cấu trúc Response
**GET Parts:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "Part 1: Photographs",
      "section": "LISTENING", // "LISTENING" hoặc "READING"
      "totalQuestions": 120
    }
  ]
}
```

**GET Abilities:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "Identifying Actions",
      "description": "Nhận diện hành động trong ảnh",
      "status": "PUBLISHED", // "PUBLISHED", "DRAFT", "ARCHIVED"
      "totalQuestions": 45
    }
  ]
}
```

### 3. Lưu ý
- Cả hai list Parts và Abilities đều KHÔNG phân trang (`pagination` = undefined).
- **Service Guard DELETE**: Nếu `totalQuestions > 0`, throw `BadRequestException` và không cho xóa. Tránh lỗi HTTP 500 do foreign key constraint.

---

## UC-04: NGÂN HÀNG CÂU HỎI (QUESTION BANK)

### 1. Các Endpoints Cần Thiết
- `GET /api/v1/admin/questions` (Query params: `page`, `pageSize`, `partId`, `abilityId`, `type`, `difficulty`, `keyword`)
- `GET /api/v1/admin/questions/{id}` (Lấy chi tiết)
- `POST /api/v1/admin/questions` (Tạo mới)
- `PUT /api/v1/admin/questions/{id}` (Cập nhật)
- `DELETE /api/v1/admin/questions/{id}` (Xóa)

### 2. Cấu trúc Response & Request
**Response Danh sách:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1042,
      "name": "Q-1042",
      "questionSummary": "What is the man doing...",
      "partName": "Part 1",
      "abilityName": "Background Details",
      "type": "SINGLE_CHOICE",
      "difficulty": 3,
      "section": "LISTENING"
    }
  ],
  "pagination": { "page": 0, "pageSize": 10, "totalItems": 150, "totalPages": 15 }
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
  "descriptions": "Ghi chú thêm",
  "questionData": { "text": "...", "options": ["A", "B", "C", "D"] },
  "correctAnswer": { "answer": "A" }
}
```

### 3. Lưu ý
- Validate `abilityIds` phải thuộc về `partId` tương ứng. Nếu sai, ném `BadRequestException`.
- Khi update (PUT), BE xử lý xóa quan hệ với `abilityIds` cũ và thêm `abilityIds` mới.
- Không cho xóa nếu câu hỏi đã được dùng ở Lesson/Test. Thử xóa -> nếu reference -> trả về `BadRequestException` rõ ràng.

---

## UC-05: MODULE & LESSON

### 1. Các Endpoints Cần Thiết
- `GET /api/v1/admin/modules` (Lấy danh sách Modules)
- `GET /api/v1/admin/modules/{id}/lessons` (Lấy danh sách bài học của Module)
- `POST /api/v1/admin/modules` (Tạo Module)
- `PUT /api/v1/admin/modules/{id}` (Cập nhật Module)
- `DELETE /api/v1/admin/modules/{id}` (Xóa Module)
- `POST /api/v1/admin/modules/{moduleId}/lessons/video` (Tạo Video Lesson)
- `POST /api/v1/admin/modules/{moduleId}/lessons/practice` (Tạo Practice Lesson)
- `PUT /api/v1/admin/lessons/{id}` (Cập nhật Lesson)
- `DELETE /api/v1/admin/lessons/{id}` (Xóa Lesson)
- `PATCH /api/v1/admin/modules/{id}/reorder-lessons` (Đổi thứ tự lessons, nhận mảng `orderedLessonIds`)

### 2. Cấu trúc Response & Request
**GET Modules (Item):**
```json
{
  "id": 1,
  "title": "Module 1",
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

**GET Lessons (Item DTO chung cho cả Video và Practice):**
```json
{
  "id": 10,
  "lessonType": "VIDEO", // hoặc "PRACTICE"
  "title": "Overview",
  "orderNo": 1,
  "status": "PUBLISHED",
  "abilityId": 1,
  "abilityName": "Identifying Actions",
  "uri": "https://...",
  "durationSeconds": 920,
  "durationDisplay": "15:20 phút", // BE tự format
  "totalQuestions": null // Với Practice thì có số, Video thì null
}
```

### 3. Lưu ý
- **DELETE Module Guard:** Nếu module đang nằm trong Learning Path (mục tiêu học tập của user), trả về 400 Bad Request không cho xóa.
- Tạo Lesson: Backend nhận `questionIds` (với Practice) và tự lưu liên kết. Frontend có thể gửi array `[1042, 1043]`.
- Frontend đang dùng `durationDisplay` do BE trả về trực tiếp để hiển thị (VD: "15:20 phút").

---

## UC-06: MODULE TEST

### 1. Các Endpoints Cần Thiết
- `GET /api/v1/admin/modules/{moduleId}/test-questions` (Lấy danh sách câu hỏi test của Module)
- `POST /api/v1/admin/modules/{moduleId}/test-questions/batch` (Cập nhật toàn bộ danh sách câu hỏi cho Test, gửi body `{ "questionIds": [1, 2, 3] }`)

### 2. Cấu trúc Response
**GET Test Questions (trả về array các Item DTO giống QuestionListItemResponse):**
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

### 3. Lưu ý
- **POST (Cập nhật)**: BE thực hiện DELETE các câu hỏi cũ và INSERT các câu hỏi mới vào bảng trung gian (ModuleTestQuestion).
- **Guard Validation**: Không cho phép sửa đổi danh sách câu hỏi nếu Module này ĐÃ CÓ User bắt đầu làm bài test (đã phát sinh `ModuleTestAttempt`), tránh rác data hoặc sai lệch điểm số cũ.

---

## UC-07: TARGET PROFILE (AIM)

### 1. Endpoint - Target Profile
- `GET /api/v1/admin/target-profiles` (Lấy tất cả profile)
- `POST /api/v1/admin/target-profiles` (Tạo mới)
- `PUT /api/v1/admin/target-profiles/{id}` (Cập nhật)
- `DELETE /api/v1/admin/target-profiles/{id}` (Xóa profile)

### 2. Endpoint - Module Target Threshold
- `GET /api/v1/admin/module-target-thresholds` (Lấy ma trận ngưỡng pass cho tất cả Modules theo các AIM)
- `PUT /api/v1/admin/module-target-thresholds` (Cập nhật hàng loạt (batch upsert) các ngưỡng, FE sẽ gửi từng mảng item `{ moduleId, targetProfileId, passThreshold }`)

### 3. Cấu trúc Response 
**GET Module Target Threshold Matrix:**
```json
{
  "profileIds": [1, 2, 3],
  "profileNames": ["AIM 450", "AIM 550", "AIM 650"],
  "rows": [
    {
      "moduleId": 1,
      "moduleTitle": "Module 1",
      "thresholds": { "1": 60, "2": 70, "3": 80 }
    }
  ]
}
```
*(Key của `thresholds` là dạng chuỗi `profileId` chứa giá trị phần trăm `passThreshold`)*

### 4. Lưu ý
- **Guard DELETE Target Profile:** Nếu profile đang được gắn với user (trong bảng `ability_aim_plans`) thì không được xóa, trả về 400 Bad Request.
- `totalUsers` trong danh sách Target Profile là số học viên đang thiết lập mục tiêu này.
- Upsert Threshold: Nếu bản ghi mapping giữa `moduleId` và `targetProfileId` chưa tồn tại thì INSERT, nếu có rồi thì UPDATE field `passThreshold`.

---

## UC-08: THRESHOLD & EVALUATION RULE

### 1. Endpoint - Target Part Threshold (Ngưỡng Diagnostic Test phân loại Part)
- `GET /api/v1/admin/target-part-thresholds`
- `POST /api/v1/admin/target-part-thresholds`
- `PUT /api/v1/admin/target-part-thresholds/{id}`
- `DELETE /api/v1/admin/target-part-thresholds/{id}`

### 2. Endpoint - Ability Evaluation Rule (Quy tắc đánh giá Năng lực)
- `GET /api/v1/admin/ability-evaluation-rules`
- `POST /api/v1/admin/ability-evaluation-rules`
- `PUT /api/v1/admin/ability-evaluation-rules/{id}`
- `DELETE /api/v1/admin/ability-evaluation-rules/{id}`

### 3. Lưu ý Validation
- **Target Part Threshold**: 
  - Đảm bảo tính Unique cho cặp `(partId, targetProfileId)`. Trùng lặp ném `DuplicateResourceException`.
  - Bắt buộc kiểm tra: `weakThreshold < confirmThreshold < passThreshold`. Nếu sai ném `BadRequestException`.
- **Ability Evaluation Rule**:
  - Đảm bảo tính Unique cho `abilityId` (Mỗi năng lực chỉ có 1 quy tắc).
  - Bắt buộc kiểm tra: `developingThreshold < stableThreshold`.
  - Mặc định khi tạo mới nếu không truyền status thì phải là `DRAFT`.

---

## UC-09: DIAGNOSTIC TEST (ĐỀ KIỂM TRA ĐẦU VÀO)

### 1. Endpoints Cần Thiết
- `GET /api/v1/admin/diagnostic-tests` (Lấy danh sách đề thi)
- `GET /api/v1/admin/diagnostic-tests/{id}` (Lấy chi tiết đề thi kèm danh sách `questionIds`)
- `POST /api/v1/admin/diagnostic-tests` (Tạo mới)
- `PUT /api/v1/admin/diagnostic-tests/{id}` (Cập nhật)
- `DELETE /api/v1/admin/diagnostic-tests/{id}` (Xóa)

### 2. Cấu trúc Data
- Frontend sẽ truyền lên: `{ "title": "...", "description": "...", "status": true, "questionIds": [10, 11, 12] }`.

### 3. Lưu ý & Guard
- **POST/PUT**: Khi nhận `questionIds`, Backend tự lưu vào bảng trung gian (`DiagnosticTestQuestion`). Khi cập nhật (PUT), Backend xóa liên kết cũ và insert liên kết mới (hoặc sync data).
- **Guard DELETE**: Tuyệt đối không cho phép xóa `DiagnosticTest` nếu hệ thống ĐÃ CÓ học viên làm bài thi này (tồn tại bản ghi `DiagnosticAttempt` tương ứng).
