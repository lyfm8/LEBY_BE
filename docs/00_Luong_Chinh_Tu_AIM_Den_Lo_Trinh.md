# Luồng Chính: Từ Chọn AIM → Ra Lộ Trình Học

> Mục đích: Nhìn vào là hiểu toàn bộ ý tưởng và DB thay đổi như thế nào ở từng bước.

---

## Tổng Quan (3 Bước Lớn)

```
┌─────────────────┐     ┌──────────────────────┐     ┌─────────────────────┐
│   BƯỚC 1        │     │   BƯỚC 2             │     │   BƯỚC 3            │
│  Chọn AIM       │────▶│  Làm Diagnostic Test │────▶│  Ra Lộ Trình Học   │
│  (Mục tiêu)     │     │  (Bài test đầu vào)  │     │  (Cá nhân hóa)     │
└─────────────────┘     └──────────────────────┘     └─────────────────────┘
```

---

## BƯỚC 1 — Chọn Mục Tiêu AIM

**Người dùng làm gì:** Chọn 1 trong 5 mốc điểm TOEIC mục tiêu

```
UI: 5 card → [450] [550] [650★] [750] [850+]
              Bấm "Xác nhận mục tiêu"
```

**DB thay đổi:**

```
ability_aim_plans
┌────┬─────────┬───────────────────┬────────────┐
│ id │ user_id │ target_profile_id │ created_at │
├────┼─────────┼───────────────────┼────────────┤
│ 1  │   12    │        3 (650)    │  NOW()     │  ← INSERT mới
└────┴─────────┴───────────────────┴────────────┘
```

> `target_profiles` là master data Admin cài sẵn (id=3 ứng với AIM 650).
> `ability_aim_plans` lưu "học viên này đang nhắm tới AIM nào".

---

## BƯỚC 2 — Làm Diagnostic Test (Bài Test Đầu Vào Duy Nhất)

**Người dùng làm gì:** Làm 1 bài test ~40-100 câu bao phủ đủ 7 Part TOEIC

```
UI: Phòng thi → Nghe audio, xem ảnh, đọc văn bản
    Trả lời A/B/C/D → Nộp bài
```

### 2a. Khi bắt đầu lấy đề:

```
diagnostic_attempts
┌─────┬─────────┬────────────────────┬────────────┬──────────────┐
│ id  │ user_id │ diagnostic_test_id │ started_at │ completed_at │
├─────┼─────────┼────────────────────┼────────────┼──────────────┤
│ 501 │   12    │         1          │  NOW()     │    NULL      │  ← INSERT
└─────┴─────────┴────────────────────┴────────────┴──────────────┘
```

### 2b. Khi nộp bài — Bộ máy chấm điểm (toàn bộ trong 1 @Transactional):

**① Chấm điểm 7 Part, so với ngưỡng AIM:**

```
part_diagnostic_results
┌────┬──────────────────────┬─────────┬───────┬────────────┐
│ id │ diagnostic_attempt_id│ part_id │ score │ directive  │
├────┼──────────────────────┼─────────┼───────┼────────────┤
│  1 │         501          │    1    │  90%  │ PASS       │ ← Part 1 ổn
│  2 │         501          │    2    │  75%  │ PASS       │ ← Part 2 ổn
│  3 │         501          │    3    │  55%  │ CONFIRM    │ ← Part 3 gần đạt
│  4 │         501          │    4    │  35%  │ WEAK       │ ← Part 4 yếu
│  5 │         501          │    5    │  80%  │ PASS       │ ← Part 5 ổn
│  6 │         501          │    6    │  62%  │ CONFIRM    │ ← Part 6 gần đạt
│  7 │         501          │    7    │  40%  │ FULL_PART  │ ← Part 7 rất yếu
└────┴──────────────────────┴─────────┴───────┴────────────┘

Luật so sánh (đọc từ target_part_thresholds theo AIM 650):
  score ≥ pass_threshold    → PASS       (bỏ qua, không cần học)
  score ≥ confirm_threshold → CONFIRM    (làm thêm bài xác nhận)
  score thấp hơn 1 mức     → WEAK       (học module đang yếu)
  score quá thấp           → FULL_PART  (học từ nền tảng toàn bộ Part)
```

**② Tính điểm từng Ability (tự suy ra từ câu trả lời — không cần test thêm):**

