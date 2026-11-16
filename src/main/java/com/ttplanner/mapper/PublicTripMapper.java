package com.ttplanner.mapper;

import java.util.List;

import com.ttplanner.vo.PublicTripCriteria;
import com.ttplanner.vo.PublicTripVO;

/**
 * 공개 여행 검색 매퍼 (담당: 윤아연, UC-004)
 * SQL: src/main/resources/mapper/PublicTripMapper.xml
 */
public interface PublicTripMapper {

	/** 조건에 맞는 전체 공개 여행 한 페이지 (10건) */
	List<PublicTripVO> search(PublicTripCriteria cri);

	/** 조건에 맞는 전체 공개 여행 수 (페이지 번호 계산용) */
	int count(PublicTripCriteria cri);
}
