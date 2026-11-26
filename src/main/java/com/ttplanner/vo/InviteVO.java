package com.ttplanner.vo;

import lombok.Data;

/**
 * 내가 받은 초대 한 건 (UC-025 초대 응답)
 * TRIP_MEMBER(내 INVITED 줄) + TRIP(제목) + 그 여행의 OWNER 닉네임(TRIP_MEMBER · USERS)을 한 줄로 모은 것
 * 사용: 내 여행 화면 위쪽 "받은 초대 n건" 목록 (trip/_invites.jsp), 수락 문구 "'제목' 여행의 EDITOR가 되었습니다."
 */
@Data
public class InviteVO {

	private Long tripId;          // TRIP_ID
	private String title;         // TRIP.TITLE (여행 제목)
	private String ownerNickname; // 초대한 OWNER의 닉네임 (SQL에서 as owner_nickname)
	private String role;          // 초대받은 권한 : 'EDITOR' / 'VIEWER' (수락하면 이 권한의 멤버가 됨)
}
