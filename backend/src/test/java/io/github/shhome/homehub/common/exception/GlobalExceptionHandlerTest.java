package io.github.shhome.homehub.common.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import({
        GlobalExceptionHandler.class,
        GlobalExceptionHandlerTest.TestController.class
})
@DisplayName("공통 예외 처리")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("비즈니스 예외는 오류 코드 응답을 반환한다")
    void businessExceptionReturnsErrorCodeResponse() throws Exception {
        mockMvc.perform(get("/test/business"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));
    }

    @Test
    @DisplayName("요청 본문 검증 실패는 필드와 메시지를 반환한다")
    void validationFailureReturnsFieldAndMessage() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("이름은 필수입니다."))
                .andExpect(jsonPath("$.field").value("name"));
    }

    @Test
    @DisplayName("잘못된 JSON 요청은 400 응답을 반환한다")
    void unreadableJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("요청 형식이 올바르지 않습니다."));
    }

    @Test
    @DisplayName("필수 파라미터 누락은 파라미터 이름을 반환한다")
    void missingParameterReturnsParameterName() throws Exception {
        mockMvc.perform(get("/test/parameter"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("필수 요청 파라미터가 누락되었습니다."))
                .andExpect(jsonPath("$.field").value("count"));
    }

    @Test
    @DisplayName("파라미터 타입 오류는 파라미터 이름을 반환한다")
    void invalidParameterTypeReturnsParameterName() throws Exception {
        mockMvc.perform(get("/test/parameter")
                        .param("count", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("요청 파라미터 형식이 올바르지 않습니다."))
                .andExpect(jsonPath("$.field").value("count"));
    }

    @Test
    @DisplayName("메서드 파라미터 검증 실패는 검증 메시지를 반환한다")
    void methodValidationFailureReturnsParameterName() throws Exception {
        mockMvc.perform(get("/test/parameter")
                        .param("count", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("1 이상이어야 합니다."))
                .andExpect(jsonPath("$.field").value("count"));
    }

    @Test
    @DisplayName("예상하지 못한 오류는 내부 메시지를 숨긴다")
    void unexpectedRuntimeExceptionHidesInternalMessage() throws Exception {
        mockMvc.perform(get("/test/runtime"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code").value("COMMON_500"))
                .andExpect(jsonPath("$.message")
                        .value("서버 오류가 발생했습니다."));
    }

    @RestController
    public static class TestController {

        @GetMapping("/test/business")
        void businessException() {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        @PostMapping("/test/validation")
        void validation(
                @Valid @RequestBody ValidationRequest request
        ) {
        }

        @GetMapping("/test/parameter")
        void parameter(
                @RequestParam
                @Min(value = 1, message = "1 이상이어야 합니다.")
                int count
        ) {
        }

        @GetMapping("/test/runtime")
        void runtimeException() {
            throw new RuntimeException("외부에 노출되면 안 되는 내부 메시지");
        }
    }

    public record ValidationRequest(
            @NotBlank(message = "이름은 필수입니다.")
            String name
    ) {
    }
}
