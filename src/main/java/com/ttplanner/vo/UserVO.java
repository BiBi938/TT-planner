package com.ttplanner.vo;

import lombok.Data;

/**
 * 회원 (USERS 테이블 한 행)
 * 담당: 윤아연 (UC-001~003, 023, 024)
 *
 * 로그인하면 이 객체가 세션에 "loginUser" 이름으로 저장된다 (비밀번호는 지운 상태).
 * 다른 팀원은 컨트롤러에서 다음처럼 꺼내 쓴다.
 *   UserVO loginUser = (UserVO) session.getAttribute("loginUser");
 *   Long userId = loginUser.getUserId();
 * JSP에서는 ${sessionScope.loginUser.nickname}
 */
@Data
public class UserVO {

	private Long userId;      // USER_ID
	private String nickname;  // NICKNAME (최대 20자, 다른 회원과 중복 불가)
	private String email;     // EMAIL (다른 회원과 중복 불가)
	private String password;  // PASSWORD (해시값으로 저장)
	private String delYn;     // DEL_YN ('Y' = 탈퇴)
}
