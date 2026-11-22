package com.ttplanner.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.ttplanner.vo.ScheduleVO;

/**
 * SCHEDULE 테이블을 다루는 Mapper (담당: 송유림, UC-013)
 * SQL은 src/main/resources/mapper/ScheduleMapper.xml에 있다. 메서드 이름 = XML의 id
 * 삭제된 여행(TRIP.DEL_YN = 'Y')의 일정은 조회하지 않는다.
 */
public interface ScheduleMapper {

	// 여행 전체 일정: DAY 번호 → SLOT_NO 순 (dayNo 포함)
	public List<ScheduleVO> findByTrip(Long tripId);

	// DAY 하나의 일정: SLOT_NO 순
	public List<ScheduleVO> findByDay(Long dayId);

	// 일정 한 개. 이 여행의 일정이 아니거나 삭제된 여행이면 null
	// (주소의 schId만 바꿔서 다른 여행의 일정을 고치는 것을 막기 위해 tripId도 함께 확인한다)
	public ScheduleVO findOne(@Param("tripId") Long tripId, @Param("schId") Long schId);

	// DAY가 속한 여행 번호. 없는 DAY이거나 삭제된 여행이면 null
	public Long findTripIdByDay(Long dayId);

	// 여행의 공개 범위 'PRIVATE' / 'PUBLIC'. 없거나 삭제된 여행이면 null
	public String getTripScope(Long tripId);

	// 여행 전체의 일정 개수 (마지막 일정 보호에 사용)
	public int countByTrip(Long tripId);

	// 일정 추가. 실행 후 schedule.getSchId()에 새 번호가 들어 있다 (selectKey)
	public int insert(ScheduleVO schedule);

	// 시간 · 장소 · 위치 · 설명 수정 (SLOT_NO는 바꾸지 않음)
	public int update(ScheduleVO schedule);

	// SLOT_NO만 바꾸기 (추가 · 수정 · 삭제 후 1, 2, 3 ... 으로 다시 매길 때)
	public int updateSlotNo(@Param("schId") Long schId, @Param("slotNo") int slotNo);

	// SLOT_NO와 TIME 함께 바꾸기 (순서 변경: 시간은 자리에 남고 장소 · 설명만 움직인다)
	public int updateSlotNoAndTime(@Param("schId") Long schId, @Param("slotNo") int slotNo,
			@Param("time") String time);

	// 일정 삭제
	public int delete(Long schId);
}
