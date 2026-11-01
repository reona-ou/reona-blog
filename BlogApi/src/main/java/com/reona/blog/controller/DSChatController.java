package com.reona.blog.controller;

import com.reona.blog.config.AuthAccess;
import com.reona.blog.entity.AssistantSetting;
import com.reona.blog.entity.SiteSetting;
import com.reona.blog.mapper.SiteSettingMapper;
import com.reona.blog.service.AssistantSettingService;
import com.reona.blog.service.RateLimiter;
import com.reona.blog.utils.AssistantPromptBuilder;
import com.reona.blog.utils.ClientIpUtils;
import com.reona.blog.utils.ReplyLanguageResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

@Slf4j
@RestController
@RequestMapping("/chat")
public class DSChatController {

    private static final int MAX_REQUESTS_PER_MINUTE = 15;
    private static final int MAX_CONCURRENT = 2;
    private static final long REQUEST_WINDOW_MS = 60_000L;

    private final Semaphore concurrentSlots = new Semaphore(MAX_CONCURRENT);
    private final RestTemplate restTemplate = createRestTemplate();

    @Resource
    private SiteSettingMapper siteSettingMapper;

    @Resource
    private AssistantSettingService assistantSettingService;

    @Resource
    private RateLimiter rateLimiter;

    @Value("${proxy.trusted:127.0.0.1,0:0:0:0:0:0:0:1,::1}")
    private String trustedProxies;

