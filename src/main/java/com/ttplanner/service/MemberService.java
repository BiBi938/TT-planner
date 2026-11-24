package com.ttplanner.service;

import java.util.List;

import com.ttplanner.vo.MemberVO;

/**
 * 여행 멤버 관리 (담당: 양지호, SFR-005 멤버 초대 및 권한 관리)
 * getRole은 팀 전원이 쓰는 연결 메서드(구성정의서 8장 나.) → 이름·매개변수 바꾸지 않음
 */
public interface MemberService {

	// 이 여행에서 이 회원의 권한: "OWNER" / "EDITOR" / "VIEWER"
	// 참여 중(JOINED)인 멤버만 인정한다.
	// 멤버가 아니거나, 초대 중 · 참여 요청 중이거나, 로그인하지 않았으면(userId == null) null
	public String getRole(Long tripId, Long userId);

	// UC-017 초대할 회원 검색: 닉네임에 keyword가 들어간 회원 (닉네임 순, 최대 20명, 탈퇴 회원 · 나 자신 제외)
	// userId = 검색하는 사람(로그인 회원). 이 여행의 OWNER만 검색할 수 있다
	// 일치하는 회원이 없으면 빈 목록 (5A 문구는 화면에서 표시)
	// OWNER가 아니거나(로그인 안 함 포함), 검색어가 비었으면 IllegalStateException
	public List<MemberVO> searchUsers(Long tripId, String keyword, Long userId);

	// UC-017 멤버 초대: targetUserId 회원을 role(EDITOR 또는 VIEWER)로 초대한다 (STATUS = 'INVITED', 수락 전에는 멤버 아님)
	// userId = 초대하는 사람(로그인 회원). 이 여행의 OWNER만 초대할 수 있다
	// 결과 문구 "○○ 님에게 EDITOR로 초대를 보냈습니다. 수락하면 멤버가 됩니다."를 돌려줌
	// OWNER 아님, 회원 안 고름(7A), 권한 잘못됨, 없는 회원, 이미 멤버(7B) · 초대 중(7C) · 참여 요청 중이면 IllegalStateException
	public String invite(Long tripId, Long targetUserId, String role, Long userId);

	// UC-017 여행 멤버 목록 (참여 중 · 초대 중 · 참여 요청 중 모두, OWNER → EDITOR → VIEWER → 요청 순)
	// 박준성 TripController.detail이 model "members"로 넣고 trip/_members.jsp가 사용 (구성정의서 8장 나.)
	// 멤버만 목록을 볼 수 있다는 검사는 detail.jsp(trip.member일 때만 include)에서 한다
	public List<MemberVO> findByTrip(Long tripId);
}
