package com.example.imagedetection.service.impl;

import com.example.imagedetection.entity.Image;
import com.example.imagedetection.exception.ImageProcessingException;
import com.example.imagedetection.repository.ImageRepository;
import com.example.imagedetection.service.ImageService;
import com.example.imagedetection.service.StorageService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.*;
import java.util.stream.Collectors;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;

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

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String generateImageUrl(String imagePath) {
        return String.format("https://amazed-obviously-bluejay.ngrok-free.app/uploads/" + imagePath);
    }

    @Override
    @Transactional
    public Map<String, Object> uploadImage(MultipartFile file) {
        try {
            log.debug("开始处理上传图片: {}", file.getOriginalFilename());
            
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFilename = UUID.randomUUID().toString() + extension;
            
            // 确保上传目录存在
            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            
            // 保存文件
            Path filePath = uploadDir.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            log.debug("文件已保存到: {}", filePath);
            
            // 保存记录到数据库
            Image image = new Image();
            image.setImagePath(newFilename);
            image = imageRepository.save(image);
            log.debug("数据库记录已创建，ID: {}", image.getImageId());
            
            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("fileId", image.getImageId());
            result.put("fileName", originalFilename);
            result.put("url", generateImageUrl(newFilename));
            
            return result;
            
        } catch (IOException e) {
            log.error("保存图片失败: {}", e.getMessage(), e);
            throw new ImageProcessingException("保存图片失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public List<Map<String, Object>> detectImage(Long id) {
        try {
            log.debug("开始处理图片检测，ID: {}", id);
            
            // 检查图片是否存在
            Image image = imageRepository.findById(id)
                    .orElseThrow(() -> new ImageProcessingException("图片不存在"));
            
            log.debug("找到图片记录: {}", image);

            // 生成检测结果（这里是示例，实际应该调用检测服务）
            List<Map<String, Object>> detectionResults = new ArrayList<>();
            
            Map<String, Object> result1 = new HashMap<>();
            result1.put("label", "划痕");
            result1.put("confidence", 0.95);
            detectionResults.add(result1);
            
            Map<String, Object> result2 = new HashMap<>();
            result2.put("label", "夹杂物");
            result2.put("confidence", 0.80);
            detectionResults.add(result2);
            
            // 生成结果图和热力图路径
            String timestamp = String.valueOf(System.currentTimeMillis());
            String resultPath = String.format("results/%d_%s_result.jpg", id, timestamp);
            String heatmapPath = String.format("heatmaps/%d_%s_heatmap.jpg", id, timestamp);
            
            // 确保目录存在
            Files.createDirectories(Paths.get(uploadPath, "results"));
            Files.createDirectories(Paths.get(uploadPath, "heatmaps"));
            
            log.debug("生成的路径 - 结果图: {}, 热力图: {}", resultPath, heatmapPath);
            
            // 更新图片记录
            image.setResultPath(resultPath);
            image.setHeatmapPath(heatmapPath);
            image.setDetectionText(objectMapper.writeValueAsString(detectionResults));
            
            // 保存更新
            imageRepository.save(image);
            log.debug("已更新图片检测结果");
            
            return detectionResults;
        } catch (Exception e) {
            log.error("检测失败: {}", e.getMessage(), e);
            throw new ImageProcessingException("检测失败: " + e.getMessage());
        }
    }

    @Override
    public byte[] getImageData(Long id) {
        try {
            log.debug("开始获取图片数据，ID: {}", id);
            
            // 获取图片记录
            Image image = imageRepository.findById(id)
                    .orElseThrow(() -> new ImageProcessingException("图片不存在"));

            // 读取图片文件
            Path imagePath = Paths.get(uploadPath, image.getImagePath());
            if (!Files.exists(imagePath)) {
                throw new ImageProcessingException("图片文件不存在: " + imagePath);
            }

            byte[] data = Files.readAllBytes(imagePath);
            log.debug("成功读取图片数据，大小: {} bytes", data.length);
            
            return data;
        } catch (IOException e) {
            log.error("读取图片文件失败: {}", e.getMessage(), e);
            throw new ImageProcessingException("读取图片文件失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getHistory() {
        try {
            log.debug("开始获取历史记录");
            
            List<Image> images = imageRepository.findAll();
            log.debug("查询到 {} 条记录", images.size());
            
            return images.stream().map(image -> {
                Map<String, Object> record = new HashMap<>();
                record.put("id", image.getImageId());
                
                // 格式化日期
                record.put("date", image.getCreateTime() != null ? 
                    image.getCreateTime().toLocalDate().toString() : 
                    LocalDate.now().toString());
                
                // 图片URL
                record.put("initial_image", generateImageUrl(image.getImagePath()));
                if (image.getResultPath() != null) {
                    record.put("result_image", generateImageUrl(image.getResultPath()));
                }
                if (image.getHeatmapPath() != null) {
                    record.put("heatmap", generateImageUrl(image.getHeatmapPath()));
                }
                
                // 解析检测结果
                List<Map<String, Object>> results = new ArrayList<>();
                try {
                    if (image.getDetectionText() != null) {
                        results = objectMapper.readValue(
                            image.getDetectionText(), 
                            new TypeReference<List<Map<String, Object>>>() {}
                        );
                    }
                } catch (Exception e) {
                    log.warn("解析检测结果失败: {}", e.getMessage());
                }
                record.put("results", results);
                
                return record;
            }).collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("获取历史记录失败: {}", e.getMessage(), e);
            throw new ImageProcessingException("获取历史记录失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteImage(Long id) {
        try {
            log.debug("开始删除图片，ID: {}", id);
            
            // 获取图片记录
            Image image = imageRepository.findById(id)
                    .orElseThrow(() -> new ImageProcessingException("图片不存在"));

            // 删除物理文件
            Path imagePath = Paths.get(uploadPath, image.getImagePath());
            if (Files.exists(imagePath)) {
                Files.delete(imagePath);
                log.debug("已删除原始图片文件: {}", imagePath);
            }

            // 删除结果图（如果存在）
            if (image.getResultPath() != null) {
                Path resultPath = Paths.get(uploadPath, image.getResultPath());
                if (Files.exists(resultPath)) {
                    Files.delete(resultPath);
                    log.debug("已删除结果图文件: {}", resultPath);
                }
            }

            // 删除热力图（如果存在）
            if (image.getHeatmapPath() != null) {
                Path heatmapPath = Paths.get(uploadPath, image.getHeatmapPath());
                if (Files.exists(heatmapPath)) {
                    Files.delete(heatmapPath);
                    log.debug("已删除热力图文件: {}", heatmapPath);
                }
            }

            // 删除数据库记录
            imageRepository.deleteById(id);
            log.debug("已删除数据库记录");

        } catch (IOException e) {
            log.error("删除图片文件失败: {}", e.getMessage(), e);
            throw new ImageProcessingException("删除图片文件失败: " + e.getMessage());
        }
    }
} 