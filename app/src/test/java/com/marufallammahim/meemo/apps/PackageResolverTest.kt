package com.marufallammahim.meemo.apps

import android.content.Context
import com.marufallammahim.meemo.domain.model.InstalledAppInfo
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class PackageResolverTest {

    private val resolver = PackageResolver(mockk<Context>(relaxed = true))

    @Test
    fun \`matches app names consistently under Turkish default locale\`() {
        val previousLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            val apps = listOf(
                InstalledAppInfo("com.example.istanbul", "Istanbul")
            )

            val result = resolver.resolveAppByName("istanbul", apps)

            assertEquals(
                AppResolutionResult.ExactOrSingleMatch(apps.single()),
                result
            )
        } finally {
            Locale.setDefault(previousLocale)
        }
    }

    @Test
    fun \`matches app label tokens separated by repeated whitespace and punctuation\`() {
        val app = InstalledAppInfo("com.example.music", "My   Music_Player")
        val result = resolver.resolveAppByName("music", listOf(app))

        assertEquals(AppResolutionResult.ExactOrSingleMatch(app), result)
    }
}
