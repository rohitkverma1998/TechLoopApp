package com.book.teachloop

import java.util.Locale

object QuestionOptionSupport {
    data class Presentation(
        val type: QuestionType,
        val options: List<LocalizedText>,
        val correctOptionIndex: Int?,
        val solutionAnswer: LocalizedText,
    )

    private val numberRegex = Regex("-?\\d[\\d,]*(?:\\.\\d+)?")
    private val fractionRegex = Regex("^(?:(\\d+)\\s+)?(\\d+)/(\\d+)$")
    private val romanRegex = Regex("^[IVXLCDM]+$", RegexOption.IGNORE_CASE)

    private val geometryPool = listOf(
        text("point", "बिंदु"),
        text("line", "रेखा"),
        text("ray", "किरण"),
        text("line segment", "रेखाखंड"),
        text("curve", "वक्र"),
        text("triangle", "त्रिभुज"),
        text("rectangle", "आयत"),
        text("square", "वर्ग"),
        text("circle", "वृत्त"),
        text("diameter", "व्यास"),
        text("radius", "अर्धव्यास"),
        text("circumference", "परिधि"),
        text("polygon", "बहुभुज"),
    )

    private val numberTheoryPool = listOf(
        text("prime number", "अभाज्य संख्या"),
        text("composite number", "संयोज्य संख्या"),
        text("factor", "गुणनखंड"),
        text("multiple", "गुणज"),
        text("even number", "सम संख्या"),
        text("odd number", "विषम संख्या"),
    )

    private val measurementPool = listOf(
        text("perimeter", "परिमाप"),
        text("area", "क्षेत्रफल"),
        text("volume", "आयतन"),
        text("diameter", "व्यास"),
        text("radius", "अर्धव्यास"),
        text("circumference", "परिधि"),
    )

    private val anglePool = listOf(
        text("acute angle", "न्यून कोण"),
        text("right angle", "समकोण"),
        text("straight angle", "सरल कोण"),
        text("full angle", "पूर्ण कोण"),
        text("parallel lines", "समांतर रेखाएं"),
        text("perpendicular lines", "लंबवत रेखाएं"),
    )

    private val genericPool = listOf(
        text("0"),
        text("1"),
        text("2"),
        text("3"),
        text("4"),
        text("5"),
        text("10"),
        text("100"),
        text("<"),
        text(">"),
        text("="),
        text("Cannot be determined", "निर्धारित नहीं किया जा सकता"),
    )

    fun present(question: QuizQuestion): Presentation? {
        val solutionAnswer = solutionAnswer(question) ?: return null
        return if (question.type == QuestionType.MULTIPLE_CHOICE && question.options.size >= 4) {
            Presentation(
                type = QuestionType.MULTIPLE_CHOICE,
                options = question.options,
                correctOptionIndex = (question.correctOptionIndex ?: 0).coerceIn(0, question.options.lastIndex),
                solutionAnswer = solutionAnswer,
            )
        } else if (question.type == QuestionType.MULTIPLE_CHOICE) {
            topUpMultipleChoice(question, solutionAnswer)
        } else {
            buildGeneratedMultipleChoice(question, solutionAnswer)
        }
    }

    private fun solutionAnswer(question: QuizQuestion): LocalizedText? {
        return when (question.type) {
            QuestionType.MULTIPLE_CHOICE -> {
                if (question.options.isEmpty()) {
                    null
                } else {
                    question.options[(question.correctOptionIndex ?: 0).coerceIn(0, question.options.lastIndex)]
                }
            }

            QuestionType.TEXT_INPUT -> localizedAcceptedAnswer(question.acceptedAnswers)
        }
    }

    private fun localizedAcceptedAnswer(acceptedAnswers: List<String>): LocalizedText? {
        val cleaned = acceptedAnswers
            .map(String::trim)
            .filter(String::isNotBlank)
        if (cleaned.isEmpty()) return null

        val english = cleaned.firstOrNull { !containsDevanagari(it) } ?: cleaned.first()
        val hindi = cleaned.firstOrNull(::containsDevanagari) ?: english
        return text(english, hindi)
    }

