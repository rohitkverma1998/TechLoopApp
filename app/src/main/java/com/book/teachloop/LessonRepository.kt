package com.book.teachloop

import android.content.Context

object LessonRepository {
    const val BOOK_ID = "class5_rs_aggarwal_maths"

    private val builtInCatalog = listOf(
        SubjectPackCatalogItem(
            id = BOOK_ID,
            title = text("RS Aggarwal Mathematics Class 5", "आर. एस. अग्रवाल गणित कक्षा 5"),
            assetPath = "subject_packs/class5_rs_aggarwal_math.json",
        ),
        SubjectPackCatalogItem(
            id = "class5_science_story_lab",
            title = text("Science Story Lab", "साइंस स्टोरी लैब"),
            assetPath = "subject_packs/class5_science_story_lab.json",
        ),
        SubjectPackCatalogItem(
            id = "class5_english_reading_trails",
            title = text("English Reading Trails", "इंग्लिश रीडिंग ट्रेल्स"),
            assetPath = "subject_packs/class5_english_reading_trails.json",
        ),
    )

    private val inMemoryBooks = mutableMapOf<String, StudyBook>()

    fun catalog(context: Context): List<SubjectPackCatalogItem> {
        return SubjectPackLoader.loadCatalog(context) ?: builtInCatalog
    }

    fun book(context: Context, id: String): StudyBook {
        return inMemoryBooks.getOrPut(id) {
            val catalogItem = catalog(context).firstOrNull { it.id == id }
            val assetBook = catalogItem?.assetPath?.let { SubjectPackLoader.loadBook(context, it) }
            assetBook ?: builtInBook(id)
        }
    }

    fun builtInMathBook(): StudyBook = builtInBook(BOOK_ID)

    private fun builtInBook(id: String): StudyBook {
        return when (id) {
            BOOK_ID -> authoredGrade5MathBook()
            else -> authoredGrade5MathBook()
        }
    }

    private fun authoredGrade5MathBook(): StudyBook {
        return StudyBook(
            id = BOOK_ID,
            subjectTitle = text("Mathematics", "गणित"),
            bookTitle = text("RS Aggarwal Mathematics Class 5", "आर. एस. अग्रवाल गणित कक्षा 5"),
            teacherNote = text(
                english = "Use the main path first, then revision and weak-topic practice with the book examples.",
                hindi = "पहले मुख्य पथ चलाइए, फिर पुनरावृत्ति और कमज़ोर-विषय अभ्यास कराइए।",
            ),
            topics = baseGrade5MathTopics().flatMap(::expandTopicIntoAuthoredSubtopics),
        )
    }

    private fun expandTopicIntoAuthoredSubtopics(source: LessonTopic): List<StudyTopic> {
        val blueprints = chapter1To5Blueprints(source.id)
            ?: chapter6To10Blueprints(source.id)
            ?: chapter11To15Blueprints(source.id)
            ?: emptyList()

        return blueprints.map { blueprint ->
            StudyTopic(
                id = "${source.id}_${blueprint.idSuffix}",
                sourceLessonId = source.id,
                chapterNumber = source.chapterNumber,
                chapterTitle = source.chapterTitle,
                lessonTitle = source.topicTitle,
                subtopicTitle = blueprint.subtopicTitle,
                knowPrompt = blueprint.knowPrompt,
                explanationTitle = blueprint.explanationTitle,
                explanationParagraphs = blueprint.explanationParagraphs,
                examples = blueprint.exampleIndices.mapNotNull(source.examples::getOrNull),
                visuals = blueprint.visuals,
                questions = blueprint.questionIndices.mapNotNull(source.questions::getOrNull),
                tags = blueprint.tags,
                mistakeFocus = blueprint.mistakeFocus,
            )
        }
    }

