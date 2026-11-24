package com.ttplanner.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.ttplanner.vo.CopyDayVO;

/**
 * 여행 복사 SQL (담당: 송유림, UC-020) ↔ src/main/resources/mapper/CopyMapper.xml
 * 메서드 이름 = XML의 id
 */
public interface CopyMapper {

	// ===== DAY 선택 화면 (S-09-1) =====

	// 여행의 DAY 목록 + 날짜, 일정 개수, 장소 일부 (DAY 번호 순, 삭제한 여행이면 빈 목록)
	List<CopyDayVO> getCopyDays(Long tripId);

	// 고른 DAY_ID들의 DAY 정보 (삭제한 여행의 DAY는 빠진다). 여행 → DAY 번호 순
	List<CopyDayVO> getSourceDays(@Param("dayIds") List<Long> dayIds);

	// 복사한 DAY로 새 여행을 만들 때 원본 제목 · 지역은 TripService.find(박준성)로 가져온다 (임시 SQL 2개는 지움)

	// ===== insertDays용 SQL 3개 =====
	// TODO(송유림): feature/yurim-insertdays에서 아래 3개를 DayMapper로 옮겨 DayService.insertDays를 만들고 여기서 지운다.

	// ① 넣을 자리(position) 뒤의 DAY 번호를 count만큼 민다
	int shiftDaysFrom(@Param("tripId") Long tripId, @Param("position") int position, @Param("count") int count);

	// ② 새 DAY 한 개 넣기. 실행 후 day.dayId에 새 DAY_ID가 들어간다 (selectKey)
	int insertCopyDay(CopyDayVO day);

	// ③ 원본 DAY의 일정을 새 DAY로 복사 → 복사한 일정 수
	//    넣을 여행이 전체 공개면 시간 · 장소가 모두 있는 일정만 복사한다
	int copySchedules(@Param("newDayId") Long newDayId, @Param("sourceDayId") Long sourceDayId,
			@Param("targetTripId") Long targetTripId);
}
