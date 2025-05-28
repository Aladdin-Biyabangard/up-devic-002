package com.team.updevic001.services.interfaces;

import com.team.updevic001.model.dtos.request.CommentDto;
import com.team.updevic001.model.dtos.response.comment.ResponseCommentDto;

import java.util.List;

public interface CommentService {

    ResponseCommentDto addCommentToCourse(String courseId, CommentDto commentDto);

    ResponseCommentDto addCommentToLesson(String lessonId, CommentDto commentDto);

    ResponseCommentDto updateComment(String commentId, CommentDto commentDto);

    List<ResponseCommentDto> getCourseComment(String courseId);

    List<ResponseCommentDto> getLessonComment(String lessonId);

    void deleteComment(String commentId);
}
