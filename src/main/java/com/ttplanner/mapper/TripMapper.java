package com.ttplanner.mapper;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.ttplanner.vo.TripVO;

/**
 * 여행 SQL (담당: 박준성) ↔ src/main/resources/mapper/TripMapper.xml
 */
public interface TripMapper {

	// ---------- 조회 ----------
	List<TripVO> getMyTrips(Long userId);

	TripVO read(Long tripId);

	TripVO readByShareToken(String shareToken);

	List<String> getRegions(Long tripId);

	// 참여(JOINED) 중인 멤버의 권한. 멤버가 아니면 null
	String getRole(@Param("tripId") Long tripId, @Param("userId") Long userId);

	// ---------- 여행 생성 ----------
	// TRIP_ID는 seq_trip으로 채워 trip.tripId에 넣는다. SCOPE는 항상 'PRIVATE'
	int insert(TripVO trip);

	int insertRegions(@Param("tripId") Long tripId, @Param("regions") List<String> regions);

	int insertOwner(@Param("tripId") Long tripId, @Param("userId") Long userId);

	// ---------- 수정 ----------
	int updateTitle(@Param("tripId") Long tripId, @Param("title") String title);

	int deleteRegions(Long tripId);

	int updateStartDate(@Param("tripId") Long tripId, @Param("startDate") Date startDate);

	int updateScope(@Param("tripId") Long tripId, @Param("scope") String scope);

	// 삭제는 DEL_YN = 'Y'로 표시만 한다
	int delete(Long tripId);

	// 회원이 OWNER인 여행을 모두 삭제 표시 (회원 탈퇴)
	int deleteByOwner(Long userId);

	// ---------- 일정 수 (공개 범위 · 기간 변경 검사) ----------
	int countSchedules(Long tripId);

	// 시간이나 장소가 비어 있는 일정 수
	int countIncompleteSchedules(Long tripId);

	// DAY 번호가 dayNo 이하 / 초과인 DAY의 일정 수
	int countSchedulesUpTo(@Param("tripId") Long tripId, @Param("dayNo") int dayNo);

	int countSchedulesAfter(@Param("tripId") Long tripId, @Param("dayNo") int dayNo);

	// DAY 추가 · 삭제는 DayService.syncDays(양지호)가 한다 (임시 SQL insertDay · deleteDaysAfter는 지움)
}
