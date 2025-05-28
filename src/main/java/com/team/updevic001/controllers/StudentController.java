package com.team.updevic001.controllers;

import com.team.updevic001.model.dtos.response.course.ResponseCourseShortInfoDto;
import com.team.updevic001.model.dtos.response.course.ResponseFullCourseDto;
import com.team.updevic001.services.interfaces.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

//    @Operation(summary = "Enroll a student in a course")
//    @PostMapping("/enroll")
//    public ResponseEntity<String> enrollInCourse(@RequestParam String courseId) {
//        studentService.enrollInCourse(courseId);
//        return ResponseEntity.ok("Student successfully enrolled in the course.");
//    }

    @Operation(summary = "Unenroll a student from a course")
    @DeleteMapping("/unenroll")
    public ResponseEntity<String> unenrollFromCourse(@RequestParam String courseId) {
        studentService.unenrollUserFromCourse(courseId);
        return ResponseEntity.ok("Student successfully unenrolled from the course.");
    }

    @Operation(summary = "Get a student's course information")
    @GetMapping
    public ResponseEntity<ResponseCourseShortInfoDto> getStudentCourse(@RequestParam String courseId) {
        ResponseCourseShortInfoDto studentCourse = studentService.getStudentCourse(courseId);
        return ResponseEntity.ok(studentCourse);
    }

    @Operation(summary = "Get all courses of a student")
    @GetMapping("/courses")
    public ResponseEntity<List<ResponseCourseShortInfoDto>> getStudentCourses() {
        List<ResponseCourseShortInfoDto> courses = studentService.getStudentCourses();
        return ResponseEntity.ok(courses);
    }

    @Operation(summary = "Get all lessons of a student")
    @GetMapping("/lessons")
    public ResponseEntity<List<ResponseFullCourseDto>> getStudentLessons() {
        List<ResponseFullCourseDto> lessons = studentService.getStudentLessons();
        return ResponseEntity.ok(lessons);
    }


    @Operation(summary = "API request to become a teacher!")
    @GetMapping(path = "/for-teacher")
    public ResponseEntity<String> requestToBecameTeacher() {
        return ResponseEntity.ok("https://forms.gle/GersS1t7jwena3Dz9");
    }
}
