package com.team.updevic001.dao.repositories;

import com.team.updevic001.dao.entities.Course;
import com.team.updevic001.dao.entities.User;
import com.team.updevic001.dao.entities.UserCourseFee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCourseFeeRepository extends JpaRepository<UserCourseFee, String> {

    boolean existsUserCourseFeeByCourseAndUser(Course course, User user);
}
