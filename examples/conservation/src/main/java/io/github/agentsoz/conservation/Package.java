package io.github.agentsoz.conservation;

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

import java.io.Serializable;

import com.google.gson.Gson;

/**
 * This class contains all 26 packages defined in the application
 * 
 * @author Sewwandi Perera
 */
public class Package implements Serializable {
	private static final long serialVersionUID = 1L;
	public int id;
	public String description;
	public double opportunityCost;

	// Note the 5 sets of number below are:
	// package number, then details of the package (3 numbers in
	// brackets) which show the number of each species expected to be
	// saved for that package Malleefowl Phascogale Python (or
	// someting like that). Finalnumber is the opportunity cost associated with
	// that bid.

	private static Package[] packages = {
			new Package(1, "30,00,00", 0.216664574),
			new Package(2, "60,00,00", 0.631793358),
			new Package(3, "00,30,00", 0.146948856),
			new Package(4, "00,60,00", 0.377448467),
			new Package(5, "00,00,03", 0.079435508),
			new Package(6, "00,00,06", 0.191386892),
			new Package(7, "30,30,00", 0.262469578),
			new Package(8, "30,60,00", 0.379338017),
			new Package(9, "30,00,03", 0.247099957),
			new Package(10, "30,00,06", 0.297563895),
			new Package(11, "60,30,00", 0.56396719),
			new Package(12, "60,60,00", 0.567204456),
			new Package(13, "60,00,03", 0.600741296),
			new Package(14, "60,00,06", 0.589717788),
			new Package(15, "00,30,03", 0.190300745),
			new Package(16, "00,30,06", 0.253681188),
			new Package(17, "00,60,03", 0.372229416),
			new Package(18, "00,60,06", 0.38703892),
			new Package(19, "30,30,03", 0.273658157),
			new Package(20, "30,30,06", 0.30487529),
			new Package(21, "30,60,03", 0.371279791),
			new Package(22, "30,60,06", 0.383250119),
			new Package(23, "60,30,03", 0.542992458),
			new Package(24, "60,30,06", 0.542046281),
			new Package(25, "60,60,03", 0.556307055),
			new Package(26, "60,60,06", 0.565438208), };

	/**
	 * Package Constructor
	 * 
	 * @param id
	 * @param description
	 * @param opportunityCost
	 */
	public Package(int id, String description, double opportunityCost) {
		this.id = id;
		this.description = description;
		this.opportunityCost = opportunityCost;
	}

	/**
	 * Returns the package when the package id is given
	 * 
	 * @param id
	 * @return
	 */
	public static Package getPackage(int id) {
		for (Package p : packages) {
			if (p.id == id) {
				return p;
			}
		}

		return null;
	}

	/**
	 * Returns an array of packages when the number of packages you want is
	 * given
	 * 
	 * @param size
	 * @return
	 */
	public static Package[] getPackages(int size) {
		Package[] subset = new Package[size];
		for (int i = 0; i < ((size < packages.length) ? size : packages.length); i++) {
			subset[i] = packages[i];
		}
		return subset;
	}

	/**
	 * {@inheritDoc}
	 */
	public String toString() {
		return new Gson().toJson(this);
	}

}
