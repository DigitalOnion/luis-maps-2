package com.outerspace.luismaps2.domain

import com.outerspace.luismaps2.R

enum class LoginScreens(val loginLabel: Int, val passwordLabel: Int, val buttonLabel: Int) {
    CHOOSE_LOGIN (R.string.login_email, R.string.password_email, R.string.login_button_face),
    EMAIL_LOGIN (R.string.login_email, R.string.password_email, R.string.login_button_face),
    EMAIL_SIGN_ON (R.string.sign_on_email, R.string.sign_on_password, R.string.signon_button_face),
    PASSWORD_UPDATE (R.string.login_email, R.string.new_password, R.string.update_button_face),
}