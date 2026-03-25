package com.book.teachloop

internal fun chapter6To10Blueprints(sourceId: String): List<SubtopicBlueprint>? {
    return when (sourceId) {
        "ch6_multiplication" -> listOf(
            subtopic(
                idSuffix = "groups_to_product",
                subtopicTitle = text("See equal groups", "बराबर समूह पहचानो"),
                knowPrompt = text("Do you know how equal groups become a multiplication sentence?", "क्या आप जानते हैं कि बराबर समूह गुणा का वाक्य कैसे बनाते हैं?"),
                explanationTitle = text("Multiplication counts equal groups quickly", "गुणा बराबर समूहों को जल्दी गिनता है"),
                explanationParagraphs = listOf(
                    text("If 6 trays have 13 bottles each, multiplication joins the equal groups in one step.", "यदि 6 ट्रे में 13-13 बोतलें हैं, तो गुणा बराबर समूहों को एक ही कदम में जोड़ देता है।"),
                    text("Changing the order of factors does not change the total product.", "गुणकों का क्रम बदलने से कुल गुणनफल नहीं बदलता।"),
                ),
                exampleIndices = listOf(0),
                questionIndices = listOf(0),
                visuals = listOf(
                    gridVisual(
                        title = text("Array view", "सरणी चित्र"),
                        description = text("Rows and columns show equal groups clearly.", "पंक्तियाँ और स्तंभ बराबर समूहों को साफ दिखाते हैं।"),
                        rows = listOf(
                            listOf(text("6 rows"), text("13 in each row")),
                            listOf(text("6 x 13"), text("78")),
                        ),
                    ),
                ),
                mistakeFocus = MistakeType.OPERATION_LINK,
                tags = listOf(text("equal groups", "बराबर समूह")),
            ),
            subtopic(
                idSuffix = "tens_pattern",
                subtopicTitle = text("Multiply by tens", "दहाइयों से गुणा"),
                knowPrompt = text("Can you use place value to multiply by 20 or 30?", "क्या आप 20 या 30 से गुणा करने में स्थान-मूल्य का उपयोग कर सकते हैं?"),
                explanationTitle = text("Tens change the place value", "दहाइयाँ स्थान-मूल्य बदल देती हैं"),
                explanationParagraphs = listOf(
                    text("When a factor is 20, think of it as 2 tens. The product keeps that tens value.", "जब कोई गुणक 20 हो, तो उसे 2 दहाइयों की तरह सोचो। गुणनफल भी दहाई का मान रखता है।"),
                    text("Tens times tens becomes hundreds, so 30 x 20 gives hundreds.", "दहाई x दहाई = सैकड़ा, इसलिए 30 x 20 में सैकड़ों का परिणाम आता है।"),
                ),
                exampleIndices = listOf(1, 2),
                questionIndices = listOf(0, 1),
                visuals = listOf(
                    stepVisual(
                        title = text("Place-value ladder", "स्थान-मूल्य सीढ़ी"),
                        description = text("Track the jump from ones to tens to hundreds.", "इकाई से दहाई और दहाई से सैकड़ा बनने की छलाँग देखो।"),
                        chips = listOf(text("8 x 20"), text("8 x 2 tens"), text("16 tens"), text("160")),
                    ),
                ),
                mistakeFocus = MistakeType.PLACE_VALUE,
                tags = listOf(text("place value", "स्थान-मूल्य")),
            ),
        )

        "ch7_patterns" -> listOf(
            subtopic(
                idSuffix = "find_weave_rule",
                subtopicTitle = text("Catch the pattern rule", "पैटर्न का नियम पकड़ो"),
                knowPrompt = text("Can you continue a weave after spotting the repeating rule?", "क्या आप दोहराने वाला नियम पहचानकर बुनाई को आगे बढ़ा सकते हैं?"),
                explanationTitle = text("A pattern grows by repeating a rule", "पैटर्न किसी नियम को दोहराकर बढ़ता है"),
                explanationParagraphs = listOf(
                    text("In a weave, over-under or color-color-gap rules repeat again and again.", "बुनाई में ऊपर-नीचे या रंग-रंग-खाली जैसे नियम बार-बार दोहरते हैं।"),
                    text("Once the child says the rule aloud, the next step becomes easier to predict.", "जब बच्चा नियम बोल देता है, तो अगला भाग बताना आसान हो जाता है।"),
                ),
                exampleIndices = listOf(0),
                questionIndices = listOf(0),
                visuals = listOf(
                    stepVisual(
                        title = text("Repeat the weave", "बुनाई दोहराओ"),
                        description = text("Say the rule before drawing the next part.", "अगला भाग बनाने से पहले नियम बोलो।"),
                        chips = listOf(text("under"), text("over"), text("under"), text("over")),
                    ),
                ),
                mistakeFocus = MistakeType.PATTERN_RULE,
                tags = listOf(text("repetition", "दोहराव")),
            ),
            subtopic(
                idSuffix = "shapes_that_fit",
                subtopicTitle = text("Choose shapes that fit", "फिट होने वाली आकृतियाँ चुनो"),
                knowPrompt = text("Do you know why some shapes cover the floor without gaps?", "क्या आप जानते हैं कि कुछ आकृतियाँ फर्श को बिना खाली जगह के क्यों भर देती हैं?"),
                explanationTitle = text("Some corners meet perfectly", "कुछ कोने ठीक-ठीक मिलते हैं"),
                explanationParagraphs = listOf(
                    text("Squares, equilateral triangles, and regular hexagons can meet around a point neatly.", "वर्ग, समबाहु त्रिभुज और नियमित षट्भुज एक बिंदु के चारों ओर ठीक बैठ सकते हैं।"),
                    text("Shapes like regular pentagons leave gaps, so they do not tessellate on their own.", "नियमित पंचभुज जैसी आकृतियाँ खाली जगह छोड़ती हैं, इसलिए वे अकेले टेसेलेट नहीं करतीं।"),
                ),
                exampleIndices = listOf(1, 2),
                questionIndices = listOf(0, 1),
                visuals = listOf(
                    tableVisual(
                        title = text("Tessellation check", "टेसेलेशन जाँच"),
                        description = text("See which shapes fit and which ones leave gaps.", "देखो कौन-सी आकृतियाँ फिट होती हैं और कौन-सी खाली जगह छोड़ती हैं।"),
                        rows = listOf(
                            listOf(text("square"), text("fits")),
                            listOf(text("triangle"), text("fits")),
                            listOf(text("pentagon"), text("gaps")),
                        ),
                    ),
                ),
                mistakeFocus = MistakeType.PATTERN_RULE,
                tags = listOf(text("tessellation", "टेसेलेशन")),
            ),
        )

        "ch8_weight" -> listOf(
            subtopic(
                idSuffix = "estimate_weight",
                subtopicTitle = text("Estimate sensible weight", "उचित वजन का अनुमान"),
                knowPrompt = text("Can you tell when a written weight sounds too small or too large?", "क्या आप पहचान सकते हैं कि लिखा हुआ वजन बहुत छोटा है या बहुत बड़ा?"),
                explanationTitle = text("Estimation protects us from silly answers", "अनुमान हमें बेढंगे उत्तरों से बचाता है"),
                explanationParagraphs = listOf(
                    text("Before converting, think whether the weight makes sense for the object.", "रूपांतरण से पहले सोचो कि वह वजन उस वस्तु के लिए ठीक बैठता है या नहीं।"),
                    text("A refrigerator cannot weigh grams, and a pencil box usually does not weigh kilograms.", "फ्रिज ग्राम में नहीं तौला जाता, और पेंसिल बॉक्स सामान्यतः किलोग्राम में नहीं तौला जाता।"),
                ),
                exampleIndices = listOf(2),
                questionIndices = listOf(1),
                visuals = listOf(
                    tableVisual(
                        title = text("Reasonable estimate", "उचित अनुमान"),
                        description = text("Match the object with a sensible unit.", "वस्तु को समझदारी वाली इकाई से मिलाओ।"),
                        rows = listOf(
                            listOf(text("apple"), text("grams")),
                            listOf(text("school bag"), text("kilograms")),
                            listOf(text("fridge"), text("many kilograms")),
                        ),
                    ),
                ),
                mistakeFocus = MistakeType.MEASUREMENT_ESTIMATE,
                tags = listOf(text("estimate", "अनुमान")),
            ),
            subtopic(
                idSuffix = "convert_and_compare",
                subtopicTitle = text("Convert and compare", "रूपांतरण करके तुलना"),
                knowPrompt = text("Can you compare mixed weights after converting them into one unit?", "क्या आप मिली-जुली मात्राओं को एक इकाई में बदलकर तुलना कर सकते हैं?"),
                explanationTitle = text("One unit makes comparison fair", "एक इकाई तुलना को सही बनाती है"),
                explanationParagraphs = listOf(
                    text("3 kg 500 g becomes 3500 g, so comparison becomes direct.", "3 किग्रा 500 ग्राम, 3500 ग्राम बन जाता है और तुलना सीधी हो जाती है।"),
                    text("The heavier weight is the one with the larger number after both are written in the same unit.", "दोनों को एक इकाई में लिखने के बाद जिसका मान बड़ा हो, वही अधिक भारी होता है।"),
                ),
                exampleIndices = listOf(0, 1, 2),
                questionIndices = listOf(0, 1),
                visuals = listOf(
                    stepVisual(
                        title = text("Conversion path", "रूपांतरण पथ"),
                        description = text("Kilograms become thousands of grams.", "किलोग्राम, हजारों ग्राम में बदलते हैं।"),
                        chips = listOf(text("3 kg"), text("3000 g"), text("+ 500 g"), text("3500 g")),
                    ),
                ),
                mistakeFocus = MistakeType.UNIT_CONVERSION,
                tags = listOf(text("comparison", "तुलना")),
            ),
        )

        "ch9_division" -> listOf(
            subtopic(
                idSuffix = "fact_family",
                subtopicTitle = text("Build the fact family", "तथ्य परिवार बनाओ"),
                knowPrompt = text("Can you read two division facts from one multiplication fact?", "क्या आप एक गुणा तथ्य से दो भाग तथ्य पढ़ सकते हैं?"),
                explanationTitle = text("Multiplication and division are partners", "गुणा और भाग साथी क्रियाएँ हैं"),
                explanationParagraphs = listOf(
                    text("If 5 groups of 7 make 35, then 35 divided by 7 gives 5 and 35 divided by 5 gives 7.", "यदि 7 के 5 समूह मिलकर 35 बनाते हैं, तो 35 ÷ 7 = 5 और 35 ÷ 5 = 7 होगा।"),
                    text("The same three numbers form a family of related multiplication and division facts.", "वही तीन संख्याएँ मिलकर गुणा और भाग का एक संबंधी परिवार बनाती हैं।"),
                ),
                exampleIndices = listOf(0, 1, 2),
                questionIndices = listOf(0),
                visuals = listOf(
                    tableVisual(
                        title = text("Fact family", "तथ्य परिवार"),
                        description = text("One product creates two matching division facts.", "एक गुणनफल दो सही भाग तथ्य बनाता है।"),
                        rows = listOf(
                            listOf(text("5 x 7"), text("35")),
                            listOf(text("35 / 7"), text("5")),
                            listOf(text("35 / 5"), text("7")),
                        ),
                    ),
                ),
                mistakeFocus = MistakeType.OPERATION_LINK,
                tags = listOf(text("fact family", "तथ्य परिवार")),
            ),
            subtopic(
                idSuffix = "equal_share_story",
                subtopicTitle = text("Share equally", "बराबर बाँटो"),
                knowPrompt = text("Do you know how division answers an equal-sharing story?", "क्या आप जानते हैं कि बराबर बाँटने की कहानी का उत्तर भाग से कैसे मिलता है?"),
                explanationTitle = text("Division tells one equal share", "भाग एक बराबर हिस्सा बताता है"),
                explanationParagraphs = listOf(
                    text("When a total distance or total objects are shared across equal days or equal groups, division finds one share.", "जब कुल दूरी या कुल वस्तुएँ बराबर दिनों या बराबर समूहों में बाँटी जाएँ, तो भाग एक हिस्सा बताता है।"),
                    text("The total is the dividend, the number of equal groups is the divisor, and one share is the quotient.", "कुल मात्रा भाज्य होती है, बराबर समूहों की संख्या भाजक और एक हिस्सा भागफल।"),
                ),
                exampleIndices = listOf(0),
                questionIndices = listOf(1),
                visuals = listOf(
                    stepVisual(
                        title = text("Daily share", "दैनिक हिस्सा"),
                        description = text("Total divided into equal days gives one-day distance.", "कुल दूरी को बराबर दिनों में बाँटने पर एक दिन की दूरी मिलती है।"),
                        chips = listOf(text("160 km"), text("/ 20 days"), text("8 km each")),
                    ),
                ),
                mistakeFocus = MistakeType.OPERATION_LINK,
                tags = listOf(text("equal sharing", "बराबर बाँटना")),
            ),
        )

        "ch10_symmetry" -> listOf(
            subtopic(
                idSuffix = "fold_line",
                subtopicTitle = text("Find the fold line", "मोड़ रेखा खोजो"),
                knowPrompt = text("Can you test whether both halves match after a fold?", "क्या आप जाँच सकते हैं कि मोड़ने पर दोनों हिस्से मिलते हैं या नहीं?"),
                explanationTitle = text("Reflection symmetry needs matching halves", "परावर्तन सममिति में दोनों हिस्से मिलते हैं"),
                explanationParagraphs = listOf(
                    text("A line of symmetry works only when the shape folds into two exact matching parts.", "सममिति रेखा तभी सही है जब आकृति मोड़ने पर दो एकदम मिलते हुए हिस्सों में बँटे।"),
                    text("Letters and paper cutouts are easy ways to test a reflection line.", "अक्षर और कागज़ की कटिंग, परावर्तन रेखा जाँचने के आसान तरीके हैं।"),
                ),
                exampleIndices = listOf(0, 1),
                questionIndices = listOf(0),
                visuals = listOf(
                    tableVisual(
                        title = text("Fold test", "मोड़ जाँच"),
                        description = text("Ask whether the two sides overlap after folding.", "पूछो कि मोड़ने पर दोनों भाग एक-दूसरे पर चढ़ते हैं या नहीं।"),
                        rows = listOf(
                            listOf(text("A"), text("vertical yes")),
                            listOf(text("H"), text("vertical yes")),
                            listOf(text("H"), text("horizontal yes")),
                        ),
                    ),
                ),
                mistakeFocus = MistakeType.CONCEPT_CONFUSION,
                tags = listOf(text("reflection", "परावर्तन")),
            ),
            subtopic(
                idSuffix = "turn_and_match",
                subtopicTitle = text("Turn and match again", "घुमाकर फिर से मिलाओ"),
                knowPrompt = text("Can you spot a shape that looks the same after a quarter or half turn?", "क्या आप ऐसी आकृति पहचान सकते हैं जो चौथाई या आधे मोड़ के बाद वैसी ही दिखे?"),
                explanationTitle = text("Rotational symmetry comes from turning", "घूर्णन सममिति घुमाने से दिखती है"),
                explanationParagraphs = listOf(
                    text("A firki or some letters match themselves after a turn smaller than a full turn.", "फिरकी या कुछ अक्षर पूरे चक्कर से पहले भी घुमाने पर फिर वैसे ही दिखते हैं।"),
                    text("Quarter turns and half turns are the easiest way to describe that matching.", "उस मिलान को बताने के लिए चौथाई और आधे मोड़ की भाषा सबसे आसान है।"),
                ),
                exampleIndices = listOf(2),
                questionIndices = listOf(1),
                visuals = listOf(
                    clockVisual(
                        title = text("Turn check", "मोड़ जाँच"),
                        description = text("Use quarter-turn steps to see when the shape matches again.", "यह देखने के लिए चौथाई मोड़ के कदमों का उपयोग करो कि आकृति कब फिर मिलती है।"),
                        rows = listOf(
                            listOf(text("start"), text("1/4"), text("1/2"), text("3/4"), text("full")),
                        ),
                    ),
                ),
                mistakeFocus = MistakeType.ANGLE_TURN,
                tags = listOf(text("rotation", "घूर्णन")),
            ),
        )

        else -> null
    }
}