    private fun topUpMultipleChoice(
        question: QuizQuestion,
        solutionAnswer: LocalizedText,
    ): Presentation {
        val options = mutableListOf<LocalizedText>()
        question.options.forEach { candidate ->
            if (options.none { sameChoice(it, candidate) }) {
                options += candidate
            }
        }
        if (options.none { sameChoice(it, solutionAnswer) }) {
            options += solutionAnswer
        }

        val distractors = generateDistractors(question, solutionAnswer, options)
        distractors.forEach { candidate ->
            if (options.size < 4 && options.none { sameChoice(it, candidate) }) {
                options += candidate
            }
        }

        for (candidate in fallbackPool(question)) {
            if (options.size >= 4) break
            if (options.none { sameChoice(it, candidate) }) {
                options += candidate
            }
        }

        val correctIndex = options.indexOfFirst { sameChoice(it, solutionAnswer) }.let { index ->
            if (index >= 0) index else 0
        }

        return Presentation(
            type = QuestionType.MULTIPLE_CHOICE,
            options = options,
            correctOptionIndex = correctIndex,
            solutionAnswer = solutionAnswer,
        )
    }

    private fun buildGeneratedMultipleChoice(
        question: QuizQuestion,
        solutionAnswer: LocalizedText,
    ): Presentation {
        val wrongOptions = mutableListOf<LocalizedText>()
        val distractors = generateDistractors(question, solutionAnswer, wrongOptions)
        distractors.forEach { candidate ->
            if (wrongOptions.size < 3 && wrongOptions.none { sameChoice(it, candidate) }) {
                wrongOptions += candidate
            }
        }

        for (candidate in fallbackPool(question)) {
            if (wrongOptions.size >= 3) break
            if (!sameChoice(candidate, solutionAnswer) && wrongOptions.none { sameChoice(it, candidate) }) {
                wrongOptions += candidate
            }
        }

        val correctSlot = stableIndex(question.id, 4)
        val options = mutableListOf<LocalizedText>()
        var wrongIndex = 0
        repeat(4) { slot ->
            if (slot == correctSlot) {
                options += solutionAnswer
            } else {
                options += wrongOptions[wrongIndex++]
            }
        }

        return Presentation(
            type = QuestionType.MULTIPLE_CHOICE,
            options = options,
            correctOptionIndex = correctSlot,
            solutionAnswer = solutionAnswer,
        )
    }

    private fun generateDistractors(
        question: QuizQuestion,
        solutionAnswer: LocalizedText,
        existingOptions: List<LocalizedText>,
    ): List<LocalizedText> {
        val candidates = mutableListOf<LocalizedText>()
        val forbiddenAnswers = mutableListOf<String>().apply {
            addAll(question.acceptedAnswers)
            add(solutionAnswer.english)
            add(solutionAnswer.hindi)
            existingOptions.forEach { option ->
                add(option.english)
                add(option.hindi)
            }
        }

        fun push(items: Iterable<LocalizedText>) {
            items.forEach { candidate ->
                if (candidate.english.isBlank() && candidate.hindi.isBlank()) return@forEach
                if (forbiddenAnswers.any { sameText(candidate.english, it) || sameText(candidate.hindi, it) }) return@forEach
                if (candidates.any { sameChoice(it, candidate) }) return@forEach
                candidates += candidate
            }
        }

        push(symbolDistractors(solutionAnswer))
        push(fractionDistractors(solutionAnswer))
        push(romanDistractors(solutionAnswer))
        push(numericDistractors(solutionAnswer))
        push(listDistractors(solutionAnswer))
        push(fallbackPool(question))
        return candidates
    }

    private fun symbolDistractors(solutionAnswer: LocalizedText): List<LocalizedText> {
        return when (solutionAnswer.english.trim()) {
            "<" -> listOf(text(">"), text("="), text("!="))
            ">" -> listOf(text("<"), text("="), text("!="))
            "=" -> listOf(text("<"), text(">"), text("!="))
            else -> emptyList()
        }
    }

