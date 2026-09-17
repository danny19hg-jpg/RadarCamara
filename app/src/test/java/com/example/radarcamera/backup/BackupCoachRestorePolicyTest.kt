package com.example.radarcamera.backup

import com.example.radarcamera.data.local.CoachEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupCoachRestorePolicyTest {
    @Test fun emptyBackupCoachesPreservesExistingLocalProfile() {
        val existing = CoachEntity("coach", "Entrenador", null, null, 1, 1)
        assertEquals(listOf(existing), BackupCoachRestorePolicy.coachesToRestore(emptyList(), existing))
    }

    @Test fun backupCoachRemainsAuthoritativeWhenPresent() {
        val existing = CoachEntity("old", "Anterior", null, null, 1, 1)
        val restored = CoachEntity("new", "Respaldo", null, null, 2, 2)
        assertEquals(listOf(restored), BackupCoachRestorePolicy.coachesToRestore(listOf(restored), existing))
    }
}
