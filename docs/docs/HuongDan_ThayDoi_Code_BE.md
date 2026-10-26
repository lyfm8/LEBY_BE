# Hướng Dẫn Thay Đổi Code BE Hiện Tại
> **Đọc file này TRƯỚC khi viết bất kỳ dòng code nghiệp vụ nào mới.**
> File này tổng hợp **9 thay đổi kiến trúc và Entity cốt lõi** cần chốt dứt điểm ngay từ đầu.
> Từng mục đều có giải thích chi tiết **"Tại sao bắt buộc phải làm như vậy"** để bạn và nhóm hiểu rõ lý do, tránh việc sau này phải đập đi sửa lại database.

---

## MỤC LỤC 9 THAY ĐỔI CỐT LÕI
1. [Thay đổi 1: Sửa quan hệ User ↔ LearningPath (OneToOne → OneToMany)](#thay-đổi-1--sửa-quan-hệ-user--learningpath-onetoone--onetomany)
2. [Thay đổi 2: Thêm status vào DiagnosticAttempt](#thay-đổi-2--thêm-status-vào-diagnosticattempt)
3. [Thay đổi 3: Tạo Entity DiagnosticAnswer (Lưu chi tiết bài làm)](#thay-đổi-3--tạo-entity-diagnosticanswer-quan-trọng-nhất)
4. [Thay đổi 4: Cập nhật AbilityAimPlan (Bỏ bảng trùng lặp Items)](#thay-đổi-4--cập-nhật-abilityaimplan-thêm-fk-bỏ-items)
5. [Thay đổi 5: Thêm ability_id vào LearningPathItem](#thay-đổi-5--thêm-ability_id-vào-learningpathitem)
6. [Thay đổi 6: Thêm defaultPassScore vào Module](#thay-đổi-6--thêm-defaultpassscore-vào-module-phòng-ngừa-npe)
7. [Thay đổi 7: Thêm isFree vào Lesson (Chuẩn bị Freemium/Thu phí)](#thay-đổi-7--thêm-isfree-vào-lesson-chuẩn-bị-freemium)
8. [Thay đổi 8: Thêm parent_id vào Question (Chùm câu hỏi Part 3, 4, 6, 7)](#thay-đổi-8--thêm-quan-hệ-tự-trỏ-parent_id-vào-question)
9. [Thay đổi 9: Thêm testType và durationMinutes vào DiagnosticTest](#thay-đổi-9--thêm-testtype-và-durationminutes-vào-diagnostictest)
10. [Thay đổi 10: Thuật toán Sinh Lộ Trình Thích Ứng & Bảng Tham Số Động Database](#thay-đổi-10--thuật-toán-sinh-lộ-trình-thích-ứng--bảng-tham-số-động-database)
11. [Phụ lục: Chuẩn hóa JSON giải thích (Transcript) & Quy đổi điểm TOEIC 10-990](#phụ-lục-1--chuẩn-hóa-cấu-trúc-json-explanation-cho-listening--reading)

---

## THAY ĐỔI 1 — Sửa quan hệ `User ↔ LearningPath` (OneToOne → OneToMany)

### Tại sao bắt buộc phải làm như vậy?
* **Vấn đề cũ:** Trong `User.java` cũ đang để `@OneToOne` với `LearningPath`. Điều này có nghĩa là mỗi người dùng trong cả đời chỉ có duy nhất 1 lộ trình học.
* **Hậu quả nếu không sửa:** Khi học viên muốn **Đổi mục tiêu AIM** (ví dụ từ 550 lên 650) hoặc muốn **Làm lại bài kiểm tra đầu vào để học lại từ đầu**, hệ thống sẽ bị lỗi ghi đè dữ liệu, mất sạch lịch sử học tập cũ, hoặc vi phạm ràng buộc Unique Foreign Key trong Database.
* **Giải pháp chuẩn:** Chuyển thành `@OneToMany` (1 User có nhiều LearningPath). Tại mỗi thời điểm chỉ có 1 lộ trình có `status = ACTIVE` (đang học), các lộ trình cũ chuyển sang `ARCHIVED` (đã lưu trữ).

### Code cần sửa:

**File cần sửa:** `src/main/java/com/example/be/entity/user/User.java`
* Xóa đoạn `@OneToOne`:
```java
// XÓA ĐOẠN NÀY:
@OneToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "learning_path_id", nullable = true)
private LearningPath learningPath;
```
* Thay bằng `@OneToMany`:
```java
// THAY BẰNG:
// User 1 ---- * LearningPath (Lịch sử các lộ trình theo thời gian)
@OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
private List<LearningPath> learningPaths = new ArrayList<>();
```

**File cần sửa:** `src/main/java/com/example/be/entity/user/LearningPath.java`
* Thêm quan hệ ngược về User và trường trạng thái:
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
private User user;

// Trạng thái lộ trình: ACTIVE (đang học), ARCHIVED (đã lưu trữ khi đổi AIM), COMPLETED (đã hoàn thành)
@Column(nullable = false, length = 20)
private String status = "ACTIVE";
```

### Migration SQL cần chạy:
```sql
ALTER TABLE learning_paths ADD COLUMN user_id BIGINT NOT NULL DEFAULT 0;
ALTER TABLE learning_paths ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE learning_paths ADD CONSTRAINT fk_lp_user FOREIGN KEY (user_id) REFERENCES users(id);

-- Xóa cột learning_path_id cũ khỏi bảng users
ALTER TABLE users DROP COLUMN learning_path_id;
```

---

## THAY ĐỔI 2 — Thêm `status` vào `DiagnosticAttempt`

### Tại sao bắt buộc phải làm như vậy?
* **Vấn đề cũ:** Bảng `diagnostic_attempts` chỉ có `startedAt` và `completedAt`. Khi học viên đang làm bài, hệ thống không phân biệt được lượt thi này đã nộp bài thành công hay đang làm dở dang hay đã bị hết giờ.
* **Hậu quả nếu không sửa:** Học viên có thể F5 tải lại trang để gian lận thời gian, hoặc gửi request nộp bài nhiều lần (Double-submit) làm thuật toán chấm điểm bị kích hoạt lặp lại nhiều lần.
* **Giải pháp chuẩn:** Thêm enum `EAttemptStatus` gồm các trạng thái rõ ràng: `IN_PROGRESS`, `SUBMITTED`, `TIMED_OUT`.

### Code cần sửa:

**Tạo enum mới:** `src/main/java/com/example/be/enums/diagnostic/EAttemptStatus.java`
```java
package com.example.be.enums.diagnostic;

public enum EAttemptStatus {
    IN_PROGRESS,   // Đang trong thời gian làm bài
    SUBMITTED,     // Học viên chủ động bấm nộp bài
    TIMED_OUT,     // Hết giờ làm bài, máy chủ tự động thu bài
    ABANDONED      // Bỏ thi giữa chừng
}
```

**File cần sửa:** `src/main/java/com/example/be/entity/diagnostic/DiagnosticAttempt.java`
```java
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 20)
private EAttemptStatus status = EAttemptStatus.IN_PROGRESS;

// Thời gian làm bài thực tế tính bằng giây
private Integer durationSeconds;
```

### Migration SQL:
```sql
ALTER TABLE diagnostic_attempts 
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
ADD COLUMN duration_seconds INT NULL;
```

---

## THAY ĐỔI 3 — Tạo Entity `DiagnosticAnswer` (QUAN TRỌNG NHẤT)

### Tại sao bắt buộc phải làm như vậy?
* **Vấn đề cũ:** Trong mã nguồn ban đầu có `ModuleTestAnswer` (lưu bài làm thi module), nhưng **HOÀN TOÀN KHÔNG CÓ `DiagnosticAnswer`** để lưu câu trả lời bài kiểm tra chẩn đoán! Hệ thống cũ chỉ lưu điểm tổng của từng Part trong `PartDiagnosticResult`.
* **Hậu quả nếu không sửa:**
  1. Học viên nộp bài xong **không thể bấm xem lại bài thi** xem mình làm đúng câu nào, sai câu nào!
  2. Không thể làm tính năng **"Lưu nháp tức thì (Auto-save)"** khi đang làm bài.
  3. Không thể làm tính năng **"Sổ tay câu sai (Mistake Review)"** để học viên ôn tập lại các câu đã làm sai.
  4. Khi học viên đổi AIM, không thể tái sử dụng bài làm cũ mà bắt học viên phải ngồi làm lại từ đầu!
* **Giải pháp chuẩn:** Tạo mới Entity `DiagnosticAnswer` lưu chi tiết: lần thi nào, câu hỏi nào, chọn đáp án gì, đúng hay sai.

### Code tạo mới:

**Tạo file mới:** `src/main/java/com/example/be/entity/diagnostic/DiagnosticAnswer.java`
```java
package com.example.be.entity.diagnostic;

import com.example.be.entity.question.Question;
import tools.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Lưu câu trả lời chi tiết của học viên trong bài Diagnostic Test & Mock Test.
 */
@Entity
@Table(name = "diagnostic_answers")
@Getter
@Setter
@NoArgsConstructor
public class DiagnosticAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Đáp án học viên chọn (ví dụ: {"selected": "A"}).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    private JsonNode userAnswer;

    /**
     * Kết quả đúng hay sai.
     */
    @Column(nullable = false)
    private Boolean isCorrect;

    /**
     * Cờ đánh dấu câu hỏi này học viên có muốn xem lại không.
     */
    @Column(nullable = false)
    private Boolean isFlagged = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagnostic_attempt_id", nullable = false)
    private DiagnosticAttempt diagnosticAttempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;
}
```

**Thêm quan hệ vào `DiagnosticAttempt.java`:**
```java
@OneToMany(mappedBy = "diagnosticAttempt", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
private List<DiagnosticAnswer> answers = new ArrayList<>();
```

### Migration SQL:
```sql
CREATE TABLE diagnostic_answers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_answer JSON NULL,
    is_correct BOOLEAN NOT NULL,
    is_flagged BOOLEAN NOT NULL DEFAULT FALSE,
    diagnostic_attempt_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    CONSTRAINT fk_da_attempt FOREIGN KEY (diagnostic_attempt_id) REFERENCES diagnostic_attempts(id),
    CONSTRAINT fk_da_question FOREIGN KEY (question_id) REFERENCES questions(id)
);
```

---

## THAY ĐỔI 4 — Cập nhật `AbilityAimPlan` (Thêm FK, bỏ Items)

### Tại sao bắt buộc phải làm như vậy?
* **Vấn đề cũ:** Bảng `ability_aim_plan_items` chứa các trường `(module_id, order_no, plan_id)`. Bảng này hoàn toàn **trùng lặp 100% về mặt ý nghĩa** với bảng `learning_path_items` (cũng chứa `module_id, order_no, learning_path_id`).
* **Hậu quả nếu không sửa:** Lập trình viên phải duy trì 2 bảng giống hệt nhau, khi học viên hoàn thành 1 module phải cập nhật cả 2 nơi, rất dễ gây lệch dữ liệu (Desync data).
* **Giải pháp chuẩn:** Xóa bỏ bảng thừa `ability_aim_plan_items`. Bảng `ability_aim_plans` chỉ đóng vai trò là "Bản ghi kế hoạch mục tiêu" liên kết giữa `User`, `TargetProfile` (AIM đã chọn), `DiagnosticAttempt` (bài thi cơ sở) và `LearningPath` (lộ trình sinh ra).

### Code cần sửa:

**File cần sửa:** `src/main/java/com/example/be/entity/ability/AbilityAimPlan.java`
* Xóa quan hệ `items` và thêm các trường liên kết:
```java
// XÓA: private List<AbilityAimPlanItem> items;

@OneToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "diagnostic_attempt_id", nullable = true)
private DiagnosticAttempt diagnosticAttempt;

@OneToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "learning_path_id", nullable = true)
private LearningPath learningPath;

@CreationTimestamp
private LocalDateTime createdAt;
```

### Migration SQL:
```sql
ALTER TABLE ability_aim_plans 
ADD COLUMN diagnostic_attempt_id BIGINT NULL,
ADD COLUMN learning_path_id BIGINT NULL,
ADD COLUMN created_at DATETIME NULL DEFAULT NOW();

ALTER TABLE ability_aim_plans 
ADD CONSTRAINT fk_aap_attempt FOREIGN KEY (diagnostic_attempt_id) REFERENCES diagnostic_attempts(id),
ADD CONSTRAINT fk_aap_lp FOREIGN KEY (learning_path_id) REFERENCES learning_paths(id);

DROP TABLE IF EXISTS ability_aim_plan_items;
```

---

## THAY ĐỔI 5 — Thêm `ability_id` vào `LearningPathItem`

### Tại sao bắt buộc phải làm như vậy?
* **Vấn đề sau khi bỏ `AbilityAimPlanItem`:** Ta cần biết *"Module này được đưa vào lộ trình học là nhằm khắc phục năng lực (Ability) yếu nào của học viên?"* để sau này hiển thị lên giao diện (ví dụ: *"Module này giúp bạn luyện Kỹ năng nghe chi tiết của Part 3"*).
* **Giải pháp chuẩn:** Thêm trực tiếp trường `ability_id` vào `LearningPathItem`.

### Code cần sửa:

**File cần sửa:** `src/main/java/com/example/be/entity/user/LearningPathItem.java`
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "ability_id", nullable = true)
private Ability ability;
```

### Migration SQL:
```sql
ALTER TABLE learning_path_items ADD COLUMN ability_id BIGINT NULL;
ALTER TABLE learning_path_items ADD CONSTRAINT fk_lpi_ability FOREIGN KEY (ability_id) REFERENCES abilities(id);
```

---

## THAY ĐỔI 6 — Thêm `defaultPassScore` vào `Module` (Phòng ngừa NPE)

### Tại sao bắt buộc phải làm như vậy?
* **Vấn đề cũ:** Điểm đỗ (Pass Score) của Module Test hiện tại được tra cứu từ bảng `ModuleTargetThreshold` (điểm đỗ theo từng AIM). Nếu Admin tạo một Module mới mà chưa kịp cấu hình bảng ngưỡng cho nó, khi học viên làm bài thi vượt ải, hệ thống gọi `threshold.getPassScore()` sẽ bị lỗi `NullPointerException (NPE)` làm sập API!
* **Giải pháp chuẩn:** Thêm trường `defaultPassScore = 65` ngay trên bảng `Module` làm giá trị an toàn dự phòng (Fallback).

### Code cần sửa:

**File cần sửa:** `src/main/java/com/example/be/entity/content/Module.java`
```java
@Column(nullable = false, columnDefinition = "INT DEFAULT 65")
private Integer defaultPassScore = 65;
```

### Migration SQL:
```sql
ALTER TABLE modules ADD COLUMN default_pass_score INT NOT NULL DEFAULT 65;
```

---

## THAY ĐỔI 7 — Thêm `isFree` vào `Lesson` (Chuẩn bị Freemium)

### Tại sao bắt buộc phải làm như vậy?
* **Mục đích:** Bạn yêu cầu chừa sẵn đường mở rộng để sau này bán gói thu phí mà **không phải sửa đổi cấu trúc Database**.
* **Cách vận hành 2 giai đoạn:**
  * **Giai đoạn 1 (Hiện tại - Đồ án):** Cột `is_free` mặc định là `true` cho tất cả các bài học. Logic khóa bài hiện tại chỉ dựa vào việc học viên đã hoàn thành bài liền trước chưa (`lockReason = "PREVIOUS_INCOMPLETE"`). Hệ thống không có cổng thanh toán.
  * **Giai đoạn 2 (Sau này - Bán gói):** Admin chỉ cần vào màn hình sửa các bài nâng cao thành `is_free = false`. Backend chỉ cần thêm đúng 1 dòng kiểm tra: `if (!lesson.getIsFree() && !user.isVip()) return LOCKED_PAYMENT`. Toàn bộ giao diện và DB cũ giữ nguyên 100%!

### Code cần sửa:

**File cần sửa:** `src/main/java/com/example/be/entity/content/Lesson.java`
```java
@Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
private Boolean isFree = true;
```

### Migration SQL:
```sql
ALTER TABLE lessons ADD COLUMN is_free BOOLEAN NOT NULL DEFAULT TRUE;
```

---

## THAY ĐỔI 8 — Thêm quan hệ tự trỏ `parent_id` vào `Question`

### Tại sao bắt buộc phải làm như vậy?
* **Vấn đề cốt lõi của đề thi TOEIC:** TOEIC chia làm 2 dạng câu hỏi:
  1. Câu đơn độc lập: Part 1, 2, 5.
  2. Chùm câu hỏi (Group / Item Set): Part 3, 4 (nghe 1 audio trả lời 3 câu) và Part 6, 7 (đọc 1-3 đoạn văn trả lời 2-5 câu).
* **Hậu quả nếu không có `parent_id`:** Lập trình viên sẽ phải copy-paste toàn bộ đoạn văn đọc hiểu Part 7 vào tất cả 5 câu hỏi con. Dữ liệu bị trùng lặp 5 lần và Frontend **hoàn toàn không biết cách chia đôi màn hình (Split-screen: bài đọc bên trái, 5 câu hỏi bên phải)**.
* **Giải pháp chuẩn:** Dùng quan hệ tự trỏ (Self-referencing):
  * **Bản ghi Cha (`parent_id = NULL`):** Đóng vai trò là bài đọc chung (chứa đoạn văn `passage`) hoặc bài nghe chung (chứa `audioUrl`).
  * **Các bản ghi Con (`parent_id = ID_CHA`):** Chính là từng câu hỏi trắc nghiệm A/B/C/D cụ thể.

### Code cần sửa:

**File cần sửa:** `src/main/java/com/example/be/entity/question/Question.java`
```java
/**
 * Câu hỏi cha chứa nội dung bài đọc (Part 6, 7) hoặc audio chung (Part 3, 4).
 * Nếu là câu hỏi độc lập (Part 1, 2, 5) thì parent = null.
 */
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "parent_id", nullable = true)
private Question parent;

@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
private List<Question> subQuestions = new ArrayList<>();
```

### Migration SQL:
```sql
ALTER TABLE questions ADD COLUMN parent_id BIGINT NULL;
ALTER TABLE questions ADD CONSTRAINT fk_question_parent FOREIGN KEY (parent_id) REFERENCES questions(id);
```

---

## THAY ĐỔI 9 — Thêm `testType` và `durationMinutes` vào `DiagnosticTest`

### Tại sao bắt buộc phải làm như vậy?
* **Yêu cầu thực tế:** Hệ thống cần có cả **Bài thi chẩn đoán đầu vào (40 - 60 câu, 45 phút)** lẫn **Bài thi thử tốt nghiệp đầu ra y chang thi thật (200 câu, 120 phút)**.
* **Giải pháp cực kỳ thông minh:** Bảng `diagnostic_tests` vốn dĩ đã là một bộ khung đề thi liên kết với câu hỏi qua `diagnostic_test_questions`. Ta chỉ cần thêm 2 trường này là **TÁI SỬ DỤNG 100% HẠ TẦNG PHÒNG THI** (đồng hồ, lưu nháp, nộp bài, báo cáo điểm) cho cả 2 loại đề mà không tốn công tạo thêm bất kỳ bảng mới nào!

### Code cần sửa:

**Tạo enum mới:** `src/main/java/com/example/be/enums/diagnostic/ETestType.java`
```java
package com.example.be.enums.diagnostic;

public enum ETestType {
    DIAGNOSTIC,       // Đề thi chẩn đoán phân loại đầu vào (ít câu, 45 phút)
    FULL_MOCK_TEST    // Đề thi thử TOEIC toàn diện đầu ra chuẩn ETS (200 câu, 120 phút)
}
```

**File cần sửa:** `src/main/java/com/example/be/entity/diagnostic/DiagnosticTest.java`
```java
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 30)
private ETestType testType = ETestType.DIAGNOSTIC;

@Column(nullable = false, columnDefinition = "INT DEFAULT 45")
private Integer durationMinutes = 45;
```

### Migration SQL:
```sql
ALTER TABLE diagnostic_tests 
ADD COLUMN test_type VARCHAR(30) NOT NULL DEFAULT 'DIAGNOSTIC',
ADD COLUMN duration_minutes INT NOT NULL DEFAULT 45;
```

---

---

## THAY ĐỔI 10 — Thuật toán Sinh Lộ Trình Thích Ứng & Bảng Tham Số Động Database

> [!CAUTION]
> **CẢNH BÁO QUAN TRỌNG DÀNH CHO DEV (KHOA & VĂN): TUYỆT ĐỐI KHÔNG HARDCODE CÁC THÔNG SỐ TRONG JAVA CODE!**
> Tất cả các con số xuất hiện trong tài liệu (80%, 60%, 50%, 75%, 35%, 65 điểm, 45 phút, 120 phút...) **chỉ là Dữ liệu Mẫu Ban Đầu (Default Seed Data)**.
> Hệ thống được thiết kế để **Giáo viên / Quản trị viên toàn quyền thay đổi linh hoạt trong Database** qua giao diện Admin Web mà không cần dev phải sửa code hay deploy lại server.

### 1. Bảng Tra Cứu Các Tham Số Cấu Hình Động Trong Toàn Bộ Hệ Thống

| STT | Tham số nghiệp vụ | Giá trị Seed Data mẫu | Ai có quyền thay đổi? | Thay đổi tại đâu (Màn hình Use Case)? | Bảng và Cột lưu trong Database |
| :---: | :--- | :---: | :---: | :--- | :--- |
| **1** | **Ngưỡng điểm Part theo AIM**<br>- Điểm đạt chuẩn (`passScore`)<br>- Điểm cận chuẩn (`confirmingScore`)<br>- Điểm mất gốc (`fullPartThreshold`) | 80%<br>60%<br>35% | **Giáo viên / Admin** | **UC A08**: Quản lý Hồ sơ AIM & Ngưỡng phần thi | Bảng `target_part_thresholds`:<br>- `pass_score`<br>- `confirming_score`<br>- `full_part_threshold` |
| **2** | **Ngưỡng lọc Năng lực Ability**<br>- Ngưỡng lọc khi Part là WEAK (`stableThreshold`)<br>- Ngưỡng lọc khi Part là CONFIRM (`developingThreshold`) | 75%<br><br>50% | **Giáo viên / Admin** | **UC A10**: Quản lý Quy tắc đánh giá năng lực | Bảng `ability_evaluation_rules`:<br>- `stable_threshold`<br>- `developing_threshold` |
| **3** | **Điểm chuẩn đỗ bài thi vượt ải**<br>- Điểm đỗ theo từng AIM<br>- Điểm đỗ mặc định toàn hệ thống | 65 / 100<br>65 / 100 | **Giáo viên / Admin** | **UC A09**: Quản lý Ngưỡng đỗ Module<br>**UC A05**: Quản lý Module | Bảng `module_target_thresholds`: `pass_score`<br>Bảng `modules`: `default_pass_score` |
| **4** | **Điều kiện mở khóa thi vượt ải**<br>Tỷ lệ hoàn thành bài học trong Module | >= 80% số bài giảng | **Giáo viên / Admin** | **UC A05**: Quản lý Module & Bài giảng | Cấu hình cài đặt module (mặc định 80%) |
| **5** | **Thời gian làm bài thi**<br>- Đề thi chẩn đoán (Diagnostic)<br>- Đề thi vượt ải (Module Test)<br>- Đề thi thử (Full Mock Test 200 câu) | 45 phút<br>25 phút<br>120 phút | **Giáo viên / Admin** (khi tạo/sửa đề) | **UC A06**: Quản lý Đề thi chẩn đoán & Thi thử<br>**UC A07**: Quản lý Bài thi vượt ải | Bảng `diagnostic_tests`: `duration_minutes`<br>Bảng `modules`: `duration_minutes` |
| **6** | **Số lượng câu hỏi trong đề thi**<br>- Đề chẩn đoán đầu vào<br>- Đề thi vượt ải Module Test<br>- Đề thi thử ETS toàn diện | 40 - 60 câu<br>20 - 25 câu<br>200 câu | **Giáo viên / Admin** (chọn từ Question Bank) | **UC A04**: Ngân hàng câu hỏi<br>**UC A06**: Đề thi chẩn đoán<br>**UC A07**: Bài thi vượt ải | Bảng `diagnostic_test_questions`<br>Bảng `module_test_questions` |
| **7** | **Thời hạn hiệu lực mã xác thực OTP** | 10 phút (tối đa 5 lần nhập sai) | **Quản trị hệ thống** | Cấu hình tham số backend | File `application.yml` (`security.otp.expiration-minutes`) |

---

### 2. Thuật toán Sinh Lộ Trình Thích Ứng (Adaptive Path Generation)

Sau khi học viên nộp bài Diagnostic Test, service `AdaptivePathGeneratorService` sẽ:
1. Đọc kết quả từng Part đối chiếu với bảng `target_part_thresholds` (lấy giá trị động từ DB theo AIM) để phân thành **4 nhánh Tầng 1**:
   - `PASS`: Đạt chuẩn AIM -> Bỏ qua toàn bộ Part, không xét Tầng 2 (0 module).
   - `CONFIRM`: Cận chuẩn -> Xuống Tầng 2 lọc với ngưỡng khắt khe `developingThreshold` (lấy từ DB, Seed Data mẫu 50%).
   - `ABILITY_DIAGNOSTIC` (WEAK): Dưới chuẩn -> Xuống Tầng 2 lọc với ngưỡng an toàn `stableThreshold` (lấy từ DB, Seed Data mẫu 75%).
   - `FULL_PART`: Mất gốc -> Bốc toàn bộ 100% module của Part theo `sequence` gốc, không cần lọc Tầng 2.
2. Tầng 2 đánh giá **Nhị phân 2 cấp (Binary: STABLE vs WEAK)**:
   - Khử trùng lặp Module bằng `Map<Long, Module>` hoặc `Set<Long>`.
3. Sắp xếp danh sách Module theo **`Module.sequence` chuẩn sư phạm** (bài cơ bản trước, bài nâng cao sau). Tuyệt đối không đảo lộn lung tung.
4. Đánh số `orderNo = 1, 2, 3...`, mở khóa bài đầu tiên (`IN_PROGRESS`), khóa các bài sau (`LOCKED`).

### Code Java hoàn chỉnh của Service (Sẵn sàng triển khai ở Tuần 5):

```java
package com.example.be.service.learningpath.implement;

import com.example.be.entity.ability.AbilityEvaluationRule;
import com.example.be.entity.ability.TargetPartThreshold;
import com.example.be.entity.content.Module;
import com.example.be.entity.diagnostic.DiagnosticAttempt;
import com.example.be.entity.diagnostic.PartDiagnosticResult;
import com.example.be.entity.user.LearningPath;
import com.example.be.entity.user.LearningPathItem;
import com.example.be.enums.content.ELessonStatus;
import com.example.be.enums.diagnostic.EPartDirective;
import com.example.be.repository.ability.AbilityEvaluationRuleRepository;
import com.example.be.repository.ability.TargetPartThresholdRepository;
import com.example.be.repository.content.ModuleRepository;
import com.example.be.repository.user.LearningPathItemRepository;
import com.example.be.repository.user.LearningPathRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdaptivePathGeneratorServiceImpl {

    private final TargetPartThresholdRepository targetPartThresholdRepo;
    private final AbilityEvaluationRuleRepository abilityRuleRepo;
    private final ModuleRepository moduleRepo;
    private final LearningPathRepository learningPathRepo;
    private final LearningPathItemRepository learningPathItemRepo;

    @Transactional
    public LearningPath generateAdaptivePath(DiagnosticAttempt attempt, Long targetProfileId) {
        LearningPath learningPath = new LearningPath();
        learningPath.setUser(attempt.getUser());
        learningPath.setStatus(LearningPath.EStatus.ACTIVE);
        learningPath = learningPathRepo.save(learningPath);

        // Tập hợp chứa các Module đã chọn để tự động khử trùng lặp
        Map<Long, Module> selectedModuleMap = new LinkedHashMap<>();

        // Quét từng Part trong kết quả thi chẩn đoán (7 Part)
        for (PartDiagnosticResult partResult : attempt.getPartResults()) {
            Long partId = partResult.getPart().getId();
            float partScore = partResult.getScore();

            // ĐỌC NGƯỠNG ĐỘNG TỪ DATABASE THEO AIM ĐÃ CHỌN (KHÔNG HARDCODE)
            TargetPartThreshold threshold = targetPartThresholdRepo
                    .findByTargetProfileIdAndPartId(targetProfileId, partId)
                    .orElseThrow(() -> new IllegalStateException("Chưa cấu hình ngưỡng cho Part " + partId));

            EPartDirective directive;
            if (partScore >= threshold.getPassScore()) {
                directive = EPartDirective.PASS;
            } else if (threshold.getConfirmingScore() != null && partScore >= threshold.getConfirmingScore()) {
                directive = EPartDirective.CONFIRM;
            } else if (threshold.getFullPartThreshold() != null && partScore < threshold.getFullPartThreshold()) {
                directive = EPartDirective.FULL_PART;
            } else {
                directive = EPartDirective.ABILITY_DIAGNOSTIC;
            }
            partResult.setDirective(directive);

            // XỬ LÝ 4 NHÁNH TẦNG 1
            switch (directive) {
                case PASS:
                    // Nhánh 1: Đạt chuẩn -> MIỄN HỌC 100% Part này (0 module)
                    log.info("Part {} đạt chuẩn PASS ({}%), miễn học", partId, partScore);
                    break;

                case FULL_PART:
                    // Nhánh 4: Mất gốc (< fullPartThreshold) -> Bốc toàn bộ 100% module của Part
                    log.info("Part {} mất gốc FULL_PART ({}%), bốc toàn bộ module", partId, partScore);
                    List<Module> allModulesOfPart = moduleRepo.findByPartIdOrderBySequenceAsc(partId);
                    for (Module m : allModulesOfPart) {
                        selectedModuleMap.put(m.getId(), m);
                    }
                    break;

                case CONFIRM:
                case ABILITY_DIAGNOSTIC:
                    // Nhánh 2 & 3: Xuống Tầng 2 soi Ability vi mô
                    // CONFIRM dùng ngưỡng developingThreshold (DB mẫu 50%)
                    // ABILITY_DIAGNOSTIC dùng ngưỡng stableThreshold (DB mẫu 75%)
                    processAbilityTier(partResult, directive, selectedModuleMap);
                    break;
            }
        }

        // BƯỚC SẮP XẾP SƯ PHẠM BẤT DI BẤT DỊCH: Theo Module.sequence
        List<Module> finalModuleList = new ArrayList<>(selectedModuleMap.values());
        finalModuleList.sort(Comparator.comparing(Module::getSequence));

        // TẠO CÁC BẢN GHI LEARNING PATH ITEMS (Gán orderNo tăng dần)
        int order = 1;
        for (Module m : finalModuleList) {
            LearningPathItem item = new LearningPathItem();
            item.setLearningPath(learningPath);
            item.setModule(m);
            item.setOrderNo(order);
            item.setProgressPercent(0);
            item.setIsCorrect(false);
            // Module đầu tiên mở khóa sẵn, các module sau bị khóa
            item.setStatus(order == 1 ? ELessonStatus.IN_PROGRESS : ELessonStatus.LOCKED);
            learningPathItemRepo.save(item);
            order++;
        }

        return learningPath;
    }

    private void processAbilityTier(PartDiagnosticResult partResult, EPartDirective directive, Map<Long, Module> selectedModuleMap) {
        // Giả sử logic bóc tách điểm các Ability con trong Part này từ bài làm của học viên
        List<AbilityScoreDTO> abilityScores = calculateAbilityScores(partResult);

        for (AbilityScoreDTO abilityScore : abilityScores) {
            // ĐỌC QUY TẮC NGƯỠNG ĐỘNG TỪ BẢNG ability_evaluation_rules (KHÔNG HARDCODE)
            AbilityEvaluationRule rule = abilityRuleRepo.findByAbilityId(abilityScore.getAbilityId())
                    .orElse(null);

            float cutoffThreshold;
            if (rule != null) {
                // Nhánh CONFIRM lấy developingThreshold (ví dụ 50%), Nhánh WEAK lấy stableThreshold (ví dụ 75%)
                cutoffThreshold = (directive == EPartDirective.CONFIRM) 
                        ? rule.getDevelopingThreshold() 
                        : rule.getStableThreshold();
            } else {
                cutoffThreshold = (directive == EPartDirective.CONFIRM) ? 50.0f : 75.0f;
            }

            // ĐÁNH GIÁ NHỊ PHÂN: Nếu tỷ lệ đúng < ngưỡng cắt -> BỐC MODULE
            if (abilityScore.getAccuracyRate() < cutoffThreshold) {
                List<Module> modulesForAbility = moduleRepo.findByAbilityId(abilityScore.getAbilityId());
                for (Module m : modulesForAbility) {
                    selectedModuleMap.put(m.getId(), m); // Tự động khử trùng lặp qua Map
                }
            }
        }
    }
}
```

---

## PHỤ LỤC 1 — Chuẩn hóa cấu trúc JSON `explanation` cho Listening & Reading

Trong bảng `questions`, trường `explanation` kiểu JSON cần tuân thủ cấu trúc chuẩn sau để Frontend hiển thị đầy đủ Lời thoại và Bản dịch:

```json
{
  "transcript": "M: Good morning. I'm calling about the conference room booking...\nW: Yes, Mr. Tanaka. The room on the 4th floor is available.",
  "translation": "Nam: Chào buổi sáng. Tôi gọi về việc đặt phòng hội thảo...\nNữ: Vâng, chào anh Tanaka. Phòng ở tầng 4 đang còn trống.",
  "clue": "Người phụ nữ nói 'The room on the 4th floor is available' -> Đáp án C là chính xác.",
  "analysis": "Cần chú ý từ khóa 'conference room' và 'available'. Đáp án A và B là bẫy về thời gian.",
  "vocabulary": [
    {"word": "conference", "type": "n", "meaning": "hội thảo"},
    {"word": "available", "type": "adj", "meaning": "có sẵn, còn trống"}
  ]
}
```

---

## PHỤ LỤC 2 — Bảng quy đổi điểm TOEIC chuẩn (10 – 990)

Tạo file cấu hình hoặc bảng Master Data `toeic_score_conversions` để chuyển số câu đúng sang điểm chuẩn TOEIC:

```sql
CREATE TABLE toeic_score_conversions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    correct_count INT NOT NULL UNIQUE,
    listening_score INT NOT NULL,
    reading_score INT NOT NULL
);

-- Ví dụ nạp dữ liệu chuẩn ETS:
-- Đúng 1 câu: Listening 5, Reading 5
-- Đúng 50 câu: Listening 245, Reading 210
-- Đúng 75 câu: Listening 395, Reading 355
-- Đúng 100 câu: Listening 495, Reading 495
```

---

## THỨ TỰ THỰC HIỆN KHUYÊN NGHỊ CHO DEV (THEO ĐÚNG TUẦN 1)

```
Bước 1: Tạo các Enum mới (EAttemptStatus, ETestType)
Bước 2: Thay đổi 2, 6, 7, 8, 9 (Thêm các cột mới vào DiagnosticAttempt, Module, Lesson, Question, DiagnosticTest)
Bước 3: Thay đổi 3 (Tạo entity mới DiagnosticAnswer)
Bước 4: Thay đổi 5 (Thêm ability_id vào LearningPathItem)
Bước 5: Thay đổi 4 (Cập nhật AbilityAimPlan, xóa bỏ bảng thừa)
Bước 6: Thay đổi 1 (Sửa quan hệ User ↔ LearningPath thành OneToMany)
```

> 💡 **Quy tắc an toàn:** Sau khi sửa xong từng entity, chạy lệnh `./mvnw compile` để kiểm tra lỗi cú pháp trước khi chạy migration vào MySQL.
