<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%--
  공통 레이아웃 윗부분 (담당: 윤아연)
  모든 화면은 왼쪽 사이드바(내 여행, 공개 여행, 설정)와 오른쪽 본문으로 구성한다 (화면설계서 1-나).
  사용법: JSP 맨 위에 <%@include file="../includes/header.jsp"%>, 맨 아래에 <%@include file="../includes/footer.jsp"%>
  로그인 회원은 ${sessionScope.loginUser} (UserVO: userId, nickname, email)
--%>
<!DOCTYPE html>
<html lang="ko">

<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">

    <title>TT플래너</title>

    <!-- Bootstrap 3.4.1 -->
    <link href="/resources/vendor/bootstrap/css/bootstrap.min.css" rel="stylesheet">
    <!-- MetisMenu (사이드바 접기 · 펼치기) -->
    <link href="/resources/vendor/metisMenu/metisMenu.min.css" rel="stylesheet">
    <!-- DataTables (환경 확인용 게시판에서 사용) -->
    <link href="/resources/vendor/datatables-plugins/dataTables.bootstrap.css" rel="stylesheet">
    <link href="/resources/vendor/datatables-responsive/dataTables.responsive.css" rel="stylesheet">
    <!-- SB Admin 2 -->
    <link href="/resources/dist/css/sb-admin-2.css" rel="stylesheet">
    <link href="/resources/vendor/font-awesome/css/font-awesome.min.css" rel="stylesheet" type="text/css">

    <style>
        /* TT플래너 공통 스타일 */
        .tt-brand { font-weight: bold; color: #337ab7 !important; }
        .tt-user { padding: 15px; color: #555; }
        .tt-field-error { color: #a94442; font-size: 13px; margin-top: 4px; }
        .tt-field-ok { color: #3c763d; font-size: 13px; margin-top: 4px; }
        .tt-narrow { max-width: 480px; margin: 0 auto; padding-top: 30px; } /* margin을 쓰면 #page-wrapper 위에 빈 줄이 생긴다 */
        .tt-logout-form { display: none; }
    </style>

    <!-- jQuery 3.1.0 (구성정의서 2장). 본문 JSP의 <script>에서 $를 쓸 수 있도록 head에서 먼저 불러온다 -->
    <script src="/resources/vendor/jquery/jquery.min.js"></script>
</head>

<body>

    <div id="wrapper">

        <nav class="navbar navbar-default navbar-static-top" role="navigation" style="margin-bottom: 0">
            <div class="navbar-header">
                <button type="button" class="navbar-toggle" data-toggle="collapse" data-target=".navbar-collapse">
                    <span class="sr-only">메뉴 열기</span>
                    <span class="icon-bar"></span>
                    <span class="icon-bar"></span>
                    <span class="icon-bar"></span>
                </button>
                <%-- 사이드바 주소 (구성정의서 v1.4 8장 나.): 내 여행 GET /trips (박준성), 공개 여행 GET /public/trips (윤아연),
                     내 여행 통계 GET /settings/stats (송유림). /trips를 바꾸면 UserController.MY_TRIPS_URL도 함께 바꾼다 --%>
                <a class="navbar-brand tt-brand" href="${empty sessionScope.loginUser ? '/public/trips' : '/trips'}">TT플래너</a>
            </div>

            <ul class="nav navbar-top-links navbar-right">
                <c:choose>
                    <c:when test="${not empty sessionScope.loginUser}">
                        <li class="tt-user"><i class="fa fa-user fa-fw"></i> <c:out value="${sessionScope.loginUser.nickname}"/>님</li>
                    </c:when>
                    <c:otherwise>
                        <li><a href="/user/login"><i class="fa fa-sign-in fa-fw"></i> 로그인</a></li>
                    </c:otherwise>
                </c:choose>
            </ul>

            <div class="navbar-default sidebar" role="navigation">
                <div class="sidebar-nav navbar-collapse">
                    <ul class="nav" id="side-menu">
                        <li>
                            <%-- 비로그인 상태로 누르면 LoginInterceptor가 "로그인이 필요합니다."와 함께 로그인 화면으로 보낸다 --%>
                            <a href="/trips"><i class="fa fa-suitcase fa-fw"></i> 내 여행</a>
                        </li>
                        <li>
                            <a href="/public/trips"><i class="fa fa-globe fa-fw"></i> 공개 여행</a>
                        </li>
                        <li>
                            <a href="#"><i class="fa fa-cog fa-fw"></i> 설정<span class="fa arrow"></span></a>
                            <ul class="nav nav-second-level">
                                <li><a href="/settings/mypage">마이페이지</a></li>
                                <li><a href="/settings/stats">내 여행 통계</a></li>
                                <c:if test="${not empty sessionScope.loginUser}">
                                    <li><a href="#" id="tt-logout-link">로그아웃</a></li>
                                </c:if>
                                <li><a href="/settings/withdraw">회원 탈퇴</a></li>
                            </ul>
                        </li>
                        <c:if test="${empty sessionScope.loginUser}">
                            <%-- 비로그인 사이드바: 아래쪽에 로그인 · 회원가입 메뉴 (S-01 6번) --%>
                            <li><a href="/user/login"><i class="fa fa-sign-in fa-fw"></i> 로그인</a></li>
                            <li><a href="/user/join"><i class="fa fa-user-plus fa-fw"></i> 회원가입</a></li>
                        </c:if>
                    </ul>
                </div>
            </div>
        </nav>

        <%-- 로그아웃은 POST로 보낸다 (UC-003). 사이드바의 [로그아웃]을 누르면 이 폼이 제출된다 --%>
        <form id="tt-logout-form" class="tt-logout-form" method="post" action="/user/logout"></form>
        <script>
            $(function () {
                $('#tt-logout-link').on('click', function (e) {
                    e.preventDefault();
                    $('#tt-logout-form').submit();
                });
            });
        </script>

        <div id="page-wrapper">
