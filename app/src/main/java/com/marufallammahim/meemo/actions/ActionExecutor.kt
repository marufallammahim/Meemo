package com.marufallammahim.meemo.actions

interface ActionExecutor<in A : MeemoAction> {
    suspend fun execute(action: A): ActionResult
}
