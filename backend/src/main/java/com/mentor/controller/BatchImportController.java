package com.mentor.controller;

import com.mentor.service.BatchImportService;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 批量导入控制器（仅管理员）
 */
@RestController
@RequestMapping("/admin/batch")
public class BatchImportController {

    @Autowired
    private BatchImportService batchImportService;

    /**
     * 下载导师导入模板
     */
    @GetMapping("/template/mentor")
    @RequiresRoles("ADMIN")
    public ResponseEntity<byte[]> downloadMentorTemplate() throws Exception {
        byte[] data = batchImportService.generateMentorTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=mentor_import_template.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    /**
     * 下载学生导入模板
     */
    @GetMapping("/template/student")
    @RequiresRoles("ADMIN")
    public ResponseEntity<byte[]> downloadStudentTemplate() throws Exception {
        byte[] data = batchImportService.generateStudentTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=student_import_template.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    /**
     * 批量导入导师
     */
    @PostMapping("/import/mentor")
    @RequiresRoles("ADMIN")
    public Map<String, Object> importMentors(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (file.isEmpty()) {
                result.put("code", 400);
                result.put("message", "请选择文件");
                return result;
            }
            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
                result.put("code", 400);
                result.put("message", "仅支持Excel文件(.xlsx/.xls)");
                return result;
            }
            Map<String, Object> importResult = batchImportService.importMentors(file);
            result.put("code", 0);
            result.put("message", "导入完成");
            result.put("data", importResult);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "导入失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 批量导入学生
     */
    @PostMapping("/import/student")
    @RequiresRoles("ADMIN")
    public Map<String, Object> importStudents(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (file.isEmpty()) {
                result.put("code", 400);
                result.put("message", "请选择文件");
                return result;
            }
            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
                result.put("code", 400);
                result.put("message", "仅支持Excel文件(.xlsx/.xls)");
                return result;
            }
            Map<String, Object> importResult = batchImportService.importStudents(file);
            result.put("code", 0);
            result.put("message", "导入完成");
            result.put("data", importResult);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "导入失败: " + e.getMessage());
        }
        return result;
    }
}
