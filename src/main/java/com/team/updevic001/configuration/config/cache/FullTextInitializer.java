package com.team.updevic001.configuration.config.cache;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FullTextInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initFullTextIndex() {
        String sql = "ALTER TABLE courses ADD FULLTEXT(title, description)";
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            System.out.println("FullText index already exists or error occurred: " + e.getMessage());
        }
    }
}
