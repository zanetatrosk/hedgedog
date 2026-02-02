package com.example.bedanceapp.service

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.SecretKey

@Service
class JwtService {

    @Value("\${jwt.secret}")
    private lateinit var secret: String

    @Value("\${jwt.expiration}")
    private var expiration: Long = 0

    @Value("\${jwt.refresh-expiration}")
    private var refreshExpiration: Long = 0

    private fun getSigningKey(): SecretKey {
        return Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun generateAccessToken(userId: UUID, email: String): String {
        return generateToken(userId, email, expiration)
    }

    fun generateRefreshToken(userId: UUID, email: String): String {
        return generateToken(userId, email, refreshExpiration)
    }

    private fun generateToken(userId: UUID, email: String, expirationTime: Long): String {
        val now = Date()
        val expiryDate = Date(now.time + expirationTime)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(getSigningKey())
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getUserIdFromToken(token: String): UUID {
        val claims = getClaimsFromToken(token)
        return UUID.fromString(claims.subject)
    }

    fun getEmailFromToken(token: String): String {
        val claims = getClaimsFromToken(token)
        return claims.get("email", String::class.java)
    }

    private fun getClaimsFromToken(token: String): Claims {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .payload
    }

    fun getExpirationTime(): Long = expiration
}

