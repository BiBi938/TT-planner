package com.ttplanner.vo;

import java.util.List;

import lombok.Data;

// 내 여행 통계 화면(S-10)에 보여 줄 값을 한 번에 담아 JSP로 넘긴다.
@Data
public class StatsVO {

	private int tripCount;    // 총 여행 수: 내가 멤버인, 종료일이 지난, 삭제하지 않은 여행
	private int regionCount;  // 방문 지역 수: 중복 제외 (regions 개수)
	private int likeCount;    // 좋아요 받은 수: 내가 OWNER인 여행, 삭제한 여행 포함
	private List<RegionStatVO> regions; // 지역별 여행 횟수, 많은 순 (원그래프 · 순위)
}
