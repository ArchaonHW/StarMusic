package com.starmusic.video;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/videos/uploads")
public class UploadController {

    public record UploadDto(long id, String title, String category, String description,
                            String originalFilename, String uploader, String status,
                            String reviewNote, String createdAt, String reviewedAt) {
        static UploadDto of(VideoUpload e) {
            return new UploadDto(e.getId(), e.getTitle(), e.getCategory(), e.getDescription(),
                    e.getOriginalFilename(), e.getUploader(), e.getStatus(), e.getReviewNote(),
                    e.getCreatedAt().toString(),
                    e.getReviewedAt() == null ? null : e.getReviewedAt().toString());
        }
    }

    public record ReviewRequest(String note) {
    }

    private static final Set<String> ALLOWED_EXT =
            Set.of(".mp4", ".m4v", ".mov", ".webm", ".mkv");
    private static final int MAX_TITLE = 200;
    private static final int MAX_DESCRIPTION = 2000;

    private final VideoUploadRepository uploads;
    private final Path uploadDir;

    public UploadController(VideoUploadRepository uploads,
                            @org.springframework.beans.factory.annotation.Value("${starmusic.upload-dir:uploads}") String uploadDir) {
        this.uploads = uploads;
        this.uploadDir = Path.of(uploadDir);
    }

    @PostMapping
    public ResponseEntity<UploadDto> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "description", required = false) String description,
            @RequestHeader(value = "X-User-Name", required = false) String uploaderName) {
        String uploader = dec(uploaderName);
        if (uploader == null || uploader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "請先登入會員");
        }
        if (file.isEmpty() || title == null || title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "標題與影片檔案為必填");
        }
        if (title.length() > MAX_TITLE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "標題過長");
        }
        if (description != null && description.length() > MAX_DESCRIPTION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "描述過長");
        }
        String ext = ext(file.getOriginalFilename());
        if (!ALLOWED_EXT.contains(ext)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "僅支援影片格式：" + String.join(" ", ALLOWED_EXT));
        }
        String contentType = file.getContentType();
        if (contentType != null && !contentType.startsWith("video/")
                && !contentType.equals("application/octet-stream")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "檔案類型必須為影片");
        }
        try {
            Files.createDirectories(uploadDir);
            String stored = UUID.randomUUID() + ext;
            file.transferTo(uploadDir.resolve(stored));

            VideoUpload u = new VideoUpload();
            u.setTitle(title);
            u.setCategory(category);
            u.setDescription(description);
            u.setFilename(stored);
            u.setOriginalFilename(file.getOriginalFilename());
            u.setUploader(uploader);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(UploadDto.of(uploads.save(u)));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "檔案儲存失敗");
        }
    }

    @GetMapping("/mine")
    public List<UploadDto> mine(@RequestHeader(value = "X-User-Name", required = false) String uploaderName) {
        String uploader = dec(uploaderName);
        if (uploader == null || uploader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "請先登入會員");
        }
        return uploads.findByUploaderOrderByCreatedAtDesc(uploader).stream()
                .map(UploadDto::of).toList();
    }

    @GetMapping
    public List<UploadDto> list(
            @RequestParam(value = "status", required = false) String status,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        List<VideoUpload> list = status == null || status.isBlank()
                ? uploads.findAll()
                : uploads.findByStatusOrderByCreatedAtDesc(status);
        return list.stream().map(UploadDto::of).toList();
    }

    @PostMapping("/{id}/approve")
    public UploadDto approve(@PathVariable long id,
                             @RequestBody(required = false) ReviewRequest req,
                             @RequestHeader(value = "X-User-Role", required = false) String role) {
        return review(id, VideoUpload.APPROVED, req, role);
    }

    @PostMapping("/{id}/reject")
    public UploadDto reject(@PathVariable long id,
                            @RequestBody(required = false) ReviewRequest req,
                            @RequestHeader(value = "X-User-Role", required = false) String role) {
        return review(id, VideoUpload.REJECTED, req, role);
    }

    @PostMapping("/{id}/takedown")
    public UploadDto takedown(@PathVariable long id,
                              @RequestHeader(value = "X-User-Name", required = false) String userName,
                              @RequestHeader(value = "X-User-Role", required = false) String role) {
        VideoUpload u = uploads.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "上傳紀錄不存在"));
        requireOwnerOrAdmin(u, userName, role);
        if (!VideoUpload.APPROVED.equals(u.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "僅已上架的影片可下架");
        }
        u.setStatus(VideoUpload.TAKEN_DOWN);
        u.setReviewedAt(Instant.now());
        return UploadDto.of(uploads.save(u));
    }

    @PostMapping("/{id}/resubmit")
    public UploadDto resubmit(@PathVariable long id,
                              @RequestHeader(value = "X-User-Name", required = false) String userName) {
        if (userName == null || userName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "請先登入會員");
        }
        VideoUpload u = uploads.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "上傳紀錄不存在"));
        if (!userName.equals(u.getUploader())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "僅上傳本人可重新送審");
        }
        if (!VideoUpload.TAKEN_DOWN.equals(u.getStatus())
                && !VideoUpload.REJECTED.equals(u.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "此影片目前無法重新送審");
        }
        u.setStatus(VideoUpload.PENDING);
        u.setReviewNote(null);
        u.setReviewedAt(null);
        return UploadDto.of(uploads.save(u));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable long id,
            @RequestHeader(value = "X-User-Name", required = false) String userName,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        VideoUpload u = uploads.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "上傳紀錄不存在"));
        requireOwnerOrAdmin(u, userName, role);
        uploads.delete(u);
        try {
            Files.deleteIfExists(uploadDir.resolve(u.getFilename()).normalize());
        } catch (IOException ignored) {
        }
        return ResponseEntity.noContent().build();
    }

    private void requireOwnerOrAdmin(VideoUpload u, String userName, String role) {
        boolean owner = userName != null && userName.equals(u.getUploader());
        if (!owner && !"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "沒有權限操作此影片");
        }
    }

    private UploadDto review(long id, String status, ReviewRequest req, String role) {
        requireAdmin(role);
        VideoUpload u = uploads.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "上傳紀錄不存在"));
        u.setStatus(status);
        u.setReviewNote(req == null ? null : req.note());
        u.setReviewedAt(Instant.now());
        return UploadDto.of(uploads.save(u));
    }

    private void requireAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "需要管理員權限");
        }
    }

    private static String dec(String v) {
        return v == null ? null : URLDecoder.decode(v, StandardCharsets.UTF_8);
    }

    private String ext(String name) {
        if (name == null) {
            return "";
        }
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot).toLowerCase(Locale.ROOT) : "";
    }
}
