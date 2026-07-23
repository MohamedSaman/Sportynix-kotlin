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
    object SportDetail : Screen("sport_detail/{sportId}/{venueId}") {
        fun createRoute(sportId: String, venueId: String) = "sport_detail/$sportId/$venueId"
    }
    object VenueSlotPicker : Screen("venue_slot_picker/{venueId}") {
        fun createRoute(venueId: String) = "venue_slot_picker/$venueId"
    }
    object BookingSummary : Screen("booking_summary/{venueId}/{slotId}/{date}") {
        fun createRoute(venueId: String, slotId: String, date: String) = "booking_summary/$venueId/$slotId/$date"
    }
    object BookingHistory : Screen("booking_history")
    object BookingDetail : Screen("booking_detail/{bookingId}") {
        fun createRoute(bookingId: String) = "booking_detail/$bookingId"
    }
    object Payment : Screen("payment/{bookingId}/{amount}") {
        fun createRoute(bookingId: String, amount: Double) = "payment/$bookingId/$amount"
    }
    object Profile : Screen("profile")
    object EditProfile : Screen("edit_profile")
    object Notification : Screen("notification")
    object Chat : Screen("chat/{receiverId}") {
        fun createRoute(receiverId: String) = "chat/$receiverId"
    }
    object MessagesList : Screen("messages_list")
    object Settings : Screen("settings")
    object Search : Screen("search")
    object Favorites : Screen("favorites")
    object Team : Screen("team")
    object Points : Screen("points")
    object Referrals : Screen("referrals")
    object AboutUs : Screen("about_us")
    object LeagueList : Screen("league_list")
    object LeagueDetail : Screen("league_detail/{leagueId}") {
        fun createRoute(leagueId: String) = "league_detail/$leagueId"
    }
    object TournamentList : Screen("tournament_list")
    object TournamentDetail : Screen("tournament_detail/{tournamentId}") {
        fun createRoute(tournamentId: String) = "tournament_detail/$tournamentId"
    }
    object LiveCricketScoring : Screen("live_cricket_scoring/{matchId}") {
        fun createRoute(matchId: String) = "live_cricket_scoring/$matchId"
    }
    object Auction : Screen("auction/{auctionId}") {
        fun createRoute(auctionId: String) = "auction/$auctionId"
    }
}

