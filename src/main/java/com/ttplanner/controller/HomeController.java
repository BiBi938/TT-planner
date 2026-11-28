package com.ttplanner.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.extern.log4j.Log4j;

/**
 * 첫 주소 (공통)
 *
 * GET / → 내 여행 GET /trips (박준성, S-03 메인)로 보낸다.
 *   로그인함   → 내 여행 화면
 *   로그인 안 함 → /trips에서 LoginInterceptor(윤아연)가 막고 /user/login?required로 보냄
 *                → 브라우저 팝업 "로그인이 필요합니다." → 로그인 화면 → 로그인하면 다시 /trips로 돌아온다
 * ("/" 자체는 servlet-context.xml에서 로그인 검사 예외라 여기까지는 누구나 온다)
 */
@Controller
@Log4j
public class HomeController {

	@GetMapping("/")
	public String home() {
		log.info("home → /trips");
		return "redirect:/trips";
	}
}
