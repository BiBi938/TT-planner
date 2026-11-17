package com.ttplanner.vo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.Data;

/**
 * 여행 (TRIP 테이블 한 행 + SQL에서 계산한 값)
 * 담당: 박준성 (UC-005, 007, 008, 009, 010, 015, 016)
 *
 * 사용: TripMapper.xml → TripServiceImpl → TripController → trip/list.jsp, trip/detail.jsp
 */
@Data
public class TripVO {

	// ---------- TRIP 컬럼 ----------
	private Long tripId;         // TRIP_ID
	private String title;        // TITLE (입력 50자 이하)
	private Date startDate;      // START_DATE
	private String scope;        // SCOPE : 'PRIVATE' 비공개 / 'PUBLIC' 전체 공개
	private String shareToken;   // SHARE_TOKEN : 여행을 만들 때 UUID로 생성, 재발급 없음
	private String delYn;        // DEL_YN
	private Date createdAt;      // CREATED_AT : 작성일

	// ---------- 테이블 컬럼이 아니라 SQL에서 계산해서 넣는 값 ----------
	private Date endDate;        // 종료일 = START_DATE + DAY 개수 - 1 (V_TRIP_PERIOD)
	private int dayCount;        // DAY 개수 (V_TRIP_PERIOD)
	private String myRole;       // 로그인 회원의 권한 OWNER / EDITOR / VIEWER (멤버가 아니면 null)
	private String regionNames;  // 내 여행 카드용 "서울, 부산"
	private String ownerNickname; // OWNER 닉네임
	private int memberCount;     // 참여(JOINED) 멤버 수 (OWNER 포함)

	private List<String> regions = new ArrayList<>(); // 여행 지역 목록 (TRIP_REGION)

	/** JSP: ${trip.owner} — 로그인 회원이 OWNER인지 */
	public boolean isOwner() {
		return "OWNER".equals(myRole);
	}

	/** JSP: ${trip.member} — 로그인 회원이 이 여행의 멤버인지 */
	public boolean isMember() {
		return myRole != null;
	}

	/** JSP: ${trip.publicScope} — 전체 공개 여행인지 */
	public boolean isPublicScope() {
		return "PUBLIC".equals(scope);
	}

	/** 비공개 / 전체 공개 */
	public String getScopeLabel() {
		return isPublicScope() ? "전체 공개" : "비공개";
	}

	/** 작성일 "2026.04.25" */
	public String getCreatedDateText() {
		return createdAt == null ? "" : new SimpleDateFormat("yyyy.MM.dd").format(createdAt);
	}

	/** 여행 기간 "2026.06.20 ~ 06.23" (해가 바뀌면 "2025.12.30 ~ 2026.01.02") */
	public String getPeriodText() {
		if (startDate == null || endDate == null) {
			return "";
		}
		String start = new SimpleDateFormat("yyyy.MM.dd").format(startDate);
		boolean sameYear = new SimpleDateFormat("yyyy").format(startDate)
				.equals(new SimpleDateFormat("yyyy").format(endDate));
		String end = new SimpleDateFormat(sameYear ? "MM.dd" : "yyyy.MM.dd").format(endDate);
		return start + " ~ " + end;
	}

	/** 일수 "3박 4일", DAY가 1개면 "당일치기" */
	public String getDurationText() {
		if (dayCount <= 1) {
			return "당일치기";
		}
		return (dayCount - 1) + "박 " + dayCount + "일";
	}

	/** 멤버 요약 "홍길동 외 2명" (멤버가 OWNER 혼자면 "홍길동") */
	public String getMemberSummary() {
		if (ownerNickname == null) {
			return "";
		}
		return memberCount <= 1 ? ownerNickname : ownerNickname + " 외 " + (memberCount - 1) + "명";
	}

	/** 기간 입력칸(type=date) 값 "2026-06-20" */
	public String getStartDateInput() {
		return startDate == null ? "" : new SimpleDateFormat("yyyy-MM-dd").format(startDate);
	}

	public String getEndDateInput() {
		return endDate == null ? "" : new SimpleDateFormat("yyyy-MM-dd").format(endDate);
	}
}
