package com.example.be.enums.media;

/**
 * Loại tài nguyên tệp tin media được hỗ trợ tải lên đám mây.
 */
public enum EMediaResourceType {

    /**
     * Tệp tin hình ảnh (ảnh chụp câu hỏi TOEIC, biểu đồ, avatar...).
     */
    IMAGE,

    /**
     * Tệp tin video (bài giảng video bài học, video giải thích câu hỏi...).
     */
    VIDEO,

    /**
     * Tệp tin thô / âm thanh (Audio bài nghe TOEIC Part 1 - Part 4, file tài liệu đính kèm...).
     */
    RAW,

    /**
     * Tự động nhận diện bởi Cloudinary dựa trên MIME type của file.
     */
    AUTO
}