    private fun fractionDistractors(solutionAnswer: LocalizedText): List<LocalizedText> {
        val match = fractionRegex.matchEntire(solutionAnswer.english.trim()) ?: return emptyList()
        val whole = match.groups[1]?.value?.toIntOrNull()
        val numerator = match.groups[2]?.value?.toIntOrNull() ?: return emptyList()
        val denominator = match.groups[3]?.value?.toIntOrNull() ?: return emptyList()
        if (denominator == 0) return emptyList()

        fun formatFraction(updatedWhole: Int?, updatedNumerator: Int, updatedDenominator: Int): String {
            return if (updatedWhole != null) {
                "$updatedWhole $updatedNumerator/$updatedDenominator"
            } else {
                "$updatedNumerator/$updatedDenominator"
            }
        }

        val variants = linkedSetOf(
            formatFraction(whole, numerator + 1, denominator),
            formatFraction(whole, maxOf(1, numerator - 1), denominator),
            formatFraction(whole, numerator, denominator + 1),
            formatFraction(whole, denominator, numerator.coerceAtLeast(1)),
        )
        return variants.map(::text)
    }

    private fun romanDistractors(solutionAnswer: LocalizedText): List<LocalizedText> {
        val raw = solutionAnswer.english.trim().uppercase(Locale.US)
        if (!romanRegex.matches(raw)) return emptyList()
        val value = romanToInt(raw) ?: return emptyList()
        val candidates = linkedSetOf(value + 1, value + 2, value - 1, value + 5)
            .filter { it in 1..3999 }
            .mapNotNull(::intToRoman)
        return candidates.map(::text)
    }

    private fun numericDistractors(solutionAnswer: LocalizedText): List<LocalizedText> {
        val baseText = if (solutionAnswer.english.any { it.isDigit() }) solutionAnswer.english else solutionAnswer.hindi
        val match = numberRegex.find(baseText) ?: return emptyList()
        val rawNumber = match.value.replace(",", "")
        val value = rawNumber.toDoubleOrNull() ?: return emptyList()
        val decimalPlaces = rawNumber.substringAfter('.', "").takeIf { '.' in rawNumber }?.length ?: 0
        val magnitude = when {
            value >= 1000 || value <= -1000 -> 100.0
            value >= 100 || value <= -100 -> 10.0
            value >= 10 || value <= -10 -> 5.0
            else -> 1.0
        }

        val candidateValues = linkedSetOf(
            value + 1,
            value - 1,
            value + magnitude,
            value - magnitude,
            value * 2,
            if (value == 0.0) 1.0 else value / 2,
        ).filter { it >= 0.0 }

        return candidateValues.map { candidate ->
            val replacement = formatNumber(candidate, decimalPlaces)
            text(
                replaceFirstNumber(solutionAnswer.english, replacement),
                replaceFirstNumber(solutionAnswer.hindi, replacement),
            )
        }
    }

    private fun listDistractors(solutionAnswer: LocalizedText): List<LocalizedText> {
        val tokens = solutionAnswer.english
            .split(Regex("\\s*,\\s*|\\s+and\\s+|\\s+or\\s+"))
            .map(String::trim)
            .filter(String::isNotBlank)
        if (tokens.size < 2) return emptyList()

        val variants = mutableListOf<String>()
        variants += tokens.dropLast(1).joinToString(", ")
        variants += tokens.drop(1).joinToString(", ")
        if (tokens.size > 2) {
            variants += tokens.take(2).joinToString(", ")
        }
        return variants.distinct().map(::text)
    }

    private fun fallbackPool(question: QuizQuestion): List<LocalizedText> {
        val prompt = question.prompt.english.lowercase(Locale.US)
        val pool = when {
            listOf("angle", "turn", "parallel", "perpendicular").any(prompt::contains) -> anglePool
            listOf("circle", "radius", "diameter", "circumference", "perimeter", "area", "volume").any(prompt::contains) -> measurementPool
            listOf("factor", "multiple", "prime", "composite", "odd", "even").any(prompt::contains) -> numberTheoryPool
            listOf("point", "line", "ray", "curve", "polygon", "triangle", "rectangle", "square", "hexagon", "circle").any(prompt::contains) -> geometryPool
            else -> geometryPool + numberTheoryPool + measurementPool + anglePool + genericPool
        }
        val offset = stableIndex(question.id, pool.size)
        return pool.drop(offset) + pool.take(offset)
    }

