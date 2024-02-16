package com.udacity.project4

//import androidx.compose.ui.test.junit4.createComposeRule
import android.content.Context
import android.util.Log
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onSibling
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.printToString
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.udacity.project4.view.LIST_OF_REMINDERS
import com.udacity.project4.view.ReminderListActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import com.udacity.project4.repositories.RemindersLocalRepository
import com.udacity.project4.repositories.WorldLocationDao
import com.udacity.project4.repositories.WorldLocationDatabase
import com.udacity.project4.repositories.WorldLocationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


// reference: https://dagger.dev/hilt/testing

@HiltAndroidTest
class RemindersActivityTest {
    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val reminderListActivityTestRule = createAndroidComposeRule<ReminderListActivity>()

    private val appContext = ApplicationProvider.getApplicationContext<Context>()

    private lateinit var db: WorldLocationDatabase
    private lateinit var dao: WorldLocationDao
    private lateinit var repo: RemindersLocalRepository

    @Before
    fun initialize() {
        db = Room.inMemoryDatabaseBuilder(
            context = appContext,
            WorldLocationDatabase::class.java,
        ).build()
        dao = db.worldLocationDao()
        repo = RemindersLocalRepository(dao)

        hiltRule.inject()
    }

    @After
    fun finalize() {
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun remindersListActivity_Test() {
        val activity = reminderListActivityTestRule.activity
        val contentDescriptionDelete = activity.getString(R.string.content_description_delete)

        runTest(UnconfinedTestDispatcher()) {

            val n0 = repo.getLocations().size

            reminderListActivityTestRule
                .onNodeWithTag(LIST_OF_REMINDERS)
                .assertExists()

            reminderListActivityTestRule
                .onNodeWithTag(LIST_OF_REMINDERS)
                .onChildren()
                .assertCountEquals(5)

            reminderListActivityTestRule
                .onNodeWithTag(LIST_OF_REMINDERS)
                .performScrollToIndex(3)

            val cardNode = reminderListActivityTestRule
                .onNodeWithTag(LIST_OF_REMINDERS)
                .onChildren()[3]

            cardNode.assertHasClickAction()
            cardNode.performClick()

            advanceUntilIdle()

            cardNode
                .onChildren()
                .filterToOne(hasContentDescription(contentDescriptionDelete))
                .performClick()

            advanceUntilIdle()

            val n1 = repo.getLocations().size

            Log.d("LUIS", "N0 = $n0, N1 = $n1")

//            Log.d("LUIS", reminderListActivityTestRule
//                .onNodeWithTag(LIST_OF_REMINDERS)
//                .onChildren()
//                .printToString(1))

            delay(1000)

        }
        Log.d("LUIS", "Stop!")
    }
}


//composeTestRule
//.onNodeWithContentDescription(label = appContext.getString(R.string.content_description_location_list))


//val params = object: LocationParamInterface {
//    override fun deleteLocation(location: WorldLocation) {
//        runTest {
//            locationVM.removeLocation(location)
//            geofenceVM.remove(composeTestRule.activity, location)
//        }
//    }
//
//    override fun editLocation(location: WorldLocation) {
//        runTest {
//            locationVM.addOrUpdateLocation(location)
//        }
//    }
//    override fun navigateBack() {}
//    override fun toastMessage(message: String) {}
//}