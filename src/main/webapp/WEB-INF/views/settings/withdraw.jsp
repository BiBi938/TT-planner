<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%--
  S-12 회원 탈퇴 (담당: 윤아연, UC-024 / SFR-011)
  1~2단계 GET  /settings/withdraw        → 삭제될 여행(ownedTrips), OWNER 넘기기 안내, 참여 여행 수(joinedCount)
  3~5단계 POST /settings/withdraw/check  → 비밀번호 확인. 맞으면 confirmStep=true → 확인 창을 띄운다
  6~7단계 POST /settings/withdraw        → 탈퇴 처리 후 로그인 화면 ("탈퇴가 완료되었습니다.")
  6A     확인 창 [취소]                  → 창만 닫고 탈퇴하지 않는다
--%>
<%@include file="../includes/header.jsp"%>

<div class="row">
	<div class="col-lg-12">
		<h2 class="page-header">회원 탈퇴</h2>
	</div>
</div>

<div class="row">
	<div class="col-md-8">

		<%-- 삭제 안내 (S-12 1번) --%>
		<div class="panel panel-danger">
			<div class="panel-heading">탈퇴하면 함께 처리되는 내용</div>
			<div class="panel-body">
				<c:choose>
					<c:when test="${empty ownedTrips}">
						<p>내가 OWNER인 여행이 없습니다.</p>
					</c:when>
					<c:otherwise>
						<p>내가 OWNER인 여행 <strong>${fn:length(ownedTrips)}개</strong>가 함께 삭제됩니다.</p>
						<ul>
							<c:forEach items="${ownedTrips}" var="trip">
								<li>
									<c:out value="${trip.title}"/>
									<c:if test="${trip.otherMemberCount > 0}">
										<span class="text-muted">(다른 멤버 ${trip.otherMemberCount}명)</span>
									</c:if>
								</li>
							</c:forEach>
						</ul>
					</c:otherwise>
				</c:choose>
				<p>멤버로 참여 중인 여행 ${joinedCount}개에서는 자동으로 빠집니다.</p>

				<%-- OWNER 넘기기 안내 (S-12 2번) --%>
				<c:if test="${ownedWithMembers > 0}">
					<div class="alert alert-warning" style="margin-bottom: 0">
						<strong>다른 멤버가 계속 쓰려면 탈퇴 전에 OWNER를 넘기세요.</strong>
						<ul>
							<c:forEach items="${ownedTrips}" var="trip">
								<c:if test="${trip.otherMemberCount > 0}">
									<li><a href="/trips/${trip.tripId}"><c:out value="${trip.title}"/></a></li>
								</c:if>
							</c:forEach>
						</ul>
					</div>
				</c:if>
			</div>
		</div>

		<%-- 비밀번호 확인 (S-12 3번) --%>
		<div class="panel panel-default">
			<div class="panel-body">
				<form method="post" action="/settings/withdraw/check" novalidate>
					<div class="form-group">
						<label for="password">비밀번호 확인</label>
						<input type="password" class="form-control" id="password" name="password" autofocus>
						<div class="tt-field-error"><c:out value="${error}"/></div>
					</div>
					<button type="submit" class="btn btn-danger">회원 탈퇴</button>
					<a href="/settings/mypage" class="btn btn-default">취소</a>
				</form>
			</div>
		</div>
	</div>
</div>

<%-- 최종 확인 창 (UC-024 5단계). 비밀번호가 맞은 경우에만 띄운다 --%>
<c:if test="${confirmStep}">
	<div class="modal fade" id="withdrawModal" tabindex="-1" role="dialog" aria-labelledby="withdrawModalTitle">
		<div class="modal-dialog" role="document">
			<div class="modal-content">
				<div class="modal-header">
					<h4 class="modal-title" id="withdrawModalTitle">회원 탈퇴</h4>
				</div>
				<div class="modal-body">
					정말 탈퇴할까요? 내가 OWNER인 여행 ${fn:length(ownedTrips)}개가 함께 삭제되고 되돌릴 수 없습니다.
				</div>
				<div class="modal-footer">
					<form method="post" action="/settings/withdraw">
						<button type="button" class="btn btn-default" data-dismiss="modal">취소</button>
						<button type="submit" class="btn btn-danger">탈퇴</button>
					</form>
				</div>
			</div>
		</div>
	</div>
	<script>
		// bootstrap.js는 footer.jsp에서 불러오므로, 문서를 다 읽은 뒤에 창을 띄운다
		$(function () {
			$('#withdrawModal').modal({ backdrop: 'static', show: true });
		});
	</script>
</c:if>

<%@include file="../includes/footer.jsp"%>
