package com.team.updevic001.dao.repositories;

import com.team.updevic001.dao.entities.Lesson;
import com.team.updevic001.dao.entities.User;
import com.team.updevic001.dao.entities.UserLessonStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserLessonStatusRepository extends JpaRepository<UserLessonStatus, String> {
    Optional<UserLessonStatus> findByUserAndLesson(User user, Lesson lesson);

}
