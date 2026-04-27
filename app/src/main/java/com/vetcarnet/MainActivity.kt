package com.vetcarnet

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.vetcarnet.ui.screens.detail.AnimalDetailScreen
import com.vetcarnet.ui.screens.form.AnimalFormScreen
import com.vetcarnet.ui.screens.list.AnimalListScreen
import com.vetcarnet.ui.theme.VetCarnetTheme
import com.vetcarnet.worker.VaccinationCheckWorker

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        VaccinationCheckWorker.createNotificationChannel(this)
        VaccinationCheckWorker.schedule(this)

        setContent {
            VetCarnetTheme {
                RequestNotificationPermission()
                VetCarnetMainApp()
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        LaunchedEffect(Unit) {
            if (!permissionState.status.isGranted) {
                permissionState.launchPermissionRequest()
            }
        }
    }
}

//  Routes

private object Routes {
    const val LIST   = "animals"
    const val DETAIL = "animals/{animalId}"
    const val FORM   = "animals/form?animalId={animalId}"

    fun detail(id: String) = "animals/$id"
    fun form(id: String? = null) =
        if (id != null) "animals/form?animalId=$id" else "animals/form"
}

//  Navigation Host

@Composable
fun VetCarnetMainApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.LIST) {

        // Liste
        composable(Routes.LIST) {
            AnimalListScreen(
                onNavigateToForm   = { navController.navigate(Routes.form(it)) },
                onNavigateToDetail = { navController.navigate(Routes.detail(it)) }
            )
        }

        // Détail
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("animalId") { type = NavType.StringType })
        ) { back ->
            val id = back.arguments?.getString("animalId") ?: return@composable
            AnimalDetailScreen(
                animalId = id,
                onNavigateBack   = { navController.popBackStack() },
                onNavigateToEdit = { navController.navigate(Routes.form(it)) }
            )
        }

        // Formulaire
        composable(
            route = Routes.FORM,
            arguments = listOf(
                navArgument("animalId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { back ->
            AnimalFormScreen(
                animalId       = back.arguments?.getString("animalId"),
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
