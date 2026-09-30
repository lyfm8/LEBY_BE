# UC-07: Target Profile (AIM)

## Mục tiêu
Quản lý các mốc điểm TOEIC mục tiêu (AIM 450/550/650/750/850+). Bên dưới là ma trận ngưỡng điểm pass Module Test theo từng AIM.

---

## Backend

**Endpoints — Target Profile:**
```
GET    /api/v1/admin/target-profiles          ← tất cả profiles (không phân trang, ~5 records)
POST   /api/v1/admin/target-profiles          ← tạo mới
PUT    /api/v1/admin/target-profiles/{id}     ← cập nhật
DELETE /api/v1/admin/target-profiles/{id}     ← xóa
```

**Response `data: TargetProfileResponse[]`:**
```json
{
  "id": 3,
  "name": "Đột phá 650",
  "aimScore": 650,
  "description": "Mục tiêu phổ biến cho doanh nghiệp tuyển dụng...",
  "totalUsers": 1142
}
```
> `totalUsers` = COUNT `ability_aim_plans` WHERE `target_profile_id = ?`

**Guard DELETE:** có user đang dùng profile → `BadRequestException`

---

**Endpoints — Module Target Threshold (ma trận):**
```
GET /api/v1/admin/module-target-thresholds    ← toàn bộ matrix
PUT /api/v1/admin/module-target-thresholds    ← batch upsert
```

**Response GET matrix — `data: ThresholdMatrixResponse`:**
```json
{
  "profileIds": [1, 2, 3, 4, 5],
  "profileNames": ["AIM 450", "AIM 550", "AIM 650", "AIM 750", "AIM 850+"],
  "rows": [
    {
      "moduleId": 1,
      "moduleTitle": "Module 1: Photo Basics",
      "thresholds": { "1": 60, "2": 70, "3": 75, "4": 80, "5": 90 }
    }
  ]
}
```
> Key của `thresholds` = `targetProfileId` (string vì JSON object key).

**Request PUT batch upsert:**
```json
{
  "items": [
    { "moduleId": 1, "targetProfileId": 3, "passThreshold": 78 }
  ]
}
```
> Upsert: nếu chưa có → INSERT, đã có → UPDATE `passThreshold`.

---

## Frontend

**Feature folder:** `src/features/admin-target/`

**Luồng:**
1. Page mount → fetch profiles + fetch threshold matrix song song (2 API calls)
2. Render `AimCardList` (horizontal) từ profiles
3. Render `ThresholdTable`: rows = modules, columns = mỗi AIM một cột
4. Click "Sửa" trên 1 ô → inline edit (input number) hoặc mở modal nhỏ
5. Submit sửa → gọi PUT batch upsert với item vừa thay đổi

**Service pattern:**
```typescript
export const adminTargetProfileService = {
    getAll(): Promise<ApiResponse<TargetProfileResponse[]>> {
        return apiClient.get('/api/v1/admin/target-profiles');
    },
    create(data: CreateTargetProfileRequest): Promise<ApiResponse<TargetProfileResponse>> {
        return apiClient.post('/api/v1/admin/target-profiles', data);
    },
    // update, delete tương tự

    getThresholdMatrix(): Promise<ApiResponse<ThresholdMatrixResponse>> {
        return apiClient.get('/api/v1/admin/module-target-thresholds');
    },
    batchUpdateThresholds(data: BatchUpdateThresholdRequest): Promise<ApiResponse<null>> {
        return apiClient.put('/api/v1/admin/module-target-thresholds', data);
    },
};
```

**Lưu ý giao tiếp BE–FE:**
- Matrix response shape `thresholds: Record<string, number>` (profileId → percent) → FE render theo `profileIds` array để đảm bảo đúng thứ tự cột
- Ô AIM cao nhất trong mỗi row highlight màu cam → FE tự tính max index
- `totalUsers` trên AIM card → hiển thị số học viên đang theo mục tiêu đó
