package com.team.updevic001.services.impl;

import com.team.updevic001.configuration.mappers.CommentMapper;
import com.team.updevic001.dao.entities.Comment;
import com.team.updevic001.dao.entities.Course;
import com.team.updevic001.dao.entities.Lesson;
import com.team.updevic001.dao.entities.User;
import com.team.updevic001.dao.repositories.CommentRepository;
import com.team.updevic001.dao.repositories.CourseRepository;
import com.team.updevic001.dao.repositories.LessonRepository;
import com.team.updevic001.dao.repositories.UserCourseFeeRepository;
import com.team.updevic001.exceptions.ForbiddenException;
import com.team.updevic001.exceptions.PaymentStatusException;
import com.team.updevic001.exceptions.ResourceNotFoundException;
import com.team.updevic001.model.dtos.request.CommentDto;
import com.team.updevic001.model.dtos.response.comment.ResponseCommentDto;
import com.team.updevic001.services.interfaces.CommentService;
import com.team.updevic001.utility.AuthHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CourseRepository courseRepository;
    private final CommentMapper commentMapper;
    private final LessonRepository lessonRepository;
    private final CommentRepository commentRepository;
    private final AuthHelper authHelper;
    private final CourseServiceImpl courseServiceImpl;
    private final UserCourseFeeRepository userCourseFeeRepository;
    private final LessonServiceImpl lessonServiceImpl;

    @Override
    @Transactional
    public ResponseCommentDto addCommentToCourse(String courseId, CommentDto commentDto) {
        User authenticatedUser = authHelper.getAuthenticatedUser();
        Course course = courseServiceImpl.findCourseById(courseId);
        log.info("Operation of adding new comment to course with ID {} started by user with ID {}", courseId, authenticatedUser.getId());
        boolean exists = userCourseFeeRepository.existsUserCourseFeeByCourseAndUser(course, authenticatedUser);
        if (exists) {
            Comment comment = Comment.builder()
                    .content(commentDto.getContent())
                    .user(authenticatedUser)
                    .course(course)
                    .build();
            commentRepository.save(comment);
            log.info("Comment successfully created to course with ID {}.", courseId);
            return commentMapper.toDto(comment);
        } else {
            throw new PaymentStatusException("The user has not paid for the course.");
        }
    }

    @Override
    @Transactional
    public ResponseCommentDto addCommentToLesson(String lessonId, CommentDto commentDto) {
        User authenticatedUser = authHelper.getAuthenticatedUser();
        log.info("Operation of adding new comment to lesson with ID {} started by user with ID {}", lessonId, authenticatedUser.getId());
        Lesson lesson = lessonServiceImpl.findLessonById(lessonId);
        boolean exists = userCourseFeeRepository.existsUserCourseFeeByCourseAndUser(lesson.getCourse(), authenticatedUser);
        if (exists) {
            Comment comment = Comment.builder()
                    .content(commentDto.getContent())
                    .user(authenticatedUser)
                    .lesson(lesson)
                    .build();
            commentRepository.save(comment);
            log.info("Comment successfully created to lesson with ID {}.", lessonId);
            return commentMapper.toDto(comment);
        } else {
            throw new PaymentStatusException("The user has not paid for the course.");
        }
    }

    @Override
    @Transactional
    public ResponseCommentDto updateComment(String commentId, CommentDto commentDto) {
        User authenticatedUser = authHelper.getAuthenticatedUser();
        log.info("Operation of updating comment with ID {} started by user with ID {}", commentId, authenticatedUser.getId());
        Comment comment = findCommentById(commentId);
        if (!comment.getUser().getId().equals(authenticatedUser.getId())) {
            log.error("User wit ID {} not allowed to delete comment with ID {}: User is not author of comment", authenticatedUser.getId(), commentId);
            throw new ForbiddenException("NOT_ALLOWED_UPDATE_COMMENT");
        }
        comment.setContent(commentDto.getContent());
        commentRepository.save(comment);
        log.info("Comment with ID {} successfully updated", commentId);
        return commentMapper.toDto(comment);
    }

    @Override
    public List<ResponseCommentDto> getCourseComment(String courseId) {
        log.info("Operation of getting comments for course with ID {} started", courseId);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(Course.class));
        List<Comment> comments = course.getComments();
        List<ResponseCommentDto> commentDtos = !comments.isEmpty() ? commentMapper.toDto(comments) : List.of();
        log.info("Comments for course with ID {} are returned to user", courseId);
        return commentDtos;
    }

    @Override
    public List<ResponseCommentDto> getLessonComment(String lessonId) {
        log.info("Operation of getting comments for lesson with ID {} started", lessonId);
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException(Lesson.class));
        List<Comment> comments = lesson.getComments();
        List<ResponseCommentDto> commentDtos = !comments.isEmpty() ? commentMapper.toDto(comments) : List.of();
        log.info("Comments for lesson with ID {} are returned to user", lessonId);
        return commentDtos;
    }

    @Override
    @Transactional
    public void deleteComment(String commentId) {
        User authenticatedUser = authHelper.getAuthenticatedUser();
        log.info("Operation of deleting comment with ID {} started by user with ID {}", commentId, authenticatedUser.getId());
        Comment comment = findCommentById(commentId);
        if (!comment.getUser().getId().equals(authenticatedUser.getId())
                || comment.getCourse().getTeacherCourses().stream()
                .map(tc -> tc.getTeacher().getUser().getId())
                .noneMatch(authenticatedUser.getId()::equals)) {
            log.error("User with ID {} not allowed to delete comment: User must be either admin of the course or author of course", authenticatedUser.getId());
            throw new ForbiddenException("NOT_ALLOWED_DELETE_COMMENT");
        }
        commentRepository.deleteById(commentId);
        log.info("Comment successfully deleted");
    }

    public Comment findCommentById(String commentId) {
        log.debug("Looking for comment with ID: {}", commentId);
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment with ID " + commentId + " not found"));
    }
}
