package com.book.teachloop

object LessonRepository {
    const val BOOK_ID = "class5_math_mela_grade_5"
    const val BOOK_TITLE = "NCERT Math-Mela Grade 5"
    private val SUBTOPIC_FOCUS = listOf(
        text("Key idea", "मुख्य विचार"),
        text("Worked example", "उदाहरण के साथ"),
        text("Try it yourself", "खुद करके देखो"),
    )
    private val SUBTOPIC_PROMPTS = listOf(
        text(
            "Do you already know the key idea in this part?",
            "क्या आपको इस भाग का मुख्य विचार पहले से पता है?"
        ),
        text(
            "Can you follow the example in this part?",
            "क्या आप इस भाग के उदाहरण को समझ सकते हैं?"
        ),
        text(
            "Can you solve the quick check in this part?",
            "क्या आप इस भाग की झटपट जाँच हल कर सकते हैं?"
        ),
    )
    private val SUBTOPIC_TITLES = listOf(
        text("Core concept", "मूल अवधारणा"),
        text("Guided practice", "मार्गदर्शित अभ्यास"),
        text("Independent check", "स्वतंत्र जाँच"),
    )

    fun library(): List<StudyBook> {
        return listOf(grade5MathMelaBook())
    }

    fun grade5MathMelaBook(): StudyBook {
        return StudyBook(
            id = BOOK_ID,
            subjectTitle = text("Mathematics", "गणित"),
            bookTitle = text(BOOK_TITLE, "एनसीईआरटी मैथ मेला कक्षा 5"),
            topics = grade5MathMela().flatMap(::expandTopicIntoSubtopics),
        )
    }

