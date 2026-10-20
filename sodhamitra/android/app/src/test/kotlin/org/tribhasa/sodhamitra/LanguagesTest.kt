package org.tribhasa.sodhamitra

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.tribhasa.sodhamitra.data.Manuscript
import org.tribhasa.sodhamitra.data.MsScope
import org.tribhasa.sodhamitra.data.PlannerBook
import org.tribhasa.sodhamitra.data.PlannerList
import org.tribhasa.sodhamitra.i18n.AppLanguage
import org.tribhasa.sodhamitra.i18n.LanguageStore

/**
 * Every interface language on the real screens with the real bundled fonts: labels come out in the chosen language, what
 * the user wrote is never touched, and screenshots are written so that scripts and layout can be looked at.
 */
@RunWith(AndroidJUnit4::class)
@Config(application = TestApp::class, qualifiers = "w411dp-h891dp-xxhdpi")
class LanguagesTest {
    @get:Rule val rule = createEmptyComposeRule()
    private val shots = File("build/screenshots").apply { mkdirs() }

    private lateinit var scenarioRef: ActivityScenario<MainActivity>

    /** draws the activity's window into a bitmap (software canvas) and saves it as a PNG */
    private fun shot(name: String) {
        rule.waitForIdle()
        var bmp: Bitmap? = null
        scenarioRef.onActivity { act ->
            val v = act.window.decorView
            bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
            v.draw(android.graphics.Canvas(bmp!!))
        }
        File(shots, "$name.png").outputStream().use { bmp!!.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun tour(lang: AppLanguage) {
        val app = ApplicationProvider.getApplicationContext<Context>() as TestApp
        LanguageStore.set(app, lang)
        val loc = LanguageStore.wrap(app, lang)
        fun s(id: Int) = loc.getString(id)
        val ui = Ui(rule)
        val kannadaTitle = "ರಾಮಾಯಣ ಹಸ್ತಪ್ರತಿ"; val mixedTitle = "Śaṅkarabhāṣya — తెలుగు"
        // scholarly content in several scripts, entered once; it must come out exactly the same in every interface language
        runBlocking {
            app.repository.saveManuscript(Manuscript(scope = MsScope.EDIT, title = kannadaTitle, serialNo = "KN-1", stakeholder = "ORI", place = "Mysuru", script = "ಕನ್ನಡ",
                author = "Vālmīki", language = "संस्कृतम्", material = "PALM_LEAF", folioCount = "120", published = true))
            app.repository.savePlannerBook(PlannerBook(listType = PlannerList.EDITING, title = mixedTitle, author = "Śaṅkara", year = "1900"))
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenarioRef = scenario
            fun back() { scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }; rule.waitForIdle() }
            val tag = lang.code
            try {

            // Home: the logo block, then the three sections and Settings, all in the chosen language
            ui.await(s(R.string.home_phd), 20_000)
            ui.assertOrder("Śodhamitra", "Research Planner", "©Dr. Diddigi Vamsi Krishna", s(R.string.home_phd), s(R.string.home_individual), s(R.string.home_planner), s(R.string.settings_title))
            shot("$tag-01-home")

            // Settings: six options in order, copyright footer
            ui.click(s(R.string.settings_title))
            ui.assertOrder(s(R.string.settings_profile), s(R.string.settings_language), s(R.string.settings_about_app), s(R.string.settings_about_dvk),
                s(R.string.settings_support), s(R.string.settings_feedback), "©Dr. Diddigi Vamsi Krishna")
            shot("$tag-02-settings")

            ui.click(s(R.string.settings_about_dvk)); ui.await(s(R.string.dvk_recognition_title)); shot("$tag-03-about-dvk"); back()
            ui.click(s(R.string.settings_about_app)); ui.await(s(R.string.about_fonts_title)); shot("$tag-04-about-app"); back()
            ui.click(s(R.string.settings_support)); ui.await(s(R.string.support_copy_upi)); shot("$tag-05-support"); back()
            back()   // Settings -> Home

            // Editing / Translation Planner -> Future Editing List: the user's own text is untouched
            ui.click(s(R.string.home_planner)); ui.click(s(R.string.future_editing_list))
            ui.await(kannadaTitle); ui.await(mixedTitle)
            ui.assertShown(s(R.string.heading_manuscript), s(R.string.heading_book), "ORI", "ಕನ್ನಡ", "संस्कृतम्", "Vālmīki")
            shot("$tag-06-editing-list")

            // the manuscript form: fourteen fields in order, localised labels, the planning checkboxes
            ui.click(s(R.string.add_plus), 0)
            val labels = listOf(R.string.ms_serial_no, R.string.ms_accession_no, R.string.ms_title, R.string.stakeholder, R.string.place, R.string.ms_edited_label,
                R.string.ms_author, R.string.language_label, R.string.script_label, R.string.material_label, R.string.folio_count, R.string.completeness,
                R.string.catalogue_ref, R.string.remarks).map { s(it) }.toMutableList()
            labels[2] = labels[2] + " *"
            ui.assertOrder(*labels.toTypedArray(), s(R.string.status_published), s(R.string.status_unpublished), s(R.string.to_be_translated))
            shot("$tag-07-manuscript-form")
            // validation message in this language
            ui.type(s(R.string.ms_title), "x"); ui.type(s(R.string.stakeholder), "y"); ui.click(s(R.string.save))
            ui.await(s(R.string.validation_place_required)); shot("$tag-08-validation")
            } catch (e: Throwable) { shot("$tag-FAILED"); throw e }
        }
        // stored content is byte-for-byte what was entered
        runBlocking {
            val m = app.repository.editingManuscripts().let { f -> f.first() }.first { it.serialNo == "KN-1" }
            assertEquals(kannadaTitle, m.title); assertEquals("ಕನ್ನಡ", m.script); assertEquals("संस्कृतम्", m.language)
        }
    }

    @Test fun english() = tour(AppLanguage.ENGLISH)
    @Test fun kannada() = tour(AppLanguage.KANNADA)
    @Test fun sanskrit() = tour(AppLanguage.SANSKRIT)
    @Test fun telugu() = tour(AppLanguage.TELUGU)
    @Test fun hindi() = tour(AppLanguage.HINDI)
    @Test fun tamil() = tour(AppLanguage.TAMIL)
    @Test fun bengali() = tour(AppLanguage.BENGALI)
    @Test fun gujarati() = tour(AppLanguage.GUJARATI)
    @Test fun malayalam() = tour(AppLanguage.MALAYALAM)
    @Test fun odia() = tour(AppLanguage.ODIA)
}
