# UC-02: Quản lý User

## Mục tiêu
Xem danh sách học viên (phân trang + filter), xem chi tiết, thêm mới, cập nhật, toggle active/inactive.

---

## Backend

**Endpoints:**
```
GET    /api/v1/admin/users          ← danh sách + filter + phân trang
GET    /api/v1/admin/users/{id}     ← chi tiết
POST   /api/v1/admin/users          ← tạo mới
PUT    /api/v1/admin/users/{id}     ← cập nhật
PATCH  /api/v1/admin/users/{id}/toggle-active  ← bật/tắt tài khoản
```

**Query params GET danh sách:**
```
?page=0&pageSize=10&keyword=tên_hoặc_email&isActive=true&role=STUDENT&aimScore=650
```

**Response item (GET danh sách) — `data: UserListItemResponse[]`:**
```json
{
  "id": 101,
  "fullName": "Nguyễn Văn Nam",
  "email": "nam@gmail.com",
  "role": "STUDENT",
  "isActive": true,
  "learnerType": "FREE",
  "aimTarget": "AIM 650",
  "createdAt": "2024-10-12T00:00:00"
}
```
> `aimTarget` = JOIN sang `ability_aim_plans → target_profiles.name` của user.  
> Nếu user chưa chọn AIM → `aimTarget: null`.

**Response item (GET chi tiết) — thêm:** `dob`, `avatar`, `username`.

**Request POST/PUT:**
```json
{
  "username": "hocvien01",
  "email": "hocvien01@gmail.com",
  "fullName": "Học Viên 01",
  "password": "Abcd1234!",   ← chỉ POST có field này
  "role": "STUDENT",
  "isActive": false
}
```

**Service guard:**
- Unique `username` + `email` → throw `DuplicateResourceException`
- DELETE không có — chỉ toggle-active (soft deactivate)
- Khi deactivate: tăng `user.tokenVersion` → invalidate JWT cũ ngay lập tức
- Dùng `Specification<User>` để build dynamic WHERE clause (keyword, isActive, role, aimScore)

---

## Frontend

**Feature folder:** `src/features/admin-user/`

**Luồng:**
1. `AdminUserPage` (page) quản lý state: `filters`, `page`, `selectedUser`, `isModalOpen`
2. Khi filter/page thay đổi → gọi lại `adminUserService.getUsers(filters, page)`
3. Bảng hiển thị danh sách, nút edit mở modal với data đã load, nút toggle gọi PATCH
4. Form tạo/sửa dùng `react-hook-form` + `zod` schema validation

**Service pattern:**
```typescript
// services/adminUserService.ts
export const adminUserService = {
    getUsers(params: UserFilterParams): Promise<ApiResponse<UserListItemResponse[]>> {
        return apiClient.get('/api/v1/admin/users', { params });
    },
    getById(id: number): Promise<ApiResponse<UserDetailResponse>> {
        return apiClient.get(`/api/v1/admin/users/${id}`);
    },
    create(data: CreateUserRequest): Promise<ApiResponse<UserDetailResponse>> {
        return apiClient.post('/api/v1/admin/users', data);
    },
    update(id: number, data: UpdateUserRequest): Promise<ApiResponse<UserDetailResponse>> {
        return apiClient.put(`/api/v1/admin/users/${id}`, data);
    },
    toggleActive(id: number): Promise<ApiResponse<{ isActive: boolean }>> {
        return apiClient.patch(`/api/v1/admin/users/${id}/toggle-active`);
    },
};
```

**Pagination:** response có `ApiResponse.pagination` → FE dùng `pagination.totalPages` và `pagination.page` để render component phân trang.

**Lưu ý giao tiếp BE–FE:**
- `aimTarget` nullable → FE hiển thị "—" khi null
- Sau toggle-active thành công → refetch danh sách (không cần reload trang)
- Password chỉ gửi khi POST (tạo mới), PUT không có field password
