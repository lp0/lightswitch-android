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
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.IdentityHashSet;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import uk.me.sa.lightswitch.android.data.Light;
import android.util.Log;

import com.btmatthews.hamcrest.regex.PatternMatcher;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@RunWith(PowerMockRunner.class)
@PrepareForTest(value = TestRequestMessage.class, fullyQualifiedNames = { "android.util.Log", "java.net.InetAddress", "java.lang.System",
		"uk.me.sa.lightswitch.android.net.RequestMessage" })
@PowerMockIgnore("javax.crypto.*")
@SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
public class TestRequestMessage {
	private static final Charset UTF8 = Charset.forName("UTF-8");

	@Mock
	InetAddress address1;

	@Mock
	InetAddress address2;

	@Mock
	InetAddress address3;

	@Mock
	DatagramSocket socket;

	@Mock
	JSONObject jsonMock;

	@Mock(answer = Answers.RETURNS_SMART_NULLS)
	SecretKeySpec keyMock;

	@Before
	public void mock() throws Exception {
		mockStatic(Log.class);

		mockStatic(InetAddress.class);
		when(InetAddress.getAllByName("test.node.invalid")).thenReturn(new InetAddress[] { address1 });
		when(InetAddress.getAllByName("test.nodes.invalid")).thenReturn(new InetAddress[] { address1, address2, address3 });
		when(InetAddress.getAllByName("unknown.node.invalid")).thenThrow(new UnknownHostException());
		when(InetAddress.getAllByName("empty.node.invalid")).thenReturn(new InetAddress[0]);

		whenNew(DatagramSocket.class).withAnyArguments().thenReturn(socket);

		mockStatic(System.class);
	}

	@Test
	public void testMessageLeft() throws Exception {
		CapturePackets capture = new CapturePackets();

		doAnswer(capture).when(socket).send(Mockito.isA(DatagramPacket.class));
		when(System.currentTimeMillis()).thenReturn(1401822383746L);

		// Send message
		new RequestMessage("secret1", Light.LEFT).sendTo("test.node.invalid");

		// Check message content
		assertEquals(1, capture.getCount(address1));
		assertNotNull(capture.getData(address1));

		JSONObject msg = new JSONObject(new String(capture.getData(address1), UTF8));

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
		CapturePackets capture = new CapturePackets();

		doAnswer(capture).when(socket).send(Mockito.isA(DatagramPacket.class));
		when(System.currentTimeMillis()).thenReturn(1401822469944L);

		// Send message
		new RequestMessage("secret2", Light.RIGHT).sendTo("test.node.invalid");

		// Check message content
		assertEquals(1, capture.getCount(address1));
		assertNotNull(capture.getData(address1));

		JSONObject msg = new JSONObject(new String(capture.getData(address1), UTF8));

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
		String digest = new String(Hex.encodeHex(hmac.doFinal(msg.getString("request").getBytes(UTF8))));

		assertEquals(digest, msg.getString("digest"));
	}

	@Test
	public void testMultipleMessages() throws Exception {
		CapturePackets capture = new CapturePackets();

		doAnswer(capture).when(socket).send(Mockito.isA(DatagramPacket.class));
		when(System.currentTimeMillis()).thenReturn(1401823296160L);

		// Send message
		new RequestMessage("secret3", Light.LEFT).sendTo("test.nodes.invalid");

		// Check message content
		assertEquals(1, capture.getCount(address1));
		assertNotNull(capture.getData(address1));

		JSONObject msg1 = new JSONObject(new String(capture.getData(address1), UTF8));
		JSONObject req1 = new JSONObject(msg1.getString("request"));

		assertEquals(1401823296, req1.getInt("ts"));

		assertEquals(1, capture.getCount(address2));
		assertNotNull(capture.getData(address2));
		assertEquals(1, capture.getCount(address3));
		assertNotNull(capture.getData(address3));

		assertEquals(new String(capture.getData(address1), UTF8), new String(capture.getData(address2), UTF8));
		assertEquals(new String(capture.getData(address1), UTF8), new String(capture.getData(address3), UTF8));
	}

