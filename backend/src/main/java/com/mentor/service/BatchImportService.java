package com.mentor.service;

import com.mentor.entity.Mentor;
import com.mentor.entity.Role;
import com.mentor.entity.Student;
import com.mentor.entity.User;
import com.mentor.mapper.AuthMapper;
import com.mentor.mapper.MentorMapper;
import com.mentor.mapper.StudentMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

@Service
public class BatchImportService {

    @Autowired
    private AuthMapper authMapper;
    @Autowired
    private StudentMapper studentMapper;
    @Autowired
    private MentorMapper mentorMapper;

    /**
     * 生成导师导入模板
     */
    public byte[] generateMentorTemplate() throws Exception {
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("导师批量导入");
        String[] headers = {"用户名*", "密码*", "邮箱*", "姓名*", "职称", "机构", "院系",
                "研究方向(逗号分隔)", "关键词(逗号分隔)", "个人简介", "组内方向",
                "期望学生素质", "指导风格", "可用名额", "经费状况", "教育背景",
                "个人主页", "最大学生数"};
        createHeaderRow(wb, sheet, headers);
        // 示例行
        Row example = sheet.createRow(1);
        String[] exampleData = {"mentor_zhang", "123456", "zhang@pku.edu.cn", "张教授", "教授",
                "北京大学", "计算机学院", "人工智能,机器学习", "AI,ML,DL",
                "专注于AI研究", "大模型应用", "数学基础扎实", "每周组会", "3",
                "国家自然科学基金", "北大博士", "https://example.com", "5"};
        for (int i = 0; i < exampleData.length; i++) {
            example.createCell(i).setCellValue(exampleData[i]);
        }
        return toBytes(wb);
    }

    /**
     * 生成学生导入模板
     */
    public byte[] generateStudentTemplate() throws Exception {
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("学生批量导入");
        String[] headers = {"用户名*", "密码*", "邮箱*", "姓名*", "学校", "专业", "学位(本科/硕士/博士)",
                "毕业年份", "GPA", "研究兴趣(逗号分隔)", "关键词(逗号分隔)", "个人简介",
                "编程技能(逗号分隔)", "项目经验", "期望研究方向", "期望导师风格"};
        createHeaderRow(wb, sheet, headers);
        Row example = sheet.createRow(1);
        String[] exampleData = {"student_li", "123456", "li@tsinghua.edu.cn", "李同学",
                "清华大学", "计算机科学", "硕士", "2026", "3.8",
                "自然语言处理,深度学习", "NLP,DL", "热爱科研",
                "Python,Java,C++", "参与过NLP项目", "大模型研究", "鼓励自主探索"};
        for (int i = 0; i < exampleData.length; i++) {
            example.createCell(i).setCellValue(exampleData[i]);
        }
        return toBytes(wb);
    }

