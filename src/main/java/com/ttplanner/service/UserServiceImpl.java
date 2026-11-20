package com.ttplanner.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ttplanner.mapper.UserMapper;
import com.ttplanner.vo.OwnedTripVO;
import com.ttplanner.vo.UserVO;

import lombok.Setter;
import lombok.extern.log4j.Log4j;

/**
 * 회원 서비스 구현 (담당: 윤아연)
 */
@Log4j
@Service
public class UserServiceImpl implements UserService {

	public static final String MSG_NICKNAME_REQUIRED = "닉네임을 입력하세요.";
	public static final String MSG_NICKNAME_TOO_LONG = "닉네임은 20자 이하로 입력하세요.";
	public static final String MSG_NICKNAME_RESERVED = "사용할 수 없는 닉네임입니다.";
	public static final String MSG_NICKNAME_TAKEN = "이미 사용 중인 닉네임입니다.";
	public static final String MSG_EMAIL_REQUIRED = "이메일 주소를 입력하세요.";
	public static final String MSG_EMAIL_FORMAT = "이메일 형식이 올바르지 않습니다.";
	public static final String MSG_EMAIL_TAKEN = "이미 가입된 이메일 주소입니다.";
	public static final String MSG_PASSWORD_REQUIRED = "비밀번호를 입력하세요.";
	public static final String MSG_PASSWORD_RULE = "8자리 이상, 영문·숫자·특수문자를 각각 1자 이상 넣어 주세요.";
	public static final String MSG_PASSWORD_CONFIRM = "비밀번호와 일치하지 않습니다.";
	public static final String MSG_NEW_PASSWORD_CONFIRM = "새 비밀번호와 일치하지 않습니다.";
	public static final String MSG_CURRENT_PASSWORD_WRONG = "현재 비밀번호가 맞지 않습니다.";

	private static final int NICKNAME_MAX = 20;
	private static final int EMAIL_MAX = 100;
	private static final int PASSWORD_MAX = 64;

	/** 탈퇴 회원의 닉네임이 '탈퇴회원' + 번호로 바뀌므로, 이 글자로 시작하는 닉네임은 쓰지 못하게 한다 */
	private static final String RESERVED_NICKNAME_PREFIX = "탈퇴회원";

	private static final Pattern EMAIL_PATTERN =
			Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

	/** 영문 1자 이상, 숫자 1자 이상, 특수문자(영문 · 숫자 · 공백이 아닌 글자) 1자 이상, 8자 이상 */
	private static final Pattern PASSWORD_PATTERN =
			Pattern.compile("^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[^A-Za-z0-9\\s]).{8,}$");

	@Setter(onMethod_ = @Autowired)
	private UserMapper mapper;

	/** 회원 탈퇴 때 내가 OWNER인 여행 삭제 표시 (박준성, 구성정의서 8장 나.) */
	@Setter(onMethod_ = @Autowired)
	private TripService tripService;

	// ------------------------------------------------------------ 회원가입 (UC-001)

	@Override
	public Map<String, String> join(UserVO user, String passwordConfirm) {
		user.setNickname(trim(user.getNickname()));
		user.setEmail(normalizeEmail(user.getEmail()));

		Map<String, String> errors = new LinkedHashMap<String, String>();
		putIfError(errors, "nickname", checkNickname(user.getNickname(), null));
		putIfError(errors, "email", checkEmail(user.getEmail(), null));

		String password = user.getPassword();
		if (isBlank(password)) {
			errors.put("password", MSG_PASSWORD_REQUIRED);
		} else {
			putIfError(errors, "password", checkPasswordRule(password));
		}
		if (password != null && !password.equals(passwordConfirm)) {
			errors.put("passwordConfirm", MSG_PASSWORD_CONFIRM);
		}
		if (!errors.isEmpty()) {
			return errors;
		}

		user.setPassword(PasswordHasher.hash(password));
		try {
			mapper.insert(user);
		} catch (DuplicateKeyException e) {
			// 검사와 저장 사이에 다른 사람이 같은 값으로 먼저 가입한 경우
			log.info("join duplicate: " + e.getMessage());
			putIfError(errors, "nickname", checkNickname(user.getNickname(), null));
			putIfError(errors, "email", checkEmail(user.getEmail(), null));
			if (errors.isEmpty()) {
				errors.put("nickname", MSG_NICKNAME_TAKEN);
			}
			return errors;
		} finally {
			user.setPassword(null); // 화면으로 돌아갈 때 해시값이 따라가지 않게 지운다
		}
		log.info("join: userId=" + user.getUserId());
		return errors;
	}

