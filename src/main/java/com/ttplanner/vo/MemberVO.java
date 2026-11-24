package com.ttplanner.vo;

import lombok.Data;

/**
 * 여행 멤버 한 명 (TRIP_MEMBER 한 줄 + USERS의 닉네임)
 * 사용: UC-017 초대할 회원 검색 결과, 멤버 목록(_members.jsp)
 * 회원 검색 결과에서는 userId, nickname만 채워진다 (아직 이 여행의 멤버가 아닐 수 있으므로)
 */
@Data
public class MemberVO {

	private Long tripId;       // TRIP_ID
	private Long userId;       // USER_ID
	private String nickname;   // USERS.NICKNAME (TRIP_MEMBER에는 없고 USERS에서 가져옴)
	private String role;       // ROLE : 'OWNER' / 'EDITOR' / 'VIEWER' (참여 요청 중이면 null)
	private String status;     // STATUS : 'JOINED' 참여 중 / 'INVITED' 초대 중 / 'REQUESTED' 참여 요청 중
}
