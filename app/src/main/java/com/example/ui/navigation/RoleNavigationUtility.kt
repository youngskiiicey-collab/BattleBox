package com.example.ui.navigation

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.example.data.model.UserProfile
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Type-safe route definitions for Battle Box role dashboards and shared feature screens.
 */
object BattleBoxRoute {
    const val ROLE_ROUTER = "role_router"
    const val MEMBER_HOME = "dashboard/member"
    const val TRAINER_HOME = "dashboard/trainer"
    const val ADMIN_HOME = "dashboard/admin"
    const val SCANNER = "staff/scanner"
    const val PASS = "pass"
    const val PLANS = "plans"
    const val PROFILE = "profile"
}

enum class RoleDashboardDestination(
    val route: String,
    val roleKey: String,
    val dashboardLabel: String
) {
    MEMBER_HOME(
        route = BattleBoxRoute.MEMBER_HOME,
        roleKey = UserProfile.ROLE_MEMBER,
        dashboardLabel = "Member Home Dashboard"
    ),
    TRAINER_HOME(
        route = BattleBoxRoute.TRAINER_HOME,
        roleKey = UserProfile.ROLE_TRAINER,
        dashboardLabel = "Trainer Command Dashboard"
    ),
    ADMIN_HOME(
        route = BattleBoxRoute.ADMIN_HOME,
        roleKey = UserProfile.ROLE_ADMIN,
        dashboardLabel = "Admin Operations Dashboard"
    )
}

data class RoleResolutionResult(
    val uid: String,
    val resolvedRole: String,
    val destination: RoleDashboardDestination,
    val onboardingCompleted: Boolean,
    val hasAdminRegistryDoc: Boolean
)

/**
 * Navigation utility that checks the user's authoritative role (Member, Trainer, Admin)
 * from Firestore upon login and redirects them to their respective home dashboard.
 */
object RoleNavigationUtility {

    private const val OWNER_ADMIN_EMAIL = "youngskiiicey@gmail.com"

    /**
     * Pure role-to-dashboard mapper used by both live login checks and tests.
     */
    fun destinationForRole(
        role: String?,
        hasAdminRegistryDoc: Boolean = false,
        isVerifiedOwner: Boolean = false
    ): RoleDashboardDestination {
        val normalized = role?.trim()?.lowercase()
        return when {
            isVerifiedOwner || hasAdminRegistryDoc || normalized == UserProfile.ROLE_ADMIN ->
                RoleDashboardDestination.ADMIN_HOME
            normalized == UserProfile.ROLE_TRAINER ->
                RoleDashboardDestination.TRAINER_HOME
            else ->
                RoleDashboardDestination.MEMBER_HOME
        }
    }

    /**
     * Reads the authenticated user's profile (`users/{uid}`) and admin registry (`admins/{uid}`)
     * directly from Firestore upon login and computes their target home dashboard.
     */
    suspend fun checkUserRoleAndResolveDashboard(
        db: FirebaseFirestore,
        uid: String,
        email: String,
        isEmailVerified: Boolean
    ): RoleResolutionResult {
        val isVerifiedOwner = isEmailVerified && email.equals(OWNER_ADMIN_EMAIL, ignoreCase = true)

        val userSnap = runCatching {
            db.collection("users").document(uid).get().await()
        }.getOrNull()

        val profile = userSnap?.takeIf { it.exists() }
            ?.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

        val hasAdminDoc = runCatching {
            db.collection("admins").document(uid).get().await().exists()
        }.getOrDefault(false)

        val destination = destinationForRole(
            role = profile?.role,
            hasAdminRegistryDoc = hasAdminDoc,
            isVerifiedOwner = isVerifiedOwner
        )

        val resolvedRole = when (destination) {
            RoleDashboardDestination.ADMIN_HOME -> UserProfile.ROLE_ADMIN
            RoleDashboardDestination.TRAINER_HOME -> UserProfile.ROLE_TRAINER
            RoleDashboardDestination.MEMBER_HOME -> UserProfile.ROLE_MEMBER
        }

        return RoleResolutionResult(
            uid = uid,
            resolvedRole = resolvedRole,
            destination = destination,
            onboardingCompleted = profile?.onboardingCompleted == true,
            hasAdminRegistryDoc = hasAdminDoc
        )
    }

    /**
     * Redirects the NavHostController to the resolved role's Home Dashboard and clears
     * intermediate routing state from the backstack.
     */
    fun redirectToRoleDashboard(
        navController: NavHostController,
        destination: RoleDashboardDestination
    ) {
        val currentRoute = navController.currentDestination?.route
        if (currentRoute == destination.route) return

        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                inclusive = true
            }
            launchSingleTop = true
        }
    }
}
