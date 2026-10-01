# UC-01: Admin Dashboard

## Mục tiêu
Trang tổng quan: 4 stat cards, 3 biểu đồ (học viên theo tháng, phân bố AIM, PASS/FAIL Module Test), bảng hoạt động gần nhất.

---

## Backend

**1 endpoint duy nhất:** `GET /api/admin/dashboard/summary` — `@PreAuthorize("hasRole('ADMIN')")`

**Response `data`:**
```json
{
  "stats": {
    "totalUsers": 2847,
    "totalUsersGrowthPercent": 12.5,
    "activeUsersThisMonth": 1420,
    "activeUsersGrowthPercent": 8.3,
    "totalModules": 16,
    "totalTestsCompleted": 12405
  },
  "usersByMonth": [{ "month": "T5", "count": 180 }, ...],
  "aimDistribution": [{ "aimName": "AIM 450", "count": 512, "percent": 18 }, ...],
  "moduleTestPassRate": { "passPercent": 75, "failPercent": 25 },
  "recentActivities": [{
    "userId": 101,
    "fullName": "Nguyễn Nam",
    "action": "Đã hoàn thành Module Test 4",
    "aimTarget": "AIM 650",
    "timeAgo": "5 phút trước",
    "resultBadge": "PASS"
  }]
}
```

**Service tổng hợp từ các repository:**
- COUNT users, so sánh với tháng trước → growthPercent
- GROUP BY MONTH(createdAt) 6 tháng gần nhất → usersByMonth
- GROUP BY targetProfile → aimDistribution
- COUNT attempts WHERE isSubmitted=true → passRate
- TOP 10 attempts ORDER BY submittedAt DESC → recentActivities

---

## Frontend

**Feature folder:** `src/features/admin-dashboard/`

**Luồng:**
1. `AdminDashboardPage` (page) mount → gọi `adminDashboardService.getSummary()`
2. Lưu kết quả vào `useState<DashboardSummary | null>`
3. Truyền props xuống các Presentational components:
   - `StatCardGrid` nhận `stats`
   - `ChartSection` nhận `usersByMonth`, `aimDistribution`, `moduleTestPassRate` → render bằng thư viện chart (cân nhắc thêm `recharts` vào dependencies nếu cần biểu đồ)
   - `RecentActivityTable` nhận `recentActivities`

**Service pattern:**
```typescript
// services/adminDashboardService.ts
export const adminDashboardService = {
    getSummary(): Promise<ApiResponse<DashboardSummary>> {
        return apiClient.get<ApiResponse<DashboardSummary>>('/api/admin/dashboard/summary');
    },
};
```

**Lưu ý giao tiếp BE–FE:**
- `resultBadge` phải là string literal type: `'PASS' | 'NEW' | 'SCORE'`
- `timeAgo` BE tính sẵn bằng tiếng Việt (đơn giản nhất) hoặc FE tính từ `submittedAt` ISO string
- Dashboard không phân trang → `ApiResponse.pagination` = undefined

---

## Điểm chú ý khi làm
- Dashboard là read-only, không có form, chỉ GET
- Nếu chưa có thư viện chart → dùng CSS bar đơn giản trước, thêm recharts sau
- `activeUsersThisMonth` định nghĩa = user login trong 30 ngày (không phải MAU chính xác), cần thống nhất với BE
