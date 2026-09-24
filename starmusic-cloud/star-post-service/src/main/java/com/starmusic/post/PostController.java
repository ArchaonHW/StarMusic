package com.starmusic.post;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    public record PostDto(long id, String type, String title, String category, String body,
                          String mediaUrl, String originalFilename, String author,
                          String status, String reviewNote, String createdAt, String reviewedAt) {
        static PostDto of(PostEntity e) {
            return new PostDto(e.getId(), e.getType(), e.getTitle(), e.getCategory(), e.getBody(),
                    e.getFilename() == null ? null : "/api/posts/files/" + e.getFilename(),
                    e.getOriginalFilename(), e.getAuthor(), e.getStatus(), e.getReviewNote(),
                    e.getCreatedAt().toString(),
                    e.getReviewedAt() == null ? null : e.getReviewedAt().toString());
        }
    }

    public record ReviewRequest(String note) {
    }

    private static final Set<String> VIDEO_EXT = Set.of(".mp4", ".m4v", ".mov", ".webm", ".mkv");
    private static final Set<String> AUDIO_EXT = Set.of(".mp3", ".wav", ".ogg", ".m4a", ".flac");
    private static final Set<String> IMAGE_EXT = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp", ".avif");
    private static final Map<String, Set<String>> ALLOWED_EXT = Map.of(
            "VIDEO", VIDEO_EXT,
            "AUDIO", AUDIO_EXT,
            "IMAGE", IMAGE_EXT,
            "ARTICLE", IMAGE_EXT);
    private static final Map<String, String> CONTENT_TYPE_PREFIX = Map.of(
            "VIDEO", "video/",
            "AUDIO", "audio/",
            "IMAGE", "image/",
            "ARTICLE", "image/");
    private static final Map<String, MediaType> FILE_MEDIA_TYPE = Map.ofEntries(
            Map.entry(".mp4", MediaType.parseMediaType("video/mp4")),
            Map.entry(".m4v", MediaType.parseMediaType("video/mp4")),
            Map.entry(".mov", MediaType.parseMediaType("video/quicktime")),
            Map.entry(".webm", MediaType.parseMediaType("video/webm")),
            Map.entry(".mkv", MediaType.parseMediaType("video/x-matroska")),
            Map.entry(".mp3", MediaType.parseMediaType("audio/mpeg")),
            Map.entry(".wav", MediaType.parseMediaType("audio/wav")),
            Map.entry(".ogg", MediaType.parseMediaType("audio/ogg")),
            Map.entry(".m4a", MediaType.parseMediaType("audio/mp4")),
            Map.entry(".flac", MediaType.parseMediaType("audio/flac")),
            Map.entry(".jpg", MediaType.IMAGE_JPEG),
            Map.entry(".jpeg", MediaType.IMAGE_JPEG),
            Map.entry(".png", MediaType.IMAGE_PNG),
            Map.entry(".gif", MediaType.IMAGE_GIF),
            Map.entry(".webp", MediaType.parseMediaType("image/webp")),
            Map.entry(".avif", MediaType.parseMediaType("image/avif")));
    private static final int MAX_TITLE = 200;
    private static final int MAX_CATEGORY = 50;
    private static final int MAX_BODY = 10000;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_POSTS_PER_DAY = 10;

    private final PostRepository posts;
    private final Path uploadDir;

    public PostController(PostRepository posts,
                          @Value("${starmusic.upload-dir:uploads}") String uploadDir) {
        this.posts = posts;
        this.uploadDir = Path.of(uploadDir);
    }

    @PostMapping
    public ResponseEntity<PostDto> create(
            @RequestParam("type") String type,
            @RequestParam("title") String title,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "body", required = false) String body,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Name", required = false) String author) {
        if (userId == null || author == null || author.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "請先登入會員");
        }
        long recent = posts.countByMemberIdAndCreatedAtAfter(
                userId, Instant.now().minus(1, ChronoUnit.DAYS));
        if (recent >= MAX_POSTS_PER_DAY) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "今日投稿數已達上限");
        }
        String t = type == null ? "" : type.toUpperCase(Locale.ROOT);
        if (!ALLOWED_EXT.containsKey(t)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "無效的投稿類型");
        }
        if (title == null || title.isBlank() || title.length() > MAX_TITLE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "標題必填且不超過 200 字");
        }
        if (category != null && category.length() > MAX_CATEGORY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "分類過長");
        }
        boolean isArticle = "ARTICLE".equals(t);
        if (isArticle && (body == null || body.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文章內文為必填");
        }
        if (body != null && body.length() > MAX_BODY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "內文過長");
        }
        boolean hasFile = file != null && !file.isEmpty();
        if (!isArticle && !hasFile) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "請選擇要上傳的檔案");
        }

        PostEntity p = new PostEntity();
        p.setMemberId(userId);
        p.setAuthor(author);
        p.setType(t);
        p.setTitle(title.trim());
        p.setCategory(category);
        p.setBody(body);

        if (hasFile) {
            String ext = ext(file.getOriginalFilename());
            if (!ALLOWED_EXT.get(t).contains(ext)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "此類型僅支援：" + String.join(" ", ALLOWED_EXT.get(t)));
            }
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith(CONTENT_TYPE_PREFIX.get(t))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "檔案類型與投稿類型不符");
            }
            if (!magicMatches(file, ext)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "檔案內容與副檔名不符");
            }
            try {
                Files.createDirectories(uploadDir);
                String stored = UUID.randomUUID() + ext;
                file.transferTo(uploadDir.resolve(stored));
                p.setFilename(stored);
                p.setOriginalFilename(file.getOriginalFilename());
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "檔案儲存失敗");
            }
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(PostDto.of(posts.save(p)));
    }

    @GetMapping
    public List<PostDto> list(@RequestParam(value = "type", required = false) String type,
                              @RequestParam(value = "page", defaultValue = "0") int page,
                              @RequestParam(value = "size", defaultValue = "12") int size) {
        Pageable pageable = pageable(page, size);
        Page<PostEntity> result = type == null || type.isBlank()
                ? posts.findByStatus(PostEntity.APPROVED, pageable)
                : posts.findByStatusAndType(
                        PostEntity.APPROVED, type.toUpperCase(Locale.ROOT), pageable);
        return result.map(PostDto::of).getContent();
    }

    @GetMapping("/mine")
    public List<PostDto> mine(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                              @RequestParam(value = "page", defaultValue = "0") int page,
                              @RequestParam(value = "size", defaultValue = "20") int size) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "請先登入會員");
        }
        return posts.findByMemberId(userId, pageable(page, size))
                .map(PostDto::of).getContent();
    }

    @GetMapping("/pending")
    public List<PostDto> pending(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        requireAdmin(role);
        return posts.findByStatus(PostEntity.PENDING, pageable(page, size))
                .map(PostDto::of).getContent();
    }

    @GetMapping("/files/{filename}")
    public ResponseEntity<Resource> file(
            @PathVariable String filename,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!filename.matches("[0-9a-f-]{36}\\.[a-z0-9]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "無效的檔名");
        }
        PostEntity p = posts.findByFilename(filename).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "檔案不存在"));
        boolean allowed = PostEntity.APPROVED.equals(p.getStatus())
                || (userId != null && userId.equals(p.getMemberId()))
                || "ADMIN".equals(role);
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "此檔案尚未公開");
        }
        Path path = uploadDir.resolve(filename).normalize();
        if (!path.startsWith(uploadDir) || !Files.isRegularFile(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "檔案不存在");
        }
        MediaType mediaType = FILE_MEDIA_TYPE.getOrDefault(
                ext(filename), MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(new FileSystemResource(path));
    }

    @PostMapping("/{id}/approve")
    public PostDto approve(@PathVariable long id,
                           @RequestBody(required = false) ReviewRequest req,
                           @RequestHeader(value = "X-User-Role", required = false) String role) {
        return review(id, PostEntity.APPROVED, req, role);
    }

    @PostMapping("/{id}/reject")
    public PostDto reject(@PathVariable long id,
                          @RequestBody(required = false) ReviewRequest req,
                          @RequestHeader(value = "X-User-Role", required = false) String role) {
        return review(id, PostEntity.REJECTED, req, role);
    }

    private PostDto review(long id, String status, ReviewRequest req, String role) {
        requireAdmin(role);
        PostEntity p = posts.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "投稿不存在"));
        if (!PostEntity.PENDING.equals(p.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "此投稿已完成審核");
        }
        p.setStatus(status);
        p.setReviewNote(req == null ? null : req.note());
        p.setReviewedAt(Instant.now());
        return PostDto.of(posts.save(p));
    }

    private void requireAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "需要管理員權限");
        }
    }

    private Pageable pageable(int page, int size) {
        int p = Math.max(0, page);
        int s = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        return PageRequest.of(p, s, Sort.by("createdAt").descending());
    }

    private String ext(String name) {
        if (name == null) {
            return "";
        }
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot).toLowerCase(Locale.ROOT) : "";
    }

    private boolean magicMatches(MultipartFile file, String ext) {
        byte[] head = new byte[12];
        try (InputStream in = file.getInputStream()) {
            int n = in.readNBytes(head, 0, head.length);
            if (n < 4) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
        return switch (ext) {
            case ".png" -> at(head, 0, 0x89, 0x50, 0x4E, 0x47);
            case ".jpg", ".jpeg" -> at(head, 0, 0xFF, 0xD8, 0xFF);
            case ".gif" -> at(head, 0, 'G', 'I', 'F', '8');
            case ".webp" -> at(head, 0, 'R', 'I', 'F', 'F') && at(head, 8, 'W', 'E', 'B', 'P');
            case ".avif" -> boxIs(head, "avif") || boxIs(head, "avis");
            case ".mp3" -> at(head, 0, 'I', 'D', '3')
                    || (head[0] == (byte) 0xFF && (head[1] & 0xE0) == 0xE0);
            case ".wav" -> at(head, 0, 'R', 'I', 'F', 'F') && at(head, 8, 'W', 'A', 'V', 'E');
            case ".ogg" -> at(head, 0, 'O', 'g', 'g', 'S');
            case ".m4a", ".mp4", ".m4v", ".mov" -> at(head, 4, 'f', 't', 'y', 'p');
            case ".flac" -> at(head, 0, 'f', 'L', 'a', 'C');
            case ".webm", ".mkv" -> at(head, 0, 0x1A, 0x45, 0xDF, 0xA3);
            default -> false;
        };
    }

    private boolean boxIs(byte[] head, String brand) {
        if (!at(head, 4, 'f', 't', 'y', 'p')) {
            return false;
        }
        String b = new String(head, 8, 4, java.nio.charset.StandardCharsets.US_ASCII);
        return b.equals(brand);
    }

    private boolean at(byte[] head, int off, int... bytes) {
        if (head.length < off + bytes.length) {
            return false;
        }
        for (int i = 0; i < bytes.length; i++) {
            if ((head[off + i] & 0xFF) != (bytes[i] & 0xFF)) {
                return false;
            }
        }
        return true;
    }
}
