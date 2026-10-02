package com.ltebandslock.data.model

import java.util.UUID

data class RouterProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val ipAddress: String = "192.168.8.1",
    val username: String = "admin",
    val password: String = "admin",
    val isDefault: Boolean = false
)
