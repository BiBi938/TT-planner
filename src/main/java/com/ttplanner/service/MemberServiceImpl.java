package com.ttplanner.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ttplanner.mapper.MemberMapper;

import lombok.Setter;
import lombok.extern.log4j.Log4j;

@Log4j
@Service
public class MemberServiceImpl implements MemberService {

	@Setter(onMethod_ = @Autowired)
	private MemberMapper mapper;

	@Override
	public String getRole(Long tripId, Long userId) {
		// 로그인하지 않았으면 DB에 물어볼 필요 없이 "멤버 아님"
		if (userId == null) {
			return null;
		}
		String role = mapper.getRole(tripId, userId);
		log.info("getRole...... tripId=" + tripId + ", userId=" + userId + " → " + role);
		return role;
	}
}
