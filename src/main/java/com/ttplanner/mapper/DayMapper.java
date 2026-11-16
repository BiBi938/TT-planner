package com.ttplanner.mapper;

import java.util.List;

import com.ttplanner.vo.DayVO;

/**
 * TRIP_DAY 테이블을 다루는 Mapper (SQL은 resources/mapper/DayMapper.xml에 있음)
 * 메서드 이름 = XML의 id
 */
public interface DayMapper {

	// 여행 하나의 DAY 목록 (DAY_NO 순서, 날짜·일정 개수 포함)
	public List<DayVO> findDays(Long tripId);

	// 여행 하나의 DAY 개수 (삭제된 여행이거나 없는 여행이면 0)
	public int countDays(Long tripId);

	// DAY 한 개 추가 (day에 tripId, dayNo를 넣어서 호출)
	public void insertDay(DayVO day);

	// ===== DAY 삭제용 (UC-011 1A~1D) =====

	// 여행의 공개 범위 'PUBLIC' / 'PRIVATE' (삭제된 여행이거나 없으면 null)
	public String getTripScope(Long tripId);

	// 여행 전체의 일정 개수 (모든 DAY 합계)
	public int countTripSchedules(Long tripId);

	// 아래 3개는 day에 tripId, dayNo를 넣어서 호출. 반환값 = 바뀐 행 수
	// ① 그 DAY의 일정 삭제
	public int deleteDaySchedules(DayVO day);

	// ② 그 DAY 삭제
	public int deleteDay(DayVO day);

	// ③ 뒤 DAY 번호를 하나씩 당김 (dayNo보다 큰 DAY_NO - 1)
	public int shiftDaysAfter(DayVO day);
}
