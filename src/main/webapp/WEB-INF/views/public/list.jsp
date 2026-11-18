<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%--
  S-07 공개 여행 검색 (담당: 윤아연, UC-004 / SFR-007)
  GET /public/trips?keyword=&region=&days=0&page=1 → PublicTripController.list()
  model: cri(검색 조건), regions(17개 시 · 도), trips(카드 목록), total, totalPages, startPage, endPage
  로그인하지 않아도 볼 수 있다.
--%>
<%@include file="../includes/header.jsp"%>

<style>
	.tt-trip-card { display: block; color: inherit; }
	.tt-trip-card:hover { text-decoration: none; }
	.tt-trip-card .panel { min-height: 120px; }
	.tt-trip-card .panel:hover { border-color: #337ab7; }
	.tt-trip-title { font-size: 16px; font-weight: bold; margin-bottom: 8px; }
	.tt-like { color: #d9534f; }
</style>

<div class="row">
	<div class="col-lg-12">
		<h2 class="page-header">공개 여행</h2>
	</div>
</div>

<%-- 검색어 · 필터 (S-07 1~2번). page는 보내지 않으므로 검색하면 1페이지부터 --%>
<form id="searchForm" class="form-inline" method="get" action="/public/trips" style="margin-bottom: 20px">
	<div class="form-group">
		<label class="sr-only" for="keyword">여행 제목</label>
		<input type="text" class="form-control" id="keyword" name="keyword" maxlength="50"
			placeholder="여행 제목 검색" value="<c:out value='${cri.keyword}'/>">
	</div>
	<div class="form-group">
		<label class="sr-only" for="region">지역</label>
		<select class="form-control tt-filter" id="region" name="region">
			<option value="">지역 전체</option>
			<c:forEach items="${regions}" var="r">
				<option value="${r}" ${r == cri.region ? 'selected' : ''}>${r}</option>
			</c:forEach>
		</select>
	</div>
	<div class="form-group">
		<label class="sr-only" for="days">여행 일수</label>
		<select class="form-control tt-filter" id="days" name="days">
			<option value="0" ${cri.days == 0 ? 'selected' : ''}>일수 전체</option>
			<option value="1" ${cri.days == 1 ? 'selected' : ''}>당일치기</option>
			<option value="2" ${cri.days == 2 ? 'selected' : ''}>1박 2일</option>
			<option value="3" ${cri.days == 3 ? 'selected' : ''}>2박 3일</option>
			<option value="4" ${cri.days == 4 ? 'selected' : ''}>3박 4일 이상</option>
		</select>
	</div>
	<button type="submit" class="btn btn-primary"><i class="fa fa-search"></i> 검색</button>
</form>

<c:choose>
	<c:when test="${total == 0}">
		<%-- 확장 5A --%>
		<div class="alert alert-info">조건에 맞는 공개 여행이 없습니다.</div>
	</c:when>
	<c:otherwise>
		<p class="text-muted">공개 여행 ${total}건</p>

		<%-- 검색 결과 카드 (S-07 3번). 누르면 여행 상세 /trips/{번호} (박준성, UC-005)
		     검색 조건이 주소에 남아 있어서 상세에서 [뒤로]를 누르면 같은 조건 · 페이지로 돌아온다 --%>
		<div class="row">
			<c:forEach items="${trips}" var="trip">
				<div class="col-sm-6 col-md-4">
					<a class="tt-trip-card" href="/trips/${trip.tripId}">
						<div class="panel panel-default">
							<div class="panel-body">
								<div class="tt-trip-title"><c:out value="${trip.title}"/></div>
								<div>
									<i class="fa fa-calendar fa-fw"></i>
									<fmt:formatDate value="${trip.startDate}" pattern="yyyy.MM.dd"/>
									~ <fmt:formatDate value="${trip.endDate}" pattern="yyyy.MM.dd"/>
									<span class="label label-default">${trip.periodLabel}</span>
								</div>
								<div class="tt-like"><i class="fa fa-heart"></i> ${trip.likeCount}</div>
							</div>
						</div>
					</a>
				</div>
			</c:forEach>
		</div>

		<%-- 페이지 번호 (확장 5B): 10건을 넘으면 표시 --%>
		<c:if test="${totalPages > 1}">
			<nav class="text-center">
				<ul class="pagination">
					<c:if test="${startPage > 1}">
						<li><a href="/public/trips?${cri.queryWithoutPage}&page=${startPage - 1}" aria-label="이전">&laquo;</a></li>
					</c:if>
					<c:forEach begin="${startPage}" end="${endPage}" var="p">
						<li class="${p == cri.page ? 'active' : ''}">
							<a href="/public/trips?${cri.queryWithoutPage}&page=${p}">${p}</a>
						</li>
					</c:forEach>
					<c:if test="${endPage < totalPages}">
						<li><a href="/public/trips?${cri.queryWithoutPage}&page=${endPage + 1}" aria-label="다음">&raquo;</a></li>
					</c:if>
				</ul>
			</nav>
		</c:if>
	</c:otherwise>
</c:choose>

<script>
	$(function () {
		// 지역 · 일수를 고르면 바로 검색한다 (S-07 2번)
		$('.tt-filter').on('change', function () {
			$('#searchForm').submit();
		});
	});
</script>

<%@include file="../includes/footer.jsp"%>
