package com.ttplanner.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ttplanner.interceptor.LoginInterceptor;
import com.ttplanner.service.UserService;
import com.ttplanner.vo.OwnedTripVO;
import com.ttplanner.vo.UserVO;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j;

/**
 * 회원 화면 (담당: 윤아연)
 *
 *   UC-001 회원가입       GET/POST /user/join                → user/join.jsp
 *   UC-002 로그인         GET/POST /user/login               → user/login.jsp
 *   UC-003 로그아웃       POST     /user/logout              → 로그인 화면으로 이동
 *   UC-001, 023 중복 확인 GET      /api/users/check (Ajax)    → JSON
 *   UC-023 마이페이지     GET/POST /settings/mypage, POST /settings/password → settings/mypage.jsp
 *   UC-024 회원 탈퇴      GET /settings/withdraw, POST /settings/withdraw/check, POST /settings/withdraw
 *                                                           → settings/withdraw.jsp
 */
@Controller
@Log4j
@AllArgsConstructor
public class UserController {

	/**
	 * 로그인 후 첫 화면(내 여행, S-03) 주소.
	 * TODO(박준성): 내 여행 목록 주소가 확정되면 여기만 바꾼다.
	 */
	private static final String MY_TRIPS_URL = "/trips";

	/** 탈퇴 비밀번호 확인 후 [탈퇴]를 눌러야 하는 제한 시간 (5분) */
	private static final long WITHDRAW_CONFIRM_MILLIS = 5 * 60 * 1000L;
	private static final String WITHDRAW_VERIFIED_AT = "withdrawVerifiedAt";

	private UserService service;

	// =============================================================== UC-002 로그인

	@GetMapping("/user/login")
	public String loginForm(@RequestParam(value = "back", required = false) String back,
			@RequestParam(value = "required", required = false) String required,
			HttpSession session, Model model) {

		if (session.getAttribute(LoginInterceptor.LOGIN_USER) != null) {
			return "redirect:" + MY_TRIPS_URL; // 이미 로그인한 상태
		}
		// 좋아요 · 복사 버튼에서 온 경우: /user/login?back=/trips/12 → 로그인 후 그 여행으로 돌아간다 (확장 5A)
		if (isSafeBackUrl(back)) {
			session.setAttribute(LoginInterceptor.BACK_URL, back);
		}
		if (required != null && !model.containsAttribute("message")) {
			model.addAttribute("message", "로그인이 필요합니다.");
		}
		return "user/login";
	}

	@PostMapping("/user/login")
	public String login(@RequestParam(value = "email", required = false) String email,
			@RequestParam(value = "password", required = false) String password,
			HttpServletRequest request, Model model) {

		if (isBlank(email) || isBlank(password)) {
			model.addAttribute("email", email);
			model.addAttribute("error", "이메일 주소와 비밀번호를 모두 입력하세요.");
			return "user/login";
		}

		UserVO user = service.login(email, password);
		if (user == null) {
			// 어느 항목이 틀렸는지 구분하지 않는다 (확장 3A)
			model.addAttribute("email", email);
			model.addAttribute("error", "이메일 주소 또는 비밀번호가 올바르지 않습니다.");
			return "user/login";
		}

		// 로그인 전 세션을 버리고 새 세션을 만든다 (세션 고정 공격 방지). 돌아갈 주소만 옮겨 담는다.
		HttpSession oldSession = request.getSession();
		Object back = oldSession.getAttribute(LoginInterceptor.BACK_URL);
		oldSession.invalidate();

		HttpSession session = request.getSession(true);
		session.setAttribute(LoginInterceptor.LOGIN_USER, user);
		log.info("login: userId=" + user.getUserId());

		if (back instanceof String && isSafeBackUrl((String) back)) {
			return "redirect:" + back;
		}
		return "redirect:" + MY_TRIPS_URL;
	}

	// =============================================================== UC-003 로그아웃

	@PostMapping("/user/logout")
	public String logout(HttpSession session, RedirectAttributes rttr) {
		Object user = session.getAttribute(LoginInterceptor.LOGIN_USER);
		session.invalidate();
		log.info("logout: " + user);
		rttr.addFlashAttribute("message", "로그아웃되었습니다.");
		return "redirect:/user/login";
	}

