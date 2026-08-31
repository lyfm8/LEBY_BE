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
    private static final String CLAIM_ROLE    = "role";

    private final JwtProperties jwtProperties;
    private final CookieProperties cookieProperties;

    // -----------------------------------------------------------------------
    // Token Generation
    // -----------------------------------------------------------------------

    /**
     * Sinh Access Token chứa userId, tokenVersion và role.
     * Ký bằng Access Secret, TTL từ application.properties (mặc định 15 phút).
     *
     * Role được nhúng vào claim để JwtAuthenticationFilter đọc trực tiếp
     * mà không cần query DB thêm lần nào cho việc phân quyền.
     *
     * Lưu ý quan trọng: Khi role của user bị thay đổi (ADMIN đổi role),
     * phải gọi incrementTokenVersion() để vô hiệu hóa Access Token cũ
     * còn chứa role cũ — tránh leo thang quyền.
     *
     * @param userId       ID của user.
     * @param tokenVersion Phiên bản token hiện tại.
     * @param role         Tên role (giá trị ERole.name(), ví dụ "STUDENT", "ADMIN").
     */
    public String generateAccessToken(Long userId, Integer tokenVersion, String role) {
        return buildToken(
                userId,
                tokenVersion,
                role,
                jwtProperties.getAccess().getExpirationMs(),
                getAccessSigningKey()
        );
    }

    /**
     * Sinh Refresh Token chứa userId và tokenVersion.
     * Ký bằng Refresh Secret, TTL từ application.properties (mặc định 7 ngày).
     *
     * Refresh Token KHÔNG chứa role claim — chỉ dùng để cấp lại Access Token mới.
     * Role được đọc lại từ DB tại thời điểm refresh để đảm bảo luôn mới nhất.
     */
    public String generateRefreshToken(Long userId, Integer tokenVersion) {
        return buildToken(
                userId,
                tokenVersion,
                null,   // Refresh Token không cần role.
                jwtProperties.getRefresh().getExpirationMs(),
                getRefreshSigningKey()
        );
    }

    private String buildToken(Long userId, Integer version, String role, long ttlMs, Key signingKey) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ttlMs);

        var builder = Jwts.builder()
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_VERSION, version);

        // Role chỉ được nhúng vào Access Token, không có trong Refresh Token.
        if (role != null) {
            builder.claim(CLAIM_ROLE, role);
        }

        return builder
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

    /** Lấy role từ Claims đã parse (chỉ có trong Access Token). */
    public String getRole(Claims claims) {
        return claims.get(CLAIM_ROLE, String.class);
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