    fun grade5MathMela(): List<LessonTopic> {
        return listOf(
            LessonTopic(
                id = "ch1_large_numbers",
                chapterNumber = 1,
                chapterTitle = "We the Travellers - I",
                topicTitle = "Reading and Writing Large Numbers",
                knowPrompt = "Do you know how to read and write large numbers using place value?",
                explanationTitle = "Start with place value and build up",
                explanationParagraphs = listOf(
                    "This chapter begins with travel and then uses that idea to talk about large counts such as vehicles, distances, and populations. The key idea is place value. Ten ones make one ten, ten tens make one hundred, ten hundreds make one thousand, and ten thousands make ten thousand.",
                    "When you read a number like 10,458, you read each place from left to right: one ten thousand, zero thousands, four hundreds, five tens, and eight ones. Commas help us read large numbers more easily.",
                    "The same ten digits 0 to 9 can build very large numbers because the value of a digit changes with its position. So 45,867 is not just a string of digits. It means forty-five thousand, eight hundred, sixty-seven."
                ),
                examples = listOf(
                    "10,024 is read as ten thousand twenty-four.",
                    "13,520 means 1 ten thousand, 3 thousands, 5 hundreds, 2 tens, and 0 ones.",
                    "45,867 is read as forty-five thousand eight hundred sixty-seven."
                ),
                questions = listOf(
                    mcq(
                        id = "ch1_q1",
                        prompt = "Which option correctly reads 10,024?",
                        options = listOf(
                            "One thousand twenty-four",
                            "Ten thousand twenty-four",
                            "Ten thousand two hundred four",
                            "One lakh twenty-four"
                        ),
                        correctOptionIndex = 1,
                        hint = "Look at the ten-thousands place carefully."
                    ),
                    input(
                        id = "ch1_q2",
                        prompt = "Fill the answer: 13,520 is read as",
                        acceptedAnswers = listOf(
                            "thirteen thousand five hundred twenty",
                            "thirteen thousand five hundred and twenty"
                        ),
                        hint = "Read the number from the thousands place first."
                    )
                )
            ),
            LessonTopic(
                id = "ch2_fractions",
                chapterNumber = 2,
                chapterTitle = "Fractions",
                topicTitle = "Comparing and Building Equivalent Fractions",
                knowPrompt = "Do you know how fractions work when the wholes are the same?",
                explanationTitle = "Fractions only compare fairly when the wholes match",
                explanationParagraphs = listOf(
                    "The chapter shows that you cannot compare one-third of one chocolate with one-half of another chocolate unless both chocolates are the same size. Fractions must come from the same whole when you compare them directly.",
                    "The chapter also builds equivalent fractions. If one-half is split into two equal parts, each part becomes one-fourth. So one-half is equal to two-fourths.",
                    "Equivalent fractions name the same part of a whole in different ways. For example, one-third is equal to two-sixths because the same shaded area can be divided into more equal pieces."
                ),
                examples = listOf(
                    "1/2 = 2/4",
                    "1/3 = 2/6",
                    "Two 1/4 pieces make 1/2"
                ),
                questions = listOf(
                    mcq(
                        id = "ch2_q1",
                        prompt = "Which fraction is equivalent to 1/2?",
                        options = listOf("2/4", "1/3", "2/5", "3/8"),
                        correctOptionIndex = 0,
                        hint = "Think of two quarter pieces making a half."
                    ),
                    input(
                        id = "ch2_q2",
                        prompt = "How many 1/4 pieces make 1/2?",
                        acceptedAnswers = listOf("2", "two"),
                        hint = "Two equal quarters join to form one half."
                    )
                )
            ),
            LessonTopic(
                id = "ch3_angles",
                chapterNumber = 3,
                chapterTitle = "Angles as Turns",
                topicTitle = "Understanding Angles Through Turns",
                knowPrompt = "Do you know how quarter turns and half turns connect to angles?",
                explanationTitle = "Angles can be understood as turns",
                explanationParagraphs = listOf(
                    "This chapter uses turning actions like a child spinning, a giant wheel, or the minute hand of a clock. A full turn means coming back to the starting position. A half turn is half of that, and a quarter turn is one-fourth of a full turn.",
                    "When you turn by a quarter turn, you make a right angle. Less than a quarter turn makes an acute angle. More than a quarter turn but less than a half turn makes an obtuse angle. A half turn makes a straight angle.",
                    "Thinking in turns helps children notice angles in real life instead of memorising names only."
                ),
                examples = listOf(
                    "1 quarter turn = right angle",
                    "1 half turn = straight angle",
                    "4 quarter turns = 1 full turn"
                ),
                questions = listOf(
                    mcq(
                        id = "ch3_q1",
                        prompt = "A quarter turn makes which angle?",
                        options = listOf("Acute angle", "Right angle", "Straight angle", "Full angle"),
                        correctOptionIndex = 1,
                        hint = "A quarter turn is one-fourth of a full circle."
                    ),
                    input(
                        id = "ch3_q2",
                        prompt = "How many quarter turns make one full turn?",
                        acceptedAnswers = listOf("4", "four"),
                        hint = "Imagine dividing a circle into equal fourths."
                    )
                )
            ),
            LessonTopic(
                id = "ch4_add_subtract",
                chapterNumber = 4,
                chapterTitle = "We the Travellers - II",
                topicTitle = "Addition, Subtraction, and Their Relationship",
                knowPrompt = "Do you know how addition and subtraction support each other in word problems?",
                explanationTitle = "Addition and subtraction are inverse operations",
                explanationParagraphs = listOf(
                    "The chapter uses fuel and travel examples to show addition and subtraction in action. If a lorry has 28 litres and 75 more litres are added, you find the total by addition.",
                    "If you know the total after refuelling and the amount already in the tank, you find the added amount by subtraction. That is why addition and subtraction are connected.",
                    "A good checking method is to reverse the operation. If 18 litres plus 47 litres gives 65 litres, then 65 minus 18 must give 47 and 65 minus 47 must give 18."
                ),
                examples = listOf(
                    "28 + 75 = 103",
                    "If 78 + 164 = 242, then 242 - 78 = 164",
                    "If 65 - 18 = 47, then 18 + 47 = 65"
                ),
                questions = listOf(
                    input(
                        id = "ch4_q1",
                        prompt = "A lorry has 28 litres of fuel and 75 litres are added. What is the total quantity?",
                        acceptedAnswers = listOf("103", "103 litres", "103 liter", "103 l"),
                        hint = "Add the tens and ones carefully."
                    ),
                    mcq(
                        id = "ch4_q2",
                        prompt = "If 78 + 164 = 242, then 242 - 78 = ?",
                        options = listOf("86", "154", "164", "174"),
                        correctOptionIndex = 2,
                        hint = "Subtraction can undo addition."
                    )
                )
            ),
            LessonTopic(
                id = "ch5_length",
                chapterNumber = 5,
                chapterTitle = "Far and Near",
                topicTitle = "Measuring Length and Distance",
                knowPrompt = "Do you know when to use centimetres, metres, and kilometres?",
                explanationTitle = "Choose the correct unit for the distance",
                explanationParagraphs = listOf(
                    "This chapter moves from short lengths to long distances. Small objects like a handkerchief or mobile phone are measured in centimetres. Bigger objects like the height of a gate are often measured in metres.",
                    "For long distances such as travel routes, race paths, or the distance between stations, kilometre is a better unit. One thousand metres make one kilometre.",
                    "The chapter also compares measures written in different units. To compare them correctly, convert them into the same unit first."
                ),
                examples = listOf(
                    "100 cm = 1 m",
                    "1000 m = 1 km",
                    "456 cm is less than 5 m because 5 m = 500 cm"
                ),
                questions = listOf(
                    mcq(
                        id = "ch5_q1",
                        prompt = "How many metres are there in 1 kilometre?",
                        options = listOf("100", "500", "1000", "10000"),
                        correctOptionIndex = 2,
                        hint = "Kilo means one thousand."
                    ),
                    input(
                        id = "ch5_q2",
                        prompt = "Convert 2 km into metres.",
                        acceptedAnswers = listOf("2000", "2000 m", "2000 metres", "2000 meter"),
                        hint = "Each kilometre is 1000 metres."
                    )
                )
            ),
            LessonTopic(
                id = "ch6_multiplication",
                chapterNumber = 6,
                chapterTitle = "The Dairy Farm",
                topicTitle = "Multiplication Patterns and Place Value",
                knowPrompt = "Do you know how multiplication patterns work with tens and hundreds?",
                explanationTitle = "Multiplication grows quickly with place value",
                explanationParagraphs = listOf(
                    "This chapter continues multiplication using arrays, group size, and patterns. It reminds us that changing the order of factors does not change the product. So 6 times 13 and 13 times 6 give the same answer.",
                    "When numbers are multiplied by 10, 100, or 1000, each digit shifts to a higher place value. That is why the product becomes much larger.",
                    "Looking for patterns helps children solve multiplication more confidently instead of depending only on repeated addition."
                ),
                examples = listOf(
                    "6 x 13 = 78 and 13 x 6 = 78",
                    "8 x 20 = 160",
                    "30 x 20 = 600"
                ),
                questions = listOf(
                    mcq(
                        id = "ch6_q1",
                        prompt = "Which value is equal to 8 x 20?",
                        options = listOf("28", "80", "160", "800"),
                        correctOptionIndex = 2,
                        hint = "8 x 2 tens = 16 tens."
                    ),
                    input(
                        id = "ch6_q2",
                        prompt = "Fill the answer: 30 x 20 =",
                        acceptedAnswers = listOf("600", "six hundred"),
                        hint = "3 tens times 2 tens gives 6 hundreds."
                    )
                )
            ),
            LessonTopic(
                id = "ch7_patterns",
                chapterNumber = 7,
                chapterTitle = "Shapes and Patterns",
                topicTitle = "Weaving Patterns and Tessellation",
                knowPrompt = "Do you know which shapes can cover a surface without gaps?",
                explanationTitle = "Patterns repeat, and some shapes tessellate",
                explanationParagraphs = listOf(
                    "The weaving part of the chapter shows repeating over-under patterns. Recognising a rule in one row helps us continue the design correctly.",
                    "The tessellation part asks which regular shapes can cover a region without gaps or overlaps. Equilateral triangles, squares, and regular hexagons can do this. Regular pentagons and regular octagons cannot tessellate on their own.",
                    "This is useful because it connects visual design, shape properties, and mathematical reasoning."
                ),
                examples = listOf(
                    "A simple weave can repeat as 1 under, 1 over.",
                    "4 squares fit neatly around a point.",
                    "Regular pentagons leave gaps, so they do not tessellate."
                ),
                questions = listOf(
                    mcq(
                        id = "ch7_q1",
                        prompt = "Which regular shape can tessellate without gaps?",
                        options = listOf("Regular pentagon", "Regular octagon", "Equilateral triangle", "Circle"),
                        correctOptionIndex = 2,
                        hint = "Think of shapes that can meet around a point exactly."
                    ),
                    input(
                        id = "ch7_q2",
                        prompt = "How many squares fit around one point without gaps?",
                        acceptedAnswers = listOf("4", "four"),
                        hint = "Each corner angle of a square is a right angle."
                    )
                )
            ),
            LessonTopic(
                id = "ch8_weight",
                chapterNumber = 8,
                chapterTitle = "Weight and Capacity",
                topicTitle = "Converting Between Kilograms and Grams",
                knowPrompt = "Do you know how to compare weights written in kilograms and grams?",
                explanationTitle = "Use one common unit before comparing",
                explanationParagraphs = listOf(
                    "This chapter asks children to decide whether a recorded weight makes sense, like spotting that a refrigerator cannot weigh only 50 grams. So estimation matters as much as exact calculation.",
                    "For conversion, one kilogram is equal to one thousand grams. If you want to compare two weights written in different units, convert both into the same unit first.",
                    "For example, 3 kilograms 500 grams becomes 3500 grams. Once both weights are in grams or both are in kilograms, comparison becomes easier."
                ),
                examples = listOf(
                    "1 kg = 1000 g",
                    "3 kg 500 g = 3500 g",
                    "5 kg 50 g is heavier than 4 kg 500 g"
                ),
                questions = listOf(
                    input(
                        id = "ch8_q1",
                        prompt = "Convert 3 kg 500 g into grams.",
                        acceptedAnswers = listOf("3500", "3500 g", "3500 grams"),
                        hint = "3 kg means 3000 g. Add 500 g more."
                    ),
                    mcq(
                        id = "ch8_q2",
                        prompt = "Which weight is greater?",
                        options = listOf("4 kg 500 g", "5 kg 50 g", "Both are equal", "Cannot compare"),
                        correctOptionIndex = 1,
                        hint = "Convert both to grams if needed."
                    )
                )
            ),
            LessonTopic(
                id = "ch9_division",
                chapterNumber = 9,
                chapterTitle = "Coconut Farm",
                topicTitle = "Connecting Multiplication and Division",
                knowPrompt = "Do you know how one multiplication fact gives two division facts?",
                explanationTitle = "Division can be read from multiplication",
                explanationParagraphs = listOf(
                    "The chapter begins with arrays of coconuts. If 5 groups of 7 make 35, then 35 divided by 7 gives 5 and 35 divided by 5 gives 7.",
                    "This relationship can be written as dividend equals divisor times quotient. Once children see this connection, multiplication and division stop feeling like separate ideas.",
                    "The chapter also uses place value patterns in division, like 1000 divided by 10 and 4000 divided by 40."
                ),
                examples = listOf(
                    "5 x 7 = 35",
                    "35 / 7 = 5",
                    "35 / 5 = 7"
                ),
                questions = listOf(
                    mcq(
                        id = "ch9_q1",
                        prompt = "If 5 x 7 = 35, which division fact is correct?",
                        options = listOf("35 / 7 = 5", "35 / 7 = 7", "7 / 35 = 5", "5 / 35 = 7"),
                        correctOptionIndex = 0,
                        hint = "The product becomes the dividend."
                    ),
                    input(
                        id = "ch9_q2",
                        prompt = "Sabina cycles 160 km in 20 equal days. How many kilometres does she cycle each day?",
                        acceptedAnswers = listOf("8", "8 km", "8 kilometres", "8 kilometers"),
                        hint = "Divide the total distance by the number of days."
                    )
                )
            ),
            LessonTopic(
                id = "ch10_symmetry",
                chapterNumber = 10,
                chapterTitle = "Symmetrical Designs",
                topicTitle = "Reflection and Rotational Symmetry",
                knowPrompt = "Do you know how to identify reflection symmetry and rotational symmetry?",
                explanationTitle = "A shape can match itself in more than one way",
                explanationParagraphs = listOf(
                    "This chapter starts with paper cutouts of letters. A vertical or horizontal fold can show a line of symmetry. If the two halves match exactly, the figure has reflection symmetry.",
                    "The chapter then moves to rotational symmetry using letters and a firki. A figure has rotational symmetry if it looks the same after a turn smaller than a full turn.",
                    "For example, the letter H has both reflection symmetry and rotational symmetry after a half turn. A firki can match itself after quarter turns."
                ),
                examples = listOf(
                    "The letter A has a vertical line of symmetry.",
                    "The letter H has both vertical and horizontal reflection symmetry.",
                    "A firki can match itself after 1/4, 1/2, and 3/4 turns."
                ),
                questions = listOf(
                    mcq(
                        id = "ch10_q1",
                        prompt = "Which letter has both vertical and horizontal lines of symmetry?",
                        options = listOf("A", "H", "K", "N"),
                        correctOptionIndex = 1,
                        hint = "Imagine folding it from both directions."
                    ),
                    input(
                        id = "ch10_q2",
                        prompt = "How many quarter turns make one full turn for a firki?",
                        acceptedAnswers = listOf("4", "four"),
                        hint = "A full circle is made of four equal quarters."
                    )
                )
            ),
            LessonTopic(
                id = "ch11_perimeter_area",
                chapterNumber = 11,
                chapterTitle = "Grandmother's Quilt",
                topicTitle = "Perimeter and Area Through Tiling",
                knowPrompt = "Do you know the difference between perimeter and area?",
                explanationTitle = "Border length and covered region are different ideas",
                explanationParagraphs = listOf(
                    "The chapter uses a quilt, lace, and table coverings. The perimeter of a shape is the total length of its border. That is useful when you want to know how much lace is needed.",
                    "Area is different. Area tells how much surface is covered. The chapter explains area through tiling with shapes that cover a region without gaps or overlaps.",
                    "Squares, rectangles, and triangles can tile a surface. Circles leave gaps, so they are not used for exact tiling in the same way."
                ),
                examples = listOf(
                    "A square of side 5 cm has perimeter 20 cm.",
                    "Perimeter measures the boundary.",
                    "Area measures the inside region covered by tiles."
                ),
                questions = listOf(
                    input(
                        id = "ch11_q1",
                        prompt = "What is the perimeter of a square with side 5 cm?",
                        acceptedAnswers = listOf("20", "20 cm", "20 centimetres", "20 centimeters"),
                        hint = "A square has four equal sides."
                    ),
                    mcq(
                        id = "ch11_q2",
                        prompt = "Which shape leaves gaps when used alone to cover a table top?",
                        options = listOf("Square", "Rectangle", "Triangle", "Circle"),
                        correctOptionIndex = 3,
                        hint = "Think about tiling without gaps or overlaps."
                    )
                )
            ),
            LessonTopic(
                id = "ch12_time",
                chapterNumber = 12,
                chapterTitle = "Racing Seconds",
                topicTitle = "Reading Time, Elapsed Time, and Seconds",
                knowPrompt = "Do you know how to work with minutes, seconds, and 24-hour time?",
                explanationTitle = "Short activities need precise time measurement",
                explanationParagraphs = listOf(
                    "This chapter compares how long activities take and introduces seconds for more precise measurement. In a race, everyone may finish within one minute, but seconds show who was faster.",
                    "It also connects 12-hour and 24-hour time. Morning times stay the same, while p.m. times after noon are found by adding 12 to the hour.",
                    "Remember that 1 minute equals 60 seconds. This helps when moving between the two units."
                ),
                examples = listOf(
                    "1 min = 60 seconds",
                    "02:30 p.m. = 14:30 hours",
                    "05:30 a.m. = 05:30 hours"
                ),
                questions = listOf(
                    input(
                        id = "ch12_q1",
                        prompt = "Fill the answer: 1 minute = ____ seconds",
                        acceptedAnswers = listOf("60", "sixty"),
                        hint = "Watch the second hand complete one round."
                    ),
                    mcq(
                        id = "ch12_q2",
                        prompt = "What is 02:30 p.m. in 24-hour format?",
                        options = listOf("02:30", "12:30", "14:30", "22:30"),
                        correctOptionIndex = 2,
                        hint = "For p.m. after noon, add 12 to the hour."
                    )
                )
            ),
            LessonTopic(
                id = "ch13_factors_multiples",
                chapterNumber = 13,
                chapterTitle = "Animal Jumps",
                topicTitle = "Factors, Multiples, and Common Multiples",
                knowPrompt = "Do you know how factors and multiples are related?",
                explanationTitle = "Arrays help reveal factors and multiples",
                explanationParagraphs = listOf(
                    "This chapter explains that factors are numbers that divide another number exactly, and multiples are numbers made by multiplying a number by whole numbers.",
                    "For 12, the arrays 1 x 12, 2 x 6, and 3 x 4 show that 1, 2, 3, 4, 6, and 12 are factors of 12. Because 12 can be made from these factors, 12 is also a multiple of them.",
                    "The chapter also looks at common multiples with number-line jumps, such as the common multiples of 3 and 4. The first common multiple of 3 and 4 is 12."
                ),
                examples = listOf(
                    "Factors of 12 include 1, 2, 3, 4, 6, and 12.",
                    "12 is a common multiple of 3 and 4.",
                    "13 is prime because it has only 1 and 13 as factors."
                ),
                questions = listOf(
                    mcq(
                        id = "ch13_q1",
                        prompt = "Which of these is a common multiple of 3 and 4?",
                        options = listOf("8", "10", "12", "14"),
                        correctOptionIndex = 2,
                        hint = "Look for a number in both tables."
                    ),
                    input(
                        id = "ch13_q2",
                        prompt = "Complete the statement: 2 x ____ = 12",
                        acceptedAnswers = listOf("6", "six"),
                        hint = "Think of pairs of factors of 12."
                    )
                )
            ),
            LessonTopic(
                id = "ch14_maps",
                chapterNumber = 14,
                chapterTitle = "Maps and Locations",
                topicTitle = "Using Directions and Reading Maps",
                knowPrompt = "Do you know how to use east, west, north, and south on a map?",
                explanationTitle = "Directions help us describe location and movement",
                explanationParagraphs = listOf(
                    "The chapter begins by using the rising Sun to identify east. Once east is known, west is behind, north is to the left when facing east, and south is to the right.",
                    "Children then practise reading maps of tents, rooms, roads, and bus routes. The important skill is describing position in relation to another object or place.",
                    "Maps turn travel and movement into mathematical language: north, south, east, west, left turn, right turn, and nearest route."
                ),
                examples = listOf(
                    "Facing the rising Sun means facing east.",
                    "If you face east, your left points north.",
                    "If you face east, your right points south."
                ),
                questions = listOf(
                    mcq(
                        id = "ch14_q1",
                        prompt = "If you face the rising Sun, which direction are you facing?",
                        options = listOf("North", "South", "East", "West"),
                        correctOptionIndex = 2,
                        hint = "The Sun rises in the east."
                    ),
                    input(
                        id = "ch14_q2",
                        prompt = "If you face east, your left hand points to which direction?",
                        acceptedAnswers = listOf("north"),
                        hint = "Stand facing east and imagine your left shoulder."
                    )
                )
            ),
            LessonTopic(
                id = "ch15_data",
                chapterNumber = 15,
                chapterTitle = "Data Through Pictures",
                topicTitle = "Reading Data with Pictographs and Scales",
                knowPrompt = "Do you know how a pictograph uses symbols and scales to show data?",
                explanationTitle = "Pictures can summarise data clearly",
                explanationParagraphs = listOf(
                    "This chapter collects information and then shows it through pictures. A pictograph uses icons to represent quantities, making the data easier to read quickly.",
                    "When there are many objects, using one picture for every single item can become messy. A scale solves that problem. For example, one icon may stand for 5 toys.",
                    "To answer questions from a pictograph, always read the scale first and then count the number of symbols carefully."
                ),
                examples = listOf(
                    "If 1 icon = 5 toys, then 4 icons = 20 toys.",
                    "A scale lets us show large data using fewer pictures.",
                    "The first thing to read in a pictograph is the key or scale."
                ),
                questions = listOf(
                    mcq(
                        id = "ch15_q1",
                        prompt = "Why do we use a scale in a pictograph?",
                        options = listOf(
                            "To change the topic",
                            "To use fewer symbols for larger numbers",
                            "To avoid counting",
                            "To turn data into fractions"
                        ),
                        correctOptionIndex = 1,
                        hint = "A scale makes large data easier to show."
                    ),
                    input(
                        id = "ch15_q2",
                        prompt = "If 1 icon stands for 5 toys, how many toys do 4 icons show?",
                        acceptedAnswers = listOf("20", "twenty"),
                        hint = "Multiply the number of icons by the scale."
                    )
                )
            ),
        )
    }

