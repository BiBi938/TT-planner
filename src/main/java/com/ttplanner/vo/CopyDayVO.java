package com.ttplanner.vo;

import java.util.Date;

import lombok.Data;

/**
 * 여행 복사(UC-020)에서 쓰는 DAY 한 개 (담당: 송유림)
 *
 * 사용 1) DAY 선택 화면(S-09-1): CopyMapper.getCopyDays(tripId) → trip/copy.jsp
 *         각 줄에 날짜, 일정 개수, 장소 일부를 보여 준다.
 * 사용 2) DAY 넣기 전 확인: CopyMapper.getSourceDays(dayIds) → 복사한 DAY가 남아 있는지, 원본 여행 · DAY 번호 순서
 *         (실제로 넣는 일은 DayService.insertDays가 한다)
 */
@Data
public class CopyDayVO {

	private Long dayId;        // DAY_ID
	private Long tripId;       // TRIP_ID
	private int dayNo;         // DAY_NO

	// 아래는 테이블 컬럼이 아니라 SQL에서 계산해서 넣는 값
	private Date dayDate;      // DAY 날짜 = START_DATE + DAY_NO - 1
	private int scheduleCount; // 이 DAY의 일정 개수
	private String placeNames; // 장소 일부: 일정 순서대로 앞의 3곳 (예: "성산일출봉, 섭지코지, 우도")
}
