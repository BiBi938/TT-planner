package com.ttplanner.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

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
	// 실행 후 day.getDayId()에 새 DAY_ID가 들어 있다 (selectKey, insertDays에서 일정 복사에 사용)
	public void insertDay(DayVO day);

	// ===== DAY 삭제용 (UC-011 1A~1D) =====

	// 여행의 공개 범위 'PUBLIC' / 'PRIVATE' (삭제된 여행이거나 없으면 null)
	public String getTripScope(Long tripId);

	// 여행 전체의 일정 개수 (모든 DAY 합계)
	public int countTripSchedules(Long tripId);

	// 아래 3개는 findDays()로 찾은 DayVO를 그대로 넘겨서 호출. 반환값 = 바뀐 행 수
	// ① 그 DAY의 일정 삭제 (dayId 사용)
	public int deleteDaySchedules(DayVO day);

	// ② 그 DAY 삭제 (dayId 사용)
	public int deleteDay(DayVO day);

	// ③ 뒤 DAY 번호를 하나씩 당김 (tripId, dayNo 사용: dayNo보다 큰 DAY_NO - 1)
	public int shiftDaysAfter(DayVO day);

	// ===== 여행 기간에 맞춰 DAY 개수 맞추기 (syncDays: 박준성 UC-008 여행 생성, UC-009 기간 변경) =====
	// 값이 2개라 @Param으로 이름표를 붙인다. "dayNo보다 뒤" = DAY_NO > dayNo

	// dayNo보다 뒤에 있는 DAY들의 일정 개수 (기간을 줄이면 지워질 일정 수)
	public int countSchedulesAfter(@Param("tripId") Long tripId, @Param("dayNo") int dayNo);

	// dayNo보다 뒤에 있는 DAY들의 일정 삭제 (DAY보다 먼저 지운다)
	public int deleteSchedulesAfter(@Param("tripId") Long tripId, @Param("dayNo") int dayNo);

	// dayNo보다 뒤에 있는 DAY 삭제 (뒤에서 잘라 내므로 남는 DAY의 번호는 그대로)
	public int deleteDaysAfter(@Param("tripId") Long tripId, @Param("dayNo") int dayNo);

	// ===== 복사한 DAY 넣기 (insertDays: 송유림 UC-020 여행 복사) =====

	// ① position보다 뒤에 있는 DAY의 번호를 count만큼 민다 (DAY_NO > position → DAY_NO + count)
	public int shiftDaysFrom(@Param("tripId") Long tripId, @Param("position") int position, @Param("count") int count);

	// ③ 원본 DAY(sourceDayId)의 일정을 새 DAY(newDayId)로 복사 → 복사한 일정 수
	//    넣을 여행(targetTripId)이 전체 공개면 시간 · 장소가 모두 있는 일정만 복사한다
	public int copySchedules(@Param("newDayId") Long newDayId, @Param("sourceDayId") Long sourceDayId,
			@Param("targetTripId") Long targetTripId);
}
