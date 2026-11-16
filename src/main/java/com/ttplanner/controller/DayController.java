package com.ttplanner.controller;

import java.text.SimpleDateFormat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ttplanner.service.DayService;
import com.ttplanner.vo.DayVO;

import lombok.Setter;
import lombok.extern.log4j.Log4j;

/**
 * DAY 추가·삭제 요청 (UC-011)
 * 처리 후 여행 상세 화면(/trips/{tripId}, 박준성)으로 이동하고,
 * 결과 문구는 flash "msg"로 전달 → trip/_days.jsp에서 출력
 */
@Controller
@Log4j
@RequestMapping("/trips/{tripId}/days")
public class DayController {

	@Setter(onMethod_ = @Autowired)
	private DayService service;

	// DAY 추가: POST /trips/1/days
	@PostMapping("")
	public String add(@PathVariable("tripId") Long tripId, RedirectAttributes rttr) {
		log.info("add day......" + tripId);

		// TODO(윤아연): 세션 로그인 정보 확정 후 권한 검사 (OWNER, EDITOR만)

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

	// DAY 삭제: POST /trips/1/days/2/delete
	// (일정이 있을 때의 확인 창 1C는 화면 _days.jsp의 JavaScript confirm()에서 처리)
	@PostMapping("/{dayNo}/delete")
	public String delete(@PathVariable("tripId") Long tripId,
						 @PathVariable("dayNo") int dayNo,
						 RedirectAttributes rttr) {
		log.info("delete day......" + tripId + ", DAY " + dayNo);

		// TODO(윤아연): 세션 로그인 정보 확정 후 권한 검사 (OWNER, EDITOR만)

		try {
			service.deleteDay(tripId, dayNo);
			rttr.addFlashAttribute("msg", "뒤의 DAY가 하루씩 당겨지고 종료일이 하루 줄었습니다.");
		} catch (IllegalStateException e) {
			// Service에서 막은 경우 (1B DAY 1개, DAY 없음, 1D 마지막 일정 보호)
			rttr.addFlashAttribute("msg", e.getMessage());
		}

		return "redirect:/trips/" + tripId;
	}
}
