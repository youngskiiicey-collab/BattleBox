package com.example.ui.navigation

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.UserProfile
import com.example.data.repository.BattleBoxRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class RoleNavigationUtilityRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun destinationForRole_mapsEachRoleToDedicatedHomeDashboard() {
        assertEquals(
            RoleDashboardDestination.MEMBER_HOME,
            RoleNavigationUtility.destinationForRole(UserProfile.ROLE_MEMBER)
        )
        assertEquals(
            RoleDashboardDestination.TRAINER_HOME,
            RoleNavigationUtility.destinationForRole(UserProfile.ROLE_TRAINER)
        )
        assertEquals(
            RoleDashboardDestination.ADMIN_HOME,
            RoleNavigationUtility.destinationForRole(UserProfile.ROLE_ADMIN)
        )
        assertEquals(
            RoleDashboardDestination.ADMIN_HOME,
            RoleNavigationUtility.destinationForRole(UserProfile.ROLE_MEMBER, hasAdminRegistryDoc = true)
        )
        assertEquals(
            RoleDashboardDestination.ADMIN_HOME,
            RoleNavigationUtility.destinationForRole(UserProfile.ROLE_MEMBER, isVerifiedOwner = true)
        )
    }

    @Test
    fun checkUserRoleAndResolveDashboard_readsFirestoreRoleOnLogin() = runBlocking {
        // 1. Standard Member login resolves from Firestore users/{uid} -> MEMBER_HOME
        val memberEmail = "nav_member_${UUID.randomUUID().toString().take(8)}@test.com"
        val memberUid = signInTestUser(memberEmail)
        val repo = BattleBoxRepository(firestore)

        withTimeout(8000L) {
            repo.completeOnboarding(
                displayName = "Kiran Das",
                email = memberEmail,
                phone = null,
                emergencyContact = null,
                photoUrl = null,
                isBootstrappedOwner = false
            )
        }

        val memberResult = withTimeout(8000L) {
            RoleNavigationUtility.checkUserRoleAndResolveDashboard(
                db = firestore,
                uid = memberUid,
                email = memberEmail,
                isEmailVerified = true
            )
        }
        assertEquals(UserProfile.ROLE_MEMBER, memberResult.resolvedRole)
        assertEquals(RoleDashboardDestination.MEMBER_HOME, memberResult.destination)
        assertTrue(memberResult.onboardingCompleted)
        assertFalse(memberResult.hasAdminRegistryDoc)

        // 2. Owner/Admin resolves via verified owner / Firestore role -> ADMIN_HOME
        val adminResult = withTimeout(8000L) {
            RoleNavigationUtility.checkUserRoleAndResolveDashboard(
                db = firestore,
                uid = memberUid,
                email = "youngskiiicey@gmail.com",
                isEmailVerified = true
            )
        }
        assertEquals(UserProfile.ROLE_ADMIN, adminResult.resolvedRole)
        assertEquals(RoleDashboardDestination.ADMIN_HOME, adminResult.destination)
    }
}
