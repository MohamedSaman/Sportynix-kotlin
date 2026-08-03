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
    object ForgotPasswordOtp : Screen("forgot_password_otp?email={email}") {
        fun createRoute(email: String) = "forgot_password_otp?email=${android.net.Uri.encode(email)}"
    }
    object ResetPassword : Screen("reset_password?email={email}&otp={otp}") {
        fun createRoute(email: String, otp: String) = "reset_password?email=${android.net.Uri.encode(email)}&otp=$otp"
    }
    object Home : Screen("home")
    object VenueList : Screen("venue_list")
    object VenueDetail : Screen("venue_detail/{venueId}") {
        fun createRoute(venueId: String) = "venue_detail/$venueId"
    }
    object SportDetail : Screen("sport_detail/{sportId}/{venueId}") {
        fun createRoute(sportId: String, venueId: String) = "sport_detail/$sportId/$venueId"
    }
    object VenueMap : Screen("venue_map?venueId={venueId}&lat={lat}&lng={lng}&name={name}&location={location}&rating={rating}&image={image}") {
        fun createRoute(venueId: String, lat: Double, lng: Double, name: String, location: String, rating: Int, image: String) =
            "venue_map?venueId=$venueId&lat=$lat&lng=$lng&name=${java.net.URLEncoder.encode(name, "UTF-8")}&location=${java.net.URLEncoder.encode(location, "UTF-8")}&rating=$rating&image=${java.net.URLEncoder.encode(image, "UTF-8")}"
    }
    object VenueSlotPicker : Screen("venue_slot_picker/{venueId}") {
        fun createRoute(venueId: String) = "booking/$venueId/1"
    }
    object Booking : Screen("booking/{venueId}/{sportId}") {
        fun createRoute(venueId: String, sportId: String) = "booking/$venueId/$sportId"
    }
    object BookingSummary : Screen("booking_summary/{venueId}/{sportId}/{date}/{slotIds}/{bookingType}?selectedDays={selectedDays}") {
        fun createRoute(venueId: String, sportId: String, date: String, slotIds: String, bookingType: String = "Normal", selectedDays: String = "") =
            "booking_summary/$venueId/$sportId/$date/$slotIds/$bookingType?selectedDays=$selectedDays"
    }
    object BookingAdvancePaymentPreview : Screen("booking_advance_payment_preview/{orderId}/{amount}?checkoutUrl={checkoutUrl}") {
        fun createRoute(orderId: String, amount: Double, checkoutUrl: String) =
            "booking_advance_payment_preview/$orderId/$amount?checkoutUrl=${java.net.URLEncoder.encode(checkoutUrl, "UTF-8")}"
    }
    object BookingConfirmation : Screen("booking_confirmation")
    object BookingCancellationReview : Screen("booking_cancellation_review/{bookingId}/{cancellationMode}") {
        fun createRoute(bookingId: String, cancellationMode: String = "single") =
            "booking_cancellation_review/$bookingId/$cancellationMode"
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
    object TeamInvitations : Screen("team_invitations")
    object Points : Screen("points")
    object Referrals : Screen("referrals")
    object PaymentMethods : Screen("payment_methods")
    object NotificationsSettings : Screen("notifications_settings")
    object PrivacySecurity : Screen("privacy_security")
    object BlockedUsers : Screen("blocked_users")
    object ReportedUsers : Screen("reported_users")
    object HelpSupport : Screen("help_support")
    object AboutUs : Screen("about_us")
    object PlayerProfile : Screen("player_profile")
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
