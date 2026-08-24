package com.example.be.common.security;

import com.example.be.common.exception.BaseException;
import com.example.be.common.exception.ErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * Xử lý toàn bộ vòng đời của JWT: sinh token, parse, tạo/xóa Cookie.
 *
 * Access Token:  thời hạn ngắn (15 phút), dùng để xác thực mỗi request.
 * Refresh Token: thời hạn dài  (7 ngày),  dùng để lấy Access Token mới.
 *
 * Cả hai loại đều chứa: userId (Long) và version (Integer).
 * version dùng để đối chiếu với tokenVersion trong DB khi xác thực.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_VERSION = "version";

    private final JwtProperties jwtProperties;
    private final CookieProperties cookieProperties;

    // -----------------------------------------------------------------------
    // Token Generation
    // -----------------------------------------------------------------------

    /**
     * Sinh Access Token chứa userId và tokenVersion.
     * Ký bằng Access Secret, TTL từ application.properties (mặc định 15 phút).
     */
    public String generateAccessToken(Long userId, Integer tokenVersion) {
        return buildToken(
                userId,
                tokenVersion,
                jwtProperties.getAccess().getExpirationMs(),
                getAccessSigningKey()
        );
    }

    /**
     * Sinh Refresh Token chứa userId và tokenVersion.
     * Ký bằng Refresh Secret, TTL từ application.properties (mặc định 7 ngày).
     */
    public String generateRefreshToken(Long userId, Integer tokenVersion) {
        return buildToken(
                userId,
                tokenVersion,
                jwtProperties.getRefresh().getExpirationMs(),
                getRefreshSigningKey()
        );
    }

    private String buildToken(Long userId, Integer version, long ttlMs, Key signingKey) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ttlMs);

        return Jwts.builder()
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_VERSION, version)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // -----------------------------------------------------------------------
    // Token Parsing
    // -----------------------------------------------------------------------

    /**
     * Parse và xác thực Access Token.
     *
     * @throws BaseException TOKEN_EXPIRED nếu token hết hạn.
     * @throws BaseException TOKEN_INVALID nếu token sai định dạng hoặc bị giả mạo.
     */
    public Claims parseAccessToken(String token) {
        return parseClaims(token, getAccessSigningKey());
    }

    /**
     * Parse và xác thực Refresh Token.
     *
     * @throws BaseException TOKEN_EXPIRED nếu token hết hạn.
     * @throws BaseException TOKEN_INVALID nếu token sai định dạng hoặc bị giả mạo.
     */
    public Claims parseRefreshToken(String token) {
        return parseClaims(token, getRefreshSigningKey());
    }

    private Claims parseClaims(String token, Key signingKey) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException ex) {
            throw new BaseException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BaseException(ErrorCode.TOKEN_INVALID);
        }
    }

    /** Lấy userId từ Claims đã parse. */
    public Long getUserId(Claims claims) {
        return claims.get(CLAIM_USER_ID, Long.class);
    }

    /** Lấy tokenVersion từ Claims đã parse. */
    public Integer getVersion(Claims claims) {
        return claims.get(CLAIM_VERSION, Integer.class);
    }

    // -----------------------------------------------------------------------
    // Cookie Utilities
    // -----------------------------------------------------------------------

    /**
     * Tạo HTTP-Only Cookie chứa Access Token.
     * SameSite=Strict bảo vệ chống CSRF mà không cần CSRF Token.
     *
     * @param accessToken giá trị Access Token.
     * @return ResponseCookie sẵn sàng được gắn vào Response Header.
     */
    public ResponseCookie buildAccessTokenCookie(String accessToken) {
        return buildCookie(
                cookieProperties.getAccessTokenName(),
                accessToken,
                (int) (jwtProperties.getAccess().getExpirationMs() / 1000)
        );
    }

    /**
     * Tạo HTTP-Only Cookie chứa Refresh Token.
     *
     * @param refreshToken giá trị Refresh Token.
     * @return ResponseCookie sẵn sàng được gắn vào Response Header.
     */
    public ResponseCookie buildRefreshTokenCookie(String refreshToken) {
        return buildCookie(
                cookieProperties.getRefreshTokenName(),
                refreshToken,
                (int) (jwtProperties.getRefresh().getExpirationMs() / 1000)
        );
    }

    /**
     * Tạo Cookie rỗng (Max-Age=0) để xóa Access Token Cookie trên browser.
     */
    public ResponseCookie clearAccessTokenCookie() {
        return clearCookie(cookieProperties.getAccessTokenName());
    }

    /**
     * Tạo Cookie rỗng (Max-Age=0) để xóa Refresh Token Cookie trên browser.
     */
    public ResponseCookie clearRefreshTokenCookie() {
        return clearCookie(cookieProperties.getRefreshTokenName());
    }

    private ResponseCookie buildCookie(String name, String value, int maxAgeSeconds) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(false) // Đặt true khi deploy HTTPS (production)
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }

    private ResponseCookie clearCookie(String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();
    }

    // -----------------------------------------------------------------------
    // Signing Key Helpers
    // -----------------------------------------------------------------------

    private Key getAccessSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(
                java.util.Base64.getEncoder().encodeToString(
                        jwtProperties.getAccess().getSecret().getBytes()
                )
        );
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private Key getRefreshSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(
                java.util.Base64.getEncoder().encodeToString(
                        jwtProperties.getRefresh().getSecret().getBytes()
                )
        );
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
