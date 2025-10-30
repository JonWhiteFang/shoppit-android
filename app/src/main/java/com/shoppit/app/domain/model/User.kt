package com.shoppit.app.domain.model

/**
 * Domain model representing an authenticated user.
 *
 * @property id Unique identifier from the cloud backend
 * @property email User's email address
 * @property name User's display name
 * @property createdAt Timestamp when the account was created
 */
data class User(
    val id: String,
    val email: String,
    val name: String,
    val createdAt: Long
)
