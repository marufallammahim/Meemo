package com.marufallammahim.meemo.assistant.planning

import com.marufallammahim.meemo.actions.ActionResult
import com.marufallammahim.meemo.actions.MeemoAction
import com.marufallammahim.meemo.apps.AppResolutionResult
import com.marufallammahim.meemo.apps.PackageResolver
import com.marufallammahim.meemo.assistant.entity.ExtractedEntity
import com.marufallammahim.meemo.assistant.intent.MeemoIntent
import com.marufallammahim.meemo.domain.model.InstalledAppInfo
import com.marufallammahim.meemo.domain.repository.MeemoRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ActionPlannerTest {
    private val packageResolver = mockk<PackageResolver>()
    private val repository = mockk<MeemoRepository>()
    private val planner = ActionPlanner(packageResolver, repository)

    @Test
    fun `plans an exact application match`() = runTest {
        val app = InstalledAppInfo(
            packageName = "com.example.browser",
            appLabel = "Browser"
        )
        every { packageResolver.resolveAppByName("Browser") } returns
            AppResolutionResult.ExactOrSingleMatch(app)

        val outcome = planner.plan(
            MeemoIntent.OPEN_APPLICATION,
            ExtractedEntity.AppQuery("Browser"),
            preferredBrowserPackage = null
        )

        assertEquals(
            ActionPlanOutcome.Planned(
                MeemoAction.OpenApplication(app.packageName, app.appLabel)
            ),
            outcome
        )
    }

    @Test
    fun `keeps ambiguous application matches explicit`() = runTest {
        val candidates = listOf(
            InstalledAppInfo("com.example.one", "Browser One"),
            InstalledAppInfo("com.example.two", "Browser Two")
        )
        every { packageResolver.resolveAppByName("Browser") } returns
            AppResolutionResult.AmbiguousMatches(candidates)

        val outcome = planner.plan(
            MeemoIntent.OPEN_APPLICATION,
            ExtractedEntity.AppQuery("Browser"),
            preferredBrowserPackage = null
        )

        assertEquals(
            ActionPlanOutcome.ImmediateResult(
                ActionResult.Ambiguous(
                    query = "Browser",
                    candidateApps = candidates
                )
            ),
            outcome
        )
    }
}
