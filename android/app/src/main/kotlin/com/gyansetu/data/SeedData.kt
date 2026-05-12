package com.gyansetu.data

/** Initial syllabus content seeded into Room on first launch.
 *  Mirrors the bilingual KB shipped with the web demo. */
object SeedData {
    fun allRows(): List<SyllabusEntity> = buildList {
        // ANIMALS
        add(row("animals", "cow", "ગાય", "/kaʊ/", "🐄",
            "The cow gives us milk. It eats grass.",
            "ગાય દૂધ આપે છે. તે ઘાસ ખાય છે."))
        add(row("animals", "dog", "કૂતરો", "/dɒɡ/", "🐕",
            "The dog is a loyal animal. It says woof woof.",
            "કૂતરો વફાદાર પ્રાણી છે. તે ભૌ ભૌ બોલે છે."))
        add(row("animals", "cat", "બિલાડી", "/kæt/", "🐈",
            "The cat drinks milk. It says meow.",
            "બિલાડી દૂધ પીવે છે. તે મ્યાઉં બોલે છે."))
        add(row("animals", "elephant", "હાથી", "/ˈel.ɪ.fənt/", "🐘",
            "The elephant is a big animal. It has a long trunk.",
            "હાથી મોટું પ્રાણી છે. તેને લાંબી સૂંઢ હોય છે."))
        add(row("animals", "lion", "સિંહ", "/ˈlaɪ.ən/", "🦁",
            "The lion is the king of the jungle.",
            "સિંહ જંગલનો રાજા છે."))
        add(row("animals", "tiger", "વાઘ", "/ˈtaɪ.ɡər/", "🐅",
            "The tiger is the national animal of India.",
            "વાઘ ભારતનું રાષ્ટ્રીય પ્રાણી છે."))
        add(row("animals", "monkey", "વાંદરો", "/ˈmʌŋ.ki/", "🐒",
            "The monkey jumps on trees. It eats bananas.",
            "વાંદરો ઝાડ પર કૂદે છે. તે કેળાં ખાય છે."))
        add(row("animals", "rabbit", "સસલું", "/ˈræb.ɪt/", "🐇",
            "The rabbit eats carrots. It runs fast.",
            "સસલું ગાજર ખાય છે. તે ઝડપથી દોડે છે."))

        // FRUITS
        add(row("fruits", "apple", "સફરજન", "/ˈæp.əl/", "🍎",
            "Apple is a red fruit. It tastes sweet.",
            "સફરજન લાલ ફળ છે. તે મીઠું હોય છે."))
        add(row("fruits", "banana", "કેળું", "/bəˈnæn.ə/", "🍌",
            "Banana is a yellow fruit. Monkeys love it.",
            "કેળું પીળું ફળ છે. તે વાંદરાને બહુ ભાવે છે."))
        add(row("fruits", "mango", "કેરી", "/ˈmæŋ.ɡoʊ/", "🥭",
            "Mango is the queen of fruits. It comes in summer.",
            "કેરી ફળોની રાણી છે. ઉનાળામાં મળે છે."))
        add(row("fruits", "orange", "નારંગી", "/ˈɒr.ɪndʒ/", "🍊",
            "Orange is sweet and sour. It has vitamin C.",
            "નારંગી ખાટું મીઠું ફળ છે. તેમાં વિટામિન C હોય છે."))
        add(row("fruits", "grape", "દ્રાક્ષ", "/ɡreɪp/", "🍇",
            "Grapes are small round fruits.",
            "દ્રાક્ષ નાનું ગોળ ફળ છે."))
        add(row("fruits", "watermelon", "તરબૂચ", "/ˈwɔː.tərˌmel.ən/", "🍉",
            "Watermelon is a summer fruit. It has lots of water.",
            "તરબૂચ ઉનાળાનું ફળ છે. તેમાં બહુ પાણી હોય છે."))

        // VEGETABLES
        add(row("vegetables", "tomato", "ટામેટું", "/təˈmeɪ.toʊ/", "🍅",
            "Tomato is a red vegetable.",
            "ટામેટું લાલ શાક છે."))
        add(row("vegetables", "potato", "બટાકા", "/pəˈteɪ.toʊ/", "🥔",
            "Potatoes grow in the ground.",
            "બટાકા જમીનમાં ઉગે છે."))
        add(row("vegetables", "carrot", "ગાજર", "/ˈkær.ət/", "🥕",
            "Carrots are orange.",
            "ગાજર નારંગી રંગનું હોય છે."))
        add(row("vegetables", "onion", "ડુંગળી", "/ˈʌn.jən/", "🧅",
            "Onions make us cry.",
            "ડુંગળી કાપતા આંસુ આવે છે."))
        add(row("vegetables", "cabbage", "કોબી", "/ˈkæb.ɪdʒ/", "🥬",
            "Cabbage is a green leafy vegetable.",
            "કોબી લીલા પાંદડાંવાળું શાક છે."))
        add(row("vegetables", "spinach", "પાલક", "/ˈspɪn.ɪtʃ/", "🥬",
            "Spinach is full of iron and keeps us strong.",
            "પાલકમાં લોહતત્ત્વ હોય છે, તે આપણને મજબૂત રાખે છે."))

        // CLASSROOM
        add(row("classroom", "book", "પુસ્તક", "/bʊk/", "📖",
            "We learn by reading books.",
            "પુસ્તક વાંચીને આપણે શીખીએ છીએ."))
        add(row("classroom", "pencil", "પેન્સિલ", "/ˈpen.səl/", "✏️",
            "We write and draw with a pencil.",
            "પેન્સિલથી લખીએ અને ચીતરીએ."))
        add(row("classroom", "pen", "પેન", "/pen/", "🖊️",
            "A pen has ink inside.",
            "પેનની શાહી હોય છે."))
        add(row("classroom", "bag", "દફતર", "/bæɡ/", "🎒",
            "We keep books in a bag.",
            "દફતરમાં પુસ્તકો રાખીએ."))
        add(row("classroom", "blackboard", "કાળું પાટિયું", "/ˈblæk.bɔːrd/", "🪧",
            "The teacher writes on the blackboard.",
            "શિક્ષક પાટિયા પર લખે છે."))
        add(row("classroom", "eraser", "રબર", "/ɪˈreɪ.zər/", "🩹",
            "We erase pencil mistakes with a rubber.",
            "રબરથી પેન્સિલની ભૂલ ભૂંસીએ."))
        add(row("classroom", "ruler", "ફુટપટ્ટી", "/ˈruː.lər/", "📏",
            "We draw straight lines with a ruler.",
            "ફુટપટ્ટીથી સીધી લીટી દોરીએ."))
        add(row("classroom", "chair", "ખુરશી", "/tʃer/", "🪑",
            "We sit on a chair.",
            "આપણે ખુરશી પર બેસીએ."))

        // COLORS
        add(row("colors", "red", "લાલ", null, "🔴",
            "Apples and roses are red.",
            "સફરજન અને ગુલાબ લાલ હોય છે."))
        add(row("colors", "blue", "વાદળી", null, "🔵",
            "The sky and sea are blue.",
            "આકાશ અને દરિયો વાદળી છે."))
        add(row("colors", "green", "લીલો", null, "🟢",
            "Grass and leaves are green.",
            "ઘાસ અને પાંદડા લીલા છે."))
        add(row("colors", "yellow", "પીળો", null, "🟡",
            "The sun and bananas are yellow.",
            "સૂર્ય અને કેળાં પીળાં છે."))
        add(row("colors", "orange", "કેસરી", null, "🟠",
            "Saffron is the colour of energy.",
            "કેસરી રંગ ઊર્જાનો રંગ છે."))
        add(row("colors", "white", "સફેદ", null, "⚪",
            "Milk and snow are white.",
            "દૂધ અને બરફ સફેદ છે."))
        add(row("colors", "black", "કાળો", null, "⚫",
            "The night is black.",
            "રાત કાળી હોય છે."))
        add(row("colors", "pink", "ગુલાબી", null, "🌸",
            "Roses are pink.",
            "ગુલાબ ગુલાબી હોય છે."))

        // BODY
        add(row("body", "eye", "આંખ", null, "👁️",
            "We see with our eyes.",
            "આંખથી આપણે જોઈએ."))
        add(row("body", "ear", "કાન", null, "👂",
            "We hear with our ears.",
            "કાનથી આપણે સાંભળીએ."))
        add(row("body", "nose", "નાક", null, "👃",
            "We smell with our nose.",
            "નાકથી આપણે સૂંઘીએ."))
        add(row("body", "mouth", "મોં", null, "👄",
            "We speak and eat with our mouth.",
            "મોંથી આપણે બોલીએ અને ખાઈએ."))
        add(row("body", "hand", "હાથ", null, "✋",
            "We work with our hands.",
            "હાથથી આપણે કામ કરીએ."))
        add(row("body", "foot", "પગ", null, "🦶",
            "We walk with our feet.",
            "પગથી આપણે ચાલીએ."))

        // FAMILY
        add(row("family", "father", "પપ્પા", null, "👨",
            "Father is our protector.",
            "પપ્પા આપણા રક્ષક છે."))
        add(row("family", "mother", "મમ્મી", null, "👩",
            "Mother tells stories with love.",
            "મમ્મી પ્રેમથી વાર્તા કહે છે."))
        add(row("family", "brother", "ભાઈ", null, "👦",
            "It is fun to play with brother.",
            "ભાઈ સાથે રમવાની મજા આવે."))
        add(row("family", "sister", "બહેન", null, "👧",
            "We read with sister.",
            "બહેન સાથે વાંચીએ."))
        add(row("family", "grandfather", "દાદા", null, "👴",
            "Grandfather tells old stories.",
            "દાદા જૂની વાર્તાઓ કહે."))
        add(row("family", "grandmother", "દાદી", null, "👵",
            "Grandmother cooks tasty food.",
            "દાદી સ્વાદિષ્ટ ખાવાનું બનાવે."))

        // WEATHER
        add(row("weather", "sun", "સૂર્ય", null, "☀️",
            "The sun gives light during day.",
            "સૂર્ય દિવસે પ્રકાશ આપે છે."))
        add(row("weather", "rain", "વરસાદ", null, "🌧️",
            "Rain helps crops grow.",
            "વરસાદ પાક ઉગાડે છે."))
        add(row("weather", "cloud", "વાદળ", null, "☁️",
            "Clouds carry water.",
            "વાદળમાં પાણી હોય છે."))
        add(row("weather", "wind", "પવન", null, "🌬️",
            "Wind moves the leaves.",
            "પવન પાંદડાં હલાવે છે."))
        add(row("weather", "rainbow", "મેઘધનુષ", null, "🌈",
            "A rainbow has seven colours.",
            "મેઘધનુષમાં સાત રંગ હોય."))
        add(row("weather", "snow", "બરફ", null, "❄️",
            "Snow falls in cold places.",
            "ઠંડી જગ્યાએ બરફ પડે."))

        // TRANSPORT
        add(row("transport", "car", "ગાડી", null, "🚗",
            "A car runs on the road.",
            "ગાડી રસ્તા પર ચાલે છે."))
        add(row("transport", "bus", "બસ", null, "🚌",
            "Many people travel by bus.",
            "બસમાં ઘણા લોકો બેસે."))
        add(row("transport", "train", "રેલગાડી", null, "🚆",
            "The train runs on tracks.",
            "રેલગાડી પાટા પર દોડે."))
        add(row("transport", "cycle", "સાયકલ", null, "🚲",
            "Riding a cycle is fun.",
            "સાયકલ ચલાવવી મજાની છે."))
        add(row("transport", "aeroplane", "વિમાન", null, "✈️",
            "An aeroplane flies in the sky.",
            "વિમાન આકાશમાં ઊડે છે."))
        add(row("transport", "boat", "હોડી", null, "⛵",
            "A boat floats on water.",
            "હોડી પાણી પર તરે છે."))

        // NUMBERS 1-20
        listOf(
            "one" to "એક", "two" to "બે", "three" to "ત્રણ", "four" to "ચાર",
            "five" to "પાંચ", "six" to "છ", "seven" to "સાત", "eight" to "આઠ",
            "nine" to "નવ", "ten" to "દસ",
            "eleven" to "અગિયાર", "twelve" to "બાર", "thirteen" to "તેર",
            "fourteen" to "ચૌદ", "fifteen" to "પંદર", "sixteen" to "સોળ",
            "seventeen" to "સત્તર", "eighteen" to "અઢાર", "nineteen" to "ઓગણીસ",
            "twenty" to "વીસ",
        ).forEachIndexed { i, (en, gu) ->
            val n = i + 1
            val icon = if (n <= 10) "${n}️⃣" else "🔢"
            add(row("numbers", en, gu, null, icon,
                "The number $n in English is $en.",
                "$n નું ગુજરાતી $gu છે."))
        }

        // DAYS
        listOf(
            "Sunday" to "રવિવાર", "Monday" to "સોમવાર",
            "Tuesday" to "મંગળવાર", "Wednesday" to "બુધવાર",
            "Thursday" to "ગુરુવાર", "Friday" to "શુક્રવાર",
            "Saturday" to "શનિવાર",
        ).forEach { (en, gu) ->
            add(row("days", en, gu, null, "📅",
                "$en is a day of the week.",
                "$gu અઠવાડિયાનો એક દિવસ છે."))
        }

        // MONTHS
        listOf(
            "January" to "જાન્યુઆરી", "February" to "ફેબ્રુઆરી",
            "March" to "માર્ચ", "April" to "એપ્રિલ",
            "May" to "મે", "June" to "જૂન",
            "July" to "જુલાઈ", "August" to "ઓગસ્ટ",
            "September" to "સપ્ટેમ્બર", "October" to "ઓક્ટોબર",
            "November" to "નવેમ્બર", "December" to "ડિસેમ્બર",
        ).forEach { (en, gu) ->
            add(row("months", en, gu, null, "🗓️",
                "$en is a month of the year.",
                "$gu વર્ષનો એક મહિનો છે."))
        }

        // FACTS (GK)
        add(row("facts", "sky blue",  "આકાશ વાદળી", null, "🌤️",
            "The sky looks blue because sunlight scatters in the air. Blue scatters the most!",
            "આકાશ વાદળી દેખાય છે કારણ કે સૂર્યનો પ્રકાશ હવામાં છૂટો પડે છે. વાદળી રંગ સૌથી વધુ વેરાય છે!"))
        add(row("facts", "sun", "સૂર્ય", null, "☀️",
            "The sun is a big star. It gives us light and warmth.",
            "સૂર્ય એક મોટો તારો છે. તે આપણને પ્રકાશ અને ગરમી આપે છે."))
        add(row("facts", "rainbow", "મેઘધનુષ", null, "🌈",
            "A rainbow has seven colours. It appears after rain.",
            "મેઘધનુષ સાત રંગો ધરાવે છે. વરસાદ પછી તે દેખાય છે."))
        add(row("facts", "water", "પાણી", null, "💧",
            "Water is essential for life. It exists as liquid, ice and vapour.",
            "પાણી જીવન માટે જરૂરી છે. તે ત્રણ રૂપમાં હોય છે — પ્રવાહી, બરફ અને વરાળ."))
        add(row("facts", "moon", "ચંદ્ર", null, "🌙",
            "The moon is Earth's satellite. It shines at night.",
            "ચંદ્ર પૃથ્વીનો ઉપગ્રહ છે. તે રાત્રે ચમકે છે."))
        add(row("facts", "star", "તારો", null, "⭐",
            "Stars are like our sun, but very far away.",
            "તારા સૂર્ય જેવા જ હોય છે, પણ બહુ દૂર છે."))
        add(row("facts", "india", "ભારત", null, "🇮🇳",
            "India is our country. Its capital is New Delhi.",
            "ભારત આપણો દેશ છે. તેની રાજધાની નવી દિલ્હી છે."))
        add(row("facts", "gujarat", "ગુજરાત", null, "🪷",
            "Gujarat is our state. Its capital is Gandhinagar.",
            "ગુજરાત આપણું રાજ્ય છે. તેની રાજધાની ગાંધીનગર છે."))
    }

    private fun row(
        topic: String, en: String, gu: String, phon: String?, icon: String,
        storyEn: String, storyGu: String,
    ) = SyllabusEntity(
        topic = topic, en = en, gu = gu, phon = phon, icon = icon,
        storyEn = storyEn, storyGu = storyGu,
    )
}
