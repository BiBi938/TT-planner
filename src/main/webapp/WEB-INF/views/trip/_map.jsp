<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%--
  지도 조각 (UC-006 지도 보기) - 담당: 윤아연
  사용법: trip/detail.jsp(박준성)의 지도 자리에서 <jsp:include page="_map.jsp"/>  (구성정의서 v1.6 8장 나.)

  필요한 model 값 (TripController.detail(박준성)이 넣어 줌)
    - days      : List<DayVO>       DAY 탭 [DAY 1][DAY 2]... (dayNo)
    - schedules : List<ScheduleVO>  여행 전체 일정, DAY 번호 → SLOT_NO 순 (ScheduleService.findByTrip, 송유림)
                  dayNo, placeName, time, lat, lng 사용. lat · lng가 없는 일정(장소 이름만)은 핀을 찍지 않는다

  화면 (UC-006 시나리오)
    1. 처음에는 [전체] 보기
    3. 전체: DAY마다 다른 색 핀에 "DAY-순서" 번호 (예: 2-1). 색은 DAY 카드(_days.jsp) 왼쪽 색과 같다
    4~5. [DAY n] 탭 또는 DAY 카드의 [지도] 버튼: 그 DAY 핀만 1, 2, 3 번호로 표시하고 이동 순서대로 선으로 잇는다
    6. [전체] 탭: 다시 전체 보기
    2A. 지도 API를 쓸 수 없음(키 없음, 인터넷 · 도메인 문제) → "지도를 표시할 수 없습니다." (여행 · 일정 정보는 그대로)
    3A. 장소가 입력된 일정이 없음 → "장소가 있는 일정이 없습니다."
  핀 번호는 DAY 카드의 일정 목록 순서와 같다 (장소가 없는 일정은 번호만 건너뛴다. 예: 2-1, 2-3)

  카카오 키: ${applicationScope.kakaoAppKey} (KakaoKeyLoader가 secret.properties에서 읽음)
  동작 코드: /resources/js/tt-map.js, SDK 불러오기: /resources/js/tt-kakao.js (장소 검색 조각과 함께 써서 SDK는 한 번만 불러온다)
  DAY 카드의 [지도] 버튼: class="tt-map-day-btn" data-day="DAY 번호"인 버튼을 누르면 그 DAY를 보여 준다 (_days.jsp, 양지호)
--%>

<div class="panel panel-default tt-map" data-kakao-key="<c:out value='${applicationScope.kakaoAppKey}'/>">
	<div class="panel-heading">
		<i class="fa fa-map-o"></i> 지도
	</div>
	<div class="panel-body">
		<%-- 4 · 6. 보기 고르기: [전체] [DAY 1] [DAY 2] ... --%>
		<div class="tt-map-tabs">
			<button type="button" class="btn btn-xs btn-primary tt-map-tab" data-day="0">전체</button>
			<c:forEach items="${days}" var="d">
				<button type="button" class="btn btn-xs btn-default tt-map-tab" data-day="<c:out value='${d.dayNo}'/>">DAY <c:out value="${d.dayNo}"/></button>
			</c:forEach>
		</div>

		<div class="tt-map-canvas"></div>
		<%-- 2A · 3A 안내 문구 --%>
		<div class="tt-map-msg text-muted"></div>

		<%-- 지도에 찍을 일정 (화면에는 보이지 않음). 값은 c:out으로 넣어 장소 이름의 특수문자가 그대로 글자로만 쓰인다 --%>
		<ul class="tt-map-data" hidden>
			<c:forEach items="${schedules}" var="s">
				<li data-day="<c:out value='${s.dayNo}'/>"
					data-lat="<c:out value='${s.lat}'/>"
					data-lng="<c:out value='${s.lng}'/>"
					data-name="<c:out value='${s.placeName}'/>"
					data-time="<c:out value='${s.time}'/>"></li>
			</c:forEach>
		</ul>
	</div>
</div>

<style>
	.tt-map-tabs { margin-bottom: 8px; }
	.tt-map-tabs .btn { margin: 0 2px 4px 0; }
	.tt-map-canvas { width: 100%; height: 360px; }
	.tt-map-msg { padding: 40px 10px; text-align: center; background: #f5f5f5; display: none; }
	/* 핀: 동그라미 안에 번호 (색은 tt-map.js가 DAY 색으로 칠함) */
	.tt-map-pin { min-width: 26px; height: 26px; padding: 0 5px; border-radius: 13px; border: 2px solid #fff;
		color: #fff; font-size: 12px; font-weight: bold; line-height: 22px; text-align: center;
		box-shadow: 0 1px 4px rgba(0, 0, 0, .4); cursor: pointer; white-space: nowrap; }
	/* 핀을 누르면 뜨는 작은 설명 */
	.tt-map-info { background: #fff; border: 1px solid #ccc; border-radius: 4px; padding: 6px 10px;
		font-size: 12px; box-shadow: 0 2px 6px rgba(0, 0, 0, .2); white-space: nowrap; margin-bottom: 34px; }
	.tt-map-info strong { display: block; font-size: 13px; }
</style>
<script src="/resources/js/tt-kakao.js"></script>
<script src="/resources/js/tt-map.js"></script>
