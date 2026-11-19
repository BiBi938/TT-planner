package com.ttplanner.controller;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Setter;
import lombok.extern.log4j.Log4j;

/**
 * LikeController MockMvc 테스트 (담당: 김민정, UC-021)
 *
 * 커버하는 시나리오:
 *   2B: 비로그인으로 POST /api/trips/{번호}/like → 401 + JSON 응답 확인
 *
 * 세션 없이 요청하면 LikeController가 세션 loginUser 를 직접 검사해 401 을 반환한다.
 * tripId 는 세션 검사가 먼저이므로 DB에 없는 번호여도 무방하다.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration({
    "file:src/main/webapp/WEB-INF/spring/root-context.xml",
    "file:src/main/webapp/WEB-INF/spring/appServlet/servlet-context.xml"
})
@Log4j
public class LikeControllerTests {

    @Setter(onMethod_ = @Autowired)
    private WebApplicationContext ctx;

    private MockMvc mockMvc;

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(ctx).build();
    }

    /** 2B: 비로그인 POST → 401 + {"message": "로그인이 필요합니다.", "loginUrl": "/user/login"} */
    @Test
    public void test비로그인401() throws Exception {
        String responseBody = mockMvc
            .perform(post("/api/trips/1/like")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized())
            .andReturn().getResponse().getContentAsString();

        log.info("비로그인 응답: " + responseBody);

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> body = objectMapper.readValue(
                responseBody, new TypeReference<Map<String, Object>>() {});
        assertEquals("로그인이 필요합니다.", body.get("message"));
        assertEquals("/user/login", body.get("loginUrl"));
    }
}
