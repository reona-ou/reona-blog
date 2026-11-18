package com.reona.blog.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.reona.blog.commcn.Constants;
import com.reona.blog.commcn.Result;
import com.reona.blog.commcn.RoleEnum;
import com.reona.blog.config.AuthAccess;
import com.reona.blog.entity.CalendarSetting;
import com.reona.blog.entity.User;
import com.reona.blog.exception.ServiceException;
import com.reona.blog.mapper.CalendarSettingMapper;
import com.reona.blog.utils.TokenUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequestMapping("/calendar")
public class CalendarController {

    private static final int MAX_ICS_BYTES = 8 * 1024 * 1024;
    private static final int MAX_CALENDARS = 20;
    private static final int MAX_NAME_LENGTH = 48;
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private static final Pattern COLOR_PATTERN = Pattern.compile("^#[0-9a-fA-F]{6}$");
    private static final Set<String> PUBLIC_EVENT_PROPERTIES = Set.of(
            "UID", "SUMMARY", "DTSTART", "DTEND", "DURATION", "RRULE", "RDATE", "EXDATE",
            "RECURRENCE-ID", "SEQUENCE", "STATUS", "TRANSP", "DTSTAMP", "CREATED", "LAST-MODIFIED"
    );
    private static final Set<String> PRIVATE_CALENDAR_PROPERTIES = Set.of(
            "NAME", "DESCRIPTION", "SOURCE", "URL", "ORGANIZER", "ATTENDEE",
            "X-WR-CALNAME", "X-WR-CALDESC"
    );
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @Resource
    private CalendarSettingMapper calendarSettingMapper;

    private final Object fetchLock = new Object();
    private final Map<Integer, CachedFeed> feedCache = new ConcurrentHashMap<>();

    @GetMapping("/sources")
    public Result sources() {
        requireSuperAdmin();
        List<CalendarSourceView> views = calendarSettingMapper.selectList(
                        Wrappers.<CalendarSetting>lambdaQuery().orderByAsc(CalendarSetting::getId))
                .stream()
                .map(this::toView)
                .toList();
        return Result.success(views);
    }

    @PostMapping("/sources")
    public Result createSource(@RequestBody CalendarConfigRequest request) {
        requireSuperAdmin();
        if (request == null) {
            throw new ServiceException(Constants.CODE_400, "参数错误");
        }
        long count = calendarSettingMapper.selectCount(null);
        if (count >= MAX_CALENDARS) {
            throw new ServiceException(Constants.CODE_400, "最多只能添加 " + MAX_CALENDARS + " 个日历");
        }

        String name = normalizeName(request.name());
        String color = normalizeColor(request.color());
        String url = validateIcsUrl(request.icsUrl());
        String ics = fetchCalendar(url);

        CalendarSetting setting = new CalendarSetting();
        setting.setName(name);
        setting.setColor(color);
        setting.setIcsUrl(url);
        setting.setVisible(request.visible() == null || request.visible());
        calendarSettingMapper.insert(setting);

        CalendarSetting saved = calendarSettingMapper.selectById(setting.getId());
        feedCache.put(saved.getId(), new CachedFeed(url, ics, Instant.now()));
        return Result.success(toView(saved));
    }

    @PutMapping("/sources/{id}")
    public Result updateSource(@PathVariable Integer id, @RequestBody CalendarConfigRequest request) {
        requireSuperAdmin();
        if (request == null) {
            throw new ServiceException(Constants.CODE_400, "参数错误");
        }
        CalendarSetting setting = calendarSettingMapper.selectById(id);
        if (setting == null) {
            throw new ServiceException(Constants.CODE_404, "日历不存在");
        }

        String name = StrUtil.isBlank(request.name()) ? setting.getName() : normalizeName(request.name());
        String color = StrUtil.isBlank(request.color()) ? setting.getColor() : normalizeColor(request.color());
        setting.setName(name);
        setting.setColor(color);
        if (request.visible() != null) {
            setting.setVisible(request.visible());
        }

        String newUrl = StrUtil.isBlank(request.icsUrl()) ? null : validateIcsUrl(request.icsUrl());
        if (newUrl != null) {
            String ics = fetchCalendar(newUrl);
            setting.setIcsUrl(newUrl);
            feedCache.put(setting.getId(), new CachedFeed(newUrl, ics, Instant.now()));
        }
        calendarSettingMapper.updateById(setting);

        CalendarSetting saved = calendarSettingMapper.selectById(id);
        return Result.success(toView(saved));
    }

