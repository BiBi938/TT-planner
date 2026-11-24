package com.ttplanner.vo;

import lombok.Data;

/**
 * DAY의 일정 한 개 (SCHEDULE 테이블 한 줄 + DAY 번호) - 담당: 송유림, UC-013
 * 필드 이름은 구성정의서 8장 나.의 약속 그대로 (박준성 UC-005, 윤아연 UC-006 지도 · UC-014 장소 검색이 같은 이름을 쓴다)
 *
 * 사용: ScheduleMapper.findByTrip(tripId) → ScheduleService.findByTrip(tripId)
 *       → TripController.detail()이 model "schedules"로 넣음 → trip/_schedules.jsp, trip/_map.jsp
 * 일정 등록 · 수정 폼의 입력칸 이름(dayId, time, placeName, lat, lng, description)도 이 필드 이름과 같다.
 */
@Data
public class ScheduleVO {

	private Long schId;          // SCH_ID
	private Long dayId;          // DAY_ID
	private int slotNo;          // SLOT_NO : DAY 안에서의 순서 1, 2, 3 ...
	private String time;         // TIME : 'HH:MM' 글자 (예: "09:30"). 비공개 여행은 비어 있을 수 있음
	private String placeName;    // PLACE_NAME : 장소 이름 (최대 100자). 비공개 여행은 비어 있을 수 있음

	// 위도 · 경도: 장소 이름만 입력했으면 null → 지도에 표시하지 않음
	// double(기본형)이 아니라 Double(객체형)이어야 빈 값("")이 null로 들어온다
	private Double lat;          // LAT
	private Double lng;          // LNG

	private String description;  // DESCRIPTION : 설명 (최대 500자)

	// 테이블 컬럼이 아니라 SQL에서 TRIP_DAY와 join해서 넣는 값 (지도 "DAY-순서" 번호에 사용)
	private int dayNo;           // DAY_NO
}
