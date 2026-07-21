package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.example.auth.SaseAuthEnvironment
import com.example.auth.SessionRepositoryProvision
import com.example.ui.auth.LoginScreen
import com.example.viewmodel.LabViewModel
import com.example.viewmodel.StaffLoginViewModel

/**
 * Punto de entrada común de la aplicación. Resuelve el backend de sesión desde
 * el entorno (MOCK por defecto; SUPABASE solo con configuración explícita) y,
 * si no puede proveerse, muestra el acceso deshabilitado con el motivo — nunca
 * cae en silencio al backend demo.
 */
@Composable
fun SaseApp(
    provision: SessionRepositoryProvision = remember { SaseAuthEnvironment.provision() }
) {
    val loginViewModel = remember(provision) { StaffLoginViewModel(provision) }
    val sessionRepository = loginViewModel.sessionRepository

    if (sessionRepository == null) {
        LoginScreen(viewModel = loginViewModel)
        return
    }

    val viewModel = remember(sessionRepository) {
        LabViewModel(sessionRepository = sessionRepository)
    }

    // Reanuda una sesión previa del backend, si la hubiera. Si no hay ninguna,
    // el gate permanece cerrado.
    LaunchedEffect(loginViewModel) { loginViewModel.restoreSession() }

    SaseAppContent(viewModel = viewModel, loginViewModel = loginViewModel)
}
