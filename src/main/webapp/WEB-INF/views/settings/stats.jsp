<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%--
  S-10 내 여행 통계 (담당: 송유림, UC-022 / SFR-009, SFR-011)
  GET /settings/stats → StatsController.stats() → model "stats" (StatsVO)
    stats.tripCount   총 여행 수
    stats.regionCount 방문 지역 수 (중복 제외)
    stats.likeCount   좋아요 받은 수
    stats.regions     지역별 여행 횟수 목록 (RegionStatVO: region, tripCount), 많은 순으로 정렬되어 옴
  끝난 여행이 없으면 regions가 비어 있다 → 수치 0과 안내 문구 (UC-022 2A)

  원그래프는 라이브러리 없이 CSS conic-gradient로 그린다.
  아래 순위 표의 각 줄(data-count)을 JS가 읽어 각도를 계산한다 → JS 안에 EL(${...})을 쓰지 않는다.
--%>
<%@include file="../includes/header.jsp"%>

<style>
	.tt-stat-box { text-align: center; padding: 18px 10px; }
	.tt-stat-box .tt-stat-num { font-size: 32px; font-weight: bold; color: #337ab7; line-height: 1.2; }
	.tt-stat-box .tt-stat-label { color: #777; margin-top: 4px; }
	.tt-pie { width: 220px; height: 220px; border-radius: 50%; margin: 10px auto 20px; background: #eee; }
	.tt-swatch { display: inline-block; width: 12px; height: 12px; border-radius: 2px; margin-right: 6px; vertical-align: -1px; }
	.tt-rank-table td, .tt-rank-table th { vertical-align: middle !important; }
	.tt-empty { text-align: center; color: #777; padding: 40px 0; }
</style>

<div class="row">
	<div class="col-lg-12">
		<h2 class="page-header">내 여행 통계</h2>
	</div>
</div>

<%-- 요약 수치 3개 --%>
<div class="row">
	<div class="col-sm-4">
		<div class="panel panel-default tt-stat-box">
			<div class="tt-stat-num"><c:out value="${stats.tripCount}"/></div>
			<div class="tt-stat-label">총 여행 수</div>
		</div>
	</div>
	<div class="col-sm-4">
		<div class="panel panel-default tt-stat-box">
			<div class="tt-stat-num"><c:out value="${stats.regionCount}"/></div>
			<div class="tt-stat-label">방문 지역 수</div>
		</div>
	</div>
	<div class="col-sm-4">
		<div class="panel panel-default tt-stat-box">
			<div class="tt-stat-num"><c:out value="${stats.likeCount}"/></div>
			<div class="tt-stat-label">좋아요 받은 수</div>
		</div>
	</div>
</div>

<%-- 지역별 여행 횟수: 원그래프 + 순위 --%>
<div class="panel panel-default">
	<div class="panel-heading">지역별 여행 횟수</div>
	<div class="panel-body">
		<c:choose>
			<%-- UC-022 2A: 끝난 여행이 없음 --%>
			<c:when test="${empty stats.regions}">
				<div class="tt-empty">아직 끝난 여행이 없습니다.</div>
			</c:when>

			<c:otherwise>
				<div class="row">
					<div class="col-md-5">
						<div id="ttPie" class="tt-pie" role="img" aria-label="지역별 여행 횟수 원그래프"></div>
					</div>
					<div class="col-md-7">
						<table class="table table-condensed tt-rank-table" id="ttRankTable">
							<thead>
								<tr>
									<th style="width: 60px;">순위</th>
									<th>지역</th>
									<th style="width: 90px;" class="text-right">여행 횟수</th>
								</tr>
							</thead>
							<tbody>
								<%-- 공동 순위: 횟수가 앞 지역과 같으면 순위를 그대로 둔다 (예: 1, 1, 3) --%>
								<c:forEach items="${stats.regions}" var="r" varStatus="st">
									<c:if test="${st.first or r.tripCount ne prevCount}">
										<c:set var="rank" value="${st.count}"/>
									</c:if>
									<c:set var="prevCount" value="${r.tripCount}"/>
									<tr class="tt-rank-row" data-count="<c:out value='${r.tripCount}'/>">
										<td><c:out value="${rank}"/>위</td>
										<td><span class="tt-swatch"></span><c:out value="${r.region}"/></td>
										<td class="text-right"><c:out value="${r.tripCount}"/>회</td>
									</tr>
								</c:forEach>
							</tbody>
						</table>
					</div>
				</div>
			</c:otherwise>
		</c:choose>

		<p class="help-block small" style="margin-bottom: 0;">
			여행 수와 지역은 내가 만들었거나 멤버로 참여한 여행 중 종료일이 지난 여행만 셉니다.
			지역이 여러 개인 여행은 지역마다 1회씩 셉니다.
			좋아요는 내가 OWNER인 여행이 받은 좋아요의 합계이며, 삭제한 여행이 받았던 좋아요도 포함합니다.
		</p>
	</div>
</div>

<script>
	// 원그래프 그리기
	// 예) 제주 3, 전남 1 → 전체 4 → 제주 0~270도, 전남 270~360도
	//     → background: conic-gradient(#337ab7 0deg 270deg, #5cb85c 270deg 360deg)
	$(function () {
		var colors = ['#337ab7', '#5cb85c', '#f0ad4e', '#d9534f', '#5bc0de', '#8e6bbf', '#e377c2', '#17becf',
			'#bcbd22', '#7f7f7f', '#2c6e49', '#c0392b', '#1f4e79', '#b07d2b', '#6a8caf', '#a05195', '#3c8d93'];
		var rows = $('#ttRankTable .tt-rank-row');
		if (rows.length === 0) {
			return; // 끝난 여행이 없으면 그래프 없음
		}

		var total = 0;
		rows.each(function () { total += Number($(this).data('count')); });

		var start = 0;
		var parts = [];
		rows.each(function (i) {
			var color = colors[i % colors.length];
			var end = start + Number($(this).data('count')) / total * 360;
			parts.push(color + ' ' + start + 'deg ' + end + 'deg');
			$(this).find('.tt-swatch').css('background', color); // 순위 표에도 같은 색 표시
			start = end;
		});
		$('#ttPie').css('background', 'conic-gradient(' + parts.join(', ') + ')');
	});
</script>

<%@include file="../includes/footer.jsp"%>
