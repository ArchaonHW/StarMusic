package com.starmusic.post;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
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
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    public record PostDto(long id, String type, String title, String category, String body,
                          String mediaUrl, String originalFilename, String author,
                          String status, String reviewNote, long likeCount,
                          String createdAt, String reviewedAt) {
        static PostDto of(PostEntity e) {
            return of(e, 0);
        }

        static PostDto of(PostEntity e, long likeCount) {
            return new PostDto(e.getId(), e.getType(), e.getTitle(), e.getCategory(), e.getBody(),
                    e.getFilename() == null ? null : "/api/posts/files/" + e.getFilename(),
                    e.getOriginalFilename(), e.getAuthor(), e.getStatus(), e.getReviewNote(),
                    likeCount, e.getCreatedAt().toString(),
                    e.getReviewedAt() == null ? null : e.getReviewedAt().toString());
        }
    }

    public record CommentDto(long id, long postId, String author, String body,
                             long memberId, String createdAt) {
        static CommentDto of(PostComment e) {
            return new CommentDto(e.getId(), e.getPostId(), e.getAuthor(), e.getBody(),
                    e.getMemberId(), e.getCreatedAt().toString());
        }
    }

    public record CommentRequest(String body) {
    }

    public record ReviewRequest(String note) {
    }

    public record LikeState(long likes, boolean liked) {
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
    private static final int MAX_TITLE = 200;
    private static final int MAX_CATEGORY = 50;
    private static final int MAX_BODY = 10000;
    private static final int MAX_COMMENT = 500;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_POSTS_PER_DAY = 10;

    private final PostRepository posts;
    private final PostLikeRepository likes;
    private final PostCommentRepository comments;
    private final Path uploadDir;

    public PostController(PostRepository posts, PostLikeRepository likes,
                          PostCommentRepository comments,
                          @Value("${starmusic.upload-dir:uploads}") String uploadDir) {
        this.posts = posts;
        this.likes = likes;
        this.comments = comments;
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
        String authorName = dec(author);
        if (userId == null || authorName == null || authorName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "請先登入會員");
        }
        long today = posts.countByMemberIdAndCreatedAtAfter(
                userId, Instant.now().minus(Duration.ofDays(1)));
        if (today >= MAX_POSTS_PER_DAY) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "已達每日投稿上限（" + MAX_POSTS_PER_DAY + " 篇）");
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
        p.setAuthor(authorName);
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
            String prefix = CONTENT_TYPE_PREFIX.get(t);
            if (contentType == null || !contentType.startsWith(prefix)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "檔案類型與投稿類型不符");
            }
            byte[] head = head(file);
            if (!magicOk(ext, head)) {
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
        return ResponseEntity.status(HttpStatus.CREATED).body(PostDto.of(posts.save(p), 0));
    }

    @GetMapping
    public Page<PostDto> list(@RequestParam(value = "type", required = false) String type,
                              @RequestParam(value = "page", defaultValue = "0") int page,
                              @RequestParam(value = "size", defaultValue = "12") int size) {
        Pageable pageable = pageable(page, size);
        Page<PostEntity> result = type == null || type.isBlank()
                ? posts.findByStatus(PostEntity.APPROVED, pageable)
                : posts.findByStatusAndType(
                        PostEntity.APPROVED, type.toUpperCase(Locale.ROOT), pageable);
        return withLikes(result);
    }

    @GetMapping("/mine")
    public Page<PostDto> mine(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                              @RequestParam(value = "page", defaultValue = "0") int page,
                              @RequestParam(value = "size", defaultValue = "12") int size) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "請先登入會員");
        }
        return withLikes(posts.findByMemberId(userId, pageable(page, size)));
    }

    @GetMapping("/pending")
    public Page<PostDto> pending(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "12") int size) {
        requireAdmin(role);
        return withLikes(posts.findByStatus(PostEntity.PENDING, pageable(page, size)));
    }

    @GetMapping("/manage")
    public List<PostDto> manage(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size) {
        requireAdmin(role);
        Pageable pageable = pageable(page, size);
        Page<PostEntity> result = status == null || status.isBlank()
                ? posts.findAll(pageable)
                : posts.findByStatus(status.toUpperCase(Locale.ROOT), pageable);
        return withLikes(result.getContent());
    }

    @GetMapping("/{id:\\d+}")
    public PostDto detail(@PathVariable long id,
                          @RequestHeader(value = "X-User-Id", required = false) Long userId,
                          @RequestHeader(value = "X-User-Role", required = false) String role) {
        PostEntity p = findPost(id);
        requireVisible(p, userId, role);
        return PostDto.of(p, likes.countByPostId(id));
    }

    @GetMapping("/files/{filename:.+}")
    public ResponseEntity<Resource> file(
            @PathVariable String filename,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        PostEntity p = posts.findByFilename(filename).orElse(null);
        if (p == null || p.getFilename() == null) {
            return ResponseEntity.notFound().build();
        }
        boolean owner = userId != null && userId.equals(p.getMemberId());
        boolean admin = "ADMIN".equals(role);
        if (!PostEntity.APPROVED.equals(p.getStatus()) && !owner && !admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "此投稿尚未上架");
        }
        Path path = uploadDir.resolve(filename).normalize();
        if (!path.startsWith(uploadDir) || !Files.isRegularFile(path)) {
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType = mediaTypeFor(ext(filename));
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

    @PostMapping("/{id}/takedown")
    public PostDto takedown(@PathVariable long id,
                            @RequestHeader(value = "X-User-Id", required = false) Long userId,
                            @RequestHeader(value = "X-User-Role", required = false) String role) {
        PostEntity p = findPost(id);
        requireOwnerOrAdmin(p, userId, role);
        if (!PostEntity.APPROVED.equals(p.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "僅已上架的投稿可下架");
        }
        p.setStatus(PostEntity.TAKEN_DOWN);
        p.setReviewedAt(Instant.now());
        return PostDto.of(posts.save(p), likes.countByPostId(id));
    }

    @PostMapping("/{id}/resubmit")
    public PostDto resubmit(@PathVariable long id,
                            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        requireLogin(userId);
        PostEntity p = findPost(id);
        if (!userId.equals(p.getMemberId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "僅投稿本人可重新送審");
        }
        if (!PostEntity.REJECTED.equals(p.getStatus())
                && !PostEntity.TAKEN_DOWN.equals(p.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "此投稿狀態無法重新送審");
        }
        p.setStatus(PostEntity.PENDING);
        p.setReviewNote(null);
        p.setReviewedAt(null);
        return PostDto.of(posts.save(p), likes.countByPostId(id));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public void delete(@PathVariable long id,
                       @RequestHeader(value = "X-User-Id", required = false) Long userId,
                       @RequestHeader(value = "X-User-Role", required = false) String role) {
        PostEntity p = findPost(id);
        requireOwnerOrAdmin(p, userId, role);
        comments.deleteByPostId(id);
        likes.deleteByPostId(id);
        posts.delete(p);
        if (p.getFilename() != null) {
            try {
                Files.deleteIfExists(uploadDir.resolve(p.getFilename()).normalize());
            } catch (IOException ignored) {
            }
        }
    }

    @PostMapping("/{id}/like")
    public LikeState like(@PathVariable long id,
                          @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        requireLogin(userId);
        findPost(id);
        Optional<PostLike> existing = likes.findByMemberIdAndPostId(userId, id);
        boolean liked;
        if (existing.isPresent()) {
            likes.delete(existing.get());
            liked = false;
        } else {
            PostLike l = new PostLike();
            l.setMemberId(userId);
            l.setPostId(id);
            likes.save(l);
            liked = true;
        }
        return new LikeState(likes.countByPostId(id), liked);
    }

    @GetMapping("/{id}/likes")
    public LikeState likes(@PathVariable long id,
                           @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        findPost(id);
        boolean liked = userId != null
                && likes.findByMemberIdAndPostId(userId, id).isPresent();
        return new LikeState(likes.countByPostId(id), liked);
    }

    @GetMapping("/{id}/comments")
    public List<CommentDto> comments(
            @PathVariable long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        PostEntity p = findPost(id);
        requireVisible(p, userId, role);
        return comments.findByPostIdOrderByCreatedAtDesc(id).stream()
                .map(CommentDto::of).toList();
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentDto> addComment(
            @PathVariable long id,
            @RequestBody CommentRequest req,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Name", required = false) String author) {
        String authorName = dec(author);
        requireLogin(userId);
        if (authorName == null || authorName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "請先登入會員");
        }
        String body = req == null ? null : req.body();
        if (body == null || body.isBlank() || body.length() > MAX_COMMENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "留言必填且不超過 500 字");
        }
        findPost(id);
        PostComment c = new PostComment();
        c.setPostId(id);
        c.setMemberId(userId);
        c.setAuthor(authorName);
        c.setBody(body.trim());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommentDto.of(comments.save(c)));
    }

    @DeleteMapping("/comments/{commentId}")
    public void deleteComment(
            @PathVariable long commentId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        PostComment c = comments.findById(commentId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "留言不存在"));
        boolean owner = userId != null && userId.equals(c.getMemberId());
        if (!owner && !"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "無權刪除此留言");
        }
        comments.delete(c);
    }

    private PostDto review(long id, String status, ReviewRequest req, String role) {
        requireAdmin(role);
        PostEntity p = posts.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "投稿不存在"));
        p.setStatus(status);
        p.setReviewNote(req == null ? null : req.note());
        p.setReviewedAt(Instant.now());
        return PostDto.of(posts.save(p), likes.countByPostId(id));
    }

    private PostEntity findPost(long id) {
        return posts.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "投稿不存在"));
    }

    private void requireLogin(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "請先登入會員");
        }
    }

    private void requireApproved(PostEntity p) {
        if (!PostEntity.APPROVED.equals(p.getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "投稿不存在或未上架");
        }
    }

    private void requireVisible(PostEntity p, Long userId, String role) {
        if (!PostEntity.APPROVED.equals(p.getStatus()) && !isOwnerOrAdmin(p, userId, role)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "投稿不存在或未上架");
        }
    }

    private boolean isOwnerOrAdmin(PostEntity p, Long userId, String role) {
        return "ADMIN".equals(role) || (userId != null && userId.equals(p.getMemberId()));
    }

    private void requireOwnerOrAdmin(PostEntity p, Long userId, String role) {
        if (!isOwnerOrAdmin(p, userId, role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "無權操作此投稿");
        }
    }

    private Page<PostDto> toDtoPage(Page<PostEntity> page) {
        return page.map(e -> PostDto.of(e, likes.countByPostId(e.getId())));
    }

    private Page<PostDto> withLikes(Page<PostEntity> page) {
        return new PageImpl<>(withLikes(page.getContent()),
                page.getPageable(), page.getTotalElements());
    }

    private List<PostDto> withLikes(List<PostEntity> entities) {
        List<Long> ids = entities.stream().map(PostEntity::getId).toList();
        Map<Long, Long> counts = ids.isEmpty()
                ? Map.of()
                : likes.countByPostIdIn(ids).stream()
                        .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
        return entities.stream()
                .map(e -> PostDto.of(e, counts.getOrDefault(e.getId(), 0L)))
                .toList();
    }

    private static String dec(String v) {
        if (v == null) {
            return null;
        }
        try {
            return URLDecoder.decode(v, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return v;
        }
    }

    private void requireAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "需要管理員權限");
        }
    }

    private Pageable pageable(int page, int size) {
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private String ext(String name) {
        if (name == null) {
            return "";
        }
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot).toLowerCase(Locale.ROOT) : "";
    }

    private byte[] head(MultipartFile file) {
        try {
            byte[] buf = new byte[16];
            int n = file.getInputStream().read(buf);
            if (n <= 0) {
                return new byte[0];
            }
            byte[] head = new byte[n];
            System.arraycopy(buf, 0, head, 0, n);
            return head;
        } catch (IOException e) {
            return new byte[0];
        }
    }

    private boolean magicOk(String ext, byte[] h) {
        return switch (ext) {
            case ".jpg", ".jpeg" -> h.length >= 3
                    && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF;
            case ".png" -> h.length >= 4 && (h[0] & 0xFF) == 0x89
                    && h[1] == 'P' && h[2] == 'N' && h[3] == 'G';
            case ".gif" -> h.length >= 4 && h[0] == 'G' && h[1] == 'I' && h[2] == 'F' && h[3] == '8';
            case ".webp" -> h.length >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                    && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P';
            case ".avif" -> hasFtyp(h);
            case ".mp3" -> h.length >= 2 && ((h[0] & 0xFF) == 0xFF && (h[1] & 0xE0) == 0xE0
                    || h.length >= 3 && h[0] == 'I' && h[1] == 'D' && h[2] == '3');
            case ".wav" -> h.length >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                    && h[8] == 'W' && h[9] == 'A' && h[10] == 'V' && h[11] == 'E';
            case ".ogg" -> h.length >= 4 && h[0] == 'O' && h[1] == 'g' && h[2] == 'g' && h[3] == 'S';
            case ".m4a", ".m4v", ".mp4", ".mov" -> hasFtyp(h);
            case ".flac" -> h.length >= 4 && h[0] == 'f' && h[1] == 'L' && h[2] == 'a' && h[3] == 'C';
            case ".webm", ".mkv" -> h.length >= 4 && (h[0] & 0xFF) == 0x1A
                    && (h[1] & 0xFF) == 0x45 && (h[2] & 0xFF) == 0xDF && (h[3] & 0xFF) == 0xA3;
            default -> false;
        };
    }

    private boolean hasFtyp(byte[] h) {
        return h.length >= 8 && h[4] == 'f' && h[5] == 't' && h[6] == 'y' && h[7] == 'p';
    }

    private MediaType mediaTypeFor(String ext) {
        return switch (ext) {
            case ".jpg", ".jpeg" -> MediaType.IMAGE_JPEG;
            case ".png" -> MediaType.IMAGE_PNG;
            case ".gif" -> MediaType.IMAGE_GIF;
            case ".webp" -> MediaType.parseMediaType("image/webp");
            case ".avif" -> MediaType.parseMediaType("image/avif");
            case ".mp3" -> MediaType.parseMediaType("audio/mpeg");
            case ".wav" -> MediaType.parseMediaType("audio/wav");
            case ".ogg" -> MediaType.parseMediaType("audio/ogg");
            case ".m4a" -> MediaType.parseMediaType("audio/mp4");
            case ".flac" -> MediaType.parseMediaType("audio/flac");
            case ".mp4", ".m4v" -> MediaType.parseMediaType("video/mp4");
            case ".mov" -> MediaType.parseMediaType("video/quicktime");
            case ".webm" -> MediaType.parseMediaType("video/webm");
            case ".mkv" -> MediaType.parseMediaType("video/x-matroska");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }
}
