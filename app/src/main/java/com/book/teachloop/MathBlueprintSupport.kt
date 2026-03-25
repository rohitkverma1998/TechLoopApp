package com.book.teachloop

data class SubtopicBlueprint(
    val idSuffix: String,
    val subtopicTitle: LocalizedText,
    val knowPrompt: LocalizedText,
    val explanationTitle: LocalizedText,
    val explanationParagraphs: List<LocalizedText>,
    val exampleIndices: List<Int>,
    val questionIndices: List<Int>,
    val visuals: List<VisualBlock>,
    val mistakeFocus: MistakeType,
    val tags: List<LocalizedText> = emptyList(),
)

fun tableVisual(
    title: LocalizedText,
    description: LocalizedText,
    rows: List<List<LocalizedText>>,
): VisualBlock {
    return VisualBlock(
        title = title,
        description = description,
        kind = VisualKind.TABLE,
        rows = rows,
    )
}

fun numberLineVisual(
    title: LocalizedText,
    description: LocalizedText,
    chips: List<LocalizedText>,
): VisualBlock {
    return VisualBlock(
        title = title,
        description = description,
        kind = VisualKind.NUMBER_LINE,
        chips = chips,
    )
}

fun gridVisual(
    title: LocalizedText,
    description: LocalizedText,
    rows: List<List<LocalizedText>>,
): VisualBlock {
    return VisualBlock(
        title = title,
        description = description,
        kind = VisualKind.GRID,
        rows = rows,
    )
}

fun clockVisual(
    title: LocalizedText,
    description: LocalizedText,
    rows: List<List<LocalizedText>>,
): VisualBlock {
    return VisualBlock(
        title = title,
        description = description,
        kind = VisualKind.CLOCK,
        rows = rows,
    )
}

fun compassVisual(
    title: LocalizedText,
    description: LocalizedText,
    rows: List<List<LocalizedText>>,
): VisualBlock {
    return VisualBlock(
        title = title,
        description = description,
        kind = VisualKind.COMPASS,
        rows = rows,
    )
}

fun pictographVisual(
    title: LocalizedText,
    description: LocalizedText,
    rows: List<List<LocalizedText>>,
): VisualBlock {
    return VisualBlock(
        title = title,
        description = description,
        kind = VisualKind.PICTOGRAPH,
        rows = rows,
    )
}

fun stepVisual(
    title: LocalizedText,
    description: LocalizedText,
    chips: List<LocalizedText>,
): VisualBlock {
    return VisualBlock(
        title = title,
        description = description,
        kind = VisualKind.STEP_FLOW,
        chips = chips,
    )
}

fun subtopic(
    idSuffix: String,
    subtopicTitle: LocalizedText,
    knowPrompt: LocalizedText,
    explanationTitle: LocalizedText,
    explanationParagraphs: List<LocalizedText>,
    exampleIndices: List<Int>,
    questionIndices: List<Int>,
    visuals: List<VisualBlock>,
    mistakeFocus: MistakeType,
    tags: List<LocalizedText> = emptyList(),
): SubtopicBlueprint {
    return SubtopicBlueprint(
        idSuffix = idSuffix,
        subtopicTitle = subtopicTitle,
        knowPrompt = knowPrompt,
        explanationTitle = explanationTitle,
        explanationParagraphs = explanationParagraphs,
        exampleIndices = exampleIndices,
        questionIndices = questionIndices,
        visuals = visuals,
        mistakeFocus = mistakeFocus,
        tags = tags,
    )
}
