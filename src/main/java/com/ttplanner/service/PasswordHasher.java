package com.ttplanner.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * 비밀번호 해시 도우미 (담당: 윤아연)
 *
 * pom.xml에 라이브러리를 추가하지 않고 Java 8 기본 기능(PBKDF2)만 쓴다.
 * DB에는 "반복횟수:소금(salt):해시" 형태의 문자열로 저장한다. (약 75자, PASSWORD 컬럼 100자 안에 들어감)
 *   예) 65536:Qm9vbGVhbi4uLg==:Zm9vYmFyLi4u...
 * 같은 비밀번호라도 소금이 매번 달라서 저장값이 서로 다르다.
 */
public final class PasswordHasher {

	private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
	private static final int ITERATIONS = 65536;
	private static final int SALT_BYTES = 16;
	private static final int HASH_BITS = 256;

	private static final SecureRandom RANDOM = new SecureRandom();

	private PasswordHasher() {
	}

	/** 평문 비밀번호 → 저장용 문자열 */
	public static String hash(String rawPassword) {
		byte[] salt = new byte[SALT_BYTES];
		RANDOM.nextBytes(salt);
		byte[] hash = pbkdf2(rawPassword, salt, ITERATIONS);
		Base64.Encoder enc = Base64.getEncoder();
		return ITERATIONS + ":" + enc.encodeToString(salt) + ":" + enc.encodeToString(hash);
	}

	/** 입력한 비밀번호가 저장값과 맞는지 확인한다 */
	public static boolean matches(String rawPassword, String stored) {
		if (rawPassword == null || stored == null) {
			return false;
		}
		String[] parts = stored.split(":");
		if (parts.length != 3) {
			return false; // 탈퇴 회원('-') 등 형식이 다른 값
		}
		try {
			int iterations = Integer.parseInt(parts[0]);
			Base64.Decoder dec = Base64.getDecoder();
			byte[] salt = dec.decode(parts[1]);
			byte[] expected = dec.decode(parts[2]);
			byte[] actual = pbkdf2(rawPassword, salt, iterations);
			// 걸린 시간으로 정답을 추측하지 못하도록 끝까지 비교하는 메서드를 쓴다
			return MessageDigest.isEqual(expected, actual);
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	private static byte[] pbkdf2(String password, byte[] salt, int iterations) {
		PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, HASH_BITS);
		try {
			return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new IllegalStateException("비밀번호 해시를 만들 수 없습니다.", e);
		} finally {
			spec.clearPassword();
		}
	}
}
