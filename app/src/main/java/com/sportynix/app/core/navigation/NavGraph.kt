package com.sportynix.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.sportynix.app.presentation.authentication.ForgotPasswordEmailScreen
import com.sportynix.app.presentation.authentication.ForgotPasswordOtpScreen
import com.sportynix.app.presentation.authentication.OTPVerificationScreen
import com.sportynix.app.presentation.authentication.ResetPasswordScreen
import com.sportynix.app.presentation.authentication.SignInScreen
import com.sportynix.app.presentation.authentication.SignUpScreen
import com.sportynix.app.presentation.authentication.WelcomeScreen
import com.sportynix.app.data.remote.dto.BookingPayload
import com.sportynix.app.presentation.booking.BookingDetailScreen
import com.sportynix.app.presentation.booking.BookingHistoryScreen
import com.sportynix.app.presentation.booking.BookingSummaryScreen
import com.sportynix.app.presentation.home.HomeScreen
import com.sportynix.app.presentation.notification.NotificationScreen
import com.sportynix.app.presentation.payment.PaymentScreen
import com.sportynix.app.presentation.profile.EditProfileScreen
import com.sportynix.app.presentation.profile.FavoritesScreen
import com.sportynix.app.presentation.profile.PaymentMethodsScreen
import com.sportynix.app.presentation.profile.PointsScreen
import com.sportynix.app.presentation.profile.ProfileScreen
import com.sportynix.app.presentation.profile.ReferralScreen
import com.sportynix.app.presentation.profile.TeamScreen
import com.sportynix.app.presentation.search.SearchScreen
import com.sportynix.app.presentation.settings.AboutScreen
import com.sportynix.app.presentation.settings.BlockedUsersScreen
import com.sportynix.app.presentation.settings.HelpSupportScreen
import com.sportynix.app.presentation.settings.NotificationsSettingsScreen
import com.sportynix.app.presentation.settings.PrivacySecurityScreen
import com.sportynix.app.presentation.settings.ReportedUsersScreen
import com.sportynix.app.presentation.settings.SettingsScreen
import com.sportynix.app.presentation.splash.SplashScreen
import com.sportynix.app.presentation.venue.SportDetailScreen
import com.sportynix.app.presentation.venue.VenueDetailScreen
import com.sportynix.app.presentation.venue.VenueSlotPickerScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onNavigateToSignIn = { navController.navigate(Screen.Login.route) },
                onNavigateToSignUp = { navController.navigate(Screen.Register.route) },
                onContinueAsGuest = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            SignInScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToSignUp = { navController.navigate(Screen.Register.route) },
                onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPasswordEmail.route) }
            )
        }

        composable(Screen.Register.route) {
            SignUpScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateToSignIn = { navController.popBackStack() },
                onNavigateToOtp = { sessionId, phone, email ->
                    navController.navigate(Screen.OTPVerification.createRoute(sessionId, phone, email))
                }
            )
        }

        composable(
            route = Screen.OTPVerification.route,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.StringType },
                navArgument("phone") { type = NavType.StringType },
                navArgument("email") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            val phone = backStackEntry.arguments?.getString("phone") ?: ""
            val email = backStackEntry.arguments?.getString("email") ?: ""
            OTPVerificationScreen(
                sessionId = sessionId,
                phoneNumber = phone,
                email = email,
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ForgotPasswordEmail.route) {
            ForgotPasswordEmailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToResetOtp = { sessionId, email ->
                    navController.navigate(Screen.ForgotPasswordOtp.createRoute(email))
                }
            )
        }

        composable(
            route = Screen.ForgotPasswordOtp.route,
            arguments = listOf(navArgument("email") { type = NavType.StringType; defaultValue = "" })
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            ForgotPasswordOtpScreen(
                email = email,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToReset = { resetEmail, otp ->
                    navController.navigate(Screen.ResetPassword.createRoute(resetEmail, otp))
                }
            )
        }

        composable(
            route = Screen.ResetPassword.route,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType; defaultValue = "" },
                navArgument("otp") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val otp = backStackEntry.arguments?.getString("otp") ?: ""
            ResetPasswordScreen(
                email = email,
                otpCode = otp,
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Home.route) {
            com.sportynix.app.presentation.navigation.MainTabScreen(
                onNavigateToVenueDetail = { venueId ->
                    navController.navigate(Screen.VenueDetail.createRoute(venueId))
                },
                onNavigateToBookingDetail = { bookingId ->
                    navController.navigate(Screen.BookingDetail.createRoute(bookingId.toString()))
                },
                onNavigateToNewBooking = {
                    navController.navigate(Screen.Booking.createRoute("1", "1"))
                },
                onNavigateToNotification = { navController.navigate(Screen.Notification.route) },
                onNavigateToLeagues = { navController.navigate(Screen.LeagueList.route) },
                onNavigateToTournaments = { navController.navigate(Screen.TournamentList.route) },
                onNavigateToLiveCricket = { matchId -> navController.navigate(Screen.LiveCricketScoring.createRoute(matchId)) },
                onNavigateToAuction = { },
                onNavigateToEditProfile = { navController.navigate(Screen.EditProfile.route) },
                onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
                onNavigateToPoints = { navController.navigate(Screen.Points.route) },
                onNavigateToReferral = { navController.navigate(Screen.Referrals.route) },
                onNavigateToPaymentMethods = { navController.navigate(Screen.PaymentMethods.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToHelpSupport = { navController.navigate(Screen.HelpSupport.route) },
                onNavigateToAbout = { navController.navigate(Screen.AboutUs.route) },
                onNavigateToTeam = { navController.navigate(Screen.Team.route) },
                onNavigateToSignIn = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.VenueList.route) {
            com.sportynix.app.presentation.venue.NearbyVenuesScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToVenueDetail = { venueId ->
                    navController.navigate(Screen.VenueDetail.createRoute(venueId))
                }
            )
        }

        composable(
            route = Screen.VenueDetail.route,
            arguments = listOf(navArgument("venueId") { type = NavType.StringType })
        ) { backStackEntry ->
            val venueId = backStackEntry.arguments?.getString("venueId") ?: ""
            com.sportynix.app.presentation.venue.VenueDetailScreen(
                venueId = venueId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSportDetail = { vId, sId ->
                    navController.navigate(Screen.SportDetail.createRoute(sId, vId))
                },
                onNavigateToBooking = { vId, sId, _, _, _ ->
                    navController.navigate(Screen.Booking.createRoute(vId, sId))
                },
                onNavigateToMap = { vId, lat, lng, name, loc, rating, img ->
                    navController.navigate(Screen.VenueMap.createRoute(vId, lat, lng, name, loc, rating, img))
                },
                onNavigateToLeagueDetail = { leagueId ->
                    navController.navigate(Screen.LeagueDetail.createRoute(leagueId))
                },
                onNavigateToTournamentDetail = { tournamentId ->
                    navController.navigate(Screen.TournamentDetail.createRoute(tournamentId))
                }
            )
        }

        composable(
            route = Screen.SportDetail.route,
            arguments = listOf(
                navArgument("sportId") { type = NavType.StringType },
                navArgument("venueId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sportId = backStackEntry.arguments?.getString("sportId") ?: ""
            val venueId = backStackEntry.arguments?.getString("venueId") ?: ""
            com.sportynix.app.presentation.venue.SportDetailScreen(
                sportId = sportId,
                venueId = venueId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToBooking = { vId, sId, _, _, _ ->
                    navController.navigate(Screen.Booking.createRoute(vId, sId))
                }
            )
        }

        composable(
            route = Screen.VenueMap.route,
            arguments = listOf(
                navArgument("venueId") { type = NavType.StringType; defaultValue = "1" },
                navArgument("lat") { type = NavType.FloatType; defaultValue = 7.118318f },
                navArgument("lng") { type = NavType.FloatType; defaultValue = 80.079777f },
                navArgument("name") { type = NavType.StringType; defaultValue = "Venue" },
                navArgument("location") { type = NavType.StringType; defaultValue = "" },
                navArgument("rating") { type = NavType.IntType; defaultValue = 5 },
                navArgument("image") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val lat = backStackEntry.arguments?.getFloat("lat")?.toDouble() ?: 7.118318
            val lng = backStackEntry.arguments?.getFloat("lng")?.toDouble() ?: 80.079777
            val name = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("name") ?: "Venue", "UTF-8")
            val location = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("location") ?: "", "UTF-8")
            val rating = backStackEntry.arguments?.getInt("rating") ?: 5
            val image = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("image") ?: "", "UTF-8")

            com.sportynix.app.presentation.venue.ComplexMapView(
                complexName = name,
                complexLocation = location,
                complexRating = rating,
                complexImageURL = image,
                latitude = lat,
                longitude = lng,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Booking.route,
            arguments = listOf(
                navArgument("venueId") { type = NavType.StringType },
                navArgument("sportId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val venueId = backStackEntry.arguments?.getString("venueId") ?: ""
            val sportId = backStackEntry.arguments?.getString("sportId") ?: ""
            com.sportynix.app.presentation.booking.BookingScreen(
                venueId = venueId.toIntOrNull() ?: 1,
                sportId = sportId.toIntOrNull() ?: 1,
                sportName = "Sport",
                sportPrice = "400.00",
                sportImageURL = "",
                complexName = "Sportynix Complex",
                complexLocation = "Location",
                complexRating = 4.5,
                complexReviews = 10,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSummary = { payload ->
                    val slotKeys = payload.slots.joinToString(",") { "${it.startTime}-${it.endTime}" }
                    val daysStr = payload.selectedDays.joinToString(",")
                    navController.navigate(Screen.BookingSummary.createRoute(payload.venueId.toString(), payload.sportId.toString(), payload.bookingDate, slotKeys, payload.bookingType, daysStr))
                }
            )
        }

        composable(
            route = Screen.BookingSummary.route,
            arguments = listOf(
                navArgument("venueId") { type = NavType.StringType },
                navArgument("sportId") { type = NavType.StringType },
                navArgument("date") { type = NavType.StringType },
                navArgument("slotIds") { type = NavType.StringType },
                navArgument("bookingType") { type = NavType.StringType; defaultValue = "Normal" },
                navArgument("selectedDays") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val venueId = backStackEntry.arguments?.getString("venueId") ?: ""
            val sportId = backStackEntry.arguments?.getString("sportId") ?: ""
            val date = backStackEntry.arguments?.getString("date") ?: ""
            val slotIds = backStackEntry.arguments?.getString("slotIds") ?: ""
            val bookingType = backStackEntry.arguments?.getString("bookingType") ?: "Normal"
            val selectedDays = backStackEntry.arguments?.getString("selectedDays")?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

            val slotsList = slotIds.split(",").mapNotNull { key ->
                val parts = key.split("-")
                if (parts.size >= 2) {
                    com.sportynix.app.data.remote.dto.BookingSlotInfo(
                        startTime = parts[0],
                        endTime = parts[1],
                        displayStart = parts[0],
                        displayEnd = parts[1],
                        duration = 60,
                        price = 500.0
                    )
                } else null
            }

            val payload = com.sportynix.app.data.remote.dto.BookingPayload(
                sportId = sportId.toIntOrNull() ?: 1,
                sportName = "Sport",
                sportPrice = "500.00",
                sportImageURL = "",
                venueId = venueId.toIntOrNull() ?: 1,
                venueName = "Sportynix Venue",
                venueAddress = "Location",
                bookingType = bookingType,
                bookingDate = date,
                selectedDays = selectedDays,
                slots = slotsList,
                totalPrice = slotsList.sumOf { it.price }
            )

            com.sportynix.app.presentation.booking.BookingSummaryScreen(
                payload = payload,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCheckout = { checkoutResp ->
                    val orderId = checkoutResp.payment?.orderId ?: "ORD123"
                    val amount = checkoutResp.payment?.amount ?: 500.0
                    val url = checkoutResp.checkout?.url ?: ""
                    navController.navigate(Screen.BookingAdvancePaymentPreview.createRoute(orderId, amount, url))
                }
            )
        }

        composable(
            route = Screen.BookingAdvancePaymentPreview.route,
            arguments = listOf(
                navArgument("orderId") { type = NavType.StringType },
                navArgument("amount") { type = NavType.FloatType },
                navArgument("checkoutUrl") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            val amount = backStackEntry.arguments?.getFloat("amount")?.toDouble() ?: 0.0
            val checkoutUrl = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("checkoutUrl") ?: "", "UTF-8")

            com.sportynix.app.presentation.booking.BookingAdvancePaymentPreviewScreen(
                checkoutResponse = com.sportynix.app.data.remote.dto.PaymentCheckoutResponseDto(
                    checkout = com.sportynix.app.data.remote.dto.PaymentCheckoutUrlDto(url = checkoutUrl),
                    payment = com.sportynix.app.data.remote.dto.PaymentOrderInfoDto(orderId = orderId, amount = amount, purpose = "advance"),
                    bookings = emptyList(),
                    reservationExpiresAt = null
                ),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToConfirmation = { confirmedBookings, type ->
                    navController.navigate(Screen.BookingConfirmation.route) {
                        popUpTo(Screen.Booking.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.BookingConfirmation.route) {
            com.sportynix.app.presentation.booking.BookingConfirmationScreen(
                bookings = emptyList(),
                bookingType = "Normal",
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToBookingDetail = { bookingId ->
                    navController.navigate(Screen.BookingDetail.createRoute(bookingId))
                }
            )
        }

        composable(
            route = Screen.BookingCancellationReview.route,
            arguments = listOf(
                navArgument("bookingId") { type = NavType.StringType },
                navArgument("cancellationMode") { type = NavType.StringType; defaultValue = "single" }
            )
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getString("bookingId")?.toIntOrNull() ?: 1
            val cancellationMode = backStackEntry.arguments?.getString("cancellationMode") ?: "single"

            com.sportynix.app.presentation.booking.BookingCancellationReviewScreen(
                bookingId = bookingId,
                cancellationMode = cancellationMode,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToBookingDetail = { id ->
                    navController.navigate(Screen.BookingDetail.createRoute(id.toString()))
                },
                onNavigateToBookingHistory = {
                    navController.navigate(Screen.BookingHistory.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
            )
        }

        composable(Screen.BookingHistory.route) {
            com.sportynix.app.presentation.booking.BookingHistoryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { booking ->
                    navController.navigate(Screen.BookingDetail.createRoute(booking.bookingId.toString()))
                },
                onNavigateToCancel = { booking ->
                    navController.navigate(Screen.BookingCancellationReview.createRoute(booking.bookingId.toString(), if (booking.isPermanent) "series" else "single"))
                }
            )
        }

        composable(
            route = Screen.BookingDetail.route,
            arguments = listOf(navArgument("bookingId") { type = NavType.StringType }),
            deepLinks = listOf(
                navDeepLink { uriPattern = "sportynix://booking-detail?id={bookingId}" },
                navDeepLink { uriPattern = "sportynix://booking?id={bookingId}" }
            )
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
            com.sportynix.app.presentation.booking.BookingDetailScreen(
                bookingId = bookingId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCancel = { booking, mode ->
                    navController.navigate(Screen.BookingCancellationReview.createRoute(booking.bookingId.toString(), mode))
                },
                onNavigateToCheckout = { checkoutResp ->
                    val orderId = checkoutResp.payment?.orderId ?: "ORD123"
                    val amount = checkoutResp.payment?.amount ?: 500.0
                    val url = checkoutResp.checkout?.url ?: ""
                    navController.navigate(Screen.BookingAdvancePaymentPreview.createRoute(orderId, amount, url))
                }
            )
        }

        composable(
            route = Screen.Payment.route,
            arguments = listOf(
                navArgument("bookingId") { type = NavType.StringType },
                navArgument("amount") { type = NavType.FloatType }
            )
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
            val amount = backStackEntry.arguments?.getFloat("amount")?.toDouble() ?: 0.0
            PaymentScreen(
                bookingId = bookingId,
                amount = amount,
                onNavigateBack = { navController.popBackStack() },
                onPaymentSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToBookingHistory = { navController.navigate(Screen.BookingHistory.route) },
                onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
                onNavigateToTeam = { navController.navigate(Screen.Team.route) },
                onNavigateToEditProfile = { navController.navigate(Screen.EditProfile.route) },
                onNavigateToPayments = { navController.navigate(Screen.PaymentMethods.route) },
                onNavigateToPoints = { navController.navigate(Screen.Points.route) },
                onNavigateToReferrals = { navController.navigate(Screen.Referrals.route) },
                onNavigateToAboutUs = { navController.navigate(Screen.AboutUs.route) },
                onLogout = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.EditProfile.route) {
            EditProfileScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Points.route) {
            PointsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Referrals.route) {
            ReferralScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.PaymentMethods.route) {
            PaymentMethodsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.NotificationsSettings.route) {
            NotificationsSettingsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.PrivacySecurity.route) {
            PrivacySecurityScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.BlockedUsers.route) {
            BlockedUsersScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.ReportedUsers.route) {
            ReportedUsersScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.HelpSupport.route) {
            HelpSupportScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.AboutUs.route) {
            AboutScreen(onNavigateBack = { navController.popBackStack() })
        }



        composable(Screen.Favorites.route) {
            FavoritesScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToVenueDetail = { venueId ->
                    navController.navigate(Screen.VenueDetail.createRoute(venueId))
                }
            )
        }

        composable(Screen.Team.route) {
            TeamScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.TeamInvitations.route) {
            TeamScreen(onNavigateBack = { navController.popBackStack() }, initialTab = 2)
        }

        composable(Screen.Notification.route) {
            NotificationScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToBookingDetail = { navController.navigate(Screen.BookingDetail.createRoute(it.toString())) },
                onNavigateToBookingHistory = { navController.navigate(Screen.BookingHistory.route) },
                onNavigateToTeam = { _, invitations -> navController.navigate(if (invitations) Screen.TeamInvitations.route else Screen.Team.route) },
                onNavigateToPoints = { navController.navigate(Screen.Points.route) }
            )
        }

        composable(Screen.Search.route) {
            com.sportynix.app.presentation.search.SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToVenueDetail = { venueId ->
                    navController.navigate(Screen.VenueDetail.createRoute(venueId))
                },
                onNavigateToSportDetail = { sportId, venueId ->
                    navController.navigate(Screen.SportDetail.createRoute(sportId, venueId))
                },
                onNavigateToTeamDetail = { teamId ->
                    navController.navigate(Screen.Team.route)
                },
                onNavigateToEvents = { navController.navigate(Screen.LeagueList.route) },
                onNavigateToHistory = { navController.navigate(Screen.BookingHistory.route) },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditProfile = { navController.navigate(Screen.EditProfile.route) },
                onNavigateToBlockedUsers = { navController.navigate(Screen.BlockedUsers.route) },
                onNavigateToReportedUsers = { navController.navigate(Screen.ReportedUsers.route) },
                onLogout = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.LeagueList.route) {
            com.sportynix.app.presentation.leagues.LeagueListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLeagueDetail = { leagueId ->
                    navController.navigate(Screen.LeagueDetail.createRoute(leagueId))
                }
            )
        }

        composable(
            route = Screen.LeagueDetail.route,
            arguments = listOf(navArgument("leagueId") { type = NavType.StringType }),
            deepLinks = listOf(
                navDeepLink { uriPattern = "sportynix://league?id={leagueId}" },
                navDeepLink { uriPattern = "sportynix://league-detail?id={leagueId}" }
            )
        ) { backStackEntry ->
            val leagueId = backStackEntry.arguments?.getString("leagueId") ?: ""
            com.sportynix.app.presentation.leagues.LeagueDetailScreen(
                leagueId = leagueId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.TournamentList.route) {
            com.sportynix.app.presentation.tournaments.TournamentListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTournamentDetail = { tournamentId ->
                    navController.navigate(Screen.TournamentDetail.createRoute(tournamentId))
                }
            )
        }

        composable(
            route = Screen.TournamentDetail.route,
            arguments = listOf(navArgument("tournamentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tournamentId = backStackEntry.arguments?.getString("tournamentId") ?: ""
            com.sportynix.app.presentation.tournaments.TournamentDetailScreen(
                tournamentId = tournamentId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.LiveCricketScoring.route,
            arguments = listOf(navArgument("matchId") { type = NavType.StringType })
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
            com.sportynix.app.presentation.cricket.LiveCricketScoringScreen(
                matchId = matchId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Auction.route,
            arguments = listOf(navArgument("auctionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val auctionId = backStackEntry.arguments?.getString("auctionId") ?: ""
            com.sportynix.app.presentation.auction.AuctionScreen(
                auctionId = auctionId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
