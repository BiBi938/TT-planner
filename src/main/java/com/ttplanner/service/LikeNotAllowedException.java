package com.ttplanner.service;

/** 비공개 여행이거나 삭제된 여행에 좋아요를 시도할 때 발생 (UC-021 1A 서버 검사) */
public class LikeNotAllowedException extends RuntimeException {

    public LikeNotAllowedException(String message) {
        super(message);
    }
}
