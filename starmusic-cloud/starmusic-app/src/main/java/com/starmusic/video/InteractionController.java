package com.starmusic.video;

import com.starmusic.video.VideoController.Video;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/videos")
public class InteractionController {

    public record CommentDto(long id, long videoId, String author, String body,
                             long memberId, String createdAt) {
        static CommentDto of(VideoComment e) {
            return new CommentDto(e.getId(), e.getVideoId(), e.getAuthor(), e.getBody(),
                    e.getMemberId(), e.getCreatedAt().toString());
        }
    }

    public record CommentRequest(String body) {
    }

    public record HistoryDto(Video video, String watchedAt) {
    }

    private static final int MAX_COMMENT = 500;

    private final VideoFavoriteRepository favorites;
    private final WatchHistoryRepository history;
    private final VideoCommentRepository comments;
    private final VideoController catalog;

    public InteractionController(VideoFavoriteRepository favorites,
                                 WatchHistoryRepository history,
                                 VideoCommentRepository comments,
                                 VideoController catalog) {
        this.favorites = favorites;
        this.history = history;
        this.comments = comments;
        this.catalog = catalog;
    }

    // ---------- 收藏 ----------

    @PostMapping("/{id}/favorite")
    public Map<String, Boolean> toggleFavorite(
            @PathVariable long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        requireLogin(userId);
        requireVideo(id);
        return favorites.findByMemberIdAndVideoId(userId, id)
                .map(f -> {
                    favorites.delete(f);
                    return Map.of("favorited", false);
                })
                .orElseGet(() -> {
                    VideoFavorite f = new VideoFavorite();
                    f.setMemberId(userId);
                    f.setVideoId(id);
                    favorites.save(f);
                    return Map.of("favorited", true);
                });
    }

    @GetMapping("/{id}/favorite")
    public Map<String, Boolean> favorited(
            @PathVariable long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        boolean on = userId != null
                && favorites.findByMemberIdAndVideoId(userId, id).isPresent();
        return Map.of("favorited", on);
    }

    @GetMapping("/favorites/mine")
    public List<Video> myFavorites(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        requireLogin(userId);
        Map<Long, Video> byId = catalog.catalog().stream()
                .collect(Collectors.toMap(Video::id, Function.identity()));
        return favorites.findByMemberIdOrderByCreatedAtDesc(userId).stream()
                .map(f -> byId.get(f.getVideoId()))
                .filter(v -> v != null)
                .toList();
    }

    // ---------- 觀看紀錄 ----------

    @PostMapping("/{id}/history")
    public Map<String, Object> recordHistory(
            @PathVariable long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        requireLogin(userId);
        requireVideo(id);
        WatchHistory h = history.findByMemberIdAndVideoId(userId, id)
                .orElseGet(() -> {
                    WatchHistory n = new WatchHistory();
                    n.setMemberId(userId);
                    n.setVideoId(id);
                    return n;
                });
        h.setWatchedAt(Instant.now());
        WatchHistory saved = history.save(h);
        return Map.of("videoId", saved.getVideoId(), "watchedAt", saved.getWatchedAt().toString());
    }

    @GetMapping("/history/mine")
    public List<HistoryDto> myHistory(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        requireLogin(userId);
        Map<Long, Video> byId = catalog.catalog().stream()
                .collect(Collectors.toMap(Video::id, Function.identity()));
        return history.findByMemberIdOrderByWatchedAtDesc(userId).stream()
                .filter(h -> byId.containsKey(h.getVideoId()))
                .map(h -> new HistoryDto(byId.get(h.getVideoId()), h.getWatchedAt().toString()))
                .toList();
    }

    // ---------- 評論 ----------

    @GetMapping("/{id}/comments")
    public List<CommentDto> listComments(@PathVariable long id) {
        requireVideo(id);
        return comments.findByVideoIdOrderByCreatedAtDesc(id).stream()
                .map(CommentDto::of).toList();
    }

    @PostMapping("/{id}/comments")
    public CommentDto addComment(
            @PathVariable long id,
            @RequestBody(required = false) CommentRequest req,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {
        requireLogin(userId);
        requireVideo(id);
        String body = req == null || req.body() == null ? "" : req.body().trim();
        if (body.isEmpty() || body.length() > MAX_COMMENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "評論必填且不超過 " + MAX_COMMENT + " 字");
        }
        String author = userName == null ? null
                : URLDecoder.decode(userName, StandardCharsets.UTF_8);
        VideoComment c = new VideoComment();
        c.setVideoId(id);
        c.setMemberId(userId);
        c.setAuthor(author == null || author.isBlank() ? "會員" : author);
        c.setBody(body);
        return CommentDto.of(comments.save(c));
    }

    @DeleteMapping("/comments/{commentId}")
    public void deleteComment(
            @PathVariable long commentId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireLogin(userId);
        VideoComment c = comments.findById(commentId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "評論不存在"));
        boolean owner = c.getMemberId().equals(userId);
        if (!owner && !"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只能刪除自己的評論");
        }
        comments.delete(c);
    }

    private void requireLogin(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "請先登入會員");
        }
    }

    private void requireVideo(long videoId) {
        if (!catalog.exists(videoId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "影片不存在");
        }
    }
}
