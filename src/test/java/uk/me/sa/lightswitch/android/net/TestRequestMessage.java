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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import uk.me.sa.lightswitch.android.data.Light;
import android.util.Log;

import com.btmatthews.hamcrest.regex.PatternMatcher;

@RunWith(PowerMockRunner.class)
@PrepareForTest(value = TestRequestMessage.class, fullyQualifiedNames = {
	"android.util.Log",
	"java.net.InetAddress",
	"java.lang.System",
	"uk.me.sa.lightswitch.android.net.RequestMessage"
})
@PowerMockIgnore("javax.crypto.*") 
public class TestRequestMessage {
	private static final Charset UTF8 = Charset.forName("UTF-8");

	@Mock
	InetAddress address;

	@Mock
	DatagramSocket socket;

	@Before
	public void mock() throws Exception {
		mockStatic(Log.class);

		mockStatic(InetAddress.class);
		when(InetAddress.getAllByName("test.node.invalid")).thenReturn(new InetAddress[] { address });

		whenNew(DatagramSocket.class).withAnyArguments().thenReturn(socket);

		mockStatic(System.class);
	}

	@Test
	public void testMessageLeft() throws Exception {
		// Prepare to capture packet
		final AtomicReference<byte[]> data = new AtomicReference<byte[]>();

		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				DatagramPacket packet = (DatagramPacket)invocation.getArguments()[0];
				data.set(Arrays.copyOfRange(packet.getData(), packet.getOffset(), packet.getLength()));
				return null;
			}
		}).when(socket).send(Mockito.isA(DatagramPacket.class));

		when(System.currentTimeMillis()).thenReturn(1401822383746L);

		// Send message
		new RequestMessage("secret1", Light.LEFT).sendTo("test.node.invalid");

		// Check message content
		assertNotNull(data.get());
		JSONObject msg = new JSONObject(new String(data.get(), UTF8));

		assertEquals("SHA256", msg.getString("hash"));
		assertThat(msg.getString("digest"), PatternMatcher.matches("[0-9a-f]{64}"));

		JSONObject req = new JSONObject(msg.getString("request"));

		assertEquals(1401822383, req.getInt("ts"));
		assertEquals("L", req.getString("light"));
		assertThat(req.getString("nonce"), PatternMatcher.matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[0-9a-f]{4}-[0-9a-f]{12}"));


		// Check message digest
		Mac hmac = Mac.getInstance("HmacSHA256");
		SecretKeySpec key = new SecretKeySpec("secret1".getBytes("US-ASCII"), "HmacSHA256");
		hmac.init(key);
		String digest = new String(Hex.encodeHex(hmac.doFinal(msg.getString("request").getBytes("UTF-8"))));

		assertEquals(digest, msg.getString("digest"));
	}

	@Test
	public void testMessageRight() throws Exception {
		// Prepare to capture packet
		final AtomicReference<byte[]> data = new AtomicReference<byte[]>();

		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				DatagramPacket packet = (DatagramPacket)invocation.getArguments()[0];
				data.set(Arrays.copyOfRange(packet.getData(), packet.getOffset(), packet.getLength()));
				return null;
			}
		}).when(socket).send(Mockito.isA(DatagramPacket.class));

		when(System.currentTimeMillis()).thenReturn(1401822469944L);


		// Send message
		new RequestMessage("secret2", Light.RIGHT).sendTo("test.node.invalid");


		// Check message content
		assertNotNull(data.get());
		JSONObject msg = new JSONObject(new String(data.get(), UTF8));

		assertEquals("SHA256", msg.getString("hash"));
		assertThat(msg.getString("digest"), PatternMatcher.matches("[0-9a-f]{64}"));

		JSONObject req = new JSONObject(msg.getString("request"));

		assertEquals(1401822469, req.getInt("ts"));
		assertEquals("R", req.getString("light"));
		assertThat(req.getString("nonce"), PatternMatcher.matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[0-9a-f]{4}-[0-9a-f]{12}"));


		// Check message digest
		Mac hmac = Mac.getInstance("HmacSHA256");
		SecretKeySpec key = new SecretKeySpec("secret2".getBytes("US-ASCII"), "HmacSHA256");
		hmac.init(key);
		String digest = new String(Hex.encodeHex(hmac.doFinal(msg.getString("request").getBytes("UTF-8"))));

		assertEquals(digest, msg.getString("digest"));
	}
}
