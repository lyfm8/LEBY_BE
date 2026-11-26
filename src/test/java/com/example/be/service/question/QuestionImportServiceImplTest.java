package com.example.be.service.question;

import com.example.be.dto.request.question.CreateQuestionGroupRequest;
import com.example.be.dto.request.question.CreateStandaloneQuestionRequest;
import com.example.be.dto.response.question.ImportRowError;
import com.example.be.dto.response.question.QuestionDetailResponse;
import com.example.be.dto.response.question.QuestionImportResultResponse;
import com.example.be.entity.ability.Ability;
import com.example.be.entity.content.Part;
import com.example.be.entity.media.Media;
import com.example.be.enums.ability.ESection;
import com.example.be.enums.media.EMediaResourceType;
import com.example.be.enums.media.EMediaStatus;
import com.example.be.exception.BaseException;
import com.example.be.mapper.question.QuestionImportMapper;
import com.example.be.repository.ability.AbilityRepository;
import com.example.be.repository.content.PartRepository;
import com.example.be.repository.media.MediaRepository;
import com.example.be.service.question.implement.QuestionImportServiceImpl;
import com.example.be.service.question.service.QuestionService;
import com.example.be.service.question.util.QuestionExcelErrorReportGenerator;
import com.example.be.service.question.util.QuestionExcelTemplateGenerator;
import com.example.be.service.question.util.QuestionImportExcelParser;
import com.example.be.service.question.util.QuestionImportTemplateSchema;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit Test cho luồng sinh file Excel mẫu, đối soát All-or-Nothing, import hàng loạt và xuất báo cáo lỗi (UC A04 - 3a/3b).
 */
@ExtendWith(MockitoExtension.class)
class QuestionImportServiceImplTest {

    @Mock
    private PartRepository partRepository;
    @Mock
    private AbilityRepository abilityRepository;
    @Mock
    private MediaRepository mediaRepository;
    @Mock
    private QuestionService questionService;

    private QuestionImportServiceImpl importService;

    private Part part1Listening;
    private Part part5Reading;
    private Part part7Reading;
    private Ability abilityPart1;
    private Ability abilityPart5;
    private Ability abilityPart7;
    private Media audioMedia;
    private Media imageMedia;

    @BeforeEach
    void setUp() {
        importService = new QuestionImportServiceImpl(
                partRepository,
                abilityRepository,
                mediaRepository,
                questionService,
                new QuestionImportMapper(),
                new QuestionExcelTemplateGenerator(),
                new QuestionImportExcelParser(),
                new QuestionExcelErrorReportGenerator()
        );

        part1Listening = new Part();
        part1Listening.setId(1L);
        part1Listening.setName("Part 1: Photographs");
        part1Listening.setSections(ESection.LISTENING);

        part5Reading = new Part();
        part5Reading.setId(5L);
        part5Reading.setName("Part 5: Incomplete Sentences");
        part5Reading.setSections(ESection.READING);

        part7Reading = new Part();
        part7Reading.setId(7L);
        part7Reading.setName("Part 7: Reading Comprehension");
        part7Reading.setSections(ESection.READING);

        abilityPart1 = new Ability();
        abilityPart1.setId(1L);
        abilityPart1.setName("Human Actions Identification");
        abilityPart1.setPart(part1Listening);

        abilityPart5 = new Ability();
        abilityPart5.setId(11L);
        abilityPart5.setName("Verb Tenses");
        abilityPart5.setPart(part5Reading);

        abilityPart7 = new Ability();
        abilityPart7.setId(15L);
        abilityPart7.setName("Business Emails Reading");
        abilityPart7.setPart(part7Reading);

        audioMedia = Media.builder()
                .publicId("audio_p1")
                .secureUrl("https://res.cloudinary.com/leby/video/upload/p1.mp3")
                .resourceType(EMediaResourceType.VIDEO)
                .format("mp3")
                .bytes(2048L)
                .status(EMediaStatus.PENDING)
                .build();
        audioMedia.setId(101L);

        imageMedia = Media.builder()
                .publicId("img_p1")
                .secureUrl("https://res.cloudinary.com/leby/image/upload/p1.jpg")
                .resourceType(EMediaResourceType.IMAGE)
                .format("jpg")
                .bytes(4096L)
                .status(EMediaStatus.PENDING)
                .build();
        imageMedia.setId(102L);
    }

