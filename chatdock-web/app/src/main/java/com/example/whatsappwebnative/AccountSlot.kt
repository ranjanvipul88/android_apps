package com.example.whatsappwebnative

import androidx.annotation.IdRes

data class AccountSlot(
    val number: Int,
    val suffix: String,
    @IdRes val buttonId: Int,
    val activityClass: Class<out WebAccountActivity>
) {
    companion object {
        val all = listOf(
            AccountSlot(1, "account_1", R.id.accountOneButton, AccountOneActivity::class.java),
            AccountSlot(2, "account_2", R.id.accountTwoButton, AccountTwoActivity::class.java),
            AccountSlot(3, "account_3", R.id.accountThreeButton, AccountThreeActivity::class.java),
            AccountSlot(4, "account_4", R.id.accountFourButton, AccountFourActivity::class.java),
            AccountSlot(5, "account_5", R.id.accountFiveButton, AccountFiveActivity::class.java)
        )
    }
}
