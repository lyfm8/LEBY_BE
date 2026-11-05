package com.example.be.enums.media;

/**
 * Trạng thái vòng đời của tệp tin media trong hệ thống LEBY.
 */
public enum EMediaStatus {

    /**
     * File vừa được upload từ Client lên Cloudinary nhưng chưa được liên kết chính thức
     * vào bất kỳ thực thể nghiệp vụ nào (Question, Lesson, User Avatar).
     * Sẽ bị xóa tự động bởi Scheduler nếu hết hạn pendingExpiresAt.
     */
    PENDING,

    /**
     * File đã được liên kết chính thức với thực thể nghiệp vụ và được lưu trữ vĩnh viễn.
     */
    ACTIVE
}
