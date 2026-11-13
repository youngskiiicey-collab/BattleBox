package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.ai.BattleBoxConciergeRepository
import com.example.data.model.AttendanceRecord
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.repository.BattleBoxRepository
import com.example.data.repository.Phase2Repository
import com.example.security.DeviceFix
import com.example.security.GeofenceUtils
import com.example.ui.auth.SignInScreen
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.signOutUser
import com.example.ui.components.BattleBoxCrest
import com.example.ui.components.ConciergeSheet
import com.example.ui.navigation.BattleBoxRoute
import com.example.ui.navigation.RoleDashboardDestination
import com.example.ui.navigation.RoleNavigationUtility
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.screens.AdminConsoleScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.PassAndCheckInScreen
import com.example.ui.screens.PlansAndBillingScreen
import com.example.ui.screens.ProfileAndLegalScreen
import com.example.ui.screens.QrAttendanceScannerScreen
import com.example.ui.screens.TrainerRosterScreen
import com.example.ui.theme.BattleBoxTheme
import com.example.ui.viewmodel.BattleBoxViewModel
import com.example.utils.BattleBoxNotificationHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private data class NavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun BattleBoxApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }
    var isDarkTheme by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = { currentUser = Firebase.auth.currentUser },
            onUnauthenticated = {},
            scope = scope
        )
    }

    BattleBoxTheme(darkTheme = isDarkTheme) {
        val user = currentUser
        if (user == null) {
            SignInScreen(
                onAuthSuccess = { currentUser = Firebase.auth.currentUser }
            )
        } else {
            key(user.uid) {
                val appContext = context.applicationContext
                val db = remember(appContext) {
                    FirebaseFirestore.getInstance(appContext.getString(R.string.firestore_database_id))
                }
                val vm: BattleBoxViewModel = viewModel(
                    key = user.uid,
                    factory = viewModelFactory {
                        initializer {
                            val app = checkNotNull(this[APPLICATION_KEY])
                            val dbId = app.getString(R.string.firestore_database_id)
                            val firestoreDb = FirebaseFirestore.getInstance(dbId)
                            val coreRepo = BattleBoxRepository(firestoreDb)
                            BattleBoxViewModel(
                                repository = coreRepo,
                                phase2Repository = Phase2Repository(firestoreDb, coreRepo),
                                conciergeRepository = BattleBoxConciergeRepository(),
                                currentUserId = user.uid,
                                currentUserEmail = user.email.orEmpty(),
                                currentUserDisplayName = user.displayName.orEmpty(),
                                isEmailVerified = user.isEmailVerified
                            )
                        }
                    }
                )

                val vmDark by vm.isDarkTheme.collectAsStateWithLifecycle()
                LaunchedEffect(vmDark) { isDarkTheme = vmDark }

                AuthenticatedBattleBoxShell(
                    firestoreDb = db,
                    viewModel = vm,
                    onSignOut = {
                        signOutUser(credentialManager, scope) {
                            currentUser = null
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedBattleBoxShell(
    firestoreDb: FirebaseFirestore,
    viewModel: BattleBoxViewModel,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val gymConfig by viewModel.gymConfig.collectAsStateWithLifecycle()
    val gymPublicStatus by viewModel.gymPublicStatus.collectAsStateWithLifecycle()
    val myAttendance by viewModel.myAttendance.collectAsStateWithLifecycle()
    val myConsents by viewModel.myConsents.collectAsStateWithLifecycle()
    val rejections by viewModel.rejections.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val liveRoster by viewModel.liveRoster.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
    val assignedMembers by viewModel.assignedMembers.collectAsStateWithLifecycle()
    val plans by viewModel.plans.collectAsStateWithLifecycle()
    val myPaymentRequests by viewModel.myPaymentRequests.collectAsStateWithLifecycle()
    val allPaymentRequests by viewModel.allPaymentRequests.collectAsStateWithLifecycle()
    val mySubscriptions by viewModel.mySubscriptions.collectAsStateWithLifecycle()
    val myPayments by viewModel.myPayments.collectAsStateWithLifecycle()
    val notificationPref by viewModel.notificationPref.collectAsStateWithLifecycle()
    val announcements by viewModel.announcements.collectAsStateWithLifecycle()
    val deliveryLogs by viewModel.deliveryLogs.collectAsStateWithLifecycle()
    val actionBanner by viewModel.actionBanner.collectAsStateWithLifecycle()
    val lastScanOutcome by viewModel.lastScanOutcome.collectAsStateWithLifecycle()
    val monthlyExportState by viewModel.monthlyExportState.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isConciergeBusy by viewModel.isConciergeBusy.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var deviceFix by remember { mutableStateOf<DeviceFix?>(null) }
    var showConcierge by remember { mutableStateOf(false) }
    var lastNotifiedAnnId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(announcements.firstOrNull()?.id) {
        val latest = announcements.firstOrNull()
        if (latest != null && latest.id != lastNotifiedAnnId && latest.sendPushNotification) {
            lastNotifiedAnnId = latest.id
            BattleBoxNotificationHelper.showAnnouncementNotification(
                context = context,
                idSeed = latest.id,
                title = "Battle Box: ${latest.title}",
                body = "${latest.message}${if (latest.effectiveHours.isNotBlank()) " (${latest.effectiveHours})" else ""}"
            )
        }
    }

    LaunchedEffect(actionBanner) {
        val msg = actionBanner
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissBanner()
        }
    }

    LaunchedEffect(Unit) {
        deviceFix = GeofenceUtils.fetchCurrentLocation(context)
    }

    when (val state = profileState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is UiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Unable to load profile",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(text = state.message, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        is UiState.Success -> {
            val profile = state.data
            if (profile == null || !profile.onboardingCompleted) {
                OnboardingScreen(
                    initialName = viewModel.currentUserDisplayName,
                    email = viewModel.currentUserEmail,
                    isBootstrappedOwner = viewModel.isBootstrappedOwner,
                    isBusy = isBusy,
                    onSubmit = { name, phone, emergency ->
                        viewModel.completeOnboarding(
                            displayName = name,
                            phone = phone,
                            emergencyContact = emergency,
                            photoUrl = null
                        )
                    },
                    onSignOut = onSignOut
                )
            } else {
                val effectiveProfile = if (viewModel.isBootstrappedOwner && profile.role != UserProfile.ROLE_ADMIN) {
                    profile.copy(role = UserProfile.ROLE_ADMIN)
                } else {
                    profile
                }

                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route

                var resolvedDestination by remember(effectiveProfile.userId) {
                    mutableStateOf(
                        RoleNavigationUtility.destinationForRole(
                            role = effectiveProfile.role,
                            isVerifiedOwner = viewModel.isBootstrappedOwner
                        )
                    )
                }

                // Role Navigation Utility: checks user's role in Firestore upon login & redirects
                LaunchedEffect(effectiveProfile.userId, effectiveProfile.role, viewModel.isBootstrappedOwner) {
                    val resolution = RoleNavigationUtility.checkUserRoleAndResolveDashboard(
                        db = firestoreDb,
                        uid = effectiveProfile.userId,
                        email = viewModel.currentUserEmail,
                        isEmailVerified = viewModel.isEmailVerified
                    )
                    resolvedDestination = resolution.destination
                    RoleNavigationUtility.redirectToRoleDashboard(navController, resolution.destination)
                }

                // Background sweep for 3h auto-checkout
                LaunchedEffect(myAttendance, liveRoster, resolvedDestination) {
                    when (resolvedDestination) {
                        RoleDashboardDestination.ADMIN_HOME -> viewModel.sweepExpired(liveRoster, isAdmin = true)
                        RoleDashboardDestination.TRAINER_HOME -> viewModel.sweepExpired(liveRoster, isAdmin = false)
                        RoleDashboardDestination.MEMBER_HOME -> viewModel.sweepExpired(myAttendance, isAdmin = false)
                    }
                }

                val roleHomeTab = when (resolvedDestination) {
                    RoleDashboardDestination.ADMIN_HOME -> NavTab(
                        route = BattleBoxRoute.ADMIN_HOME,
                        label = "Admin",
                        selectedIcon = Icons.Filled.AdminPanelSettings,
                        unselectedIcon = Icons.Outlined.AdminPanelSettings
                    )
                    RoleDashboardDestination.TRAINER_HOME -> NavTab(
                        route = BattleBoxRoute.TRAINER_HOME,
                        label = "Trainer",
                        selectedIcon = Icons.Filled.FitnessCenter,
                        unselectedIcon = Icons.Outlined.FitnessCenter
                    )
                    RoleDashboardDestination.MEMBER_HOME -> NavTab(
                        route = BattleBoxRoute.MEMBER_HOME,
                        label = "Home",
                        selectedIcon = Icons.Filled.Home,
                        unselectedIcon = Icons.Outlined.Home
                    )
                }

                val isStaffRole = resolvedDestination == RoleDashboardDestination.TRAINER_HOME ||
                    resolvedDestination == RoleDashboardDestination.ADMIN_HOME

                val tabs = remember(roleHomeTab.route, isStaffRole) {
                    buildList {
                        add(roleHomeTab)
                        if (isStaffRole) {
                            add(
                                NavTab(
                                    BattleBoxRoute.SCANNER,
                                    "Scanner",
                                    Icons.Filled.QrCodeScanner,
                                    Icons.Outlined.QrCodeScanner
                                )
                            )
                        }
                        add(NavTab(BattleBoxRoute.PASS, "Pass", Icons.Filled.QrCode2, Icons.Outlined.QrCode2))
                        add(NavTab(BattleBoxRoute.PLANS, "Plans", Icons.Filled.CreditCard, Icons.Outlined.CreditCard))
                        add(NavTab(BattleBoxRoute.PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person))
                    }
                }

                val navigateTopLevel: (String) -> Unit = { targetRoute ->
                    navController.navigate(targetRoute) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }

                val activeSession = myAttendance.firstOrNull {
                    it.status == AttendanceRecord.STATUS_CHECKED_IN
                }

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val useRail = maxWidth >= 680.dp

                    Scaffold(
                        contentWindowInsets = WindowInsets.safeDrawing,
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            TopAppBar(
                                title = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        BattleBoxCrest(size = 34.dp)
                                        Text(
                                            text = gymConfig.gymName.uppercase(),
                                            style = MaterialTheme.typography.headlineMedium
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        },
                        floatingActionButton = {
                            FloatingActionButton(
                                onClick = { showConcierge = true },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.testTag("concierge_fab")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "Open Battle Box AI Concierge")
                            }
                        },
                        bottomBar = {
                            if (!useRail) {
                                NavigationBar {
                                    tabs.forEach { tab ->
                                        val selected = currentRoute == tab.route
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { navigateTopLevel(tab.route) },
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                    contentDescription = tab.label
                                                )
                                            },
                                            label = { Text(tab.label) },
                                            modifier = Modifier.testTag("nav_${tab.route}")
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (useRail) {
                                NavigationRail(
                                    modifier = Modifier.fillMaxHeight()
                                ) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    tabs.forEach { tab ->
                                        val selected = currentRoute == tab.route
                                        NavigationRailItem(
                                            selected = selected,
                                            onClick = { navigateTopLevel(tab.route) },
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                    contentDescription = tab.label
                                                )
                                            },
                                            label = { Text(tab.label) },
                                            modifier = Modifier.testTag("rail_${tab.route}")
                                        )
                                    }
                                }
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                NavHost(
                                    navController = navController,
                                    startDestination = BattleBoxRoute.ROLE_ROUTER
                                ) {
                                    composable(BattleBoxRoute.ROLE_ROUTER) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                CircularProgressIndicator()
                                                Text(
                                                    text = "Routing to ${resolvedDestination.dashboardLabel}...",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    composable(BattleBoxRoute.MEMBER_HOME) {
                                        HomeDashboardScreen(
                                            userProfile = effectiveProfile,
                                            gymConfig = gymConfig,
                                            gymPublicStatus = gymPublicStatus,
                                            activeSession = activeSession,
                                            liveRoster = liveRoster,
                                            announcements = announcements,
                                            deviceFix = deviceFix,
                                            onOpenPassTab = { navigateTopLevel(BattleBoxRoute.PASS) },
                                            onCheckOutActive = { attId -> viewModel.checkOut(attId) },
                                            onOpenConcierge = { showConcierge = true },
                                            onOpenAdminRoster = { navigateTopLevel(BattleBoxRoute.ADMIN_HOME) }
                                        )
                                    }

                                    composable(BattleBoxRoute.TRAINER_HOME) {
                                        TrainerRosterScreen(
                                            currentUser = effectiveProfile,
                                            assignedMembers = assignedMembers,
                                            liveRoster = liveRoster,
                                            gymConfig = gymConfig,
                                            gymPublicStatus = gymPublicStatus,
                                            announcements = announcements,
                                            isBusy = isBusy,
                                            onOpenScannerTab = { navigateTopLevel(BattleBoxRoute.SCANNER) },
                                            onOpenPassTab = { navigateTopLevel(BattleBoxRoute.PASS) },
                                            onCheckOutSession = { attId, name ->
                                                viewModel.checkOut(attId, isAdminAction = false, memberName = name)
                                            },
                                            onSweepExpired = { records ->
                                                viewModel.sweepExpired(records, isAdmin = false)
                                            }
                                        )
                                    }

                                    composable(BattleBoxRoute.SCANNER) {
                                        QrAttendanceScannerScreen(
                                            currentUser = effectiveProfile,
                                            gymConfig = gymConfig,
                                            gymPublicStatus = gymPublicStatus,
                                            liveRoster = liveRoster,
                                            recentRejections = rejections,
                                            deviceFix = deviceFix,
                                            lastScanOutcome = lastScanOutcome,
                                            isBusy = isBusy,
                                            onRefreshFix = { deviceFix = it },
                                            onMemberQrScanned = { payload, fix ->
                                                if (effectiveProfile.role == UserProfile.ROLE_ADMIN) {
                                                    viewModel.performFrontDeskScan(payload, fix)
                                                } else {
                                                    viewModel.performTrainerQrScan(payload, fix)
                                                }
                                            },
                                            onCheckOutSession = { attId, name ->
                                                viewModel.checkOut(
                                                    attendanceId = attId,
                                                    isAdminAction = effectiveProfile.role == UserProfile.ROLE_ADMIN,
                                                    memberName = name
                                                )
                                            },
                                            onSweepExpired = { records ->
                                                viewModel.sweepExpired(
                                                    records = records,
                                                    isAdmin = effectiveProfile.role == UserProfile.ROLE_ADMIN
                                                )
                                            },
                                            onDismissOutcome = { viewModel.clearLastScanOutcome() }
                                        )
                                    }

                                    composable(BattleBoxRoute.ADMIN_HOME) {
                                        AdminConsoleScreen(
                                            gymConfig = gymConfig,
                                            gymPublicStatus = gymPublicStatus,
                                            liveRoster = liveRoster,
                                            allUsers = allUsers,
                                            auditLogs = auditLogs,
                                            rejections = rejections,
                                            announcements = announcements,
                                            paymentRequests = allPaymentRequests,
                                            deliveryLogs = deliveryLogs,
                                            deviceFix = deviceFix,
                                            monthlyExportState = monthlyExportState,
                                            isBusy = isBusy,
                                            onOpenScannerTab = { navigateTopLevel(BattleBoxRoute.SCANNER) },
                                            onFrontDeskScan = { payload, fix ->
                                                viewModel.performFrontDeskScan(payload, fix)
                                            },
                                            onAdminCheckOut = { attId, name ->
                                                viewModel.checkOut(attId, isAdminAction = true, memberName = name)
                                            },
                                            onSweepExpired = { records ->
                                                viewModel.sweepExpired(records, isAdmin = true)
                                            },
                                            onUpdateUser = { userToEdit, role, plan, status, trainerId ->
                                                viewModel.adminUpdateUser(userToEdit, role, plan, status, trainerId)
                                            },
                                            onUpdateGymConfig = { updated ->
                                                viewModel.updateGymConfig(updated)
                                            },
                                            onFetchMonthlyCsv = { year, month ->
                                                viewModel.loadMonthlyAttendanceForExport(year, month)
                                            },
                                            onPostAnnouncement = { type, title, msg, hours, push, wa ->
                                                viewModel.adminPostAnnouncement(type, title, msg, hours, push, wa)
                                            },
                                            onClearAnnouncement = { annId ->
                                                viewModel.adminDeactivateAnnouncement(annId)
                                            },
                                            onSeedDefaultPlans = { viewModel.adminSeedDefaultPlans() },
                                            onReconcileSandboxPayment = { req ->
                                                viewModel.adminReconcileSandboxPayment(req)
                                            },
                                            onCopyMessage = { msg -> viewModel.showBanner(msg) }
                                        )
                                    }

                                    composable(BattleBoxRoute.PASS) {
                                        PassAndCheckInScreen(
                                            userProfile = effectiveProfile,
                                            gymConfig = gymConfig,
                                            deviceFix = deviceFix,
                                            myAttendance = myAttendance,
                                            rejections = rejections,
                                            isBusy = isBusy,
                                            onRefreshFix = { deviceFix = it },
                                            onKioskCheckIn = { payload, fix ->
                                                viewModel.performKioskCheckIn(payload, effectiveProfile, fix)
                                            },
                                            onCheckOut = { attId -> viewModel.checkOut(attId) },
                                            onCopyMessage = { msg -> viewModel.showBanner(msg) }
                                        )
                                    }

                                    composable(BattleBoxRoute.PLANS) {
                                        PlansAndBillingScreen(
                                            userProfile = effectiveProfile,
                                            plans = plans,
                                            subscriptions = mySubscriptions,
                                            paymentRequests = myPaymentRequests,
                                            payments = myPayments,
                                            notificationPref = notificationPref,
                                            isBusy = isBusy,
                                            onCreateCheckoutIntent = { plan, mode, upiApp ->
                                                viewModel.createCheckoutIntent(effectiveProfile, plan, mode, upiApp)
                                            },
                                            onCancelAutopay = { subId ->
                                                viewModel.requestAutopayCancellation(subId)
                                            },
                                            onSaveNotificationPreference = { pref ->
                                                viewModel.saveNotificationPreference(pref)
                                            },
                                            onShowMessage = { msg -> viewModel.showBanner(msg) }
                                        )
                                    }

                                    composable(BattleBoxRoute.PROFILE) {
                                        ProfileAndLegalScreen(
                                            userProfile = effectiveProfile,
                                            gymConfig = gymConfig,
                                            myAttendance = myAttendance,
                                            mySubscriptions = mySubscriptions,
                                            consentReceipts = myConsents,
                                            isDarkTheme = isDarkTheme,
                                            onToggleTheme = { viewModel.toggleDarkTheme() },
                                            onOpenPlansTab = { navigateTopLevel(BattleBoxRoute.PLANS) },
                                            onOpenPassTab = { navigateTopLevel(BattleBoxRoute.PASS) },
                                            onSignOut = onSignOut
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (showConcierge) {
                        ConciergeSheet(
                            messages = chatMessages,
                            isBusy = isConciergeBusy,
                            onSendMessage = { viewModel.sendConciergeMessage(it) },
                            onDismiss = { showConcierge = false }
                        )
                    }
                }
            }
        }
    }
}
