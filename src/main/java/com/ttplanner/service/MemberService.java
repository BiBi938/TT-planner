package com.ttplanner.service;

/**
 * 여행 멤버 관리 (담당: 양지호, SFR-005 멤버 초대 및 권한 관리)
 * getRole은 팀 전원이 쓰는 연결 메서드(구성정의서 8장 나.) → 이름·매개변수 바꾸지 않음
 */
public interface MemberService {

	// 이 여행에서 이 회원의 권한: "OWNER" / "EDITOR" / "VIEWER"
	// 참여 중(JOINED)인 멤버만 인정한다.
	// 멤버가 아니거나, 초대 중 · 참여 요청 중이거나, 로그인하지 않았으면(userId == null) null
	public String getRole(Long tripId, Long userId);
}
