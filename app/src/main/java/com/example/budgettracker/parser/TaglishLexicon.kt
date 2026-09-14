package com.example.budgettracker.parser

object TaglishLexicon {

    // Pillar 1: Morphological Affix Stripping
    // Matches nag-Grab -> Grab, nagpa-gas -> gas, mag-Starbucks -> Starbucks, etc.
    val PREFIX_REGEX = Regex("^(?:nag-|nagpa-|mag-|pina-|nang-|pag-|ipag-)", RegexOption.IGNORE_CASE)

    fun stripAffix(word: String): String {
        return word.replace(PREFIX_REGEX, "").trim()
    }

    // Pillar 2: Tagalog word amounts
    val TAGALOG_NUMBER_MAP = mapOf(
        "isang libo" to 1000L,
        "isang daan" to 100L,
        "dalawang libo" to 2000L,
        "dalawang daan" to 200L,
        "tatlong libo" to 3000L,
        "tatlong daan" to 300L,
        "apat na libo" to 4000L,
        "apat na daan" to 400L,
        "limang libo" to 5000L,
        "limang daan" to 500L,
        "sampung libo" to 10000L
    )

    // Pillar 3: Semantic Case Markers
    // Followed by payment method or source account
    val PAYMENT_METHOD_MARKERS = listOf(
        "gamit ang", "gamit", "via", "thru", "through",
        "galing sa", "galing kay", "mula sa", "using", "with my"
    )

    // Followed by destination account in a transfer
    val DESTINATION_MARKERS = listOf(
        "papunta sa", "papunta kay", "patungo sa", "to", "send to", "transfer to"
    )

    // Followed by merchant or recipient person
    val RECIPIENT_MERCHANT_MARKERS = listOf(
        "para kay", "kay", "kina", "para sa", "pambayad sa", "sa"
    )

    // Pillar 4: Intent Verbs & Keywords
    val TRANSFER_KEYWORDS = setOf(
        "transfer", "lipat", "nilipat", "send", "sinend", "pasa", "pinasa",
        "padala", "pinadala", "transferred", "forward"
    )

    val INCOME_KEYWORDS = setOf(
        "salary", "sahod", "sweldo", "sweldohan", "kita", "tinubo", "sideline",
        "raket", "ayuda", "bonus", "income", "interest", "kinita", "deposit", "pumasok",
        "received", "receive", "natanggap", "tanggap", "naka-receive", "freelance",
        "allowance", "remittance", "inflow", "credited", "sinahod"
    )

    val INSTALLMENT_KEYWORDS = setOf(
        "installment", "hulugan", "buwan-buwan", "hulog", "spaylater", "lazpaylater",
        "billease", "homecredit", "home credit", "ggives", "sloan", "gloan", "atome"
    )

    val EXPENSE_KEYWORDS = setOf(
        "bayad", "nagbayad", "binayaran", "bili", "bumili", "binili", "gastos",
        "gumastos", "kinain", "nakain", "ambag", "libre", "kaltas", "load", "paid", "bought"
    )

    // Connective grammar and filler words to clean out when synthesizing titles
    val CONNECTIVE_FILLER_WORDS = setOf(
        "ako", "akong", "ko", "ikaw", "mo", "ka", "kami", "namin", "tayo", "natin",
        "siya", "niya", "sila", "nila", "ang", "mga", "ng", "nang", "sa",
        "kay", "kina", "yung", "ung", "iyong", "ito", "nito", "eto", "neto",
        "na", "pa", "naman", "din", "rin", "daw", "raw", "po", "opo",
        "gamit", "via", "thru", "through", "using", "from",
        "para", "pambayad", "pambili", "halagang", "pesos", "peso", "php", "petot", "bucks",
        "that", "this", "the", "a", "an", "there", "here", "it", "is", "was", "has", "had", "have",
        "meron", "mayroon", "may"
    )
}
