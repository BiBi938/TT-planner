package com.ttplanner.service;

import java.util.Date;
import java.util.List;

import com.ttplanner.vo.DayVO;

/**
 * DAY 관리 (UC-011 DAY 추가·삭제, UC-012 DAY 날짜 변경)
 * findDays는 박준성 님(UC-005 여행 열람)이 가져다 쓰는 팀 연결 메서드 → 이름·매개변수 바꾸지 않음
 */
public interface DayService {

	// 여행 하나의 DAY 목록 (DAY_NO 순서, 날짜·일정 개수 포함)
	public List<DayVO> findDays(Long tripId);

	// 마지막 DAY 뒤에 빈 DAY 추가 → 추가한 DAY(dayNo, dayDate = 새 종료일)를 돌려줌
	// 여행이 없거나 DAY가 30개면 IllegalStateException
	public DayVO addDay(Long tripId);

	// DAY 삭제 → 그 DAY의 일정도 함께 삭제, 뒤 DAY 번호를 하나씩 당김 (시작일은 그대로)
	// 여행·DAY가 없거나, DAY가 1개뿐이거나(1B), 전체 공개 여행의 일정이 0개가 되면(1D) IllegalStateException
	// dayId = DAY의 고유 번호(DAY_ID). DAY 번호(dayNo)는 삭제 때마다 바뀌므로 쓰지 않는다
	public void deleteDay(Long tripId, Long dayId);

	// 여행 기간(start ~ end)에 맞춰 DAY 개수를 맞춘다 (박준성 UC-008 여행 생성, UC-009 기간 변경에서 호출)
	// 늘면 뒤에 빈 DAY 추가, 줄면 뒤 DAY와 그 일정 삭제. TRIP.START_DATE는 바꾸지 않는다
	// 날짜가 비었거나 거꾸로이거나, 30일 초과, 여행 없음, 전체 공개 여행의 일정이 0개가 되면 IllegalStateException
	// 팀 연결 메서드(구성정의서 8장 나.) → 이름·매개변수 바꾸지 않음
	public void syncDays(Long tripId, Date start, Date end);

	// DAY 날짜 변경 (UC-012): 그 DAY가 newDate가 되도록 여행 시작일을 다시 계산한다
	// → 모든 DAY 날짜와 여행 기간이 같은 만큼 이동 (DAY 개수 · 번호 · 일정은 그대로)
	// 결과 문구 "DAY 2을 11/26로 옮겼습니다. 여행 기간이 11/25 ~ 11/27로 함께 바뀌었습니다."를 돌려줌
	// 날짜가 비었거나, 여행 · DAY가 없으면 IllegalStateException
	public String changeDayDate(Long tripId, Long dayId, Date newDate);
}
