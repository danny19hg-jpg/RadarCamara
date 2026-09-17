package com.example.radarcamera.security

import org.junit.Assert.assertEquals
import org.junit.Test

class CoachingConfigurationStateTest {
    @Test
    fun pinPresentWhileProfileIsLoadingRemainsLoading() {
        assertEquals(
            CoachingConfigurationState.Loading,
            CoachingConfigurationResolver.resolve(profileLoadCompleted = false, profilePresent = null, pinCheckCompleted = true, pinPresent = true)
        )
    }

    @Test
    fun completedProfileAfterPinPresentMovesDirectlyToPinRequired() {
        assertEquals(
            CoachingConfigurationState.PinRequired,
            CoachingConfigurationResolver.resolve(profileLoadCompleted = true, profilePresent = true, pinCheckCompleted = true, pinPresent = true)
        )
    }

    @Test
    fun profilePresentWhilePinCheckIsPendingRemainsLoading() {
        assertEquals(
            CoachingConfigurationState.Loading,
            CoachingConfigurationResolver.resolve(profileLoadCompleted = true, profilePresent = true, pinCheckCompleted = false, pinPresent = null)
        )
    }

    @Test
    fun bothAbsentAfterBothChecksCompleteRequiresFirstSetup() {
        assertEquals(
            CoachingConfigurationState.FirstSetup,
            CoachingConfigurationResolver.resolve(profileLoadCompleted = true, profilePresent = false, pinCheckCompleted = true, pinPresent = false)
        )
    }

    @Test
    fun bothPresentRequiresPin() {
        assertEquals(
            CoachingConfigurationState.PinRequired,
            CoachingConfigurationResolver.resolve(profileLoadCompleted = true, profilePresent = true, pinCheckCompleted = true, pinPresent = true)
        )
    }

    @Test
    fun onlyPinRequiresVerificationBeforeCreatingProfile() {
        assertEquals(
            CoachingConfigurationState.RecoveryRequired(RecoveryMode.VerifyExistingPinThenCreateProfile),
            CoachingConfigurationResolver.resolve(profileLoadCompleted = true, profilePresent = false, pinCheckCompleted = true, pinPresent = true)
        )
    }

    @Test
    fun onlyProfileRequiresNewPinWithoutReplacingProfile() {
        assertEquals(
            CoachingConfigurationState.RecoveryRequired(RecoveryMode.CreatePinForExistingProfile),
            CoachingConfigurationResolver.resolve(profileLoadCompleted = true, profilePresent = true, pinCheckCompleted = true, pinPresent = false)
        )
    }

    @Test
    fun recreatedGateResolvesFromCompletedSnapshotInsteadOfFreezingLoading() {
        val first = CoachingConfigurationResolver.resolve(false, null, true, true)
        val recreated = CoachingConfigurationResolver.resolve(true, true, true, true)

        assertEquals(CoachingConfigurationState.Loading, first)
        assertEquals(CoachingConfigurationState.PinRequired, recreated)
    }

    @Test
    fun lifecycleChangesDoNotTurnIncompleteChecksIntoAbsence() {
        assertEquals(
            CoachingConfigurationState.Loading,
            CoachingConfigurationResolver.resolve(profileLoadCompleted = false, profilePresent = null, pinCheckCompleted = false, pinPresent = null)
        )
    }

    @Test
    fun visibleStageOneTextContainsNoMojibakeMarkers() {
        stageOneVisibleTexts.forEach { text ->
            assertEquals(-1, text.indexOf('Ã'))
            assertEquals(-1, text.indexOf('Â'))
            assertEquals(-1, text.indexOf('�'))
        }
    }
}
