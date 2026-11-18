package com.ttplanner.vo;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import lombok.Data;

/**
 * 공개 여행 검색 조건 (담당: 윤아연, UC-004)
 *
 * GET /public/trips?keyword=제주&region=제주&days=4&page=2 처럼 주소에 담겨 온다.
 * 조건이 주소에 남기 때문에 여행 상세에서 [뒤로]로 돌아와도 검색어 · 필터 · 페이지가 유지된다 (확장 6A).
 */
@Data
public class PublicTripCriteria {

	public static final int PAGE_SIZE = 10;

	/** 지역 목록: schema.sql의 ck_trip_region_region과 같은 17개 시 · 도 */
	public static final List<String> REGIONS = Collections.unmodifiableList(Arrays.asList(
			"서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종", "경기",
			"강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주"));

	private String keyword;  // 여행 제목 검색어
	private String region;   // 지역 1개 (비어 있으면 전체)
	private int days;        // 0 = 전체, 1 = 당일치기, 2 = 1박 2일, 3 = 2박 3일, 4 = 3박 4일 이상
	private int page = 1;

	/** 잘못된 값이 들어오면 기본값으로 바꾼다 (주소를 직접 고쳐 들어온 경우) */
	public void normalize() {
		if (keyword != null) {
			keyword = keyword.trim();
			if (keyword.isEmpty()) {
				keyword = null;
			} else if (keyword.length() > 50) {
				keyword = keyword.substring(0, 50);
			}
		}
		if (region != null && !REGIONS.contains(region)) {
			region = null;
		}
		if (days < 0 || days > 4) {
			days = 0;
		}
		if (page < 1) {
			page = 1;
		}
	}

	/** Oracle OFFSET 값 (1페이지 = 0, 2페이지 = 10 ...) */
	public int getOffset() {
		return (page - 1) * PAGE_SIZE;
	}

	public int getPageSize() {
		return PAGE_SIZE;
	}

	/** 페이지 번호 링크에 붙일 검색 조건 (page 제외). 예) keyword=%EC%A0%9C%EC%A3%BC&region=&days=0 */
	public String getQueryWithoutPage() {
		return "keyword=" + encode(keyword) + "&region=" + encode(region) + "&days=" + days;
	}

	private static String encode(String s) {
		if (s == null) {
			return "";
		}
		try {
			return URLEncoder.encode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return "";
		}
	}
}
