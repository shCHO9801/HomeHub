package io.github.shhome.homehub.agent.security;

import io.github.shhome.homehub.agent.config.AgentProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AgentTokenAuthenticationFilter
        extends OncePerRequestFilter {

    private static final String AGENT_PATH_PREFIX = "/api/agent/";
    private static final String UNAUTHORIZED_BODY =
            "{\"status\":401,"
                    + "\"message\":\"Agent 인증에 실패했습니다.\"}";

    private final AgentProperties properties;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(
                AGENT_PATH_PREFIX
        );
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String expected = "Bearer " + properties.token();
        String received = request.getHeader(
                HttpHeaders.AUTHORIZATION
        );

        if (!matches(expected, received)) {
            writeUnauthorized(response);
            return;
        }

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        "homehub-backend",
                        null,
                        List.of()
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private boolean matches(String expected, String received) {
        String normalizedReceived =
                received == null ? "" : received;

        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                normalizedReceived.getBytes(StandardCharsets.UTF_8)
        );
    }

    private void writeUnauthorized(HttpServletResponse response)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(UNAUTHORIZED_BODY);
    }
}
