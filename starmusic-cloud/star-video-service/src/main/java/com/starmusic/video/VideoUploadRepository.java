package com.starmusic.video;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VideoUploadRepository extends JpaRepository<VideoUpload, Long> {

    List<VideoUpload> findByStatusOrderByCreatedAtDesc(String status);

    List<VideoUpload> findByUploaderOrderByCreatedAtDesc(String uploader);

    List<VideoUpload> findByStatus(String status);
}
