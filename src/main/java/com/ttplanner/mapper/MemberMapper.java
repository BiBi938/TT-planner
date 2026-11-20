package com.ttplanner.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * 여행 멤버(TRIP_MEMBER) SQL (담당: 양지호) ↔ src/main/resources/mapper/MemberMapper.xml
 */
public interface MemberMapper {

	// 이 여행에서 이 회원의 권한: 'OWNER' / 'EDITOR' / 'VIEWER'
	// 참여 중(STATUS = 'JOINED')일 때만 돌려준다. 초대 중 · 참여 요청 중 · 멤버 아님 → null
	String getRole(@Param("tripId") Long tripId, @Param("userId") Long userId);
}
