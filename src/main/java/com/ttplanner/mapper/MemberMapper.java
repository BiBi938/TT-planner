package com.ttplanner.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.ttplanner.vo.InviteVO;
import com.ttplanner.vo.JoinRequestVO;
import com.ttplanner.vo.MemberVO;

/**
 * 여행 멤버(TRIP_MEMBER) SQL (담당: 양지호) ↔ src/main/resources/mapper/MemberMapper.xml
 */
public interface MemberMapper {

	// 이 여행에서 이 회원의 권한: 'OWNER' / 'EDITOR' / 'VIEWER'
	// 참여 중(STATUS = 'JOINED')일 때만 돌려준다. 초대 중 · 참여 요청 중 · 멤버 아님 → null
	String getRole(@Param("tripId") Long tripId, @Param("userId") Long userId);

	// UC-017 초대할 회원 검색: 닉네임에 keyword가 들어간 회원 (닉네임 순, 최대 20명)
	// 탈퇴 회원과 excludeUserId(검색하는 OWNER 자신)는 뺀다. 결과는 userId, nickname만 채워짐
	List<MemberVO> searchUsers(@Param("keyword") String keyword, @Param("excludeUserId") Long excludeUserId);

	// UC-017 이 여행에서 이 회원의 상태: 'JOINED' / 'INVITED' / 'REQUESTED'
	// getRole과 달리 JOINED만 보지 않는다. 줄이 없으면(멤버 · 초대 · 요청 모두 아님) null
	String getStatus(@Param("tripId") Long tripId, @Param("userId") Long userId);

	// UC-017 회원 닉네임 (탈퇴하지 않은 회원만). 없거나 탈퇴했으면 null
	String getNickname(Long userId);

	// UC-017 초대 보내기: TRIP_MEMBER에 STATUS = 'INVITED'로 한 줄 넣는다
	// role은 'EDITOR' 또는 'VIEWER' (검사는 Service에서). 넣은 줄 수(1)를 돌려준다
	int insertInvite(@Param("tripId") Long tripId, @Param("userId") Long userId, @Param("role") String role);

	// UC-017 여행 멤버 목록 (멤버 목록 창 _members.jsp): JOINED · INVITED · REQUESTED 모두
	// 정렬: OWNER → EDITOR → VIEWER → 권한 없음(참여 요청), 같은 권한 안에서는 닉네임 순. 탈퇴 회원 제외
	List<MemberVO> findByTrip(Long tripId);

	// UC-018 이 여행의 멤버 한 명 (닉네임 · 권한 · 상태를 한 번에). 줄이 없거나 탈퇴 회원이면 null
	// 권한 변경 · 내보내기 · OWNER 넘기기 전에 "대상이 참여 중인 멤버인가"를 확인할 때 쓴다
	MemberVO getMember(@Param("tripId") Long tripId, @Param("userId") Long userId);

	// UC-018 권한 바꾸기 (참여 중인 멤버만). 바꾼 줄 수(1 또는 0)를 돌려준다
	// 권한 변경(EDITOR ↔ VIEWER)과 OWNER 넘기기(강등 · 승격 두 번)에 함께 쓴다
	int updateRole(@Param("tripId") Long tripId, @Param("userId") Long userId, @Param("role") String role);

	// UC-018 이 여행에서 이 회원의 줄 지우기. 지운 줄 수(1 또는 0)를 돌려준다
	// 내보내기(UC-018), 여행 나가기(UC-019), 초대 거절(UC-025)에 함께 쓴다 → 누구를 지울지 검사는 Service에서
	int deleteMember(@Param("tripId") Long tripId, @Param("userId") Long userId);

	// UC-025 내가 받은 초대 목록 (응답 대기 INVITED만, 삭제된 여행 제외, 여행 번호 큰 순). 없으면 빈 목록
	List<InviteVO> findInvites(Long userId);

	// UC-025 이 여행에서 내가 받은 초대 한 건 (응답 대기 INVITED + 삭제 안 된 여행). 없으면 null
	// 수락 · 거절 전에 "아직 응답할 초대가 있는가" 확인 + 수락 문구의 여행 제목 · 권한에 쓴다
	InviteVO getInvite(@Param("tripId") Long tripId, @Param("userId") Long userId);

	// UC-025 초대 수락: 내 INVITED 줄의 STATUS를 'JOINED'로 (ROLE은 초대할 때 정한 그대로). 바꾼 줄 수(1 또는 0)
	// 거절은 새 SQL 없이 deleteMember를 쓴다
	int acceptInvite(@Param("tripId") Long tripId, @Param("userId") Long userId);

	// UC-026 멤버가 아닌 회원이 이 여행을 "볼 수 있는가" (참여 요청 가능 조건) → 1 또는 0
	// 삭제 안 된 여행 + (전체 공개 또는 공유 링크 token이 맞음). token이 null이면 전체 공개만 1
	// (TripService.canView를 쓰지 않는 이유: TripServiceImpl이 MemberService를 주입받아 순환 주입이 됨)
	int countRequestable(@Param("tripId") Long tripId, @Param("token") String token);

	// UC-026 참여 요청: TRIP_MEMBER에 (ROLE = NULL, STATUS = 'REQUESTED')로 한 줄 넣는다. 넣은 줄 수(1)
	int insertRequest(@Param("tripId") Long tripId, @Param("userId") Long userId);

	// UC-027 내가 OWNER인 여행들에 온 참여 요청 목록 (삭제된 여행 · 탈퇴한 요청자 제외, 여행 번호 큰 순 → 닉네임 순)
	// 없으면 빈 목록
	List<JoinRequestVO> findJoinRequests(Long ownerUserId);

	// UC-027 참여 요청 수락: REQUESTED 줄의 ROLE을 role(EDITOR · VIEWER)로 채우고 STATUS를 'JOINED'로 (한 문장에서 같이)
	// 바꾼 줄 수(1 또는 0). 거절은 새 SQL 없이 deleteMember를 쓴다
	int acceptRequest(@Param("tripId") Long tripId, @Param("userId") Long userId, @Param("role") String role);

	// UC-027 삭제되지 않은 여행인가 → 1 또는 0 (6B: 그사이 여행이 삭제됐으면 "권한이 없습니다.")
	int countActiveTrip(Long tripId);

	// 회원 탈퇴(UC-024, 윤아연) 연결: 이 회원의 TRIP_MEMBER 줄 중 OWNER가 아닌 것을 모두 지운다
	// 참여 중(EDITOR · VIEWER) · 받은 초대(INVITED) · 보낸 참여 요청(REQUESTED). 지운 줄 수를 돌려준다
	int deleteByUser(Long userId);
}
