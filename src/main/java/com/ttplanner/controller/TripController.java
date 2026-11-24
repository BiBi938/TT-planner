package com.ttplanner.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
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
import com.ttplanner.service.DayService;
import com.ttplanner.service.LikeService;
import com.ttplanner.service.MemberService;
import com.ttplanner.service.ScheduleService;
import com.ttplanner.service.TripAccessException;
import com.ttplanner.service.TripPeriodConfirmException;
import com.ttplanner.service.TripService;
import com.ttplanner.service.TripValidationException;
import com.ttplanner.vo.TripFormVO;
import com.ttplanner.vo.TripVO;
import com.ttplanner.vo.UserVO;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j;

/**
 * 여행 화면 (담당: 박준성)
 *
 * GET  /trips                     UC-007 내 여행 (+ UC-008 새 여행 만들기 창)
 * POST /trips                     UC-008 여행 생성 → /trips/{번호}
 * GET  /trips/{tripId}            UC-005 여행 열람 (로그인 없이도 열림, 볼 수 있는지는 Service에서 검사)
 * GET  /s/{shareToken}            UC-005 · UC-016 공유 링크로 열람 (로그인 없이, 공개 범위와 관계없이)
 * POST /trips/{tripId}/info       UC-009 제목 · 지역 수정        (OWNER)
 * POST /trips/{tripId}/period     UC-009 1A 여행 기간 변경       (OWNER)
 * POST /trips/{tripId}/scope      UC-015 공개 범위 설정          (OWNER)
 * POST /trips/{tripId}/delete     UC-010 여행 삭제               (OWNER)
 *
 * 처리 결과 문구는 flash "tripMsg"로 넘긴다.
 * (양지호 님 DayController가 쓰는 flash "msg"와 겹치지 않게 이름을 다르게 둠)
 */
@Controller
@Log4j
@AllArgsConstructor
public class TripController {

	/** 로그인이 필요할 때 보내는 주소 (로그인 화면에 "로그인이 필요합니다." 표시) */
	private static final String LOGIN_REQUIRED = "redirect:/user/login?required";

	private TripService service;

	private DayService dayService; // DAY 목록 (양지호, UC-011 · UC-012)

	private LikeService likeService; // 좋아요 수 · 내가 눌렀는지 (김민정, UC-021)

	private ScheduleService scheduleService; // 일정 목록 (송유림, UC-013)

	private MemberService memberService; // 멤버 목록 (양지호, UC-017)

	// ===================== UC-007 내 여행 =====================

	@GetMapping("/trips")
	public String list(HttpSession session, Model model) {
		Long userId = loginUserId(session);
		if (userId == null) {
			return LOGIN_REQUIRED; // 1A. 비로그인
		}
		log.info("list......" + userId);

		model.addAttribute("trips", service.getMyTrips(userId));
		model.addAttribute("regionOptions", TripService.REGIONS);
		if (!model.containsAttribute("newForm")) {
			model.addAttribute("newForm", new TripFormVO());
		}
		// UC-007 2B. 여행 복사 중인지는 list.jsp가 세션 copyDays(송유림)로 직접 확인한다
		return "trip/list";
	}

	// ===================== UC-008 여행 생성 =====================

	@PostMapping("/trips")
	public String create(TripFormVO form, HttpSession session, RedirectAttributes rttr) {
		Long userId = loginUserId(session);
		if (userId == null) {
			return LOGIN_REQUIRED;
		}
		try {
			Long tripId = service.create(userId, form);
			rttr.addFlashAttribute("tripMsg", "여행을 만들었습니다. 공개 범위는 비공개입니다.");
			return "redirect:/trips/" + tripId;
		} catch (TripValidationException e) {
			// 입력 창을 다시 열고 입력값과 오류 문구를 보여 준다 (3단계로 돌아감)
			rttr.addFlashAttribute("tripErrors", e.getErrors());
			rttr.addFlashAttribute("newForm", form);
			rttr.addFlashAttribute("openDialog", "new");
			return "redirect:/trips";
		}
	}

	// ===================== UC-005 여행 열람 =====================

