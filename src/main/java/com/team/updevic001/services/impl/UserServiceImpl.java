package com.team.updevic001.services.impl;

import com.team.updevic001.configuration.mappers.UserMapper;
import com.team.updevic001.dao.entities.User;
import com.team.updevic001.dao.entities.UserProfile;
import com.team.updevic001.dao.repositories.UserProfileRepository;
import com.team.updevic001.dao.repositories.UserRepository;
import com.team.updevic001.exceptions.ResourceNotFoundException;
import com.team.updevic001.model.dtos.request.UserProfileDto;
import com.team.updevic001.model.dtos.request.security.ChangePasswordDto;
import com.team.updevic001.model.dtos.response.user.ResponseUserDto;
import com.team.updevic001.model.dtos.response.user.ResponseUserProfileDto;
import com.team.updevic001.model.dtos.response.video.PhotoUploadResponse;
import com.team.updevic001.model.enums.Status;
import com.team.updevic001.services.interfaces.UserService;
import com.team.updevic001.utility.AuthHelper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final UserProfileRepository userProfileRepository;
    private final AuthHelper authHelper;
    private final PasswordEncoder passwordEncoder;
    private final VideoServiceImpl videoServiceImpl;

    @Override
    @Transactional
    public void updateUserProfileInfo(UserProfileDto userProfileDto) {
        User authenticatedUser = authHelper.getAuthenticatedUser();
        log.info("Updating user profile for ID: {}", authenticatedUser.getId());
        UserProfile userProfile = getUserProfile(authenticatedUser);
        modelMapper.map(userProfileDto, userProfile);
        userProfile.getUser().setPassword(passwordEncoder.encode(userProfileDto.getPassword()));
        userProfileRepository.save(userProfile);
        log.info("User with ID {} updated successfully.", authenticatedUser.getId());
    }

    @Override
    @Transactional
    public void updateUserPassword(ChangePasswordDto passwordDto) {
        User authenticatedUser = authHelper.getAuthenticatedUser();
        if (!passwordEncoder.matches(passwordDto.getCurrentPassword(), authenticatedUser.getPassword()) || !passwordDto.getNewPassword().equals(passwordDto.getRetryPassword())) {
            log.error("Old password incorrect or retry password mismatches");
            throw new IllegalArgumentException("Password incorrect!");
        }
        authenticatedUser.setPassword(passwordEncoder.encode(passwordDto.getNewPassword()));
        userRepository.save(authenticatedUser);
    }

    @Override
    public ResponseUserProfileDto getUserProfile() {
        User user = authHelper.getAuthenticatedUser();
        UserProfile userProfile = user.getUserProfile();
        return userMapper.toUserProfileDto(user.getFirstName(), user.getLastName(), userProfile);
    }

    @Override
    public ResponseUserDto getUserById(String uuid) {
        log.info("Searching for a user with this ID: {}", uuid);
        User user = userRepository.findById(uuid).orElseThrow(() -> {
            log.error("User not found with these ID: {}", uuid);
            return new ResourceNotFoundException("User not found Exception!");
        });

        return userMapper.toResponse(user, ResponseUserDto.class);
    }

    @Override
    public List<ResponseUserDto> getUser(String query) {
        List<User> users = userRepository.findByFirstNameContainingIgnoreCase(query);
        if (!users.isEmpty()) {
            log.info("Users matching this name!");
            return userMapper.toResponseList(users, ResponseUserDto.class);
        } else {
            throw new ResourceNotFoundException("There is no user with this name.");
        }
    }

    @Override
    public String uploadUserPhoto(MultipartFile multipartFile) throws IOException {
        User authenticatedUser = authHelper.getAuthenticatedUser();
        UserProfile userProfile = getUserProfile(authenticatedUser);
        PhotoUploadResponse photoUploadResponse = videoServiceImpl.uploadPhoto(multipartFile, authenticatedUser.getId());
        authenticatedUser.getUserProfile().setProfilePhotoKey(photoUploadResponse.getKey());
        authenticatedUser.getUserProfile().setProfilePhoto_url(photoUploadResponse.getUrl());

        userProfileRepository.save(userProfile);
        return photoUploadResponse.getUrl();
    }


    @Override
    @Transactional
    public void deleteUser() {
        User authenticatedUser = authHelper.getAuthenticatedUser();
        log.info("Attempting to delete user with ID: {}", authenticatedUser.getId());
        User user = findUserById(authenticatedUser.getId());
        user.setStatus(Status.INACTIVE);
        userRepository.save(user);
        log.info("User successfully deleted.");
    }


    public UserProfile getUserProfile(User user) {
        UserProfile userProfile = userProfileRepository.findByUser(user);
        if (userProfile == null) {
            log.warn("UserProfile not found for user ID: {}, creating new profile.", user.getId());
            userProfile = UserProfile.builder()
                    .user(user)
                    .build();
        }
        return userProfile;
    }

    public User findUserById(String id) {
        log.info("Finding user with ID: {}", id);
        return userRepository.findById(id).orElseThrow(() -> {
            log.error("User not found with ID: {}", id);
            return new ResourceNotFoundException("User not found");
        });
    }
}
