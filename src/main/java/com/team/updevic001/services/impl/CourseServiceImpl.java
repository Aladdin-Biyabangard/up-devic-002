package com.team.updevic001.services.impl;

import com.team.updevic001.configuration.mappers.CourseMapper;
import com.team.updevic001.dao.entities.*;
import com.team.updevic001.dao.repositories.*;
import com.team.updevic001.exceptions.AlreadyExistsException;
import com.team.updevic001.exceptions.ForbiddenException;
import com.team.updevic001.exceptions.ResourceNotFoundException;
import com.team.updevic001.model.dtos.request.CourseDto;
import com.team.updevic001.model.dtos.response.course.ResponseCategoryDto;
import com.team.updevic001.model.dtos.response.course.ResponseCourseDto;
import com.team.updevic001.model.dtos.response.course.ResponseCourseShortInfoDto;
import com.team.updevic001.model.dtos.response.course.ResponseFullCourseDto;
import com.team.updevic001.model.dtos.response.video.PhotoUploadResponse;
import com.team.updevic001.model.enums.*;
import com.team.updevic001.services.interfaces.CourseService;
import com.team.updevic001.services.interfaces.TeacherService;
import com.team.updevic001.specification.CourseSpecification;
import com.team.updevic001.utility.AuthHelper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {


    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;
    private final TeacherRepository teacherRepository;
    private final TeacherService teacherService;
    private final ModelMapper modelMapper;
    private final TeacherCourseRepository teacherCourseRepository;
    private final VideoServiceImpl videoServiceImpl;
    private final CourseRatingRepository courseRatingRepository;
    private final AuthHelper authHelper;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ResponseCourseDto createCourse(CourseCategoryType courseCategoryType,
                                          CourseDto courseDto) {
        Teacher authenticatedTeacher = teacherService.getAuthenticatedTeacher();
        log.info("Operation of creating new course started by user with ID {}(whose teacher ID is {}", authenticatedTeacher.getUser().getId(), authenticatedTeacher.getId());
        Course course = modelMapper.map(courseDto, Course.class);
        course.setCourseCategoryType(courseCategoryType);
        course.setHeadTeacher(authenticatedTeacher);
        courseRepository.save(course);

        log.info("Course saved successfully. Course ID: {}", course.getId());

        TeacherCourse teacherCourse = TeacherCourse.builder()
                .teacher(authenticatedTeacher)
                .course(course)
                .teacherPrivilege(TeacherPrivileges.HEAD_TEACHER)
                .build();
        teacherCourseRepository.save(teacherCourse);
        log.info("TeacherCourse relationship saved successfully. Teacher ID: {}, Course ID: {}", authenticatedTeacher.getId(), course.getId());
        ResponseCourseDto responseCourseDto = courseMapper.courseDto(course);
        log.info("Operation of creating course ended successfully");
        return responseCourseDto;
    }


    @Override
    @Transactional
    public ResponseCourseDto addTeacherToCourse(String courseId, String userId) {
        Teacher authenticatedTeacher = teacherService.getAuthenticatedTeacher();
        log.info("Operation of adding new teacher with user ID {} to course with ID {} started by user with ID {}(whose teacher ID is {}", userId, courseId, authenticatedTeacher.getUser().getId(), authenticatedTeacher.getId());
        Course course = findCourseById(courseId);
        Teacher newTeacher = teacherRepository.findByUserId(userId).orElseThrow(() -> new ResourceNotFoundException(Teacher.class));

        TeacherCourse teacherCourse = validateAccess(courseId, authenticatedTeacher);

        if (!teacherCourse.getTeacherPrivilege().hasPermission(TeacherPermission.ADD_TEACHER)) {
            log.error("Teacher with ID {} doesn't have permission to add new teacher", authenticatedTeacher.getId());
            throw new ForbiddenException("NOT_ALLOWED");
        }

        if (course.getTeacherCourses().stream().anyMatch(teacherCourse1 -> teacherCourse1.getTeacher().getId().equals(newTeacher.getId()))) {
            log.info("Teacher with ID {} is already teacher in course with ID {}", newTeacher.getId(), courseId);
            throw new AlreadyExistsException("TEACHER_ALREADY_EXISTS_IN_THIS_COURSE");
        }

        TeacherCourse newTeacherCourseRelation = TeacherCourse.builder()
                .course(course)
                .teacher(newTeacher)
                .teacherPrivilege(TeacherPrivileges.ASSISTANT_TEACHER)
                .build();
        teacherCourseRepository.save(newTeacherCourseRelation);
        log.info("New teacher successfully added to course");
        return courseMapper.courseDto(course);
    }


    @Override
    @Transactional
    public ResponseCourseDto updateCourse(String courseId, CourseDto courseDto) {
        Teacher authenticatedTeacher = teacherService.getAuthenticatedTeacher();
        log.info("Operation of updating course with ID {} started by user with ID {}(whose teacher ID is {}", courseId, authenticatedTeacher.getUser().getId(), authenticatedTeacher.getId());
        Course findCourse = courseRepository
                .findById(courseId).orElseThrow(() -> new ResourceNotFoundException("Course not found!"));

        TeacherCourse teacherCourse = validateAccess(courseId, authenticatedTeacher);
        modelMapper.map(courseDto, findCourse);

        courseRepository.save(findCourse);
        teacherCourse.setCourse(findCourse);
        teacherCourseRepository.save(teacherCourse);

        log.info("Teacher course updated successfully. Course ID: {}", findCourse.getId());
        return courseMapper.courseDto(findCourse);
    }

    public String uploadCoursePhoto(String courseId, MultipartFile multipartFile) throws IOException {
        if (multipartFile != null && !multipartFile.isEmpty()) {
            Course course = findCourseById(courseId);
            PhotoUploadResponse photoUploadResponse = videoServiceImpl.uploadPhoto(multipartFile, courseId);
            course.setPhotoKey(photoUploadResponse.getKey());
            course.setPhoto_url(photoUploadResponse.getUrl());
            courseRepository.save(course);
            return photoUploadResponse.getUrl();
        }
        throw new IllegalArgumentException("Multipart file is empty or null!");
    }

    @Override
    public void deleteCourse(String courseId) {
        Teacher authenticatedTeacher = teacherService.getAuthenticatedTeacher();
        log.info("Operation of deleting course with ID {} started by teacher with ID {}(whose user ID is {}", courseId, authenticatedTeacher.getId(), authenticatedTeacher.getUser().getId());
        TeacherCourse teacherCourse = validateAccess(courseId, authenticatedTeacher);
        if (!teacherCourse.getTeacherPrivilege().hasPermission(TeacherPermission.DELETE_COURSE)) {
            log.error("Authenticated teacher with ID {}(whose user ID is {}) doesn't have permission to delete course with ID {}", authenticatedTeacher.getId(), authenticatedTeacher.getUser().getId(), courseId);
            throw new ForbiddenException("NOT_ALLOWED");
        }
        Course course = findCourseById(courseId);
        List<User> users = userRepository.findAllByWishlistContaining(course);
        if (!users.isEmpty()) {
            users.forEach(user -> user.getWishlist().remove(course));
            userRepository.saveAll(users);
        }
        courseRepository.deleteById(courseId);
    }

    public TeacherCourse validateAccess(String courseId, Teacher authenticatedTeacher) {
        return teacherCourseRepository.findByCourseIdAndTeacher(courseId, authenticatedTeacher).orElseThrow(() -> {
            log.error("TeacherCourse relation is not present: That means teacher with ID {} is not teacher in course with ID {} or curse doesn't exit", authenticatedTeacher.getId(), courseId);
            return new ForbiddenException("NOT_ALLOWED");
        });
    }

    @Override
    @Cacheable(
            value = "courseSearchCache",
            key = "T(java.util.Objects).hash(#level, #minPrice, #maxPrice, #courseCategoryType)",
            unless = "#result==null",
            cacheManager = "cacheManager"
    )
    public List<ResponseCourseShortInfoDto> findCourseByCriteria(CourseLevel level, BigDecimal minPrice, BigDecimal maxPrice, CourseCategoryType courseCategoryType) {
        Specification<Course> specification = Specification.where(
                        CourseSpecification.hasLevel(level))
                .and(CourseSpecification.priceGreaterThanOrEqual(minPrice))
                .and(CourseSpecification.priceLessThanOrEqual(maxPrice))
                .and(CourseSpecification.hasCategory(courseCategoryType));
        List<Course> courses = courseRepository.findAll(specification);

        return courseMapper.toCourseResponse(courses);
    }


    @Override
    @Cacheable(value = "courseSearchCache", key = "#keyword", unless = "#result == null", cacheManager = "cacheManager")
    public List<ResponseCourseShortInfoDto> searchCourse(String keyword) {
        List<Course> courses = courseRepository.searchCoursesByKeyword(keyword);
        return !courses.isEmpty() ? courseMapper.toCourseResponse(courses) : List.of();
    }

    @Override
    @Cacheable(value = "courseSearchCache", key = "#courseId", unless = "#result==null", cacheManager = "cacheManager")
    public ResponseFullCourseDto getCourse(String courseId) {
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        return courseMapper.toFullResponse(course);
    }

    @Override
    @Cacheable(value = "courseSearchCache", unless = "#result.isEmpty()", cacheManager = "cacheManager")
    public List<ResponseCourseShortInfoDto> getCourses() {
        List<Course> courses = courseRepository.findAllByOrderByRatingDescCreatedAtDesc();
        return !courses.isEmpty() ? courseMapper.toCourseResponse(courses) : List.of();
    }

    @Override
    public void addToWishList(String courseId) {
        User authenticatedUser = authHelper.getAuthenticatedUser();
        Course course = findCourseById(courseId);
        if (authenticatedUser.getWishlist().isEmpty()) {
            authenticatedUser.setWishlist(new ArrayList<>());
        }
        authenticatedUser.getWishlist().add(course);
        userRepository.save(authenticatedUser);
    }

    @Override
    public List<ResponseCourseShortInfoDto> getWishList() {
        User authenticatedUser = authHelper.getAuthenticatedUser();
        if (!authenticatedUser.getWishlist().isEmpty()) {
            List<Course> wishlist = authenticatedUser.getWishlist();
            return courseMapper.toCourseResponse(wishlist);
        }
        return new ArrayList<>();
    }

    @Override
    public List<ResponseCategoryDto> getCategories() {
        return Arrays.stream(CourseCategoryType.values())
                .map(type -> {
                    List<Course> courses = courseRepository.findCourseByCourseCategoryType(type);
                    return new ResponseCategoryDto(type, courses.size());
                })
                .toList();
    }

    @Override
    public List<ResponseCourseShortInfoDto> findCoursesByCategory(CourseCategoryType categoryType) {
        List<Course> courseByCourseCategoryType = courseRepository.findCourseByCourseCategoryType(categoryType);
        return !courseByCourseCategoryType.isEmpty() ? courseMapper.toCourseResponse(courseByCourseCategoryType) : new ArrayList<>();
    }

    @Override
    public void updateRatingCourse(String courseId, int rating) {
        User user = authHelper.getAuthenticatedUser();
        Course course = findCourseById(courseId);
        CourseRating courseRating = courseRatingRepository.findCourseRatingByCourseAndUser(course, user)
                .orElseGet(() ->
                        CourseRating.builder()
                                .course(course)
                                .user(user)
                                .build());
        courseRating.setRating(rating);
        courseRatingRepository.save(courseRating);
        course.setRating(getAverageRating(course));
        courseRepository.save(course);
    }

    @Override
    public List<ResponseCourseShortInfoDto> getMost5PopularCourses() {
        List<Course> top5ByOrderByRatingDesc = courseRepository.findTop5ByOrderByRatingDesc();
        return courseMapper.toCourseResponse(top5ByOrderByRatingDesc);
    }

    @Override
    @Cacheable(
            value = "courseSortCache",
            key = "'sort:' + #sortBy.name() + ':dir:' + #direction.name()",
            unless = "#result == null or #result.isEmpty()",
            cacheManager = "cacheManager"
    )
    public List<ResponseCourseShortInfoDto> filterAndSortCourses(SortType sortBy, SortDirection direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.name()); // ASC or DESC
        Sort sort = Sort.by(sortDirection, sortBy.name());
        List<Course> courses = courseRepository.findAll(sort);
        return courses.isEmpty() ? List.of() : courseMapper.toCourseResponse(courses);
    }

    public Course findCourseById(String courseId) {
        log.debug("Fetching course with ID: {}", courseId);
        return courseRepository.findById(courseId)
                .orElseThrow(() -> {
                    log.error("Course not found with ID: {}", courseId);
                    return new ResourceNotFoundException("COURSE_NOT_FOUND");
                });
    }

    @Scheduled(fixedRate = 300000) // 5 dəqiqə (300000 ms)
    @CacheEvict(value = "courseSearchCache", allEntries = true)
    public void clearCache() {
        System.out.println("Cache təmizləndi.");
    }

    @Override
    public void removeFromWishList(String courseId) {
        User authenticatedUser = authHelper.getAuthenticatedUser();
        Course course = findCourseById(courseId);
        authenticatedUser.getWishlist().remove(course);
    }

    double getAverageRating(Course course) {
        List<CourseRating> ratings = courseRatingRepository.findByCourse(course);
        return ratings.stream()
                .mapToInt(CourseRating::getRating)
                .average()
                .orElse(0.0);
    }

}