	@Test
	public void testMultipleMessagesWithRemoteFailure1() throws Exception {
		CapturePackets capture = new CapturePackets(address1);

		doAnswer(capture).when(socket).send(Mockito.isA(DatagramPacket.class));
		when(System.currentTimeMillis()).thenReturn(1401823296160L);

		// Send message
		new RequestMessage("secret3", Light.LEFT).sendTo("test.nodes.invalid");

		// Check message content
		assertEquals(1, capture.getCount(address1));
		assertNotNull(capture.getData(address1));

		JSONObject msg1 = new JSONObject(new String(capture.getData(address1), UTF8));
		JSONObject req1 = new JSONObject(msg1.getString("request"));

		assertEquals(1401823296, req1.getInt("ts"));

		assertEquals(1, capture.getCount(address2));
		assertNotNull(capture.getData(address2));
		assertEquals(1, capture.getCount(address3));
		assertNotNull(capture.getData(address3));

		assertEquals(new String(capture.getData(address1), UTF8), new String(capture.getData(address2), UTF8));
		assertEquals(new String(capture.getData(address1), UTF8), new String(capture.getData(address3), UTF8));
	}

	@Test
	public void testMultipleMessagesWithRemoteFailure12() throws Exception {
		CapturePackets capture = new CapturePackets(address1, address2);

		doAnswer(capture).when(socket).send(Mockito.isA(DatagramPacket.class));
		when(System.currentTimeMillis()).thenReturn(1401823296160L);

		// Send message
		new RequestMessage("secret3", Light.LEFT).sendTo("test.nodes.invalid");

		// Check message content
		assertEquals(1, capture.getCount(address1));
		assertNotNull(capture.getData(address1));

		JSONObject msg1 = new JSONObject(new String(capture.getData(address1), UTF8));
		JSONObject req1 = new JSONObject(msg1.getString("request"));

		assertEquals(1401823296, req1.getInt("ts"));

		assertEquals(1, capture.getCount(address2));
		assertNotNull(capture.getData(address2));
		assertEquals(1, capture.getCount(address3));
		assertNotNull(capture.getData(address3));

		assertEquals(new String(capture.getData(address1), UTF8), new String(capture.getData(address2), UTF8));
		assertEquals(new String(capture.getData(address1), UTF8), new String(capture.getData(address3), UTF8));
	}

	@Test(expected = RemoteMessageException.class)
	public void testMultipleMessagesWithRemoteFailure123() throws Exception {
		CapturePackets capture = new CapturePackets(address1, address2, address3);

		doAnswer(capture).when(socket).send(Mockito.isA(DatagramPacket.class));
		when(System.currentTimeMillis()).thenReturn(1401823296160L);

		// Send message
		new RequestMessage("secret3", Light.LEFT).sendTo("test.nodes.invalid");

		// Check message content
		assertEquals(1, capture.getCount(address1));
		assertNotNull(capture.getData(address1));

		JSONObject msg1 = new JSONObject(new String(capture.getData(address1), UTF8));
		JSONObject req1 = new JSONObject(msg1.getString("request"));

		assertEquals(1401823296, req1.getInt("ts"));

		assertEquals(1, capture.getCount(address2));
		assertNotNull(capture.getData(address2));
		assertEquals(1, capture.getCount(address3));
		assertNotNull(capture.getData(address3));

		assertEquals(new String(capture.getData(address1), UTF8), new String(capture.getData(address2), UTF8));
		assertEquals(new String(capture.getData(address1), UTF8), new String(capture.getData(address3), UTF8));
	}

