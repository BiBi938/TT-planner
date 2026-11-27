package com.ttplanner.controller;

import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.ttplanner.interceptor.LoginInterceptor;
import com.ttplanner.service.StatsService;
import com.ttplanner.vo.UserVO;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j;

/**
 * 내 여행 통계 화면 (담당: 송유림, UC-022 · 화면설계서 S-10)
 *
 *   GET /settings/stats → StatsService.getStats(userId) → model "stats"(StatsVO) → settings/stats.jsp
 *
 * 사이드바 [설정] > [내 여행 통계] 링크(header.jsp)가 이 주소를 연다 (구성정의서 8장 나.).
 */
@Controller
@Log4j
@AllArgsConstructor
public class StatsController {

	/** 로그인하지 않았을 때 보낼 주소: 로그인 후 통계 화면으로 돌아온다 (UserController.loginForm의 back) */
	private static final String LOGIN_URL = "/user/login?required&back=/settings/stats";

	private StatsService service;

	@GetMapping("/settings/stats")
	public String stats(HttpSession session, Model model) {
		UserVO loginUser = (UserVO) session.getAttribute(LoginInterceptor.LOGIN_USER);

		// 로그인 검사: LoginInterceptor(servlet-context.xml에 등록됨)가 먼저 막으므로 보통 여기까지 오지 않는다.
		// 혹시 모를 경우를 위해 Controller에서도 한 번 더 막는다.
		if (loginUser == null) {
			return "redirect:" + LOGIN_URL;
		}

		model.addAttribute("stats", service.getStats(loginUser.getUserId()));
		return "settings/stats";
	}
}
