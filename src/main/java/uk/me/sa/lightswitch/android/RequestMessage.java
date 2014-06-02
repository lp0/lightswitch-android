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
package uk.me.sa.lightswitch.android;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.json.JSONException;
import org.json.JSONObject;

public class RequestMessage {
	private static final Charset UTF8 = Charset.forName("UTF-8");
	private static final String HASH = "SHA256";

	private String secret;
	private Light light;
	private Long now = System.currentTimeMillis() / 1000;

	public RequestMessage(String secret, Light light) {
		this.secret = secret;
		this.light = light;
	}

	private String createJSONRequest() throws JSONException {
		JSONObject jso = new JSONObject();
		jso.put("ts", now);
		jso.put("light", light);
		jso.put("nonce", UUID.randomUUID().toString());
		return jso.toString();
	}

	private String createJSONMessage(String request) throws IOException, GeneralSecurityException, JSONException {
		Mac hmac = Mac.getInstance("Hmac" + HASH);
		SecretKeySpec key = new SecretKeySpec(secret.getBytes("US-ASCII"), "Hmac" + HASH);
		hmac.init(key);
		String digest = new String(Hex.encodeHex(hmac.doFinal(request.getBytes(UTF8))));

		JSONObject jso = new JSONObject();
		jso.put("request", request);
		jso.put("hash", HASH);
		jso.put("digest", digest);
		return jso.toString();
	}

	public byte[] toByteArray() throws IOException, GeneralSecurityException, JSONException {
		return createJSONMessage(createJSONRequest()).getBytes(UTF8);
	}
}