	// =============================================================== UC-001 회원가입

	@GetMapping("/user/join")
	public String joinForm(HttpSession session) {
		if (session.getAttribute(LoginInterceptor.LOGIN_USER) != null) {
			return "redirect:" + MY_TRIPS_URL;
		}
		return "user/join";
	}

	@PostMapping("/user/join")
	public String join(UserVO user,
			@RequestParam(value = "passwordConfirm", required = false) String passwordConfirm,
			Model model, RedirectAttributes rttr) {

		user.setUserId(null);
		Map<String, String> errors = service.join(user, passwordConfirm);
		if (!errors.isEmpty()) {
			user.setPassword(null);
			model.addAttribute("user", user);
			model.addAttribute("errors", errors);
			return "user/join";
		}
		// 입력한 이메일이 채워진 로그인 화면으로 이동 (UC-001 7단계)
		rttr.addFlashAttribute("email", user.getEmail());
		rttr.addFlashAttribute("message", "가입이 완료되었습니다. 바로 로그인할 수 있습니다.");
		return "redirect:/user/login";
	}

	// =============================================================== 중복 확인 Ajax (UC-001, 023)

	/**
	 * GET /api/users/check?nickname=홍길동  또는  /api/users/check?email=a@b.com
	 * 응답 예) {"field":"nickname","available":false,"message":"이미 사용 중인 닉네임입니다."}
	 * 로그인한 상태(마이페이지)면 내 닉네임 · 이메일은 중복으로 보지 않는다.
	 */
	@GetMapping(value = "/api/users/check", produces = "application/json;charset=UTF-8")
	@ResponseBody
	public Map<String, Object> check(@RequestParam(value = "nickname", required = false) String nickname,
			@RequestParam(value = "email", required = false) String email, HttpSession session) {

		UserVO loginUser = (UserVO) session.getAttribute(LoginInterceptor.LOGIN_USER);
		Long excludeUserId = loginUser == null ? null : loginUser.getUserId();

		Map<String, Object> result = new LinkedHashMap<String, Object>();
		String error;
		if (nickname != null) {
			result.put("field", "nickname");
			error = service.checkNickname(nickname, excludeUserId);
			result.put("message", error == null ? "사용할 수 있는 닉네임입니다." : error);
		} else {
			result.put("field", "email");
			error = service.checkEmail(email, excludeUserId);
			result.put("message", error == null ? "사용할 수 있는 이메일 주소입니다." : error);
		}
		result.put("available", error == null);
		return result;
	}

	// =============================================================== UC-023 마이페이지

	@GetMapping("/settings/mypage")
	public String mypage(HttpSession session, Model model) {
		UserVO loginUser = loginUser(session);
		if (!model.containsAttribute("user")) {
			model.addAttribute("user", service.get(loginUser.getUserId()));
		}
		return "settings/mypage";
	}

	@PostMapping("/settings/mypage")
	public String updateInfo(@RequestParam("nickname") String nickname, @RequestParam("email") String email,
			HttpSession session, Model model, RedirectAttributes rttr) {

		UserVO loginUser = loginUser(session);
		UserVO user = new UserVO();
		user.setUserId(loginUser.getUserId());
		user.setNickname(nickname);
		user.setEmail(email);

		Map<String, String> errors = service.updateInfo(user);
		if (!errors.isEmpty()) {
			model.addAttribute("user", user);
			model.addAttribute("infoErrors", errors);
			return "settings/mypage";
		}
		// 사이드바에 보이는 닉네임도 바뀌도록 세션 값을 고친다
		loginUser.setNickname(user.getNickname());
		loginUser.setEmail(user.getEmail());
		session.setAttribute(LoginInterceptor.LOGIN_USER, loginUser);

		rttr.addFlashAttribute("message", "회원 정보를 저장했습니다.");
		return "redirect:/settings/mypage";
	}

