package com.ttplanner.service;

import com.ttplanner.vo.StatsVO;

/**
 * 내 여행 통계 (담당: 송유림, UC-022 · 화면설계서 S-10)
 *
 * 흐름: GET /settings/stats → StatsController.stats() → StatsService.getStats(userId)
 *       → StatsMapper.xml (집계 SQL) → model "stats" → settings/stats.jsp
 */
public interface StatsService {

	/**
	 * 로그인한 회원의 여행 통계를 한 번에 돌려준다.
	 * userId는 로그인한 회원 번호 (null이면 IllegalArgumentException. 로그인 검사는 Controller에서 한다)
	 *
	 * [집계 대상 여행] 아래를 모두 만족하는 여행
	 *   - 내가 멤버: TRIP_MEMBER.STATUS = 'JOINED' (만든 사람도 OWNER로 JOINED)
	 *   - 종료일이 지남: V_TRIP_PERIOD.END_DATE < 오늘 (오늘 끝나는 여행은 아직 세지 않는다)
	 *   - 삭제하지 않음: TRIP.DEL_YN = 'N'
	 *
	 * [StatsVO에 담는 값]
	 *   - tripCount   : 집계 대상 여행 수
	 *   - regions     : 집계 대상 여행의 지역별 횟수. 지역이 여러 개인 여행은 지역마다 1회
	 *                   횟수가 많은 순, 같으면 TripService.REGIONS 순서 (서울, 부산, …)
	 *   - regionCount : 방문 지역 수 (regions 개수 = 중복 제외)
	 *   - likeCount   : 내가 OWNER인 여행이 받은 좋아요 합계. 삭제한 여행도 포함한다
	 *                   (여행을 삭제해도 OWNER 행과 TRIP_LIKE는 남는다, schema.sql)
	 *
	 * 끝난 여행이 없으면 tripCount = 0, regionCount = 0, regions = 빈 목록 (UC-022 2A)
	 * likeCount는 끝난 여행 여부와 관계없이 센다.
	 */
	StatsVO getStats(Long userId);
}
