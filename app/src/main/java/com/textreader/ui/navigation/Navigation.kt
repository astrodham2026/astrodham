package com.textreader.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.textreader.ui.screens.DocumentEditorScreen
import com.textreader.ui.screens.DocumentListScreen
import com.textreader.ui.screens.DocumentReaderScreen
import com.textreader.viewmodel.DocumentViewModel

sealed class Screen(val route: String) {
    data object DocumentList : Screen("document_list")
    data object DocumentEditor : Screen("document_editor?documentId={documentId}") {
        fun createRoute(documentId: String? = null): String {
            return if (documentId != null) {
                "document_editor?documentId=$documentId"
            } else {
                "document_editor"
            }
        }
    }
    data object DocumentReader : Screen("document_reader/{documentId}") {
        fun createRoute(documentId: String): String = "document_reader/$documentId"
    }
}

@Composable
fun TextReaderNavHost() {
    val navController = rememberNavController()
    val viewModel: DocumentViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Screen.DocumentList.route
    ) {
        composable(Screen.DocumentList.route) {
            DocumentListScreen(
                viewModel = viewModel,
                onNavigateToEditor = { documentId ->
                    navController.navigate(Screen.DocumentEditor.createRoute(documentId))
                },
                onNavigateToReader = { documentId ->
                    navController.navigate(Screen.DocumentReader.createRoute(documentId))
                }
            )
        }

        composable(
            route = Screen.DocumentEditor.route,
            arguments = listOf(
                navArgument("documentId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getString("documentId")
            DocumentEditorScreen(
                viewModel = viewModel,
                documentId = documentId,
                onNavigateBack = {
                    viewModel.resetEditorState()
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.DocumentReader.route,
            arguments = listOf(
                navArgument("documentId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getString("documentId") ?: return@composable
            DocumentReaderScreen(
                viewModel = viewModel,
                documentId = documentId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditor = { id ->
                    navController.navigate(Screen.DocumentEditor.createRoute(id))
                }
            )
        }
    }
}
