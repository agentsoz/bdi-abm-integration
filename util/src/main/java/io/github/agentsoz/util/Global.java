/**
 * 
 */
package io.github.agentsoz.util;

import java.util.Random;

/**
 * @author kainagel
 *
 */
public class Global {
	// all application code should use this same instance of Random
	private static final Random random = new Random();

	private Global() {} // do not instantiate

	synchronized
	public static Random getRandom() {
		return random;
	}

}
