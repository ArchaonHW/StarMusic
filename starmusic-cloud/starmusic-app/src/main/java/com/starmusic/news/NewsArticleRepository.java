package com.starmusic.news;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsArticleRepository extends JpaRepository<NewsArticleEntity, Long> {
}
