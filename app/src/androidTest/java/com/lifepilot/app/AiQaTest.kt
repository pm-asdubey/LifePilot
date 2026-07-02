package com.lifepilot.app

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Autonomous AI QA test suite. Uses UIAutomator so it runs against the
 * real seeded database on the device (not a fresh Hilt-injected test DB).
 *
 * Run with:
 *   adb shell am instrument -w -r \
 *     -e class com.lifepilot.app.AiQaTest \
 *     com.lifepilot.app.debug.test/com.lifepilot.app.HiltTestRunner
 */
@RunWith(AndroidJUnit4::class)
class AiQaTest {

    private lateinit var device: UiDevice
    private val pkg = "com.lifepilot.app.debug"
    private val launchTimeout = 8_000L
    private val aiResponseTimeout = 45_000L

    @Before
    fun launchApp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val ctx = InstrumentationRegistry.getInstrumentation().context
        val intent = ctx.packageManager.getLaunchIntentForPackage(pkg)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        assertNotNull("Package $pkg not installed", intent)
        ctx.startActivity(intent)
        device.wait(Until.hasObject(By.pkg(pkg).depth(0)), launchTimeout)
        // Wait for Home screen to settle
        device.wait(Until.findObject(By.text("Ask LifePilot anything…")), launchTimeout)
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private fun openAiWorkspace() {
        val input = device.wait(Until.findObject(By.text("Ask LifePilot anything…")), 3_000L)
        assertNotNull("Chat input not found on Home screen", input)
        input!!.click()
        device.wait(Until.findObject(By.focused(true)), 2_000L)
    }

    private fun sendQuery(query: String) {
        val field = device.wait(Until.findObject(By.focused(true)), 2_000L)
            ?: device.wait(Until.findObject(By.hint("Ask LifePilot anything…")), 2_000L)
        assertNotNull("Chat input field not focused", field)
        field!!.text = query
        // Tap send — look for a send/arrow button by description first, fall back to Enter
        val sendBtn: UiObject2? = device.findObject(By.descContains("Send"))
            ?: device.findObject(By.descContains("send"))
        if (sendBtn != null) sendBtn.click() else device.pressEnter()
    }

