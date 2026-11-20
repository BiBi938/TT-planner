package com.ttplanner.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ttplanner.mapper.StatsMapper;
import com.ttplanner.vo.RegionStatVO;
import com.ttplanner.vo.StatsVO;

import lombok.Setter;
import lombok.extern.log4j.Log4j;

/**
 * 내 여행 통계 서비스 구현 (담당: 송유림, UC-022)
 *
 * StatsMapper의 집계 SQL 3개를 불러 StatsVO 하나에 담는다.
 *   tripCount   ← mapper.countEndedTrips(userId)
 *   regions     ← mapper.getRegionStats(userId) → 같은 횟수끼리 지역 순서 정렬
 *   regionCount ← regions.size() (SQL 없이 계산)
 *   likeCount   ← mapper.countReceivedLikes(userId)
 *
 * 조회만 하므로 @Transactional은 붙이지 않는다.
 */
@Log4j
@Service
public class StatsServiceImpl implements StatsService {

	@Setter(onMethod_ = @Autowired)
	private StatsMapper mapper;

	/**
	 * 지역 순위 정렬 기준
	 *   1) 여행 횟수가 많은 순
	 *   2) 횟수가 같으면 TripService.REGIONS 순서 (서울, 부산, 대구 …)
	 * 예) [제주 2, 부산 2, 서울 1] → [부산 2, 제주 2, 서울 1]
	 * REGIONS에 없는 지역은 들어올 수 없지만(schema.sql ck_trip_region_region), 혹시 오면 맨 뒤로 보낸다.
	 */
	private static final Comparator<RegionStatVO> REGION_ORDER =
			Comparator.comparingInt(RegionStatVO::getTripCount).reversed()
					.thenComparingInt(StatsServiceImpl::regionIndex);

	@Override
	public StatsVO getStats(Long userId) {
		if (userId == null) {
			throw new IllegalArgumentException("로그인한 회원 번호가 필요합니다.");
		}

		List<RegionStatVO> regions = mapper.getRegionStats(userId);
		regions.sort(REGION_ORDER);

		StatsVO stats = new StatsVO();
		stats.setTripCount(mapper.countEndedTrips(userId));
		stats.setRegions(regions);
		stats.setRegionCount(regions.size());
		stats.setLikeCount(mapper.countReceivedLikes(userId));

		log.info("getStats: userId=" + userId + ", " + stats);
		return stats;
	}

	/** TripService.REGIONS 안의 위치 (서울 = 0, 부산 = 1 …). 목록에 없으면 맨 뒤 */
	private static int regionIndex(RegionStatVO stat) {
		int index = TripService.REGIONS.indexOf(stat.getRegion());
		return index < 0 ? Integer.MAX_VALUE : index;
	}
}
