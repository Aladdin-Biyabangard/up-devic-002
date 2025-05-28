package com.team.updevic001.services.interfaces;

import com.team.updevic001.model.dtos.request.CourseDto;
import com.team.updevic001.model.dtos.response.course.ResponseCategoryDto;
import com.team.updevic001.model.dtos.response.course.ResponseCourseDto;
import com.team.updevic001.model.dtos.response.course.ResponseCourseShortInfoDto;
import com.team.updevic001.model.dtos.response.course.ResponseFullCourseDto;
import com.team.updevic001.model.enums.CourseCategoryType;
import com.team.updevic001.model.enums.CourseLevel;
import com.team.updevic001.model.enums.SortDirection;
import com.team.updevic001.model.enums.SortType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

public interface CourseService {

    ResponseCourseDto createCourse(CourseCategoryType courseCategoryType, CourseDto courseDto);

    ResponseCourseDto addTeacherToCourse(String courseId, String userId);

    ResponseCourseDto updateCourse(String courseId, CourseDto courseDto);

    String uploadCoursePhoto(String courseId, MultipartFile multipartFile) throws IOException;

    void updateRatingCourse(String courseId, int rating);

    void addToWishList(String courseId);

    List<ResponseCourseShortInfoDto> getWishList();

    ResponseFullCourseDto getCourse(String courseId);

    List<ResponseCourseShortInfoDto> getCourses();

    List<ResponseCategoryDto> getCategories();

    List<ResponseCourseShortInfoDto> getMost5PopularCourses();

    List<ResponseCourseShortInfoDto> findCoursesByCategory(CourseCategoryType categoryType);

    List<ResponseCourseShortInfoDto> findCourseByCriteria(CourseLevel level,
                                                          BigDecimal minPrice,
                                                          BigDecimal maxPrice,
                                                          CourseCategoryType courseCategoryType);

    List<ResponseCourseShortInfoDto> searchCourse(String keyword);

    List<ResponseCourseShortInfoDto> filterAndSortCourses(SortType sortBy, SortDirection direction);

    void removeFromWishList(String courseId);

    void deleteCourse(String courseId);


}
