# SPRING BOOT JPA ENTITY GENERATION RULES

## 1. SOURCE OF TRUTH

Khi sinh Entity từ các diagram được cung cấp, tuân thủ thứ tự ưu tiên sau:

1. Class Diagram là nguồn chính để xác định:
   - Entity/Class
   - Attribute
   - Data type
   - Primary Key
   - Relationship
   - Cardinality
   - Inheritance
   - Enumeration
   - Composition/Aggregation

2. Use Case Diagram là nguồn để:
   - Xác nhận business relationship
   - Xác nhận ownership
   - Xác nhận trạng thái nghiệp vụ
   - Xác nhận các constraint như "chỉ được làm một lần", "có thể làm nhiều lần", "phải hoàn thành trước khi mở khóa", v.v.
   - Phát hiện những relationship/business rule còn thiếu trong Class Diagram.

3. Không được tự ý tạo Entity mới chỉ vì một Use Case có tên giống một đối tượng nghiệp vụ.

4. Không được tự ý thêm/xóa attribute hoặc relationship nếu không có căn cứ từ Class Diagram hoặc business rule rõ ràng.

5. Nếu Class Diagram và Use Case Diagram mâu thuẫn:
   - Không tự sửa.
   - Báo rõ conflict.
   - Đề xuất phương án.
   - Chỉ code sau khi conflict đã được giải quyết.

---

# 2. ENTITY IDENTIFICATION

Chỉ tạo `@Entity` cho các class đại diện cho persistent domain object cần lưu trong database.

Các class sau không tự động trở thành Entity:

- Actor: User, Student, Admin nếu chỉ là role của cùng một User.
- Use Case.
- Service.
- DTO.
- Request/Response.
- Utility.
- Enum.
- Abstract class nếu được thiết kế chỉ làm superclass dùng chung và không cần bảng riêng.

Nếu một class trong Class Diagram có ID và có relationship với các entity khác, mặc định xem xét nó là persistent Entity.

---

# 3. ABSTRACT CLASS

Nếu Class Diagram đánh dấu:

`<<abstract>>`

thì phải kiểm tra inheritance trước khi sinh Entity.

Không được tự động tạo một Entity/table riêng cho abstract class.

Có 3 trường hợp:

### Case A - Abstract class chỉ dùng để chia sẻ field

Sử dụng:

```java
@MappedSuperclass
```

### Case B - Abstract class là domain entity thật sự và subclasses cần polymorphic persistence

Sử dụng:

```java
@Entity
@Inheritance(...)
```

Inheritance strategy phải được chỉ định rõ trước khi code:

- `SINGLE_TABLE`
- `JOINED`
- `TABLE_PER_CLASS`

Không được tự ý chọn strategy nếu requirement chưa xác định.

### Case C - Abstract class chỉ dùng cho Java abstraction

Không tạo table riêng.

---

# 4. PRIMARY KEY

Mặc định sử dụng:

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

Trừ khi Class Diagram chỉ rõ một chiến lược khác.

Không sử dụng `String UUID` nếu diagram không yêu cầu UUID.

Không tạo composite key nếu Class Diagram không thể hiện composite primary key.

Tên PK mặc định:

```text
id
```

---

# 5. DATA TYPE MAPPING

Mapping mặc định:

| Class Diagram | Java |
|---|---|
| Long | Long |
| Integer / int | Integer |
| Float | Float |
| Double | Double |
| Boolean / boolean | Boolean |
| String | String |
| Date | LocalDate |
| DateTime | LocalDateTime |
| Timestamp | LocalDateTime |
| JSON | JsonNode / JSON mapping |
| Enum | Java enum |

Không tự đổi kiểu dữ liệu nếu không có lý do rõ ràng.

---

# 6. ENUM

Mọi Enumeration trong Class Diagram phải được tạo thành Java enum.

Ví dụ:

```java
public enum ELearnerType {
    PRIMARY_STUDENT,
    SECONDARY_STUDENT,
    HIGH_SCHOOL_STUDENT,
    UNIVERSITY_STUDENT,
    WORKING,
    OTHER
}
```

