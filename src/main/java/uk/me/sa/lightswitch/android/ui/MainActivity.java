/*
	lightswitch-android - Android Lightswitch Client

	Copyright 2014-2015  Simon Arlott

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

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.me.sa.lightswitch.android.R;
import uk.me.sa.lightswitch.android.data.Light;
import uk.me.sa.lightswitch.android.data.Prefs_;
import uk.me.sa.lightswitch.android.net.LocalMessageException;
import uk.me.sa.lightswitch.android.net.RemoteMessageException;
import uk.me.sa.lightswitch.android.net.RequestMessage;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.Toast;

@EActivity(R.layout.activity_main)
@OptionsMenu(R.menu.main_activity_actions)
public class MainActivity extends Activity {
	private Logger log = LoggerFactory.getLogger(MainActivity.class);

	@Pref
	Prefs_ prefs;

	@OptionsItem(R.id.menu_settings)
	void openSettings() {
		startActivity(new Intent(MainActivity.this, SettingsActivity.class));
	}

	@Click(R.id.button_left)
	void leftButton(View view) {
		toggleLight(Light.LEFT);
	}

	@Click(R.id.button_centre)
	void centreButton(View view) {
		toggleLight(Light.CENTRE);
	}

	@Click(R.id.button_right)
	void rightButton(View view) {
		toggleLight(Light.RIGHT);
	}

	@Background
	void toggleLight(Light light) {
		String node = prefs.node().get();
		String secret = prefs.secret().get();

		if (node.isEmpty()) {
			log.warn(getString(R.string.pref_node_missing));
			makeToast(getString(R.string.pref_node_missing));
			return;
		}

		if (secret.isEmpty()) {
			log.warn(getString(R.string.pref_secret_missing));
			makeToast(getString(R.string.pref_secret_missing));
			return;
		}

		log.info("Toggle light {}", light);

		try {
			new RequestMessage(secret, light).sendTo(node);
			makeToast(String.format(getString(R.string.switched_light), getString(light.name)));
		} catch (LocalMessageException e) {
			makeToast(getString(R.string.error_local));
		} catch (RemoteMessageException e) {
			makeToast(String.format(getString(R.string.error_remote), node));
		}
	}

	@UiThread
	void makeToast(String text) {
		Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
	}
}
