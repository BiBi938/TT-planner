package com.ttplanner.controller;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ttplanner.interceptor.LoginInterceptor;
import com.ttplanner.service.DayService;
import com.ttplanner.service.MemberService;
import com.ttplanner.vo.DayVO;
import com.ttplanner.vo.UserVO;

import lombok.Setter;
import lombok.extern.log4j.Log4j;

/**
 * DAY 추가·삭제 요청 (UC-011), DAY 날짜 변경 요청 (UC-012)
 * 처리 후 여행 상세 화면(/trips/{tripId}, 박준성)으로 이동하고,
 * 결과 문구는 flash "msg"로 전달 → trip/_days.jsp에서 출력
 * 권한: OWNER, EDITOR만 추가·삭제할 수 있다 (VIEWER, 멤버 아님 → 막음)
 *       날짜 변경은 OWNER만 할 수 있다 (UC-012)
 */
@Controller
@Log4j
@RequestMapping("/trips/{tripId}/days")
public class DayController {

	/** 로그인이 필요할 때 보내는 주소 (로그인 화면에 "로그인이 필요합니다." 표시) */
	private static final String LOGIN_REQUIRED = "redirect:/user/login?required";

	/** 권한이 없을 때 문구 */
	private static final String NO_PERMISSION = "DAY를 추가·삭제할 권한이 없습니다.";

	/** DAY 날짜 변경 권한이 없을 때 문구 (UC-012는 OWNER만) */
	private static final String NO_PERMISSION_DATE = "DAY 날짜를 변경할 권한이 없습니다.";

	@Setter(onMethod_ = @Autowired)
	private DayService service;

	@Setter(onMethod_ = @Autowired)
	private MemberService memberService;

	// DAY 추가: POST /trips/1/days
	@PostMapping("")
	public String add(@PathVariable("tripId") Long tripId, HttpSession session, RedirectAttributes rttr) {
		log.info("add day......" + tripId);

		// 권한 검사: 로그인 안 함 → 로그인 화면 / OWNER·EDITOR가 아님 → 막고 문구
		Long userId = loginUserId(session);
		if (userId == null) {
			return LOGIN_REQUIRED;
		}
		if (!canEditDays(memberService.getRole(tripId, userId))) {
			rttr.addFlashAttribute("msg", NO_PERMISSION);
			return "redirect:/trips/" + tripId;
		}

		try {
			DayVO added = service.addDay(tripId);

			// 날짜를 "11/23" 같은 글자로 바꾼다
			SimpleDateFormat format = new SimpleDateFormat("M/d");
			String endDate = format.format(added.getDayDate());

			rttr.addFlashAttribute("msg",
					"DAY " + added.getDayNo() + "을 추가했습니다. 종료일이 " + endDate + "로 늘었습니다.");
		} catch (IllegalStateException e) {
			// Service에서 막은 경우 (없는 여행, 30일 제한) → 그 문구를 그대로 보여 준다
			rttr.addFlashAttribute("msg", e.getMessage());
		}

		return "redirect:/trips/" + tripId;
	}

	// DAY 삭제: POST /trips/1/days/12/delete   (12 = DAY_ID, DAY 번호가 아님)
	// (일정이 있을 때의 확인 창 1C는 화면 _days.jsp의 JavaScript confirm()에서 처리)
	@PostMapping("/{dayId}/delete")
	public String delete(@PathVariable("tripId") Long tripId,
						 @PathVariable("dayId") Long dayId,
						 HttpSession session,
						 RedirectAttributes rttr) {
		log.info("delete day......" + tripId + ", DAY_ID " + dayId);

		// 권한 검사: 로그인 안 함 → 로그인 화면 / OWNER·EDITOR가 아님 → 막고 문구
		Long userId = loginUserId(session);
		if (userId == null) {
			return LOGIN_REQUIRED;
		}
		if (!canEditDays(memberService.getRole(tripId, userId))) {
			rttr.addFlashAttribute("msg", NO_PERMISSION);
			return "redirect:/trips/" + tripId;
		}

		try {
			service.deleteDay(tripId, dayId);
			rttr.addFlashAttribute("msg", "뒤의 DAY가 하루씩 당겨지고 종료일이 하루 줄었습니다.");
		} catch (IllegalStateException e) {
			// Service에서 막은 경우 (1B DAY 1개, DAY 없음, 1D 마지막 일정 보호)
			rttr.addFlashAttribute("msg", e.getMessage());
		}

		return "redirect:/trips/" + tripId;
	}

	// DAY 날짜 변경 (UC-012): POST /trips/1/days/12/date   (dayDate=2026-11-26, 12 = DAY_ID)
	// (3A [취소]는 화면 _days.jsp의 JavaScript에서 처리 → 여기로 오지 않음)
	@PostMapping("/{dayId}/date")
	public String changeDate(@PathVariable("tripId") Long tripId,
							 @PathVariable("dayId") Long dayId,
							 @RequestParam(value = "dayDate", required = false) String dayDate,
							 HttpSession session,
							 RedirectAttributes rttr) {
		log.info("change day date......" + tripId + ", DAY_ID " + dayId + " → " + dayDate);

		// 권한 검사: 로그인 안 함 → 로그인 화면 / OWNER가 아님 → 막고 문구 (EDITOR도 안 됨, UC-012 Actor)
		Long userId = loginUserId(session);
		if (userId == null) {
			return LOGIN_REQUIRED;
		}
		if (!"OWNER".equals(memberService.getRole(tripId, userId))) {
			rttr.addFlashAttribute("msg", NO_PERMISSION_DATE);
			return "redirect:/trips/" + tripId;
		}

		try {
			String msg = service.changeDayDate(tripId, dayId, toDate(dayDate));
			rttr.addFlashAttribute("msg", msg);
		} catch (IllegalStateException e) {
			// Service에서 막은 경우 (날짜 없음, 여행 · DAY 없음) → 그 문구를 그대로 보여 준다
			rttr.addFlashAttribute("msg", e.getMessage());
		}

		return "redirect:/trips/" + tripId;
	}

	// ===================== 도우미 =====================

	// 로그인한 회원 번호. 로그인하지 않았으면 null
	// (로그인하면 UserController(윤아연)가 세션 "loginUser"에 UserVO를 넣는다)
	private Long loginUserId(HttpSession session) {
		UserVO loginUser = (UserVO) session.getAttribute(LoginInterceptor.LOGIN_USER);
		if (loginUser == null) {
			return null;
		}
		return loginUser.getUserId();
	}

	// DAY를 추가·삭제할 수 있는 권한인지: OWNER, EDITOR만 true
	private boolean canEditDays(String role) {
		return "OWNER".equals(role) || "EDITOR".equals(role);
	}

	// "2026-11-26" 글자 → 날짜. 비었거나 모양이 틀리면 null (→ Service가 "날짜를 골라 주세요."로 막음)
	private Date toDate(String text) {
		if (text == null || text.trim().isEmpty()) {
			return null;
		}
		try {
			return java.sql.Date.valueOf(text.trim());
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
