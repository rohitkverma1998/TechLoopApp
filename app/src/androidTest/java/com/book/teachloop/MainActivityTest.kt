package com.book.teachloop

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @Test
    fun launch_showsHeaderAndDashboard() {
        ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.appTitleText)).check(matches(isDisplayed()))
        onView(withId(R.id.dashboardCard)).check(matches(isDisplayed()))
    }
}
