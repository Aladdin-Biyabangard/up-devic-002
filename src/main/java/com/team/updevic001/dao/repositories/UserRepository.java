package com.team.updevic001.dao.repositories;

import com.team.updevic001.dao.entities.Course;
import com.team.updevic001.dao.entities.User;
import com.team.updevic001.model.enums.Role;
import com.team.updevic001.model.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    boolean existsByEmail(String email);


    List<User> findByFirstNameContainingIgnoreCase(String query);

    long count();

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :role")
    List<User> findUsersByRole(@Param("role") Role role);


    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndStatus(String email, Status status);

    List<User> findAllByWishlistContaining(Course course);

}
