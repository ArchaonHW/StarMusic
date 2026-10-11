package com.starmusic.post;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    // 檔案改由 PostController /api/posts/files/{name} 依投稿狀態驗證後回傳，
    // 不再使用靜態資源對映。
}
