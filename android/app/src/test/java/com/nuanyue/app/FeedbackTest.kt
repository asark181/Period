package com.nuanyue.app

import com.nuanyue.app.ui.theme.FeedbackColors
import com.nuanyue.app.ui.theme.contrastRatio
import com.nuanyue.app.ui.theme.relativeLuminance
import com.nuanyue.app.ui.theme.toastHoldMs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackTest {
    @Test fun pillIsALightSurface() {
        assertTrue(relativeLuminance(FeedbackColors.BACKGROUND) > 0.9)
    }

    @Test fun inkOnThePillMeetsBodyTextContrast() {
        assertTrue(contrastRatio(FeedbackColors.BACKGROUND, FeedbackColors.INK) >= 7.0)
    }

    @Test fun theOldSnackbarPairingWasUnreadable() {
        // Material inverseSurface is near-black. Palette ink on it was the 1.3 bug.
        assertTrue(contrastRatio(FeedbackColors.MATERIAL_INVERSE_SURFACE, FeedbackColors.INK) < 2.0)
    }

    @Test fun toastStaysUpLongEnoughToRead() {
        assertEquals(2400L, toastHoldMs("同步完成"))
        val longer = toastHoldMs("邀请码生成好啦，发给他就可以一起看了")
        assertTrue(longer > toastHoldMs("同步完成"))
        assertTrue(longer <= 4200L)
        assertEquals(4200L, toastHoldMs("错".repeat(80)))
    }
}
