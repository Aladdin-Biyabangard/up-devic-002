package com.team.updevic001.services.impl;

import com.team.updevic001.configuration.mappers.LessonMapper;
import com.team.updevic001.dao.entities.*;
import com.team.updevic001.dao.repositories.LessonRepository;
import com.team.updevic001.dao.repositories.TeacherCourseRepository;
import com.team.updevic001.dao.repositories.UserCourseFeeRepository;
import com.team.updevic001.dao.repositories.UserLessonStatusRepository;
import com.team.updevic001.exceptions.ForbiddenException;
import com.team.updevic001.exceptions.PaymentStatusException;
import com.team.updevic001.exceptions.ResourceNotFoundException;
import com.team.updevic001.model.dtos.request.LessonDto;
import com.team.updevic001.model.dtos.response.lesson.ResponseLessonDto;
import com.team.updevic001.model.dtos.response.lesson.ResponseLessonShortInfoDto;
import com.team.updevic001.model.dtos.response.video.LessonVideoResponse;
import com.team.updevic001.model.dtos.response.video.PhotoUploadResponse;
import com.team.updevic001.model.enums.TeacherPermission;
import com.team.updevic001.services.interfaces.LessonService;
import com.team.updevic001.utility.AuthHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class LessonServiceImpl implements LessonService {
    private final LessonRepository lessonRepository;
    private final TeacherServiceImpl teacherServiceImpl;
    private final ModelMapper modelMapper;
    private final CourseServiceImpl courseServiceImpl;
    private final VideoServiceImpl videoServiceImpl;
    private final LessonMapper lessonMapper;
    private final TeacherCourseRepository teacherCourseRepository;
    private final UserCourseFeeRepository userCourseFeeRepository;
    private final AuthHelper authHelper;
    private final UserLessonStatusRepository userLessonStatusRepository;


    @Override
    @Transactional
    public ResponseLessonDto assignLessonToCourse(String courseId, LessonDto lessonDto, MultipartFile multipartFile) throws Exception {
        Teacher authenticatedTeacher = teacherServiceImpl.getAuthenticatedTeacher();
        log.info("Assigning lesson to course. Teacher ID: {},User ID {}, Course ID: {}", authenticatedTeacher.getId(), authenticatedTeacher.getUser().getId(), courseId);

        Lesson lesson = modelMapper.map(lessonDto, Lesson.class);

        Course course = courseServiceImpl.findCourseById(courseId);

        TeacherCourse teacherCourse = courseServiceImpl.validateAccess(courseId, authenticatedTeacher);

        if (!teacherCourse.getTeacherPrivilege().hasPermission(TeacherPermission.ADD_LESSON)) {
            log.error("Failed to add new lesson to course with ID {}: Teacher with ID {} doesn't have permission to add lesson", courseId, authenticatedTeacher.getId());
            throw new ForbiddenException("NOT_ALLOWED");
        }


        if (multipartFile != null && !multipartFile.isEmpty()) {
            lesson.setCourse(course);
            course.getLessons().add(lesson);
            lessonRepository.save(lesson);
            String videoUrl = videoServiceImpl.uploadVideo(multipartFile, lesson.getId());
            File file = videoServiceImpl.convertToFile(multipartFile);
            String videoDurationInSeconds = videoServiceImpl.getVideoDurationInSeconds(file);
            lesson.setDuration(videoDurationInSeconds);
            lesson.setVideoUrl(videoUrl);
            lesson.setTeacher(authenticatedTeacher);
            lessonRepository.save(lesson);
        }


        log.info("Lesson assigned successfully. Lesson ID: {}", lesson.getId());
        return modelMapper.map(lesson, ResponseLessonDto.class);
    }

    @Override
    @Transactional
    public ResponseLessonDto updateLessonInfo(String lessonId, LessonDto lessonDto) {
        Teacher authenticatedTeacher = teacherServiceImpl.getAuthenticatedTeacher();
        log.info("Operation of updating lesson with ID {} started by teacher with ID {}(whose user ID is {})", lessonId, authenticatedTeacher.getId(), authenticatedTeacher.getUser().getId());
        Lesson lesson = findLessonById(lessonId);
        if (!lesson.getTeacher().getId().equals(authenticatedTeacher.getId())) {
            log.error("Teacher with ID {}(whose user ID is {}) is not allowed to update the lesson with ID {}", authenticatedTeacher.getId(), authenticatedTeacher.getUser().getId(), lessonId);
            throw new ForbiddenException("NOT_ALLOWED_UPDATE_LESSON");
        }
        modelMapper.map(lessonDto, lesson);
        ResponseLessonDto updatedLesson = modelMapper.map(lesson, ResponseLessonDto.class);
        log.info("Lesson info successfully updated");
        return updatedLesson;
    }

    @Override
    public String uploadLessonPhoto(String lessonId, MultipartFile multipartFile) throws IOException {
        if (multipartFile != null && !multipartFile.isEmpty()) {
            Lesson lesson = findLessonById(lessonId);
            PhotoUploadResponse photoUploadResponse = videoServiceImpl.uploadPhoto(multipartFile, lessonId);
            lesson.setPhotoKey(photoUploadResponse.getKey());
            lesson.setPhoto_url(photoUploadResponse.getUrl());
            lessonRepository.save(lesson);
            return photoUploadResponse.getUrl();
        }
        throw new IllegalArgumentException("Multipart file is empty or null!");
    }

    @Override
    public List<ResponseLessonShortInfoDto> getShortLessonsByCourse(String courseId) {
        log.info("Getting lessons of course.Course ID: {}", courseId);
        List<Lesson> lessons = lessonRepository.findLessonByCourseId(courseId);
        return lessons.isEmpty() ? List.of() : lessonMapper.toShortLesson(lessons);
    }

    public ResponseLessonDto getFullLessonByLessonId(String lessonId) {
        User authenticatedUser = authHelper.getAuthenticatedUser();
        Lesson lesson = findLessonById(lessonId);
        boolean exists = userCourseFeeRepository.existsUserCourseFeeByCourseAndUser(lesson.getCourse(), authenticatedUser);
        if (exists) {
            markLessonAsWatched(authenticatedUser, lesson);
            return lessonMapper.toDto(lesson);
        } else {
            throw new PaymentStatusException("The user has not paid for the course. ");
        }
    }

    @Override
    public List<ResponseLessonDto> getTeacherLessons() {
        Teacher authenticatedTeacher = teacherServiceImpl.getAuthenticatedTeacher();
        log.info("Getting teacher lessons. Teacher ID: {}", authenticatedTeacher.getId());

        List<TeacherCourse> teacherCourses = teacherCourseRepository.findTeacherCourseByTeacher(authenticatedTeacher);
        List<ResponseLessonDto> lessons = teacherCourses.stream()
                .flatMap(teacherCourse -> teacherCourse.getCourse().getLessons().stream()).map(lessonMapper::toDto).toList();

        log.info("Retrieved {} lessons for teacher ID: {}", lessons.size(), authenticatedTeacher.getId());
        return lessons;
    }

    //TODO burda response iki ferqli clasdir. Buna nezerat e

    @Override
    public LessonVideoResponse getVideo(String lessonId) {
        Lesson lesson = findLessonById(lessonId);
        return new LessonVideoResponse(
                lesson.getTitle(),
                lesson.getDescription(),
                lesson.getVideoUrl(),
                lesson.getDuration()
        );
    }


    @Override
    public Lesson findLessonById(String lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found these Id"));
    }

    @Override
    @Transactional
    public void deleteLesson(String lessonId) {
        Teacher authenticatedTeacher = teacherServiceImpl.getAuthenticatedTeacher();
        log.info("Operation of deleting lesson with ID {} started by teacher with ID {}(whose user ID is {})", lessonId, authenticatedTeacher.getId(), authenticatedTeacher.getUser().getId());
        Lesson lesson = findLessonById(lessonId);
        TeacherCourse teacherCourse = courseServiceImpl.validateAccess(lesson.getCourse().getId(), authenticatedTeacher);
        if (!teacherCourse.getTeacherPrivilege().hasPermission(TeacherPermission.DELETE_LESSON) || !lesson.getTeacher().getId().equals(authenticatedTeacher.getId())) {
            log.error("Teacher with ID {}(whose user ID is {}) is not allowed to delete the lesson with ID {}", authenticatedTeacher.getId(), authenticatedTeacher.getUser().getId(), lessonId);
            throw new ForbiddenException("NOT_ALLOWED_DELETE_LESSON");
        }
        videoServiceImpl.deleteVideoFromS3(lesson.getId());
        lessonRepository.delete(lesson);
        log.info("Lesson successfully deleted");
    }

    @Override
    @Transactional
    public void deleteTeacherLessons() {
        Teacher authenticatedTeacher = teacherServiceImpl.getAuthenticatedTeacher();
        log.info("Deleting all teacher lessons. Teacher ID: {}", authenticatedTeacher.getId());

        List<TeacherCourse> teacherCourses = teacherCourseRepository.findTeacherCourseByTeacher(authenticatedTeacher);
        List<Lesson> lessons = teacherCourses.stream().flatMap(teacherCourse -> teacherCourse.getCourse().getLessons().stream()).toList();
        videoServiceImpl.deleteVideosFromS3(lessons);
        lessonRepository.deleteAll(lessons);

        log.info("All teacher lessons deleted successfully. Teacher ID: {}", authenticatedTeacher.getId());
    }

    public void markLessonAsWatched(User user, Lesson lesson) {
        UserLessonStatus userLessonStatus = new UserLessonStatus();
        userLessonStatus.setUser(user);
        userLessonStatus.setLesson(lesson);
        userLessonStatus.setWatched(true);
        userLessonStatusRepository.save(userLessonStatus);
    }

}


//    @Override
//    public LessonVideoResponse getVideo(String lessonId, String videoName) throws MalformedURLException {
//        String filePath = VIDEO_DIRECTORY + videoName;
//        File videoFile = new File(filePath);
//
//        Lesson lesson = findLessonById(lessonId);
//
//        if (!lesson.getVideoUrl().equalsIgnoreCase(filePath)) {
//            throw new IllegalArgumentException("There is no video in the lesson.");
//        }
//        if (!videoFile.exists()) {
//            log.warn("Requested a non-existing video: {}", videoName);
//            throw new ResourceNotFoundException("VIDEO_NOT_FOUND");
//        }
//
//        Path path = Paths.get(videoFile.getAbsolutePath());
//        log.info("Video loaded successfully: {}", path);
//
//        Resource videoResource = new UrlResource(path.toUri());
//
//        return new LessonVideoResponse(
//                lesson.getTitle(),
//                lesson.getDescription(),
//                videoResource
//        );
//    }