    @AuthAccess
    @PostMapping("/completions")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, Object> request,
                                                    HttpServletRequest httpRequest) {
        String ip = ClientIpUtils.clientIp(httpRequest, trustedProxies);
        if (!rateLimiter.tryAcquire("chat|" + ip, MAX_REQUESTS_PER_MINUTE, REQUEST_WINDOW_MS)) {
            return error(HttpStatus.TOO_MANY_REQUESTS, "问得有点快，稍等一下再试");
        }
        if (!concurrentSlots.tryAcquire()) {
            return error(HttpStatus.TOO_MANY_REQUESTS, "助手正在想别的问题，稍等一下再试");
        }

        try {
            AssistantSetting assistant = assistantSettingService.get();
            if (!assistantSettingService.enabled(assistant)) {
                return error(HttpStatus.SERVICE_UNAVAILABLE, "站点助手暂时关闭");
            }
            List<Map<String, Object>> history = sanitizeMessages(request.get("messages"));
            if (history.isEmpty()) {
                return error(HttpStatus.BAD_REQUEST, "请先输入内容");
            }
            String language = ReplyLanguageResolver.resolve(
                    request.get("language") == null ? "zh" : String.valueOf(request.get("language")),
                    history);
            Map<String, Object> payload = buildPayload(assistant, history, language, false);

            try {
                ResponseEntity<Map> response = restTemplate.postForEntity(
                        assistantSettingService.apiUrl(assistant),
                        new HttpEntity<>(payload, buildHeaders(assistant)),
                        Map.class
                );
                if (response.getBody() == null) {
                    return error(HttpStatus.BAD_GATEWAY, "AI 服务没有返回内容");
                }
                return ResponseEntity.<Map<String, Object>>ok()
                        .header("X-Reply-Language", language)
                        .body(response.getBody());
            } catch (RestClientException e) {
                return error(HttpStatus.SERVICE_UNAVAILABLE, "AI 助手暂时不可用，请稍后再试");
            }
        } finally {
            concurrentSlots.release();
        }
    }

    @AuthAccess
    @PostMapping("/stream")
    public void stream(@RequestBody Map<String, Object> request,
                       HttpServletRequest httpRequest,
                       HttpServletResponse response) throws IOException {
        String ip = ClientIpUtils.clientIp(httpRequest, trustedProxies);
        if (!rateLimiter.tryAcquire("chat|" + ip, MAX_REQUESTS_PER_MINUTE, REQUEST_WINDOW_MS)) {
            writeJsonError(response, HttpStatus.TOO_MANY_REQUESTS, "问得有点快，稍等一下再试");
            return;
        }
        if (!concurrentSlots.tryAcquire()) {
            writeJsonError(response, HttpStatus.TOO_MANY_REQUESTS, "助手正在想别的问题，稍等一下再试");
            return;
        }

        try {
            AssistantSetting assistant = assistantSettingService.get();
            if (!assistantSettingService.enabled(assistant)) {
                writeJsonError(response, HttpStatus.SERVICE_UNAVAILABLE, "站点助手暂时关闭");
                return;
            }
            List<Map<String, Object>> history = sanitizeMessages(request.get("messages"));
            if (history.isEmpty()) {
                writeJsonError(response, HttpStatus.BAD_REQUEST, "请先输入内容");
                return;
            }
            String language = ReplyLanguageResolver.resolve(
                    request.get("language") == null ? "zh" : String.valueOf(request.get("language")),
                    history);
            Map<String, Object> payload = buildPayload(assistant, history, language, true);
            HttpHeaders headers = buildHeaders(assistant);

            response.setStatus(HttpStatus.OK.value());
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
            response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
            response.setHeader("X-Accel-Buffering", "no");
            response.setHeader("X-Reply-Language", language);
            OutputStream output = response.getOutputStream();
            output.write(": connected\n\n".getBytes(StandardCharsets.UTF_8));
            output.flush();
            response.flushBuffer();

            try {
                restTemplate.execute(assistantSettingService.apiUrl(assistant), HttpMethod.POST, req -> {
                    req.getHeaders().putAll(headers);
                    HttpMessageConverter<Object> writer = jsonWriter();
                    writer.write(payload, MediaType.APPLICATION_JSON, req);
                }, upstream -> {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(upstream.getBody(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            output.write((line + "\n").getBytes(StandardCharsets.UTF_8));
                            if (line.isEmpty()) {
                                output.flush();
                            }
                        }
                    }
                    output.flush();
                    return null;
                });
            } catch (RestClientResponseException e) {
                writeSseError(output, "AI 助手暂时不可用，请稍后再试");
            } catch (RestClientException e) {
                log.debug("流式对话结束: {}", e.getMessage());
                writeSseError(output, "AI 助手暂时不可用，请稍后再试");
            }
        } finally {
            concurrentSlots.release();
        }
    }

    private Map<String, Object> buildPayload(AssistantSetting assistant,
                                             List<Map<String, Object>> history,
                                             String language,
                                             boolean stream) {
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> system = new LinkedHashMap<>();
        system.put("role", "system");
        system.put("content", buildSystemPrompt(language, assistant));
        messages.add(system);
        messages.addAll(history);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", assistantSettingService.model(assistant));
        payload.put("messages", messages);
        payload.put("temperature", assistantSettingService.temperature(assistant));
        payload.put("max_tokens", assistantSettingService.maxTokens(assistant));
        payload.put("stream", stream);
        return payload;
    }

    private HttpHeaders buildHeaders(AssistantSetting assistant) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String apiKey = assistant.getApiKey();
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            headers.setBearerAuth(apiKey.trim());
        }
        return headers;
    }

    private String buildSystemPrompt(String language, AssistantSetting assistant) {
        SiteSetting setting = siteSettingMapper.selectById(1);
        String siteName = setting == null || setting.getSiteName() == null || setting.getSiteName().trim().isEmpty()
                ? "Default Blog" : setting.getSiteName().trim();
        String email = setting == null || setting.getEmail() == null ? "" : setting.getEmail().trim();
        return AssistantPromptBuilder.render(
                assistant.getPersonaPrompt(),
                siteName,
                AssistantPromptBuilder.contactText(email, language),
                assistantSettingService.name(assistant),
                language,
                assistantSettingService.voice(assistant, language),
                assistantSettingService.example(assistant, language)
        );
    }

    private List<Map<String, Object>> sanitizeMessages(Object raw) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (!(raw instanceof List)) {
            return result;
        }
        List<?> list = (List<?>) raw;
        int start = Math.max(0, list.size() - 10);
        for (int i = start; i < list.size(); i++) {
            Object item = list.get(i);
            if (!(item instanceof Map)) {
                continue;
            }
            Map<?, ?> rawMessage = (Map<?, ?>) item;
            String role = String.valueOf(rawMessage.get("role"));
            if (!"user".equals(role) && !"assistant".equals(role)) {
                continue;
            }
            String content = rawMessage.get("content") == null ? "" : String.valueOf(rawMessage.get("content")).trim();
            if (content.isEmpty()) {
                continue;
            }
            if (content.length() > 1500) {
                content = content.substring(0, 1500);
            }
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("role", role);
            message.put("content", content);
            result.add(message);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private HttpMessageConverter<Object> jsonWriter() {
        return (HttpMessageConverter<Object>) restTemplate.getMessageConverters().stream()
                .filter(converter -> converter.canWrite(Map.class, MediaType.APPLICATION_JSON))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("没有可用的 JSON 转换器"));
    }

    private void writeJsonError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String body = "{\"code\":\"" + status.value() + "\",\"msg\":\""
                + escapeJson(message) + "\",\"data\":null}";
        response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
        response.getOutputStream().flush();
    }

    private void writeSseError(OutputStream output, String message) {
        try {
            String data = "{\"msg\":\"" + escapeJson(message) + "\"}";
            output.write(("event: error\ndata: " + data + "\n\n").getBytes(StandardCharsets.UTF_8));
            output.flush();
        } catch (IOException ignored) {
            // 客户端已断开
        }
    }

    private String escapeJson(String value) {
        StringBuilder builder = new StringBuilder(value.length() + 16);
        for (char c : value.toCharArray()) {
            switch (c) {
                case '\\' -> builder.append("\\\\");
                case '"' -> builder.append("\\\"");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (c < 0x20) {
                        builder.append(String.format("\\u%04x", (int) c));
                    } else {
                        builder.append(c);
                    }
                }
            }
        }
        return builder.toString();
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", String.valueOf(status.value()));
        body.put("msg", message);
        body.put("data", null);
        return ResponseEntity.status(status).body(body);
    }

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(120000);
        return new RestTemplate(factory);
    }
}
