package com.ttplanner.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

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
}
