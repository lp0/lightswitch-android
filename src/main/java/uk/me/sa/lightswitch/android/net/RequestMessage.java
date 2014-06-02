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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.me.sa.lightswitch.android.data.Light;

public class RequestMessage {
	private static final Charset UTF8 = Charset.forName("UTF-8");
	private static final String HASH = "SHA256";
	private static final int SERVICE = 4094;
	
	private Logger log = LoggerFactory.getLogger(RequestMessage.class);

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
		jso.put("light", light.id);
		jso.put("nonce", UUID.randomUUID().toString());
		return jso.toString();
	}

	private String createJSONMessage(String request) throws UnsupportedEncodingException, IllegalStateException, GeneralSecurityException, JSONException {
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

	private byte[] toByteArray() throws LocalMessageException {
		try {
			return createJSONMessage(createJSONRequest()).getBytes(UTF8);
		} catch (UnsupportedEncodingException e) {
			log.error("Unable to encode request message", e);
			throw new LocalMessageException();
		} catch (GeneralSecurityException e) {
			log.error("Unable to sign request message", e);
			throw new LocalMessageException();
		} catch (JSONException e) {
			log.error("Unable to create request message", e);
			throw new LocalMessageException();
		} catch (RuntimeException e) {
			log.error("Error creating request message", e);
			throw new LocalMessageException();
		}
	}
	
	private void sendMessageTo(byte[] message, String node) throws RemoteMessageException {
		try {
			InetAddress[] addresses = InetAddress.getAllByName(node);
			DatagramSocket s = new DatagramSocket();
			
			for (InetAddress address : addresses) {
				log.debug("Sending {} bytes to {}", message.length, address);
				
				s.send(new DatagramPacket(message, message.length, new InetSocketAddress(address, SERVICE)));
			}
			
			s.close();
		} catch (UnknownHostException e) {
			log.error("Error resolving hostname", e);
			throw new RemoteMessageException();
		} catch (SocketException e) {
			log.error("Error creating socket", e);
			throw new RemoteMessageException();
		} catch (IOException e) {
			log.error("Error sending datagram packet", e);
			throw new RemoteMessageException();
		} catch (RuntimeException e) {
			log.error("Error sending request message", e);
			throw new RemoteMessageException();
		}
	}
	
	public void sendTo(String node) throws LocalMessageException, RemoteMessageException {
		log.info("Sending light request {} to {}", light, node);
		
		sendMessageTo(toByteArray(), node);
	}
}
