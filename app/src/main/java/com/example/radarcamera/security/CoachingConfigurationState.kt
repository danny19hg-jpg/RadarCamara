package com.example.radarcamera.security

sealed interface CoachingConfigurationState {
    data object Loading : CoachingConfigurationState
    data object FirstSetup : CoachingConfigurationState
    data object PinRequired : CoachingConfigurationState
    data class RecoveryRequired(val mode: RecoveryMode) : CoachingConfigurationState
}

enum class RecoveryMode {
    VerifyExistingPinThenCreateProfile,
    CreatePinForExistingProfile
}

object CoachingConfigurationResolver {
    fun resolve(
        profileLoadCompleted: Boolean,
        profilePresent: Boolean?,
        pinCheckCompleted: Boolean,
        pinPresent: Boolean?
    ): CoachingConfigurationState {
        if (!profileLoadCompleted || !pinCheckCompleted) return CoachingConfigurationState.Loading
        return when {
            profilePresent == false && pinPresent == false -> CoachingConfigurationState.FirstSetup
            profilePresent == true && pinPresent == true -> CoachingConfigurationState.PinRequired
            profilePresent == false && pinPresent == true -> CoachingConfigurationState.RecoveryRequired(
                RecoveryMode.VerifyExistingPinThenCreateProfile
            )
            profilePresent == true && pinPresent == false -> CoachingConfigurationState.RecoveryRequired(
                RecoveryMode.CreatePinForExistingProfile
            )
            else -> CoachingConfigurationState.Loading
        }
    }
}

internal val stageOneVisibleTexts = listOf(
    "Acceso Coaching",
    "Comprobando configuraci\u00f3n local\u2026",
    "Configurar Coaching",
    "Crea el perfil local y el PIN que proteger\u00e1 Coaching en este dispositivo.",
    "Recuperar acceso Coaching"
)
