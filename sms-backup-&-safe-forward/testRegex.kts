import java.util.regex.Pattern

fun main() {
    val text = "Dear Investor, 271021 is the OTP for processing your request. Bank transaction password alert verification code auth account debited credited."
    var obfuscated = text
    val replacements = mapOf(
        "(?i)\\botp\\b" to "0-T-P",
        "(?i)\\bbank\\b" to "B.a.n.k",
        "(?i)\\btransaction\\b" to "Txn",
        "(?i)\\bpassword\\b" to "P.w.d",
        "(?i)\\balert\\b" to "A.l.r.t",
        "(?i)\\bverification\\b" to "V.e.r.i.f",
        "(?i)\\bcode\\b" to "C.o.d.e",
        "(?i)\\bauth\\b" to "A.u.t.h",
        "(?i)\\baccount\\b" to "A.c.c.t",
        "(?i)\\bdebited\\b" to "Dr.",
        "(?i)\\bcredited\\b" to "Cr."
    )
    for ((regex, replacement) in replacements) {
        obfuscated = obfuscated.replace(Regex(regex), replacement)
    }
    println(obfuscated)
}