Khi mapping Entity:

```java
@Enumerated(EnumType.STRING)
private ELearnerType learnerType;
```

Không sử dụng:

```java
@Enumerated(EnumType.ORDINAL)
```

vì thay đổi thứ tự enum có thể làm sai dữ liệu database.

---

# 7. STRING LENGTH

Nếu Class Diagram không chỉ rõ length:

- String thông thường: `length = 255`
- Text dài: sử dụng `@Column(columnDefinition = "TEXT")`

Các field như:

- username
- email
- title
- name
- code

nên có length hợp lý.

Không tự đặt length quá nhỏ gây mất dữ liệu.

---

# 8. NULLABILITY

Cardinality phải quyết định nullability.

Ví dụ:

```text
User 1 ---- 1 LearningPath
```

nếu relationship bắt buộc:

```java
nullable = false
```

Nếu:

```text
User 1 ---- 0..1 LearningPath
```

thì relationship phải cho phép null.

Không mặc định tất cả field là nullable.

---

# 9. ONE-TO-ONE

Với:

```text
A 1 ---- 0..1 B
```

xác định entity nào sở hữu relationship dựa trên Class Diagram.

Ví dụ User sở hữu LearningPath:

```java
@OneToOne
@JoinColumn(name = "learning_path_id")
private LearningPath learningPath;
```

Nếu diagram thể hiện FK ở LearningPath thì FK phải nằm ở LearningPath.

Không tạo FK ở cả hai bảng.

Không dùng `@JoinColumn` và `mappedBy` cùng một phía.

---

# 10. MANY-TO-ONE

Với:

```text
A * ---- 1 B
```

Entity A thường chứa FK tới B.

Ví dụ:

```text
Module * ---- 1 LearningPath
```

thì:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "learning_path_id", nullable = false)
private LearningPath learningPath;
```

Đây là relationship mặc định khi một Entity thuộc về một Entity cha.

---

# 11. ONE-TO-MANY

Nếu:

```text
A 1 ---- * B
```

thì B thường giữ FK.

Ví dụ:

```text
LearningPath 1 ---- * Module
```

thì:

```java
// Module
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "learning_path_id")
private LearningPath learningPath;
```

Phía LearningPath chỉ thêm collection nếu domain cần navigate ngược:

```java
@OneToMany(mappedBy = "learningPath")
private List<Module> modules;
```

Không tạo FK riêng trong bảng LearningPath cho relationship này.

---

# 12. MANY-TO-MANY

Không sử dụng `@ManyToMany` trực tiếp nếu relationship có thêm thuộc tính.

Ví dụ nếu:

```text
Question * ---- * Ability
```

và relationship có:

```text
points
orderIndex
createdAt
```

thì phải tạo Entity trung gian:

```text
QuestionAbility
```

và mapping:

```java
Question 1 ---- * QuestionAbility
Ability 1 ---- * QuestionAbility
```

Không sử dụng:

```java
@ManyToMany
```

trong trường hợp này.

---

# 13. COMPOSITION

Nếu Class Diagram sử dụng composition:

```text
Parent ◆---- Child
```

thì Child phụ thuộc vòng đời vào Parent.

Ví dụ:

```text
Module ◆---- ModuleTestQuestion
```

phải xem xét:

```java
@OneToMany(
    mappedBy = "moduleTest",
    cascade = CascadeType.ALL,
    orphanRemoval = true
)
```

Tuy nhiên chỉ sử dụng `CascadeType.ALL` và `orphanRemoval = true` khi business ownership thực sự yêu cầu.

Không dùng Cascade ALL một cách máy móc.

---

# 14. AGGREGATION

Aggregation không đồng nghĩa với `CascadeType.ALL`.

Nếu class diagram thể hiện aggregation:

```text
A ◇---- B
```

thì mặc định không cascade delete B khi A bị xóa.

Phải xác định ownership dựa trên business rule.

---

# 15. FETCH TYPE

Mặc định:

```java
@ManyToOne(fetch = FetchType.LAZY)
@OneToOne(fetch = FetchType.LAZY)
@OneToMany(fetch = FetchType.LAZY)
```

Không sử dụng `EAGER` nếu không có requirement cụ thể.

Mục tiêu là tránh load toàn bộ object graph không cần thiết.

---

# 16. BIDIRECTIONAL RELATIONSHIP

Không tạo relationship hai chiều chỉ vì Class Diagram có đường nối.

Chỉ tạo bidirectional mapping khi cả hai phía thực sự cần navigation.

Ví dụ:

```java
User -> LearningPath
```

nếu chỉ cần từ User lấy LearningPath thì không cần:

```java
LearningPath -> User
```

Không tạo relationship dư thừa.

---

# 17. JSON

Nếu Class Diagram thể hiện:

```text
userAnswer: JSON
correctAnswer: JSON
questionData: JSON
```

không chuyển thành String một cách tùy tiện.

Ưu tiên:

```java
private JsonNode userAnswer;
```

và sử dụng Hibernate JSON mapping phù hợp với version Spring Boot/Hibernate của project.

Ví dụ với Hibernate hỗ trợ JSON:

```java
@JdbcTypeCode(SqlTypes.JSON)
private JsonNode userAnswer;
```

Không lưu JSON bằng `String` nếu hệ thống cần query hoặc xử lý JSON như JSON.

---

# 18. DATE / DATETIME

Nếu Class Diagram là ngày:

```text
Date
```

sử dụng:

```java
LocalDate
```

Nếu là thời điểm:

```text
DateTime
Timestamp
```

sử dụng:

```java
LocalDateTime
```

Ví dụ:

```java
private LocalDate startedAt;
private LocalDate submittedAt;
```

Nếu business cần chính xác timezone/global timestamp thì phải xem xét `Instant`.

Không tự đổi `Date` thành `LocalDateTime`.

---

# 19. AUDIT FIELDS

Nếu Class Diagram đã có:

```text
createdAt
updatedAt
```

mapping:

```java
private LocalDateTime createdAt;
private LocalDateTime updatedAt;
```

Có thể sử dụng:

```java
@CreationTimestamp
private LocalDateTime createdAt;

