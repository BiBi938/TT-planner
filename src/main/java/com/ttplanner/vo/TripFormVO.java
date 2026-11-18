package com.ttplanner.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * 여행 입력값 (새 여행 만들기 · 제목·지역 수정 · 기간 변경 폼)
 * 담당: 박준성
 *
 * 폼의 name과 필드 이름이 같으면 스프링이 자동으로 채운다.
 *   <input name="title">, <input name="startDate">, <input name="endDate">,
 *   <input type="checkbox" name="regions" value="서울"> (여러 개 고르면 List로 들어온다)
 * 입력 오류로 다시 화면에 돌려줄 때 flash에 담기므로 Serializable로 둔다.
 */
@Data
public class TripFormVO implements Serializable {

	private static final long serialVersionUID = 1L;

	private String title;      // 여행 제목
	private String startDate;  // 시작일 "yyyy-MM-dd"
	private String endDate;    // 종료일 "yyyy-MM-dd"
	private List<String> regions = new ArrayList<>(); // 여행 지역
}
