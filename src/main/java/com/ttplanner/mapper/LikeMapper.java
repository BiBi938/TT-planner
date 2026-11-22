package com.ttplanner.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * 좋아요 SQL (담당: 김민정, UC-021)
 * SQL: src/main/resources/mapper/LikeMapper.xml
 */
public interface LikeMapper {

    /** 여행의 좋아요 수 */
    int count(Long tripId);

    /** 회원이 이 여행에 좋아요를 눌렀으면 1, 아니면 0 */
    int isLiked(@Param("tripId") Long tripId, @Param("userId") Long userId);

    /** TRIP_LIKE에 한 행을 넣는다. PK 중복이면 DuplicateKeyException */
    int insertLike(@Param("tripId") Long tripId, @Param("userId") Long userId);

    /** TRIP_LIKE에서 한 행을 지운다. 없어도 오류 없음 (지운 행 수 반환) */
    int deleteLike(@Param("tripId") Long tripId, @Param("userId") Long userId);

    /** 여행이 전체 공개이고 삭제되지 않았으면 1, 아니면 0 */
    int isTripPublicAndActive(Long tripId);
}