	@GetMapping("/trips/{tripId}")
	public String detail(@PathVariable("tripId") Long tripId, HttpSession session,
						 HttpServletRequest request, Model model) {
		log.info("detail......" + tripId);
		TripVO trip = service.getTrip(tripId, loginUserId(session));
		fillDetail(trip, null, request, model);
		return "trip/detail";
	}

	@GetMapping("/s/{shareToken}")
	public String shared(@PathVariable("shareToken") String shareToken, HttpSession session,
						 HttpServletRequest request, Model model) {
		log.info("shared......" + shareToken);
		TripVO trip = service.getTripByShareToken(shareToken, loginUserId(session));
		fillDetail(trip, shareToken, request, model);
		return "trip/detail";
	}

	/**
	 * detail.jsp와 조각(_days.jsp, _members.jsp, _map.jsp, _like.jsp)이 쓰는 값 (구성정의서 8장 나.)
	 * trip, days, schedules, summary, members, myRole, likeCount, liked
	 * token: 공유 링크(/s/{token})로 열었을 때만 값이 있다 → [내 여행으로 복사]를 /trips/{id}/copy?token=…으로 보낸다
	 */
	private void fillDetail(TripVO trip, String token, HttpServletRequest request, Model model) {
		Long tripId = trip.getTripId();
		model.addAttribute("trip", trip);
		model.addAttribute("myRole", trip.getMyRole()); // 멤버가 아니거나 로그인하지 않았으면 null
		model.addAttribute("token", token);

		// DAY 카드 목록 → trip/_days.jsp (양지호). DayVO에 dayId가 있어야 [DAY 삭제] 주소가 만들어진다
		model.addAttribute("days", dayService.findDays(tripId));

		// 일정 목록 → trip/_schedules.jsp, trip/_map.jsp (송유림, UC-013). DAY 번호 → SLOT_NO 순
		model.addAttribute("schedules", scheduleService.findByTrip(tripId));

		// TODO(양지호): MemberService.summary(tripId)가 develop에 들어오면 그것으로 바꾼다.
		//              지금은 여행 정보에 이미 있는 OWNER 닉네임 · 멤버 수로 같은 이름(ownerNickname, otherCount)을 만들어 둔다.
		Map<String, Object> summary = new HashMap<>();
		summary.put("ownerNickname", trip.getOwnerNickname());
		summary.put("otherCount", Math.max(trip.getMemberCount() - 1, 0));
		model.addAttribute("summary", summary);

		// 멤버 목록 → trip/_members.jsp (양지호, UC-017). 멤버에게만 보이는지는 detail.jsp(trip.member)에서 정한다
		model.addAttribute("members", memberService.findByTrip(tripId));

		// 좋아요 수 · 내가 눌렀는지 → trip/_like.jsp (김민정, UC-021)
		// fillDetail은 session을 받지 않으므로 request에서 꺼낸다.
		// getSession(false): 세션이 없으면 새로 만들지 않고 null → 비로그인 → isLiked는 false
		Long userId = loginUserId(request.getSession(false));
		model.addAttribute("likeCount", likeService.count(tripId));
		model.addAttribute("liked", likeService.isLiked(tripId, userId));

		// UC-016 1A. 공유 링크는 멤버에게만 보인다
		if (trip.isMember()) {
			model.addAttribute("shareUrl", baseUrl(request) + "/s/" + trip.getShareToken());
		}

		// [제목·지역 수정] 창, 기간 변경 칸의 처음 값 (입력 오류로 돌아온 경우에는 flash 값이 이미 들어 있음)
		model.addAttribute("regionOptions", TripService.REGIONS);
		if (!model.containsAttribute("infoForm")) {
			TripFormVO infoForm = new TripFormVO();
			infoForm.setTitle(trip.getTitle());
			infoForm.setRegions(trip.getRegions());
			model.addAttribute("infoForm", infoForm);
		}
		if (!model.containsAttribute("periodForm")) {
			TripFormVO periodForm = new TripFormVO();
			periodForm.setStartDate(trip.getStartDateInput());
			periodForm.setEndDate(trip.getEndDateInput());
			model.addAttribute("periodForm", periodForm);
		}
	}

	// ===================== UC-009 여행 정보 수정 =====================

