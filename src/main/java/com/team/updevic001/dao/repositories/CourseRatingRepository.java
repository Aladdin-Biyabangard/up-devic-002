package com.team.updevic001.dao.repositories;

import com.team.updevic001.dao.entities.Course;
import com.team.updevic001.dao.entities.CourseRating;
import com.team.updevic001.dao.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseRatingRepository extends JpaRepository<CourseRating, String> {
    List<CourseRating> findByCourse(Course course);

    List<CourseRating> findByUser(User user);

    Optional<CourseRating> findCourseRatingByCourseAndUser(Course course, User user);

}
