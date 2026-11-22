package com.ttplanner.service;

import java.util.List;

import com.ttplanner.vo.CopyDayVO;
import com.ttplanner.vo.TripVO;

/**
 * 여행 복사 (담당: 송유림, UC-020 · 화면설계서 S-09)
 *
 * 흐름
 *   ① [내 여행으로 복사] GET  /trips/{id}/copy        → getCopyableTrip + getCopyDays → trip/copy.jsp
 *   ② [복사]              POST /trips/{id}/copy        → selectDays → 세션 copyDays에 DAY_ID 목록 저장 → /trips
 *   ③ [여기에 DAY 넣기]   POST /trips/{id}/copy/insert → insert → 세션 copyDays 삭제 → /trips/{id}
 *      [+ 복사한 DAY로 새 여행 만들기] POST /trips/copy/new → createTrip → 세션 삭제 → /trips/{새 번호}
 *      [복사 취소]        POST /trips/copy/cancel      → 세션 삭제 → /trips
 *
 * 사용자에게 보여 줄 오류는 IllegalStateException(문구)로 던진다 → CopyController가 화면에 문구를 보여 준다.
 * 볼 수 없는 여행은 TripAccessException(박준성) → CopyController가 trip/denied.jsp를 보여 준다.
 */
public interface CopyService {

	/**
	 * 복사할 수 있는 여행인지 확인하고 여행 정보를 돌려준다.
	 *   token이 없으면: TripService.getTrip — 전체 공개 여행이거나 내가 멤버인 여행
	 *   token이 있으면: TripService.getTripByShareToken — 공유 링크로 연 비공개 여행도 허용 (구성정의서 8장 나.)
	 *                   토큰의 여행이 tripId와 다르면 TripAccessException(404)
	 */
	TripVO getCopyableTrip(Long tripId, Long userId, String token);

	/** DAY 선택 화면에 보여 줄 DAY 목록 (날짜, 일정 개수, 장소 일부) */
	List<CopyDayVO> getCopyDays(Long tripId);

	/**
	 * [복사]: 고른 DAY가 그 여행의 DAY인지 확인하고 DAY 번호 순으로 정리한 DAY_ID 목록을 돌려준다.
	 * 아무것도 고르지 않았으면 IllegalStateException("복사할 DAY를 1개 이상 고르세요.")
	 */
	List<Long> selectDays(Long tripId, Long userId, String token, List<Long> dayIds);

	/**
	 * [여기에 DAY 넣기]: 복사한 DAY를 targetTripId 여행의 position(이 DAY 뒤, 0 = 맨 앞)에 넣는다.
	 * 넣을 여행에서 내 권한이 OWNER · EDITOR여야 한다.
	 * @return 화면에 보여 줄 문구
	 */
	String insert(Long userId, Long targetTripId, int position, List<Long> copyDays);

	/**
	 * [+ 복사한 DAY로 새 여행 만들기]: 입력 화면 없이 새 여행을 만들고 복사한 DAY를 넣는다.
	 * 제목 '원본 제목 (복사)', 시작일 오늘, 지역은 원본과 같게, 비공개, 만든 사람이 OWNER (구성정의서 8장 나.)
	 * @return 새 여행 번호
	 */
	Long createTrip(Long userId, List<Long> copyDays);
}
