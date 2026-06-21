package com.example.security

import java.util.Locale

object SmsSecurityAnalyzer {

    // Sensitive keyword exclusion list
    private val SENSITIVE_KEYWORDS = listOf(
        "otp", "one-time", "one time", "verification code", "security code",
        "temp password", "temporary password", "passcode", "pin code", "cvv",
        "credit card", "debit card", "pin", "auth code", "tan", "2fa",
        "bank", "transaction", "debited", "credited", "withdrawal", "deposit",
        "purchase of", "charge of", "authorized", "insufficient balance", "ledger",
        "payment successful", "transfer of", "wiring", "wire transfer"
    )

    // Regex patterns for detecting OTPs (e.g., 4-8 digit numeric or alphanumeric codes near authentication signals)
    private val OTP_PATTERNS = listOf(
        // Look for 4-8 digit codes in proximity to words like "code", "otp", "pin", "verification"
        Regex("(?i)\\b(code|otp|pin|verification|auth|pass|verify|verification)\\b.*?\\b\\d{4,8}\\b"),
        Regex("(?i)\\b\\d{4,8}\\b.*?\\b(code|otp|pin|verification|auth|pass|verify|verification)\\b"),
        // Match common patterns like "your code is: 123456" or "123-456 is your verification code"
        Regex("(?i)\\b(is|msg|verification):?\\s*\\b\\d{4,8}\\b"),
        // Specific format: "123456 as your" or "code 123456"
        Regex("(?i)\\b(code)\\s*\\b[0-9a-zA-Z]{4,8}\\b"),
        // Generic 5-6 digit verification numbers standing alone without a clear context, often used by login processes
        Regex("\\b\\d{5,6}\\b")
    )

    data class SecurityAnalysis(
        val isSafe: Boolean,
        val matchedPattern: String
    )

    /**
     * Checks if the SMS text contains sensitive banking/OTP identifiers that should block forwarding.
     * @return a SecurityAnalysis containing safety status and rationale.
     */
    fun analyze(messageBody: String): SecurityAnalysis {
        if (messageBody.isBlank()) {
            return SecurityAnalysis(isSafe = true, matchedPattern = "Empty Message")
        }

        val lowercaseBody = messageBody.lowercase(Locale.ROOT)

        // 1. Scan for explicit sensitive keywords
        for (keyword in SENSITIVE_KEYWORDS) {
            if (lowercaseBody.contains(keyword)) {
                return SecurityAnalysis(
                    isSafe = false,
                    matchedPattern = "Sensitive Keyword Matched: '$keyword'"
                )
            }
        }

        // 2. Scan for regular-expression OTP and Auth code patterns
        for (pattern in OTP_PATTERNS) {
            if (pattern.containsMatchIn(messageBody)) {
                val matchResult = pattern.find(messageBody)?.value ?: "Unknown Pattern Match"
                return SecurityAnalysis(
                    isSafe = false,
                    matchedPattern = "OTP/Auth pattern matched: '$matchResult'"
                )
            }
        }

        // 3. Scan for other common secure structures, viz, symbols + digit matches resembling financial tokens
        if (lowercaseBody.contains("$") || lowercaseBody.contains("rm") || lowercaseBody.contains("usd") || lowercaseBody.contains("eur")) {
            // If message contains monetary currency symbol and any significant number amount
            val amountPattern = Regex("\\d+([.,]\\d{2})?")
            if (amountPattern.containsMatchIn(lowercaseBody)) {
                return SecurityAnalysis(
                    isSafe = false,
                    matchedPattern = "Financial notification style (currency symbols + numbers)"
                )
            }
        }

        return SecurityAnalysis(isSafe = true, matchedPattern = "No Sensitive Indicators Found")
    }

    /**
     * Obfuscates sensitive keywords in the SMS body to bypass strict carrier firewalls (like O2/Lyca).
     */
    fun obfuscateForCarrierBypass(text: String): String {
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
        return obfuscated
    }
}