    private fun baseGrade5MathTopics(): List<LessonTopic> {
        return listOf(
            topic(
                id = "ch1_large_numbers",
                chapterNumber = 1,
                chapterTitle = text("We the Travellers - I", "हम यात्री - 1"),
                topicTitle = text("Reading and Writing Large Numbers", "बड़ी संख्याएँ पढ़ना और लिखना"),
                examples = listOf(
                    text("10,024 is read as ten thousand twenty-four.", "10,024 को दस हजार चौबीस पढ़ते हैं।"),
                    text("13,520 has 1 ten-thousand, 3 thousands, 5 hundreds, 2 tens, and 0 ones.", "13,520 में 1 दस-हजार, 3 हजार, 5 सैकड़े, 2 दहाइयाँ और 0 इकाइयाँ हैं।"),
                    text("45,867 is larger than 13,520 because the ten-thousand place is greater.", "45,867, 13,520 से बड़ा है क्योंकि उसका दस-हजार वाला अंक बड़ा है।"),
                ),
                questions = listOf(
                    mcq(
                        id = "ch1_q1",
                        prompt = text("Which option correctly reads 10,024?", "10,024 को सही तरह से कौन पढ़ता है?"),
                        options = listOf(
                            text("One thousand twenty-four", "एक हजार चौबीस"),
                            text("Ten thousand twenty-four", "दस हजार चौबीस"),
                            text("Ten thousand two hundred four", "दस हजार दो सौ चार"),
                            text("One lakh twenty-four", "एक लाख चौबीस"),
                        ),
                        correctOptionIndex = 1,
                        hint = text("Look at the ten-thousands place first.", "पहले दस-हजार वाले स्थान को देखो।"),
                        supportExample = text("10,024 = 10 thousand + 24.", "10,024 = 10 हजार + 24।"),
                        mistakeType = MistakeType.READING,
                        reteachTitle = text("Read the number in parts", "संख्या को हिस्सों में पढ़ो"),
                        reteachParagraphs = listOf(
                            text("Pause at the comma, read the thousands part, then the last three digits.", "कॉमा पर रुको, पहले हजारों वाला भाग पढ़ो, फिर आखिरी तीन अंक पढ़ो।"),
                        ),
                    ),
                    input(
                        id = "ch1_q2",
                        prompt = text("Fill in the answer: 13,520 is read as", "उत्तर भरो: 13,520 को कैसे पढ़ेंगे?"),
                        acceptedAnswers = listOf(
                            "thirteen thousand five hundred twenty",
                            "thirteen thousand five hundred and twenty",
                            "तेरह हजार पाँच सौ बीस",
                            "तेरह हजार पांच सौ बीस",
                        ),
                        hint = text("Read the number from the thousands place first.", "संख्या को पहले हजारों के स्थान से पढ़ो।"),
                        supportExample = text("13,520 = 13 thousand + 520.", "13,520 = 13 हजार + 520।"),
                        mistakeType = MistakeType.PLACE_VALUE,
                        reteachTitle = text("Match each digit with its place", "हर अंक को उसके स्थान से मिलाओ"),
                        reteachParagraphs = listOf(
                            text("Write the number in a place-value chart before reading it aloud.", "संख्या को पहले स्थान-मूल्य तालिका में लिखो, फिर बोलकर पढ़ो।"),
                        ),
                    ),
                ),
            ),
            topic(
                id = "ch2_fractions",
                chapterNumber = 2,
                chapterTitle = text("Fractions", "भिन्न"),
                topicTitle = text("Comparing and Building Equivalent Fractions", "भिन्नों की तुलना और समतुल्य भिन्न"),
                examples = listOf(
                    text("1/2 = 2/4", "1/2 = 2/4"),
                    text("1/3 = 2/6", "1/3 = 2/6"),
                    text("Two quarter pieces make one half.", "दो चौथाई मिलकर एक आधा बनाते हैं।"),
                ),
                questions = listOf(
                    mcq(
                        id = "ch2_q1",
                        prompt = text("Which fraction is equivalent to 1/2?", "1/2 के बराबर कौन-सा भिन्न है?"),
                        options = listOf(text("2/4", "2/4"), text("1/3", "1/3"), text("2/5", "2/5"), text("3/8", "3/8")),
                        correctOptionIndex = 0,
                        hint = text("Think of two quarter pieces making a half.", "सोचो: दो चौथाई मिलकर आधा बनाते हैं।"),
                        supportExample = text("Shade one half, then split the same region into four equal parts.", "पहले आधा भाग छायांकित करो, फिर उसी भाग को चार बराबर हिस्सों में बाँटो।"),
                        mistakeType = MistakeType.FRACTION_COMPARE,
                        reteachTitle = text("Keep the whole same", "पूरे भाग को एक जैसा रखो"),
                        reteachParagraphs = listOf(text("Equivalent fractions change the number of pieces, not the total shaded amount.", "समतुल्य भिन्नों में टुकड़ों की संख्या बदलती है, कुल भाग नहीं।")),
                    ),
                    input(
                        id = "ch2_q2",
                        prompt = text("How many 1/4 pieces make 1/2?", "कितने 1/4 के टुकड़े मिलकर 1/2 बनाते हैं?"),
                        acceptedAnswers = listOf("2", "two", "दो"),
                        hint = text("Join equal quarter pieces mentally.", "बराबर चौथाई टुकड़ों को मन में जोड़ो।"),
                        supportExample = text("1/4 + 1/4 = 1/2", "1/4 + 1/4 = 1/2"),
                        mistakeType = MistakeType.CONCEPT_CONFUSION,
                        reteachTitle = text("Join equal pieces", "बराबर टुकड़ों को जोड़ो"),
                        reteachParagraphs = listOf(text("When the pieces are equal, we can count how many make the larger fraction.", "जब टुकड़े बराबर होते हैं, तब हम गिनते हैं कि बड़े भिन्न को कितने टुकड़े बनाते हैं।")),
                    ),
                ),
            ),
            topic(
                id = "ch3_angles",
                chapterNumber = 3,
                chapterTitle = text("Angles as Turns", "मोड़ के रूप में कोण"),
                topicTitle = text("Understanding Angles Through Turns", "मोड़ों से कोण समझना"),
                examples = listOf(
                    text("A quarter turn makes a right angle.", "चौथाई मोड़ एक समकोण बनाता है।"),
                    text("A half turn makes a straight angle.", "आधा मोड़ सरल कोण बनाता है।"),
                    text("Four quarter turns make one full turn.", "चार चौथाई मोड़ एक पूरा मोड़ बनाते हैं।"),
                ),
                questions = listOf(
                    mcq(
                        id = "ch3_q1",
                        prompt = text("A quarter turn makes which angle?", "चौथाई मोड़ कौन-सा कोण बनाता है?"),
                        options = listOf(text("Acute angle", "न्यून कोण"), text("Right angle", "समकोण"), text("Straight angle", "सरल कोण"), text("Full angle", "पूर्ण कोण")),
                        correctOptionIndex = 1,
                        hint = text("Think of turning from 12 to 3 on a clock.", "घड़ी में 12 से 3 तक घूमने की कल्पना करो।"),
                        supportExample = text("One corner of a notebook shows the same turn.", "कॉपी का एक कोना भी यही मोड़ दिखाता है।"),
                        mistakeType = MistakeType.ANGLE_TURN,
                        reteachTitle = text("Match turn size with the angle", "मोड़ की मात्रा को कोण से मिलाओ"),
                        reteachParagraphs = listOf(text("Quarter turn = right angle, half turn = straight angle.", "चौथाई मोड़ = समकोण, आधा मोड़ = सरल कोण।")),
                    ),
                    input(
                        id = "ch3_q2",
                        prompt = text("How many quarter turns make one full turn?", "एक पूरा मोड़ बनाने के लिए कितने चौथाई मोड़ चाहिए?"),
                        acceptedAnswers = listOf("4", "four", "चार"),
                        hint = text("A full circle has four equal quarters.", "पूरा घेरा चार बराबर चौथाइयों का होता है।"),
                        supportExample = text("12 -> 3 -> 6 -> 9 -> 12 shows four quarter turns.", "12 -> 3 -> 6 -> 9 -> 12 चार चौथाई मोड़ दिखाते हैं।"),
                        mistakeType = MistakeType.ANGLE_TURN,
                        reteachTitle = text("Count quarter turns", "चौथाई मोड़ गिनो"),
                        reteachParagraphs = listOf(text("Mark four equal stops in a circle and count the jumps.", "घेरे में चार बराबर ठहराव बनाकर छलाँगें गिनो।")),
                    ),
                ),
            ),
            topic(
                id = "ch4_add_subtract",
                chapterNumber = 4,
                chapterTitle = text("We the Travellers - II", "हम यात्री - 2"),
                topicTitle = text("Addition, Subtraction, and Their Relationship", "जोड़, घटाव और उनका संबंध"),
                examples = listOf(
                    text("28 + 75 = 103", "28 + 75 = 103"),
                    text("If 78 + 164 = 242, then 242 - 78 = 164.", "यदि 78 + 164 = 242, तो 242 - 78 = 164।"),
                    text("If 65 - 18 = 47, then 18 + 47 = 65.", "यदि 65 - 18 = 47, तो 18 + 47 = 65।"),
                ),
                questions = listOf(
                    input(
                        id = "ch4_q1",
                        prompt = text("A lorry has 28 litres of fuel and 75 litres are added. What is the total quantity?", "एक लॉरी में 28 लीटर ईंधन है और 75 लीटर और डाला गया। कुल कितना ईंधन हुआ?"),
                        acceptedAnswers = listOf("103", "103 litres", "103 liters", "103 लीटर"),
                        hint = text("Add the tens and ones carefully.", "दहाइयों और इकाइयों को ध्यान से जोड़ो।"),
                        supportExample = text("28 + 70 = 98, then add 5 more to get 103.", "28 + 70 = 98, फिर 5 जोड़कर 103 बनता है।"),
                        mistakeType = MistakeType.OPERATION_LINK,
                        reteachTitle = text("Look for the total", "कुल मात्रा खोजो"),
                        reteachParagraphs = listOf(text("When more quantity is added to the starting quantity, use addition.", "जब शुरुआती मात्रा में और मात्रा जुड़ती है, तो जोड़ का उपयोग करो।")),
                    ),
                    mcq(
                        id = "ch4_q2",
                        prompt = text("If 78 + 164 = 242, then 242 - 78 = ?", "यदि 78 + 164 = 242, तो 242 - 78 = ?"),
                        options = listOf(text("86", "86"), text("154", "154"), text("164", "164"), text("174", "174")),
                        correctOptionIndex = 2,
                        hint = text("Subtraction can undo addition.", "घटाव जोड़ को उल्टा कर देता है।"),
                        supportExample = text("Total - one part = the missing part.", "कुल - एक भाग = छूटा हुआ भाग।"),
                        mistakeType = MistakeType.OPERATION_LINK,
                        reteachTitle = text("Reverse the operation", "क्रिया को उल्टा करो"),
                        reteachParagraphs = listOf(text("Check addition by subtracting one addend from the sum.", "जोड़ की जाँच के लिए योग में से एक संख्या घटाओ।")),
                    ),
                ),
            ),
            topic(
                id = "ch5_length",
                chapterNumber = 5,
                chapterTitle = text("Far and Near", "दूर और पास"),
                topicTitle = text("Measuring Length and Distance", "लंबाई और दूरी मापना"),
                examples = listOf(
                    text("100 cm = 1 m", "100 सेमी = 1 मीटर"),
                    text("1000 m = 1 km", "1000 मीटर = 1 किलोमीटर"),
                    text("5 m = 500 cm, so 456 cm is shorter than 5 m.", "5 मीटर = 500 सेमी, इसलिए 456 सेमी, 5 मीटर से छोटा है।"),
                ),
                questions = listOf(
                    mcq(
                        id = "ch5_q1",
                        prompt = text("How many metres are there in 1 kilometre?", "1 किलोमीटर में कितने मीटर होते हैं?"),
                        options = listOf(text("100", "100"), text("500", "500"), text("1000", "1000"), text("10000", "10000")),
                        correctOptionIndex = 2,
                        hint = text("Kilo means one thousand.", "किलो का अर्थ एक हजार होता है।"),
                        supportExample = text("2 km = 2000 m, so 1 km = 1000 m.", "2 किमी = 2000 मीटर, इसलिए 1 किमी = 1000 मीटर।"),
                        mistakeType = MistakeType.UNIT_CONVERSION,
                        reteachTitle = text("Use the conversion bridge", "रूपांतरण पुल का उपयोग करो"),
                        reteachParagraphs = listOf(text("Kilometre to metre means multiply by 1000.", "किलोमीटर से मीटर जाने पर 1000 से गुणा करते हैं।")),
                    ),
                    input(
                        id = "ch5_q2",
                        prompt = text("Convert 2 km into metres.", "2 किमी को मीटर में बदलो।"),
                        acceptedAnswers = listOf("2000", "2000 m", "2000 metres", "2000 meters", "2000 मीटर"),
                        hint = text("Each kilometre is 1000 metres.", "हर किलोमीटर 1000 मीटर होता है।"),
                        supportExample = text("1 km = 1000 m, so 2 km = 2 x 1000 m.", "1 किमी = 1000 मीटर, इसलिए 2 किमी = 2 x 1000 मीटर।"),
                        mistakeType = MistakeType.UNIT_CONVERSION,
                        reteachTitle = text("Multiply by 1000", "1000 से गुणा करो"),
                        reteachParagraphs = listOf(text("For km to m, multiply. For m to km, divide.", "किमी से मीटर के लिए गुणा करो। मीटर से किमी के लिए भाग दो।")),
                    ),
                ),
            ),
            topic(
                id = "ch6_multiplication",
                chapterNumber = 6,
                chapterTitle = text("The Dairy Farm", "डेयरी फार्म"),
                topicTitle = text("Multiplication Patterns and Place Value", "गुणा के पैटर्न और स्थान-मूल्य"),
                examples = listOf(text("6 x 13 = 78", "6 x 13 = 78"), text("8 x 20 = 160", "8 x 20 = 160"), text("30 x 20 = 600", "30 x 20 = 600")),
                questions = listOf(
                    mcq(
                        id = "ch6_q1",
                        prompt = text("Which value is equal to 8 x 20?", "8 x 20 के बराबर कौन-सा मान है?"),
                        options = listOf(text("28", "28"), text("80", "80"), text("160", "160"), text("800", "800")),
                        correctOptionIndex = 2,
                        hint = text("8 x 2 tens = 16 tens.", "8 x 2 दहाई = 16 दहाई।"),
                        supportExample = text("16 tens = 160.", "16 दहाई = 160।"),
                        mistakeType = MistakeType.PLACE_VALUE,
                        reteachTitle = text("Think in tens", "दहाइयों में सोचो"),
                        reteachParagraphs = listOf(text("Multiply the simple numbers first, then attach the tens value.", "पहले सरल संख्याओं का गुणा करो, फिर दहाई का मान जोड़ो।")),
                    ),
                    input(
                        id = "ch6_q2",
                        prompt = text("Fill in the answer: 30 x 20 =", "उत्तर भरो: 30 x 20 ="),
                        acceptedAnswers = listOf("600", "six hundred", "छह सौ"),
                        hint = text("3 tens x 2 tens = 6 hundreds.", "3 दहाई x 2 दहाई = 6 सैकड़े।"),
                        supportExample = text("30 x 2 = 60, then multiply by 10 again to get 600.", "30 x 2 = 60, फिर 10 से गुणा कर 600 मिलता है।"),
                        mistakeType = MistakeType.PLACE_VALUE,
                        reteachTitle = text("Track the zeroes through place value", "शून्यों को स्थान-मूल्य से समझो"),
                        reteachParagraphs = listOf(text("Tens times tens becomes hundreds.", "दहाई x दहाई = सैकड़ा होता है।")),
                    ),
                ),
            ),
            topic(
                id = "ch7_patterns",
                chapterNumber = 7,
                chapterTitle = text("Shapes and Patterns", "आकार और पैटर्न"),
                topicTitle = text("Weaving Patterns and Tessellation", "बुनाई के पैटर्न और टेसेलेशन"),
                examples = listOf(text("Under-over repeating weave", "नीचे-ऊपर दोहराता हुआ बुनाई पैटर्न"), text("Squares tessellate", "वर्ग टेसेलेट करते हैं"), text("Regular pentagons leave gaps", "नियमित पंचभुज खाली जगह छोड़ते हैं")),
                questions = listOf(
                    mcq(
                        id = "ch7_q1",
                        prompt = text("Which regular shape can tessellate without gaps?", "कौन-सी नियमित आकृति बिना खाली जगह छोड़े टेसेलेट करती है?"),
                        options = listOf(text("Regular pentagon", "नियमित पंचभुज"), text("Regular octagon", "नियमित अष्टभुज"), text("Equilateral triangle", "समबाहु त्रिभुज"), text("Circle", "वृत्त")),
                        correctOptionIndex = 2,
                        hint = text("Think about fitting shapes around a point exactly.", "सोचो कि कौन-सी आकृति एक बिंदु के चारों ओर ठीक बैठती है।"),
                        supportExample = text("Six equilateral triangles can meet around one point neatly.", "छह समबाहु त्रिभुज एक बिंदु के चारों ओर ठीक बैठते हैं।"),
                        mistakeType = MistakeType.PATTERN_RULE,
                        reteachTitle = text("Test the corners", "कोनों को परखो"),
                        reteachParagraphs = listOf(text("A shape tessellates when copies meet without gaps or overlaps.", "जब प्रतियाँ बिना खाली जगह या ओवरलैप के मिलें, तब आकृति टेसेलेट करती है।")),
                    ),
                    input(
                        id = "ch7_q2",
                        prompt = text("How many squares fit around one point without gaps?", "एक बिंदु के चारों ओर बिना खाली जगह कितने वर्ग बैठते हैं?"),
                        acceptedAnswers = listOf("4", "four", "चार"),
                        hint = text("Each square corner is a right angle.", "हर वर्ग का कोना समकोण होता है।"),
                        supportExample = text("4 right angles make one full turn.", "4 समकोण मिलकर एक पूरा चक्कर बनाते हैं।"),
                        mistakeType = MistakeType.PATTERN_RULE,
                        reteachTitle = text("Count the corner angles", "कोने के कोण गिनो"),
                        reteachParagraphs = listOf(text("Four 90-degree corners fill a full turn exactly.", "चार 90 डिग्री के कोने एक पूरा चक्कर ठीक-ठीक भर देते हैं।")),
                    ),
                ),
            ),
            topic(
                id = "ch8_weight",
                chapterNumber = 8,
                chapterTitle = text("Weight and Capacity", "वजन और धारिता"),
                topicTitle = text("Converting Between Kilograms and Grams", "किलोग्राम और ग्राम में रूपांतरण"),
                examples = listOf(text("1 kg = 1000 g", "1 किग्रा = 1000 ग्राम"), text("3 kg 500 g = 3500 g", "3 किग्रा 500 ग्राम = 3500 ग्राम"), text("5 kg 50 g is heavier than 4 kg 500 g", "5 किग्रा 50 ग्राम, 4 किग्रा 500 ग्राम से भारी है")),
                questions = listOf(
                    input(
                        id = "ch8_q1",
                        prompt = text("Convert 3 kg 500 g into grams.", "3 किग्रा 500 ग्राम को ग्राम में बदलो।"),
                        acceptedAnswers = listOf("3500", "3500 g", "3500 grams", "3500 ग्राम"),
                        hint = text("3 kg means 3000 g. Add 500 g more.", "3 किग्रा = 3000 ग्राम। उसमें 500 ग्राम और जोड़ो।"),
                        supportExample = text("3000 g + 500 g = 3500 g", "3000 ग्राम + 500 ग्राम = 3500 ग्राम"),
                        mistakeType = MistakeType.UNIT_CONVERSION,
                        reteachTitle = text("Break the mixed measure", "मिली-जुली मात्रा को तोड़ो"),
                        reteachParagraphs = listOf(text("Convert kilograms first, then add the extra grams.", "पहले किलोग्राम को ग्राम में बदलो, फिर अतिरिक्त ग्राम जोड़ो।")),
                    ),
                    mcq(
                        id = "ch8_q2",
                        prompt = text("Which weight is greater?", "कौन-सा वजन अधिक है?"),
                        options = listOf(text("4 kg 500 g", "4 किग्रा 500 ग्राम"), text("5 kg 50 g", "5 किग्रा 50 ग्राम"), text("Both are equal", "दोनों बराबर हैं"), text("Cannot compare", "तुलना नहीं कर सकते")),
                        correctOptionIndex = 1,
                        hint = text("Convert both to grams first.", "दोनों को पहले ग्राम में बदलो।"),
                        supportExample = text("4 kg 500 g = 4500 g and 5 kg 50 g = 5050 g.", "4 किग्रा 500 ग्राम = 4500 ग्राम और 5 किग्रा 50 ग्राम = 5050 ग्राम।"),
                        mistakeType = MistakeType.MEASUREMENT_ESTIMATE,
                        reteachTitle = text("Compare in one unit", "एक इकाई में तुलना करो"),
                        reteachParagraphs = listOf(text("Different-looking weights become easy to compare after conversion.", "रूपांतरण के बाद अलग दिखने वाले वजन भी आसानी से तुलना किए जा सकते हैं।")),
                    ),
                ),
            ),
            topic(
                id = "ch9_division",
                chapterNumber = 9,
                chapterTitle = text("Coconut Farm", "नारियल फार्म"),
                topicTitle = text("Connecting Multiplication and Division", "गुणा और भाग का संबंध"),
                examples = listOf(text("5 x 7 = 35", "5 x 7 = 35"), text("35 / 7 = 5", "35 / 7 = 5"), text("35 / 5 = 7", "35 / 5 = 7")),
                questions = listOf(
                    mcq(
                        id = "ch9_q1",
                        prompt = text("If 5 x 7 = 35, which division fact is correct?", "यदि 5 x 7 = 35, तो कौन-सा भाग तथ्य सही है?"),
                        options = listOf(text("35 / 7 = 5", "35 / 7 = 5"), text("35 / 7 = 7", "35 / 7 = 7"), text("7 / 35 = 5", "7 / 35 = 5"), text("5 / 35 = 7", "5 / 35 = 7")),
                        correctOptionIndex = 0,
                        hint = text("The product becomes the dividend.", "गुणनफल भाग देने वाली संख्या बनता है।"),
                        supportExample = text("In a x b = c, c / b = a.", "यदि a x b = c है, तो c / b = a।"),
                        mistakeType = MistakeType.OPERATION_LINK,
                        reteachTitle = text("Read division from multiplication", "गुणा से भाग पढ़ो"),
                        reteachParagraphs = listOf(text("One multiplication fact gives two division facts.", "एक गुणा तथ्य से दो भाग तथ्य बनते हैं।")),
                    ),
                    input(
                        id = "ch9_q2",
                        prompt = text("Sabina cycles 160 km in 20 equal days. How many kilometres does she cycle each day?", "सबीना 160 किमी दूरी 20 बराबर दिनों में तय करती है। वह प्रतिदिन कितने किमी चलती है?"),
                        acceptedAnswers = listOf("8", "8 km", "8 kilometres", "8 kilometers", "8 किमी"),
                        hint = text("Divide the total distance by the number of days.", "कुल दूरी को दिनों की संख्या से भाग दो।"),
                        supportExample = text("160 ÷ 20 = 8", "160 ÷ 20 = 8"),
                        mistakeType = MistakeType.OPERATION_LINK,
                        reteachTitle = text("Share equally", "बराबर बाँटो"),
                        reteachParagraphs = listOf(text("Division tells how much one equal group gets from the total.", "भाग बताता है कि कुल से एक बराबर समूह को कितना मिलता है।")),
                    ),
                ),
            ),
            topic(
                id = "ch10_symmetry",
                chapterNumber = 10,
                chapterTitle = text("Symmetrical Designs", "सममित डिज़ाइन"),
                topicTitle = text("Reflection and Rotational Symmetry", "परावर्तन और घूर्णन सममिति"),
                examples = listOf(text("A has a vertical line of symmetry", "A में एक ऊर्ध्वाधर सममिति रेखा है"), text("H has vertical and horizontal symmetry", "H में ऊर्ध्वाधर और क्षैतिज सममिति है"), text("A firki can match after quarter turns", "फिरकी चौथाई मोड़ के बाद भी मेल खा सकती है")),
                questions = listOf(
                    mcq(
                        id = "ch10_q1",
                        prompt = text("Which letter has both vertical and horizontal lines of symmetry?", "किस अक्षर में ऊर्ध्वाधर और क्षैतिज दोनों सममिति रेखाएँ हैं?"),
                        options = listOf(text("A", "A"), text("H", "H"), text("K", "K"), text("N", "N")),
                        correctOptionIndex = 1,
                        hint = text("Imagine folding the letter both ways.", "कल्पना करो कि अक्षर को दोनों दिशाओं में मोड़ रहे हो।"),
                        supportExample = text("H matches after both a vertical fold and a horizontal fold.", "H, ऊर्ध्वाधर और क्षैतिज दोनों मोड़ों पर मेल खाता है।"),
                        mistakeType = MistakeType.CONCEPT_CONFUSION,
                        reteachTitle = text("Check each fold", "हर मोड़ को जाँचो"),
                        reteachParagraphs = listOf(text("A reflection line works only when both halves overlap exactly.", "सममिति रेखा तभी सही है जब दोनों हिस्से बिल्कुल एक-दूसरे पर चढ़ जाएँ।")),
                    ),
                    input(
                        id = "ch10_q2",
                        prompt = text("How many quarter turns make one full turn for a firki?", "फिरकी के लिए एक पूरा चक्कर बनाने में कितने चौथाई मोड़ लगते हैं?"),
                        acceptedAnswers = listOf("4", "four", "चार"),
                        hint = text("A full circle has four equal quarters.", "पूरा घेरा चार बराबर चौथाइयों का होता है।"),
                        supportExample = text("Quarter + quarter + quarter + quarter = full turn.", "चौथाई + चौथाई + चौथाई + चौथाई = पूरा चक्कर।"),
                        mistakeType = MistakeType.ANGLE_TURN,
                        reteachTitle = text("Count the turns", "मोड़ गिनो"),
                        reteachParagraphs = listOf(text("Rotational symmetry often uses quarter-turn and half-turn language.", "घूर्णन सममिति में चौथाई और आधे मोड़ की भाषा बहुत काम आती है।")),
                    ),
                ),
            ),
            topic(
                id = "ch11_perimeter_area",
                chapterNumber = 11,
                chapterTitle = text("Grandmother's Quilt", "दादी की रजाई"),
                topicTitle = text("Perimeter and Area Through Tiling", "टाइलिंग से परिमाप और क्षेत्रफल"),
                examples = listOf(text("A square of side 5 cm has perimeter 20 cm", "5 सेमी भुजा वाले वर्ग का परिमाप 20 सेमी है"), text("Perimeter is the border length", "परिमाप सीमा की लंबाई है"), text("Area is the inside region covered by tiles", "क्षेत्रफल अंदर का ढका हुआ भाग है")),
                questions = listOf(
                    input(
                        id = "ch11_q1",
                        prompt = text("What is the perimeter of a square with side 5 cm?", "5 सेमी भुजा वाले वर्ग का परिमाप क्या होगा?"),
                        acceptedAnswers = listOf("20", "20 cm", "20 centimetres", "20 centimeters", "20 सेमी"),
                        hint = text("A square has four equal sides.", "वर्ग की चारों भुजाएँ बराबर होती हैं।"),
                        supportExample = text("5 + 5 + 5 + 5 = 20", "5 + 5 + 5 + 5 = 20"),
                        mistakeType = MistakeType.CONCEPT_CONFUSION,
                        reteachTitle = text("Walk around the border", "सीमा के चारों ओर चलो"),
                        reteachParagraphs = listOf(text("Perimeter is found by adding every outside side.", "परिमाप निकालने के लिए बाहर की सभी भुजाएँ जोड़ते हैं।")),
                    ),
                    mcq(
                        id = "ch11_q2",
                        prompt = text("Which shape leaves gaps when used alone to cover a table top?", "मेज़ की सतह को अकेले ढकने पर कौन-सी आकृति खाली जगह छोड़ती है?"),
                        options = listOf(text("Square", "वर्ग"), text("Rectangle", "आयत"), text("Triangle", "त्रिभुज"), text("Circle", "वृत्त")),
                        correctOptionIndex = 3,
                        hint = text("Think about tiling without gaps or overlaps.", "बिना खाली जगह या ओवरलैप के टाइलिंग के बारे में सोचो।"),
                        supportExample = text("Circles touch at points but leave curved gaps between them.", "वृत्त कुछ बिंदुओं पर मिलते हैं, पर उनके बीच खाली जगह बचती है।"),
                        mistakeType = MistakeType.CONCEPT_CONFUSION,
                        reteachTitle = text("Tile the surface mentally", "मन में सतह को टाइल करो"),
                        reteachParagraphs = listOf(text("Shapes used for exact tiling should cover the plane fully.", "सही टाइलिंग के लिए आकृतियों को सतह पूरी तरह भरनी चाहिए।")),
                    ),
                ),
            ),
            topic(
                id = "ch12_time",
                chapterNumber = 12,
                chapterTitle = text("Racing Seconds", "दौड़ती सेकंड"),
                topicTitle = text("Reading Time, Elapsed Time, and Seconds", "समय पढ़ना, बीता समय और सेकंड"),
                examples = listOf(text("1 minute = 60 seconds", "1 मिनट = 60 सेकंड"), text("02:30 p.m. = 14:30", "02:30 p.m. = 14:30"), text("05:30 a.m. = 05:30", "05:30 a.m. = 05:30")),
                questions = listOf(
                    input(
                        id = "ch12_q1",
                        prompt = text("Fill in the answer: 1 minute = ____ seconds", "उत्तर भरो: 1 मिनट = ____ सेकंड"),
                        acceptedAnswers = listOf("60", "sixty", "साठ"),
                        hint = text("Watch the second hand complete one round.", "सेकंड वाली सुई का एक पूरा चक्कर देखो।"),
                        supportExample = text("One full round of the second hand takes 60 seconds.", "सेकंड वाली सुई का एक पूरा चक्कर 60 सेकंड लेता है।"),
                        mistakeType = MistakeType.TIME_READING,
                        reteachTitle = text("Use the clock hand", "घड़ी की सुई का उपयोग करो"),
                        reteachParagraphs = listOf(text("The second hand must pass all 60 marks to make one minute.", "एक मिनट पूरा करने के लिए सेकंड वाली सुई 60 निशानों से गुजरती है।")),
                    ),
                    mcq(
                        id = "ch12_q2",
                        prompt = text("What is 02:30 p.m. in 24-hour format?", "02:30 p.m. को 24-घंटे के रूप में कैसे लिखेंगे?"),
                        options = listOf(text("02:30", "02:30"), text("12:30", "12:30"), text("14:30", "14:30"), text("22:30", "22:30")),
                        correctOptionIndex = 2,
                        hint = text("For p.m. time after noon, add 12 to the hour.", "दोपहर के बाद वाले p.m. समय में घंटे में 12 जोड़ते हैं।"),
                        supportExample = text("2 + 12 = 14, so 2:30 p.m. becomes 14:30.", "2 + 12 = 14, इसलिए 2:30 p.m. = 14:30।"),
                        mistakeType = MistakeType.TIME_READING,
                        reteachTitle = text("Switch to 24-hour time", "24-घंटे के समय में बदलो"),
                        reteachParagraphs = listOf(text("Morning a.m. times stay the same; p.m. times usually need 12 added.", "सुबह वाले a.m. समय वैसे ही रहते हैं; p.m. समय में प्रायः 12 जोड़ा जाता है।")),
                    ),
                ),
            ),
            topic(
                id = "ch13_factors_multiples",
                chapterNumber = 13,
                chapterTitle = text("Animal Jumps", "जानवरों की छलाँग"),
                topicTitle = text("Factors, Multiples, and Common Multiples", "गुणनखंड, गुणज और समान गुणज"),
                examples = listOf(text("Factors of 12 are 1, 2, 3, 4, 6, and 12", "12 के गुणनखंड 1, 2, 3, 4, 6 और 12 हैं"), text("12 is a common multiple of 3 and 4", "12, 3 और 4 का समान गुणज है"), text("13 is prime", "13 अभाज्य है")),
                questions = listOf(
                    mcq(
                        id = "ch13_q1",
                        prompt = text("Which of these is a common multiple of 3 and 4?", "इनमें से कौन 3 और 4 का समान गुणज है?"),
                        options = listOf(text("8", "8"), text("10", "10"), text("12", "12"), text("14", "14")),
                        correctOptionIndex = 2,
                        hint = text("Look for a number that appears in both tables.", "ऐसी संख्या खोजो जो दोनों पहाड़ों में आती हो।"),
                        supportExample = text("Multiples of 3: 3, 6, 9, 12; multiples of 4: 4, 8, 12.", "3 के गुणज: 3, 6, 9, 12; 4 के गुणज: 4, 8, 12।"),
                        mistakeType = MistakeType.PATTERN_RULE,
                        reteachTitle = text("List both jump patterns", "दोनों छलाँग पैटर्न लिखो"),
                        reteachParagraphs = listOf(text("The first number where both patterns land is the first common multiple.", "जहाँ दोनों पैटर्न पहली बार साथ पहुँचें, वही पहला समान गुणज है।")),
                    ),
                    input(
                        id = "ch13_q2",
                        prompt = text("Complete the statement: 2 x ____ = 12", "वाक्य पूरा करो: 2 x ____ = 12"),
                        acceptedAnswers = listOf("6", "six", "छह"),
                        hint = text("Think of factor pairs of 12.", "12 के गुणनखंड युग्म सोचो।"),
                        supportExample = text("2 and 6 make 12 as a factor pair.", "2 और 6, 12 का एक गुणनखंड युग्म हैं।"),
                        mistakeType = MistakeType.OPERATION_LINK,
                        reteachTitle = text("Use factor pairs", "गुणनखंड युग्म का उपयोग करो"),
                        reteachParagraphs = listOf(text("A factor pair multiplies to make the given number.", "गुणनखंड युग्म का गुणन दी हुई संख्या बनाता है।")),
                    ),
                ),
            ),
            topic(
                id = "ch14_maps",
                chapterNumber = 14,
                chapterTitle = text("Maps and Locations", "मानचित्र और स्थान"),
                topicTitle = text("Using Directions and Reading Maps", "दिशाओं का उपयोग और मानचित्र पढ़ना"),
                examples = listOf(text("Facing the rising sun means facing east", "उगते सूरज की ओर मुँह करना पूरब की ओर होना है"), text("If you face east, your left points north", "यदि तुम पूरब की ओर देखते हो, तो बायाँ उत्तर की ओर होगा"), text("If you face east, your right points south", "यदि तुम पूरब की ओर देखते हो, तो दायाँ दक्षिण की ओर होगा")),
                questions = listOf(
                    mcq(
                        id = "ch14_q1",
                        prompt = text("If you face the rising sun, which direction are you facing?", "यदि तुम उगते सूरज की ओर मुँह करो, तो कौन-सी दिशा की ओर हो?"),
                        options = listOf(text("North", "उत्तर"), text("South", "दक्षिण"), text("East", "पूरब"), text("West", "पश्चिम")),
                        correctOptionIndex = 2,
                        hint = text("The sun rises in the east.", "सूरज पूरब में उगता है।"),
                        supportExample = text("Morning sunlight helps us mark east first.", "सुबह की धूप हमें पहले पूरब पहचानने में मदद करती है।"),
                        mistakeType = MistakeType.DIRECTION,
                        reteachTitle = text("Start with east", "पूरब से शुरुआत करो"),
                        reteachParagraphs = listOf(text("Once east is fixed, west is behind, north is left, and south is right.", "पूरब तय होते ही पश्चिम पीछे, उत्तर बाईं ओर और दक्षिण दाईं ओर मिलता है।")),
                    ),
                    input(
                        id = "ch14_q2",
                        prompt = text("If you face east, your left hand points to which direction?", "यदि तुम पूरब की ओर देख रहे हो, तो तुम्हारा बायाँ हाथ किस दिशा की ओर होगा?"),
                        acceptedAnswers = listOf("north", "उत्तर"),
                        hint = text("Imagine standing with the sun in front of you.", "कल्पना करो कि सूरज तुम्हारे सामने है।"),
                        supportExample = text("Facing east means left side is north.", "पूरब की ओर मुँह होने पर बाईं ओर उत्तर होता है।"),
                        mistakeType = MistakeType.DIRECTION,
                        reteachTitle = text("Turn your body on the map", "मानचित्र पर अपने शरीर को घुमाओ"),
                        reteachParagraphs = listOf(text("Use your own left and right to feel the direction change.", "दिशा बदलने को महसूस करने के लिए अपने बाएँ-दाएँ का उपयोग करो।")),
                    ),
                ),
            ),
            topic(
                id = "ch15_data",
                chapterNumber = 15,
                chapterTitle = text("Data Through Pictures", "चित्रों से आँकड़े"),
                topicTitle = text("Reading Data with Pictographs and Scales", "पिक्टोग्राफ और स्केल से आँकड़े पढ़ना"),
                examples = listOf(text("If 1 icon = 5 toys, then 4 icons = 20 toys", "यदि 1 चित्र = 5 खिलौने, तो 4 चित्र = 20 खिलौने"), text("A scale helps large data look neat", "स्केल बड़े आँकड़ों को साफ दिखाता है"), text("The first thing to check is the key", "सबसे पहले कुंजी देखनी चाहिए")),
                questions = listOf(
                    mcq(
                        id = "ch15_q1",
                        prompt = text("Why do we use a scale in a pictograph?", "पिक्टोग्राफ में स्केल का उपयोग क्यों करते हैं?"),
                        options = listOf(text("To change the topic", "विषय बदलने के लिए"), text("To use fewer symbols for larger numbers", "बड़ी संख्याओं के लिए कम चित्रों का उपयोग करने के लिए"), text("To avoid counting", "गिनती से बचने के लिए"), text("To turn data into fractions", "आँकड़ों को भिन्न में बदलने के लिए")),
                        correctOptionIndex = 1,
                        hint = text("Scale makes large data easier to show.", "स्केल बड़े आँकड़ों को दिखाना आसान बनाता है।"),
                        supportExample = text("If each picture means 5, then 4 pictures can show 20 objects.", "यदि हर चित्र 5 दिखाता है, तो 4 चित्र 20 वस्तुएँ दिखा सकते हैं।"),
                        mistakeType = MistakeType.DATA_SCALE,
                        reteachTitle = text("Read the key first", "पहले कुंजी पढ़ो"),
                        reteachParagraphs = listOf(text("Without the scale, the picture count alone can mislead you.", "स्केल के बिना केवल चित्रों की गिनती तुम्हें भ्रमित कर सकती है।")),
                    ),
                    input(
                        id = "ch15_q2",
                        prompt = text("If 1 icon stands for 5 toys, how many toys do 4 icons show?", "यदि 1 चित्र 5 खिलौनों को दिखाता है, तो 4 चित्र कितने खिलौने दिखाएँगे?"),
                        acceptedAnswers = listOf("20", "twenty", "बीस"),
                        hint = text("Multiply the number of icons by the scale.", "चित्रों की संख्या को स्केल से गुणा करो।"),
                        supportExample = text("4 x 5 = 20", "4 x 5 = 20"),
                        mistakeType = MistakeType.DATA_SCALE,
                        reteachTitle = text("Count groups through the scale", "स्केल से समूह गिनो"),
                        reteachParagraphs = listOf(text("Each icon represents a full group, not just one object.", "हर चित्र एक पूरे समूह को दिखाता है, केवल एक वस्तु को नहीं।")),
                    ),
                ),
            ),
        )
    }

