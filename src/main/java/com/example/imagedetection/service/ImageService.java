package com.example.imagedetection.service;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

public interface ImageService {
    Long uploadImage(MultipartFile file);
    List<Map<String, Object>> detectImage(Long id);
    byte[] getImageData(Long id);
}