    private fun expandTopicIntoSubtopics(source: LessonTopic): List<StudyTopic> {
        val paragraphs = source.explanationParagraphs.takeIf { it.isNotEmpty() }
            ?: listOf(source.explanationTitle)
        val exampleSet = source.examples.takeIf { it.isNotEmpty() }
            ?: listOf(source.topicTitle)

        return SUBTOPIC_TITLES.mapIndexed { index, subtopicTitle ->
            val paragraph = paragraphs.getOrElse(index) { paragraphs.last() }
            val exampleWindow = listOfNotNull(
                exampleSet.getOrNull(index),
                exampleSet.getOrNull((index + 1).coerceAtMost(exampleSet.lastIndex)),
            ).distinct()

            val visualChips = extractVisualChips(exampleWindow + paragraph)
            val visuals = listOf(
                VisualBlock(
                    title = text("Book visual", "पुस्तक दृश्य"),
                    description = text(
                        english = paragraph,
                        hindi = "इस भाग का मुख्य संकेत: $paragraph"
                    ),
                    chips = visualChips,
                ),
                VisualBlock(
                    title = text("Quick cues", "त्वरित संकेत"),
                    description = text(
                        english = "Use these small cues before answering the question.",
                        hindi = "उत्तर देने से पहले इन छोटे संकेतों को देखिए।",
                    ),
                    chips = exampleWindow.map { text(it, it) },
                ),
            )

            StudyTopic(
                id = "${source.id}_step_${index + 1}",
                sourceLessonId = source.id,
                chapterNumber = source.chapterNumber,
                chapterTitle = text(source.chapterTitle, source.chapterTitle),
                lessonTitle = text(source.topicTitle, source.topicTitle),
                subtopicTitle = subtopicTitle,
                knowPrompt = SUBTOPIC_PROMPTS[index],
                explanationTitle = SUBTOPIC_FOCUS[index],
                explanationParagraphs = listOf(
                    text(paragraph, paragraph),
                    text(
                        english = buildStepSummary(source, index),
                        hindi = buildHindiStepSummary(index)
                    )
                ),
                examples = exampleWindow.map { text(it, it) },
                visuals = visuals,
                questions = source.questions,
                questionSeedIndex = index % source.questions.size.coerceAtLeast(1),
            )
        }
    }

