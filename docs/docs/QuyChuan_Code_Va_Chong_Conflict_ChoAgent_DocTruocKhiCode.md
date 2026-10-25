# HƯỚNG DẪN DÀNH CHO AI CODING AGENT: QUY CHUẨN MÃ NGUỒN & CHỐNG XUNG ĐỘT (ZERO-CONFLICT)
> **Dành riêng cho**: AI Coding Assistant / AI Agent (Claude Code, Cursor, Antigravity, Copilot) khi sinh mã nguồn cho dự án LEBY.  
> **Dự án**: LEBY - Nền tảng học TOEIC cá nhân hóa thích ứng (Adaptive Learning Platform)  
> **Mục tiêu tối thượng**: Sinh code chuẩn doanh nghiệp, tuyệt đối KHÔNG gây xung đột mã nguồn (Zero Merge Conflict) giữa hai nhánh phát triển của **Văn** và **Khoa**.

---

<agent_identity_and_roles>
## 1. PHÂN ĐỊNH VAI TRÒ & RANH GIỚI TRÁCH NHIỆM CỦA AGENT

Khi người dùng yêu cầu lập trình, Agent BẮT BUỘC phải xác định mình đang đóng vai trò nào trong hai kỹ sư:

### 🔹 VAI TRÒ A: AGENT CỦA VĂN (TRỤC HỌC TẬP & NỘI DUNG)
- **Phạm vi Backend (Spring Boot)**:
  - Package: `com.example.be.entity.content.*`, `com.example.be.service.content.*`, `com.example.be.controller.content.*`
  - Các Entity/Service phụ trách: `Part`, `Ability`, `Module`, `Lesson`, `LearningPath`, `LearningPathItem`, `ModuleTargetThreshold`.
  - Thuật toán cốt lõi: `AdaptivePathGeneratorServiceImpl.java` (Sinh lộ trình thích ứng 4 nhánh Tầng 1 và lọc nhị phân Tầng 2).
- **Phạm vi Frontend (React 19 + TypeScript)**:
  - Thư mục được phép sửa đổi:
    - `src/features/roadmap/**`
    - `src/features/lesson/**`
    - `src/features/dashboard/**`
    - `src/features/profile/**`
    - `src/features/admin-part/**`
    - `src/features/admin-module/**`
    - `src/features/admin-threshold/**`
  - **ĐẶC QUYỀN**: Agent của Văn là **Code Owner duy nhất** có quyền chỉnh sửa tập tin định tuyến chung `src/routes/index.tsx`.

### 🔹 VAI TRÒ B: AGENT CỦA KHOA (TRỤC CHẨN ĐOÁN & KHẢO THÍ)
- **Phạm vi Backend (Spring Boot)**:
  - Package: `com.example.be.entity.diagnostic.*`, `com.example.be.service.diagnostic.*`, `com.example.be.controller.diagnostic.*`, `com.example.be.entity.ability.*`
  - Các Entity/Service phụ trách: `DiagnosticTest`, `DiagnosticAttempt`, `DiagnosticAnswer`, `Question`, `Option`, `ModuleTest`, `TargetProfile`, `TargetPartThreshold`, `AbilityEvaluationRule`.
  - Nghiệp vụ cốt lõi: Phòng thi đếm ngược 45 phút, Auto-save câu trả lời, Chấm điểm 2 tầng chẩn đoán, Xáo trộn đề thi vượt ải 25 câu (BR03).
- **Phạm vi Frontend (React 19 + TypeScript)**:
  - Thư mục được phép sửa đổi:
    - `src/features/target/**`
    - `src/features/diagnostic/**`
    - `src/features/module-test/**`
    - `src/features/admin-question/**`
    - `src/features/admin-diagnostic-test/**`
    - `src/features/admin-user/**`
    - `src/features/auth/**`
  - **RÀNG BUỘC TUYỆT ĐỐI**: Khi tạo trang mới, Agent của Khoa CHỈ ĐƯỢC `export default` component từ thư mục `pages/` của feature mình. **CẤM sửa file `src/routes/index.tsx`**.
</agent_identity_and_roles>

---

<hard_invariants>
## 2. CÁC QUY TẮC BẤT BIẾN CHỐNG CONFLICT (HARD CONSTRAINTS)

AI Agent khi sinh code BẮT BUỘC tuân thủ 100% các điều cấm kỵ sau:

### ❌ ĐIỀU CẤM 1: CẤM SỬA FILE DÙNG CHUNG CÙNG MỘT LÚC
- **Tập tin Routes (`src/routes/index.tsx`)**: Chỉ Agent của Văn chỉnh sửa.
- **Tập tin Mock tổng (`src/mocks/handlers.ts`)**:
  - ❌ CẤM viết trực tiếp mock endpoints vào file `handlers.ts`.
  -  BẮT BUỘC: Viết mock vào file tính năng riêng (`src/mocks/handlers/diagnosticHandlers.ts`, `roadmapHandlers.ts`...) rồi import vào mảng tổng.
- **Tập tin API Client (`src/core/api/apiClient.ts`)**: CẤM sửa đổi. Mỗi feature tự viết file service riêng (`diagnosticService.ts`, `roadmapService.ts`).

### ❌ ĐIỀU CẤM 2: CẤM HARDCODE THÔNG SỐ NGHIỆP VỤ TRONG CODE
- ❌ CẤM gán cứng các con số: 80% (passScore Part), 60% (confirmingScore), 50% (developingThreshold), 75% (stableThreshold), 35% (fullPartThreshold), 65 điểm (Module Test passScore), 45 phút / 120 phút.
-  BẮT BUỘC: Đọc động từ Database qua các bảng:
  - `target_part_thresholds`
  - `ability_evaluation_rules`
  - `module_target_thresholds`
  - `diagnostic_tests`
