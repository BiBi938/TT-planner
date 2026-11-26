<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%--
  S-08 여행 상세의 [참여 요청] 자리 (UC-026 참여 요청) · 담당: 양지호
  trip/detail.jsp(박준성)가 [내 여행으로 복사] 옆에 <jsp:include page="_join.jsp"/>로 넣는다.

  model:
    trip(TripVO)     tripId, member(참여 중인 멤버인지)
    myStatus(String) MemberService.getStatus(tripId, 로그인 회원) — JOINED / INVITED / REQUESTED / null(관계 없음 · 비로그인)
    token(String)    공유 링크(/s/{token})로 열었을 때만 값이 있음 → 폼에 담아 보내면 요청 후 같은 공유 링크 화면으로 돌아옴

  2B. 이미 멤버(trip.member)         → 아무것도 그리지 않음
  2C. 참여 요청 중(REQUESTED)         → "참여 요청 중" (다시 요청 불가)
  2D. 초대받음(INVITED)              → "받은 초대가 있습니다. 내 여행 화면에서 수락하세요."
  그 밖(관계 없음 · 비로그인)          → [참여 요청] → 확인 상자 "이 여행의 OWNER에게 참여를 요청할까요?"
      [확인] 폼 제출 POST /trips/{tripId}/members/request (token) / [취소] 상자만 닫음(5A)
      비로그인이 [확인]을 누르면 LoginInterceptor가 로그인 화면으로 보낸다 (2A)
--%>

<c:if test="${not trip.member}">
	<c:choose>
		<%-- 2C --%>
		<c:when test="${myStatus == 'REQUESTED'}">
			<button type="button" class="btn btn-default" disabled>참여 요청 중</button>
		</c:when>

		<%-- 2D --%>
		<c:when test="${myStatus == 'INVITED'}">
			<span class="text-info tt-join-invited">
				받은 초대가 있습니다. <a href="/trips">내 여행 화면</a>에서 수락하세요.
			</span>
		</c:when>

		<%-- 2 · 3. [참여 요청] + 4. 확인 상자 --%>
		<c:otherwise>
			<button type="button" class="btn btn-primary" id="joinRequestBtn">참여 요청</button>

			<div id="joinConfirm" class="well well-sm" style="display: none; margin-top: 10px;">
				<p style="margin-bottom: 8px;">이 여행의 OWNER에게 참여를 요청할까요?</p>
				<form id="joinConfirmForm" method="post" action="/trips/<c:out value='${trip.tripId}'/>/members/request" class="text-right">
					<c:if test="${not empty token}">
						<input type="hidden" name="token" value="<c:out value='${token}'/>">
					</c:if>
					<button type="button" class="btn btn-default btn-sm" id="joinCancelBtn">취소</button>
					<button type="submit" class="btn btn-primary btn-sm" id="joinOkBtn">확인</button>
				</form>
			</div>

			<script>
				$(function () {
					// 3 → 4. [참여 요청] → 확인 상자 열기
					$('#joinRequestBtn').on('click', function () {
						$('#joinConfirm').show();
					});
					// 5A. [취소] → 상자만 닫음 (아무것도 바뀌지 않음)
					$('#joinCancelBtn').on('click', function () {
						$('#joinConfirm').hide();
					});
					// 5. [확인] → 폼 제출, 두 번 눌러 두 번 가지 않게 잠금
					$('#joinConfirmForm').on('submit', function () {
						$('#joinOkBtn').prop('disabled', true);
					});
				});
			</script>
		</c:otherwise>
	</c:choose>
</c:if>
