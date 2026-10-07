package com.moasseum.app.domain

/** UI-thread request generation; outdated responses must not overwrite a newer view. */
class LatestRequestGate {
    private var generation = 0L
    fun start(): Long = ++generation
    fun invalidate() { generation++ }
    fun isCurrent(request: Long): Boolean = request == generation
}