	@Test
	public void testMultipleMessagesWithRemoteFailure2() throws Exception {
		CapturePackets capture = new CapturePackets(address2);

		doAnswer(capture).when(socket).send(Mockito.isA(DatagramPacket.class));
		when(System.currentTimeMillis()).thenReturn(1401823296160L);

		// Send message
		new RequestMessage("secret3", Light.LEFT).sendTo("test.nodes.invalid");

		// Check message content
		assertEquals(1, capture.getCount(address1));
		assertNotNull(capture.getData(address1));

		JSONObject msg1 = new JSONObject(new String(capture.getData(address1), UTF8));
		JSONObject req1 = new JSONObject(msg1.getString("request"));

		assertEquals(1401823296, req1.getInt("ts"));

		assertEquals(1, capture.getCount(address2));
		assertNotNull(capture.getData(address2));
		assertEquals(1, capture.getCount(address3));
		assertNotNull(capture.getData(address3));

		assertEquals(new String(capture.getData(address1), UTF8), new String(capture.getData(address2), UTF8));
		assertEquals(new String(capture.getData(address1), UTF8), new String(capture.getData(address3), UTF8));
	}

	@Test
	public void testMultipleMessagesWithRemoteFailure23() throws Exception {
		CapturePackets capture = new CapturePackets(address2, address3);

		doAnswer(capture).when(socket).send(Mockito.isA(DatagramPacket.class));
		when(System.currentTimeMillis()).thenReturn(1401823296160L);

		// Send message
		new RequestMessage("secret3", Light.LEFT).sendTo("test.nodes.invalid");

		// Check message content
		assertEquals(1, capture.getCount(address1));
		assertNotNull(capture.getData(address1));

		JSONObject msg1 = new JSONObject(new String(capture.getData(address1), UTF8));
		JSONObject req1 = new JSONObject(msg1.getString("request"));

		assertEquals(1401823296, req1.getInt("ts"));

		assertEquals(1, capture.getCount(address2));
		assertNotNull(capture.getData(address2));
		assertEquals(1, capture.getCount(address3));
		assertNotNull(capture.getData(address3));

		assertEquals(new String(capture.getData(address1), UTF8), new String(capture.getData(address2), UTF8));
		assertEquals(new String(capture.getData(address1), UTF8), new String(capture.getData(address3), UTF8));
	}

	@Test
	public void testMultipleMessagesWithRemoteFailure3() throws Exception {
		CapturePackets capture = new CapturePackets(address3);

		doAnswer(capture).when(socket).send(Mockito.isA(DatagramPacket.class));
		when(System.currentTimeMillis()).thenReturn(1401823296160L);

		// Send message
		new RequestMessage("secret3", Light.LEFT).sendTo("test.nodes.invalid");

		// Check message content
		assertEquals(1, capture.getCount(address1));
		assertNotNull(capture.getData(address1));

		JSONObject msg1 = new JSONObject(new String(capture.getData(address1), UTF8));
		JSONObject req1 = new JSONObject(msg1.getString("request"));

		assertEquals(1401823296, req1.getInt("ts"));

		assertEquals(1, capture.getCount(address2));
		assertNotNull(capture.getData(address2));
		assertEquals(1, capture.getCount(address3));
		assertNotNull(capture.getData(address3));

		assertEquals(new String(capture.getData(address1), UTF8), new String(capture.getData(address2), UTF8));
		assertEquals(new String(capture.getData(address1), UTF8), new String(capture.getData(address3), UTF8));
	}

	@Test(expected = RemoteMessageException.class)
	public void testMessageNoHosts() throws Exception {
		doThrow(new IOException()).when(socket).send(Mockito.isA(DatagramPacket.class));
		new RequestMessage("secret4", Light.LEFT).sendTo("empty.node.invalid");
	}

	@Test(expected = LocalMessageException.class)
	public void testLocalJSONException() throws Exception {
		doAnswer(new CapturePackets()).when(socket).send(Mockito.isA(DatagramPacket.class));

		whenNew(JSONObject.class).withAnyArguments().thenReturn(jsonMock);
		doThrow(new JSONException("")).when(jsonMock).put(Mockito.anyString(), Mockito.any());

		new RequestMessage("secret4", Light.LEFT).sendTo("test.node.invalid");
	}

