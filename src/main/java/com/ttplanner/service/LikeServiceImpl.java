package com.ttplanner.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.ttplanner.mapper.LikeMapper;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j;

/**
 * 좋아요 서비스 구현체 (담당: 김민정, UC-021)
 *
 * toggle 순서:
 *   1. deleteLike 시도 → 지운 행이 있으면 → 좋아요 취소 (UC-021 2A)
 *   2. 없으면 insertLike → 성공이면 좋아요 추가
 *   3. insertLike에서 DuplicateKeyException → 동시 클릭으로 이미 삽입됨 → 좋아요로 처리
 *   행이 1개씩 바뀌므로 @Transactional 불필요
 */
@Service
@AllArgsConstructor
@Log4j
public class LikeServiceImpl implements LikeService {

    private LikeMapper mapper;

    @Override
    public int count(Long tripId) {
        return mapper.count(tripId);
    }

    @Override
    public boolean isLiked(Long tripId, Long userId) {
        if (userId == null) {
            return false; // 비로그인은 DB 조회 없이 false (UC-021 2B 대비)
        }
        return mapper.isLiked(tripId, userId) > 0;
    }

    @Override
    public boolean toggle(Long tripId, Long userId) {
        // 1A 서버 검사: 전체 공개이고 삭제되지 않은 여행인지 확인
        if (mapper.isTripPublicAndActive(tripId) == 0) {
            throw new LikeNotAllowedException("좋아요는 전체 공개 여행에만 할 수 있습니다.");
        }

        if (mapper.deleteLike(tripId, userId) > 0) {
            // 기존에 좋아요가 있었으므로 취소 (UC-021 2A)
            log.info("좋아요 취소: tripId=" + tripId + ", userId=" + userId);
            return false;
        }

        // 좋아요 없음 → 새로 추가
        try {
            mapper.insertLike(tripId, userId);
            log.info("좋아요 추가: tripId=" + tripId + ", userId=" + userId);
        } catch (DuplicateKeyException e) {
            // 동시 클릭으로 같은 행이 이미 삽입된 경우 → 좋아요 상태 유지
            log.info("좋아요 중복 삽입 무시(동시 클릭): tripId=" + tripId + ", userId=" + userId);
        }
        return true;
    }
}
