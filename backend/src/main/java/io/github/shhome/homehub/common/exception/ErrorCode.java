package io.github.shhome.homehub.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_REQUEST(
            HttpStatus.BAD_REQUEST,
            "COMMON_400",
            "잘못된 요청입니다."
    ),

    AUTHENTICATION_FAILED(
            HttpStatus.UNAUTHORIZED,
            "AUTH_401_01",
            "아이디 또는 비밀번호가 올바르지 않습니다."
    ),

    AUTHENTICATION_REQUIRED(
            HttpStatus.UNAUTHORIZED,
            "AUTH_401_02",
            "로그인이 필요합니다."
    ),

    ACCESS_DENIED(
            HttpStatus.FORBIDDEN,
            "AUTH_403_01",
            "접근 권한이 없습니다."
    ),

    USER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "USER_404_01",
            "사용자를 찾을 수 없습니다."
    ),

    USER_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "USER_409_01",
            "이미 존재하는 사용자명입니다."
    ),

    INTERNAL_SERVER_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "COMMON_500",
            "서버 오류가 발생했습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