    private fun topic(
        id: String,
        chapterNumber: Int,
        chapterTitle: LocalizedText,
        topicTitle: LocalizedText,
        examples: List<LocalizedText>,
        questions: List<QuizQuestion>,
    ): LessonTopic {
        return LessonTopic(
            id = id,
            chapterNumber = chapterNumber,
            chapterTitle = chapterTitle,
            topicTitle = topicTitle,
            explanationParagraphs = listOf(topicTitle),
            examples = examples,
            questions = questions,
        )
    }

    private fun mcq(
        id: String,
        prompt: LocalizedText,
        options: List<LocalizedText>,
        correctOptionIndex: Int,
        hint: LocalizedText,
        supportExample: LocalizedText,
        mistakeType: MistakeType,
        reteachTitle: LocalizedText,
        reteachParagraphs: List<LocalizedText>,
    ): QuizQuestion {
        return QuizQuestion(
            id = id,
            prompt = prompt,
            type = QuestionType.MULTIPLE_CHOICE,
            options = options,
            correctOptionIndex = correctOptionIndex,
            hint = hint,
            supportExample = supportExample,
            wrongReason = defaultWrongReason(mistakeType),
            mistakeType = mistakeType,
            reteachTitle = reteachTitle,
            reteachParagraphs = reteachParagraphs,
        )
    }

