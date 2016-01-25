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
 * Data structure to store information about bids made by land holders.
 * 
 * @author Sewwandi Perera
 */
public class Bid implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * Package id, to which the bid was made
	 */
	public int id;

	/**
	 * Bid price
	 */
	public double price;

	/**
	 * Create a bid store.
	 * 
	 * @param id
	 * @param price
	 */
	public Bid(int id, double price) {
		this.id = id;
		this.price = price;
	}

	/**
	 * {@inheritDoc}
	 */
	public String toString() {
		return new Gson().toJson(this);
	}
}
