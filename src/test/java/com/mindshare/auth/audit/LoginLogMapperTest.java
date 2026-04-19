package com.mindshare.auth.audit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LoginLogMapperTest {

    @Autowired
    private LoginLogMapper loginLogMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldInsertLoginLog() {
        jdbcTemplate.update("""
                INSERT INTO users (id, phone, email, password_hash, nickname, create_time, update_time)
                VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP())
                """, 2001L, "13700137000", "log@example.com", "hash", "Logger");

        LoginLog log = new LoginLog();
        log.setUserId(2001L);
        log.setIdentifier("log@example.com");
        log.setChannel("password");
        log.setIp("127.0.0.1");
        log.setUserAgent("JUnit");
        log.setResult("SUCCESS");
        log.setCreateTime(Instant.parse("2026-04-19T00:00:00Z"));

        loginLogMapper.insert(log);

        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM login_logs WHERE id = ?", log.getId());
        assertThat(row.get("identifier")).isEqualTo("log@example.com");
        assertThat(row.get("channel")).isEqualTo("password");
        assertThat(row.get("result")).isEqualTo("SUCCESS");
    }
}
