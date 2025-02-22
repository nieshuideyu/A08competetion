package com.example.imagedetection.repository;

import com.example.imagedetection.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    List<Image> findByUploadTimeBetween(LocalDateTime start, LocalDateTime end);
} 