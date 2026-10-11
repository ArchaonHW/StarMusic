package com.starmusic.video;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VideoUploadRepository extends JpaRepository<VideoUpload, Long> {

    List<VideoUpload> findByStatusOrderByCreatedAtDesc(String status);

    List<VideoUpload> findByUploaderOrderByCreatedAtDesc(String uploader);

    List<VideoUpload> findByStatus(String status);

    Optional<VideoUpload> findByFilename(String filename);
}
