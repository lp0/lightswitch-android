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
package uk.me.sa.lightswitch.android.net;

import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.regex.Pattern;

import org.hamcrest.MatcherAssert;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import uk.me.sa.lightswitch.android.data.Light;
import android.util.Log;

import com.btmatthews.hamcrest.regex.PatternMatcher;
	
@RunWith(PowerMockRunner.class)
@PrepareForTest(value = TestRequestMessage.class, fullyQualifiedNames = { "android.util.Log" })
@PowerMockIgnore("javax.crypto.*") 
public class TestRequestMessage {
	@Before
	public void mockLog() {
		mockStatic(Log.class);
	}

	@Test
	public void testRequestLeft() throws JSONException {
		MatcherAssert.assertThat(new RequestMessage("secret", Light.LEFT).createJSONRequest(),
			PatternMatcher.matches("\\{\"ts\":[0-9]+,\"nonce\":\"[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[0-9a-f]{4}-[0-9a-f]{12}\",\"light\":\"L\"\\}"));
	}

	@Test
	public void testRequestRight() throws JSONException {
		MatcherAssert.assertThat(new RequestMessage("secret", Light.RIGHT).createJSONRequest(),
			PatternMatcher.matches("\\{\"ts\":[0-9]+,\"nonce\":\"[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[0-9a-f]{4}-[0-9a-f]{12}\",\"light\":\"R\"\\}"));
	}

	@Test
	public void testMessageLeft() throws JSONException, UnsupportedEncodingException, IllegalStateException, GeneralSecurityException {
		RequestMessage req = new RequestMessage("secret", Light.LEFT);
		String json = req.createJSONRequest();
		MatcherAssert.assertThat(req.createJSONMessage(json),
				PatternMatcher.matches("\\{\"hash\":\"SHA256\",\"request\":\""
						+ Pattern.quote(json.replace("\\", "\\\\").replace("\"", "\\\"")) + "\",\"digest\":\"[0-9a-f]{64}\"\\}"));
	}

	@Test
	public void testMessageRight() throws JSONException, UnsupportedEncodingException, IllegalStateException, GeneralSecurityException {
		RequestMessage req = new RequestMessage("secret", Light.RIGHT);
		String json = req.createJSONRequest();
		MatcherAssert.assertThat(req.createJSONMessage(json),
				PatternMatcher.matches("\\{\"hash\":\"SHA256\",\"request\":\""
						+ Pattern.quote(json.replace("\\", "\\\\").replace("\"", "\\\"")) + "\",\"digest\":\"[0-9a-f]{64}\"\\}"));
	}
}
