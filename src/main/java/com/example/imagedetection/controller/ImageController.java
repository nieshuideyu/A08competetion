package com.example.imagedetection.controller;

import com.example.imagedetection.exception.ImageProcessingException;
import com.example.imagedetection.service.ImageService;
import com.example.imagedetection.util.ImageValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ImageController {

    @Autowired
    private ImageService imageService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        log.info("收到图片上传请求: {}", file.getOriginalFilename());
        try {
            // 验证文件
            ImageValidator.validateImage(file);
            
            // 处理图片
            Long fileId = imageService.uploadImage(file);
            
            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "文件上传成功");
            
            Map<String, Object> data = new HashMap<>();
            data.put("fileId", fileId);
            response.put("data", data);
            
            return ResponseEntity.ok(response);
        } catch (ImageProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("图片上传失败", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "上传失败");
        }
    }

    @GetMapping("/detect/{id}")
    public ResponseEntity<Map<String, Object>> detectImage(@PathVariable Long id) {
        try {
            List<Map<String, Object>> results = imageService.detectImage(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            
            Map<String, Object> data = new HashMap<>();
            data.put("results", results);
            response.put("data", data);
            
            return ResponseEntity.ok(response);
        } catch (ImageProcessingException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "图片不存在");
        } catch (Exception e) {
            log.error("检测失败", e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "检测失败");
        }
    }

    @GetMapping(value = "/get_initial_image/{id}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getInitialImage(@PathVariable Long id) {
        try {
            byte[] imageData = imageService.getImageData(id);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            headers.setContentLength(imageData.length);
            
            return new ResponseEntity<>(imageData, headers, HttpStatus.OK);
        } catch (ImageProcessingException e) {
            log.error("获取原始图片失败: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getHistory() {
        try {
            List<Map<String, Object>> history = imageService.getHistory();
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取历史记录成功");
            
            Map<String, Object> data = new HashMap<>();
            data.put("records", history);
            response.put("data", data);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取历史记录失败", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "获取历史记录失败");
        }
    }
} 