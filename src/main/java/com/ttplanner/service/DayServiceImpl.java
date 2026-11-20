package com.ttplanner.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ttplanner.mapper.DayMapper;
import com.ttplanner.mapper.TripMapper;
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

	// TODO(박준성): TripService.changeStartDate(tripId, startDate)가 생기면
	//              tripService.changeStartDate로 바꾸고 이 필드는 지운다 (UC-012 시작일 저장)
	@Setter(onMethod_ = @Autowired)
	private TripMapper tripMapper;

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
	public String changeDayDate(Long tripId, Long dayId, Date newDate) {
		log.info("changeDayDate......" + tripId + ", DAY_ID " + dayId + " → " + newDate);

		if (newDate == null) {
			throw new IllegalStateException("날짜를 골라 주세요.");
		}

		// 1. 이 여행의 DAY 중에서 고른 DAY 찾기 (deleteDay와 같은 방식)
		List<DayVO> days = mapper.findDays(tripId);
		if (days.size() == 0) {
			throw new IllegalStateException("여행을 찾을 수 없습니다.");
		}
		DayVO target = null;
		for (DayVO day : days) {
			if (day.getDayId().equals(dayId)) {
				target = day;
			}
		}
		if (target == null) {
			throw new IllegalStateException("DAY를 찾을 수 없습니다.");
		}

		// 2. 새 시작일 = 고른 날짜 - (DAY 번호 - 1)일   예) DAY 2를 11/26으로 → 11/25
		long oneDay = 24L * 60 * 60 * 1000; // 하루 = 86,400,000 밀리초
		Date newStart = new Date(newDate.getTime() - (target.getDayNo() - 1) * oneDay);

		// 3. 시작일 저장 → DAY 날짜는 START_DATE + DAY_NO - 1 이라 모든 DAY가 함께 이동
		tripMapper.updateStartDate(tripId, newStart); // TODO(박준성): tripService.changeStartDate로 교체

		// 4. 바뀐 DAY 목록으로 새 기간(첫 DAY ~ 마지막 DAY)을 구해 문구 만들기
		List<DayVO> changed = mapper.findDays(tripId);
		SimpleDateFormat format = new SimpleDateFormat("M/d");
		String first = format.format(changed.get(0).getDayDate());
		String last = format.format(changed.get(changed.size() - 1).getDayDate());

		return "DAY " + target.getDayNo() + "을 " + format.format(newDate) + "로 옮겼습니다. "
				+ "여행 기간이 " + first + " ~ " + last + "로 함께 바뀌었습니다.";
	}
}
