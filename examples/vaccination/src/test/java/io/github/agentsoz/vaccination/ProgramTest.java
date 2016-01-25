package io.github.agentsoz.vaccination;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2015 by its authors. See AUTHORS file.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import static org.junit.Assert.*;
import io.github.agentsoz.vaccination.Program;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ProgramTest {

	static ServerSocket server;
	static Socket socket;
	static PrintWriter out = null;
	static BufferedReader in = null; 
	static final int port = 23456;

	@Before
	public void setUp() throws Exception {
		// Start the server in a new thread
		server = new ServerSocket(port);
		new Thread() {
			public void run() {
				try {
					ProgramTest.socket = ProgramTest.server.accept();
					out = new PrintWriter(socket.getOutputStream(),true);
					in = new BufferedReader(new InputStreamReader(socket.getInputStream()));				
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();
		// Start the client in a new thread
		new Thread() {
			public void run() {
				String[] args = {
						"-port", Integer.toString(port)
				};
				Program.main(args);
			}
		}.start();
		// Check every 100ms if the connection is up; give up after 5 secs
		int tries = 0;
		while (tries < 50 && (out == null || in == null)) {
			tries++;
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if (out == null) {
			fail("Could not establish connection on port "+port+" (reached 5s timeout)");
		}
	}

	@After
	public void tearDown() throws Exception {
		try { socket.close(); } catch (Exception e) {}
		try { server.close(); } catch (Exception e) {}
		try { in.close(); } catch (Exception e) {}
		try { out.close(); } catch (Exception e) {}
	}

	/**
	 * Test that START succeeds
	 */
	@Test
	public void testStart() {
		String xml = "";
		xml += "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		xml += "<message id=\"START\"/>";
		out.println(xml);
		if (!waitForReply(in, 1000)) {
			fail("Did not receive a reply, was expecting one. Message sent was: " + xml);
		}
	} 

	/**
	 * Test that TERMINATE_PROGRAM will terminate the connection
	 */
	@Test
	public void testTerminate() {
		String xml = "";
		xml += "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		xml += "<message id=\"TERMINATE_PROGRAM\"/>";
		out.println(xml);
		if (!waitForReply(in, 1000)) {
			fail("Did not receive a reply, was expecting one. Message sent was: " + xml);
		}
	}

	/**
	 * Waits for a reply on the input stream within the given timeframe.
	 * @param stream The input stream
	 * @param timeout The maximum time in ms to wait. Must be in range [0,5000]. 
	 * @return true if/when a reply is received in the given time frame, 
	 *         false otherwise
	 */
	private static boolean waitForReply(final BufferedReader stream, int timeout) {
		assert(timeout >= 0 && timeout <= 5000);

		// Read the input stream in a new thread.
		// This thread will block until a reply is received
		Thread t = new Thread() {
			public void run() {
				try { stream.readLine(); } catch (Exception e) {}
			}
		};
		t.start();

		// Check every 50 ms if the thread has finished
		final int sleep = 50; 
		int tries = 0;
		while (tries < Math.ceil(timeout/sleep) && t.isAlive()) {
			tries++;
			try {
				Thread.sleep(sleep);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		boolean didReceive = !t.isAlive();
		
		// Kill the thread
		t.interrupt();
		
		return didReceive;
	}
}
