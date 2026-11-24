package com.ttplanner.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ttplanner.interceptor.LoginInterceptor;
import com.ttplanner.service.CopyService;
import com.ttplanner.service.TripAccessException;
import com.ttplanner.vo.TripVO;
import com.ttplanner.vo.UserVO;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j;

/**
 * 여행 복사 화면 (담당: 송유림, UC-020 · 화면설계서 S-09)
 *
 * GET  /trips/{tripId}/copy          ① 복사할 DAY 선택 화면 (?token= : 공유 링크로 연 여행)
 * POST /trips/{tripId}/copy          ② [복사] → 세션 copyDays 저장 → /trips (내 여행)
 * POST /trips/{tripId}/copy/insert   ③ [여기에 DAY 넣기] (폼 값 position) → /trips/{tripId}
 * POST /trips/copy/new               [+ 복사한 DAY로 새 여행 만들기] → /trips/{새 번호}
 * POST /trips/copy/cancel            [복사 취소] → /trips
 *
 * 세션 copyDays (List<Long>, 고른 DAY_ID): JSP에서 ${not empty sessionScope.copyDays}로 복사 중인지 확인한다.
 * 처리 결과 문구는 flash "tripMsg"로 넘긴다 → trip/list.jsp, trip/detail.jsp(박준성)가 보여 준다.
 */
@Controller
@Log4j
@AllArgsConstructor
public class CopyController {

	/** 복사한 DAY_ID 목록을 담는 세션 이름 (구성정의서 8장 나.) */
	public static final String COPY_DAYS = "copyDays";

	private CopyService service;

	// ===================== ① DAY 선택 화면 =====================

	@GetMapping("/trips/{tripId}/copy")
	public String copyForm(@PathVariable("tripId") Long tripId,
			@RequestParam(value = "token", required = false) String token,
			HttpSession session, Model model) {
		Long userId = loginUserId(session);
		if (userId == null) {
			return loginRedirect(tripId, token); // 로그인 후 이 화면으로 돌아온다
		}
		TripVO trip = service.getCopyableTrip(tripId, userId, token);
		model.addAttribute("trip", trip);
		model.addAttribute("days", service.getCopyDays(tripId));
		model.addAttribute("token", token);
		return "trip/copy";
	}

	// ===================== ② [복사] =====================

	@PostMapping("/trips/{tripId}/copy")
	public String copy(@PathVariable("tripId") Long tripId,
			@RequestParam(value = "token", required = false) String token,
			@RequestParam(value = "dayIds", required = false) List<Long> dayIds,
			HttpSession session, Model model, RedirectAttributes rttr) {
		Long userId = loginUserId(session);
		if (userId == null) {
			return loginRedirect(tripId, token);
		}
		try {
			List<Long> copyDays = service.selectDays(tripId, userId, token, dayIds);
			session.setAttribute(COPY_DAYS, new ArrayList<>(copyDays));

			rttr.addFlashAttribute("tripMsg", "DAY " + copyDays.size()
					+ "개를 복사했습니다. 넣을 여행을 열고 [여기에 DAY 넣기]를 누르세요.");
			return "redirect:/trips";
		} catch (IllegalStateException e) {
			// 아무것도 고르지 않음 등: 선택 화면을 다시 보여 주고 문구 표시
			model.addAttribute("trip", service.getCopyableTrip(tripId, userId, token));
			model.addAttribute("days", service.getCopyDays(tripId));
			model.addAttribute("token", token);
			model.addAttribute("copyError", e.getMessage());
			return "trip/copy";
		}
	}

	// ===================== ③ [여기에 DAY 넣기] =====================

	@PostMapping("/trips/{tripId}/copy/insert")
	public String insert(@PathVariable("tripId") Long tripId,
			@RequestParam(value = "position", defaultValue = "-1") int position,
			HttpSession session, RedirectAttributes rttr) {
		Long userId = loginUserId(session);
		if (userId == null) {
			return "redirect:/user/login?required";
		}
		try {
			String msg = service.insert(userId, tripId, position, getCopyDays(session));
			session.removeAttribute(COPY_DAYS); // 넣은 뒤에는 복사 상태가 끝난다 (S-09 5)
			rttr.addFlashAttribute("tripMsg", msg);
		} catch (IllegalStateException e) {
			rttr.addFlashAttribute("tripMsg", e.getMessage()); // 복사 상태는 그대로 둔다
		}
		return "redirect:/trips/" + tripId;
	}

	// ===================== [+ 복사한 DAY로 새 여행 만들기] =====================

	@PostMapping("/trips/copy/new")
	public String createTrip(HttpSession session, RedirectAttributes rttr) {
		Long userId = loginUserId(session);
		if (userId == null) {
			return "redirect:/user/login?required";
		}
		try {
			Long newTripId = service.createTrip(userId, getCopyDays(session));
			session.removeAttribute(COPY_DAYS);
			rttr.addFlashAttribute("tripMsg",
					"복사한 DAY로 새 여행을 만들었습니다. 공개 범위는 비공개이고, 날짜는 여행 기간에서 바꿀 수 있습니다.");
			return "redirect:/trips/" + newTripId;
		} catch (IllegalStateException e) {
			rttr.addFlashAttribute("tripMsg", e.getMessage());
			return "redirect:/trips";
		}
	}

	// ===================== [복사 취소] =====================

	@PostMapping("/trips/copy/cancel")
	public String cancel(HttpSession session, RedirectAttributes rttr) {
		session.removeAttribute(COPY_DAYS);
		rttr.addFlashAttribute("tripMsg", "복사를 취소했습니다.");
		return "redirect:/trips";
	}

	// ===================== 볼 수 없는 여행 =====================

	/** TripController와 같은 방식: trip/denied.jsp(박준성)에 문구를 보여 준다 */
	@ExceptionHandler(TripAccessException.class)
	public ModelAndView accessDenied(TripAccessException e, HttpServletResponse response) {
		log.info("copy access denied: " + e.getMessage());
		response.setStatus(e.getStatus());
		ModelAndView mav = new ModelAndView("trip/denied");
		mav.addObject("message", e.getMessage());
		return mav;
	}

	// ===================== helpers =====================

	/** 로그인 회원 번호 (로그인하지 않았으면 null) */
	private Long loginUserId(HttpSession session) {
		UserVO loginUser = (UserVO) session.getAttribute(LoginInterceptor.LOGIN_USER);
		return loginUser == null ? null : loginUser.getUserId();
	}

	/** 세션의 복사한 DAY_ID 목록 (복사 중이 아니면 null) */
	@SuppressWarnings("unchecked")
	private List<Long> getCopyDays(HttpSession session) {
		return (List<Long>) session.getAttribute(COPY_DAYS);
	}

	/** 로그인 화면으로 보냈다가 로그인 후 복사 화면으로 돌아오게 한다 (UserController.loginForm의 back) */
	private String loginRedirect(Long tripId, String token) {
		String back = "/trips/" + tripId + "/copy";
		if (token != null && !token.trim().isEmpty()) {
			back += "?token=" + token.trim();
		}
		try {
			return "redirect:/user/login?required&back=" + URLEncoder.encode(back, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return "redirect:/user/login?required"; // UTF-8은 항상 있으므로 오지 않는다
		}
	}
}
