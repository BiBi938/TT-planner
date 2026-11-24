package com.ttplanner.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ttplanner.interceptor.LoginInterceptor;
import com.ttplanner.service.MemberService;
import com.ttplanner.service.ScheduleService;
import com.ttplanner.vo.ScheduleVO;
import com.ttplanner.vo.UserVO;

import lombok.Setter;
import lombok.extern.log4j.Log4j;

/**
 * 일정 등록 · 수정 · 삭제와 순서 변경 (담당: 송유림, UC-013)
 *
 * 화면 요청 (폼 제출 → 여행 정보 화면으로 이동, 결과 문구는 flash "msg" → trip/_days.jsp가 출력)
 *   POST /trips/{tripId}/schedules                 일정 추가 (dayId, time, placeName, lat, lng, description)
 *   POST /trips/{tripId}/schedules/{schId}/edit    일정 수정 (time, placeName, lat, lng, description)
 *   POST /trips/{tripId}/schedules/{schId}/delete  일정 삭제
 *
 * Ajax (구성정의서 4장, 일정 순서 변경)
 *   POST /api/days/{dayId}/order   본문 JSON [12, 10, 11] (새 순서대로 나열한 SCH_ID)
 *     성공     200  {"schedules": [{"schId": 12, "slotNo": 1, "time": "09:00"}, ...]}
 *     비로그인  401  {"message": "로그인이 필요합니다.", "loginUrl": "/user/login"}
 *     권한 없음 403  {"message": "..."}
 *     목록 틀림 400  {"message": "..."}
 *
 * 권한: OWNER, EDITOR만 (VIEWER, 멤버 아님 → 막음). DayController(양지호)와 같은 방식으로 검사한다.
 */
@Controller
@Log4j
public class ScheduleController {

	/** 로그인이 필요할 때 보내는 주소 (로그인 화면에 "로그인이 필요합니다." 표시) */
	private static final String LOGIN_REQUIRED = "redirect:/user/login?required";

	private static final String NO_PERMISSION = "일정을 편집할 권한이 없습니다.";

	@Setter(onMethod_ = @Autowired)
	private ScheduleService service;

	@Setter(onMethod_ = @Autowired)
	private MemberService memberService;

	// 일정 추가: POST /trips/1/schedules
	// 폼 입력칸 이름이 ScheduleVO 필드 이름과 같아서 schedule에 자동으로 채워진다
	@PostMapping("/trips/{tripId}/schedules")
	public String add(@PathVariable("tripId") Long tripId, ScheduleVO schedule, BindingResult binding,
			HttpSession session, RedirectAttributes rttr) {
		log.info("add schedule......" + tripId);

		String denied = checkEditable(tripId, session, rttr);
		if (denied != null) {
			return denied;
		}
		if (binding.hasErrors()) { // 예: 숨은 칸 lat에 숫자가 아닌 값
			rttr.addFlashAttribute("msg", "입력값이 올바르지 않습니다. 다시 입력해 주세요.");
			return redirectTrip(tripId);
		}

		try {
			schedule.setSchId(null); // 새 번호는 시퀀스로 만든다
			service.add(tripId, schedule);
			rttr.addFlashAttribute("msg", "일정을 추가했습니다.");
		} catch (IllegalStateException e) {
			// Service에서 막은 경우 (전체 공개 여행 시간 · 장소 없음, 글자 수, 없는 DAY 등) → 그 문구를 그대로
			rttr.addFlashAttribute("msg", e.getMessage());
		}
		return redirectTrip(tripId);
	}

	// 일정 수정: POST /trips/1/schedules/35/edit   (35 = SCH_ID)
	@PostMapping("/trips/{tripId}/schedules/{schId}/edit")
	public String edit(@PathVariable("tripId") Long tripId, @PathVariable("schId") Long schId,
			ScheduleVO schedule, BindingResult binding, HttpSession session, RedirectAttributes rttr) {
		log.info("edit schedule......" + tripId + ", SCH_ID " + schId);

		String denied = checkEditable(tripId, session, rttr);
		if (denied != null) {
			return denied;
		}
		if (binding.hasErrors()) {
			rttr.addFlashAttribute("msg", "입력값이 올바르지 않습니다. 다시 입력해 주세요.");
			return redirectTrip(tripId);
		}

		try {
			schedule.setSchId(schId); // 주소의 번호를 쓴다
			service.update(tripId, schedule);
			rttr.addFlashAttribute("msg", "일정을 수정했습니다.");
		} catch (IllegalStateException e) {
			rttr.addFlashAttribute("msg", e.getMessage());
		}
		return redirectTrip(tripId);
	}

