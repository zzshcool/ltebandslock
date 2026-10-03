package com.ltebandslock.data.model

data class SmsMessage(
    val index: Long = 0,
    val phone: String = "",
    val content: String = "",
    val date: String = "",
    val isUnread: Boolean = false
)

data class SmsCount(
    val total: Int = 0,
    val unread: Int = 0
)
