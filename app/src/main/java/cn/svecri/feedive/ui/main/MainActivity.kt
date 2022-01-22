package cn.svecri.feedive.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cn.svecri.feedive.R
import cn.svecri.feedive.ui.theme.FeediveTheme

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

sealed class BottomNavItem(var title: String, var icon: Int, var screenRoute: String) {

    object InfoFlow : BottomNavItem("Flow", R.drawable.ic_feeds, "info_flow")
}

@Composable
fun MainScreenView() {
    val navController = rememberNavController()
    // A surface container using the 'background' color from the theme
    Scaffold(
        backgroundColor = MaterialTheme.colors.background,
        bottomBar = { BottomNavigationBar(navController) }
    ) {
        NavigationGraph(navController)
    }
}

@Composable
fun NavigationGraph(navController: NavHostController) {
    NavHost(navController, startDestination = BottomNavItem.InfoFlow.screenRoute) {
        composable(BottomNavItem.InfoFlow.screenRoute) {
            InfoFlowView()
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(BottomNavItem.InfoFlow)
    BottomNavigation {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        items.forEach { item ->
            BottomNavigationItem(
                icon = {
                    Icon(
                        painter = painterResource(id = item.icon),
                        contentDescription = item.title
                    )
                },
                label = { Text(text = item.title) },
                selected = currentDestination?.hierarchy?.any { it.route == item.screenRoute } == true,
                onClick = {
                    navController.navigate(item.screenRoute) {
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