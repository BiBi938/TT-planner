package com.ttplanner.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

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

	// UC-008: 새 여행 만들기 창의 [저장]. 입력값을 검사하고 create(trip, regions, ownerUserId)로 만든 뒤 날짜마다 빈 DAY를 만든다 → 새 여행 번호
	// 입력 오류면 TripValidationException (오류 문구는 UC-008 5A~5E)
	Long create(Long userId, TripFormVO form);

	// UC-005: 여행 열람 (멤버이거나 전체 공개 여행만). userId는 로그인하지 않았으면 null
	// 없는 여행 → TripAccessException.notFound(), 볼 수 없는 비공개 여행 → TripAccessException.privateTrip()
	TripVO getTrip(Long tripId, Long userId);

	// UC-005 · UC-016: 공유 링크로 열람 (공개 범위와 관계없이 볼 수 있음, 멤버면 본인 권한 적용)
	TripVO getTripByShareToken(String shareToken, Long userId);

	// UC-009: 제목 · 지역 수정 (OWNER만) → 화면에 보여 줄 문구
	String modifyInfo(Long userId, Long tripId, TripFormVO form);

	// UC-009 1A: 여행 기간 변경 (OWNER만) → 화면에 보여 줄 문구 ("DAY n~m이 추가되었습니다." 등)
	// 줄여서 일정이 있는 DAY가 지워지는데 confirmDelete가 false면 TripPeriodConfirmException
	String changePeriod(Long userId, Long tripId, TripFormVO form, boolean confirmDelete);

	// UC-015: 공개 범위 변경 (OWNER만). scope = "PRIVATE" 또는 "PUBLIC" → 화면에 보여 줄 문구
	String changeScope(Long userId, Long tripId, String scope);

	// ===================== 팀 연결 지점 (구성정의서 v1.4 8장 나.) =====================

	// UC-008 · UC-020(송유림): 여행 · 지역 · OWNER 행 · 공유 링크(SHARE_TOKEN)까지 만들고 DAY는 만들지 않는다 → 새 tripId
	// trip에는 title, startDate만 채워서 넘긴다 (공개 범위는 항상 비공개)
	// DAY는 같은 @Transactional 안에서 이어서 DayService.syncDays 또는 DayService.insertDays로 만든다
	// 제목이 비었거나 60자를 넘거나, 시작일이 없거나, 지역이 목록에 없으면 TripValidationException
	Long create(TripVO trip, List<String> regions, Long ownerUserId);

	// UC-020(송유림): 여행 한 개 (권한 검사 없음). 없거나 삭제한 여행이면 null
	// 돌려주는 TripVO 필드: tripId, title, startDate, endDate, dayCount, scope, shareToken, createdAt,
	//                       regions(지역 목록), ownerNickname, memberCount  (myRole은 null)
	TripVO find(Long tripId);

	// UC-020(송유림): 볼 수 있는 여행인지. 삭제되지 않았고 (전체 공개이거나 멤버)이면 true
	// userId는 로그인하지 않았으면 null. 공유 링크로 연 경우는 findByToken으로 따로 확인한다
	boolean canView(Long tripId, Long userId);

	// UC-020(송유림): 공유 링크 토큰으로 여행 찾기 (권한 검사 없음). 없거나 삭제한 여행이면 null
	// 돌려주는 필드는 find와 같다
	TripVO findByToken(String token);

	// UC-024 회원 탈퇴(윤아연): 회원이 OWNER인 여행을 모두 DEL_YN = 'Y'로 표시 → 표시한 여행 수
	// UC-010 여행 삭제와 같은 규칙: 행은 지우지 않고 좋아요는 남긴다
	int deleteByOwner(Long userId);

	// UC-012 DAY 날짜 변경(양지호)에서 쓰는 연결 메서드: 여행 시작일(TRIP.START_DATE)만 바꾼다.
	// DAY · 권한(OWNER) 검사 · 결과 문구는 건드리지 않는다 → 호출하는 쪽(DayService · DayController)에서 처리
	// 시작일만 바꾸면 모든 DAY 날짜와 종료일이 함께 이동한다 (DAY 날짜 = START_DATE + DAY_NO - 1)
	void changeStartDate(Long tripId, Date startDate);

	// UC-010: 여행 삭제 (OWNER만, DEL_YN = 'Y'로 표시. 좋아요는 남긴다)
	void remove(Long userId, Long tripId);
}
