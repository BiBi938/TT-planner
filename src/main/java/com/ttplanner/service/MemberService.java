package com.ttplanner.service;

import java.util.List;

import com.ttplanner.vo.InviteVO;
import com.ttplanner.vo.JoinRequestVO;
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

	// UC-025 내가 받은 초대 목록 (응답 대기, 삭제된 여행 제외, 여행 번호 큰 순)
	// 내 여행 화면 위쪽 "받은 초대 n건"(trip/_invites.jsp). 없거나 로그인 안 했으면(userId == null) 빈 목록 (2A)
	// 박준성 TripController.list가 model "invites"로 넣는다 (연결 메서드)
	public List<InviteVO> findInvites(Long userId);

	// UC-025 초대 수락: 초대받은 권한의 멤버가 된다 (STATUS INVITED → JOINED)
	// 결과 문구 "'서울 여행' 여행의 VIEWER가 되었습니다."를 돌려줌
	// 응답할 초대가 없으면(이미 응답 · 다른 사람 · 삭제된 여행 · 로그인 안 함) IllegalStateException
	public String acceptInvite(Long tripId, Long userId);

	// UC-025 초대 거절(3A): 초대 줄을 지운다 → OWNER 멤버 목록의 "초대 중"도 사라짐
	// 결과 문구 "초대를 거절했습니다."를 돌려줌. 응답할 초대가 없으면 IllegalStateException
	public String rejectInvite(Long tripId, Long userId);

	// UC-026 이 여행과 이 회원의 관계: "JOINED" / "INVITED" / "REQUESTED" / 관계 없음 · 로그인 안 함 → null
	// 여행 상세 화면의 [참여 요청] 자리(trip/_join.jsp)가 버튼 · "참여 요청 중" · 초대 안내 중 무엇을 보일지 정할 때 쓴다
	// 박준성 TripController.fillDetail이 model "myStatus"로 넣는다 (연결 메서드)
	public String getStatus(Long tripId, Long userId);

	// UC-026 참여 요청: 멤버가 아닌 로그인 회원 userId가 이 여행의 OWNER에게 참여를 요청한다 (ROLE NULL, STATUS 'REQUESTED')
	// token = 공유 링크(/s/{token})로 열었을 때의 토큰 (비공개 여행도 요청 가능), 전체 공개 여행 화면에서는 null
	// 결과 문구 "참여를 요청했습니다. OWNER가 수락하면 멤버가 됩니다."를 돌려줌
	// 볼 수 없거나 삭제된 여행(6B), 이미 멤버(2B) · 요청 중(2C) · 초대받음(2D), 로그인 안 함이면 IllegalStateException
	public String requestJoin(Long tripId, Long userId, String token);

	// UC-027 내가 OWNER인 여행들에 온 참여 요청 목록 (내 여행 화면 위쪽 "받은 참여 요청 n건", trip/_invites.jsp)
	// 없거나 로그인 안 했으면 빈 목록. 박준성 TripController.list가 model "joinRequests"로 넣는다 (연결 메서드)
	public List<JoinRequestVO> findJoinRequests(Long userId);

	// UC-027 참여 요청 수락: targetUserId의 요청을 role(EDITOR 또는 VIEWER)로 받아 멤버로 만든다
	// userId = 처리하는 사람(로그인 회원). 이 여행의 OWNER만. 결과 문구 "○○ 님을 EDITOR로 받았습니다."
	// OWNER 아님 · 삭제된 여행(4A · 6B), 권한 잘못됨, 이미 처리된 요청(6A)이면 IllegalStateException
	public String acceptRequest(Long tripId, Long targetUserId, String role, Long userId);

	// UC-027 참여 요청 거절(5A): 요청 줄을 지운다 (요청한 회원은 다시 요청할 수 있음)
	// 결과 문구 "○○ 님의 참여 요청을 거절했습니다." 실패 조건은 acceptRequest와 같음
	public String rejectRequest(Long tripId, Long targetUserId, Long userId);

	// 회원 탈퇴(UC-024, 윤아연 UserServiceImpl.withdraw) 연결: 이 회원을 모든 여행에서 뺀다 → 지운 줄 수
	// 참여 중(EDITOR · VIEWER), 받은 초대(INVITED), 보낸 참여 요청(REQUESTED) 줄을 지운다
	// OWNER 줄은 남긴다 (그 여행은 탈퇴 ①단계 TripService.deleteByOwner가 이미 삭제 표시). userId가 null이면 0
	public int deleteByUser(Long userId);
}
