package com.sportynix.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sportynix.app.presentation.authentication.ForgotPasswordEmailScreen
import com.sportynix.app.presentation.authentication.OTPVerificationScreen
import com.sportynix.app.presentation.authentication.ResetPasswordScreen
import com.sportynix.app.presentation.authentication.SignInScreen
import com.sportynix.app.presentation.authentication.SignUpScreen
import com.sportynix.app.presentation.authentication.WelcomeScreen
import com.sportynix.app.presentation.booking.BookingDetailScreen
import com.sportynix.app.presentation.booking.BookingHistoryScreen
import com.sportynix.app.presentation.booking.BookingSummaryScreen
import com.sportynix.app.presentation.home.HomeScreen
import com.sportynix.app.presentation.notification.NotificationScreen
import com.sportynix.app.presentation.payment.PaymentScreen
import com.sportynix.app.presentation.profile.EditProfileScreen
import com.sportynix.app.presentation.profile.FavoritesScreen
import com.sportynix.app.presentation.profile.ProfileScreen
import com.sportynix.app.presentation.profile.TeamScreen
import com.sportynix.app.presentation.search.SearchScreen
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
                    navController.navigate(Screen.ResetPassword.createRoute(email))
                }
            )
        }

        composable(
            route = Screen.ResetPassword.route,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            ResetPasswordScreen(
                email = email,
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToVenueDetail = { venueId ->
                    navController.navigate(Screen.VenueDetail.createRoute(venueId))
                },
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToNotification = { navController.navigate(Screen.Notification.route) },
                onNavigateToBookingHistory = { navController.navigate(Screen.BookingHistory.route) },
                onNavigateToLeagues = { navController.navigate(Screen.LeagueList.route) },
                onNavigateToTournaments = { navController.navigate(Screen.TournamentList.route) }
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
        ) {
            VenueDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSlotBooking = { venueId ->
                    navController.navigate(Screen.Booking.createRoute(venueId, "1"))
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
            SportDetailScreen(
                sportId = sportId,
                venueId = venueId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSlotPicker = { id ->
                    navController.navigate(Screen.Booking.createRoute(venueId, sportId))
                }
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
                venueId = venueId,
                sportId = sportId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSummary = { vId, sId, date, slotIds, bType, days ->
                    navController.navigate(Screen.BookingSummary.createRoute(vId, sId, date, slotIds, bType, days))
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
            val selectedDays = backStackEntry.arguments?.getString("selectedDays") ?: ""
            com.sportynix.app.presentation.booking.BookingSummaryScreen(
                venueId = venueId,
                sportId = sportId,
                date = date,
                slotIds = slotIds,
                bookingType = bookingType,
                selectedDays = selectedDays,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPaymentPreview = { checkoutUrl, orderId, amount ->
                    navController.navigate(Screen.BookingAdvancePaymentPreview.createRoute(orderId, amount, checkoutUrl))
                },
                onNavigateToConfirmation = {
                    navController.navigate(Screen.BookingConfirmation.route)
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
                checkoutUrl = checkoutUrl,
                orderId = orderId,
                amount = amount,
                onNavigateBack = { navController.popBackStack() },
                onPaymentVerified = {
                    navController.navigate(Screen.BookingConfirmation.route)
                }
            )
        }

        composable(Screen.BookingConfirmation.route) {
            com.sportynix.app.presentation.booking.BookingConfirmationScreen(
                onHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToDetails = { bookingId ->
                    navController.navigate(Screen.BookingDetail.createRoute(bookingId))
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
                onNavigateToPayments = { navController.navigate(Screen.Payment.createRoute("history", 0.0)) },
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

        composable(Screen.BookingHistory.route) {
            BookingHistoryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { bookingId ->
                    navController.navigate(Screen.BookingDetail.createRoute(bookingId))
                }
            )
        }

        composable(
            route = Screen.BookingDetail.route,
            arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
            com.sportynix.app.presentation.booking.BookingDetailScreen(
                bookingId = bookingId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCancellationReview = { bId, mode ->
                    navController.navigate(Screen.BookingCancellationReview.createRoute(bId, mode))
                },
                onNavigateToPayBalance = { bId, amt ->
                    navController.navigate(Screen.BookingAdvancePaymentPreview.createRoute(bId, amt, ""))
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
            val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
            val cancellationMode = backStackEntry.arguments?.getString("cancellationMode") ?: "single"
            com.sportynix.app.presentation.booking.BookingCancellationReviewScreen(
                bookingId = bookingId,
                cancellationMode = cancellationMode,
                onNavigateBack = { navController.popBackStack() },
                onCancellationCompleted = {
                    navController.navigate(Screen.BookingHistory.route) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
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

        composable(Screen.Notification.route) {
            NotificationScreen(onNavigateBack = { navController.popBackStack() })
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
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
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
            arguments = listOf(navArgument("leagueId") { type = NavType.StringType })
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

