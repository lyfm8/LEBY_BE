package com.example.be.service.question.implement;

import com.example.be.dto.request.question.CreateQuestionGroupRequest;
import com.example.be.dto.request.question.CreateStandaloneQuestionRequest;
import com.example.be.dto.request.question.ImportGroupItemRowDto;
import com.example.be.dto.request.question.ImportGroupParentRowDto;
import com.example.be.dto.request.question.ImportStandaloneRowDto;
import com.example.be.dto.response.question.ImportRowError;
import com.example.be.dto.response.question.QuestionImportResultResponse;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.content.Part;
import com.example.be.entity.media.Media;
import com.example.be.enums.ability.ESection;
import com.example.be.enums.media.EMediaKind;
import com.example.be.mapper.question.QuestionImportMapper;
import com.example.be.repository.ability.AbilityRepository;
import com.example.be.repository.content.PartRepository;
import com.example.be.repository.media.MediaRepository;
import com.example.be.service.question.service.QuestionImportService;
import com.example.be.service.question.service.QuestionService;
import com.example.be.service.question.util.MediaKindResolver;
import com.example.be.service.question.util.QuestionExcelErrorReportGenerator;
import com.example.be.service.question.util.QuestionExcelTemplateGenerator;
import com.example.be.service.question.util.QuestionImportExcelParser;
import com.example.be.service.question.util.QuestionImportExcelParser.GroupRowKind;
import com.example.be.service.question.util.QuestionImportExcelParser.ParsedGroupSheetRow;
import com.example.be.service.question.util.QuestionImportExcelParser.ParsedWorkbookResult;
import com.example.be.service.question.util.QuestionImportTemplateSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Triển khai nghiệp vụ Import câu hỏi hàng loạt từ Excel (UC A04 - Luồng thay thế 3a & 3b).
 * Tuân thủ chiến lược All-or-Nothing: toàn bộ file hợp lệ mới tiến hành lưu trữ vào cơ sở dữ liệu.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionImportServiceImpl implements QuestionImportService {

    private static final int MAX_NAME_LENGTH = 200;
    private static final int MIN_DIFFICULTY = 1;
    private static final int MAX_DIFFICULTY = 5;
    private static final int MIN_GROUP_ITEMS = 2;
    private static final int MAX_GROUP_ITEMS = 5;
    private static final int MAX_AUDIO_PER_QUESTION = 1;
    private static final int MAX_VIDEO_PER_QUESTION = 1;
    private static final int MAX_IMAGE_PER_QUESTION = 3;
    private static final Set<String> VALID_CORRECT_KEYS = Set.of("A", "B", "C", "D");

    private final PartRepository partRepository;
    private final AbilityRepository abilityRepository;
    private final MediaRepository mediaRepository;
    private final QuestionService questionService;
    private final QuestionImportMapper questionImportMapper;
    private final QuestionExcelTemplateGenerator templateGenerator;
    private final QuestionImportExcelParser excelParser;
    private final QuestionExcelErrorReportGenerator errorReportGenerator;

    @Override
    @Transactional(readOnly = true)
    public byte[] generateImportTemplate() {
        List<Part> parts = partRepository.findAll(Sort.by("id").ascending());
        List<Ability> abilities = abilityRepository.findAll(Sort.by("id").ascending());
        return templateGenerator.generateTemplate(parts, abilities);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuestionImportResultResponse importFromExcel(MultipartFile file) {
        ParsedWorkbookResult parsed = excelParser.parse(file);

        List<ImportRowError> errors = new ArrayList<>(parsed.getStructuralErrors());
        if (!errors.isEmpty()) {
            return buildFailedResult(0, 0, 0, errors);
        }

        List<ImportStandaloneRowDto> standaloneRows = parsed.getStandaloneRows();
        List<ParsedGroupSheetRow> rawGroupRows = parsed.getGroupRows();

        if (standaloneRows.isEmpty() && rawGroupRows.isEmpty()) {
            errors.add(ImportRowError.builder()
                    .sheet("Cấu trúc tệp")
                    .rowNumber(2)
                    .messages(List.of("Tệp Excel không chứa dòng dữ liệu câu hỏi nào (các dòng có tiền tố [VÍ DỤ] được tự động bỏ qua)"))
                    .build());
            return buildFailedResult(0, 0, 0, errors);
        }

        // Nạp một lần dữ liệu tham chiếu Part, Ability và Media để triệt tiêu N+1 query khi đối soát
        Map<Long, Part> partMap = partRepository.findAll().stream()
                .collect(Collectors.toMap(Part::getId, Function.identity()));
        Map<Long, Ability> abilityMap = abilityRepository.findAll().stream()
                .collect(Collectors.toMap(Ability::getId, Function.identity()));

        // Bước 1: Parse định dạng số / ID thô và thu thập toàn bộ mediaId được tham chiếu
        Set<Long> referencedMediaIds = new LinkedHashSet<>();
        validateAndParseStandaloneRows(standaloneRows, partMap, abilityMap, referencedMediaIds, errors);

        Map<String, GroupBucket> groupBuckets = organizeAndPreParseGroupRows(
                rawGroupRows,
                partMap,
                abilityMap,
                referencedMediaIds,
                errors
        );

        Map<Long, Media> mediaMap = referencedMediaIds.isEmpty()
                ? Map.of()
                : mediaRepository.findAllById(referencedMediaIds).stream()
                        .collect(Collectors.toMap(Media::getId, Function.identity()));

        // Bước 2: Đối soát toàn bộ ràng buộc Media theo Section (Listening/Reading) và quy tắc Reading
        validateStandaloneMedias(standaloneRows, partMap, mediaMap, errors);
        validateGroupBundles(groupBuckets, partMap, abilityMap, mediaMap, errors);

        int standaloneCount = standaloneRows.size();
        int groupCount = groupBuckets.size();
        int groupItemCount = groupBuckets.values().stream().mapToInt(b -> b.items.size()).sum();

        // Nguyên tắc All-or-Nothing: phát hiện bất kỳ lỗi nào thì từ chối lưu toàn bộ
        if (!errors.isEmpty()) {
            log.warn("Import câu hỏi bị từ chối với {} lỗi (standalone={}, groups={})",
                    errors.size(), standaloneCount, groupCount);
            return buildFailedResult(standaloneCount, groupCount, groupItemCount, errors);
        }

        // Bước 3: Ánh xạ qua QuestionImportMapper và lưu vào DB qua QuestionService
        for (ImportStandaloneRowDto row : standaloneRows) {
            CreateStandaloneQuestionRequest request = questionImportMapper.toStandaloneRequest(row);
            questionService.createStandaloneQuestion(request);
        }

        for (GroupBucket bucket : groupBuckets.values()) {
            List<ImportGroupItemRowDto> sortedItems = new ArrayList<>(bucket.items);
            sortedItems.sort(Comparator.comparing(
                    item -> item.getOrderNo() != null ? item.getOrderNo() : Integer.MAX_VALUE
            ));
            CreateQuestionGroupRequest request = questionImportMapper.toGroupRequest(bucket.parent, sortedItems);
            questionService.createQuestionGroup(request);
        }

        int totalEntities = standaloneCount + groupCount;
        log.info("Import hàng loạt thành công {} câu đơn và {} chùm ({} câu con)",
                standaloneCount, groupCount, groupItemCount);

        return QuestionImportResultResponse.builder()
                .imported(true)
                .totalRows(totalEntities)
                .standaloneCount(standaloneCount)
                .groupCount(groupCount)
                .groupItemCount(groupItemCount)
                .successCount(totalEntities)
                .failCount(0)
                .errors(List.of())
                .build();
    }

    @Override
    public byte[] exportErrorReport(List<ImportRowError> errors) {
        return errorReportGenerator.generateErrorReport(errors);
    }

    // -------------------------------------------------------------------------
    // Standalone Validation
    // -------------------------------------------------------------------------

    private void validateAndParseStandaloneRows(
            List<ImportStandaloneRowDto> rows,
            Map<Long, Part> partMap,
            Map<Long, Ability> abilityMap,
            Set<Long> referencedMediaIds,
            List<ImportRowError> errors
    ) {
        for (ImportStandaloneRowDto row : rows) {
            List<String> rowErrors = new ArrayList<>();

            validateName(row.getName(), "Mã gợi nhớ câu hỏi", rowErrors);

            Part part = parseAndValidatePart(row.getRawPartId(), partMap, false, rowErrors);
            if (part != null) {
                row.setPartId(part.getId());
            }

            List<Long> abilityIds = parseIdList(row.getRawAbilityIds(), "Mã kỹ năng (Ability)", true, rowErrors);
            if (abilityIds != null) {
                validateAbilitiesBelongToPart(abilityIds, part, abilityMap, rowErrors);
                row.setAbilityIds(abilityIds);
            }

            Integer difficulty = parseDifficulty(row.getRawDifficulty(), rowErrors);
            row.setDifficulty(difficulty);

            boolean isPart2 = isPartNumber(part, 2);
            validateOptionsAndCorrectKey(
                    row.getOptionA(),
                    row.getOptionB(),
                    row.getOptionC(),
                    row.getOptionD(),
                    row.getCorrectKey(),
                    isPart2,
                    rowErrors
            );

            List<Long> mediaIds = parseIdList(row.getRawMediaIds(), "Mã Media", false, rowErrors);
            if (mediaIds != null) {
                row.setMediaIds(mediaIds);
                referencedMediaIds.addAll(mediaIds);
            }

            validateVocabularyFormat(row.getExplVocab(), rowErrors);

            if (!rowErrors.isEmpty()) {
                errors.add(ImportRowError.builder()
                        .sheet(QuestionImportTemplateSchema.SHEET_STANDALONE)
                        .rowNumber(row.getRowNumber())
                        .questionName(row.getName())
                        .messages(rowErrors)
                        .build());
            }
        }
    }

    private void validateStandaloneMedias(
            List<ImportStandaloneRowDto> rows,
            Map<Long, Part> partMap,
            Map<Long, Media> mediaMap,
            List<ImportRowError> errors
    ) {
        for (ImportStandaloneRowDto row : rows) {
            if (row.getPartId() == null) {
                continue;
            }
            Part part = partMap.get(row.getPartId());
            if (part == null) {
                continue;
            }

            List<String> mediaErrors = new ArrayList<>();
            validateMediaRules(
                    row.getMediaIds(),
                    part.getSections(),
                    false,
                    mediaMap,
                    mediaErrors
            );

            if (!mediaErrors.isEmpty()) {
                appendRowErrors(
                        errors,
                        QuestionImportTemplateSchema.SHEET_STANDALONE,
                        row.getRowNumber(),
                        null,
                        row.getName(),
                        mediaErrors
                );
            }
        }
    }

    // -------------------------------------------------------------------------
    // Group Organization & Validation
    // -------------------------------------------------------------------------

    private Map<String, GroupBucket> organizeAndPreParseGroupRows(
            List<ParsedGroupSheetRow> rawRows,
            Map<Long, Part> partMap,
            Map<Long, Ability> abilityMap,
            Set<Long> referencedMediaIds,
            List<ImportRowError> errors
    ) {
        Map<String, GroupBucket> buckets = new LinkedHashMap<>();

        for (ParsedGroupSheetRow rawRow : rawRows) {
            List<String> rowErrors = new ArrayList<>();
            String groupRowId = rawRow.getGroupRowId();

            if (groupRowId == null || groupRowId.isBlank()) {
                rowErrors.add("Mã chùm (groupRowId) không được để trống");
            }
            if (rawRow.getKind() == GroupRowKind.UNKNOWN) {
                rowErrors.add("Loại dòng '" + rawRow.getRawRowType()
                        + "' không hợp lệ (chỉ chấp nhận 'CHÙM' hoặc 'CÂU CON')");
            }

            if (!rowErrors.isEmpty()) {
                errors.add(ImportRowError.builder()
                        .sheet(QuestionImportTemplateSchema.SHEET_GROUP)
                        .rowNumber(rawRow.getRowNumber())
                        .groupRowId(groupRowId)
                        .messages(rowErrors)
                        .build());
                continue;
            }

            GroupBucket bucket = buckets.computeIfAbsent(groupRowId.trim(), k -> new GroupBucket(k));
            if (rawRow.getKind() == GroupRowKind.GROUP) {
                ImportGroupParentRowDto parent = rawRow.getParentRow();
                if (bucket.parent != null) {
                    errors.add(ImportRowError.builder()
                            .sheet(QuestionImportTemplateSchema.SHEET_GROUP)
                            .rowNumber(parent.getRowNumber())
                            .groupRowId(bucket.groupRowId)
                            .questionName(parent.getName())
                            .messages(List.of("Mã chùm '" + bucket.groupRowId + "' đã có dòng CHÙM ở dòng " + bucket.parent.getRowNumber()))
                            .build());
                    continue;
                }
                bucket.parent = parent;
                validateParentFields(parent, partMap, referencedMediaIds, errors);
            } else if (rawRow.getKind() == GroupRowKind.ITEM) {
                ImportGroupItemRowDto item = rawRow.getItemRow();
                bucket.items.add(item);
                validateItemPreParseFields(item, referencedMediaIds, errors);
            }
        }

        // Kiểm tra chùm có thiếu dòng CHÙM hay sai số lượng câu con
        for (GroupBucket bucket : buckets.values()) {
            if (bucket.parent == null) {
                int firstItemRow = bucket.items.isEmpty() ? 2 : bucket.items.get(0).getRowNumber();
                errors.add(ImportRowError.builder()
                        .sheet(QuestionImportTemplateSchema.SHEET_GROUP)
                        .rowNumber(firstItemRow)
                        .groupRowId(bucket.groupRowId)
                        .messages(List.of("Mã chùm '" + bucket.groupRowId + "' thiếu dòng khai báo 'CHÙM'"))
                        .build());
                continue;
            }

            if (bucket.items.size() < MIN_GROUP_ITEMS || bucket.items.size() > MAX_GROUP_ITEMS) {
                appendRowErrors(
                        errors,
                        QuestionImportTemplateSchema.SHEET_GROUP,
                        bucket.parent.getRowNumber(),
                        bucket.groupRowId,
                        bucket.parent.getName(),
                        List.of("Chùm '" + bucket.groupRowId + "' hiện có " + bucket.items.size()
                                + " câu con, hệ thống yêu cầu từ " + MIN_GROUP_ITEMS + " đến " + MAX_GROUP_ITEMS + " câu con")
                );
                continue;
            }

            // Kiểm tra Ability của từng câu con khớp với Part của chùm
            Part groupPart = bucket.parent.getPartId() != null ? partMap.get(bucket.parent.getPartId()) : null;
            boolean isPart2 = isPartNumber(groupPart, 2);
            for (ImportGroupItemRowDto item : bucket.items) {
                List<String> itemErrors = new ArrayList<>();
                if (item.getAbilityIds() != null) {
                    validateAbilitiesBelongToPart(item.getAbilityIds(), groupPart, abilityMap, itemErrors);
                }
                validateOptionsAndCorrectKey(
                        item.getOptionA(),
                        item.getOptionB(),
                        item.getOptionC(),
                        item.getOptionD(),
                        item.getCorrectKey(),
                        isPart2,
                        itemErrors
                );
                if (!itemErrors.isEmpty()) {
                    appendRowErrors(
                            errors,
                            QuestionImportTemplateSchema.SHEET_GROUP,
                            item.getRowNumber(),
                            bucket.groupRowId,
                            item.getName(),
                            itemErrors
                    );
                }
            }
        }

        return buckets;
    }

    private void validateParentFields(
            ImportGroupParentRowDto parent,
            Map<Long, Part> partMap,
            Set<Long> referencedMediaIds,
            List<ImportRowError> errors
    ) {
        List<String> rowErrors = new ArrayList<>();
        validateName(parent.getName(), "Tên chùm câu hỏi", rowErrors);

        Part part = parseAndValidatePart(parent.getRawPartId(), partMap, true, rowErrors);
        if (part != null) {
            parent.setPartId(part.getId());
        }

        List<Long> mediaIds = parseIdList(parent.getRawMediaIds(), "Mã Media chung của chùm", false, rowErrors);
        if (mediaIds != null) {
            parent.setMediaIds(mediaIds);
            referencedMediaIds.addAll(mediaIds);
        }

        if (!rowErrors.isEmpty()) {
            appendRowErrors(
                    errors,
                    QuestionImportTemplateSchema.SHEET_GROUP,
                    parent.getRowNumber(),
                    parent.getGroupRowId(),
                    parent.getName(),
                    rowErrors
            );
        }
    }

    private void validateItemPreParseFields(
            ImportGroupItemRowDto item,
            Set<Long> referencedMediaIds,
            List<ImportRowError> errors
    ) {
        List<String> rowErrors = new ArrayList<>();
        validateName(item.getName(), "Tên câu con", rowErrors);

        if (item.getRawOrderNo() != null && !item.getRawOrderNo().isBlank()) {
            Integer orderNo = parsePositiveInt(item.getRawOrderNo(), "Thứ tự câu con", 1, MAX_GROUP_ITEMS, rowErrors);
            item.setOrderNo(orderNo);
        }

        List<Long> abilityIds = parseIdList(item.getRawAbilityIds(), "Mã kỹ năng câu con", true, rowErrors);
        if (abilityIds != null) {
            item.setAbilityIds(abilityIds);
        }

        item.setDifficulty(parseDifficulty(item.getRawDifficulty(), rowErrors));

        List<Long> mediaIds = parseIdList(item.getRawMediaIds(), "Mã Media riêng câu con", false, rowErrors);
        if (mediaIds != null) {
            item.setMediaIds(mediaIds);
            referencedMediaIds.addAll(mediaIds);
        }

        validateVocabularyFormat(item.getExplVocab(), rowErrors);

        if (!rowErrors.isEmpty()) {
            appendRowErrors(
                    errors,
                    QuestionImportTemplateSchema.SHEET_GROUP,
                    item.getRowNumber(),
                    item.getGroupRowId(),
                    item.getName(),
                    rowErrors
            );
        }
    }

    private void validateGroupBundles(
            Map<String, GroupBucket> buckets,
            Map<Long, Part> partMap,
            Map<Long, Ability> abilityMap,
            Map<Long, Media> mediaMap,
            List<ImportRowError> errors
    ) {
        for (GroupBucket bucket : buckets.values()) {
            if (bucket.parent == null || bucket.parent.getPartId() == null) {
                continue;
            }
            Part part = partMap.get(bucket.parent.getPartId());
            if (part == null) {
                continue;
            }

            List<String> parentMediaErrors = new ArrayList<>();
            List<Media> parentMedias = validateMediaRules(
                    bucket.parent.getMediaIds(),
                    part.getSections(),
                    false,
                    mediaMap,
                    parentMediaErrors
            );

            // Với chùm Reading (Part 6, 7): bắt buộc có đoạn văn hoặc ít nhất 1 ảnh
            if (part.getSections() == ESection.READING) {
                boolean hasPassage = bucket.parent.getPassage() != null && !bucket.parent.getPassage().isBlank();
                boolean hasImage = parentMedias.stream().anyMatch(m -> MediaKindResolver.resolve(m) == EMediaKind.IMAGE);
                if (!hasPassage && !hasImage) {
                    parentMediaErrors.add("Chùm câu hỏi Reading bắt buộc phải có đoạn văn đọc hiểu hoặc ít nhất 1 hình ảnh đính kèm");
                }
            }

            if (!parentMediaErrors.isEmpty()) {
                appendRowErrors(
                        errors,
                        QuestionImportTemplateSchema.SHEET_GROUP,
                        bucket.parent.getRowNumber(),
                        bucket.groupRowId,
                        bucket.parent.getName(),
                        parentMediaErrors
                );
            }

            for (ImportGroupItemRowDto item : bucket.items) {
                List<String> itemMediaErrors = new ArrayList<>();
                validateMediaRules(
                        item.getMediaIds(),
                        part.getSections(),
                        true,
                        mediaMap,
                        itemMediaErrors
                );
                if (!itemMediaErrors.isEmpty()) {
                    appendRowErrors(
                            errors,
                            QuestionImportTemplateSchema.SHEET_GROUP,
                            item.getRowNumber(),
                            bucket.groupRowId,
                            item.getName(),
                            itemMediaErrors
                    );
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Shared Field Validation Helpers
    // -------------------------------------------------------------------------

    private void validateName(String name, String fieldLabel, List<String> errors) {
        if (name == null || name.isBlank()) {
            errors.add(fieldLabel + " không được để trống");
            return;
        }
        if (name.trim().length() > MAX_NAME_LENGTH) {
            errors.add(fieldLabel + " vượt quá tối đa " + MAX_NAME_LENGTH + " ký tự");
        }
    }

    private Part parseAndValidatePart(
            String rawPartId,
            Map<Long, Part> partMap,
            boolean expectGroupSheet,
            List<String> errors
    ) {
        if (rawPartId == null || rawPartId.isBlank()) {
            errors.add("Mã Phần thi (Part) không được để trống");
            return null;
        }
        Long partId = parsePositiveLong(rawPartId);
        if (partId == null) {
            errors.add("Mã Phần thi '" + rawPartId + "' phải là số nguyên dương");
            return null;
        }
        Part part = partMap.get(partId);
        if (part == null) {
            errors.add("Không tìm thấy Phần thi với mã Part = " + partId);
            return null;
        }

        boolean partIsGroup = isGroupPart(part);
        boolean partIsStandalone = isStandalonePart(part);
        if (!expectGroupSheet && partIsGroup) {
            errors.add(part.getName() + " là dạng chùm câu hỏi, vui lòng nhập ở sheet '"
                    + QuestionImportTemplateSchema.SHEET_GROUP + "'");
        } else if (expectGroupSheet && partIsStandalone) {
            errors.add(part.getName() + " là dạng câu hỏi đơn, vui lòng nhập ở sheet '"
                    + QuestionImportTemplateSchema.SHEET_STANDALONE + "'");
        }

        return part;
    }

    private void validateAbilitiesBelongToPart(
            List<Long> abilityIds,
            Part part,
            Map<Long, Ability> abilityMap,
            List<String> errors
    ) {
        for (Long abilityId : abilityIds) {
            Ability ability = abilityMap.get(abilityId);
            if (ability == null) {
                errors.add("Mã kỹ năng (Ability) = " + abilityId + " không tồn tại trong hệ thống");
                continue;
            }
            if (part != null && (ability.getPart() == null || !ability.getPart().getId().equals(part.getId()))) {
                errors.add("Kỹ năng '" + ability.getName() + "' (ID=" + abilityId
                        + ") không thuộc về " + part.getName());
            }
        }
    }

    private Integer parseDifficulty(String rawDifficulty, List<String> errors) {
        return parsePositiveInt(rawDifficulty, "Độ khó", MIN_DIFFICULTY, MAX_DIFFICULTY, errors);
    }

    private Integer parsePositiveInt(
            String raw,
            String fieldLabel,
            int min,
            int max,
            List<String> errors
    ) {
        if (raw == null || raw.isBlank()) {
            errors.add(fieldLabel + " không được để trống");
            return null;
        }
        try {
            int val = Integer.parseInt(raw.trim());
            if (val < min || val > max) {
                errors.add(fieldLabel + " phải nằm trong khoảng từ " + min + " đến " + max);
                return null;
            }
            return val;
        } catch (NumberFormatException ex) {
            errors.add(fieldLabel + " '" + raw + "' phải là số nguyên từ " + min + " đến " + max);
            return null;
        }
    }

    private void validateOptionsAndCorrectKey(
            String optionA,
            String optionB,
            String optionC,
            String optionD,
            String correctKey,
            boolean isPart2,
            List<String> errors
    ) {
        if (isBlank(optionA)) {
            errors.add("Phương án A không được để trống");
        }
        if (isBlank(optionB)) {
            errors.add("Phương án B không được để trống");
        }
        if (isBlank(optionC)) {
            errors.add("Phương án C không được để trống");
        }

        boolean hasD = !isBlank(optionD);
        if (!isPart2 && !hasD) {
            errors.add("Phương án D không được để trống (chỉ Part 2 được bỏ trống phương án D)");
        }

        if (isBlank(correctKey)) {
            errors.add("Đáp án đúng không được để trống");
            return;
        }
        String normalizedKey = correctKey.trim().toUpperCase(Locale.ROOT);
        if (!VALID_CORRECT_KEYS.contains(normalizedKey)) {
            errors.add("Đáp án đúng '" + correctKey + "' không hợp lệ (chỉ chấp nhận A, B, C hoặc D)");
            return;
        }
        if ("D".equals(normalizedKey) && !hasD) {
            errors.add("Đáp án đúng là D nhưng Phương án D đang bị bỏ trống");
        }
    }

    private List<Long> parseIdList(String rawIds, String fieldLabel, boolean required, List<String> errors) {
        if (rawIds == null || rawIds.isBlank()) {
            if (required) {
                errors.add(fieldLabel + " không được để trống");
            }
            return List.of();
        }

        String[] tokens = rawIds.split("\\|");
        Set<Long> parsedIds = new LinkedHashSet<>();
        boolean hasInvalid = false;

        for (String token : tokens) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            Long id = parsePositiveLong(trimmed);
            if (id == null) {
                errors.add(fieldLabel + " chứa giá trị không hợp lệ: '" + trimmed + "' (phải là số nguyên dương cách nhau bằng dấu |)");
                hasInvalid = true;
            } else {
                parsedIds.add(id);
            }
        }

        if (!hasInvalid && required && parsedIds.isEmpty()) {
            errors.add(fieldLabel + " phải chứa ít nhất 1 mã hợp lệ");
            return null;
        }
        return hasInvalid ? null : new ArrayList<>(parsedIds);
    }

    private List<Media> validateMediaRules(
            List<Long> mediaIds,
            ESection section,
            boolean isGroupItem,
            Map<Long, Media> mediaMap,
            List<String> errors
    ) {
        if (mediaIds == null || mediaIds.isEmpty()) {
            if (section == ESection.LISTENING && !isGroupItem) {
                errors.add("Câu hỏi thuộc phần thi LISTENING bắt buộc phải có tệp âm thanh (Audio) đính kèm");
            }
            return List.of();
        }

        List<Media> resolved = new ArrayList<>();
        for (Long mediaId : mediaIds) {
            Media media = mediaMap.get(mediaId);
            if (media == null) {
                errors.add("Mã Media = " + mediaId + " không tồn tại trong hệ thống (vui lòng upload qua popup Upload media)");
            } else {
                resolved.add(media);
            }
        }
        if (resolved.size() != mediaIds.size()) {
            return resolved;
        }

        long audioCount = 0;
        long videoCount = 0;
        long imageCount = 0;
        for (Media media : resolved) {
            EMediaKind kind = MediaKindResolver.resolve(media);
            switch (kind) {
                case AUDIO -> audioCount++;
                case VIDEO -> videoCount++;
                case IMAGE -> imageCount++;
            }
        }

        if (section == ESection.LISTENING) {
            if (isGroupItem && audioCount > 0) {
                errors.add("Câu hỏi con trong chùm Part 3/4 kế thừa audio chung của chùm, không được gắn audio riêng");
            }
            if (!isGroupItem && audioCount == 0) {
                errors.add("Câu hỏi thuộc phần thi LISTENING bắt buộc phải có 1 tệp âm thanh (Audio) đính kèm");
            }
        } else if (section == ESection.READING && audioCount > 0) {
            errors.add("Phần thi READING không được phép đính kèm tệp âm thanh (Audio)");
        }

        if (audioCount > MAX_AUDIO_PER_QUESTION) {
            errors.add("Mỗi câu hỏi chỉ được đính kèm tối đa " + MAX_AUDIO_PER_QUESTION + " tệp âm thanh");
        }
        if (videoCount > MAX_VIDEO_PER_QUESTION) {
            errors.add("Mỗi câu hỏi chỉ được đính kèm tối đa " + MAX_VIDEO_PER_QUESTION + " video");
        }
        if (imageCount > MAX_IMAGE_PER_QUESTION) {
            errors.add("Mỗi câu hỏi chỉ được đính kèm tối đa " + MAX_IMAGE_PER_QUESTION + " hình ảnh");
        }

        return resolved;
    }

    private void validateVocabularyFormat(String rawVocab, List<String> errors) {
        if (rawVocab == null || rawVocab.isBlank()) {
            return;
        }
        String[] entries = rawVocab.split(";");
        for (String entry : entries) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] parts = trimmed.split(":", 3);
            if (parts.length < 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
                errors.add("Mục từ vựng '" + trimmed + "' sai định dạng chuẩn 'từ:loại:nghĩa' (vd: arrange:v:sắp xếp)");
            }
        }
    }

    private void appendRowErrors(
            List<ImportRowError> allErrors,
            String sheet,
            int rowNumber,
            String groupRowId,
            String questionName,
            List<String> newMessages
    ) {
        for (ImportRowError existing : allErrors) {
            if (existing.getSheet().equals(sheet) && existing.getRowNumber() == rowNumber) {
                List<String> merged = new ArrayList<>(existing.getMessages());
                merged.addAll(newMessages);
                existing.setMessages(merged);
                return;
            }
        }
        allErrors.add(ImportRowError.builder()
                .sheet(sheet)
                .rowNumber(rowNumber)
                .groupRowId(groupRowId)
                .questionName(questionName)
                .messages(new ArrayList<>(newMessages))
                .build());
    }

    private QuestionImportResultResponse buildFailedResult(
            int standaloneCount,
            int groupCount,
            int groupItemCount,
            List<ImportRowError> errors
    ) {
        return QuestionImportResultResponse.builder()
                .imported(false)
                .totalRows(standaloneCount + groupCount)
                .standaloneCount(standaloneCount)
                .groupCount(groupCount)
                .groupItemCount(groupItemCount)
                .successCount(0)
                .failCount(errors.size())
                .errors(errors)
                .build();
    }

    private Long parsePositiveLong(String raw) {
        try {
            long value = Long.parseLong(raw.trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean isPartNumber(Part part, int partNo) {
        return part != null && part.getName() != null
                && part.getName().contains("Part " + partNo);
    }

    private boolean isStandalonePart(Part part) {
        return isPartNumber(part, 1) || isPartNumber(part, 2) || isPartNumber(part, 5);
    }

    private boolean isGroupPart(Part part) {
        return isPartNumber(part, 3) || isPartNumber(part, 4)
                || isPartNumber(part, 6) || isPartNumber(part, 7);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static final class GroupBucket {
        private final String groupRowId;
        private ImportGroupParentRowDto parent;
        private final List<ImportGroupItemRowDto> items = new ArrayList<>();

        private GroupBucket(String groupRowId) {
            this.groupRowId = groupRowId;
        }
    }
}
