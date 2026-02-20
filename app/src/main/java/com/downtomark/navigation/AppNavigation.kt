package com.downtomark.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.downtomark.ui.graph.GraphScreen
import com.downtomark.ui.home.HomeScreen
import com.downtomark.ui.reader.ReaderScreen
import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val HOME = "home"
    const val READER = "reader/{fileUri}"
    const val GRAPH = "graph"

    fun reader(fileUri: String): String {
        val encoded = URLEncoder.encode(fileUri, "UTF-8")
        return "reader/$encoded"
    }
}

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onFileSelected = { uri ->
                    navController.navigate(Routes.reader(uri))
                },
                onOpenGraph = {
                    navController.navigate(Routes.GRAPH)
                }
            )
        }
        composable(
            route = Routes.READER,
            arguments = listOf(navArgument("fileUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("fileUri") ?: return@composable
            val fileUri = URLDecoder.decode(encodedUri, "UTF-8")
            ReaderScreen(
                fileUri = fileUri,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.GRAPH) {
            GraphScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToFile = { uri, blockIndex ->
                    navController.navigate(Routes.reader(uri))
                }
            )
        }
    }
}
