package com.team.updevic001.dao.repositories;

import com.team.updevic001.dao.entities.Course;
import com.team.updevic001.dao.entities.Teacher;
import com.team.updevic001.dao.entities.TeacherCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeacherCourseRepository extends JpaRepository<TeacherCourse, String> {


    @Query("SELECT tc.course.id FROM TeacherCourse tc WHERE tc.teacher = :teacher")
    List<String> findAllCourseIdsByTeacher(@Param("teacher") Teacher teacher);


    int countTeacherCourseByTeacher(Teacher teacher);

    Optional<TeacherCourse> findByCourseAndTeacher(Course course, Teacher teacher);

    List<TeacherCourse> findTeacherCourseByCourse(Course course);

    List<TeacherCourse> findTeacherCourseByTeacher(Teacher teacher);

    Optional<TeacherCourse> findByCourseIdAndTeacher(String courseId, Teacher authenticatedTeacher);

}
