<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%--
  S-03 내 여행 화면 위쪽 "받은 초대"(UC-025 초대 응답) · "받은 참여 요청"(UC-027 참여 요청 처리) 조각 · 담당: 양지호
  trip/list.jsp(박준성)가 <jsp:include page="_invites.jsp"/>로 넣는다.

  model:
    invites(List<InviteVO>)  MemberService.findInvites(로그인 회원) — 응답 대기 초대, 여행 번호 큰 순
                             InviteVO: tripId, title(여행 제목), ownerNickname(초대한 OWNER), role(초대받은 권한)
    joinRequests(List<JoinRequestVO>)  MemberService.findJoinRequests(로그인 회원) — 내가 OWNER인 여행에 온 참여 요청
                             JoinRequestVO: tripId, title, userId, nickname(요청한 회원)

  2. "받은 초대 n건" + 목록(여행 제목, 초대한 OWNER, 권한)
  2A. 초대가 없으면(빈 목록) 이 조각은 아무것도 그리지 않는다
  3. [수락]  폼 제출 POST /trips/{tripId}/members/accept → 결과 문구 flash tripMsg → /trips (list.jsp가 출력)
  3A. [거절] 폼 제출 POST /trips/{tripId}/members/reject → "초대를 거절했습니다."

  UC-027 1. "받은 참여 요청 n건" + 요청한 회원 → 2. [멤버 목록에서 처리] = /trips/{tripId}?openMembers
         (여행 상세로 이동하면서 _members.jsp가 멤버 목록 창을 연다 → 거기서 수락 · 거절). 요청이 없으면 그리지 않음
--%>

<%-- 두 칸(받은 초대 · 받은 참여 요청)이 같이 쓰는 모양 --%>
<style>
	.tt-invites .list-group-item { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
	.tt-invites .tt-invite-text { flex: 1 1 240px; }
	.tt-invites .tt-invite-title { font-weight: bold; }
	.tt-invites form { display: inline; margin: 0; }
</style>

<c:if test="${not empty invites}">
	<div class="panel panel-info tt-invites">
		<div class="panel-heading">
			<strong>받은 초대 <c:out value="${fn:length(invites)}" />건</strong>
		</div>
		<ul class="list-group">
			<c:forEach var="inv" items="${invites}">
				<li class="list-group-item">
					<span class="tt-invite-text">
						<span class="tt-invite-title"><c:out value="${inv.title}" /></span>
						<span class="text-muted"> · 초대한 사람 <c:out value="${inv.ownerNickname}" /> · </span>
						<span class="label label-default"><c:out value="${inv.role}" /></span>
					</span>
					<form method="post" action="/trips/<c:out value='${inv.tripId}'/>/members/accept">
						<button type="submit" class="btn btn-primary btn-sm">수락</button>
					</form>
					<form method="post" action="/trips/<c:out value='${inv.tripId}'/>/members/reject">
						<button type="submit" class="btn btn-default btn-sm">거절</button>
					</form>
				</li>
			</c:forEach>
		</ul>
	</div>
</c:if>

<%-- UC-027 받은 참여 요청 (내가 OWNER인 여행) --%>
<c:if test="${not empty joinRequests}">
	<div class="panel panel-warning tt-invites">
		<div class="panel-heading">
			<strong>받은 참여 요청 <c:out value="${fn:length(joinRequests)}" />건</strong>
		</div>
		<ul class="list-group">
			<c:forEach var="req" items="${joinRequests}">
				<li class="list-group-item">
					<span class="tt-invite-text">
						<span class="tt-invite-title"><c:out value="${req.title}" /></span>
						<span class="text-muted"> · 요청한 회원 </span><c:out value="${req.nickname}" />
					</span>
					<a class="btn btn-default btn-sm" href="/trips/<c:out value='${req.tripId}'/>?openMembers">멤버 목록에서 처리</a>
				</li>
			</c:forEach>
		</ul>
	</div>
</c:if>
