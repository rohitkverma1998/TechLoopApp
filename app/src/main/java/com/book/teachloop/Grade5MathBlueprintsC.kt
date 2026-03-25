package com.book.teachloop

internal fun chapter11To15Blueprints(sourceId: String): List<SubtopicBlueprint>? {
    return when (sourceId) {
        "ch11_perimeter_area" -> listOf(
            subtopic(
                idSuffix = "walk_the_border",
                subtopicTitle = text("Walk around the border", "सीमा के चारों ओर चलो"),
                knowPrompt = text("Do you know how to measure only the outside boundary?", "क्या आप केवल बाहरी सीमा की माप निकालना जानते हैं?"),
                explanationTitle = text("Perimeter measures the border", "परिमाप सीमा की माप है"),
                explanationParagraphs = listOf(
                    text("Perimeter is found by adding every outside side of the shape.", "परिमाप निकालने के लिए आकृति की सभी बाहरी भुजाएँ जोड़ते हैं।"),
                    text("This is the measure we need when lace or fencing goes around the edge.", "जब किनारे के चारों ओर लेस या बाड़ लगानी हो, तब इसी माप की जरूरत होती है।"),
                ),
                exampleIndices = listOf(0, 1),
                questionIndices = listOf(0),
                visuals = listOf(
                    stepVisual(
                        title = text("Border walk", "सीमा यात्रा"),
                        description = text("Trace each side once and add all the lengths.", "हर भुजा को एक बार ट्रेस करो और सभी लंबाइयाँ जोड़ो।"),
                        chips = listOf(text("5"), text("+"), text("5"), text("+"), text("5"), text("+"), text("5")),
                    ),
                ),
                mistakeFocus = MistakeType.CONCEPT_CONFUSION,
                tags = listOf(text("perimeter", "परिमाप")),
            ),
            subtopic(
                idSuffix = "cover_the_inside",
                subtopicTitle = text("Cover the inside", "भीतर का भाग ढको"),
                knowPrompt = text("Can you tell how area is different from perimeter?", "क्या आप बता सकते हैं कि क्षेत्रफल, परिमाप से कैसे अलग है?"),
                explanationTitle = text("Area is about covering space", "क्षेत्रफल जगह ढकने के बारे में है"),
                explanationParagraphs = listOf(
                    text("Area measures the inside region that gets covered by tiles or paper pieces.", "क्षेत्रफल अंदर के उस भाग को मापता है जो टाइलों या कागज़ के टुकड़ों से ढकता है।"),
                    text("A shape can have the same perimeter as another shape but a different area.", "दो आकृतियों का परिमाप समान हो सकता है, पर क्षेत्रफल अलग हो सकता है।"),
                ),
                exampleIndices = listOf(2),
                questionIndices = listOf(1),
                visuals = listOf(
                    gridVisual(
                        title = text("Inside coverage", "अंदर का आवरण"),
                        description = text("Count or imagine the tiles that cover the inside space.", "उन टाइलों को गिनो या कल्पना करो जो अंदर की जगह को भरती हैं।"),
                        rows = listOf(
                            listOf(text("tile"), text("tile"), text("tile")),
                            listOf(text("tile"), text("tile"), text("tile")),
                        ),
                    ),
                ),
                mistakeFocus = MistakeType.CONCEPT_CONFUSION,
                tags = listOf(text("area", "क्षेत्रफल")),
            ),
        )

        "ch12_time" -> listOf(
            subtopic(
                idSuffix = "minute_and_second",
                subtopicTitle = text("Connect minutes and seconds", "मिनट और सेकंड जोड़ो"),
                knowPrompt = text("Do you know how seconds build one minute?", "क्या आप जानते हैं कि सेकंड मिलकर एक मिनट कैसे बनाते हैं?"),
                explanationTitle = text("Short activities need seconds", "छोटी गतिविधियों के लिए सेकंड चाहिए"),
                explanationParagraphs = listOf(
                    text("In a race, seconds help us compare children who all finish within one minute.", "दौड़ में सेकंड उन बच्चों की तुलना करने में मदद करते हैं जो एक मिनट के भीतर ही खत्म करते हैं।"),
                    text("The second hand must cross 60 marks to complete one minute.", "सेकंड वाली सुई को एक मिनट पूरा करने के लिए 60 निशानों से गुजरना होता है।"),
                ),
                exampleIndices = listOf(0),
                questionIndices = listOf(0),
                visuals = listOf(
                    clockVisual(
                        title = text("Second-hand round", "सेकंड सुई का चक्कर"),
                        description = text("One full round of the second hand equals one minute.", "सेकंड सुई का एक पूरा चक्कर एक मिनट के बराबर है।"),
                        rows = listOf(
                            listOf(text("0 sec"), text("15"), text("30"), text("45"), text("60")),
                        ),
                    ),
                ),
                mistakeFocus = MistakeType.TIME_READING,
                tags = listOf(text("seconds", "सेकंड")),
            ),
            subtopic(
                idSuffix = "convert_clock_format",
                subtopicTitle = text("Switch clock formats", "घड़ी के रूप बदलो"),
                knowPrompt = text("Can you change afternoon time into 24-hour form?", "क्या आप दोपहर बाद का समय 24-घंटे के रूप में बदल सकते हैं?"),
                explanationTitle = text("24-hour time removes confusion", "24-घंटे का समय भ्रम कम करता है"),
                explanationParagraphs = listOf(
                    text("Morning times usually stay the same in 24-hour form, but afternoon times add 12 to the hour.", "24-घंटे के रूप में सुबह का समय प्रायः वही रहता है, पर दोपहर बाद घंटे में 12 जोड़ा जाता है।"),
                    text("This helps timetables and race schedules stay clear and exact.", "इससे समय-सारिणी और दौड़ का कार्यक्रम साफ और सटीक रहता है।"),
                ),
                exampleIndices = listOf(1, 2),
                questionIndices = listOf(1),
                visuals = listOf(
                    tableVisual(
                        title = text("Clock conversion", "घड़ी रूपांतरण"),
                        description = text("Read morning and afternoon times in two formats.", "सुबह और दोपहर बाद के समय को दो रूपों में पढ़ो।"),
                        rows = listOf(
                            listOf(text("02:30 p.m."), text("14:30")),
                            listOf(text("05:30 a.m."), text("05:30")),
                        ),
                    ),
                ),
                mistakeFocus = MistakeType.TIME_READING,
                tags = listOf(text("24-hour time", "24-घंटे का समय")),
            ),
        )

        "ch13_factors_multiples" -> listOf(
            subtopic(
                idSuffix = "make_factor_pairs",
                subtopicTitle = text("Make factor pairs", "गुणनखंड युग्म बनाओ"),
                knowPrompt = text("Can you use arrays to find factor pairs of a number?", "क्या आप किसी संख्या के गुणनखंड युग्म खोजने में सरणी का उपयोग कर सकते हैं?"),
                explanationTitle = text("Factor pairs build the number", "गुणनखंड युग्म संख्या बनाते हैं"),
                explanationParagraphs = listOf(
                    text("A factor pair is two numbers whose product gives the target number.", "गुणनखंड युग्म वे दो संख्याएँ हैं जिनका गुणन लक्ष्य संख्या देता है।"),
                    text("Arrays show the same idea as rows and columns that fit exactly.", "सरणी भी यही विचार दिखाती है कि कितनी पंक्तियाँ और स्तंभ ठीक-ठीक बैठते हैं।"),
                ),
                exampleIndices = listOf(0, 2),
                questionIndices = listOf(1),
                visuals = listOf(
                    tableVisual(
                        title = text("Factor-pair table", "गुणनखंड-युग्म तालिका"),
                        description = text("Each row gives one way to build 12.", "हर पंक्ति 12 बनाने का एक तरीका दिखाती है।"),
                        rows = listOf(
                            listOf(text("1 x 12"), text("12")),
                            listOf(text("2 x 6"), text("12")),
                            listOf(text("3 x 4"), text("12")),
                        ),
                    ),
                ),
                mistakeFocus = MistakeType.OPERATION_LINK,
                tags = listOf(text("factor pairs", "गुणनखंड युग्म")),
            ),
            subtopic(
                idSuffix = "jump_to_common_multiples",
                subtopicTitle = text("Jump to common multiples", "समान गुणज तक छलाँग"),
                knowPrompt = text("Do you know how jump patterns on a number line reveal common multiples?", "क्या आप जानते हैं कि संख्या रेखा पर छलाँग पैटर्न समान गुणज कैसे दिखाते हैं?"),
                explanationTitle = text("Two jump patterns can land together", "दो छलाँग पैटर्न एक साथ उतर सकते हैं"),
                explanationParagraphs = listOf(
                    text("Multiples of 3 and multiples of 4 both land on 12, so 12 is a common multiple.", "3 के गुणज और 4 के गुणज दोनों 12 पर उतरते हैं, इसलिए 12 समान गुणज है।"),
                    text("The first shared landing point is the first common multiple.", "पहला साझा उतरने वाला बिंदु पहला समान गुणज होता है।"),
                ),
                exampleIndices = listOf(1),
                questionIndices = listOf(0),
                visuals = listOf(
                    numberLineVisual(
                        title = text("Jump lines", "छलाँग रेखाएँ"),
                        description = text("Look for the first place where both jumps meet.", "वह पहला स्थान खोजो जहाँ दोनों छलाँगें मिलती हैं।"),
                        chips = listOf(text("3"), text("6"), text("9"), text("12"), text("4"), text("8"), text("12")),
                    ),
                ),
                mistakeFocus = MistakeType.PATTERN_RULE,
                tags = listOf(text("common multiples", "समान गुणज")),
            ),
        )

        "ch14_maps" -> listOf(
            subtopic(
                idSuffix = "find_directions_from_sun",
                subtopicTitle = text("Start with the sun", "सूरज से शुरुआत करो"),
                knowPrompt = text("Can you derive north, south, east, and west from the rising sun?", "क्या आप उगते सूरज से उत्तर, दक्षिण, पूरब और पश्चिम निकाल सकते हैं?"),
                explanationTitle = text("East gives the rest of the directions", "पूरब से बाकी दिशाएँ मिलती हैं"),
                explanationParagraphs = listOf(
                    text("Facing the rising sun means facing east. Then west is behind, north is left, and south is right.", "उगते सूरज की ओर मुँह करने का मतलब पूरब की ओर होना है। तब पश्चिम पीछे, उत्तर बाईं ओर और दक्षिण दाईं ओर होगा।"),
                    text("Using the body helps children feel the directions instead of memorising them blindly.", "अपने शरीर का उपयोग करने से बच्चे दिशाओं को केवल रटते नहीं, बल्कि महसूस भी करते हैं।"),
                ),
                exampleIndices = listOf(0, 1, 2),
                questionIndices = listOf(0, 1),
                visuals = listOf(
                    compassVisual(
                        title = text("Body compass", "शरीर कम्पास"),
                        description = text("Stand facing east and name the other directions.", "पूरब की ओर खड़े होकर बाकी दिशाओं के नाम बताओ।"),
                        rows = listOf(
                            listOf(text("front"), text("east")),
                            listOf(text("left"), text("north")),
                            listOf(text("right"), text("south")),
                        ),
                    ),
                ),
                mistakeFocus = MistakeType.DIRECTION,
                tags = listOf(text("compass", "कम्पास")),
            ),
            subtopic(
                idSuffix = "read_map_route",
                subtopicTitle = text("Describe a route", "रास्ता बताओ"),
                knowPrompt = text("Can you describe a location by using turns and directions?", "क्या आप मोड़ और दिशाओं की मदद से किसी स्थान का रास्ता बता सकते हैं?"),
                explanationTitle = text("A map turns movement into direction language", "मानचित्र चलने को दिशा की भाषा में बदल देता है"),
                explanationParagraphs = listOf(
                    text("Maps help us say where something is in relation to another place.", "मानचित्र हमें यह बताने में मदद करते हैं कि कोई स्थान दूसरे स्थान के सापेक्ष कहाँ है।"),
                    text("Words like north, south, left turn, and right turn make the route clear.", "उत्तर, दक्षिण, बायाँ मोड़ और दायाँ मोड़ जैसे शब्द रास्ते को स्पष्ट बनाते हैं।"),
                ),
                exampleIndices = listOf(1, 2),
                questionIndices = listOf(1),
                visuals = listOf(
                    stepVisual(
                        title = text("Route steps", "रास्ते के कदम"),
                        description = text("Say each move in order from start to finish.", "शुरुआत से अंत तक हर चाल को क्रम से बोलो।"),
                        chips = listOf(text("east"), text("left to north"), text("right to east")),
                    ),
                ),
                mistakeFocus = MistakeType.DIRECTION,
                tags = listOf(text("route", "रास्ता")),
            ),
        )

        "ch15_data" -> listOf(
            subtopic(
                idSuffix = "read_the_key",
                subtopicTitle = text("Read the key first", "पहले कुंजी पढ़ो"),
                knowPrompt = text("Do you check the key before reading a pictograph?", "क्या आप पिक्टोग्राफ पढ़ने से पहले कुंजी देखते हैं?"),
                explanationTitle = text("The key tells what one picture means", "कुंजी बताती है कि एक चित्र क्या दिखाता है"),
                explanationParagraphs = listOf(
                    text("A pictograph is easy only when the child first reads the key or scale.", "पिक्टोग्राफ तभी आसान होता है जब बच्चा पहले उसकी कुंजी या स्केल पढ़े।"),
                    text("Without the key, the number of pictures alone can be misleading.", "कुंजी के बिना केवल चित्रों की संख्या भ्रम पैदा कर सकती है।"),
                ),
                exampleIndices = listOf(1, 2),
                questionIndices = listOf(0),
                visuals = listOf(
                    pictographVisual(
                        title = text("Key card", "कुंजी कार्ड"),
                        description = text("Always read what one icon stands for.", "हमेशा पढ़ो कि एक चित्र किसके बराबर है।"),
                        rows = listOf(
                            listOf(text("1 icon"), text("5 toys")),
                            listOf(text("4 icons"), text("20 toys")),
                        ),
                    ),
                ),
                mistakeFocus = MistakeType.DATA_SCALE,
                tags = listOf(text("key", "कुंजी")),
            ),
            subtopic(
                idSuffix = "scale_to_total",
                subtopicTitle = text("Use the scale to count", "गिनती में स्केल का उपयोग"),
                knowPrompt = text("Can you multiply the icons by the scale to find the total?", "क्या आप कुल संख्या निकालने के लिए चित्रों को स्केल से गुणा कर सकते हैं?"),
                explanationTitle = text("Each picture stands for a group", "हर चित्र एक समूह दिखाता है"),
                explanationParagraphs = listOf(
                    text("If one icon stands for 5 toys, then four icons stand for 4 groups of 5 toys.", "यदि एक चित्र 5 खिलौने दिखाता है, तो चार चित्र 5-5 खिलौनों के 4 समूह दिखाते हैं।"),
                    text("So the child must multiply or add the repeated groups instead of just counting icons.", "इसलिए बच्चे को केवल चित्र नहीं गिनने, बल्कि दोहराए गए समूहों को जोड़ना या गुणा करना चाहिए।"),
                ),
                exampleIndices = listOf(0),
                questionIndices = listOf(1),
                visuals = listOf(
                    stepVisual(
                        title = text("Scale multiplication", "स्केल गुणा"),
                        description = text("Icon count times scale gives the total.", "चित्रों की संख्या x स्केल = कुल संख्या।"),
                        chips = listOf(text("4 icons"), text("x 5 each"), text("20 toys")),
                    ),
                ),
                mistakeFocus = MistakeType.DATA_SCALE,
                tags = listOf(text("scale", "स्केल")),
            ),
        )

        else -> null
    }
}
