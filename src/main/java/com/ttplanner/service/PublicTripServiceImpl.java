package com.ttplanner.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ttplanner.mapper.PublicTripMapper;
import com.ttplanner.vo.PublicTripCriteria;
import com.ttplanner.vo.PublicTripVO;

import lombok.Setter;
import lombok.extern.log4j.Log4j;

/**
 * 공개 여행 검색 서비스 구현 (담당: 윤아연, UC-004)
 *
 * 좋아요 수는 카드 10장을 한 번의 SQL로 가져오려고 PublicTripMapper.xml에서 직접 센다.
 * (LikeService.count(김민정)를 카드마다 부르면 SQL이 10번 더 나간다.)
 */
@Log4j
@Service
public class PublicTripServiceImpl implements PublicTripService {

	@Setter(onMethod_ = @Autowired)
	private PublicTripMapper mapper;

	@Override
	public List<PublicTripVO> search(PublicTripCriteria cri) {
		log.info("search: " + cri);
		return mapper.search(cri);
	}

	@Override
	public int count(PublicTripCriteria cri) {
		return mapper.count(cri);
	}
}
