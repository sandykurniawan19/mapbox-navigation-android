package com.mapbox.services.android.navigation.ui.v5;

import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.mapbox.services.android.navigation.testapp.MainActivity;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class AppFlowTest {

  @Rule
  public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

  @Rule
  public GrantPermissionRule mRuntimePermissionRule =
    GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);

  @Test
  public void appFlowTest() {

    sleep();

//    ViewInteraction mapFab = onView(
//      allOf(withId(R.id.mapFab),
//        childAtPosition(
//          childAtPosition(
//            withId(R.id.drawer_layout),
//            0),
//          7),
//        isDisplayed()));
//    mapFab.perform(click());
//
//    ViewInteraction appCompatEditText = onView(
//      allOf(withId(R.id.editTextSearch),
//        childAtPosition(
//          childAtPosition(
//            withId(R.id.searchAutoCompleteView),
//            0),
//          0),
//        isDisplayed()));
//
//    appCompatEditText.perform(click());
//    appCompatEditText.perform(replaceText("Boston"));
//
//    sleep();
//
//    ViewInteraction recyclerView = onView(
//      allOf(withId(R.id.searchResultsRecyclerView),
//        childAtPosition(
//          withId(R.id.searchResultsView),
//          0)));
//    recyclerView.perform(actionOnItemAtPosition(0, click()));
//
//    sleep();
//
//    ViewInteraction mapFab2 = onView(
//      allOf(withId(R.id.mapFab),
//        childAtPosition(
//          childAtPosition(
//            withId(R.id.drawer_layout),
//            0),
//          7),
//        isDisplayed()));
//    mapFab2.perform(click());
//
//    sleep();
//
//    ViewInteraction mapFab3 = onView(
//      allOf(withId(R.id.mapFab),
//        childAtPosition(
//          childAtPosition(
//            withId(R.id.drawer_layout),
//            0),
//          7),
//        isDisplayed()));
//    mapFab3.perform(click());

    sleep();
  }

  private void sleep() {
    try {
      Thread.sleep(3000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private static Matcher<View> childAtPosition(
    final Matcher<View> parentMatcher, final int position) {

    return new TypeSafeMatcher<View>() {
      @Override
      public void describeTo(Description description) {
        description.appendText("Child at position " + position + " in parent ");
        parentMatcher.describeTo(description);
      }

      @Override
      public boolean matchesSafely(View view) {
        ViewParent parent = view.getParent();
        return parent instanceof ViewGroup && parentMatcher.matches(parent)
          && view.equals(((ViewGroup) parent).getChildAt(position));
      }
    };
  }
}
