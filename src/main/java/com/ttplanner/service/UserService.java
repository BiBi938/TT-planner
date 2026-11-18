package com.ttplanner.service;

import java.util.List;
import java.util.Map;

import com.ttplanner.vo.OwnedTripVO;
import com.ttplanner.vo.UserVO;

/**
 * 회원 서비스 (담당: 윤아연, UC-001~003, 023, 024)
 *
 * 입력 검사 결과는 Map<항목 이름, 오류 문구>로 돌려준다.
 * 비어 있으면 성공이고, JSP는 항목 바로 아래에 ${errors.nickname}처럼 문구를 보여 준다.
 */
public interface UserService {

	/** 회원가입 (UC-001). 성공하면 빈 Map */
	Map<String, String> join(UserVO user, String passwordConfirm);

	/** 로그인 (UC-002). 성공하면 비밀번호를 지운 회원 정보, 실패하면 null */
	UserVO login(String email, String password);

	/** 닉네임 검사. 문제가 없으면 null, 있으면 오류 문구 (excludeUserId: 마이페이지에서 나 자신 제외) */
	String checkNickname(String nickname, Long excludeUserId);

	/** 이메일 검사. 문제가 없으면 null, 있으면 오류 문구 */
	String checkEmail(String email, Long excludeUserId);

	/** 비밀번호 규칙 검사 (8자리 이상, 영문 · 숫자 · 특수문자 각 1자 이상). 문제가 없으면 null */
	String checkPasswordRule(String password);

	/** 회원 정보 조회 (비밀번호 제외) */
	UserVO get(Long userId);

	/** 닉네임 · 이메일 변경 (UC-023). 성공하면 빈 Map */
	Map<String, String> updateInfo(UserVO user);

	/** 비밀번호 변경 (UC-023). 성공하면 빈 Map */
	Map<String, String> changePassword(Long userId, String currentPassword, String newPassword, String newPasswordConfirm);

	/** 비밀번호가 맞는지 확인 (UC-024 탈퇴 전 재확인) */
	boolean checkPassword(Long userId, String password);

	/** 내가 OWNER인 여행 목록 (UC-024 탈퇴 안내) */
	List<OwnedTripVO> getOwnedTrips(Long userId);

	/** 내가 멤버로 참여 중인 여행 수 (UC-024 탈퇴 안내) */
	int countJoinedTrips(Long userId);

	/** 회원 탈퇴 (UC-024). OWNER 여행 삭제 → 참여 여행에서 빠지기 → 회원 탈퇴 표시를 한 번에 처리 */
	boolean withdraw(Long userId);
}
