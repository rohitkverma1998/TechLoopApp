package com.book.teachloop

object TeachingScriptBuilder {
    fun build(
        topic: StudyTopic,
        language: AppLanguage,
    ): List<String> {
        val script = mutableListOf<String>()
        val immersiveTeachScript = topic.usesImmersiveTeachExperience()

        if (!immersiveTeachScript) {
            appendLines(script, topic.explanationTitle.display(language))
        }
        topic.explanationParagraphs.forEach { paragraph ->
            appendLines(script, paragraph.display(language))
        }

        topic.visuals.forEach { visual ->
            appendLines(script, visual.title.display(language))
            appendLines(script, visual.description.display(language))
            visual.chips.forEach { chip ->
                appendLines(script, chip.display(language))
            }
            visual.rows.forEach { row ->
                val rowText = row.joinToString(", ") { cell ->
                    cell.display(language).replace("\n", ", ")
                }
                appendLines(script, rowText)
            }
        }

        if (topic.examples.isNotEmpty()) {
            if (immersiveTeachScript) {
                topic.examples.forEach { example ->
                    appendLines(script, example.display(language))
                }
            } else {
                appendLines(script, text("Examples", "उदाहरण").display(language))
                topic.examples.forEachIndexed { index, example ->
                    val prefix = text("Example ${index + 1}", "उदाहरण ${index + 1}").display(language)
                    appendLines(script, "$prefix: ${example.display(language)}")
                }
            }
        }

        return dedupe(script)
    }

    @Suppress("UNUSED_PARAMETER")
    fun buildQuestionSolution(
        topic: StudyTopic,
        question: RenderedQuestion,
        result: QuizResult?,
        language: AppLanguage,
    ): List<String> {
        val script = mutableListOf<String>()

        appendLines(script, text("Question", "प्रश्न").display(language) + ": " + question.prompt.display(language))
        appendLines(script, text("Solution", "समाधान").display(language) + ":")
        question.reteachParagraphs.forEach { appendLines(script, it.display(language)) }

        return dedupe(script)
    }

    private fun dedupe(script: List<String>): List<String> {
        return script
            .map(::normalizeWhitespace)
            .filter { it.isNotBlank() }
            .fold(mutableListOf<String>()) { acc, line ->
                if (acc.lastOrNull() != line) {
                    acc += line
                }
                acc
            }
    }

    private fun appendLines(
        target: MutableList<String>,
        text: String,
    ) {
        splitIntoUnits(text).forEach { line ->
            if (line.isNotBlank()) {
                target += line
            }
        }
    }

    private fun splitIntoUnits(text: String): List<String> {
        return text
            .split(Regex("(?<=[.!?।])\\s+|\\n+"))
            .map(::normalizeWhitespace)
            .filter { it.isNotBlank() }
    }

    private fun normalizeWhitespace(text: String): String {
        return text
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
