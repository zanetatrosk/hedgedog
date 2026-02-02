package com.example.bedanceapp.repository

import com.example.bedanceapp.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserRepository : JpaRepository<User, UUID> {
    fun findByEmail(email: String): User?
    fun findByProviderAndProviderId(provider: String, providerId: String): User?
}
