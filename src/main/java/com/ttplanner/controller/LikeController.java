package com.ttplanner.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.ttplanner.interceptor.LoginInterceptor;
import com.ttplanner.service.LikeNotAllowedException;
import com.ttplanner.service.LikeService;
import com.ttplanner.vo.UserVO;

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
 * LoginInterceptor(servlet-context.xml)가 먼저 같은 형식의 401을 돌려주지만,
 * 등록 여부와 관계없이 동작하도록 세션을 직접 한 번 더 검사한다.
 */
@Controller
@Log4j
@AllArgsConstructor
public class LikeController {

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
            boolean liked = service.toggle(tripId, userId);
            int count = service.count(tripId);
            Map<String, Object> result = new HashMap<String, Object>();
            result.put("liked", liked);
            result.put("count", count);
            return ResponseEntity.ok(result);
        } catch (LikeNotAllowedException e) {
            Map<String, Object> body = new HashMap<String, Object>();
            body.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
        }
    }

    private Long loginUserId(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object loginUser = session.getAttribute(LoginInterceptor.LOGIN_USER);
        if (loginUser instanceof UserVO) {
            return ((UserVO) loginUser).getUserId();
        }
        return null;
    }
}
