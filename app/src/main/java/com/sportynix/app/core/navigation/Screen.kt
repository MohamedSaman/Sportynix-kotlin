package com.sportynix.app.core.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Welcome : Screen("welcome")
    object Login : Screen("login")
    object Register : Screen("register")
    object OTPVerification : Screen("otp_verification/{sessionId}/{phone}/{email}") {
        fun createRoute(sessionId: String, phone: String, email: String) =
            "otp_verification/$sessionId/$phone/$email"
    }
    object ForgotPasswordEmail : Screen("forgot_password_email")
    object ResetPassword : Screen("reset_password?email={email}") {
        fun createRoute(email: String) = "reset_password?email=$email"
    }
    object Home : Screen("home")
    object VenueList : Screen("venue_list")
    object VenueDetail : Screen("venue_detail/{venueId}") {
        fun createRoute(venueId: String) = "venue_detail/$venueId"
    }
    object VenueSlotPicker : Screen("venue_slot_picker/{venueId}") {
        fun createRoute(venueId: String) = "venue_slot_picker/$venueId"
    }
    object BookingSummary : Screen("booking_summary/{venueId}/{slotId}/{date}") {
        fun createRoute(venueId: String, slotId: String, date: String) = "booking_summary/$venueId/$slotId/$date"
    }
    object BookingHistory : Screen("booking_history")
    object Payment : Screen("payment/{bookingId}/{amount}") {
        fun createRoute(bookingId: String, amount: Double) = "payment/$bookingId/$amount"
    }
    object Profile : Screen("profile")
    object Notification : Screen("notification")
    object Chat : Screen("chat/{receiverId}") {
        fun createRoute(receiverId: String) = "chat/$receiverId"
    }
    object Settings : Screen("settings")
    object Search : Screen("search")
}