    private fun stableIndex(seed: String, size: Int): Int {
        if (size <= 0) return 0
        return ((seed.hashCode() % size) + size) % size
    }

    private fun replaceFirstNumber(input: String, replacement: String): String {
        val match = numberRegex.find(input) ?: return replacement
        return input.replaceRange(match.range, replacement)
    }

    private fun formatNumber(value: Double, decimalPlaces: Int): String {
        return if (decimalPlaces > 0) {
            "%.${decimalPlaces}f".format(Locale.US, value)
        } else {
            value.toLong().toString()
        }
    }

    private fun sameChoice(left: LocalizedText, right: LocalizedText): Boolean {
        return sameText(left.english, right.english) ||
            sameText(left.english, right.hindi) ||
            sameText(left.hindi, right.english) ||
            sameText(left.hindi, right.hindi)
    }

    private fun sameText(left: String, right: String): Boolean {
        return normalize(left) == normalize(right) ||
            normalizeIgnoringUnits(left) == normalizeIgnoringUnits(right)
    }

    private fun normalize(value: String): String {
        return value
            .trim()
            .lowercase(Locale.US)
            .replace(",", "")
            .replace(".", "")
            .replace("?", "")
            .replace("-", " ")
            .replace("'", "")
            .replace(Regex("\\s+"), " ")
    }

    private fun normalizeIgnoringUnits(value: String): String {
        if (value.none { it.isDigit() }) {
            return normalize(value)
        }

        var sanitized = value.lowercase(Locale.US)
        val unitPatterns = listOf(
            "sq\\.?\\s*(cm|m|km|mm)",
            "cu\\.?\\s*(cm|m|km|mm)",
            "centimetres?",
            "centimeters?",
            "metres?",
            "meters?",
            "kilometres?",
            "kilometers?",
            "millimetres?",
            "millimeters?",
            "kilograms?",
            "grams?",
            "millilitres?",
            "milliliters?",
            "litres?",
            "liters?",
            "rupees?",
            "paise",
            "paisa",
            "cm2",
            "m2",
            "km2",
            "mm2",
            "cm3",
            "m3",
            "km3",
            "mm3",
            "सेमी",
            "मीटर",
            "किमी",
            "मिमी",
            "किग्रा",
            "ग्राम",
            "लीटर",
            "मिलीलीटर",
            "रुपये",
            "रुपया",
            "पैसे",
        )
        unitPatterns.forEach { pattern ->
            sanitized = sanitized.replace(Regex("\\b$pattern\\b"), " ")
        }
        return normalize(sanitized)
    }

    private fun containsDevanagari(value: String): Boolean {
        return value.any { it in '\u0900'..'\u097F' }
    }

    private fun romanToInt(value: String): Int? {
        val romanMap = mapOf(
            'I' to 1,
            'V' to 5,
            'X' to 10,
            'L' to 50,
            'C' to 100,
            'D' to 500,
            'M' to 1000,
        )
        var total = 0
        var index = 0
        while (index < value.length) {
            val current = romanMap[value[index]] ?: return null
            val next = romanMap[value.getOrNull(index + 1)]
            if (next != null && next > current) {
                total += next - current
                index += 2
            } else {
                total += current
                index += 1
            }
        }
        return total
    }

    private fun intToRoman(value: Int): String? {
        if (value !in 1..3999) return null
        val numerals = listOf(
            1000 to "M",
            900 to "CM",
            500 to "D",
            400 to "CD",
            100 to "C",
            90 to "XC",
            50 to "L",
            40 to "XL",
            10 to "X",
            9 to "IX",
            5 to "V",
            4 to "IV",
            1 to "I",
        )
        var remaining = value
        val builder = StringBuilder()
        for ((amount, numeral) in numerals) {
            while (remaining >= amount) {
                builder.append(numeral)
                remaining -= amount
            }
        }
        return builder.toString()
    }
}
