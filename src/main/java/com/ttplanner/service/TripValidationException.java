package com.ttplanner.service;

import java.util.Map;

/**
 * 입력값 오류 (담당: 박준성)
 * errors의 key는 오류를 표시할 입력 항목: title, period, regions, scope
 * value는 화면에 보여 줄 문구 (유스케이스 명세의 문구 그대로)
 */
public class TripValidationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final Map<String, String> errors;

	public TripValidationException(Map<String, String> errors) {
		super(errors.toString());
		this.errors = errors;
	}

	public Map<String, String> getErrors() {
		return errors;
	}
}
