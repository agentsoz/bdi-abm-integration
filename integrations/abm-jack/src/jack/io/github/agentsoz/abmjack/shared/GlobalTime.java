package io.github.agentsoz.abmjack.shared;

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

import aos.jack.jak.util.timer.SimClock;

public class GlobalTime {
	public static SimClock prevTime = new SimClock();
	public static SimClock newTime = new SimClock();
	public static SimClock time = new SimClock();
	
	public static void updateTime() {
		time.setTime(newTime.getTime());
	}
	
	public static void increaseNewTime() {
		newTime.setTime(newTime.getTime() + 1);
	}
}
