package com.udacity.project4

//import androidx.compose.ui.test.junit4.createComposeRule
import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.outerspace.luismaps2.R
import com.udacity.project4.domain.WorldLocation
import com.udacity.project4.repositories.RemindersLocalRepository
import com.udacity.project4.view.ReminderListActivity
import com.udacity.project4.viewModels.LocationViewModel
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RemindersActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule(ReminderListActivity::class.java)

    private lateinit var locationVM: LocationViewModel
    private val appContext = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var locations: List<WorldLocation>
    private lateinit var repository: RemindersLocalRepository

    @Before
    fun initialize() {
//        repository = TestDataSource.createRepo(appContext)
        locationVM = LocationViewModel(repository)
    }

    @After
    fun finalize() {
//        TestDataSource.close()
    }

    @Test
    fun remindersListActivity_Test() {
        val emptyLocationListMessage = appContext.getString(R.string.empty_location_list)
        composeTestRule.activityRule
            .scenario.moveToState(Lifecycle.State.RESUMED)
        composeTestRule
            .onNodeWithText(emptyLocationListMessage)
            .performClick()
        val stateName = composeTestRule.activityRule.scenario.state.name
        assert(stateName == "DESTROYED")
    }
}


//composeTestRule
//.onNodeWithContentDescription(label = appContext.getString(R.string.content_description_location_list))
