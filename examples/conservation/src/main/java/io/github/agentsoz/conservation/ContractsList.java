package io.github.agentsoz.conservation;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2017 by its authors. See AUTHORS file.
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

import java.util.Arrays;
import java.util.HashMap;

public class ContractsList {

	private final int DEFAULT_CONTRACT_LENGTH = 3; // in rounds
	private int nextContractId = 1;

	// Map of Contract ID and years left on contract
	private HashMap<String,Integer> contracts;
	
	// Count of active contracts
	private int active;
	
	public ContractsList() {
		contracts = new HashMap<String, Integer>();
		active = 0;
	}
	
	/**
	 * Adds a new contract of duration {@link #DEFAULT_CONTRACT_LENGTH} 
	 * @param id
	 */
	public void addNew() {
		contracts.put(String.valueOf(nextContractId++), DEFAULT_CONTRACT_LENGTH);
		active++;
	}
	
	/** 
	 * Decrements the number of years left on each contract by one, unless
	 * already at zero (expired).
	 */
	public void decrementYearsLeftOnAllContracts() {
		active = 0;
		for (String id : contracts.keySet()) {
			// Update years left on the contract
			int yearsLeft = contracts.get(id);
			yearsLeft = (yearsLeft > 0) ? yearsLeft-1 : 0;
			contracts.put(id, yearsLeft);
			// if not expires then count as valid
			if (yearsLeft > 0) {
				active++;
			}
		}
	}
	
	/** 
	 * Returns the total number of contracts (active or expired) in this list
	 * @return
	 */
	public int totalCount() {
		return contracts.size();
	}
	
	/**
	 * Returns the total number of active contracts in this list
	 * @return
	 */
	public int activeCount() {
		return active;
	}

	public String toString() {
		return Arrays.toString(contracts.values().toArray(new Integer[0]));
	}
}
