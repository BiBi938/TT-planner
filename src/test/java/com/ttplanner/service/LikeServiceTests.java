package com.ttplanner.service;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.ttplanner.vo.PublicTripCriteria;
import com.ttplanner.vo.PublicTripVO;

import lombok.Setter;
import lombok.extern.log4j.Log4j;

/**
 * LikeService 시나리오 테스트 (담당: 김민정, UC-021)
 *
 * @Transactional: 각 테스트가 끝나면 롤백 → DB에 테스트 데이터가 남지 않음
 * 음수 ID(-801, -802) 사용: 기존 데이터와 PK 충돌 없음
 *
 * 커버하는 시나리오:
 *   기본 흐름, 2A(취소), 1A(비공개 → 예외), 1회 제한(2명), isLiked(null),
 *   시나리오 4(PublicTripService.search likeCount 반영)
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("file:src/main/webapp/WEB-INF/spring/root-context.xml")
@Transactional
@Log4j
public class LikeServiceTests {

    private static final long TEST_TRIP_PUBLIC  = -801L;
    private static final long TEST_TRIP_PRIVATE = -802L;
    private static final long TEST_USER_A       = -801L;
    private static final long TEST_USER_B       = -802L;

    @Setter(onMethod_ = @Autowired)
    private LikeService service;

    @Setter(onMethod_ = @Autowired)
    private PublicTripService publicTripService;

    @Setter(onMethod_ = @Autowired)
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    public void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        jdbc.update(
            "insert into users (user_id, nickname, email, password) values (?, ?, ?, ?)",
            TEST_USER_A, "서비스테스터A", "svcA@test.com", "pw");
        jdbc.update(
            "insert into users (user_id, nickname, email, password) values (?, ?, ?, ?)",
            TEST_USER_B, "서비스테스터B", "svcB@test.com", "pw");
        jdbc.update(
            "insert into trip (trip_id, title, start_date, scope, share_token)"
            + " values (?, ?, sysdate, 'PUBLIC', ?)",
            TEST_TRIP_PUBLIC, "서비스테스트공개여행", "svc-public-token-801");
        jdbc.update(
            "insert into trip (trip_id, title, start_date, scope, share_token)"
            + " values (?, ?, sysdate, 'PRIVATE', ?)",
            TEST_TRIP_PRIVATE, "서비스테스트비공개여행", "svc-private-token-802");
    }

    /** 기본 흐름: 누르면 liked=true, 좋아요 수+1, isLiked=true */
    @Test
    public void test기본흐름() {
        Map<String, Object> result = service.toggle(TEST_TRIP_PUBLIC, TEST_USER_A);
        log.info("기본흐름 결과: " + result);

        assertTrue((Boolean) result.get("liked"));
        assertEquals(1, result.get("count"));
        assertTrue(service.isLiked(TEST_TRIP_PUBLIC, TEST_USER_A));
    }

    /** 2A: 같은 회원이 한 번 더 누르면 취소 — liked=false, 수 원복 */
    @Test
    public void test좋아요취소() {
        service.toggle(TEST_TRIP_PUBLIC, TEST_USER_A); // 1회차: 좋아요
        Map<String, Object> result = service.toggle(TEST_TRIP_PUBLIC, TEST_USER_A); // 2회차: 취소
        log.info("취소 결과: " + result);

        assertFalse((Boolean) result.get("liked"));
        assertEquals(0, result.get("count"));
        assertFalse(service.isLiked(TEST_TRIP_PUBLIC, TEST_USER_A));
    }

    /** 1A: 비공개 여행에 toggle → LikeNotAllowedException */
    @Test(expected = LikeNotAllowedException.class)
    public void test비공개여행() {
        service.toggle(TEST_TRIP_PRIVATE, TEST_USER_A);
    }

    /** 1회 제한: 다른 회원 2명이 각각 누르면 count=2, 각각 isLiked=true */
    @Test
    public void test두회원좋아요() {
        service.toggle(TEST_TRIP_PUBLIC, TEST_USER_A);
        Map<String, Object> result = service.toggle(TEST_TRIP_PUBLIC, TEST_USER_B);
        log.info("두 회원 결과: " + result);

        assertEquals(2, result.get("count"));
        assertTrue(service.isLiked(TEST_TRIP_PUBLIC, TEST_USER_A));
        assertTrue(service.isLiked(TEST_TRIP_PUBLIC, TEST_USER_B));
    }

    /** UC-021 2B 대비: isLiked(tripId, null) → DB 조회 없이 false */
    @Test
    public void test비로그인isLiked() {
        assertFalse(service.isLiked(TEST_TRIP_PUBLIC, null));
    }

    /**
     * 시나리오 4: 좋아요 후 PublicTripService.search 결과의 likeCount에 반영
     * PublicTripMapper.xml의 search SQL이 count(*) from trip_like 로 집계하므로
     * toggle 직후 조회하면 likeCount=1 이어야 한다.
     */
    @Test
    public void test검색목록likeCount반영() {
        service.toggle(TEST_TRIP_PUBLIC, TEST_USER_A);

        PublicTripCriteria cri = new PublicTripCriteria();
        cri.setKeyword("서비스테스트공개여행"); // 테스트 여행 제목으로 검색
        List<PublicTripVO> list = publicTripService.search(cri);
        log.info("검색 결과: " + list);

        assertFalse("검색 결과가 1건 이상이어야 한다", list.isEmpty());
        assertEquals(1, list.get(0).getLikeCount());
    }
}
