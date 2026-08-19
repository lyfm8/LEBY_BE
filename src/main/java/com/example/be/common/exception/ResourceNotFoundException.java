package com.example.be.common.exception;

import org.springframework.http.HttpStatus;

/**
 * HTTP 404 - Not Found.
 * Dùng cho resource không tồn tại trong hệ thống.
 *
 * Không tạo exception riêng cho từng entity. Sử dụng chung:
 *   new ResourceNotFoundException("User not found with id: " + id)
 *   new ResourceNotFoundException("Module not found with id: " + id)
 *   new ResourceNotFoundException("Question not found with id: " + id)
 */
public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
