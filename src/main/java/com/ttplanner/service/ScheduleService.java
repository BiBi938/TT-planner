package com.ttplanner.service;

import java.util.List;

import com.ttplanner.vo.ScheduleVO;

/**
 * 일정 관리 · 순서 변경 (담당: 송유림, UC-013)
 * findByTrip은 박준성(UC-005 여행 열람) · 윤아연(UC-006 지도)이 쓰는 팀 연결 메서드(구성정의서 8장 나.)
 * → 이름 · 매개변수를 바꾸지 않는다.
 *
 * 검사에 걸리면 IllegalStateException(화면에 보여 줄 문구)을 던진다. Controller가 그 문구를 flash "msg"로 넘긴다.
 * 권한(OWNER · EDITOR) 검사는 Controller에서 MemberService.getRole로 한다 (DayController와 같은 방식).
 */
public interface ScheduleService {

	// 여행 전체 일정: DAY 번호 → SLOT_NO 순 (dayNo 포함). 삭제된 여행이면 빈 목록
	public List<ScheduleVO> findByTrip(Long tripId);

	// DAY가 속한 여행 번호 (순서 변경 API의 권한 검사용). 없는 DAY이거나 삭제된 여행이면 null
	public Long findTripIdByDay(Long dayId);

	// 일정 추가: schedule에 dayId, time, placeName, lat, lng, description을 넣어서 호출
	// 자리: 시간이 있으면 자기보다 늦은 시간인 첫 일정 바로 앞(같은 시간이면 뒤), 시간이 없으면 DAY 맨 끝
	public void add(Long tripId, ScheduleVO schedule);

	// 일정 수정: schedule에 schId, time, placeName, lat, lng, description을 넣어서 호출
	// 시간이 바뀌면 추가와 같은 규칙으로 자리를 다시 정한다. 장소 · 설명만 바뀌거나 시간을 지우면 자리 그대로
	public void update(Long tripId, ScheduleVO schedule);

	// 일정 삭제. 전체 공개 여행의 마지막 일정이면 막는다 (화면설계서 S-05 11)
	public void delete(Long tripId, Long schId);

	// 순서 변경 (Ajax): schIds = 그 DAY 일정 번호를 새 순서대로 나열한 목록
	// 시간은 자리에 남고 장소 · 설명만 움직인다. 바뀐 DAY 일정 목록(SLOT_NO 순)을 돌려준다
	public List<ScheduleVO> reorder(Long dayId, List<Long> schIds);
}
