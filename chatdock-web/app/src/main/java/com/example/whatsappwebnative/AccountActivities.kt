package com.example.whatsappwebnative

class AccountOneActivity : WebAccountActivity() {
    override val accountSlot: AccountSlot = AccountSlot.all[0]
}

class AccountTwoActivity : WebAccountActivity() {
    override val accountSlot: AccountSlot = AccountSlot.all[1]
}

class AccountThreeActivity : WebAccountActivity() {
    override val accountSlot: AccountSlot = AccountSlot.all[2]
}

class AccountFourActivity : WebAccountActivity() {
    override val accountSlot: AccountSlot = AccountSlot.all[3]
}

class AccountFiveActivity : WebAccountActivity() {
    override val accountSlot: AccountSlot = AccountSlot.all[4]
}
