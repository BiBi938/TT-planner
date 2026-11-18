package com.ttplanner.controller;

import java.util.Collections;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;

import com.ttplanner.service.PublicTripService;
import com.ttplanner.vo.PublicTripCriteria;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j;

/**
 * 공개 여행 검색 화면 S-07 (담당: 윤아연, UC-004 / SFR-007)
 *
 * GET /public/trips?keyword=제주&region=제주&days=4&page=1
 *  → PublicTripController.list()
 *  → PublicTripService.search() / count()
 *  → PublicTripMapper.xml의 search / count (V_TRIP_PERIOD, TRIP_REGION, TRIP_LIKE 사용)
 *  → public/list.jsp
 *
 * 로그인하지 않아도 이용할 수 있다 (servlet-context.xml에서 /public/** 은 LoginInterceptor 제외).
 * 카드를 누르면 여행 상세 /trips/{tripId}(박준성, UC-005)로 이동한다.
 */
@Controller
@Log4j
@AllArgsConstructor
public class PublicTripController {

	/** 아래쪽 페이지 번호를 한 번에 몇 개 보여 줄지 */
	private static final int PAGE_BLOCK = 10;

	private PublicTripService service;

	@GetMapping("/public/trips")
	public String list(PublicTripCriteria cri, BindingResult binding, Model model) {
		// days=abc처럼 숫자가 아닌 값이 오면 오류 화면 대신 기본값(전체, 1페이지)으로 검색한다
		if (binding.hasErrors()) {
			log.info("잘못된 검색 조건 무시: " + binding.getFieldErrors());
		}
		cri.normalize();

		int total = service.count(cri);
		int totalPages = Math.max(1, (total + PublicTripCriteria.PAGE_SIZE - 1) / PublicTripCriteria.PAGE_SIZE);
		if (cri.getPage() > totalPages) {
			cri.setPage(totalPages);
		}
		int startPage = ((cri.getPage() - 1) / PAGE_BLOCK) * PAGE_BLOCK + 1;
		int endPage = Math.min(startPage + PAGE_BLOCK - 1, totalPages);

		model.addAttribute("cri", cri);
		model.addAttribute("regions", PublicTripCriteria.REGIONS);
		model.addAttribute("trips", total == 0 ? Collections.emptyList() : service.search(cri));
		model.addAttribute("total", total);
		model.addAttribute("totalPages", totalPages);
		model.addAttribute("startPage", startPage);
		model.addAttribute("endPage", endPage);
		return "public/list";
	}
}
