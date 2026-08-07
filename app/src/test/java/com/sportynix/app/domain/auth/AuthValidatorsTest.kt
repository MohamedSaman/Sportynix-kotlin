package com.sportynix.app.domain.auth

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class AuthValidatorsTest {
    @Test fun `email validation accepts normal addresses and rejects malformed ones`() {
        assertNull(AuthValidators.email("player@sportynix.com"))
        assertNotNull(AuthValidators.email("player@"))
    }

    @Test fun `date of birth requires age thirteen and rejects future dates`() {
        val today = LocalDate.of(2026, 8, 7)
        assertNull(AuthValidators.dateOfBirth("2013-08-07", today))
        assertNotNull(AuthValidators.dateOfBirth("2013-08-08", today))
        assertEquals("Date of birth cannot be in the future", AuthValidators.dateOfBirth("2027-01-01", today))
    }

    @Test fun `password requires length upper lower and number`() {
        assertNull(AuthValidators.password("Premium1"))
        assertNotNull(AuthValidators.password("premium1"))
        assertNotNull(AuthValidators.password("Short1"))
    }

    @Test fun `username and referral normalization rules match mobile flow`() {
        assertNull(AuthValidators.username("player_01"))
        assertNotNull(AuthValidators.username("Player 01"))
        assertNull(AuthValidators.referral("play20"))
    }

    @Test fun `otp keeps only first six digits`() {
        assertEquals("123456", AuthValidators.otp("12a345678"))
    }
}
