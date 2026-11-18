package com.ttplanner.vo;

import java.util.Date;

import lombok.Data;

/**
 * 공개 여행 검색 결과 카드 한 장 (담당: 윤아연, UC-004)
 * 카드에는 여행 제목, 여행 기간, 일수, 좋아요 수를 표시한다.
 */
@Data
public class PublicTripVO {

	private Long tripId;
	private String title;
	private Date startDate;
	private Date endDate;   // V_TRIP_PERIOD.END_DATE
	private int dayCount;   // V_TRIP_PERIOD.DAY_COUNT (1 = 당일치기, 2 = 1박 2일 ...)
	private int likeCount;  // TRIP_LIKE 수

	/** "당일치기", "2박 3일" 같은 표시용 문구 (JSP: ${trip.periodLabel}) */
	public String getPeriodLabel() {
		if (dayCount <= 1) {
			return "당일치기";
		}
		return (dayCount - 1) + "박 " + dayCount + "일";
	}
}