    private fun buildStepSummary(
        source: LessonTopic,
        index: Int,
    ): String {
        return when (index) {
            0 -> "Start with the main idea in ${source.topicTitle} before trying examples."
            1 -> "Now connect the idea to an example from the book and notice the pattern."
            else -> "Use the pattern on your own and get ready for the check question."
        }
    }

    private fun buildHindiStepSummary(index: Int): String {
        return when (index) {
            0 -> "पहले मुख्य विचार को समझिए, फिर उदाहरण की ओर जाइए।"
            1 -> "अब पुस्तक के उदाहरण से विचार और पैटर्न को जोड़िए।"
            else -> "अब पैटर्न का उपयोग खुद कीजिए और प्रश्न के लिए तैयार हो जाइए।"
        }
    }

    private fun extractVisualChips(values: List<String>): List<LocalizedText> {
        val tokens = values
            .flatMap { value ->
                Regex("\\d+[\\d,/xX]*|[A-Za-z]+")
                    .findAll(value)
                    .map { match -> match.value }
                    .toList()
            }
            .distinct()
            .take(5)

        return tokens.map { token -> text(token, token) }
    }

    private fun mcq(
        id: String,
        prompt: String,
        options: List<String>,
        correctOptionIndex: Int,
        hint: String,
    ): QuizQuestion {
        return QuizQuestion(
            id = id,
            prompt = prompt,
            type = QuestionType.MULTIPLE_CHOICE,
            options = options,
            correctOptionIndex = correctOptionIndex,
            hint = hint,
        )
    }

    private fun input(
        id: String,
        prompt: String,
        acceptedAnswers: List<String>,
        hint: String,
    ): QuizQuestion {
        return QuizQuestion(
            id = id,
            prompt = prompt,
            type = QuestionType.TEXT_INPUT,
            acceptedAnswers = acceptedAnswers,
            hint = hint,
        )
    }
}
