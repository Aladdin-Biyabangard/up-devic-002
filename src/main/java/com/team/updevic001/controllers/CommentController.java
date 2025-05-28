package com.team.updevic001.controllers;

import com.team.updevic001.model.dtos.request.CommentDto;
import com.team.updevic001.model.dtos.response.comment.ResponseCommentDto;
import com.team.updevic001.services.interfaces.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/comment")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentServiceImpl;

    @Operation(summary = "Kursa coment yazmaq")
    @PostMapping(path = "{courseId}/course-comment")
    public ResponseEntity<ResponseCommentDto> addCommentToCourse(@PathVariable String courseId,
                                                                 @RequestBody CommentDto comment) {
        ResponseCommentDto responseCommentDto = commentServiceImpl.addCommentToCourse(courseId, comment);
        return ResponseEntity.ok(responseCommentDto);
    }

    @Operation(summary = "Derse comment yazmaq")
    @PostMapping(path = "{lessonId}/lesson-comment")
    public ResponseEntity<ResponseCommentDto> addCommentToLesson(@PathVariable String lessonId,
                                                                 @RequestBody CommentDto comment) {
        ResponseCommentDto responseCommentDto = commentServiceImpl.addCommentToLesson(lessonId, comment);
        return ResponseEntity.ok(responseCommentDto);
    }

    @Operation(summary = "Commenti yenilemek üçün")
    @PutMapping(path = "/{commentId}")
    public ResponseEntity<ResponseCommentDto> updateComment(@PathVariable String commentId,
                                                            @RequestBody CommentDto commentDto) {
        ResponseCommentDto responseCommentDto = commentServiceImpl.updateComment(commentId, commentDto);
        return ResponseEntity.ok(responseCommentDto);
    }

    @Operation(summary = "Kursun bütün kommnentleri")
    @GetMapping(path = "/{courseId}/course")
    public ResponseEntity<List<ResponseCommentDto>> getCourseComment(@PathVariable String courseId) {
        List<ResponseCommentDto> comments = commentServiceImpl.getCourseComment(courseId);
        return ResponseEntity.ok(comments);
    }

    @Operation(summary = "Dersin butun kommentleri")
    @GetMapping(path = "/{lessonId}/lesson")
    public ResponseEntity<List<ResponseCommentDto>> getLessonComment(@PathVariable String lessonId) {
        List<ResponseCommentDto> comments = commentServiceImpl.getLessonComment(lessonId);
        return ResponseEntity.ok(comments);
    }

    @Operation(summary = "Commenti silmek")
    @DeleteMapping(path = "/{commentId}")
    public ResponseEntity<String> deleteComment(@PathVariable String commentId) {
        commentServiceImpl.deleteComment(commentId);
        return ResponseEntity.ok("Comment deleted successfully! ");
    }

}
