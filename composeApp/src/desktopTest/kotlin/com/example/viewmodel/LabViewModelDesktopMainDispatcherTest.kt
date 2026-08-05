package com.example.viewmodel

import com.example.data.auth.MockAuthRepositoryImpl
import com.example.data.auth.StaffRole
import com.example.environment.AppEnvironment
import kotlin.test.Test
import kotlin.test.assertNull

/**
 * Regresion Desktop-only: usa el constructor real de produccion de
 * [LabViewModel] (sin inyectar un coroutineScope de prueba), por lo que
 * internamente construye MainScope() igual que Desktop/Android en runtime.
 * Sin un modulo que provea Dispatchers.Main en el classpath de Desktop
 * (kotlinx-coroutines-swing), authScope.launch lanza sincronamente una
 * IllegalStateException al primer signInDemo() -- en la app real el mensaje
 * es "Module with the Main dispatcher is missing"; bajo el test runner,
 * kotlinx-coroutines-test intercepta la resolucion de Dispatchers.Main y
 * lanza un IllegalStateException equivalente con un mensaje propio
 * ("Dispatchers.Main was accessed when the platform dispatcher was
 * absent...") cuando tampoco hay un dispatcher de plataforma real. Por eso
 * la asercion no depende del texto exacto: cualquier excepcion aqui indica
 * que Desktop carece de un Main dispatcher real.
 */
class LabViewModelDesktopMainDispatcherTest {

    @Test
    fun signInDemoDoesNotFailBecauseTheMainDispatcherIsMissingOnDesktop() {
        val vm = LabViewModel(
            appEnvironment = AppEnvironment.demoLocal("test"),
            authRepository = MockAuthRepositoryImpl()
        )

        val failure = runCatching { vm.signInDemo(StaffRole.SECRETARIA) }.exceptionOrNull()

        assertNull(failure, "signInDemo() fallo por ausencia de un Main dispatcher real en Desktop: $failure")
    }
}
