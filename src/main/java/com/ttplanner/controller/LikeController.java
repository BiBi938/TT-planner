package com.ttplanner.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.springframework.beans.BeanWrapperImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.ttplanner.service.LikeNotAllowedException;
import com.ttplanner.service.LikeService;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j;

/**
 * 좋아요 Ajax 엔드포인트 (담당: 김민정, UC-021)
 *
 * POST /api/trips/{tripId}/like
 *   성공     200  {"liked": true/false, "count": n}
 *   비로그인  401  {"message": "로그인이 필요합니다.", "loginUrl": "/user/login"}
 *   비공개/삭제된 여행  403  {"message": "..."}
 *
 * LoginInterceptor가 아직 servlet-context.xml에 등록되지 않아 세션을 직접 검사한다.
 * TODO(윤아연): UserVO가 develop에 들어오면 loginUserId()를
 *              ((UserVO) session.getAttribute("loginUser")).getUserId()로 바꾼다.
 */
@Controller
@Log4j
@AllArgsConstructor
public class LikeController {

    private static final String LOGIN_USER = "loginUser";

    private LikeService service;

    @PostMapping("/api/trips/{tripId}/like")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggle(
            @PathVariable("tripId") Long tripId, HttpSession session) {
        log.info("toggle like: tripId=" + tripId);

        Long userId = loginUserId(session);
        if (userId == null) {
            Map<String, Object> body = new HashMap<String, Object>();
            body.put("message", "로그인이 필요합니다.");
            body.put("loginUrl", "/user/login");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
        }

        try {
            Map<String, Object> result = service.toggle(tripId, userId);
            return ResponseEntity.ok(result);
        } catch (LikeNotAllowedException e) {
            Map<String, Object> body = new HashMap<String, Object>();
            body.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
        }
    }

    private Long loginUserId(HttpSession session) {
        Object loginUser = session == null ? null : session.getAttribute(LOGIN_USER);
        if (loginUser == null) {
            return null;
        }
        Object userId = new BeanWrapperImpl(loginUser).getPropertyValue("userId");
        return userId == null ? null : ((Number) userId).longValue();
    }
}
