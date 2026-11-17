package com.meemo.nlu

import com.meemo.core.model.Command
import com.meemo.core.model.InputSource
import com.meemo.domain.command.CommandParserPort

class CommandParser(
    private val normalizer: InputNormalizer = InputNormalizer(),
    private val synonyms: SynonymTable = SynonymTable(),
    private val matcher: IntentMatcher = IntentMatcher(),
    private val entityExtractor: EntityExtractor = EntityExtractor()
) : CommandParserPort {
    override fun parse(rawText: String, source: InputSource): Command {
        val normalized = synonyms.canonicalize(normalizer.normalize(rawText))
        entityExtractor.extract(normalized)
        val match = matcher.match(normalized)
        return Command(rawText, normalized, match.intent, match.confidence, source)
    }
}
