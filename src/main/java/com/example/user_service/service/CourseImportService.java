package com.example.user_service.service;

import com.example.user_service.entity.*;
import com.example.user_service.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CourseImportService {

    private final PythonScriptService pythonScriptService;
    private final AcademicYearRepository academicYearRepository;
    private final SemesterRepository semesterRepository;
    private final CohortRepository cohortRepository;
    private final ClassRepository classRepository;
    private final CourseRepository courseRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${search.service.url}")
    private String searchServiceUrl;

    @Transactional
    public int importCourses(MultipartFile file) throws Exception {
        // Get original file extension
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".") 
            ? originalFilename.substring(originalFilename.lastIndexOf("."))
            : ".xlsx";
        
        // Save uploaded file temporarily
        Path tempInput = Files.createTempFile("course_input_", extension);
        Files.copy(file.getInputStream(), tempInput, StandardCopyOption.REPLACE_EXISTING);

        // Process with Python script
        Path tempOutput = Files.createTempFile("course_output_", ".json");
        pythonScriptService.executeScript("process_course.py", 
            tempInput.toString(), 
            tempOutput.toString());

        // Read JSON output
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(tempOutput.toFile());

        // Delete all old data before importing new data
        deleteAllCourseData();
        
        // Clear Elasticsearch index
        clearElasticsearchIndex();

        String academicYearName = root.get("academic_year").asText();
        AcademicYear academicYear = academicYearRepository.findByYearName(academicYearName)
            .orElseGet(() -> {
                AcademicYear ay = new AcademicYear();
                ay.setYearName(academicYearName);
                return academicYearRepository.save(ay);
            });

        int count = 0;
        JsonNode semesters = root.get("semesters");
        Iterator<Map.Entry<String, JsonNode>> semesterFields = semesters.fields();
        
        while (semesterFields.hasNext()) {
            Map.Entry<String, JsonNode> semesterEntry = semesterFields.next();
            String semesterName = semesterEntry.getKey();
            
            Semester semester = semesterRepository.findBySemesterNameAndAcademicYear(semesterName, academicYear)
                .orElseGet(() -> {
                    Semester s = new Semester();
                    s.setSemesterName(semesterName);
                    s.setAcademicYear(academicYear);
                    return semesterRepository.save(s);
                });

            JsonNode cohorts = semesterEntry.getValue();
            Iterator<Map.Entry<String, JsonNode>> cohortFields = cohorts.fields();
            
            while (cohortFields.hasNext()) {
                Map.Entry<String, JsonNode> cohortEntry = cohortFields.next();
                String cohortCode = cohortEntry.getKey();
                
                Cohort cohort = cohortRepository.findByCohortCodeAndSemester(cohortCode, semester)
                    .orElseGet(() -> {
                        Cohort c = new Cohort();
                        c.setCohortCode(cohortCode);
                        c.setSemester(semester);
                        return cohortRepository.save(c);
                    });

                JsonNode classes = cohortEntry.getValue();
                Iterator<Map.Entry<String, JsonNode>> classFields = classes.fields();
                
                while (classFields.hasNext()) {
                    Map.Entry<String, JsonNode> classEntry = classFields.next();
                    String classCode = classEntry.getKey();
                    
                    ClassEntity classEntity = classRepository.findByClassCodeAndCohort(classCode, cohort)
                        .orElseGet(() -> {
                            ClassEntity ce = new ClassEntity();
                            ce.setClassCode(classCode);
                            ce.setCohort(cohort);
                            return classRepository.save(ce);
                        });

                    JsonNode courses = classEntry.getValue();
                    for (JsonNode courseNode : courses) {
                        Course course = new Course();
                        course.setCourseName(courseNode.get("course_name").asText());
                        course.setSubtopic(courseNode.has("subtopic") ? courseNode.get("subtopic").asText() : null);
                        course.setTheoryCredits(courseNode.get("theory_credits").asDouble());
                        course.setPracticalCredits(courseNode.get("practical_credits").asDouble());
                        course.setTotalCredits(courseNode.get("total_credits").asDouble());
                        course.setClassEntity(classEntity);
                        
                        courseRepository.save(course);
                        
                        // Index to ElasticSearch
                        indexCourseToElasticsearch(course);
                        count++;
                    }
                }
            }
        }

        // Cleanup temp files
        Files.deleteIfExists(tempInput);
        Files.deleteIfExists(tempOutput);

        return count;
    }

    private void deleteAllCourseData() {
        // Delete in reverse order of foreign key dependencies
        courseRepository.deleteAll();
        classRepository.deleteAll();
        cohortRepository.deleteAll();
        semesterRepository.deleteAll();
        academicYearRepository.deleteAll();
    }
    
    private void clearElasticsearchIndex() {
        try {
            String url = searchServiceUrl + "/api/courses/clear";
            restTemplate.delete(url);
        } catch (Exception e) {
            // Log error but don't fail the import
            System.err.println("Failed to clear Elasticsearch index: " + e.getMessage());
        }
    }

    private void indexCourseToElasticsearch(Course course) {
        try {
            String url = searchServiceUrl + "/api/courses/index";
            
            // Create DTO with only courseName and subtopic
            Map<String, String> indexRequest = Map.of(
                "courseName", course.getCourseName() != null ? course.getCourseName() : "",
                "subtopic", course.getSubtopic() != null ? course.getSubtopic() : ""
            );
            
            restTemplate.postForEntity(url, indexRequest, String.class);
        } catch (Exception e) {
            // Log error but don't fail the import
            System.err.println("Failed to index course to Elasticsearch: " + e.getMessage());
        }
    }
}
