package com.team.updevic001.controllers;

import com.team.updevic001.configuration.config.syncrn.RateLimit;
import com.team.updevic001.model.dtos.request.UserProfileDto;
import com.team.updevic001.model.dtos.request.security.ChangePasswordDto;
import com.team.updevic001.model.dtos.response.user.ResponseUserDto;
import com.team.updevic001.model.dtos.response.user.ResponseUserProfileDto;
import com.team.updevic001.services.interfaces.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PutMapping("/profile")
    public ResponseEntity<String> updateUserProfileInfo(@RequestBody UserProfileDto userProfileDto) {
        userService.updateUserProfileInfo(userProfileDto);
        return ResponseEntity.ok("User profile updated!");
    }

    @PatchMapping("/password")
    public ResponseEntity<String> updateUserPassword(@Valid @RequestBody ChangePasswordDto passwordDto) {
        userService.updateUserPassword(passwordDto);
        return ResponseEntity.ok("User password updated");
    }

    @PatchMapping(path = "-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadUserPhoto(@RequestPart MultipartFile multipartFile) throws IOException {
        String photo = userService.uploadUserPhoto(multipartFile);
        return ResponseEntity.ok(photo);
    }

    @GetMapping(path = "-profile")
    @Operation(summary = "Profili göstərmək üçün")
    public ResponseEntity<ResponseUserProfileDto> getUserProfile() {
        return ResponseEntity.ok(userService.getUserProfile());
    }


    @GetMapping("/{uuid}")
    public ResponseEntity<ResponseUserDto> getUserById(@PathVariable String uuid) {
        ResponseUserDto userById = userService.getUserById(uuid);
        return new ResponseEntity<>(userById, HttpStatus.OK);
    }

    @RateLimit
    @GetMapping("/search")
    public ResponseEntity<List<ResponseUserDto>> searchUsers(@RequestParam String query) {
        List<ResponseUserDto> user = userService.getUser(query);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @DeleteMapping
    public ResponseEntity<String> deleteUser() {
        userService.deleteUser();
        return ResponseEntity.ok("Delete all users!");
    }


}
