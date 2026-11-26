package com.ttplanner.vo;

import lombok.Data;

/**
 * 내가 OWNER인 여행에 온 참여 요청 한 건 (UC-027 참여 요청 처리)
 * TRIP_MEMBER(요청 줄, STATUS = 'REQUESTED') + TRIP(제목) + USERS(요청한 회원 닉네임)를 한 줄로 모은 것
 * 사용: 내 여행 화면 위쪽 "받은 참여 요청 n건" 목록 (trip/_invites.jsp) → [멤버 목록에서 처리]
 */
@Data
public class JoinRequestVO {

	private Long tripId;      // TRIP_ID (어느 여행에 온 요청인지)
	private String title;     // TRIP.TITLE (여행 제목)
	private Long userId;      // 요청한 회원 번호
	private String nickname;  // 요청한 회원 닉네임
}
