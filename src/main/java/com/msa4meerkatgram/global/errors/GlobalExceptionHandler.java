package com.msa4meerkatgram.global.errors;

import com.msa4meerkatgram.global.errors.constant.CustomErrorCode;
import com.msa4meerkatgram.global.errors.custom.*;
import com.msa4meerkatgram.global.responses.GlobalErrorRes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.nio.file.AccessDeniedException;

import java.sql.SQLException;
import java.util.Map;
import java.util.stream.Collectors;

// 커스텀한 Exception을 위해 만든 객체
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    private ResponseEntity<GlobalErrorRes> generateErrorResponse(CustomErrorCode customErrorCode) {
        return ResponseEntity.status(customErrorCode.getHttpStatus()).body(
                GlobalErrorRes.from(customErrorCode.getCode(), customErrorCode.name())
        );
    }

    // 클라이언트가 보낸 로그인이 양식에 맞지 않음
    @ExceptionHandler(NotRegisteredException.class)
    public ResponseEntity<GlobalErrorRes> notRegisteredException(NotRegisteredException e) {
        log.debug(CustomErrorCode.NOT_REGISTERED_ERROR.name(), e);
        return this.generateErrorResponse(CustomErrorCode.NOT_REGISTERED_ERROR);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<GlobalErrorRes> authenticationHandle(AuthenticationException e) {
        log.debug(CustomErrorCode.UNAUTHENTICATED_ERROR.name(), e);
        return this.generateErrorResponse(CustomErrorCode.UNAUTHENTICATED_ERROR);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<GlobalErrorRes> accessDeniedHandle(AccessDeniedException e) {
        log.debug(CustomErrorCode.UNAUTHORIZED_ERROR.name(), e);
        return this.generateErrorResponse(CustomErrorCode.UNAUTHORIZED_ERROR);
    }

    // 클라이언트 토큰 만료
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<GlobalErrorRes> invalidTokenHandle(InvalidTokenException e) {
        log.debug(CustomErrorCode.INVALID_TOKEN_ERROR.name(), e);
        return this.generateErrorResponse(CustomErrorCode.INVALID_TOKEN_ERROR);
    }

    @ExceptionHandler(DeletedRecordException.class)
    public ResponseEntity<GlobalErrorRes> deletedRecordHandler(DeletedRecordException e) {
        log.debug(CustomErrorCode.NOT_FOUND_DATA_ERROR.name(), e);
        return this.generateErrorResponse(CustomErrorCode.NOT_FOUND_DATA_ERROR);
    }

    @ExceptionHandler(DuplicatedRecordException.class)
    public ResponseEntity<GlobalErrorRes> duplicatedRecordHandle(DuplicatedRecordException e) {
        log.debug(CustomErrorCode.DUPLICATED_DATA_ERROR.name(), e);
        return this.generateErrorResponse(CustomErrorCode.DUPLICATED_DATA_ERROR);
    }

    // 클라이언트가 보낸 하나의 타입 파라미터가 미스 매치됐을때 실행됨
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<GlobalErrorRes> methodArgumentTypeMismatchHandle(MethodArgumentTypeMismatchException e) {
        log.debug(CustomErrorCode.INVALID_PARAMETER_ERROR.name(), String.format("%s : 필드를 확인해 주세요", e.getName()));
        return this.generateErrorResponse(CustomErrorCode.INVALID_PARAMETER_ERROR);
    }

    // 클라이언트가 보낸 복수의 파라미터가 미스매차됨
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GlobalErrorRes> methodArgumentNotValidHandle(MethodArgumentNotValidException e) {
        Map<String, String> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField, // 필드명
                        fieldError -> fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "유효하지 않은 값입니다.",
                        (existing, replacement) -> existing // 중복 필드가 있을 경우 기존 값 유지
                ));

        log.debug(CustomErrorCode.INVALID_PARAMETER_ERROR.name(), errors);
        return this.generateErrorResponse(CustomErrorCode.INVALID_PARAMETER_ERROR);
    }

    @ExceptionHandler(FileManagedException.class)
    public ResponseEntity<GlobalErrorRes> fileManagedHandle(FileManagedException e) {
        log.debug(CustomErrorCode.FILE_MANAGED_ERROR.name(), e);
        return this.generateErrorResponse(CustomErrorCode.FILE_MANAGED_ERROR);
    }

    @ExceptionHandler(SQLException.class)
    public ResponseEntity<GlobalErrorRes> SQLHandle(SQLException e) {
        log.error("DB 에러", e);
        return this.generateErrorResponse(CustomErrorCode.DB_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalErrorRes> otherHandle(Exception e) {
        log.error("시스템 에러", e);
        return this.generateErrorResponse(CustomErrorCode.SYSTEM_ERROR);
    }
}