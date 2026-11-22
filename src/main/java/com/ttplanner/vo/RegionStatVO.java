package com.ttplanner.vo;

import lombok.Data;

// 지역별 여행 횟수 SQL(GROUP BY)의 결과 한 줄
// SQL 별칭 REGION, TRIP_COUNT가 자동 매핑으로 region, tripCount에 들어간다.
@Data
public class RegionStatVO {

	private String region;  // TRIP_REGION.REGION (예: 제주)
	private int tripCount;  // 이 지역을 간 여행 수
}
