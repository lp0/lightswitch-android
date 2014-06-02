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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.me.sa.lightswitch.android.R;
import uk.me.sa.lightswitch.android.data.Light;
import uk.me.sa.lightswitch.android.net.LocalMessageException;
import uk.me.sa.lightswitch.android.net.RemoteMessageException;
import uk.me.sa.lightswitch.android.net.RequestMessage;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity {
	private Logger log = LoggerFactory.getLogger(MainActivity.class);

	private SharedPreferences prefs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main_activity_actions, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case R.id.action_settings:
	            openSettings();
	            return true;
	            
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	private void openSettings() {
		startActivity(new Intent(MainActivity.this, SettingsActivity.class));
	}

	public void leftButton(View view) {
		toggleLight(Light.LEFT);
	}
	
	public void rightButton(View view) {
		toggleLight(Light.RIGHT);
	}
	
	private void toggleLight(Light light) {
		String node = prefs.getString("node", "");
		String secret = prefs.getString("secret", "");
		
		if (node.isEmpty()) {
			log.warn("Node not configured");
			makeToast("Node not configured");
			return;
		}
		
		if (secret.isEmpty()) {
			log.warn("Secret not configured");
			makeToast("Secret not configured");
			return;
		}
		
		log.info("Toggle light {}", light);
		toggleLight(node, secret, light);
	}
	
	private void toggleLight(final String node, final String secret, final Light light) {
		new Thread() {
			public void run() {
				RequestMessage req = new RequestMessage(secret, light);
				try {
					req.sendTo(node);
					makeToast("Switched " + getString(light.name) + " light");
				} catch (LocalMessageException e) {
					makeToast("Error creating request");
				} catch (RemoteMessageException e) {
					makeToast("Error sending request to " + node);
				}
			}
		}.start();
	}
	
	private void makeToast(final String text) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
			}
		});
	}
}
