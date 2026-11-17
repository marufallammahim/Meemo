package com.meemo.domain.command

import com.meemo.core.model.ActionResult
import com.meemo.core.model.Command

interface CommandActionExecutor {
    suspend fun execute(command: Command): ActionResult
}
