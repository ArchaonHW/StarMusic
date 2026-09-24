package com.starmusic.magazine;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MagazineRepository extends JpaRepository<MagazineEntity, Long> {
}