@UpdateTimestamp
private LocalDateTime updatedAt;
```

Không thêm audit field nếu Class Diagram không yêu cầu, trừ khi project đã có BaseEntity thống nhất.

---

# 20. INHERITANCE TRONG DOMAIN

Nếu Class Diagram thể hiện:

```text
Lesson
   ▲
   |
VideoLesson
PracticeLesson
```

thì không được biến inheritance thành relationship.

Phải sử dụng Java inheritance và JPA inheritance strategy phù hợp.

Ví dụ:

```java
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Lesson {
}
```

và:

```java
@Entity
public class VideoLesson extends Lesson {
}
```

Không tạo:

```java
@ManyToOne
private Lesson lesson;
```

cho trường hợp inheritance.

---

# 21. BUSINESS RULES FROM USE CASE

Use Case Diagram không trực tiếp quyết định field, nhưng phải được dùng để validate Entity model.

### User / Student

Use Case thể hiện:

- Register
- Verify Email
- Login
- Logout
- Forgot Password

Do đó Entity phải có đủ persistent information để hỗ trợ authentication/account lifecycle.

Tuy nhiên:

- Login không phải Entity.
- Register không phải Entity.
- Verify Email không phải Entity.

Không tạo:

```text
LoginEntity
RegisterEntity
VerifyEmailEntity
```

chỉ vì chúng xuất hiện trong Use Case.

---

# 22. ABILITY DIAGNOSTIC

Use Case:

```text
Do Ability Diagnostic Test
Evaluate Ability Status
View Part Diagnostic Result
Evaluate Part Directive
```

Class Diagram hiện có:

```text
DiagnosticAttempt
AbilityDiagnosticAttempt
PartDiagnosticAttempt
PartDiagnosticResult
AttemptAnswer
DiagnosticAttemptQuestion
```

Các class này phải được xem xét theo lifecycle của một diagnostic attempt.

AI phải đảm bảo:

```text
User
  ↓