	// 일정 삭제: POST /trips/1/schedules/35/delete
	// (삭제 확인 창은 화면 _schedules.jsp의 JavaScript confirm()에서 처리)
	@PostMapping("/trips/{tripId}/schedules/{schId}/delete")
	public String delete(@PathVariable("tripId") Long tripId, @PathVariable("schId") Long schId,
			HttpSession session, RedirectAttributes rttr) {
		log.info("delete schedule......" + tripId + ", SCH_ID " + schId);

		String denied = checkEditable(tripId, session, rttr);
		if (denied != null) {
			return denied;
		}

		try {
			service.delete(tripId, schId);
			rttr.addFlashAttribute("msg", "일정을 삭제했습니다.");
		} catch (IllegalStateException e) {
			// 전체 공개 여행의 마지막 일정 보호 등
			rttr.addFlashAttribute("msg", e.getMessage());
		}
		return redirectTrip(tripId);
	}

	// 일정 순서 변경 (Ajax): POST /api/days/12/order   본문 [35, 33, 34]
	@PostMapping("/api/days/{dayId}/order")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> reorder(@PathVariable("dayId") Long dayId,
			@RequestBody List<Long> schIds, HttpSession session) {
		log.info("reorder......DAY_ID " + dayId + " → " + schIds);

		// LoginInterceptor가 먼저 401을 돌려주지만, 등록 여부와 관계없이 동작하도록 한 번 더 검사한다 (LikeController와 같음)
		Long userId = loginUserId(session);
		if (userId == null) {
			Map<String, Object> body = message("로그인이 필요합니다.");
			body.put("loginUrl", "/user/login");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
		}

		Long tripId = service.findTripIdByDay(dayId);
		if (tripId == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message("DAY를 찾을 수 없습니다."));
		}
		if (!canEdit(memberService.getRole(tripId, userId))) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(message(NO_PERMISSION));
		}

		try {
			List<ScheduleVO> list = service.reorder(dayId, schIds);

			// 화면이 시간 글자를 다시 그릴 수 있게 번호 · 순서 · 시간만 돌려준다
			List<Map<String, Object>> rows = new ArrayList<>();
			for (ScheduleVO s : list) {
				Map<String, Object> row = new HashMap<>();
				row.put("schId", s.getSchId());
				row.put("slotNo", s.getSlotNo());
				row.put("time", s.getTime());
				rows.add(row);
			}
			Map<String, Object> result = new HashMap<>();
			result.put("schedules", rows);
			return ResponseEntity.ok(result);
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message(e.getMessage()));
		}
	}

	// ===================== 도우미 =====================

	// 화면 요청의 권한 검사. 통과하면 null, 막히면 이동할 주소
	private String checkEditable(Long tripId, HttpSession session, RedirectAttributes rttr) {
		Long userId = loginUserId(session);
		if (userId == null) {
			return LOGIN_REQUIRED;
		}
		if (!canEdit(memberService.getRole(tripId, userId))) {
			rttr.addFlashAttribute("msg", NO_PERMISSION);
			return redirectTrip(tripId);
		}
		return null;
	}

	// 로그인한 회원 번호. 로그인하지 않았으면 null
	// (로그인하면 UserController(윤아연)가 세션 "loginUser"에 UserVO를 넣는다)
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

	// 일정을 편집할 수 있는 권한: OWNER, EDITOR
	private boolean canEdit(String role) {
		return "OWNER".equals(role) || "EDITOR".equals(role);
	}

	private String redirectTrip(Long tripId) {
		return "redirect:/trips/" + tripId;
	}

	private Map<String, Object> message(String text) {
		Map<String, Object> body = new HashMap<>();
		body.put("message", text);
		return body;
	}
}