	@PostMapping("/settings/password")
	public String changePassword(@RequestParam(value = "currentPassword", required = false) String currentPassword,
			@RequestParam(value = "newPassword", required = false) String newPassword,
			@RequestParam(value = "newPasswordConfirm", required = false) String newPasswordConfirm,
			HttpSession session, Model model, RedirectAttributes rttr) {

		UserVO loginUser = loginUser(session);
		Map<String, String> errors = service.changePassword(loginUser.getUserId(),
				currentPassword, newPassword, newPasswordConfirm);
		if (!errors.isEmpty()) {
			model.addAttribute("user", service.get(loginUser.getUserId()));
			model.addAttribute("pwErrors", errors);
			return "settings/mypage";
		}
		rttr.addFlashAttribute("message", "비밀번호를 변경했습니다.");
		return "redirect:/settings/mypage";
	}

	// =============================================================== UC-024 회원 탈퇴

	/** 1~2단계: 삭제될 여행과 OWNER 넘기기 안내를 보여 준다 */
	@GetMapping("/settings/withdraw")
	public String withdrawForm(HttpSession session, Model model) {
		addWithdrawInfo(loginUser(session).getUserId(), model);
		return "settings/withdraw";
	}

	/** 3~5단계: 비밀번호 확인 → 맞으면 "정말 탈퇴할까요?" 확인 창을 띄운다 */
	@PostMapping("/settings/withdraw/check")
	public String withdrawCheck(@RequestParam(value = "password", required = false) String password,
			HttpSession session, Model model) {

		Long userId = loginUser(session).getUserId();
		addWithdrawInfo(userId, model);

		if (!service.checkPassword(userId, password)) {
			session.removeAttribute(WITHDRAW_VERIFIED_AT);
			model.addAttribute("error", "비밀번호가 맞지 않습니다."); // 확장 4A
			return "settings/withdraw";
		}
		session.setAttribute(WITHDRAW_VERIFIED_AT, System.currentTimeMillis());
		model.addAttribute("confirmStep", true); // JSP가 확인 창을 띄운다
		return "settings/withdraw";
	}

	/** 6~7단계: 확인 창에서 [탈퇴]를 누른 경우 */
	@PostMapping("/settings/withdraw")
	public String withdraw(HttpSession session, RedirectAttributes rttr) {
		Long userId = loginUser(session).getUserId();

		Object verifiedAt = session.getAttribute(WITHDRAW_VERIFIED_AT);
		if (!(verifiedAt instanceof Long)
				|| System.currentTimeMillis() - (Long) verifiedAt > WITHDRAW_CONFIRM_MILLIS) {
			rttr.addFlashAttribute("error", "비밀번호를 다시 입력해 주세요.");
			return "redirect:/settings/withdraw";
		}

		service.withdraw(userId);
		session.invalidate();
		rttr.addFlashAttribute("message", "탈퇴가 완료되었습니다.");
		return "redirect:/user/login";
	}

	// =============================================================== 도우미

	private void addWithdrawInfo(Long userId, Model model) {
		List<OwnedTripVO> ownedTrips = service.getOwnedTrips(userId);
		int ownedWithMembers = 0;
		for (OwnedTripVO trip : ownedTrips) {
			if (trip.getOtherMemberCount() > 0) {
				ownedWithMembers++;
			}
		}
		model.addAttribute("ownedTrips", ownedTrips);
		model.addAttribute("ownedWithMembers", ownedWithMembers);
		model.addAttribute("joinedCount", service.countJoinedTrips(userId));
	}

	/** LoginInterceptor를 통과한 요청이므로 항상 로그인 회원이 있다 */
	private UserVO loginUser(HttpSession session) {
		return (UserVO) session.getAttribute(LoginInterceptor.LOGIN_USER);
	}

	/** 다른 사이트로 보내는 주소(//evil.com, http://...)는 받지 않는다 */
	private static boolean isSafeBackUrl(String url) {
		return url != null && url.startsWith("/") && !url.startsWith("//") && !url.contains("\\")
				&& !url.startsWith("/user/");
	}

	private static boolean isBlank(String s) {
		return s == null || s.trim().isEmpty();
	}
}
