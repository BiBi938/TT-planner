<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%--
  장소 검색 조각 (UC-014) - 담당: 윤아연
  일정 등록 · 수정 폼(UC-013, 송유림) 안의 "장소" 자리에 넣는다. 일정 관리(UC-013)를 확장한다.

  사용법 (일정 폼 <form> 안에서)
    새 일정:   <jsp:include page="_placeSearch.jsp"/>
    일정 수정: <jsp:include page="_placeSearch.jsp">
                 <jsp:param name="placeName" value="${schedule.placeName}"/>
                 <jsp:param name="lat" value="${schedule.lat}"/>
                 <jsp:param name="lng" value="${schedule.lng}"/>
               </jsp:include>

  폼과 함께 제출되는 값 (구성정의서 8장 나. ScheduleVO 필드 이름과 같다)
    placeName  장소 이름 (최대 100자, SCHEDULE.PLACE_NAME)
    lat, lng   위도 · 경도 (SCHEDULE.LAT · LNG). 검색에서 고르지 않았으면 빈 값 → 이름만 있는 장소
  → Controller에서 ScheduleVO로 받으면 빈 lat · lng는 null이 된다 (Double/BigDecimal 필드일 때).

  흐름: 이름 입력 → [검색] → 카카오 Places 검색(브라우저 → 카카오) → 결과 선택 → placeName · lat · lng 채움
  카카오 키: ${applicationScope.kakaoAppKey} (KakaoKeyLoader가 secret.properties에서 읽음)
  키가 없거나 지도 API를 쓸 수 없으면 "지도를 표시할 수 없습니다." (UC-014 2A) → 이름만 입력해 저장 가능
  동작 코드: /resources/js/tt-place-search.js, SDK 불러오기: /resources/js/tt-kakao.js (지도 조각과 함께 씀)
--%>

<div class="tt-place-search" data-kakao-key="<c:out value='${applicationScope.kakaoAppKey}'/>">
	<div class="input-group">
		<input type="text" name="placeName" class="form-control tt-place-name" maxlength="100"
			autocomplete="off" aria-label="장소 이름" placeholder="장소 이름 (예: 성산일출봉)"
			value="<c:out value='${param.placeName}'/>">
		<span class="input-group-btn">
			<button type="button" class="btn btn-default tt-place-btn"><i class="fa fa-search"></i> 검색</button>
		</span>
	</div>
	<input type="hidden" name="lat" class="tt-place-lat" value="<c:out value='${param.lat}'/>">
	<input type="hidden" name="lng" class="tt-place-lng" value="<c:out value='${param.lng}'/>">

	<%-- 검색 결과 목록 (3) --%>
	<div class="list-group tt-place-results"></div>
	<%-- 안내 문구: 결과 없음(3A), 지도 사용 불가(2A) --%>
	<div class="tt-place-msg"></div>
	<%-- 지금 저장될 상태: 지도 위치 있음 / 이름만 --%>
	<div class="tt-place-status small"></div>
</div>

<%-- 조각이 한 화면에 여러 번 들어가도 스타일 · 스크립트는 한 번만 넣는다 --%>
<c:if test="${empty requestScope.ttPlaceSearchLoaded}">
	<c:set var="ttPlaceSearchLoaded" value="true" scope="request"/>
	<style>
		.tt-place-search { position: relative; }
		.tt-place-results { display: none; position: absolute; z-index: 1000; left: 0; right: 0;
			max-height: 280px; overflow-y: auto; margin: 2px 0 0; box-shadow: 0 4px 10px rgba(0, 0, 0, .15); }
		.tt-place-msg { color: #a94442; font-size: 13px; margin-top: 4px; }
		.tt-place-msg:empty, .tt-place-status:empty { display: none; }
		.tt-place-status { margin-top: 4px; }
	</style>
	<script src="/resources/js/tt-kakao.js"></script>
	<script src="/resources/js/tt-place-search.js"></script>
</c:if>
