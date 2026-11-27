package com.proyecto.servicios.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Filtro global defensivo contra inyecciones SQL (SQLi) y XSS / DOM en parámetros y cuerpos JSON.
 */
@Component
public class AntiInjectionFilter extends OncePerRequestFilter {

    private static final List<Pattern> DANGEROUS_PATTERNS = List.of(
            // XSS / DOM Injection
            Pattern.compile("<\\s*(script|iframe|svg|object|embed|img|body|link|style|meta|form)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("</\\s*script\\s*>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(javascript|vbscript|data\\s*:\\s*text/html)\\s*:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bon\\w+\\s*=", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(document\\.(cookie|domain|write|location)|window\\.location|innerHTML|outerHTML)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(eval|alert|prompt|confirm)\\s*\\(", Pattern.CASE_INSENSITIVE),
            // SQL Injection
            Pattern.compile("\\b(UNION\\s+ALL\\s+SELECT|UNION\\s+SELECT|SELECT\\s+.+\\s+FROM|INSERT\\s+INTO|UPDATE\\s+.+\\s+SET|DELETE\\s+FROM|DROP\\s+(TABLE|DATABASE)|ALTER\\s+TABLE|TRUNCATE\\s+TABLE|EXEC\\s+|XP_CMDSHELL|INFORMATION_SCHEMA)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(WAITFOR\\s+DELAY|SLEEP\\s*\\(|PG_SLEEP\\s*\\(|BENCHMARK\\s*\\()", Pattern.CASE_INSENSITIVE),
            Pattern.compile("('\\s*(OR|AND)\\s*'\\s*=\\s*'|'\\s*(OR|AND)\\s+1\\s*=\\s*1|\\bOR\\s+1\\s*=\\s*1\\b)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(/\\*.*\\*/|\\x00)")
    );

    private boolean isMalicious(String value) {
        if (value == null || value.isEmpty()) return false;
        for (Pattern pattern : DANGEROUS_PATTERNS) {
            if (pattern.matcher(value).find()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // 1. Verificar URI y parámetros de consulta
        if (isMalicious(request.getRequestURI()) || isMalicious(request.getQueryString())) {
            reject(response);
            return;
        }
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            if (isMalicious(entry.getKey())) {
                reject(response);
                return;
            }
            if (entry.getValue() != null) {
                for (String v : entry.getValue()) {
                    if (isMalicious(v)) {
                        reject(response);
                        return;
                    }
                }
            }
        }

        // 2. Verificar cuerpo JSON si existe
        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase().contains("application/json")) {
            CachedBodyHttpServletRequest wrapped = new CachedBodyHttpServletRequest(request);
            String body = wrapped.getBodyAsString();
            if (isMalicious(body)) {
                reject(response);
                return;
            }
            filterChain.doFilter(wrapped, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"codigo\": 1, \"mensaje\": \"Entrada rechazada por politica de seguridad.\"}");
    }

    private static class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
        private final byte[] cachedBody;

        CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.cachedBody = request.getInputStream().readAllBytes();
        }

        String getBodyAsString() {
            return new String(cachedBody, StandardCharsets.UTF_8);
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream bais = new ByteArrayInputStream(cachedBody);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return bais.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {}

                @Override
                public int read() {
                    return bais.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
