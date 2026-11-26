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
 * 멤버 초대 (UC-017) · 멤버 권한 관리 (UC-018) · 여행 나가기 (UC-019) · 초대 응답 (UC-025) · 참여 요청 (UC-026) 요청 · 담당: 양지호
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
 * POST /api/trips/{tripId}/members/{userId}/role   role=VIEWER   (Ajax, UC-018 권한 변경 · 드래그)
 *   성공 · 이미 같은 권한   200  {"message": "편집러 님 권한을 VIEWER로 바꿨습니다.", "role": "VIEWER"}
 *   권한 없음 · 멤버 아님 등 400  {"message": "..."}
 *   로그인 안 함            401  LoginInterceptor
 *
 * POST /trips/{tripId}/members/{userId}/kick    (폼 제출, UC-018 내보내기 5A)
 * POST /trips/{tripId}/members/{userId}/owner   (폼 제출, UC-018 OWNER 넘기기 5B)
 *   결과 문구를 flash "memberMsg"로 담아 /trips/{tripId}로 이동 (invite와 같음)
 *
 * POST /trips/{tripId}/members/leave   (폼 제출, UC-019 여행 나가기)
 *   성공 → flash "tripMsg"(내 여행 화면 trip/list.jsp가 출력, 박준성) + redirect:/trips
 *   실패(멤버 아님 · OWNER) → flash "memberMsg" + redirect:/trips/{tripId} (멤버 목록 창이 다시 열림)
 *
 * POST /trips/{tripId}/members/accept   (폼 제출, UC-025 초대 수락)
 * POST /trips/{tripId}/members/reject   (폼 제출, UC-025 초대 거절 3A)
 *   성공 · 실패 모두 flash "tripMsg" + redirect:/trips (초대 목록이 있는 내 여행 화면으로 돌아감)
 *
 * POST /trips/{tripId}/members/request   token=(공유 링크로 열었을 때만)   (폼 제출, UC-026 참여 요청)
 *   성공 · 실패 모두 flash "tripMsg" + 원래 여행 상세 화면으로 (detail.jsp가 tripMsg 출력)
 *   token 있으면 /s/{token}, 없으면 /trips/{tripId} (비공개 여행은 /trips/{id}로 볼 수 없으므로)
 *
 * 권한 검사(OWNER만, 나가기는 참여 중인 EDITOR · VIEWER만, 초대 응답은 초대받은 본인만)는 MemberService가 한다.
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

	// ===== UC-018 =====

	// 권한 변경 (Ajax): POST /api/trips/1/members/20/role  (role=VIEWER)
	// 메뉴 [권한 변경]과 드래그(3A) 모두 이 주소로 보낸다
	@PostMapping("/api/trips/{tripId}/members/{userId}/role")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> changeRole(@PathVariable("tripId") Long tripId,
			@PathVariable("userId") Long targetUserId,
			@RequestParam(value = "role", required = false) String role, HttpSession session) {
		log.info("changeRole......" + tripId + ", target=" + targetUserId + ", role=" + role);

		Map<String, Object> body = new HashMap<String, Object>();
		try {
			String msg = service.changeRole(tripId, targetUserId, role, loginUserId(session));
			body.put("message", msg);
			body.put("role", role); // 화면이 이름을 옮길 칸 (EDITOR 또는 VIEWER)
			return ResponseEntity.ok(body); // 200
		} catch (IllegalStateException e) {
			// 권한 없음, 잘못된 권한, 멤버 아님, 대상이 OWNER → 400 + 문구 (화면은 드래그했으면 원래 칸으로 되돌림)
			body.put("message", e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
		}
	}

	// 내보내기 (폼 제출): POST /trips/1/members/20/kick
	@PostMapping("/trips/{tripId}/members/{userId}/kick")
	public String kick(@PathVariable("tripId") Long tripId, @PathVariable("userId") Long targetUserId,
			HttpSession session, RedirectAttributes rttr) {
		log.info("kick......" + tripId + ", target=" + targetUserId);

		Long userId = loginUserId(session);
		if (userId == null) {
			return LOGIN_REQUIRED;
		}

		try {
			rttr.addFlashAttribute("memberMsg", service.kick(tripId, targetUserId, userId));
		} catch (IllegalStateException e) {
			rttr.addFlashAttribute("memberMsg", e.getMessage());
		}
		return "redirect:/trips/" + tripId;
	}

	// OWNER 넘기기 (폼 제출): POST /trips/1/members/20/owner
	@PostMapping("/trips/{tripId}/members/{userId}/owner")
	public String transferOwner(@PathVariable("tripId") Long tripId, @PathVariable("userId") Long targetUserId,
			HttpSession session, RedirectAttributes rttr) {
		log.info("transferOwner......" + tripId + ", target=" + targetUserId);

		Long userId = loginUserId(session);
		if (userId == null) {
			return LOGIN_REQUIRED;
		}

		try {
			rttr.addFlashAttribute("memberMsg", service.transferOwner(tripId, targetUserId, userId));
		} catch (IllegalStateException e) {
			rttr.addFlashAttribute("memberMsg", e.getMessage());
		}
		return "redirect:/trips/" + tripId;
	}

	// ===== UC-019 =====

	// 여행 나가기 (폼 제출): POST /trips/1/members/leave
	// 나가면 이 여행을 더 볼 수 없으므로 내 여행 화면으로 이동한다 (레드마인 5)
	@PostMapping("/trips/{tripId}/members/leave")
	public String leave(@PathVariable("tripId") Long tripId, HttpSession session, RedirectAttributes rttr) {
		log.info("leave......" + tripId);

		Long userId = loginUserId(session);
		if (userId == null) {
			return LOGIN_REQUIRED;
		}

		try {
			rttr.addFlashAttribute("tripMsg", service.leave(tripId, userId)); // list.jsp가 출력하는 이름
			return "redirect:/trips";
		} catch (IllegalStateException e) {
			rttr.addFlashAttribute("memberMsg", e.getMessage());
			return "redirect:/trips/" + tripId;
		}
	}

	// ===== UC-025 =====

	// 초대 수락 (폼 제출): POST /trips/1/members/accept
	// 내 여행 화면의 초대 목록에서 누르므로, 결과는 내 여행 화면(tripMsg)에 보여 준다
	@PostMapping("/trips/{tripId}/members/accept")
	public String acceptInvite(@PathVariable("tripId") Long tripId, HttpSession session, RedirectAttributes rttr) {
		log.info("acceptInvite......" + tripId);

		Long userId = loginUserId(session);
		if (userId == null) {
			return LOGIN_REQUIRED;
		}

		try {
			rttr.addFlashAttribute("tripMsg", service.acceptInvite(tripId, userId));
		} catch (IllegalStateException e) {
			rttr.addFlashAttribute("tripMsg", e.getMessage());
		}
		return "redirect:/trips";
	}

	// 초대 거절 (폼 제출): POST /trips/1/members/reject
	@PostMapping("/trips/{tripId}/members/reject")
	public String rejectInvite(@PathVariable("tripId") Long tripId, HttpSession session, RedirectAttributes rttr) {
		log.info("rejectInvite......" + tripId);

		Long userId = loginUserId(session);
		if (userId == null) {
			return LOGIN_REQUIRED;
		}

		try {
			rttr.addFlashAttribute("tripMsg", service.rejectInvite(tripId, userId));
		} catch (IllegalStateException e) {
			rttr.addFlashAttribute("tripMsg", e.getMessage());
		}
		return "redirect:/trips";
	}

	// ===== UC-026 =====

	// 참여 요청 (폼 제출): POST /trips/2/members/request  (공유 링크로 열었으면 token=…)
	// 로그인 안 했으면 LoginInterceptor가 로그인 화면으로 보낸다 (2A)
	@PostMapping("/trips/{tripId}/members/request")
	public String requestJoin(@PathVariable("tripId") Long tripId,
			@RequestParam(value = "token", required = false) String token,
			HttpSession session, RedirectAttributes rttr) {
		log.info("requestJoin......" + tripId + ", token=" + token);

		Long userId = loginUserId(session);
		if (userId == null) {
			return LOGIN_REQUIRED;
		}

		try {
			rttr.addFlashAttribute("tripMsg", service.requestJoin(tripId, userId, token));
		} catch (IllegalStateException e) {
			rttr.addFlashAttribute("tripMsg", e.getMessage());
		}

		// 7. 같은 여행 상세 화면으로 돌아간다
		if (token != null && !token.trim().isEmpty()) {
			// addAttribute로 넘기면 스프링이 {token} 자리에 넣으면서 주소에 쓸 수 없는 글자를 안전하게 바꿔 준다
			rttr.addAttribute("token", token.trim());
			return "redirect:/s/{token}";
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
