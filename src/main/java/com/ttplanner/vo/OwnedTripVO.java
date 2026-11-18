package com.ttplanner.vo;

import lombok.Data;

/**
 * 회원 탈퇴 화면(S-12)에 보여 줄 "내가 OWNER인 여행" 한 줄
 * 담당: 윤아연 (UC-024)
 */
@Data
public class OwnedTripVO {

	private Long tripId;
	private String title;
	private int otherMemberCount; // 나를 뺀 멤버 수 (STATUS = 'JOINED')
}