    /**
     * 批量导入导师
     */
    @Transactional
    public Map<String, Object> importMentors(MultipartFile file) throws Exception {
        List<Map<String, Object>> results = new ArrayList<>();
        int success = 0, fail = 0;

        try (InputStream is = file.getInputStream(); Workbook wb = WorkbookFactory.create(is)) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isEmptyRow(row)) continue;
                Map<String, Object> rowResult = new LinkedHashMap<>();
                rowResult.put("row", i + 1);
                try {
                    String username = getCellString(row, 0);
                    String password = getCellString(row, 1);
                    String email = getCellString(row, 2);
                    String name = getCellString(row, 3);

                    if (username.isEmpty() || password.isEmpty() || email.isEmpty() || name.isEmpty()) {
                        throw new RuntimeException("用户名、密码、邮箱、姓名为必填项");
                    }
                    if (password.length() < 6) throw new RuntimeException("密码至少6位");
                    if (authMapper.getUserByUsername(username) != null) throw new RuntimeException("用户名已存在: " + username);
                    if (authMapper.getUserByEmail(email) != null) throw new RuntimeException("邮箱已存在: " + email);

                    // 创建用户
                    String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
                    User user = User.builder()
                            .username(username).password(hashedPassword)
                            .salt(UUID.randomUUID().toString())
                            .email(email).userType("mentor").status(1)
                            .createTime(new Date()).updateTime(new Date()).build();
                    authMapper.insertUser(user);

                    Role role = authMapper.getRoleByName("MENTOR");
                    if (role != null) authMapper.insertUserRole(user.getId(), role.getId());

                    // 创建导师记录
                    Mentor mentor = new Mentor();
                    mentor.setUserId(user.getId());
                    mentor.setName(name);
                    mentor.setEmail(email);
                    mentor.setTitle(getCellString(row, 4));
                    mentor.setInstitution(getCellString(row, 5));
                    mentor.setDepartment(getCellString(row, 6));
                    mentor.setResearchAreas(toJsonArray(getCellString(row, 7)));
                    mentor.setKeywords(toJsonArray(getCellString(row, 8)));
                    mentor.setBio(getCellString(row, 9));
                    mentor.setGroupDirection(getCellString(row, 10));
                    mentor.setExpectedStudentQualities(getCellString(row, 11));
                    mentor.setMentoringStyle(getCellString(row, 12));
                    String positions = getCellString(row, 13);
                    mentor.setAvailablePositions(positions.isEmpty() ? 3 : Integer.parseInt(positions));
                    mentor.setFundingStatus(getCellString(row, 14));
                    mentor.setEducationBackground(getCellString(row, 15));
                    mentor.setHomepageUrl(getCellString(row, 16));
                    String maxStu = getCellString(row, 17);
                    mentor.setMaxStudents(maxStu.isEmpty() ? 5 : Integer.parseInt(maxStu));
                    mentor.setAcceptingStudents(true);
                    mentor.setCurrentStudents(0);
                    mentor.setRatingAvg(BigDecimal.ZERO);
                    mentor.setRatingCount(0);
                    mentor.setViewCount(0);
                    mentor.setStatus(1);
                    mentor.setIsVerified(false);
                    mentor.setCreateTime(new Date());
                    mentor.setUpdateTime(new Date());
                    mentorMapper.insertMentor(mentor);

                    rowResult.put("status", "success");
                    rowResult.put("username", username);
                    rowResult.put("name", name);
                    success++;
                } catch (Exception e) {
                    rowResult.put("status", "fail");
                    rowResult.put("error", e.getMessage());
                    fail++;
                }
                results.add(rowResult);
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", success + fail);
        summary.put("success", success);
        summary.put("fail", fail);
        summary.put("details", results);
        return summary;
    }

    /**
     * 批量导入学生
     */
    @Transactional
    public Map<String, Object> importStudents(MultipartFile file) throws Exception {
        List<Map<String, Object>> results = new ArrayList<>();
        int success = 0, fail = 0;

        try (InputStream is = file.getInputStream(); Workbook wb = WorkbookFactory.create(is)) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isEmptyRow(row)) continue;
                Map<String, Object> rowResult = new LinkedHashMap<>();
                rowResult.put("row", i + 1);
                try {
                    String username = getCellString(row, 0);
                    String password = getCellString(row, 1);
                    String email = getCellString(row, 2);
                    String name = getCellString(row, 3);

                    if (username.isEmpty() || password.isEmpty() || email.isEmpty() || name.isEmpty()) {
                        throw new RuntimeException("用户名、密码、邮箱、姓名为必填项");
                    }
                    if (password.length() < 6) throw new RuntimeException("密码至少6位");
                    if (authMapper.getUserByUsername(username) != null) throw new RuntimeException("用户名已存在: " + username);
                    if (authMapper.getUserByEmail(email) != null) throw new RuntimeException("邮箱已存在: " + email);

                    String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
                    User user = User.builder()
                            .username(username).password(hashedPassword)
                            .salt(UUID.randomUUID().toString())
                            .email(email).userType("student").status(1)
                            .createTime(new Date()).updateTime(new Date()).build();
                    authMapper.insertUser(user);

                    Role role = authMapper.getRoleByName("STUDENT");
                    if (role != null) authMapper.insertUserRole(user.getId(), role.getId());

                    Student student = new Student();
                    student.setUserId(user.getId());
                    student.setName(name);
                    student.setEmail(email);
                    student.setCurrentInstitution(getCellString(row, 4));
                    student.setMajor(getCellString(row, 5));
                    student.setDegreeLevel(getCellString(row, 6));
                    String gradYear = getCellString(row, 7);
                    if (!gradYear.isEmpty()) student.setGraduationYear(Integer.parseInt(gradYear));
                    String gpa = getCellString(row, 8);
                    if (!gpa.isEmpty()) student.setGpa(new BigDecimal(gpa));
                    student.setResearchInterests(toJsonArray(getCellString(row, 9)));
                    student.setKeywords(toJsonArray(getCellString(row, 10)));
                    student.setBio(getCellString(row, 11));
                    student.setProgrammingSkills(toJsonArray(getCellString(row, 12)));
                    student.setProjectExperience(getCellString(row, 13));
                    student.setExpectedResearchDirection(getCellString(row, 14));
                    student.setPreferredMentorStyle(getCellString(row, 15));
                    student.setStatus(1);
                    student.setCreateTime(new Date());
                    student.setUpdateTime(new Date());
                    studentMapper.insertStudent(student);

                    rowResult.put("status", "success");
                    rowResult.put("username", username);
                    rowResult.put("name", name);
                    success++;
                } catch (Exception e) {
                    rowResult.put("status", "fail");
                    rowResult.put("error", e.getMessage());
                    fail++;
                }
                results.add(rowResult);
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", success + fail);
        summary.put("success", success);
        summary.put("fail", fail);
        summary.put("details", results);
        return summary;
    }

    // ===== 工具方法 =====

    private void createHeaderRow(Workbook wb, Sheet sheet, String[] headers) {
        CellStyle headerStyle = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        headerStyle.setFont(font);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 5000);
        }
    }

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }

    private boolean isEmptyRow(Row row) {
        for (int i = 0; i < 4; i++) {
            if (!getCellString(row, i).isEmpty()) return false;
        }
        return true;
    }

    private String toJsonArray(String commaStr) {
        if (commaStr == null || commaStr.isEmpty()) return "[]";
        String[] parts = commaStr.split("[,，]");
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(parts[i].trim()).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private byte[] toBytes(Workbook wb) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        wb.write(bos);
        wb.close();
        return bos.toByteArray();
    }
}
