package io.github.shhome.homehub.auth;

import io.github.shhome.homehub.user.entity.User;
import io.github.shhome.homehub.user.entity.UserRole;
import io.github.shhome.homehub.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("인증 통합 테스트")
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("로그인 성공 후 현재 사용자 정보를 조회한다")
    void loginSuccessAndGetCurrentUser() throws Exception {
        String username = "auth-test-" + UUID.randomUUID();
        String password = "test-password";

        User user = User.of(
                username,
                passwordEncoder.encode(password),
                UserRole.MEMBER
        );

        userRepository.saveAndFlush(user);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .param("username", username)
                        .param("password", password))
                .andExpect(status().isNoContent())
                .andReturn();

        MockHttpSession session = (MockHttpSession)
                loginResult.getRequest().getSession(false);

        assertNotNull(session);

        mockMvc.perform(get("/api/auth/me")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.role").value("MEMBER"));
    }

    @Test
    @DisplayName("존재하지 않는 사용자 로그인은 인증 실패 응답을 반환한다")
    void loginFailsWhenUserDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .param(
                                "username",
                                "missing-" + UUID.randomUUID()
                        )
                        .param("password", "wrong-password"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("AUTH_401_01"))
                .andExpect(jsonPath("$.message")
                        .value("아이디 또는 비밀번호가 올바르지 않습니다."));
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 인증 실패 응답을 반환한다")
    void loginFailsWhenPasswordDoesNotMatch() throws Exception {
        String username = "auth-test-" + UUID.randomUUID();

        User user = User.of(
                username,
                passwordEncoder.encode("correct-password"),
                UserRole.MEMBER
        );

        userRepository.saveAndFlush(user);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .param("username", username)
                        .param("password", "wrong-password"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("AUTH_401_01"))
                .andExpect(jsonPath("$.message")
                        .value("아이디 또는 비밀번호가 올바르지 않습니다."));
    }

    @Test
    @DisplayName("비활성 사용자는 로그인할 수 없다")
    void disabledUserCannotLogin() throws Exception {
        String username = "auth-test-" + UUID.randomUUID();
        String password = "test-password";

        User user = User.of(
                username,
                passwordEncoder.encode(password),
                UserRole.MEMBER
        );

        user.disable();
        userRepository.saveAndFlush(user);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .param("username", username)
                        .param("password", password))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("AUTH_401_01"))
                .andExpect(jsonPath("$.message")
                        .value("아이디 또는 비밀번호가 올바르지 않습니다."));
    }

    @Test
    @DisplayName("인증 없이 현재 사용자 조회 시 로그인이 필요하다")
    void currentUserRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("AUTH_401_02"))
                .andExpect(jsonPath("$.message")
                        .value("로그인이 필요합니다."));
    }

    @Test
    @DisplayName("CSRF 토큰이 없는 로그인 요청은 거부한다")
    void loginWithoutCsrfIsDenied() throws Exception {
        MvcResult csrfResult = mockMvc.perform(
                        get("/api/auth/csrf")
                )
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession)
                csrfResult.getRequest().getSession(false);

        assertNotNull(session);

        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .param("username", "test-user")
                        .param("password", "test-password"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("AUTH_403_01"))
                .andExpect(jsonPath("$.message")
                        .value("접근 권한이 없습니다."));
    }

    @Test
    @DisplayName("로그아웃하면 인증 세션을 폐기한다")
    void logoutInvalidatesSession() throws Exception {
        String username = "auth-test-" + UUID.randomUUID();
        String password = "test-password";

        User user = User.of(
                username,
                passwordEncoder.encode(password),
                UserRole.MEMBER
        );

        userRepository.saveAndFlush(user);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .param("username", username)
                        .param("password", password))
                .andExpect(status().isNoContent())
                .andReturn();

        MockHttpSession session = (MockHttpSession)
                loginResult.getRequest().getSession(false);

        assertNotNull(session);

        mockMvc.perform(post("/api/auth/logout")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        assertTrue(session.isInvalid());
    }
}