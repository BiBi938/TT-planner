package com.ttplanner.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.ttplanner.vo.OwnedTripVO;
import com.ttplanner.vo.UserVO;

/**
 * 회원 매퍼 (SQL은 src/main/resources/mapper/UserMapper.xml)
 * 담당: 윤아연
 */
public interface UserMapper {

	/** 회원가입: seq_users.nextval로 USER_ID를 받아 user.userId에 채운다 */
	int insert(UserVO user);

	/** 로그인용: 탈퇴하지 않은 회원을 이메일로 찾는다 (없으면 null) */
	UserVO findByEmail(String email);

	/** 탈퇴하지 않은 회원을 번호로 찾는다 (없으면 null) */
	UserVO findById(Long userId);

	/** 닉네임 중복 수. excludeUserId가 있으면 그 회원(나 자신)은 빼고 센다 */
	int countByNickname(@Param("nickname") String nickname, @Param("excludeUserId") Long excludeUserId);

	/** 이메일 중복 수. excludeUserId가 있으면 그 회원(나 자신)은 빼고 센다 */
	int countByEmail(@Param("email") String email, @Param("excludeUserId") Long excludeUserId);

	/** 닉네임 · 이메일 수정 (UC-023) */
	int updateInfo(UserVO user);

	/** 비밀번호 수정 (UC-023) */
	int updatePassword(@Param("userId") Long userId, @Param("password") String password);

	/** 내가 OWNER인 여행 목록 (UC-024 탈퇴 안내) */
	List<OwnedTripVO> findOwnedTrips(Long userId);

	/** 내가 멤버(OWNER 제외)로 참여 중인 여행 수 (UC-024 탈퇴 안내) */
	int countJoinedTrips(Long userId);

	/** 탈퇴: 회원 행을 탈퇴 표시하고 닉네임 · 이메일을 '탈퇴회원' + USER_ID로 바꾼다
	 *  (내 여행 삭제 · 멤버 제외는 다른 팀원 담당 → UserServiceImpl.withdraw()의 TODO 참고) */
	int withdraw(Long userId);
}