DiagnosticAttempt
  ↓
AttemptAnswer
  ↓
DiagnosticAttemptQuestion
```

và các subclass:

```text
DiagnosticAttempt
├── AbilityDiagnosticAttempt
└── PartDiagnosticAttempt
```

không được tự tạo thêm Result/Attempt entity nếu diagram không yêu cầu.

---

# 23. MODULE TEST

Use Case:

```text
Do Module Test
Unlock Next Module
View Module Test Result
```

Class Diagram:

```text
Module
ModuleTest
ModuleTestQuestion
ModuleTestAttempt
```

AI phải giữ đúng business lifecycle:

```text
Module
   ↓
ModuleTest
   ↓
ModuleTestQuestion
   ↓
ModuleTestAttempt
```

`ModuleTestAttempt` đại diện cho một lần Student thực hiện ModuleTest.

Không tạo thêm `ModuleTestResult` nếu kết quả đã được lưu trong `ModuleTestAttempt`.

Nếu business rule yêu cầu nhiều attempt:

```text
ModuleTest 1 ---- * ModuleTestAttempt
```

Nếu chỉ một attempt thì phải giữ đúng cardinality trong diagram.

---

# 24. LEARNING PATH

Class Diagram:

```text
User 1 ---- 0..1 LearningPath
LearningPath 1 ---- * Module
```

Phải giữ đúng ownership.

LearningPath là cấu trúc học tập được User chọn/tạo theo Ability AIM.

Không tạo Entity mới như:

```text
ChooseAIM
ViewLearningPath
GenerateLearningPath
```

vì đây là Use Case/operation chứ không phải persistent domain object.

---

# 25. AIM / TARGET PROFILE

Class Diagram có:

```text
UserAbility
AbilityAimPlan
AbilityAimPlanItem
TargetProfile
TargetPartThreshold
```

AI phải phân biệt:

### Domain Entity

```text
UserAbility
AbilityAimPlan
AbilityAimPlanItem
TargetProfile
TargetPartThreshold
```

### Domain operation

```text
chooseAIM()
updateStatus()
manageTargetProfile()
manageThreshold()
```

Các method không trở thành Entity.

---

# 26. QUESTION MODEL

Class Diagram có:

```text
<<abstract>> Question
QuestionAbility
PracticeQuestion
ModuleTestQuestion
DiagnosticAttemptQuestion
```

Nếu `Question` là abstract domain object thì phải xác định inheritance strategy.

Các relationship tới Question phải được mapping đúng.

Không tạo một Question Entity riêng cho:

```text
PracticeQuestion
ModuleTestQuestion
DiagnosticAttemptQuestion
```

nếu chúng là association/association entity chứ không phải subclass.

Phải phân biệt rõ:

```text
Inheritance
```

với:

```text
Association
```

---

# 27. QUESTION TYPE

Enumeration:

```text
MULTIPLE_CHOICE
FILL_BLANK
DRAG_DROP
TRUE_FALSE
SINGLE_CHOICE
MATCHING
```

phải được mapping:

```java
@Enumerated(EnumType.STRING)
private EQuestionType type;
```

Không tạo Entity:

```text
QuestionType
```

---

# 28. ORDER INDEX / ORDER NO

Nếu Class Diagram có:

```text
orderNo
orderIndex
sequence
```

thì giữ nguyên vì đây là thông tin thứ tự nghiệp vụ.

Ví dụ:

```text
Module
 ├── sequence
```

hoặc:

```text
AbilityAimPlanItem
 └── orderNo
