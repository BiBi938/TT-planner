<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%--
  UC-021 좋아요 버튼 조각 · 담당: 김민정
  trip/detail.jsp에서 <jsp:include page="_like.jsp"/>로 포함 (박준성 소유 파일, 직접 수정 금지).

  model:
    trip(TripVO) — 필수. trip.publicScope 가 false 이면 아무것도 표시 안 함 (UC-021 1A)
    likeCount(int) — TripController.fillDetail()(박준성)이 넣어 줌. 값이 없으면 0
    liked(boolean)  — TripController.fillDetail()(박준성)이 넣어 줌. 값이 없으면 false (비로그인도 false)

  Ajax: POST /api/trips/{tripId}/like
    200  {"liked": true/false, "count": n}  → 버튼 ♥/♡ 색상·숫자 갱신
    401  {"message": "...", "loginUrl": "..."} → /user/login?back=현재주소 로 이동 (UC-021 2B)
    403  {"message": "..."}                → 버튼 옆 오류 문구 표시
--%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<c:if test="${trip.publicScope}">
    <%-- 값이 없을 때를 대비한 기본값 --%>
    <c:set var="initLikeCount" value="${empty likeCount ? 0 : likeCount}"/>
    <c:set var="initLiked"     value="${not empty liked and liked}"/>

    <span id="likeArea" style="display: inline-block; vertical-align: middle;">
        <button type="button" id="likeBtn"
                class="btn btn-sm ${initLiked ? 'btn-danger' : 'btn-default'}"
                data-trip-id="<c:out value='${trip.tripId}'/>"
                aria-label="좋아요">
            <c:choose>
                <c:when test="${initLiked}">&#9829;</c:when>
                <c:otherwise>&#9825;</c:otherwise>
            </c:choose>
            <c:out value="${initLikeCount}"/>
        </button>
        <span id="likeMsg" class="small text-danger" style="margin-left: 6px;"></span>
    </span>

    <script>
        $(function () {
            $('#likeBtn').on('click', function () {
                var $btn   = $(this);
                var tripId = $btn.data('trip-id');

                // 동시 클릭 방지: 응답이 올 때까지 버튼 잠금
                $btn.prop('disabled', true);
                $('#likeMsg').text('');

                $.ajax({
                    type    : 'POST',
                    url     : '/api/trips/' + tripId + '/like',
                    dataType: 'json',
                    complete: function () {
                        $btn.prop('disabled', false);
                    },
                    success: function (data) {
                        // 버튼 ♥/♡, 색상, 숫자 갱신
                        if (data.liked) {
                            $btn.removeClass('btn-default').addClass('btn-danger')
                                .html('&#9829; ' + data.count);
                        } else {
                            $btn.removeClass('btn-danger').addClass('btn-default')
                                .html('&#9825; ' + data.count);
                        }
                    },
                    error: function (xhr) {
                        // 2B. 비로그인 → 로그인 화면으로 이동. 로그인 후 보던 여행으로 돌아온다
                        if (xhr.status === 401) {
                            var loginUrl = '/user/login';
                            try {
                                var d = JSON.parse(xhr.responseText);
                                if (d.loginUrl) { loginUrl = d.loginUrl; }
                            } catch (ignore) {}
                            window.location.href = loginUrl
                                + '?back=' + encodeURIComponent(location.pathname + location.search);
                            return;
                        }
                        // 403, 500 등 그 외 오류 → 버튼 옆에 문구 표시
                        var msg = '오류가 발생했습니다.';
                        try {
                            var d = JSON.parse(xhr.responseText);
                            if (d.message) { msg = d.message; }
                        } catch (ignore) {}
                        $('#likeMsg').text(msg);
                    }
                });
            });
        });
    </script>
</c:if>