    private fun input(
        id: String,
        prompt: LocalizedText,
        acceptedAnswers: List<String>,
        hint: LocalizedText,
        supportExample: LocalizedText,
        mistakeType: MistakeType,
        reteachTitle: LocalizedText,
        reteachParagraphs: List<LocalizedText>,
    ): QuizQuestion {
        return QuizQuestion(
            id = id,
            prompt = prompt,
            type = QuestionType.TEXT_INPUT,
            acceptedAnswers = acceptedAnswers,
            hint = hint,
            supportExample = supportExample,
            wrongReason = defaultWrongReason(mistakeType),
            mistakeType = mistakeType,
            reteachTitle = reteachTitle,
            reteachParagraphs = reteachParagraphs,
        )
    }

    private fun defaultWrongReason(type: MistakeType): LocalizedText {
        return when (type) {
            MistakeType.PLACE_VALUE -> text("The answer missed the place-value pattern.", "उत्तर में स्थान-मूल्य का पैटर्न छूट गया।")
            MistakeType.UNIT_CONVERSION -> text("The units were not converted into the same form.", "इकाइयों को एक ही रूप में नहीं बदला गया।")
            MistakeType.READING -> text("The number or statement was not read in the correct parts.", "संख्या या कथन को सही हिस्सों में नहीं पढ़ा गया।")
            MistakeType.FRACTION_COMPARE -> text("The fraction idea does not match the whole or equal parts correctly.", "भिन्न का विचार पूरे भाग या बराबर हिस्सों से सही तरह नहीं मिला।")
            MistakeType.ANGLE_TURN -> text("The size of the turn was matched with the wrong angle idea.", "मोड़ की मात्रा को गलत कोण विचार से मिलाया गया।")
            MistakeType.OPERATION_LINK -> text("The operation relationship was chosen incorrectly.", "क्रियाओं का संबंध सही तरह नहीं चुना गया।")
            MistakeType.PATTERN_RULE -> text("The repeating rule or fit pattern was missed.", "दोहराव का नियम या फिट होने वाला पैटर्न छूट गया।")
            MistakeType.MEASUREMENT_ESTIMATE -> text("The measurement does not make sense or was not estimated carefully.", "माप समझदारी से नहीं आँका गया या ठीक नहीं बैठता।")
            MistakeType.TIME_READING -> text("The clock or time-conversion rule was not applied correctly.", "घड़ी या समय-रूपांतरण का नियम सही तरह लागू नहीं हुआ।")
            MistakeType.DIRECTION -> text("The direction was mixed up while imagining the body or map.", "शरीर या मानचित्र की कल्पना करते समय दिशा गड़बड़ा गई।")
            MistakeType.DATA_SCALE -> text("The pictograph scale was not used correctly.", "पिक्टोग्राफ का स्केल सही तरह उपयोग नहीं हुआ।")
            else -> text("The answer does not match the key concept yet.", "उत्तर अभी मुख्य विचार से मेल नहीं खाता।")
        }
    }
}
