package com.ttplanner.vo;

import java.util.Date;

import lombok.Data;

/**
 * 여행의 DAY 한 개 (TRIP_DAY 테이블 한 줄 + 계산한 값)
 * 사용: DayMapper.findDays(tripId) → DayService.findDays(tripId) → 여행 상세 화면(UC-005)
 */
@Data
public class DayVO {

	private Long dayId;        // DAY_ID
	private Long tripId;       // TRIP_ID
	private int dayNo;         // DAY_NO : DAY 번호 1, 2, 3 ...

	// 아래 두 값은 테이블 컬럼이 아니라 SQL에서 계산해서 넣는다
	private Date dayDate;      // DAY 날짜 = START_DATE + DAY_NO - 1 (DB에 저장하지 않음)
	private int scheduleCount; // 이 DAY의 일정 개수 (DAY 삭제 확인 창 1C, 마지막 일정 보호 1D에 사용)
}