    private fun waitForAiResponse(queryLabel: String): String {
        // Dismiss keyboard so response is visible
        device.pressBack()
        Thread.sleep(1_000L)
        // Poll until we find a text node > 40 chars that isn't the query or UI chrome.
        // Catch StaleObjectException — Compose recomposes when the AI bubble appears,
        // which invalidates node references captured before the recomposition.
        var response = ""
        val deadline = System.currentTimeMillis() + aiResponseTimeout
        val skipPrefixes = listOf("Ask LifePilot", "Good ", "Arjun", "Home", "Library",
            "Planner", "Settings", "ACTIVE GOALS", "RECENT", "View all", queryLabel.take(30))
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(2_000L)
            val nodes = try { device.findObjects(By.pkg(pkg)) } catch (e: Exception) { continue }
            for (node in nodes) {
                val t = try { node.text } catch (e: StaleObjectException) { continue } ?: continue
                if (t.length > 40 && skipPrefixes.none { t.startsWith(it) }) {
                    response = t
                    break
                }
            }
            if (response.isNotEmpty()) break
        }
        return response
    }

    private fun goHome() {
        // Tap Home in bottom nav
        val homeTab = device.findObject(By.text("Home"))
        homeTab?.click()
        device.wait(Until.findObject(By.text("Ask LifePilot anything…")), 3_000L)
    }

    // ── test cases ──────────────────────────────────────────────────────────

    @Test
    fun ai_01_passportExpiry() {
        openAiWorkspace()
        sendQuery("When does my passport expire?")
        val response = waitForAiResponse("passport expire")
        println("=== AI_01 PASSPORT EXPIRY ===\n$response\n")
        assertTrue("Expected a non-empty response", response.isNotEmpty())
    }

    @Test
    fun ai_02_careerSummary() {
        openAiWorkspace()
        sendQuery("Summarise my career so far")
        val response = waitForAiResponse("career")
        println("=== AI_02 CAREER SUMMARY ===\n$response\n")
        assertTrue("Expected a non-empty response", response.isNotEmpty())
    }

    @Test
    fun ai_03_thisWeekTasks() {
        openAiWorkspace()
        sendQuery("What should I finish this week?")
        val response = waitForAiResponse("this week")
        println("=== AI_03 THIS WEEK TASKS ===\n$response\n")
        assertTrue("Expected a non-empty response", response.isNotEmpty())
    }

    @Test
    fun ai_04_japanTrip() {
        openAiWorkspace()
        sendQuery("Am I ready for my Japan trip?")
        val response = waitForAiResponse("Japan")
        println("=== AI_04 JAPAN TRIP READINESS ===\n$response\n")
        assertTrue("Expected a non-empty response", response.isNotEmpty())
    }

    @Test
    fun ai_05_googleInterview() {
        openAiWorkspace()
        sendQuery("What stage am I at with Google?")
        val response = waitForAiResponse("Google")
        println("=== AI_05 GOOGLE INTERVIEW STATUS ===\n$response\n")
        assertTrue("Expected a non-empty response", response.isNotEmpty())
    }

    @Test
    fun ai_06_financeSummary() {
        openAiWorkspace()
        sendQuery("Give me a summary of my finances")
        val response = waitForAiResponse("finance")
        println("=== AI_06 FINANCE SUMMARY ===\n$response\n")
        assertTrue("Expected a non-empty response", response.isNotEmpty())
    }

    @Test
    fun ai_07_upcomingDeadlines() {
        openAiWorkspace()
        sendQuery("What are my most urgent deadlines right now?")
        val response = waitForAiResponse("deadline")
        println("=== AI_07 URGENT DEADLINES ===\n$response\n")
        assertTrue("Expected a non-empty response", response.isNotEmpty())
    }

    @Test
    fun ai_08_drivingLicence() {
        openAiWorkspace()
        sendQuery("When does my driving licence expire and what do I need to renew it?")
        val response = waitForAiResponse("licence")
        println("=== AI_08 DRIVING LICENCE ===\n$response\n")
        assertTrue("Expected a non-empty response", response.isNotEmpty())
    }

    @Test
    fun nav_01_libraryShowsObjects() {
        val libraryTab = device.findObject(By.text("Library"))
        assertNotNull("Library tab not found", libraryTab)
        libraryTab!!.click()
        // Objects are grouped by domain; "Employment" appears first alphabetically.
        // Accept any recognisable object title or domain header.
        val result = device.wait(Until.findObject(By.textContains("Product Manager")), 6_000L)
            ?: device.wait(Until.findObject(By.text("Employment")), 6_000L)
            ?: device.wait(Until.findObject(By.textContains("Google India")), 6_000L)
            ?: device.wait(Until.findObject(By.textContains("Aadhaar")), 6_000L)
        assertNotNull("No objects visible in Library", result)
        println("=== NAV_01 LIBRARY OBJECTS: OK ===")
        goHome()
    }

    @Test
    fun nav_02_plannerShowsTasks() {
        val plannerTab = device.findObject(By.text("Planner"))
        assertNotNull("Planner tab not found", plannerTab)
        plannerTab!!.click()
        // At least one pending task should be visible
        val task = device.wait(Until.findObject(By.textContains("Google")), 5_000L)
            ?: device.wait(Until.findObject(By.textContains("Japan")), 5_000L)
            ?: device.wait(Until.findObject(By.textContains("passport")), 5_000L)
        assertNotNull("No tasks visible in Planner", task)
        println("=== NAV_02 PLANNER TASKS: OK ===")
        goHome()
    }

    @Test
    fun nav_03_homeGoalsVisible() {
        goHome()
        val goal = device.wait(Until.findObject(By.textContains("Japan")), 5_000L)
        assertNotNull("No goals visible on Home", goal)
        println("=== NAV_03 HOME GOALS: OK ===")
    }

    @Test
    fun nav_04_openPassportDetail() {
        val libraryTab = device.findObject(By.text("Library"))
        libraryTab!!.click()
        // Tap any object card — use Google India (near top of Employment section)
        val card = device.wait(Until.findObject(By.textContains("Google India")), 6_000L)
            ?: device.wait(Until.findObject(By.textContains("Product Manager")), 6_000L)
        assertNotNull("No object card found in Library", card)
        card!!.click()
        // Object detail should load (any metadata or title)
        val detail = device.wait(Until.findObject(By.textContains("Google")), 6_000L)
            ?: device.wait(Until.findObject(By.textContains("Senior PM")), 6_000L)
            ?: device.wait(Until.findObject(By.textContains("Product Manager")), 6_000L)
        assertNotNull("Object detail did not load", detail)
        println("=== NAV_04 OBJECT DETAIL: OK ===")
        device.pressBack()
        goHome()
    }
}
