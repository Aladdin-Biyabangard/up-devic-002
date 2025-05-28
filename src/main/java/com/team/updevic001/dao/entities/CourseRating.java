package com.team.updevic001.dao.entities;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "course_rating")
public class CourseRating {

    @Id
    @Column(unique = true, nullable = false, length = 12)
    private String id;

    @ManyToOne
    private User user;

    @ManyToOne(cascade = CascadeType.ALL)
    private Course course;

    private int rating;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @PrePersist
    public void generatedId() {
        if (this.id == null) {
            this.id = NanoIdUtils.randomNanoId().substring(0, 12);
        }
    }
}
