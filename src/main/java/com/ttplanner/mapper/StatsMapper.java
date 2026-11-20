package com.ttplanner.mapper;

import java.util.List;

import com.ttplanner.vo.RegionStatVO;

/**
 * 내 여행 통계 SQL (담당: 송유림, UC-022) ↔ src/main/resources/mapper/StatsMapper.xml
 * 메서드 이름 = XML의 id. 세 메서드 모두 로그인한 회원 번호(userId) 하나만 받는다.
 *
 * [집계 대상 여행] 통계의 "끝난 여행" (StatsService 주석과 같은 기준)
 *   TRIP_MEMBER.USER_ID = userId, STATUS = 'JOINED'
 *   + TRIP.DEL_YN = 'N'
 *   + V_TRIP_PERIOD.END_DATE < TRUNC(SYSDATE)
 */
public interface StatsMapper {

	// 총 여행 수: 집계 대상 여행 수 (없으면 0)
	int countEndedTrips(Long userId);

	// 지역별 여행 횟수: 집계 대상 여행의 TRIP_REGION을 지역별로 센다 (GROUP BY REGION)
	// 지역이 여러 개인 여행은 지역마다 1회. 결과는 횟수가 많은 순 (SQL 별칭 REGION, TRIP_COUNT)
	// 횟수가 같은 지역끼리의 순서(TripService.REGIONS 순)는 StatsServiceImpl에서 정한다
	List<RegionStatVO> getRegionStats(Long userId);

	// 좋아요 받은 수: 내가 OWNER(ROLE = 'OWNER', STATUS = 'JOINED')인 여행이 받은 TRIP_LIKE 수
	// 삭제한 여행(DEL_YN = 'Y')도 포함하고, 종료일 조건은 없다 (없으면 0)
	int countReceivedLikes(Long userId);
}
