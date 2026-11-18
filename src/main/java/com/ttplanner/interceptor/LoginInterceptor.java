package com.ttplanner.interceptor;

import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import lombok.extern.log4j.Log4j;

/**
 * 로그인 검사 (담당: 윤아연, UC-002 · SFR-001)
 *
 * servlet-context.xml에 등록되어 모든 요청보다 먼저 실행된다.
 * 로그인 없이 쓸 수 있는 주소(로그인, 회원가입, 공개 여행 검색, 공유 링크 /s/** 등)는
 * servlet-context.xml의 exclude-mapping에 적혀 있어서 여기까지 오지 않는다.
 *
 * 흐름 예) 비로그인 사용자가 GET /settings/mypage
 *   → preHandle(): 세션에 loginUser 없음
 *   → 돌아올 주소 "/settings/mypage"를 세션 backUrl에 저장
 *   → redirect:/user/login?required → 로그인 화면에 "로그인이 필요합니다." 표시
 *   → 로그인 성공 시 UserController가 backUrl로 돌려보낸다.
 */
@Log4j
public class LoginInterceptor extends HandlerInterceptorAdapter {

	/** 세션에 저장하는 로그인 회원 (UserVO, 비밀번호 제외) */
	public static final String LOGIN_USER = "loginUser";

	/** 로그인 후 돌아갈 주소 */
	public static final String BACK_URL = "backUrl";

	/**
	 * 여행 상세 GET /trips/{번호}는 로그인 없이도 연다 (전체 공개 여행 열람, UC-004 → UC-005).
	 * 볼 수 있는 여행인지(공개 범위 · 멤버 여부)는 TripController(박준성)가 검사한다.
	 */
	private static final Pattern PUBLIC_TRIP_DETAIL = Pattern.compile("^/trips/[0-9]+$");

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		HttpSession session = request.getSession(false);
		if (session != null && session.getAttribute(LOGIN_USER) != null) {
			return true; // 로그인한 상태
		}

		String contextPath = request.getContextPath();
		String path = request.getRequestURI().substring(contextPath.length());

		if ("GET".equals(request.getMethod()) && PUBLIC_TRIP_DETAIL.matcher(path).matches()) {
			return true;
		}

		log.info("로그인 필요: " + request.getMethod() + " " + path);

		// Ajax 요청(좋아요 등)은 화면 이동 대신 401과 JSON을 돌려준다.
		// 받는 쪽 JS 예) if (xhr.status === 401) location.href = '/user/login?back=' + encodeURIComponent(location.pathname);
		if (isAjax(request, path)) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setContentType("application/json;charset=UTF-8");
			response.getWriter().write("{\"message\":\"로그인이 필요합니다.\",\"loginUrl\":\""
					+ contextPath + "/user/login\"}");
			return false;
		}

		// 화면 요청이면 로그인 후 돌아올 주소를 기억해 둔다 (GET만. POST는 다시 보낼 수 없으므로 저장하지 않음)
		if ("GET".equals(request.getMethod())) {
			String query = request.getQueryString();
			request.getSession().setAttribute(BACK_URL, query == null ? path : path + "?" + query);
		}
		response.sendRedirect(contextPath + "/user/login?required");
		return false;
	}

	private boolean isAjax(HttpServletRequest request, String path) {
		return path.startsWith("/api/")
				|| "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
	}
}
