package com.example.radarcamera.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerDeletionPolicyTest {
    @Test fun allowsPermanentDeletionWhenThereIsNoHistory() {
        assertEquals(PlayerDeletionEligibility.Allowed, PlayerDeletionPolicy.evaluate(associatedHistoryCount = 0))
    }

    @Test fun blocksPermanentDeletionWhenHistoryExists() {
        assertEquals(PlayerDeletionEligibility.HasHistory, PlayerDeletionPolicy.evaluate(associatedHistoryCount = 1))
    }
}