```

Không xóa chỉ vì database có thể sắp xếp theo ID.

ID không thay thế được orderIndex/orderNo.

---

# 29. STATUS ENUM

Các trạng thái phải được giữ dưới dạng Enum nếu Class Diagram định nghĩa Enum.

Ví dụ:

```text
ELessonStatus
EModuleStatus
EAbilityStatus
ETestResult
```

Mapping:

```java
@Enumerated(EnumType.STRING)
private EModuleStatus status;
```

Không dùng String nếu diagram đã định nghĩa Enum.

---

# 30. METHOD TRONG CLASS DIAGRAM

Các method như:

```text
register()
login()
logout()
verifyEmail()
updateStatus()
manageQuestion()
startModuleTest()
submitModuleTest()
```

không phải database column.

Không tạo field tương ứng cho method.

Method chỉ được giữ trong Entity nếu thật sự phù hợp với domain model, nhưng không bắt buộc phải implement chỉ vì xuất hiện trên Class Diagram.

---

# 31. USE CASE INCLUDE / EXTEND

Không biến:

```text
<<include>>
<<extend>>
```

thành Entity relationship.

Ví dụ:

```text
Register
  <<include>>
Verify Email
```

không có nghĩa:

```text
Register 1 ---- 1 VerifyEmail
```

Tương tự:

```text
Do Module Test
  <<extend>>
Unlock Next Module
```

không tạo:

```text
ModuleTest 1 ---- 1 UnlockNextModule
```

Đây là relationship ở mức Use Case, không phải database relationship.

---

# 32. ACTOR INHERITANCE

Nếu:

```text
Student ──▷ User
Admin ──▷ User
```

nhưng Class Diagram chỉ có:

```text
User
role: ERole
```

thì không tạo:

```text
StudentEntity
AdminEntity
```

Mặc định:

```text
User
 └── role
```

với:

```java
@Enumerated(EnumType.STRING)
private ERole role;
```

Chỉ sử dụng Entity inheritance nếu Class Diagram domain model thực sự thể hiện Student/Admin là subclasses có dữ liệu riêng.

---

# 33. LOMBOK

Nếu project sử dụng Lombok, Entity mặc định có thể dùng:

```java
@Getter
@Setter
@NoArgsConstructor
```

Không sử dụng `@Data` một cách mặc định cho JPA Entity vì có thể gây vấn đề với:

- `equals()`
- `hashCode()`
- lazy relationship
- circular relationship
- toString()

Không dùng `@ToString` trên toàn bộ Entity graph nếu có bidirectional relationship.

---

# 34. EQUALS / HASHCODE

Không sử dụng toàn bộ relationship trong:

```java
equals()
hashCode()
```

Không sử dụng collection relationship trong `equals/hashCode`.

Ưu tiên identity-based implementation phù hợp với JPA.

---

# 35. ENTITY RELATIONSHIP OWNERSHIP

Khi xác định owner:

1. Kiểm tra FK trong Class Diagram.
2. Kiểm tra cardinality.
3. Kiểm tra composition.
4. Kiểm tra business lifecycle trong Use Case.
5. Chỉ sau đó mới quyết định:
   - `@JoinColumn`
   - `mappedBy`
   - `cascade`
   - `orphanRemoval`

Không quyết định ownership chỉ dựa vào tên class.

---

# 36. BUSINESS CONSTRAINT VALIDATION

Use Case Diagram phải được sử dụng để kiểm tra những constraint như:

```text
User có thể thực hiện nhiều ModuleTestAttempt
User có thể có 0..1 LearningPath
LearningPath chứa nhiều Module
Module chứa ModuleTest
ModuleTest có nhiều ModuleTestQuestion
```

Nếu Use Case nói:

```text
Unlock Next Module
```

thì phải kiểm tra Entity model có đủ dữ liệu để xác định điều kiện unlock hay chưa.

Ví dụ:

```text
ModuleTestAttempt
    ↓
score
    ↓
ModuleTest.passScore
    ↓