	@Test(expected = LocalMessageException.class)
	public void testLocalNullPointerException() throws Exception {
		doAnswer(new CapturePackets()).when(socket).send(Mockito.isA(DatagramPacket.class));

		whenNew(JSONObject.class).withAnyArguments().thenReturn(jsonMock);
		when(jsonMock.toString()).thenReturn(null);

		new RequestMessage("secret4", Light.LEFT).sendTo("test.node.invalid");
	}

	@Test(expected = LocalMessageException.class)
	public void testLocalGeneralSecurityException() throws Exception {
		doAnswer(new CapturePackets()).when(socket).send(Mockito.isA(DatagramPacket.class));

		whenNew(SecretKeySpec.class).withAnyArguments().thenReturn(keyMock);

		new RequestMessage("secret4", Light.LEFT).sendTo("test.node.invalid");
	}

	@Test(expected = RemoteMessageException.class)
	public void testRemoteUnknownHostException() throws Exception {
		doAnswer(new CapturePackets()).when(socket).send(Mockito.isA(DatagramPacket.class));

		new RequestMessage("secret4", Light.LEFT).sendTo("unknown.node.invalid");
	}

	@Test(expected = RemoteMessageException.class)
	public void testRemoteSocketException() throws Exception {
		/*
		 * https://code.google.com/p/powermock/issues/detail?id=366
		 * 
		 * whenNew(DatagramSocket.class).withAnyArguments().thenThrow(new SocketException());
		 */

		final AtomicBoolean workaroundPowerMockBug = new AtomicBoolean(true);
		whenNew(DatagramSocket.class).withAnyArguments().thenAnswer(new Answer<DatagramSocket>() {
			@Override
			public DatagramSocket answer(InvocationOnMock invocation) throws Throwable {
				if (workaroundPowerMockBug.get())
					return null;
				throw new SocketException();
			}
		});
		workaroundPowerMockBug.set(false);

		new RequestMessage("secret4", Light.LEFT).sendTo("test.node.invalid");
	}

	@Test(expected = RemoteMessageException.class)
	public void testRemoteNullPointerException() throws Exception {
		/*
		 * https://code.google.com/p/powermock/issues/detail?id=366
		 * 
		 * whenNew(DatagramPacket.class).withAnyArguments().thenThrow(new NullPointerException());
		 */

		final AtomicBoolean workaroundPowerMockBug = new AtomicBoolean(true);
		whenNew(DatagramPacket.class).withAnyArguments().thenAnswer(new Answer<DatagramPacket>() {
			@Override
			public DatagramPacket answer(InvocationOnMock invocation) throws Throwable {
				if (workaroundPowerMockBug.get())
					return null;
				throw new NullPointerException();
			}
		});
		workaroundPowerMockBug.set(false);

		new RequestMessage("secret4", Light.LEFT).sendTo("test.node.invalid");
	}

	static class CapturePackets implements Answer<Void> {
		private Map<InetAddress, byte[]> data = new IdentityHashMap<InetAddress, byte[]>();
		private Map<InetAddress, Integer> count = new IdentityHashMap<InetAddress, Integer>();
		private Set<InetAddress> failures = new IdentityHashSet<InetAddress>();

		CapturePackets(InetAddress... failures) {
			if (failures != null)
				this.failures.addAll(Arrays.asList(failures));
		}

		@Override
		public Void answer(InvocationOnMock invocation) throws Throwable {
			DatagramPacket packet = (DatagramPacket)invocation.getArguments()[0];
			Integer num = count.get(packet.getAddress());

			if (num == null)
				num = 0;
			count.put(packet.getAddress(), num + 1);

			data.put(packet.getAddress(), Arrays.copyOfRange(packet.getData(), packet.getOffset(), packet.getLength()));

			if (failures.contains(packet.getAddress()))
				throw new IOException("Test failure address");
			return null;
		}

		public byte[] getData(InetAddress address) {
			return data.get(address);
		}

		public int getCount(InetAddress address) {
			return count.get(address);
		}
	}
}
