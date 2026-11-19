package com.meemo.domain.command

import com.meemo.core.model.Command
import com.meemo.core.model.InputSource

interface CommandParserPort {
    fun parse(rawText: String, source: InputSource = InputSource.TEXT): Command
}
