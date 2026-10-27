package com.marufallammahim.meemo.core.utilities

import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

interface TimeProvider {
    fun nowMillis(): Long
    fun currentHourOfDay(): Int
}

@Singleton
class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun nowMillis(): Long = System.currentTimeMillis()
    override fun currentHourOfDay(): Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
}
