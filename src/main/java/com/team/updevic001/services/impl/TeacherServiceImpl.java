package com.team.updevic001.services.impl;

import com.team.updevic001.configuration.mappers.CourseMapper;
import com.team.updevic001.configuration.mappers.TeacherMapper;
import com.team.updevic001.dao.entities.Course;
import com.team.updevic001.dao.entities.Teacher;
import com.team.updevic001.dao.entities.TeacherCourse;
import com.team.updevic001.dao.entities.User;
import com.team.updevic001.dao.repositories.StudentCourseRepository;
import com.team.updevic001.dao.repositories.TeacherCourseRepository;
import com.team.updevic001.dao.repositories.TeacherRepository;
import com.team.updevic001.exceptions.ForbiddenException;
import com.team.updevic001.exceptions.ResourceNotFoundException;
import com.team.updevic001.model.dtos.response.course.ResponseCourseShortInfoDto;
import com.team.updevic001.model.dtos.response.teacher.ResponseTeacherDto;
import com.team.updevic001.model.dtos.response.teacher.TeacherMainInfo;
import com.team.updevic001.model.enums.Role;
import com.team.updevic001.services.interfaces.TeacherService;
import com.team.updevic001.utility.AuthHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherServiceImpl implements TeacherService {

    private final TeacherRepository teacherRepository;
    private final TeacherCourseRepository teacherCourseRepository;
    private final AuthHelper authHelper;
    private final TeacherMapper teacherMapper;
    private final CourseMapper courseMapper;
    private final StudentCourseRepository studentCourseRepository;


    @Override
    public List<ResponseCourseShortInfoDto> getTeacherAndRelatedCourses() {
        Teacher teacher = getAuthenticatedTeacher();
        log.info("Getting teacher and related courses. Teacher ID: {}", teacher.getId());

        List<TeacherCourse> teacherCourses = teacherCourseRepository.findTeacherCourseByTeacher(teacher);

        List<ResponseCourseShortInfoDto> courses = teacherCourses.stream()
                .map(teacherCourse -> {
                    Course course = teacherCourse.getCourse();
                    return courseMapper.toCourseResponse(course);
                })
                .toList();

        log.info("Retrieved {} courses for teacher ID: {}", courses.size(), teacher.getId());
        return courses;
    }

    @Override
    public TeacherMainInfo getInfo() {
        Teacher teacher = getAuthenticatedTeacher();
        List<String> allCourseIdsByTeacher = teacherCourseRepository.findAllCourseIdsByTeacher(teacher);
        int courseCount = allCourseIdsByTeacher.size();
        int studentCount = studentCourseRepository.countAllStudentsByCourseIds(allCourseIdsByTeacher);
        return new TeacherMainInfo(courseCount, studentCount, teacher.getBalance());
    }


    @Override
    public void deleteTeacher(String teacherId) {
        Teacher teacher = validateTeacherAndAccess(teacherId, Boolean.TRUE);
        teacherRepository.delete(teacher);
    }

    @Override
    public void deleteAllTeachers() {
        teacherRepository.deleteAll();
        teacherRepository.resetAutoIncrement();
    }

    private Teacher validateTeacherAndAccess(String teacherId, boolean isAllowedToAdmin) {
        User authenticatedUser = authHelper.getAuthenticatedUser();
        Teacher teacher = findTeacherById(teacherId);

        boolean isOwner = teacher.getUser().getId().equals(authenticatedUser.getId());
        boolean isAdmin = isAllowedToAdmin && authenticatedUser.getRoles().stream()
                .anyMatch(userRole -> userRole.getName().equals(Role.ADMIN));

        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("NOT_ALLOWED");
        }

        return teacher;
    }

    public List<ResponseTeacherDto> searchTeacher(String keyword) {
        List<Teacher> teachers = teacherRepository.searchTeacher(keyword);
        return !teachers.isEmpty() ? teacherMapper.toTeacherDto(teachers) : List.of();
    }

    public Teacher getAuthenticatedTeacher() {
        User authenticatedUser = authHelper.getAuthenticatedUser();
        Teacher teacher = authenticatedUser.getTeacher();
        if (teacher == null) {
            log.info("User is not teacher");
            throw new ForbiddenException("NOT_ALLOWED");
        }
        return teacher;
    }

    private Teacher findTeacherById(String teacherID) {
        log.info("Finding teacher by ID: {}", teacherID);
        return teacherRepository.findById(teacherID)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found this id: " + teacherID));
    }
}