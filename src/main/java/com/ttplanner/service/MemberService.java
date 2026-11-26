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

	// UC-018 권한 변경: 참여 중인 멤버 targetUserId의 권한을 role(EDITOR 또는 VIEWER)로 바꾼다
	// userId = 바꾸는 사람(로그인 회원). 이 여행의 OWNER만 바꿀 수 있다
	// 결과 문구 "○○ 님 권한을 VIEWER로 바꿨습니다."를 돌려줌 (이미 같은 권한이면 바꾸지 않고 "○○ 님은 이미 VIEWER입니다.")
	// OWNER 아님, 권한 잘못됨, 멤버 아님(초대 중 포함), 대상이 OWNER면 IllegalStateException
	public String changeRole(Long tripId, Long targetUserId, String role, Long userId);

	// UC-018 내보내기(5A): 참여 중인 멤버 targetUserId를 이 여행에서 뺀다 (TRIP_MEMBER 줄 삭제)
	// userId = 내보내는 사람(로그인 회원). 이 여행의 OWNER만 내보낼 수 있다
	// 결과 문구 "○○ 님을 내보냈습니다."를 돌려줌
	// OWNER 아님, 멤버 아님(초대 중 · 참여 요청 포함), 대상이 OWNER면 IllegalStateException
	public String kick(Long tripId, Long targetUserId, Long userId);

	// UC-018 OWNER 넘기기(5B): 참여 중인 멤버 targetUserId를 새 OWNER로, 지금 OWNER(userId)는 EDITOR로
	// userId = 넘기는 사람(로그인 회원). 이 여행의 OWNER만 넘길 수 있다
	// 결과 문구 "○○ 님이 새 OWNER가 되었습니다. △△ 님은 EDITOR입니다."를 돌려줌
	// OWNER 아님, 멤버 아님(초대 중 · 참여 요청 포함), 대상이 나 자신이면 IllegalStateException
	public String transferOwner(Long tripId, Long targetUserId, Long userId);

	// UC-019 여행 나가기: 로그인 회원 userId가 스스로 이 여행에서 빠진다 (TRIP_MEMBER 줄 삭제)
	// 참여 중인 EDITOR · VIEWER만 나갈 수 있다. OWNER는 UC-018로 넘긴 뒤 나갈 수 있다 (2A)
	// 결과 문구 "여행에서 나갔습니다."를 돌려줌
	// 멤버가 아니거나(로그인 안 함 · 초대 중 · 참여 요청 포함) OWNER면 IllegalStateException
	public String leave(Long tripId, Long userId);

	// 회원 탈퇴(UC-024, 윤아연 UserServiceImpl.withdraw) 연결: 이 회원을 모든 여행에서 뺀다 → 지운 줄 수
	// 참여 중(EDITOR · VIEWER), 받은 초대(INVITED), 보낸 참여 요청(REQUESTED) 줄을 지운다
	// OWNER 줄은 남긴다 (그 여행은 탈퇴 ①단계 TripService.deleteByOwner가 이미 삭제 표시). userId가 null이면 0
	public int deleteByUser(Long userId);
}
