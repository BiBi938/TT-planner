package com.ttplanner.service;

import com.ttplanner.vo.StatsVO;

// UC-022 내 여행 통계 (화면설계서 S-10)
// GET /settings/stats → StatsController → StatsService.getStats() → StatsMapper.xml → settings/stats.jsp
public interface StatsService {

	// 로그인한 사용자의 통계를 한 번에 돌려준다.
	// - 총 여행 수 · 지역: 내가 멤버(JOINED)인 여행 중 종료일이 지나고 삭제하지 않은 여행
	// - 좋아요 받은 수: 내가 OWNER인 여행이 받은 좋아요 합계 (삭제한 여행 포함)
	public StatsVO getStats(Long userId);
}