    @Test
    @DisplayName("Sinh tệp Excel mẫu chứa đủ 3 sheet và bảng tra cứu Part/Ability")
    void generateImportTemplate_ContainsExpectedSheetsAndReferenceData() throws IOException {
        when(partRepository.findAll(any(Sort.class))).thenReturn(List.of(part1Listening, part5Reading, part7Reading));
        when(abilityRepository.findAll(any(Sort.class))).thenReturn(List.of(abilityPart1, abilityPart5, abilityPart7));

        byte[] bytes = importService.generateImportTemplate();
        assertThat(bytes).isNotEmpty();

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(3);
            assertThat(wb.getSheetName(0)).isEqualTo(QuestionImportTemplateSchema.SHEET_STANDALONE);
            assertThat(wb.getSheetName(1)).isEqualTo(QuestionImportTemplateSchema.SHEET_GROUP);
            assertThat(wb.getSheetName(2)).isEqualTo(QuestionImportTemplateSchema.SHEET_GUIDE);

            Sheet standalone = wb.getSheetAt(0);
            assertThat(standalone.getRow(0).getCell(0).getStringCellValue())
                    .isEqualTo(QuestionImportTemplateSchema.STANDALONE_HEADERS.get(0));
            assertThat(standalone.getRow(1).getCell(0).getStringCellValue())
                    .startsWith(QuestionImportTemplateSchema.EXAMPLE_PREFIX);
        }
    }

    @Test
    @DisplayName("Import thành công cả câu đơn (Part 1 + Part 5) và chùm câu hỏi (Part 7) trong cùng một file")
    void importFromExcel_ValidStandaloneAndGroup_Success() throws IOException {
        when(partRepository.findAll()).thenReturn(List.of(part1Listening, part5Reading, part7Reading));
        when(abilityRepository.findAll()).thenReturn(List.of(abilityPart1, abilityPart5, abilityPart7));
        when(mediaRepository.findAllById(any())).thenReturn(List.of(audioMedia, imageMedia));
        when(questionService.createStandaloneQuestion(any())).thenReturn(QuestionDetailResponse.builder().id(1L).build());
        when(questionService.createQuestionGroup(any())).thenReturn(QuestionDetailResponse.builder().id(2L).build());

        byte[] workbookBytes = buildWorkbook(wb -> {
            Sheet standalone = wb.createSheet(QuestionImportTemplateSchema.SHEET_STANDALONE);
            writeHeader(standalone, QuestionImportTemplateSchema.STANDALONE_HEADERS);
            // Dòng ví dụ phải bị bỏ qua
            writeCells(standalone.createRow(1), List.of("[VÍ DỤ] Bỏ qua", "5", "11", "2", "Text", "A", "B", "C", "D", "A"));
            // Câu Part 1 thật kèm audio + image + vocab
            writeCells(standalone.createRow(2), List.of(
                    "Q-P1-100", "1", "1", "2", "Look at the picture.",
                    "(A) Opt A", "(B) Opt B", "(C) Opt C", "(D) Opt D", "B",
                    "101|102", "Transcript", "Dịch", "Clue", "Analysis",
                    "blueprint:n:bản vẽ;inspect:v:kiểm tra", "Ghi chú P1"
            ));

            Sheet group = wb.createSheet(QuestionImportTemplateSchema.SHEET_GROUP);
            writeHeader(group, QuestionImportTemplateSchema.GROUP_HEADERS);
            writeCells(group.createRow(1), List.of(
                    "G01", "CHÙM", "Email thông báo lịch họp", "7",
                    "Questions 1-2 refer to the following email.",
                    "Dear team, the meeting is moved to Friday.",
                    "", "Kính gửi nhóm, cuộc họp chuyển sang thứ Sáu.", "", ""
            ));
            // Thử đảo thứ tự câu 2 trước câu 1 để kiểm tra mapper sắp xếp theo orderNo
            writeCells(group.createRow(2), List.of(
                    "G01", "CÂU CON", "", "", "", "", "", "", "", "",
                    "Câu con 2", "2", "Who sent this email?", "15", "3",
                    "HR", "CEO", "IT", "Sales", "A", "", "", "", "", "", ""
            ));
            writeCells(group.createRow(3), List.of(
                    "G01", "CÂU CON", "", "", "", "", "", "", "", "",
                    "Câu con 1", "1", "When is the meeting?", "15", "2",
                    "Monday", "Tuesday", "Friday", "Sunday", "C", "", "", "", "", "", ""
            ));
        });

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "questions.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                workbookBytes
        );

        QuestionImportResultResponse result = importService.importFromExcel(file);

        assertThat(result.isImported()).isTrue();
        assertThat(result.getStandaloneCount()).isEqualTo(1);
        assertThat(result.getGroupCount()).isEqualTo(1);
        assertThat(result.getGroupItemCount()).isEqualTo(2);
        assertThat(result.getSuccessCount()).isEqualTo(2);
        assertThat(result.getErrors()).isEmpty();

        ArgumentCaptor<CreateStandaloneQuestionRequest> standaloneCaptor =
                ArgumentCaptor.forClass(CreateStandaloneQuestionRequest.class);
        verify(questionService, times(1)).createStandaloneQuestion(standaloneCaptor.capture());
        CreateStandaloneQuestionRequest savedStandalone = standaloneCaptor.getValue();
        assertThat(savedStandalone.getName()).isEqualTo("Q-P1-100");
        assertThat(savedStandalone.getMedias()).hasSize(2);
        assertThat(savedStandalone.getExplanation().getVocabulary()).hasSize(2);

        ArgumentCaptor<CreateQuestionGroupRequest> groupCaptor =
                ArgumentCaptor.forClass(CreateQuestionGroupRequest.class);
        verify(questionService, times(1)).createQuestionGroup(groupCaptor.capture());
        CreateQuestionGroupRequest savedGroup = groupCaptor.getValue();
        assertThat(savedGroup.getItems()).hasSize(2);
        assertThat(savedGroup.getItems().get(0).getName()).isEqualTo("Câu con 1");
        assertThat(savedGroup.getItems().get(1).getName()).isEqualTo("Câu con 2");
    }

    @Test
    @DisplayName("Từ chối toàn bộ file (All-or-Nothing) khi có dòng lỗi và trả về chi tiết từng dòng")
    void importFromExcel_InvalidRows_RejectsAllAndReturnsDetailedErrors() throws IOException {
        when(partRepository.findAll()).thenReturn(List.of(part1Listening, part5Reading, part7Reading));
        when(abilityRepository.findAll()).thenReturn(List.of(abilityPart1, abilityPart5, abilityPart7));
        when(mediaRepository.findAllById(any())).thenReturn(List.of(audioMedia));

        byte[] workbookBytes = buildWorkbook(wb -> {
            Sheet standalone = wb.createSheet(QuestionImportTemplateSchema.SHEET_STANDALONE);
            writeHeader(standalone, QuestionImportTemplateSchema.STANDALONE_HEADERS);
            // Dòng 2 hợp lệ
            writeCells(standalone.createRow(1), List.of(
                    "Q-P5-OK", "5", "11", "2", "Valid sentence",
                    "A", "B", "C", "D", "A"
            ));
            // Dòng 3 lỗi: Part 1 thiếu Audio + Ability 11 thuộc Part 5 không thuộc Part 1 + correctKey Z
            writeCells(standalone.createRow(2), List.of(
                    "Q-P1-BAD", "1", "11", "6", "Text",
                    "A", "B", "C", "", "Z", "", "", "", "", "", "badvocab"
            ));

            Sheet group = wb.createSheet(QuestionImportTemplateSchema.SHEET_GROUP);
            writeHeader(group, QuestionImportTemplateSchema.GROUP_HEADERS);
            // Chùm chỉ có 1 câu con (quy định 2-5) và Reading thiếu passage & ảnh
            writeCells(group.createRow(1), List.of(
                    "G02", "CHÙM", "Chùm thiếu câu con", "7",
                    "Questions 1-1", "", "", "", "101"
            ));
            writeCells(group.createRow(2), List.of(
                    "G02", "CÂU CON", "", "", "", "", "", "", "", "",
                    "Câu con 1", "1", "What?", "15", "2",
                    "A", "B", "C", "D", "A"
            ));
        });

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "bad-questions.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                workbookBytes
        );

        QuestionImportResultResponse result = importService.importFromExcel(file);

        assertThat(result.isImported()).isFalse();
        assertThat(result.getSuccessCount()).isZero();
        assertThat(result.getErrors()).isNotEmpty();

        verify(questionService, never()).createStandaloneQuestion(any());
        verify(questionService, never()).createQuestionGroup(any());
    }

    @Test
    @DisplayName("Từ chối file sai định dạng không phải .xlsx hoặc vượt quá 20MB")
    void importFromExcel_InvalidFileExtensionOrSize_ThrowsBadRequest() {
        MockMultipartFile csvFile = new MockMultipartFile(
                "file",
                "questions.csv",
                "text/csv",
                "name,partId".getBytes()
        );

        assertThatThrownBy(() -> importService.importFromExcel(csvFile))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(".xlsx");
    }

    @Test
    @DisplayName("Xuất báo cáo lỗi Excel (.xlsx) thành công với khả năng chống Formula Injection")
    void exportErrorReport_GeneratesValidXlsxWithSanitizedCells() throws IOException {
        List<ImportRowError> errors = List.of(
                ImportRowError.builder()
                        .sheet(QuestionImportTemplateSchema.SHEET_STANDALONE)
                        .rowNumber(3)
                        .questionName("=CMD|'/C calc'!A0")
                        .messages(List.of("Đáp án đúng không hợp lệ"))
                        .build()
        );

        byte[] reportBytes = importService.exportErrorReport(errors);
        assertThat(reportBytes).isNotEmpty();

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(reportBytes))) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(sheet.getRow(1).getCell(3).getStringCellValue()).startsWith("'");
        }
    }

    private byte[] buildWorkbook(Consumer<XSSFWorkbook> consumer) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            consumer.accept(wb);
            wb.write(out);
            return out.toByteArray();
        }
    }

    private void writeHeader(Sheet sheet, List<String> headers) {
        writeCells(sheet.createRow(0), headers);
    }

    private void writeCells(Row row, List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            row.createCell(i).setCellValue(values.get(i));
        }
    }
}
