package com.ttplanner.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.ttplanner.vo.TripFormVO;
import com.ttplanner.vo.TripVO;

/**
 * 여행 관리 (담당: 박준성)
 * UC-005 여행 열람, UC-007 내 여행 목록 조회, UC-008 여행 생성, UC-009 여행 정보 수정,
 * UC-010 여행 삭제, UC-015 공개 범위 설정, UC-016 공유 링크 복사
 */
public interface TripService {

	/** 여행 지역 목록: 17개 시·도 (schema.sql ck_trip_region_region과 같은 순서) */
	List<String> REGIONS = Collections.unmodifiableList(Arrays.asList(
			"서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종", "경기",
			"강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주"));

	/** 여행 제목 최대 글자 수 (TRIP.TITLE은 60자, 입력은 50자까지) */
	int TITLE_MAX_LENGTH = 50;

	/** 여행 기간 최대 일수 */
	int MAX_DAYS = 30;

	// UC-007: 내가 만들었거나 멤버(JOINED)로 참여한 여행 (최근에 만든 순서)
	List<TripVO> getMyTrips(Long userId);

	// UC-008: 비공개 여행을 만들고 만든 회원을 OWNER로 등록, 날짜마다 빈 DAY 생성, 공유 링크 생성 → 새 여행 번호
	// 입력 오류면 TripValidationException
	Long create(Long userId, TripFormVO form);

	// UC-005: 여행 열람 (멤버이거나 전체 공개 여행만). userId는 로그인하지 않았으면 null
	// 없는 여행 → TripAccessException.notFound(), 볼 수 없는 비공개 여행 → TripAccessException.privateTrip()
	TripVO getTrip(Long tripId, Long userId);

	// UC-005 · UC-016: 공유 링크로 열람 (공개 범위와 관계없이 볼 수 있음, 멤버면 본인 권한 적용)
	TripVO getTripByShareToken(String shareToken, Long userId);

	// UC-005: DAY 카드에 쓸 DAY 목록 (임시)
	// TODO(양지호): DayService.findDays(tripId)가 develop에 들어오면 TripController에서 그것을 쓰고 이 메서드는 지운다.
	List<Map<String, Object>> getDays(Long tripId);

	// UC-009: 제목 · 지역 수정 (OWNER만) → 화면에 보여 줄 문구
	String modifyInfo(Long userId, Long tripId, TripFormVO form);

	// UC-009 1A: 여행 기간 변경 (OWNER만) → 화면에 보여 줄 문구 ("DAY n~m이 추가되었습니다." 등)
	// 줄여서 일정이 있는 DAY가 지워지는데 confirmDelete가 false면 TripPeriodConfirmException
	String changePeriod(Long userId, Long tripId, TripFormVO form, boolean confirmDelete);

	// UC-015: 공개 범위 변경 (OWNER만). scope = "PRIVATE" 또는 "PUBLIC" → 화면에 보여 줄 문구
	String changeScope(Long userId, Long tripId, String scope);

	// UC-010: 여행 삭제 (OWNER만, DEL_YN = 'Y'로 표시. 좋아요는 남긴다)
	void remove(Long userId, Long tripId);
}
