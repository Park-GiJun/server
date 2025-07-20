package kr.hhplus.be.server.util

import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import kr.hhplus.be.server.domain.queue.QueueTokenStatus
import kr.hhplus.be.server.dto.QueueTokenStatusRequest
import kr.hhplus.be.server.exception.InvalidTokenException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtQueueTokenUtil(
    @Value("\${jwt.queue.secret}")
    private val secretKey: String,
    @Value("\${jwt.queue.expiration}")
    private val expiration: Long
) {

    private val key: SecretKey = Keys.hmacShaKeyFor(secretKey.toByteArray())

    fun generateToken(
        userId: String,
        concertId: Long,
        position: Int,
        status: QueueTokenStatus
    ): String {
        val now = Date()
        val expireTime = Date(now.time + expiration)

        return try {
            Jwts.builder()
                .setSubject(userId)
                .claim("concertId", concertId)
                .claim("position", position)
                .claim("status", status.name)
                .claim("userId", userId)
                .setIssuedAt(now)
                .setExpiration(expireTime)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact()
        } catch (e: Exception) {
            throw InvalidTokenException("Failed to generate JWT token: ${e.message}")
        }
    }

    fun parseToken(token: String): QueueTokenStatusRequest {
        try {
            val claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .body

            val userId = claims.subject ?: throw InvalidTokenException("Subject not found in token")
            val concertId = claims.get("concertId", java.lang.Long::class.java)?.toLong()
                ?: throw InvalidTokenException("Concert ID not found in token")
            val position = claims.get("position", Integer::class.java)?.toInt() ?: 0
            val statusString = claims.get("status", String::class.java)
                ?: throw InvalidTokenException("Status not found in token")

            val status = try {
                QueueTokenStatus.valueOf(statusString)
            } catch (e: IllegalArgumentException) {
                throw InvalidTokenException("Invalid status in token: $statusString")
            }

            return QueueTokenStatusRequest(
                uuid = token,
                userId=userId,
                position = position,
                concertId = concertId,
                status = status
            )

        } catch (e: ExpiredJwtException) {
            throw InvalidTokenException("Token has expired")
        } catch (e: UnsupportedJwtException) {
            throw InvalidTokenException("Unsupported token")
        } catch (e: MalformedJwtException) {
            throw InvalidTokenException("Malformed token")
        } catch (e: SecurityException) {
            throw InvalidTokenException("Invalid token signature")
        } catch (e: IllegalArgumentException) {
            throw InvalidTokenException("Invalid token")
        } catch (e: InvalidTokenException) {
            throw e
        } catch (e: Exception) {
            throw InvalidTokenException("Token parsing failed: ${e.message}")
        }
    }

    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun isTokenExpired(token: String): Boolean {
        return try {
            val claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .body
            claims.expiration.before(Date())
        } catch (e: Exception) {
            true
        }
    }

    fun getUserIdFromToken(token: String): String? {
        return try {
            val claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .body
            claims.subject
        } catch (e: Exception) {
            null
        }
    }

    fun getConcertIdFromToken(token: String): Long? {
        return try {
            val claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .body
            claims.get("concertId", java.lang.Long::class.java)?.toLong()
        } catch (e: Exception) {
            null
        }
    }

    fun getStatusFromToken(token: String): QueueTokenStatus? {
        return try {
            val claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .body
            val statusString = claims.get("status", String::class.java)
            if (statusString != null) QueueTokenStatus.valueOf(statusString) else null
        } catch (e: Exception) {
            null
        }
    }

    fun refreshToken(oldToken: String, newPosition: Int, newStatus: QueueTokenStatus): String {
        val userId = getUserIdFromToken(oldToken) ?: throw InvalidTokenException("Cannot extract user ID from token")
        val concertId = getConcertIdFromToken(oldToken) ?: throw InvalidTokenException("Cannot extract concert ID from token")

        return generateToken(
            userId = userId,
            concertId = concertId,
            position = newPosition,
            status = newStatus
        )
    }
}