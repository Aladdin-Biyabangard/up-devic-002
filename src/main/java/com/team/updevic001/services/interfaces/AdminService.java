
package com.team.updevic001.services.interfaces;

import com.team.updevic001.model.dtos.response.user.ResponseUserDto;
import com.team.updevic001.model.enums.Role;

import java.util.List;

public interface AdminService {

    void assignTeacherProfile(String studentId);

    List<ResponseUserDto> getAllUsers();

    void assignRoleToUser(String userId, Role role);

    void removeRoleFromUser(String userId, Role role);

    List<ResponseUserDto> getUsersByRole(Role role);

    void activateUser(String id);

    void permanentlyDeleteUser(String userId);

    void deactivateUser(String id);

    Long countUsers();

    void deleteUsers();


}
