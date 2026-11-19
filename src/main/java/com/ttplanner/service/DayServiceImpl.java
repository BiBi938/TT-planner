package com.ttplanner.service;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ttplanner.mapper.DayMapper;
import com.ttplanner.vo.DayVO;

import lombok.Setter;
import lombok.extern.log4j.Log4j;

@Log4j
@Service
public class DayServiceImpl implements DayService {

	// 여행 기간은 최대 30일 (제안요청서 SFR-003 규칙을 DAY 추가에도 적용)
	private static final int MAX_DAYS = 30;

	@Setter(onMethod_ = @Autowired)
	private DayMapper mapper;

	@Override
	public List<DayVO> findDays(Long tripId) {
		log.info("findDays......" + tripId);
		return mapper.findDays(tripId);
	}

	@Override
	public DayVO addDay(Long tripId) {
		log.info("addDay......" + tripId);

		// 1. 지금 DAY가 몇 개인지 센다
		int count = mapper.countDays(tripId);

		// 2. 검사: 없는(또는 삭제된) 여행, 30일 제한
		if (count == 0) {
			throw new IllegalStateException("여행을 찾을 수 없습니다.");
		}
		if (count >= MAX_DAYS) {
			throw new IllegalStateException("여행 기간은 최대 30일입니다. DAY를 더 추가할 수 없습니다.");
		}

		// 3. 새 DAY 번호 = 개수 + 1 (DAY_NO는 1부터 빈틈없이 이어지므로)
		DayVO day = new DayVO();
		day.setTripId(tripId);
		day.setDayNo(count + 1);
		mapper.insertDay(day);

		// 4. 다시 조회해서 마지막 DAY(방금 추가한 DAY)를 돌려준다 → dayDate가 새 종료일
		List<DayVO> days = mapper.findDays(tripId);
		return days.get(days.size() - 1);
	}

	// 여러 행을 함께 바꾸므로 @Transactional: 중간에 실패하면 ①②③ 전부 취소
	@Transactional
	@Override
	public void deleteDay(Long tripId, Long dayId) {
		log.info("deleteDay......" + tripId + ", DAY_ID " + dayId);

		// 1. 이 여행의 DAY 목록을 가져와서 검사에 쓴다
		List<DayVO> days = mapper.findDays(tripId);

		if (days.size() == 0) {
			throw new IllegalStateException("여행을 찾을 수 없습니다.");
		}

		// 1B. DAY가 1개만 남았으면 삭제 불가
		if (days.size() == 1) {
			throw new IllegalStateException("DAY가 1개만 남아 있어 삭제할 수 없습니다.");
		}

		// 지울 DAY 찾기: 이 여행의 DAY 중에서만 찾는다
		// (다른 여행의 DAY_ID가 주소로 들어와도 못 찾음 → 다른 여행 DAY를 지울 수 없음)
		// Long끼리는 == 대신 equals()로 비교한다 (== 는 값이 아니라 객체가 같은지 비교)
		DayVO target = null;
		for (DayVO day : days) {
			if (day.getDayId().equals(dayId)) {
				target = day;
			}
		}
		if (target == null) {
			throw new IllegalStateException("DAY를 찾을 수 없습니다.");
		}

		// 1D. 전체 공개 여행인데, 이 DAY를 지우면 남는 일정이 0개가 되는 경우
		String scope = mapper.getTripScope(tripId);
		if ("PUBLIC".equals(scope)) {
			int total = mapper.countTripSchedules(tripId);        // 여행 전체 일정 수
			int remain = total - target.getScheduleCount();      // 지운 뒤 남는 일정 수

			if (target.getScheduleCount() > 0 && remain == 0) {
				throw new IllegalStateException("전체 공개 여행에는 일정이 1개 이상 있어야 합니다. 비공개로 바꾼 뒤 삭제하세요.");
			}
		}

		// 2. 순서대로 삭제: ① 일정 → ② DAY → ③ 뒤 DAY 번호 당기기
		mapper.deleteDaySchedules(target);
		mapper.deleteDay(target);
		mapper.shiftDaysAfter(target);
	}

	@Transactional
	@Override
	public void syncDays(Long tripId, Date start, Date end) {
		log.info("syncDays......" + tripId + ", " + start + " ~ " + end);

		// ① 날짜 검사
		if (start == null || end == null) {
			throw new IllegalStateException("시작일과 종료일을 모두 입력하세요.");
		}
		int newCount = countDaysBetween(start, end);
		if (newCount < 1) {
			throw new IllegalStateException("종료일은 시작일과 같거나 이후여야 합니다.");
		}
		if (newCount > MAX_DAYS) {
			throw new IllegalStateException("여행 기간은 30일 이하로 정해 주세요.");
		}

		// ② 여행이 있는지 (새 여행은 DAY가 0개라 countDays로는 알 수 없음)
		String scope = mapper.getTripScope(tripId);
		if (scope == null) {
			throw new IllegalStateException("여행을 찾을 수 없습니다.");
		}

		int oldCount = mapper.countDays(tripId);

		// ③ 늘어남 → 뒤에 빈 DAY 추가 (예: 3개 → 5개면 DAY 4, 5)
		if (newCount > oldCount) {
			for (int dayNo = oldCount + 1; dayNo <= newCount; dayNo++) {
				DayVO day = new DayVO();
				day.setTripId(tripId);
				day.setDayNo(dayNo);
				mapper.insertDay(day);
			}
		}

		// ④ 줄어듦 → 뒤 DAY의 일정 삭제 → 뒤 DAY 삭제 (예: 5개 → 3개면 DAY 4, 5)
		if (newCount < oldCount) {
			int lost = mapper.countSchedulesAfter(tripId, newCount); // 지워질 일정 수
			int total = mapper.countTripSchedules(tripId);           // 지금 전체 일정 수
			if ("PUBLIC".equals(scope) && lost > 0 && total - lost == 0) {
				throw new IllegalStateException("전체 공개 여행에는 일정이 1개 이상 있어야 합니다. 비공개로 바꾼 뒤 기간을 줄이세요.");
			}
			mapper.deleteSchedulesAfter(tripId, newCount);
			mapper.deleteDaysAfter(tripId, newCount);
		}

		// 같으면 아무것도 안 함 (날짜만 옮긴 경우: DAY 날짜는 START_DATE로 계산되므로 저절로 이동)
	}

	// 시작일 ~ 종료일이 며칠인지 (같은 날 = 1일). 예) 11/20 ~ 11/24 → 5
	private int countDaysBetween(Date start, Date end) {
		long oneDay = 24L * 60 * 60 * 1000;          // 하루 = 86,400,000 밀리초
		long diff = end.getTime() - start.getTime(); // 두 날짜 차이 (밀리초)
		return (int) (diff / oneDay) + 1;
	}
}