    @DeleteMapping("/sources/{id}")
    public Result deleteSource(@PathVariable Integer id) {
        requireSuperAdmin();
        CalendarSetting setting = calendarSettingMapper.selectById(id);
        if (setting == null) {
            throw new ServiceException(Constants.CODE_404, "日历不存在");
        }
        calendarSettingMapper.deleteById(id);
        feedCache.remove(id);
        return Result.success();
    }

    @GetMapping("/feed")
    public ResponseEntity<Result> feed(@RequestParam(defaultValue = "false") boolean refresh) {
        requireSuperAdmin();
        List<CalendarSetting> settings = calendarSettingMapper.selectList(
                Wrappers.<CalendarSetting>lambdaQuery().orderByAsc(CalendarSetting::getId));
        return calendarFeed(settings, refresh, false);
    }

    @AuthAccess
    @GetMapping("/public/feed")
    public ResponseEntity<Result> publicFeed(@RequestParam(defaultValue = "false") boolean refresh) {
        List<CalendarSetting> settings = calendarSettingMapper.selectList(
                Wrappers.<CalendarSetting>lambdaQuery()
                        .eq(CalendarSetting::getVisible, true)
                        .orderByAsc(CalendarSetting::getId));
        return calendarFeed(settings, refresh, true);
    }

    private ResponseEntity<Result> calendarFeed(List<CalendarSetting> settings, boolean refresh,
                                                boolean publicView) {
        List<Map<String, Object>> calendars = new ArrayList<>();
        List<Map<String, Object>> failed = new ArrayList<>();
        for (CalendarSetting setting : settings) {
            try {
                CachedFeed feed = loadFeed(setting, refresh);
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", setting.getId());
                item.put("name", setting.getName());
                item.put("color", setting.getColor());
                item.put("visible", Boolean.TRUE.equals(setting.getVisible()));
                item.put("ics", publicView ? sanitizeForPublic(feed.content()) : feed.content());
                item.put("fetchedAt", feed.fetchedAt().toString());
                calendars.add(item);
            } catch (ServiceException e) {
                log.warn("读取日历 {} 失败: {}", setting.getName(), e.getMessage());
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", setting.getId());
                item.put("name", setting.getName());
                failed.add(item);
            }
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("calendars", calendars);
        body.put("failed", failed);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Result.success(body));
    }

    private String sanitizeForPublic(String ics) {
        StringBuilder out = new StringBuilder(ics.length());
        boolean inEvent = false;
        boolean skipContinuation = false;
        String skipComponent = null;

        for (String line : ics.split("\r\n|\n|\r")) {
            if (skipComponent != null) {
                if (line.equalsIgnoreCase("END:" + skipComponent)) {
                    skipComponent = null;
                }
                continue;
            }
            if (line.regionMatches(true, 0, "BEGIN:VEVENT", 0, 12)) {
                inEvent = true;
                skipContinuation = false;
                out.append(line).append("\r\n");
                continue;
            }
            if (line.regionMatches(true, 0, "END:VEVENT", 0, 10)) {
                inEvent = false;
                skipContinuation = false;
                out.append(line).append("\r\n");
                continue;
            }
            if (inEvent && line.regionMatches(true, 0, "BEGIN:VALARM", 0, 12)) {
                skipComponent = "VALARM";
                continue;
            }
            if (line.startsWith(" ") || line.startsWith("\t")) {
                if (!skipContinuation) {
                    out.append(line).append("\r\n");
                }
                continue;
            }

            String property = propertyName(line);
            if (inEvent && !PUBLIC_EVENT_PROPERTIES.contains(property)) {
                skipContinuation = true;
                continue;
            }
            if (!inEvent && PRIVATE_CALENDAR_PROPERTIES.contains(property)) {
                skipContinuation = true;
                continue;
            }
            skipContinuation = false;
            out.append(line).append("\r\n");
        }
        return out.toString();
    }

    private String propertyName(String line) {
        int end = line.length();
        int colon = line.indexOf(':');
        int semicolon = line.indexOf(';');
        if (colon >= 0) {
            end = Math.min(end, colon);
        }
        if (semicolon >= 0) {
            end = Math.min(end, semicolon);
        }
        return line.substring(0, end).trim().toUpperCase(Locale.ROOT);
    }

    private CalendarSourceView toView(CalendarSetting setting) {
        return new CalendarSourceView(
                setting.getId(),
                setting.getName(),
                setting.getColor(),
                Boolean.TRUE.equals(setting.getVisible()),
                setting.getUpdateTime()
        );
    }