Module.status
```

Nếu business rule yêu cầu pass test mới unlock module tiếp theo, Entity model phải có đủ dữ liệu để Service kiểm tra rule đó.

Không nhúng toàn bộ business logic vào Entity nếu project đang sử dụng Service Layer.

---

# 37. ENTITY VS SERVICE RESPONSIBILITY

Entity chịu trách nhiệm:

- State
- Relationship
- Domain data
- Simple domain behavior nếu phù hợp

Service chịu trách nhiệm:

- Register
- Login
- Verify Email
- Generate Learning Path
- Evaluate Diagnostic
- Unlock Module
- Submit Test
- Calculate Result

Không đưa logic use case phức tạp trực tiếp vào Entity chỉ để diagram "đủ".

---

# 38. DATABASE NAMING

Mặc định:

### Class

```text
User
LearningPath
ModuleTestAttempt
```

### Table

```text
users
learning_paths
modules
module_test_attempts
```

### Column

```text
created_at
updated_at
user_id
learning_path_id
```

Sử dụng snake_case cho database.

---

# 39. COLUMN NAMING

Không đổi tên attribute nếu Class Diagram đã đặt tên rõ ràng.

Ví dụ:

```text
targetTotalScore
```

không tự đổi thành:

```text
total
```

Chỉ đổi sang snake_case ở database:

```text
target_total_score
```

---

# 40. NO DUPLICATE ENTITY

Trước khi tạo Entity mới, kiểm tra xem domain concept đó đã tồn tại chưa.

Không tạo duplicate như:

```text
ModuleTestResult
```

nếu:

```text
ModuleTestAttempt
```

đã chứa:

```text
score
result
submittedAt
```

Không tạo:

```text
AbilityResult
```

nếu Class Diagram đã có:

```text
UserAbility
PartDiagnosticResult
```

trừ khi business requirement chứng minh cần một persistent concept riêng.

---

# 41. OUTPUT REQUIREMENT

Khi sinh code:

1. Liệt kê toàn bộ Entity sẽ tạo.
2. Liệt kê Enum.
3. Liệt kê inheritance.
4. Liệt kê relationship.
5. Liệt kê FK ownership.
6. Liệt kê những điểm còn mơ hồ.
7. Chỉ sau đó mới sinh code.

Mỗi Entity phải có:

```text
@Entity
@Table(...)
```

nếu project sử dụng explicit table mapping.

Mỗi Entity phải có:

```text
@Id
```

và strategy phù hợp.

---

# 42. PRE-CODE VALIDATION

Trước khi sinh code, AI phải kiểm tra:

- [ ] Mỗi Entity có PK.
- [ ] Mỗi FK có relationship tương ứng.
- [ ] Cardinality được mapping đúng.
- [ ] Không có FK trùng.
- [ ] Không có `mappedBy` sai.
- [ ] Không có ManyToMany dư thừa.
- [ ] Composition được xem xét cascade/orphanRemoval.
- [ ] Abstract class có inheritance strategy rõ ràng.
- [ ] Enum sử dụng STRING.
- [ ] JSON được mapping đúng.
- [ ] Date/DateTime đúng Java type.
- [ ] Không tạo Entity từ Use Case.
- [ ] Không tạo Entity từ actor.
- [ ] Không biến include/extend thành database relationship.
- [ ] Không duplicate Entity.
- [ ] Không tự thêm field ngoài diagram nếu chưa được xác nhận.

---

# 43. IMPORTANT RULE

Nếu có bất kỳ điểm nào không xác định được từ Class Diagram + Use Case Diagram:

DO NOT GUESS.

Hãy xuất:

```text
AMBIGUITIES / DECISIONS REQUIRED

1. ...
2. ...
3. ...
```

và giải thích:

```text
Current diagram:
...

Possible options:
A. ...
B. ...

Recommended:
...
Reason:
...
```

Chỉ sinh code hoàn chỉnh sau khi các ambiguity quan trọng đã được giải quyết.

---

# 44. FINAL PRINCIPLE

Class Diagram quyết định:

```text
WHAT DATA EXISTS
WHAT RELATIONSHIPS EXIST
HOW OBJECTS ARE STRUCTURED
```

Use Case Diagram quyết định/kiểm chứng:

```text
WHY THE DATA EXISTS
HOW THE DATA IS USED
WHAT BUSINESS RULES MUST BE SUPPORTED
```

Spring Boot Entity phải là kết quả của:

```text
Class Diagram
        +
Use Case Business Rules
        +
JPA Mapping Rules
        +
Database Constraints
```

Không được để AI tự suy diễn những phần quan trọng khi diagram chưa xác định.