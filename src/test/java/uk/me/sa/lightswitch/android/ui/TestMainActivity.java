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

import java.util.concurrent.Callable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowHandler;
import org.robolectric.shadows.ShadowPreferenceManager;
import org.robolectric.shadows.ShadowToast;
import org.robolectric.util.ActivityController;

import uk.me.sa.lightswitch.android.R;
import uk.me.sa.lightswitch.android.net.RequestMessage;
import android.content.SharedPreferences;

@Config(emulateSdk = 17)
@RunWith(RobolectricTestRunner.class)
public class TestMainActivity {
	SharedPreferences sharedPreferences;
	ActivityController<MainActivity> controller;
	MainActivity activity;

	@Mock
	RequestMessage requestMessage;

	@Before
	public void create() throws Exception {
		PowerMockito.whenNew(RequestMessage.class).withAnyArguments().thenReturn(requestMessage);
		ShadowToast.reset();
		sharedPreferences = ShadowPreferenceManager.getDefaultSharedPreferences(Robolectric.application.getApplicationContext());
		controller = Robolectric.buildActivity(MainActivity.class);
		activity = controller.create().start().resume().visible().get();
	}

	@After
	public void destroy() {
		controller.pause().stop().destroy();
	}

	Callable<Boolean> newToastIsAdded() {
		return new Callable<Boolean>() {
			public Boolean call() throws Exception {
				ShadowHandler.runMainLooperToEndOfTasks();
				Robolectric.runUiThreadTasks();
				return ShadowToast.shownToastCount() > 0;
			}
		};
	}

	@Test
	public void clickLeftUnconfiguredNode() throws Exception {
		sharedPreferences.edit().putString("node", "").putString("secret", "").commit();

		Robolectric.clickOn(activity.findViewById(R.id.button_left));

		await().until(newToastIsAdded());
		assertEquals("Node not configured", ShadowToast.getTextOfLatestToast());
	}

	@Test
	public void clickLeftUnconfiguredSecret() throws Exception {
		sharedPreferences.edit().putString("node", "localhost").putString("secret", "").commit();

		Robolectric.clickOn(activity.findViewById(R.id.button_left));

		await().until(newToastIsAdded());
		assertEquals("Secret not configured", ShadowToast.getTextOfLatestToast());
	}

	@Test
	public void clickLeft() throws Exception {
		sharedPreferences.edit().putString("node", "localhost").commit();
		sharedPreferences.edit().putString("secret", "test").commit();

		Robolectric.clickOn(activity.findViewById(R.id.button_left));

		await().until(newToastIsAdded());
		assertEquals("Switched light \"Left\"", ShadowToast.getTextOfLatestToast());
	}

	// TODO clickLeftLocalError

	@Test
	public void clickLeftRemoteError() throws Exception {
		sharedPreferences.edit().putString("node", "localhost.invalid").commit();
		sharedPreferences.edit().putString("secret", "test").commit();

		Robolectric.clickOn(activity.findViewById(R.id.button_left));

		await().until(newToastIsAdded());
		assertEquals("Error sending request to localhost.invalid", ShadowToast.getTextOfLatestToast());
	}

	@Test
	public void clickRightUnconfiguredNode() throws Exception {
		sharedPreferences.edit().putString("node", "").putString("secret", "").commit();

		Robolectric.clickOn(activity.findViewById(R.id.button_right));

		await().until(newToastIsAdded());
		assertEquals("Node not configured", ShadowToast.getTextOfLatestToast());
	}

	@Test
	public void clickRightUnconfiguredSecret() throws Exception {
		sharedPreferences.edit().putString("node", "localhost").putString("secret", "").commit();

		Robolectric.clickOn(activity.findViewById(R.id.button_right));

		await().until(newToastIsAdded());
		assertEquals("Secret not configured", ShadowToast.getTextOfLatestToast());
	}

	// TODO clickRightLocalError

	@Test
	public void clickRightRemoteError() throws Exception {
		sharedPreferences.edit().putString("node", "localhost.invalid").commit();
		sharedPreferences.edit().putString("secret", "test").commit();

		Robolectric.clickOn(activity.findViewById(R.id.button_right));

		await().until(newToastIsAdded());
		assertEquals("Error sending request to localhost.invalid", ShadowToast.getTextOfLatestToast());
	}

	@Test
	public void clickRight() throws Exception {
		sharedPreferences.edit().putString("node", "localhost").commit();
		sharedPreferences.edit().putString("secret", "test").commit();

		Robolectric.clickOn(activity.findViewById(R.id.button_right));

		await().until(newToastIsAdded());
		assertEquals("Switched light \"Right\"", ShadowToast.getTextOfLatestToast());
	}

	// TODO openSettings
}
