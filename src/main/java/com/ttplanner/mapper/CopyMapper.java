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
	// DAY 밀기 · 새 DAY · 일정 복사는 DayService.insertDays(DayMapper.xml)로 옮겼다
}