	@PostMapping("/trips/{tripId}/info")
	public String modifyInfo(@PathVariable("tripId") Long tripId, TripFormVO form,
							 HttpSession session, RedirectAttributes rttr) {
		Long userId = loginUserId(session);
		if (userId == null) {
			return LOGIN_REQUIRED;
		}
		try {
			rttr.addFlashAttribute("tripMsg", service.modifyInfo(userId, tripId, form));
		} catch (TripValidationException e) {
			rttr.addFlashAttribute("tripErrors", e.getErrors());
			rttr.addFlashAttribute("infoForm", form);
			rttr.addFlashAttribute("openDialog", "info");
		}
		return "redirect:/trips/" + tripId;
	}

	@PostMapping("/trips/{tripId}/period")
	public String changePeriod(@PathVariable("tripId") Long tripId, TripFormVO form,
							   @RequestParam(value = "confirmDelete", defaultValue = "N") String confirmDelete,
							   HttpSession session, RedirectAttributes rttr) {
		Long userId = loginUserId(session);
		if (userId == null) {
			return LOGIN_REQUIRED;
		}
		try {
			rttr.addFlashAttribute("tripMsg",
					service.changePeriod(userId, tripId, form, "Y".equals(confirmDelete)));
		} catch (TripValidationException e) {
			rttr.addFlashAttribute("tripErrors", e.getErrors());
			rttr.addFlashAttribute("periodForm", form);
			rttr.addFlashAttribute("openDialog", "period");
		} catch (TripPeriodConfirmException e) {
			// 1B. 확인 창: [삭제하고 저장]을 누르면 confirmDelete=Y로 다시 보낸다
			rttr.addFlashAttribute("periodConfirm", e.getMessage());
			rttr.addFlashAttribute("periodForm", form);
		}
		return "redirect:/trips/" + tripId;
	}

	// ===================== UC-015 공개 범위 설정 =====================

	@PostMapping("/trips/{tripId}/scope")
	public String changeScope(@PathVariable("tripId") Long tripId, @RequestParam("scope") String scope,
							  HttpSession session, RedirectAttributes rttr) {
		Long userId = loginUserId(session);
		if (userId == null) {
			return LOGIN_REQUIRED;
		}
		try {
			rttr.addFlashAttribute("tripMsg", service.changeScope(userId, tripId, scope));
		} catch (TripValidationException e) {
			rttr.addFlashAttribute("tripErrors", e.getErrors()); // 2A. 공개 범위는 바꾸지 않음
		}
		return "redirect:/trips/" + tripId;
	}

	// ===================== UC-010 여행 삭제 =====================

	@PostMapping("/trips/{tripId}/delete")
	public String remove(@PathVariable("tripId") Long tripId, HttpSession session, RedirectAttributes rttr) {
		Long userId = loginUserId(session);
		if (userId == null) {
			return LOGIN_REQUIRED;
		}
		service.remove(userId, tripId);
		rttr.addFlashAttribute("tripMsg", "여행을 삭제했습니다.");
		return "redirect:/trips";
	}

	// ===================== 볼 수 없는 여행 · 권한 없음 =====================

	@ExceptionHandler(TripAccessException.class)
	public ModelAndView accessDenied(TripAccessException e, HttpServletResponse response) {
		log.info("access denied: " + e.getMessage());
		response.setStatus(e.getStatus());
		ModelAndView mav = new ModelAndView("trip/denied");
		mav.addObject("message", e.getMessage());
		return mav;
	}

	// ===================== helpers =====================

	/**
	 * 로그인 회원 번호. 로그인하지 않았으면 null
	 * (로그인하면 UserController(윤아연)가 세션 "loginUser"(LoginInterceptor.LOGIN_USER)에 UserVO를 넣는다)
	 */
	private Long loginUserId(HttpSession session) {
		Object loginUser = session == null ? null : session.getAttribute(LoginInterceptor.LOGIN_USER);
		if (loginUser instanceof UserVO) {
			return ((UserVO) loginUser).getUserId();
		}
		return null;
	}

	/** "http://localhost:8080" (+ 컨텍스트 경로) */
	private static String baseUrl(HttpServletRequest request) {
		String scheme = request.getScheme();
		int port = request.getServerPort();
		boolean defaultPort = ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
		return scheme + "://" + request.getServerName() + (defaultPort ? "" : ":" + port) + request.getContextPath();
	}
}
