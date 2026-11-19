package com.ttplanner.service;

import java.util.Map;

/**
 * 좋아요 서비스 (담당: 김민정, UC-021)
 *
 * 구성정의서 8장 나. 연결 지점:
 *   LikeService.count(tripId)
 *   LikeService.isLiked(tripId, userId)
 *   LikeService.toggle(tripId, userId) → {"liked": boolean, "count": int}
 */
public interface LikeService {

    /** 여행의 좋아요 수 */
    int count(Long tripId);

    /**
     * 회원이 이 여행에 좋아요를 눌렀으면 true.
     * userId가 null(비로그인)이면 DB 조회 없이 false 반환
     */
    boolean isLiked(Long tripId, Long userId);

    /**
     * 좋아요 토글.
     * @return {"liked": true/false, "count": n}
     * @throws LikeNotAllowedException 비공개 여행이거나 삭제된 여행인 경우 (UC-021 1A)
     */
    Map<String, Object> toggle(Long tripId, Long userId);
}
