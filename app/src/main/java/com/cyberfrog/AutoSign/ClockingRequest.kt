package com.cyberfrog.AutoSign

data class ClockingRequest(
    val sessionInfo: SessionInfo,
    val clientTime: String,
    val UUIDTerm: String = "",
    val clientIP: String = "webClocking",
    val periodType: Int = 0,
    val geoloc_x: Double = 0.0,
    val geoloc_y: Double = 0.0,
    val geoloc_Z: Double? = null,
    val geoloc_ACC: Int = 50,
    val SelectPersonCode: String = ""
)

data class SessionInfo(
    val user: String,
    val password: String,
    val caseGUID: String = "16457",
    val client: String = "",
    val casetype: String = ""
)