```
Cách tính: question_abilities biết "câu này đo Ability nào"
           → gom điểm → tính accuracy_rate

user_abilities
┌────┬─────────┬────────────┬───────────────┬────────────────┬───────────┐
│ id │ user_id │ ability_id │ accuracy_rate │ evidence_count │ status    │
├────┼─────────┼────────────┼───────────────┼────────────────┼───────────┤
│  1 │   12    │     1      │     0.85      │      10        │ STABLE    │ ← nghe tranh
│  2 │   12    │     2      │     0.42      │       8        │ WEAK      │ ← nghe chi tiết
│  3 │   12    │     3      │     0.80      │      12        │ STABLE    │ ← ngữ pháp
│  4 │   12    │     4      │     0.38      │       6        │ WEAK      │ ← đọc quét
└────┴─────────┴────────────┴───────────────┴────────────────┴───────────┘

Luật phân loại (đọc từ ability_evaluation_rules):
  accuracy_rate ≥ stable_min  → STABLE
  accuracy_rate ≤ weak_max    → WEAK
  còn lại                     → DEVELOPING
```

**③ Tạo danh sách Module cần học (backlog lộ trình):**

```
ability_aim_plan_items
┌────┬─────────┬──────────────────────┬──────────┐
│ id │ plan_id │      module_id       │ order_no │
├────┼─────────┼──────────────────────┼──────────┤
│  1 │    1    │  5 (Module Part 4)   │    1     │ ← Ability WEAK → cần học
│  2 │    1    │  8 (Module Part 7)   │    2     │ ← Ability WEAK → cần học
└────┴─────────┴──────────────────────┴──────────┘

Cách tìm module_id: Ability WEAK → tra module_abilities → lấy module tương ứng
Chỉ WEAK/DEVELOPING mới được đưa vào. STABLE → bỏ qua.
```

**④ Sinh LearningPath ngay (trong cùng transaction — không cần chờ user vào trang lộ trình):**

```
learning_paths
┌────┬─────────┬────────────┐
│ id │ version │ started_at │
├────┼─────────┼────────────┤
│ 10 │    1    │  NOW()     │  ← INSERT
└────┴─────────┴────────────┘

learning_path_items
┌────┬──────────────────┬─────────────────────┬──────────┬─────────────┐
│ id │ learning_path_id │      module_id       │ order_no │   status    │
├────┼──────────────────┼─────────────────────┼──────────┼─────────────┤
│  1 │       10         │  5 (Module Part 4)  │    1     │ IN_PROGRESS │ ← mở ngay
│  2 │       10         │  8 (Module Part 7)  │    2     │ LOCKED      │ ← khóa
└────┴──────────────────┴─────────────────────┴──────────┴─────────────┘

users (cập nhật):
  learning_path_id = 10   ← gắn lộ trình vào user
```

---

## BƯỚC 3 — Người Dùng Có Lộ Trình Cá Nhân Hóa

**Kết quả người dùng thấy ngay sau khi nộp bài:**

```
Báo cáo chẩn đoán:
  Part 1 ████████████ PASS
  Part 2 ████████░░░░ PASS
  Part 3 ██████░░░░░░ CONFIRM
  Part 4 ████░░░░░░░░ WEAK       ← cần học
  Part 5 █████████░░░ PASS
  Part 6 ███████░░░░░ CONFIRM
  Part 7 ████░░░░░░░░ FULL PART  ← cần học từ đầu

Lộ trình học cá nhân (2 module, sắp xếp theo độ ưu tiên):
  [1] Module Part 4: Kỹ năng nghe độc thoại  ← SẴN SÀNG ▶
  [2] Module Part 7: Đọc hiểu dài             ← ĐANG KHÓA 🔒
        (mở sau khi vượt ải Module 1)
```

---

## Sơ Đồ Quan Hệ CSDL — Từ AIM Đến Lộ Trình

```
target_profiles ─────────────────────────────────────────┐
   (id=3, score=650)                                      │
         │                                                │
         ▼                                                ▼
ability_aim_plans                         target_part_thresholds
   (user + target)                          (ngưỡng điểm 7 Part theo từng AIM)
         │                                            │
         │                                            │ dùng để chấm
         │                                            ▼
         │                               part_diagnostic_results
         │                                  (điểm + directive 7 Part)
         │
         ▼
ability_aim_plan_items ←── question_abilities ←── user_abilities
   (Module cần học)          (câu nào đo Ability)   (Ability WEAK/STABLE)
         │
         │ tra module_abilities
         ▼
learning_path_items ──────────── modules
   (IN_PROGRESS / LOCKED)        (nội dung học)
         │
         ▼
learning_paths ◄──── users.learning_path_id
   (lộ trình của user)
```

---

## Tóm Gọn 1 Câu

> **Chọn AIM** → **Làm 1 bài test duy nhất** → Hệ thống tự chấm 7 Part + tính Ability yếu + tìm Module phù hợp → **Sinh ngay lộ trình cá nhân hóa** → Người dùng học theo thứ tự từ yếu nhất.