- Gán chú thích rõ ràng trong code: `// ponytail: Thông số cấu hình động từ DB quản trị, không hardcode`.

### ❌ ĐIỀU CẤM 3: CẤM LỆCH CHUẨN TÊN TRƯỜNG DTO (STRICT DTO MAPPING)
AI Agent BẮT BUỘC phải dùng chính xác tên trường sau đây, không được tự ý sáng tạo:

| Nghiệp vụ | Backend Java Entity & DTO | Frontend TypeScript Interface | CẤM DÙNG (TÊN CŨ BỊ LỆCH) |
| :--- | :--- | :--- | :--- |
| **Ngưỡng đỗ Part** | `Float passScore` | `passScore: number;` | ❌ `passThreshold` |
| **Ngưỡng xác nhận Part** | `Float confirmingScore` | `confirmingScore: number;` | ❌ `confirmThreshold` |
| **Ngưỡng học cả Part** | `Float fullPartThreshold` | `fullPartThreshold: number;` | ❌ `weakThreshold` |
| **Ngưỡng Ổn định Ability** | `Float stableMin` | `stableMin: number;` | ❌ `stableThreshold` |
| **Ngưỡng Phát triển Ability** | `Float developingMax` | `developingMax: number;` | ❌ `developingThreshold` |
| **Ngưỡng Yếu Ability** | `Float weakMax` | `weakMax: number;` | ❌ (bỏ trống) |
| **Số bằng chứng tối thiểu** | `Integer minEvidenceCount` | `minEvidenceCount: number;` | ❌ (bỏ trống) |
| **Thứ tự sư phạm Module** | `Integer sequence` | `sequence: number;` | ❌ `orderIndex` |
| **Thời gian thi chẩn đoán** | `Integer durationMinutes` | `durationMinutes: number;` | ❌ `estimatedMinutes` |
</hard_invariants>

---

<backend_standards>
## 3. TIÊU CHUẨN LẬP TRÌNH BACKEND (SPRING BOOT 3.X / JAVA 17)

1. **Kiến trúc phân tầng nghiêm ngặt (Strict Layering)**:
   - `Controller`: Chỉ định tuyến HTTP, validate `@Valid`, gọi Service, trả `ApiResponse<T>`. Tuyệt đối KHÔNG gọi `Repository`.
   - `Service`: Chứa toàn bộ Business Logic, giao tiếp qua Interface.
   - `Repository`: Kế thừa `JpaRepository`.
   - `Entity`: Chỉ map bảng database. CẤM trả trực tiếp Entity ra ngoài Controller (bắt buộc map sang Response DTO).
2. **Tiêm phụ thuộc (Dependency Injection)**:
   - BẮT BUỘC dùng `@RequiredArgsConstructor` trên class với `private final` fields.
   - CẤM dùng `@Autowired` trực tiếp trên field.
3. **Cấu trúc ApiResponse chuẩn hóa**:
   ```java
   public class ApiResponse<T> {
       private boolean success;
       private String message;
       private T data;
       private Pagination pagination;
   }
   ```
4. **Nhập thư viện tường minh (Clean Imports)**:
   - CẤM Wildcard Import: Không dùng `import java.util.*`, `import org.springframework.*`.
   - CẤM Fully Qualified Name (FQN) trong thân code: Không viết `@org.springframework.data...`.
</backend_standards>

---

<frontend_standards>
## 4. TIÊU CHUẨN LẬP TRÌNH FRONTEND (REACT 19 / TYPESCRIPT / VITE)

1. **Cấu trúc Feature-Sliced**:
   - `src/features/<tên-feature>/`:
     - `components/`: Dumb / Presentational components.
     - `pages/`: Smart / Container components kết nối service.
     - `services/`: File gọi API qua `apiClient.ts`.
     - `types/`: Định nghĩa Interface khớp 100% với DTO Backend.
2. **Tuyệt đối không dùng kiểu `any`**:
   - Strict typing trong TypeScript. Sử dụng union types (ví dụ: `type DirectiveType = 'PASS' | 'CONFIRM' | 'WEAK' | 'FULL_PART'`).
3. **Chuyển đổi linh hoạt Mock API và Real API**:
   - Đọc biến môi trường: `import.meta.env.VITE_USE_MOCK === 'true'`.
   - Khi `VITE_USE_MOCK=false`, mọi request được chuyển thẳng tới Backend `http://localhost:8080`.
</frontend_standards>

---

<verification_checklist>
## 5. BỘ LỆNH KIỂM TRA TỰ ĐỘNG DÀNH CHO AI AGENT

Trước khi thông báo hoàn thành nhiệm vụ cho người dùng, AI Agent BẮT BUỘC phải chạy các lệnh kiểm tra sau trong terminal:

### 1. Kiểm tra biên dịch Backend:
```bash
./mvnw clean test-compile
```
*(Yêu cầu: Build Success, không có lỗi cú pháp hoặc thiếu import).*

### 2. Kiểm tra biên dịch và kiểu dữ liệu Frontend:
```bash
npm --prefix LEBY_FE run build
```
*(Yêu cầu: Không có lỗi TypeScript, không có lỗi cú pháp JSX).*

### 3. Kiểm tra trạng thái Git:
```bash
git status
```
*(Yêu cầu: Không có file rác, không chỉnh sửa file ngoài phạm vi được phân công).*
</verification_checklist>
