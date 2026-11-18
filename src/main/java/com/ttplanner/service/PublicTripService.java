package com.ttplanner.service;

import java.util.List;

import com.ttplanner.vo.PublicTripCriteria;
import com.ttplanner.vo.PublicTripVO;

/**
 * 공개 여행 검색 서비스 (담당: 윤아연, UC-004)
 */
public interface PublicTripService {

	List<PublicTripVO> search(PublicTripCriteria cri);

	int count(PublicTripCriteria cri);
}
