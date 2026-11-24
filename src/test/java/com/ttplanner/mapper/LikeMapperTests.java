package com.ttplanner.mapper;

import static org.junit.Assert.*;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import lombok.Setter;
import lombok.extern.log4j.Log4j;

/**
 * LikeMapper SQL 테스트 (담당: 김민정, UC-021)
 *
 * @Transactional: 각 테스트가 끝나면 롤백 → DB에 테스트 데이터가 남지 않음
 * 음수 ID(-901, -902) 사용: 기존 데이터와 PK 충돌 없음
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("file:src/main/webapp/WEB-INF/spring/root-context.xml")
@Transactional
@Log4j
public class LikeMapperTests {

    private static final long TEST_TRIP_PUBLIC  = -901L;
    private static final long TEST_TRIP_PRIVATE = -902L;
    private static final long TEST_USER_A       = -901L;

    @Setter(onMethod_ = @Autowired)
    private LikeMapper mapper;

    @Setter(onMethod_ = @Autowired)
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    public void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        jdbc.update(
            "insert into users (user_id, nickname, email, password) values (?, ?, ?, ?)",
            TEST_USER_A, "매퍼테스터A", "mapperA@test.com", "pw");
        jdbc.update(
            "insert into trip (trip_id, title, start_date, scope, share_token)"
            + " values (?, ?, sysdate, 'PUBLIC', ?)",
            TEST_TRIP_PUBLIC, "매퍼테스트공개여행", "mapper-public-token-901");
        jdbc.update(
            "insert into trip (trip_id, title, start_date, scope, share_token)"
            + " values (?, ?, sysdate, 'PRIVATE', ?)",
            TEST_TRIP_PRIVATE, "매퍼테스트비공개여행", "mapper-private-token-902");
    }

    @Test
    public void testCount() {
        assertEquals(0, mapper.count(TEST_TRIP_PUBLIC));
        mapper.insertLike(TEST_TRIP_PUBLIC, TEST_USER_A);
        assertEquals(1, mapper.count(TEST_TRIP_PUBLIC));
        log.info("count(넣은 후)=" + mapper.count(TEST_TRIP_PUBLIC));
    }

    @Test
    public void testIsLiked() {
        assertEquals(0, mapper.isLiked(TEST_TRIP_PUBLIC, TEST_USER_A));
        mapper.insertLike(TEST_TRIP_PUBLIC, TEST_USER_A);
        assertEquals(1, mapper.isLiked(TEST_TRIP_PUBLIC, TEST_USER_A));
        log.info("isLiked(넣은 후)=" + mapper.isLiked(TEST_TRIP_PUBLIC, TEST_USER_A));
    }

    @Test
    public void testInsertAndDeleteLike() {
        int inserted = mapper.insertLike(TEST_TRIP_PUBLIC, TEST_USER_A);
        assertEquals(1, inserted);

        int deleted = mapper.deleteLike(TEST_TRIP_PUBLIC, TEST_USER_A);
        assertEquals(1, deleted);

        // 없는 행을 지워도 오류 없음 (지운 행 수 0)
        int deletedAgain = mapper.deleteLike(TEST_TRIP_PUBLIC, TEST_USER_A);
        assertEquals(0, deletedAgain);
        log.info("inserted=" + inserted + ", deleted=" + deleted + ", deletedAgain=" + deletedAgain);
    }

    @Test
    public void testIsTripPublicAndActive() {
        assertEquals(1, mapper.isTripPublicAndActive(TEST_TRIP_PUBLIC));
        assertEquals(0, mapper.isTripPublicAndActive(TEST_TRIP_PRIVATE));
        log.info("PUBLIC=" + mapper.isTripPublicAndActive(TEST_TRIP_PUBLIC)
                + ", PRIVATE=" + mapper.isTripPublicAndActive(TEST_TRIP_PRIVATE));
    }
}
