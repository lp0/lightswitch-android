/*
	lightswitch-android - Android Lightswitch Client

	Copyright 2014  Simon Arlott

	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.me.sa.lightswitch.android.ui;

import static com.jayway.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.verifyNew;

import java.util.concurrent.Callable;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowHandler;
import org.robolectric.shadows.ShadowIntent;
import org.robolectric.shadows.ShadowPreferenceManager;
import org.robolectric.shadows.ShadowToast;
import org.robolectric.util.ActivityController;

import uk.me.sa.lightswitch.android.R;
import uk.me.sa.lightswitch.android.data.Light;
import uk.me.sa.lightswitch.android.net.LocalMessageException;
import uk.me.sa.lightswitch.android.net.RemoteMessageException;
import uk.me.sa.lightswitch.android.net.RequestMessage;
import android.content.Intent;
import android.content.SharedPreferences;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@RunWith(RobolectricTestRunner.class)
@Config(emulateSdk = 18)
@PrepareForTest(fullyQualifiedNames = { "uk.me.sa.lightswitch.android.ui.MainActivity", "uk.me.sa.lightswitch.android.ui.MainActivity_",
		// This is not ideal...
		"uk.me.sa.lightswitch.android.ui.MainActivity_$1", "uk.me.sa.lightswitch.android.ui.MainActivity_$2",
		"uk.me.sa.lightswitch.android.ui.MainActivity_$3", "uk.me.sa.lightswitch.android.ui.MainActivity_$4" })
@PowerMockIgnore({ "*" })
@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public class TestMainActivity {
	@Rule
	public PowerMockRule rule = new PowerMockRule();

	SharedPreferences sharedPreferences;
	ActivityController<MainActivity_> controller;
	MainActivity_ activity;

	@Mock
	RequestMessage requestMessage;

	@Before
	public void create() throws Exception {
		MockitoAnnotations.initMocks(this);
		PowerMockito.whenNew(RequestMessage.class).withAnyArguments().thenReturn(requestMessage);
		ShadowToast.reset();
		sharedPreferences = ShadowPreferenceManager.getDefaultSharedPreferences(Robolectric.application.getApplicationContext());
		controller = Robolectric.buildActivity(MainActivity_.class);
		activity = controller.create().start().resume().visible().get();
	}

	@After
	public void destroy() {
		controller.pause().stop().destroy();
	}

	private static final Callable<Boolean> NON_ZERO_TOAST_COUNT = new NonZeroToastCount();

	private static class NonZeroToastCount implements Callable<Boolean> {
		public Boolean call() throws Exception {
			ShadowHandler.runMainLooperToEndOfTasks();
			Robolectric.runUiThreadTasks();
			return ShadowToast.shownToastCount() > 0;
		}
	}

	@Test
	public void clickLeftUnconfiguredNode() throws Exception {
		sharedPreferences.edit().putString("node", "").putString("secret", "").commit();

		Robolectric.clickOn(activity.findViewById(R.id.button_left));

		await().until(NON_ZERO_TOAST_COUNT);
		assertEquals("Node not configured", ShadowToast.getTextOfLatestToast());
	}

	@Test
	public void clickLeftUnconfiguredSecret() throws Exception {
		sharedPreferences.edit().putString("node", "localhost").putString("secret", "").commit();

		Robolectric.clickOn(activity.findViewById(R.id.button_left));

		await().until(NON_ZERO_TOAST_COUNT);
		assertEquals("Secret not configured", ShadowToast.getTextOfLatestToast());
	}

	@Test
	public void clickLeft() throws Exception {
		sharedPreferences.edit().putString("node", "left.node.invalid").commit();
		sharedPreferences.edit().putString("secret", "test_left").commit();

		Robolectric.clickOn(activity.findViewById(R.id.button_left));

		await().until(NON_ZERO_TOAST_COUNT);
		assertEquals("Switched light \"Left\"", ShadowToast.getTextOfLatestToast());

		verifyNew(RequestMessage.class).withArguments("test_left", Light.LEFT);
		verify(requestMessage).sendTo("left.node.invalid");
	}

	@Test
	public void clickLeftLocalError() throws Exception {
		sharedPreferences.edit().putString("node", "left.local.node.invalid").commit();
		sharedPreferences.edit().putString("secret", "test_left_local").commit();

		Mockito.doThrow(new LocalMessageException(null)).when(requestMessage).sendTo(Mockito.anyString());
		Robolectric.clickOn(activity.findViewById(R.id.button_left));

		await().until(NON_ZERO_TOAST_COUNT);
		assertEquals("Error creating request", ShadowToast.getTextOfLatestToast());

		verifyNew(RequestMessage.class).withArguments("test_left_local", Light.LEFT);
		verify(requestMessage).sendTo("left.local.node.invalid");
	}

	@Test
	public void clickLeftRemoteError() throws Exception {
		sharedPreferences.edit().putString("node", "left.remote.node.invalid").commit();
		sharedPreferences.edit().putString("secret", "test_left_remote").commit();

		Mockito.doThrow(new RemoteMessageException(null)).when(requestMessage).sendTo(Mockito.anyString());
		Robolectric.clickOn(activity.findViewById(R.id.button_left));

		await().until(NON_ZERO_TOAST_COUNT);
		assertEquals("Error sending request to left.remote.node.invalid", ShadowToast.getTextOfLatestToast());

		verifyNew(RequestMessage.class).withArguments("test_left_remote", Light.LEFT);
		verify(requestMessage).sendTo("left.remote.node.invalid");
	}

	@Test
	public void clickRightUnconfiguredNode() throws Exception {
		sharedPreferences.edit().putString("node", "").putString("secret", "").commit();

		Robolectric.clickOn(activity.findViewById(R.id.button_right));

		await().until(NON_ZERO_TOAST_COUNT);
		assertEquals("Node not configured", ShadowToast.getTextOfLatestToast());
	}

	@Test
	public void clickRightUnconfiguredSecret() throws Exception {
		sharedPreferences.edit().putString("node", "localhost").putString("secret", "").commit();

		Robolectric.clickOn(activity.findViewById(R.id.button_right));

		await().until(NON_ZERO_TOAST_COUNT);
		assertEquals("Secret not configured", ShadowToast.getTextOfLatestToast());
	}

	@Test
	public void clickRightLocalError() throws Exception {
		sharedPreferences.edit().putString("node", "right.local.node.invalid").commit();
		sharedPreferences.edit().putString("secret", "test_right_local").commit();

		Mockito.doThrow(new LocalMessageException(null)).when(requestMessage).sendTo(Mockito.anyString());
		Robolectric.clickOn(activity.findViewById(R.id.button_right));

		await().until(NON_ZERO_TOAST_COUNT);
		assertEquals("Error creating request", ShadowToast.getTextOfLatestToast());

		verifyNew(RequestMessage.class).withArguments("test_right_local", Light.RIGHT);
		verify(requestMessage).sendTo("right.local.node.invalid");
	}

	@Test
	public void clickRightRemoteError() throws Exception {
		sharedPreferences.edit().putString("node", "right.remote.node.invalid").commit();
		sharedPreferences.edit().putString("secret", "test_right_remote").commit();

		Mockito.doThrow(new RemoteMessageException(null)).when(requestMessage).sendTo(Mockito.anyString());
		Robolectric.clickOn(activity.findViewById(R.id.button_right));

		await().until(NON_ZERO_TOAST_COUNT);
		assertEquals("Error sending request to right.remote.node.invalid", ShadowToast.getTextOfLatestToast());

		verifyNew(RequestMessage.class).withArguments("test_right_remote", Light.RIGHT);
		verify(requestMessage).sendTo("right.remote.node.invalid");
	}

	@Test
	public void clickRight() throws Exception {
		sharedPreferences.edit().putString("node", "right.node.invalid").commit();
		sharedPreferences.edit().putString("secret", "test_right").commit();

		Robolectric.clickOn(activity.findViewById(R.id.button_right));

		await().until(NON_ZERO_TOAST_COUNT);
		assertEquals("Switched light \"Right\"", ShadowToast.getTextOfLatestToast());

		verifyNew(RequestMessage.class).withArguments("test_right", Light.RIGHT);
		verify(requestMessage).sendTo("right.node.invalid");
	}

	@Test
	public void openSettings() throws Exception {
		activity.openSettings();

		ShadowActivity shadowActivity = Robolectric.shadowOf_(activity);
		Intent startedIntent = shadowActivity.getNextStartedActivity();
		ShadowIntent shadowIntent = Robolectric.shadowOf_(startedIntent);
		assertEquals(SettingsActivity.class.getName(), shadowIntent.getComponent().getClassName());
	}
}
