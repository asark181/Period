package com.nuanyue.app

import com.nuanyue.app.update.CheckResult
import com.nuanyue.app.update.ManualCheck
import com.nuanyue.app.update.manualCheckLog
import com.nuanyue.app.update.updateRowModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckFeedbackTest {
    @Test fun upToDateReplacesTheVersionHint() {
        val row = updateRowModel(ManualCheck.UpToDate("1.9.0"), "1.9.0", newerName = null)
        assertEquals("已是最新 · 1.9.0", row.hint)
        assertFalse(row.error)
        assertFalse(row.spinner)
        assertTrue(row.clickable)
    }

    @Test fun errorOffersARetryAndCheckingBlocksAnotherTap() {
        val failed = updateRowModel(ManualCheck.Failed, "1.9.0", newerName = null)
        assertEquals("检查失败，点此重试", failed.hint)
        assertTrue(failed.error)
        assertTrue(failed.clickable)

        val checking = updateRowModel(ManualCheck.Checking, "1.9.0", newerName = null)
        assertNull(checking.hint)
        assertTrue(checking.spinner)
        assertFalse(checking.clickable)
    }

    @Test fun idleKeepsTheCurrentOrNewerLabel() {
        assertEquals("当前 1.9.0", updateRowModel(ManualCheck.Idle, "1.9.0", null).hint)
        assertEquals("有新版本 1.9.1", updateRowModel(ManualCheck.Idle, "1.9.0", "1.9.1").hint)
    }

    @Test fun manualCheckLogNamesEachOutcome() {
        assertEquals("manual check: remote=11 local=11 -> up-to-date", manualCheckLog(11, 11, CheckResult.Current))
        assertEquals("manual check: remote=? local=11 -> error", manualCheckLog(null, 11, CheckResult.Failed))
        assertEquals("manual check: remote=12 local=11 -> update", manualCheckLog(12, 11, CheckResult.Newer))
    }
}
