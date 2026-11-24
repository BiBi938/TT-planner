package com.ttplanner.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ttplanner.interceptor.LoginInterceptor;
import com.ttplanner.service.MemberService;
import com.ttplanner.vo.MemberVO;
import com.ttplanner.vo.UserVO;

import lombok.Setter;
import lombok.extern.log4j.Log4j;

/**
 * 멤버 초대 요청 (UC-017) · 담당: 양지호
 *
 * GET  /api/trips/{tripId}/members/search?nickname=이   (Ajax, 초대 창의 회원 검색)
 *   성공 (0명 포함)        200  {"users": [{"userId": 40, "nickname": "요청한이", ...}, ...]}
 *   OWNER 아님 · 빈 검색어  400  {"message": "..."}
 *   로그인 안 함            401  LoginInterceptor가 먼저 보냄 (/api/ 주소라 JSON)
 *
 * POST /trips/{tripId}/members   (폼 제출, 초대 창의 [초대])
 *   결과 문구를 flash "memberMsg"로 담아 여행 상세(/trips/{tripId}, 박준성)로 이동 → trip/_members.jsp에서 출력
 *   ("msg"는 _days.jsp가 쓰므로 이름을 나눔)
 *
 * 권한 검사(OWNER만)는 MemberService가 한다.
 */
@Controller
@Log4j
public class MemberController {

	/** 로그인이 필요할 때 보내는 주소 (로그인 화면에 "로그인이 필요합니다." 표시) */
	private static final String LOGIN_REQUIRED = "redirect:/user/login?required";

	@Setter(onMethod_ = @Autowired)
	private MemberService service;

	// 초대할 회원 검색 (Ajax): GET /api/trips/1/members/search?nickname=이
	@GetMapping("/api/trips/{tripId}/members/search")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> search(@PathVariable("tripId") Long tripId,
			@RequestParam(value = "nickname", required = false) String nickname, HttpSession session) {
		log.info("search members......" + tripId + ", nickname=" + nickname);

		Map<String, Object> body = new HashMap<String, Object>();
		try {
			// 로그인 안 했으면 userId가 null → Service에서 권한 없음으로 막힘
			List<MemberVO> users = service.searchUsers(tripId, nickname, loginUserId(session));
			body.put("users", users);
			return ResponseEntity.ok(body); // 200 (0명이면 빈 목록 → 화면에서 5A 문구)
		} catch (IllegalStateException e) {
			// 권한 없음, 빈 검색어 → 400 + 문구 (화면 JS가 문구를 그대로 보여 줌)
			body.put("message", e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
		}
	}

	// 초대 보내기 (폼 제출): POST /trips/1/members  (targetUserId=50, role=EDITOR)
	@PostMapping("/trips/{tripId}/members")
	public String invite(@PathVariable("tripId") Long tripId,
			@RequestParam(value = "targetUserId", required = false) Long targetUserId,
			@RequestParam(value = "role", required = false) String role,
			HttpSession session, RedirectAttributes rttr) {
		log.info("invite......" + tripId + ", target=" + targetUserId + ", role=" + role);

		// LoginInterceptor가 먼저 막지만, 등록 여부와 관계없이 동작하도록 한 번 더 확인
		Long userId = loginUserId(session);
		if (userId == null) {
			return LOGIN_REQUIRED;
		}

		try {
			String msg = service.invite(tripId, targetUserId, role, userId);
			rttr.addFlashAttribute("memberMsg", msg);
		} catch (IllegalStateException e) {
			// 7A · 7B · 7C, 권한 없음 등 → Service 문구를 그대로 보여 준다
			rttr.addFlashAttribute("memberMsg", e.getMessage());
		}
		return "redirect:/trips/" + tripId;
	}

	// 세션의 로그인 회원 번호 (로그인 안 했으면 null)
	private Long loginUserId(HttpSession session) {
		UserVO loginUser = (UserVO) session.getAttribute(LoginInterceptor.LOGIN_USER);
		if (loginUser == null) {
			return null;
		}
		return loginUser.getUserId();
	}
}
