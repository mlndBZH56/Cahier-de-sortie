package com.aca56.cahiersortiecodex.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.aca56.cahiersortiecodex.navigation.AppDestination
import com.aca56.cahiersortiecodex.navigation.allDestinations
import com.aca56.cahiersortiecodex.navigation.topLevelDestinations
import com.aca56.cahiersortiecodex.ui.components.rememberDismissKeyboardAction
import kotlinx.coroutines.launch

@Composable
fun CahierSortieScaffold(
    navController: NavHostController,
    content: @Composable (PaddingValues) -> Unit,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val usePermanentDrawer = maxWidth >= 840.dp

        if (usePermanentDrawer) {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet {
                        AppNavigationDrawer(navController = navController)
                    }
                },
                modifier = Modifier.fillMaxSize(),
            ) {
                AppScaffold(
                    navController = navController,
                    showMenuButton = false,
                    onMenuClick = {},
                    content = content,
                )
            }
        } else {
            ModalNavigationDrawer(
                drawerContent = {
                    ModalDrawerSheet {
                        AppNavigationDrawer(
                            navController = navController,
                            onItemClick = {
                                coroutineScope.launch {
                                    drawerState.close()
                                }
                            },
                        )
                    }
                },
                drawerState = drawerState,
                modifier = Modifier.fillMaxSize(),
            ) {
                AppScaffold(
                    navController = navController,
                    showMenuButton = true,
                    onMenuClick = {
                        coroutineScope.launch {
                            drawerState.open()
                        }
                    },
                    content = content,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppScaffold(
    navController: NavHostController,
    showMenuButton: Boolean,
    onMenuClick: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val dismissKeyboard = rememberDismissKeyboardAction()
    val currentBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentDestination = currentBackStackEntry?.destination
    val currentAppDestination = allDestinations.firstOrNull { destination ->
        currentDestination?.hierarchy?.any { it.route == destination.route } == true
    } ?: topLevelDestinations.first()
    val isTopLevelDestination = topLevelDestinations.any { destination ->
        currentDestination?.hierarchy?.any { it.route == destination.route } == true
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
                    ) {
                        Text(currentAppDestination.title)
                    }
                },
                navigationIcon = {
                    if (!isTopLevelDestination && navController.previousBackStackEntry != null) {
                        IconButton(
                            onClick = {
                                dismissKeyboard()
                                navController.navigateUp()
                            },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Retour",
                            )
                        }
                    } else if (showMenuButton) {
                        IconButton(
                            onClick = {
                                dismissKeyboard()
                                onMenuClick()
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Menu",
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                content(PaddingValues(0.dp))
            }
        },
    )
}

@Composable
private fun AppNavigationDrawer(
    navController: NavHostController,
    onItemClick: (() -> Unit)? = null,
) {
    val dismissKeyboard = rememberDismissKeyboardAction()
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 20.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Navigation",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )

            topLevelDestinations.forEach { destination ->
                val isSelected = currentDestination?.hierarchy?.any { destinationInHierarchy ->
                    destinationInHierarchy.route == destination.route ||
                        (
                            destination == AppDestination.History &&
                                destinationInHierarchy.route == AppDestination.HistoryDetail.route
                            )
                } == true
                NavigationDrawerItem(
                    label = { Text(destination.title) },
                    selected = isSelected,
                    colors = NavigationDrawerItemDefaults.colors(),
                    onClick = {
                        dismissKeyboard()
                        navController.navigate(
                            when (destination) {
                                AppDestination.History -> AppDestination.History.createRoute()
                                AppDestination.Remarks -> AppDestination.Remarks.createRoute()
                                else -> destination.route
                            },
                        ) {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = false
                            }
                            launchSingleTop = true
                            restoreState = false
                        }
                        onItemClick?.invoke()
                    },
                )
            }
        }
    }
}
