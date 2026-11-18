package com.ttplanner.service;

/**
 * UC-009 1B: 기간을 줄여 일정이 있는 DAY가 지워지는 경우 (담당: 박준성)
 * 바로 저장하지 않고, 확인 창에 이 문구를 띄워 [삭제하고 저장]을 눌러야 반영한다.
 */
public class TripPeriodConfirmException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public TripPeriodConfirmException(String message) {
		super(message);
	}
}
