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
import com.sportynix.app.presentation.booking.BookingSummaryScreen
import com.sportynix.app.presentation.home.HomeScreen
import com.sportynix.app.presentation.notification.NotificationScreen
import com.sportynix.app.presentation.payment.PaymentScreen
import com.sportynix.app.presentation.profile.ProfileScreen
import com.sportynix.app.presentation.search.SearchScreen
import com.sportynix.app.presentation.settings.SettingsScreen
import com.sportynix.app.presentation.splash.SplashScreen
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
                onNavigateToNotification = { navController.navigate(Screen.Notification.route) }
            )
        }

        composable(
            route = Screen.VenueDetail.route,
            arguments = listOf(navArgument("venueId") { type = NavType.StringType })
        ) {
            VenueDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSlotPicker = { venueId ->
                    navController.navigate(Screen.VenueSlotPicker.createRoute(venueId))
                }
            )
        }

        composable(
            route = Screen.VenueSlotPicker.route,
            arguments = listOf(navArgument("venueId") { type = NavType.StringType })
        ) {
            VenueSlotPickerScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToBookingSummary = { venueId, slotId, date ->
                    navController.navigate(Screen.BookingSummary.createRoute(venueId, slotId, date))
                }
            )
        }

        composable(
            route = Screen.BookingSummary.route,
            arguments = listOf(
                navArgument("venueId") { type = NavType.StringType },
                navArgument("slotId") { type = NavType.StringType },
                navArgument("date") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val venueId = backStackEntry.arguments?.getString("venueId") ?: ""
            val slotId = backStackEntry.arguments?.getString("slotId") ?: ""
            val date = backStackEntry.arguments?.getString("date") ?: ""
            BookingSummaryScreen(
                venueId = venueId,
                slotId = slotId,
                date = date,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPayment = { bookingId, amount ->
                    navController.navigate(Screen.Payment.createRoute(bookingId, amount))
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
                onNavigateToBookingHistory = {},
                onLogout = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Notification.route) {
            NotificationScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToVenueDetail = { venueId ->
                    navController.navigate(Screen.VenueDetail.createRoute(venueId))
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