	// ------------------------------------------------------------ 로그인 (UC-002)

	@Override
	public UserVO login(String email, String password) {
		UserVO user = mapper.findByEmail(normalizeEmail(email));
		if (user == null || !PasswordHasher.matches(password, user.getPassword())) {
			return null;
		}
		user.setPassword(null);
		return user;
	}

	// ------------------------------------------------------------ 검사

	@Override
	public String checkNickname(String nickname, Long excludeUserId) {
		nickname = trim(nickname);
		if (isBlank(nickname)) {
			return MSG_NICKNAME_REQUIRED;
		}
		if (nickname.length() > NICKNAME_MAX) {
			return MSG_NICKNAME_TOO_LONG;
		}
		if (nickname.startsWith(RESERVED_NICKNAME_PREFIX)) {
			return MSG_NICKNAME_RESERVED;
		}
		if (mapper.countByNickname(nickname, excludeUserId) > 0) {
			return MSG_NICKNAME_TAKEN;
		}
		return null;
	}

	@Override
	public String checkEmail(String email, Long excludeUserId) {
		email = normalizeEmail(email);
		if (isBlank(email)) {
			return MSG_EMAIL_REQUIRED;
		}
		if (email.length() > EMAIL_MAX || !EMAIL_PATTERN.matcher(email).matches()) {
			return MSG_EMAIL_FORMAT;
		}
		if (mapper.countByEmail(email, excludeUserId) > 0) {
			return MSG_EMAIL_TAKEN;
		}
		return null;
	}

	@Override
	public String checkPasswordRule(String password) {
		if (password == null || password.length() > PASSWORD_MAX
				|| !PASSWORD_PATTERN.matcher(password).matches()) {
			return MSG_PASSWORD_RULE;
		}
		return null;
	}

	// ------------------------------------------------------------ 마이페이지 (UC-023)

	@Override
	public UserVO get(Long userId) {
		UserVO user = mapper.findById(userId);
		if (user != null) {
			user.setPassword(null);
		}
		return user;
	}

	@Override
	public Map<String, String> updateInfo(UserVO user) {
		user.setNickname(trim(user.getNickname()));
		user.setEmail(normalizeEmail(user.getEmail()));

		Map<String, String> errors = new LinkedHashMap<String, String>();
		putIfError(errors, "nickname", checkNickname(user.getNickname(), user.getUserId()));
		putIfError(errors, "email", checkEmail(user.getEmail(), user.getUserId()));
		if (!errors.isEmpty()) {
			return errors;
		}
		try {
			mapper.updateInfo(user);
		} catch (DuplicateKeyException e) {
			log.info("updateInfo duplicate: " + e.getMessage());
			putIfError(errors, "nickname", checkNickname(user.getNickname(), user.getUserId()));
			putIfError(errors, "email", checkEmail(user.getEmail(), user.getUserId()));
			if (errors.isEmpty()) {
				errors.put("nickname", MSG_NICKNAME_TAKEN);
			}
		}
		return errors;
	}

	@Override
	public Map<String, String> changePassword(Long userId, String currentPassword,
			String newPassword, String newPasswordConfirm) {
		Map<String, String> errors = new LinkedHashMap<String, String>();
		if (!checkPassword(userId, currentPassword)) {
			errors.put("currentPassword", MSG_CURRENT_PASSWORD_WRONG);
			return errors;
		}
		if (isBlank(newPassword)) {
			errors.put("newPassword", MSG_PASSWORD_REQUIRED);
		} else {
			putIfError(errors, "newPassword", checkPasswordRule(newPassword));
		}
		if (newPassword != null && !newPassword.equals(newPasswordConfirm)) {
			errors.put("newPasswordConfirm", MSG_NEW_PASSWORD_CONFIRM);
		}
		if (errors.isEmpty()) {
			mapper.updatePassword(userId, PasswordHasher.hash(newPassword));
		}
		return errors;
	}

