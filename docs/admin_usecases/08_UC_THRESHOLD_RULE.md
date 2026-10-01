# UC-08: Threshold & Evaluation Rule

## Mục tiêu
Cấu hình 2 bộ quy tắc:
1. **TargetPartThreshold**: Ngưỡng phân loại kết quả Diagnostic Test theo Part × AIM
2. **AbilityEvaluationRule**: Ngưỡng đánh giá trạng thái năng lực (STABLE/DEVELOPING/WEAK)

---

## Hiểu logic ngưỡng (để BE + FE implement đúng)

### TargetPartThreshold — 4 chỉ thị với 3 ngưỡng:
```
score >= passThreshold          → PASS      (bỏ qua Part này)
confirmThreshold <= score < pass → CONFIRM  (học bình thường)
weakThreshold <= score < confirm → WEAK     (cần tập trung)
score < weakThreshold           → FULL_PART (học toàn bộ Part)
```
> Entity có `passThreshold`, `confirmThreshold`, `weakThreshold` → đủ 3 ngưỡng, 4 vùng.

### AbilityEvaluationRule — 3 trạng thái với 2 ngưỡng:
```
accuracyRate >= stableThreshold             → STABLE
developingThreshold <= rate < stable        → DEVELOPING
rate < developingThreshold                  → WEAK
```

---

## Backend

**Endpoints — TargetPartThreshold:**
```
GET    /api/admin/target-part-thresholds          ← tất cả (không phân trang)
POST   /api/admin/target-part-thresholds          ← tạo mới
PUT    /api/admin/target-part-thresholds/{id}     ← cập nhật
DELETE /api/admin/target-part-thresholds/{id}     ← xóa
```

**Response `data: TargetPartThresholdResponse[]`:**
```json
{
  "id": 1,
  "partId": 1,
  "partName": "Part 1: Photos",
  "targetProfileId": 3,
  "aimName": "AIM 650",
  "passThreshold": 75,
  "confirmThreshold": 50,
  "weakThreshold": 30
}
```

**Request POST/PUT:**
```json
{
  "partId": 1,
  "targetProfileId": 3,
  "passThreshold": 75,
  "confirmThreshold": 50,
  "weakThreshold": 30
}
```

**Validation BE:**
- Unique cặp `(partId, targetProfileId)` → `DuplicateResourceException`
- `weakThreshold < confirmThreshold < passThreshold` → `BadRequestException`
- Cả 3 giá trị trong [0, 100]

---

**Endpoints — AbilityEvaluationRule:**
```
GET    /api/admin/ability-evaluation-rules          ← tất cả
POST   /api/admin/ability-evaluation-rules          ← tạo mới
PUT    /api/admin/ability-evaluation-rules/{id}     ← cập nhật
DELETE /api/admin/ability-evaluation-rules/{id}     ← xóa
```

**Response `data: AbilityEvaluationRuleResponse[]`:**
```json
{
  "id": 1,
  "abilityId": 1,
  "abilityName": "Từ vựng cơ bản",
  "stableThreshold": 80,
  "developingThreshold": 50,
  "status": "PUBLISHED"
}
```

**Request POST/PUT:**
```json
{
  "abilityId": 5,
  "stableThreshold": 80,
  "developingThreshold": 50,
  "status": "PUBLISHED"
}
```

**Validation BE:**
- Unique `abilityId` → 1 rule per ability
- `developingThreshold < stableThreshold`
- Mặc định khi tạo: `status = DRAFT`

---

## Frontend

**Feature folder:** `src/features/admin-threshold/`

**Luồng:**
1. Page mount → fetch cả 2 danh sách song song
2. Render 2 section riêng biệt trên cùng 1 trang (theo UI Figma)
3. Nút "Sửa" → mở modal với data pre-fill
4. Form có validation: kiểm tra `weak < confirm < pass` trước khi submit
5. Dropdown PartId và TargetProfileId → lấy từ UC-03 và UC-07 services

**Service pattern:**
```typescript
export const adminThresholdService = {
    getPartThresholds(): Promise<ApiResponse<TargetPartThresholdResponse[]>> {
        return apiClient.get('/api/admin/target-part-thresholds');
    },
    createPartThreshold(data: CreatePartThresholdRequest): Promise<ApiResponse<TargetPartThresholdResponse>> {
        return apiClient.post('/api/admin/target-part-thresholds', data);
    },
    // update, delete tương tự

    getEvalRules(): Promise<ApiResponse<AbilityEvaluationRuleResponse[]>> {
        return apiClient.get('/api/admin/ability-evaluation-rules');
    },
    createEvalRule(data: CreateEvalRuleRequest): Promise<ApiResponse<AbilityEvaluationRuleResponse>> {
        return apiClient.post('/api/admin/ability-evaluation-rules', data);
    },
    // update, delete tương tự
};
```

**Lưu ý giao tiếp BE–FE:**
- FE phải validate `weak < confirm < pass` ngay trên form (không cần đợi submit) để UX tốt
- Dropdown Part và Ability dropdown tái sử dụng data đã fetch từ UC-03
- Dropdown TargetProfile tái sử dụng từ UC-07
- AbilityEvaluationRule status: chỉ PUBLISHED mới được dùng bởi engine đánh giá → FE nên hiển thị badge rõ ràng