    private String normalizeName(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty() || value.length() > MAX_NAME_LENGTH) {
            throw new ServiceException(Constants.CODE_400,
                    "请填写日历名称（1–" + MAX_NAME_LENGTH + " 个字符）");
        }
        return value;
    }

    private String normalizeColor(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            return "#2f4a3a";
        }
        if (!COLOR_PATTERN.matcher(value).matches()) {
            throw new ServiceException(Constants.CODE_400, "请选择有效的日历颜色");
        }
        return value.toLowerCase();
    }

    private void requireSuperAdmin() {
        User user = TokenUtils.getVerifiedCurrentUser();
        if (user == null || !RoleEnum.isSuperAdmin(user.getRole())) {
            throw new ServiceException(Constants.CODE_403, "只有超级管理员可以管理日历");
        }
    }

    private String validateIcsUrl(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty() || value.length() > 1024) {
            throw new ServiceException(Constants.CODE_400, "请填写 Google 日历的私密 iCal 地址");
        }
        URI uri;
        try {
            uri = new URI(value);
        } catch (URISyntaxException e) {
            throw new ServiceException(Constants.CODE_400, "日历地址格式不正确");
        }
        boolean valid = "https".equalsIgnoreCase(uri.getScheme())
                && "calendar.google.com".equalsIgnoreCase(uri.getHost())
                && (uri.getPort() == -1 || uri.getPort() == 443)
                && uri.getUserInfo() == null
                && uri.getPath() != null
                && uri.getPath().startsWith("/calendar/ical/");
        if (!valid) {
            throw new ServiceException(Constants.CODE_400, "只支持 calendar.google.com 的 https 私密 iCal 地址");
        }
        return uri.toString();
    }

    private CachedFeed loadFeed(CalendarSetting setting, boolean refresh) {
        String url = setting.getIcsUrl();
        if (StrUtil.isBlank(url)) {
            throw new ServiceException(Constants.CODE_400, "日历地址为空");
        }
        CachedFeed current = feedCache.get(setting.getId());
        if (!refresh && isFresh(current, url)) {
            return current;
        }
        synchronized (fetchLock) {
            current = feedCache.get(setting.getId());
            if (!refresh && isFresh(current, url)) {
                return current;
            }
            try {
                CachedFeed fresh = new CachedFeed(url, fetchCalendar(url), Instant.now());
                feedCache.put(setting.getId(), fresh);
                return fresh;
            } catch (ServiceException e) {
                if (current != null && current.url().equals(url)) {
                    log.warn("刷新日历 {} 失败，继续使用缓存: {}", setting.getName(), e.getMessage());
                    return current;
                }
                throw e;
            }
        }
    }

    private boolean isFresh(CachedFeed feed, String url) {
        return feed != null
                && feed.url().equals(url)
                && Duration.between(feed.fetchedAt(), Instant.now()).compareTo(CACHE_TTL) < 0;
    }

    private String fetchCalendar(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "text/calendar")
                    .header("User-Agent", "ReonaBlogCalendar/1.0")
                    .GET()
                    .build();
            HttpResponse<InputStream> response = HTTP_CLIENT.send(
                    request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                response.body().close();
                log.warn("Google 日历返回 HTTP {}", response.statusCode());
                throw new ServiceException(Constants.CODE_500,
                        "无法读取 Google 日历（HTTP " + response.statusCode() + "）");
            }
            String content = readLimited(response.body());
            String normalized = content.startsWith("\uFEFF") ? content.substring(1) : content;
            if (!normalized.stripLeading().startsWith("BEGIN:VCALENDAR")) {
                throw new ServiceException(Constants.CODE_400, "该地址不是有效的 iCal 日历");
            }
            return content;
        } catch (ServiceException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException(Constants.CODE_500, "读取 Google 日历被中断");
        } catch (Exception e) {
            log.warn("读取 Google 日历失败: {}", e.getClass().getSimpleName());
            throw new ServiceException(Constants.CODE_500, "暂时无法访问 Google 日历，请稍后再试");
        }
    }

    private String readLimited(InputStream inputStream) throws IOException {
        try (InputStream in = inputStream) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > MAX_ICS_BYTES) {
                    throw new IOException("iCal 文件超过大小限制");
                }
                out.write(buffer, 0, read);
            }
            return out.toString(StandardCharsets.UTF_8);
        }
    }

    private record CalendarConfigRequest(String name, String color, String icsUrl, Boolean visible) {
    }

    private record CalendarSourceView(Integer id, String name, String color, boolean visible,
                                      LocalDateTime updateTime) {
    }

    private record CachedFeed(String url, String content, Instant fetchedAt) {
    }
}
