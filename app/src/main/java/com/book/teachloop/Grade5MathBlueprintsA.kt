package com.book.teachloop

internal fun chapter1To5Blueprints(sourceId: String): List<SubtopicBlueprint>? {
    return when (sourceId) {
        "ch1_large_numbers" -> listOf(
            subtopic(
                idSuffix = "place_chart",
                subtopicTitle = text("Build the place-value chart", "स्थान-मूल्य तालिका बनाओ"),
                knowPrompt = text("Do you already know how each digit gets its value from its place?", "क्या आप जानते हैं कि हर अंक का मान उसके स्थान से बदलता है?"),
                explanationTitle = text("Read the places first", "पहले स्थान पहचानो"),
                explanationParagraphs = listOf(
                    text("Start from ones and move left to tens, hundreds, thousands, and ten-thousands.", "इकाई से शुरू करके दहाई, सैकड़ा, हजार और दस-हजार तक जाइए।"),
                    text("If a digit moves left, its value becomes larger because its place changes.", "जब अंक बाईं ओर जाता है तो उसका मान बढ़ जाता है क्योंकि उसका स्थान बदलता है।"),
                ),
                exampleIndices = listOf(1),
                questionIndices = listOf(0),
                visuals = listOf(
                    tableVisual(
                        title = text("Place chart", "स्थान-मूल्य तालिका"),
                        description = text("Read the number column by column.", "संख्या को स्तंभ-स्तंभ पढ़िए।"),
                        rows = listOf(
                            listOf(text("TTh"), text("Th"), text("H"), text("T"), text("O")),
                            listOf(text("1"), text("3"), text("5"), text("2"), text("0")),
                        ),
                    ),
                ),
                mistakeFocus = MistakeType.PLACE_VALUE,
                tags = listOf(text("place value", "स्थान-मूल्य")),
            ),
            subtopic(
                idSuffix = "read_with_commas",
                subtopicTitle = text("Read numbers with commas", "कॉमा देखकर संख्या पढ़ो"),
                knowPrompt = text("Can you read the thousands part smoothly when commas are present?", "क्या आप कॉमा देखकर हजारों वाला भाग आसानी से पढ़ सकते हैं?"),
                explanationTitle = text("Commas break long numbers into parts", "कॉमा संख्या को हिस्सों में बाँटते हैं"),
                explanationParagraphs = listOf(
                    text("Commas help your eyes stop at the right place while reading a long number.", "कॉमा लंबी संख्या को सही जगह रोककर पढ़ने में मदद करते हैं।"),
                    text("Read the thousands part first and then the hundreds, tens, and ones.", "पहले हजारों वाला भाग पढ़िए, फिर सैकड़ा, दहाई और इकाई पढ़िए।"),
                ),
                exampleIndices = listOf(0, 2),
                questionIndices = listOf(1),
                visuals = listOf(
                    stepVisual(
                        title = text("Reading flow", "पढ़ने का क्रम"),
                        description = text("Pause at the comma and continue.", "कॉमा पर थोड़ा रुककर आगे पढ़िए।"),
                        chips = listOf(text("10"), text("024"), text("ten thousand"), text("twenty-four")),
                    ),
                ),
                mistakeFocus = MistakeType.READING,
                tags = listOf(text("reading numbers", "संख्या पढ़ना")),
            ),
            subtopic(
                idSuffix = "compare_numbers",
                subtopicTitle = text("Compare two large numbers", "दो बड़ी संख्याओं की तुलना"),
                knowPrompt = text("Can you compare two large numbers by checking the leftmost place first?", "क्या आप बाईं ओर वाले बड़े स्थान से शुरुआत करके दो बड़ी संख्याओं की तुलना कर सकते हैं?"),
                explanationTitle = text("Compare the highest place first", "सबसे बड़े स्थान से तुलना करो"),
                explanationParagraphs = listOf(
                    text("The number with the larger digit in the highest place is greater.", "जिस संख्या के सबसे बड़े स्थान पर बड़ा अंक होगा, वही संख्या बड़ी होगी।"),
                    text("If the first place matches, move to the next place and compare again.", "यदि पहला स्थान समान है, तो अगले स्थान पर जाकर तुलना कीजिए।"),
                ),
                exampleIndices = listOf(1, 2),
                questionIndices = listOf(0),
                visuals = listOf(
                    numberLineVisual(
                        title = text("Compare on a line", "रेखा पर तुलना"),
                        description = text("Bigger place value pushes the number further right.", "बड़ा स्थान-मूल्य संख्या को दाईं ओर आगे ले जाता है।"),
                        chips = listOf(text("13,520"), text("45,867")),
                    ),
                ),
                mistakeFocus = MistakeType.PLACE_VALUE,
                tags = listOf(text("comparison", "तुलना")),
            ),
        )

        "ch2_fractions" -> listOf(
            subtopic(
                idSuffix = "same_whole",
                subtopicTitle = text("Check if the whole is same", "देखो कि पूरा भाग एक जैसा है या नहीं"),
                knowPrompt = text("Do you know why fractions can be compared only when the whole is the same?", "क्या आप जानते हैं कि भिन्नों की तुलना तभी ठीक होती है जब पूरा भाग एक जैसा हो?"),
                explanationTitle = text("Fractions depend on the whole", "भिन्न पूरे भाग पर निर्भर है"),
                explanationParagraphs = listOf(
                    text("One half of a big chocolate and one half of a tiny chocolate are not equal amounts.", "एक बड़े चॉकलेट का आधा और छोटे चॉकलेट का आधा मात्रा में बराबर नहीं होते।"),
                    text("So before comparing fractions, always ask whether the wholes match.", "इसलिए तुलना से पहले हमेशा देखिए कि पूरा भाग समान है या नहीं।"),
                ),
                exampleIndices = listOf(2),
                questionIndices = listOf(0),
                visuals = listOf(
                    gridVisual(
                        title = text("Same whole check", "एक जैसा पूरा भाग"),
                        description = text("Equal-sized wholes make fair comparison possible.", "बराबर आकार के पूरे भाग तुलना को सही बनाते हैं।"),
                        rows = listOf(
                            listOf(text("Whole A"), text("1/2")),
                            listOf(text("Whole B"), text("1/2")),
                        ),
                    ),
                ),
                mistakeFocus = MistakeType.FRACTION_COMPARE,
                tags = listOf(text("same whole", "समान पूरा भाग")),
            ),
            subtopic(
                idSuffix = "equivalent_pairs",
                subtopicTitle = text("Make equivalent pairs", "समतुल्य जोड़े बनाओ"),
                knowPrompt = text("Can you see how the same shaded part can have two names?", "क्या आप देख सकते हैं कि वही छायांकित भाग दो नामों से लिखा जा सकता है?"),
                explanationTitle = text("Equivalent fractions name the same part", "समतुल्य भिन्न एक ही भाग को नाम देते हैं"),
                explanationParagraphs = listOf(
                    text("If one half is split into two equal parts, it becomes two fourths.", "यदि आधे भाग को दो बराबर हिस्सों में बाँटें, तो वह two-fourths बन जाता है।"),
                    text("The picture changes its cuts, but the total shaded amount stays the same.", "काटने का तरीका बदलता है, पर छायांकित कुल भाग वही रहता है।"),
                ),
                exampleIndices = listOf(0, 1),
                questionIndices = listOf(0),
                visuals = listOf(
                    tableVisual(
                        title = text("Equivalent table", "समतुल्य तालिका"),
                        description = text("Match one name with another name for the same area.", "एक ही क्षेत्रफल के दो अलग नाम मिलाइए।"),
                        rows = listOf(
                            listOf(text("1/2"), text("2/4")),
                            listOf(text("1/3"), text("2/6")),
                        ),
                    ),
                ),
                mistakeFocus = MistakeType.FRACTION_COMPARE,
                tags = listOf(text("equivalent", "समतुल्य")),
            ),
            subtopic(
                idSuffix = "join_fraction_parts",
                subtopicTitle = text("Join fraction pieces", "भिन्न के टुकड़े जोड़ो"),
                knowPrompt = text("Can you combine equal fraction pieces to make a larger part?", "क्या आप बराबर भिन्न टुकड़ों को जोड़कर बड़ा भाग बना सकते हैं?"),
                explanationTitle = text("Small equal parts can join", "छोटे बराबर भाग मिल सकते हैं"),
                explanationParagraphs = listOf(
                    text("Two quarter pieces together make one half.", "दो चौथाई टुकड़े मिलकर आधा बनाते हैं।"),
                    text("Thinking with pieces helps children answer fraction questions without fear.", "टुकड़ों में सोचने से भिन्न वाले प्रश्न आसान लगते हैं।"),
                ),
                exampleIndices = listOf(2),
                questionIndices = listOf(1),
                visuals = listOf(
                    stepVisual(
                        title = text("Fraction join", "भिन्न जोड़"),
                        description = text("Join equal pieces one by one.", "बराबर टुकड़ों को एक-एक करके जोड़िए।"),
                        chips = listOf(text("1/4"), text("+"), text("1/4"), text("="), text("1/2")),
                    ),
                ),
                mistakeFocus = MistakeType.CONCEPT_CONFUSION,
                tags = listOf(text("pieces", "टुकड़े")),
            ),
        )

        "ch3_angles" -> listOf(
            subtopic(
                idSuffix = "quarter_turn",
                subtopicTitle = text("Feel a quarter turn", "चौथाई मोड़ को महसूस करो"),
                knowPrompt = text("Do you know how a quarter turn looks on your body or a wheel?", "क्या आप जानते हैं कि चौथाई मोड़ शरीर या पहिए पर कैसा दिखता है?"),
                explanationTitle = text("A quarter turn is one-fourth of a full turn", "चौथाई मोड़ पूरे मोड़ का एक-चौथाई है"),
                explanationParagraphs = listOf(
                    text("Turn from front to side and you have made a quarter turn.", "सामने से बगल की ओर घूमना एक quarter turn है।"),
                    text("This turn matches the right angle children see in corners.", "यह मोड़ उसी right angle जैसा है जो कोनों में दिखाई देता है।"),
                ),
                exampleIndices = listOf(0),
                questionIndices = listOf(0),
                visuals = listOf(
                    clockVisual(
                        title = text("Turn map", "मोड़ का मानचित्र"),
                        description = text("Moving from 12 to 3 on a clock is a quarter turn.", "घड़ी में 12 से 3 तक जाना quarter turn है।"),
                        rows = listOf(
                            listOf(text("12"), text("3"), text("quarter")),
                        ),
                    ),
                ),
                mistakeFocus = MistakeType.ANGLE_TURN,
            ),
            subtopic(
                idSuffix = "half_and_full",
                subtopicTitle = text("Half turn and full turn", "आधा और पूरा मोड़"),
                knowPrompt = text("Can you connect a half turn and a full turn with real movement?", "क्या आप आधे और पूरे मोड़ को वास्तविक घूमने से जोड़ सकते हैं?"),
                explanationTitle = text("Two quarter turns make a half turn", "दो चौथाई मोड़ मिलकर आधा मोड़ बनाते हैं"),
                explanationParagraphs = listOf(
                    text("A half turn takes you in the opposite direction.", "आधा मोड़ आपको विपरीत दिशा में ले जाता है।"),
                    text("Four quarter turns together bring you back to the starting position.", "चार quarter turns आपको फिर शुरुआत वाली जगह पर ले आते हैं।"),
                ),
                exampleIndices = listOf(1, 2),
                questionIndices = listOf(1),
                visuals = listOf(
                    stepVisual(
                        title = text("Turn count", "मोड़ की गिनती"),
                        description = text("Count quarter turns to reach half or full turn.", "आधा या पूरा मोड़ पाने के लिए quarter turns गिनिए।"),
                        chips = listOf(text("1/4"), text("1/4"), text("1/4"), text("1/4")),
                    ),
                ),
                mistakeFocus = MistakeType.ANGLE_TURN,
            ),
            subtopic(
                idSuffix = "angle_names",
                subtopicTitle = text("Match turns with angle names", "मोड़ को कोण के नाम से मिलाओ"),
                knowPrompt = text("Can you tell whether a turn is acute, right, or straight?", "क्या आप बता सकते हैं कि कोई मोड़ acute, right या straight है?"),
                explanationTitle = text("Angle names come from how much you turn", "कोण का नाम मोड़ की मात्रा से आता है"),
                explanationParagraphs = listOf(
                    text("Less than a quarter turn makes an acute angle.", "चौथाई मोड़ से कम होने पर acute angle बनता है।"),
                    text("A half turn makes a straight angle because it opens in one straight line.", "आधा मोड़ straight angle बनाता है क्योंकि वह एक सीधी रेखा जैसा खुलता है।"),
                ),
                exampleIndices = listOf(0, 1),
                questionIndices = listOf(0),
                visuals = listOf(
                    tableVisual(
                        title = text("Angle guide", "कोण मार्गदर्शिका"),
                        description = text("Match turn size to angle type.", "मोड़ की मात्रा को कोण के प्रकार से मिलाइए।"),
                        rows = listOf(
                            listOf(text("< 1/4"), text("Acute")),
                            listOf(text("1/4"), text("Right")),
                            listOf(text("1/2"), text("Straight")),
                        ),
                    ),
                ),
                mistakeFocus = MistakeType.ANGLE_TURN,
            ),
        )

        "ch4_add_subtract" -> listOf(
            subtopic(
                idSuffix = "find_total",
                subtopicTitle = text("Find the total", "कुल निकालो"),
                knowPrompt = text("Do you know when a word problem is asking for a total?", "क्या आप पहचान सकते हैं कि शब्द-प्रश्न कुल निकालने को कह रहा है?"),
                explanationTitle = text("More is being added", "यहाँ मात्रा बढ़ रही है"),
                explanationParagraphs = listOf(
                    text("If one amount is joined with another amount, addition is usually needed.", "यदि एक मात्रा में दूसरी मात्रा जुड़ रही है, तो सामान्यतः जोड़ लगता है।"),
                    text("Circle the starting amount and the added amount before solving.", "हल करने से पहले शुरुआती मात्रा और जुड़ी हुई मात्रा को पहचानिए।"),
                ),
                exampleIndices = listOf(0),
                questionIndices = listOf(0),
                visuals = listOf(
                    stepVisual(
                        title = text("Fuel story", "ईंधन कहानी"),
                        description = text("Start amount plus added amount gives the total.", "शुरुआती मात्रा + जोड़ी गई मात्रा = कुल।"),
                        chips = listOf(text("28"), text("+"), text("75"), text("="), text("103")),
                    ),
                ),
                mistakeFocus = MistakeType.OPERATION_LINK,
            ),
            subtopic(
                idSuffix = "undo_with_subtraction",
                subtopicTitle = text("Undo with subtraction", "घटाव से उल्टा सोचो"),
                knowPrompt = text("Can you use subtraction when the total is known and one part is missing?", "क्या आप तब घटाव लगा सकते हैं जब कुल पता हो और एक भाग पता न हो?"),
                explanationTitle = text("Subtraction finds the missing part", "घटाव गुम भाग खोजता है"),
                explanationParagraphs = listOf(
                    text("If the total is known and one part is known, subtraction finds the missing part.", "यदि कुल और एक भाग पता है, तो घटाव गुम भाग निकालता है।"),
                    text("This is why addition and subtraction are partner operations.", "इसीलिए जोड़ और घटाव साथी क्रियाएँ हैं।"),
                ),
                exampleIndices = listOf(1, 2),
                questionIndices = listOf(1),
                visuals = listOf(
                    tableVisual(
                        title = text("Partner operations", "साथी क्रियाएँ"),
                        description = text("One fact can be checked by the reverse fact.", "एक तथ्य को उल्टी क्रिया से जाँचा जा सकता है।"),
                        rows = listOf(
                            listOf(text("78 + 164"), text("242")),
                            listOf(text("242 - 78"), text("164")),
                        ),
                    ),
                ),
                mistakeFocus = MistakeType.OPERATION_LINK,
            ),
            subtopic(
                idSuffix = "check_your_answer",
                subtopicTitle = text("Check your answer", "उत्तर की जाँच करो"),
                knowPrompt = text("Do you know how to check a subtraction answer by adding back?", "क्या आप जानते हैं कि घटाव की जाँच वापस जोड़कर कैसे करते हैं?"),
                explanationTitle = text("Reverse the operation to verify", "उल्टी क्रिया से जाँच करो"),
                explanationParagraphs = listOf(
                    text("After subtraction, add the answer back to the known part to see if the total returns.", "घटाव के बाद उत्तर को ज्ञात भाग में जोड़कर देखिए कि कुल वापस आता है या नहीं।"),
                    text("This quick habit catches many mistakes before you move on.", "यह छोटी आदत आगे बढ़ने से पहले कई गलतियाँ पकड़ लेती है।"),
                ),
                exampleIndices = listOf(2),
                questionIndices = listOf(1),
                visuals = listOf(
                    numberLineVisual(
                        title = text("Check by going back", "वापस जाकर जाँच"),
                        description = text("Move backward with subtraction and forward with addition.", "घटाव में पीछे और जोड़ में आगे बढ़िए।"),
                        chips = listOf(text("18"), text("47"), text("65")),
                    ),
                ),
                mistakeFocus = MistakeType.OPERATION_LINK,
            ),
        )

        "ch5_length" -> listOf(
            subtopic(
                idSuffix = "pick_the_unit",
                subtopicTitle = text("Pick the right unit", "सही इकाई चुनो"),
                knowPrompt = text("Can you decide whether a measure should be in cm, m, or km?", "क्या आप तय कर सकते हैं कि माप cm, m या km में होना चाहिए?"),
                explanationTitle = text("Small, medium, and long distances need different units", "छोटी, मध्यम और लंबी दूरी के लिए अलग इकाइयाँ"),
                explanationParagraphs = listOf(
                    text("Use centimetres for small objects, metres for room-sized lengths, and kilometres for travel routes.", "छोटी चीज़ों के लिए centimetres, कमरे जितनी लंबाइयों के लिए metres और यात्रा मार्गों के लिए kilometres का उपयोग करें।"),
                    text("A sensible unit choice already tells you a lot about the answer.", "सही इकाई चुनना ही उत्तर के बारे में बहुत कुछ बता देता है।"),
                ),
                exampleIndices = listOf(0, 1),
                questionIndices = listOf(0),
                visuals = listOf(
                    tableVisual(
                        title = text("Unit chooser", "इकाई चयन"),
                        description = text("Match object size to the best unit.", "वस्तु के आकार को सही इकाई से मिलाइए।"),
                        rows = listOf(
                            listOf(text("pencil"), text("cm")),
                            listOf(text("door"), text("m")),
                            listOf(text("road"), text("km")),
                        ),
                    ),
                ),
                mistakeFocus = MistakeType.UNIT_CONVERSION,
            ),
            subtopic(
                idSuffix = "convert_units",
                subtopicTitle = text("Convert to one unit", "एक ही इकाई में बदलो"),
                knowPrompt = text("Do you know how to convert km to m before comparing?", "क्या आप तुलना करने से पहले km को m में बदल सकते हैं?"),
                explanationTitle = text("Convert before comparing", "तुलना से पहले रूपांतरण"),
                explanationParagraphs = listOf(
                    text("Two measurements should be in the same unit before you compare them.", "तुलना से पहले दोनों माप एक ही इकाई में होने चाहिए।"),
                    text("Remember the bridge: 1 km = 1000 m and 1 m = 100 cm.", "यह संबंध याद रखें: 1 km = 1000 m और 1 m = 100 cm।"),
                ),
                exampleIndices = listOf(1, 2),
                questionIndices = listOf(1),
                visuals = listOf(
                    stepVisual(
                        title = text("Conversion bridge", "रूपांतरण पुल"),
                        description = text("Move from km to m by multiplying by 1000.", "km से m में जाने के लिए 1000 से गुणा करें।"),
                        chips = listOf(text("2 km"), text("x1000"), text("2000 m")),
                    ),
                ),
                mistakeFocus = MistakeType.UNIT_CONVERSION,
            ),
            subtopic(
                idSuffix = "estimate_reasonably",
                subtopicTitle = text("Estimate reasonably", "उचित अनुमान लगाओ"),
                knowPrompt = text("Can you tell when a measure sounds too big or too small?", "क्या आप पहचान सकते हैं कि कोई माप बहुत बड़ा या बहुत छोटा लग रहा है?"),
                explanationTitle = text("Estimation protects you from silly answers", "अनुमान गलत उत्तरों से बचाता है"),
                explanationParagraphs = listOf(
                    text("If a classroom is said to be 3 kilometres long, the unit choice itself tells you something is wrong.", "यदि किसी कक्षा की लंबाई 3 kilometres बताई जाए, तो इकाई ही बता देती है कि उत्तर गलत है।"),
                    text("Before writing the final answer, ask whether the number and unit make sense together.", "अंतिम उत्तर लिखने से पहले देखिए कि संख्या और इकाई साथ में समझदारी दिखा रहे हैं या नहीं।"),
                ),
                exampleIndices = listOf(2),
                questionIndices = listOf(0),
                visuals = listOf(
                    numberLineVisual(
                        title = text("Reasonable size", "उचित आकार"),
                        description = text("Place tiny, medium, and large lengths in order.", "छोटी, मध्यम और लंबी मापों को क्रम में रखिए।"),
                        chips = listOf(text("cm"), text("m"), text("km")),
                    ),
                ),
                mistakeFocus = MistakeType.MEASUREMENT_ESTIMATE,
            ),
        )

        else -> null
    }
}
