package com.example.imagedetection.service.impl;

import com.example.imagedetection.entity.Image;
import com.example.imagedetection.exception.ImageProcessingException;
import com.example.imagedetection.repository.ImageRepository;
import com.example.imagedetection.service.ImageService;
import com.example.imagedetection.service.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@Transactional
public class ImageServiceImpl implements ImageService {

    @Autowired
    private StorageService storageService;
    
    @Autowired
    private ImageRepository imageRepository;
    
    @Value("${upload.path}")
    private String uploadPath;

    @Override
    public Long uploadImage(MultipartFile file) {
        try {
            // 存储文件
            String storedPath = storageService.store(file, file.getOriginalFilename(), "upload");
            
            // 创建记录
            Image image = new Image();
            image.setOriginalFilename(file.getOriginalFilename());
            image.setFilePath(storedPath);
            image.setUploadTime(LocalDateTime.now());
            
            // 保存记录
            image = imageRepository.save(image);
            
            return image.getId();
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new ImageProcessingException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> detectImage(Long id) {
        // 检查图片是否存在
        imageRepository.findById(id)
                .orElseThrow(() -> new ImageProcessingException("图片不存在"));

        // 返回模拟的检测结果
        List<Map<String, Object>> results = new ArrayList<>();
        
        Map<String, Object> result1 = new HashMap<>();
        result1.put("label", "划痕");
        result1.put("confidence", 0.95);
        results.add(result1);
        
        Map<String, Object> result2 = new HashMap<>();
        result2.put("label", "夹杂物");
        result2.put("confidence", 0.8);
        results.add(result2);
        
        return results;
    }

    @Override
    public byte[] getImageData(Long id) {
        try {
            // 获取图片记录
            Image image = imageRepository.findById(id)
                    .orElseThrow(() -> new ImageProcessingException("图片不存在"));

            // 读取图片文件
            Path imagePath = Paths.get(uploadPath, image.getFilePath());
            if (!Files.exists(imagePath)) {
                throw new ImageProcessingException("图片文件不存在");
            }

            // 返回图片数据
            return Files.readAllBytes(imagePath);
        } catch (IOException e) {
            log.error("读取图片文件失败: {}", e.getMessage());
            throw new ImageProcessingException("读取图片文件失败: " + e.getMessage());
        }
    }
} 