	@Override
	public boolean checkPassword(Long userId, String password) {
		UserVO user = mapper.findById(userId);
		return user != null && PasswordHasher.matches(password, user.getPassword());
	}

	// ------------------------------------------------------------ 회원 탈퇴 (UC-024)

	@Override
	public List<OwnedTripVO> getOwnedTrips(Long userId) {
		return mapper.findOwnedTrips(userId);
	}

	@Override
	public int countJoinedTrips(Long userId) {
		return mapper.countJoinedTrips(userId);
	}

	/**
	 * 회원 탈퇴 (UC-024)
	 * 여러 테이블을 함께 바꾸므로 @Transactional: 중간에 오류가 나면 모두 되돌린다.
	 *
	 * 1) 내가 OWNER인 여행 삭제 표시     → TripService.deleteByOwner(Long userId) → int  (박준성)
	 * 2) 참여 여행 · 초대 · 참여 요청 빼기 → MemberService.deleteByUser(Long userId) → int (양지호)
	 * 3) 회원 탈퇴 표시                   → UserMapper.withdraw (윤아연)
	 * 1), 2)는 구성정의서 v1.4 8장 나. 연결 지점의 메서드다.
	 * 1)은 develop에 들어와서 deleteOwnedTrips()가 TripService.deleteByOwner를 호출한다.
	 * 2)는 아직 develop에 없어서 leaveAllTrips()에 빈 값을 돌려주는 임시 구현으로 둔다 (8장 가.).
	 */
	@Transactional
	@Override
	public boolean withdraw(Long userId) {
		int deletedTrips = deleteOwnedTrips(userId);
		int leftMemberships = leaveAllTrips(userId);
		int result = mapper.withdraw(userId);
		log.info("withdraw: userId=" + userId + ", deletedTrips=" + deletedTrips
				+ ", leftMemberships=" + leftMemberships);
		return result == 1;
	}

	/**
	 * 회원이 OWNER인 여행을 모두 DEL_YN = 'Y'로 표시 → 삭제 표시한 여행 수 (TripService.deleteByOwner, 박준성)
	 * 여행 행과 OWNER의 TRIP_MEMBER 행은 지우지 않는다 → 좋아요 · 통계 유지 (6장 나.)
	 * withdraw()의 @Transactional 안에서 불리므로, 뒤 단계에서 오류가 나면 함께 되돌려진다.
	 */
	private int deleteOwnedTrips(Long userId) {
		return tripService.deleteByOwner(userId);
	}

	/**
	 * 임시 구현: 아무것도 하지 않고 0을 돌려준다.
	 * TODO(양지호): MemberService.deleteByUser(Long userId) → int 가 develop에 들어오면
	 *   return memberService.deleteByUser(userId); 로 바꾼다.
	 *   참여 중인 여행에서 빼고, 받은 초대 · 보낸 참여 요청을 지운다. OWNER 행은 남긴다 (구성정의서 v1.4 8장 나.)
	 */
	private int leaveAllTrips(Long userId) {
		return 0;
	}

	// ------------------------------------------------------------ 도우미

	private static void putIfError(Map<String, String> errors, String field, String message) {
		if (message != null && !errors.containsKey(field)) {
			errors.put(field, message);
		}
	}

	private static boolean isBlank(String s) {
		return s == null || s.trim().isEmpty();
	}

	private static String trim(String s) {
		return s == null ? null : s.trim();
	}

	/** 이메일은 앞뒤 공백을 지우고 소문자로 맞춘다 (Abc@x.com과 abc@x.com을 같은 주소로 본다) */
	private static String normalizeEmail(String email) {
		return email == null ? null : email.trim().toLowerCase();
	}
}
