package cn.svecri.feedive.ui.main

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cn.svecri.feedive.R
import cn.svecri.feedive.ui.theme.FeediveTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FeediveTheme {
                MainScreenView()
            }
        }
    }
}

sealed class BottomNavItem(
    val title: String,
    val icon: Int,
    val screenRoute: String,
    val optParameters: String = "",
    val defaultRoute: String = ""
) {

    object InfoFlow :
        BottomNavItem(
            "Flow",
            R.drawable.ic_feeds,
            "info_flow",
            "/{type}?detail={detail}",
            defaultRoute = "info_flow/all"
        )

    object SourceManage : BottomNavItem(
        "Source",
        R.drawable.ic_source_manage,
        "/source_manage",
        defaultRoute = "/source_manage"
    )

}

@Composable
fun MainScreenView() {
    val navController = rememberNavController()
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()
    // A surface container using the 'background' color from the theme
    Scaffold(
        scaffoldState = scaffoldState,
        backgroundColor = MaterialTheme.colors.background,
        bottomBar = { BottomNavigationBar(navController, scaffoldState.drawerState) },
        drawerContent = {
            NavigationDrawer(navController, afterNav = {
                scope.launch {
                    scaffoldState.drawerState.close()
                }
            })
        },
    ) {
        NavigationGraph(navController, scaffoldState.snackbarHostState, it.calculateBottomPadding())
    }
}

@Composable
fun NavigationGraph(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState, bottomPadding: Dp
) {
    NavHost(
        navController,
        startDestination = BottomNavItem.InfoFlow.screenRoute + "/{type}?detail={detail}",
        modifier = Modifier.padding(0.dp, 0.dp, 0.dp, bottomPadding),
    ) {
        Log.d(
            "MainActivity",
            BottomNavItem.InfoFlow.screenRoute + BottomNavItem.InfoFlow.optParameters,
        )
        composable(
            BottomNavItem.InfoFlow.screenRoute + BottomNavItem.InfoFlow.optParameters,
            arguments = listOf(
                navArgument("type") {
                    type = NavType.StringType
                    defaultValue = "all"
                },
                navArgument("detail") { defaultValue = "0" })
        ) {
            InfoFlowView(
                navController = navController,
                type = ArticleFetchType.buildFromArgs(
                    it.arguments?.getString("type"),
                    it.arguments?.getString("detail"),
                ),
                snackbarHostState = snackbarHostState,
            )
        }
        composable("article?link={link}") { backStackEntry ->
            backStackEntry.arguments?.getString("link")
                ?.let { ArticleView(it, navController = navController) }
        }
        composable(BottomNavItem.SourceManage.screenRoute) {
            ResourceManagerView(navController = navController)
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController, drawerState: DrawerState) {
    val scope = rememberCoroutineScope()
    val items = listOf(BottomNavItem.InfoFlow, BottomNavItem.SourceManage)
    BottomNavigation {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        BottomNavigationItem(
            icon = {
                Icon(
                    modifier = Modifier.height(24.dp),
                    painter = painterResource(id = R.drawable.ic_menu),
                    contentDescription = "Menu"
                )
            },
            label = { Text(text = "Feeds") },
            selected = false,
            onClick = { scope.launch { drawerState.open() } })
        items.forEach { item ->
            BottomNavigationItem(
                icon = {
                    Icon(
                        modifier = Modifier.height(24.dp),
                        painter = painterResource(id = item.icon),
                        contentDescription = item.title
                    )
                },
                label = { Text(text = item.title) },
                selected = currentDestination?.hierarchy?.any {
                    it.route?.split("/")?.get(0) == item.screenRoute
                } == true,
                onClick = {
                    navController.navigate(item.defaultRoute) {
                        navController.graph.startDestinationRoute?.let { screenRoute ->
                            popUpTo(screenRoute) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    FeediveTheme {
        MainScreenView()
    }
}