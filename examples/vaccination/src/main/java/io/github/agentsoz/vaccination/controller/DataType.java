package io.github.agentsoz.vaccination.controller;

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


/**
 * 
 * @author Alex Lutman
 *
 */
public enum DataType {
	agentIDs("agentIDs"), 
	agentstatelist("agentstatelist"), 
	agentdatacontainer("agentdatacontainer"),
	purposeKill("kill"),
	purposeCreate("create"),
	parameterVPDBaseline("VPDBaseline"),
	parameterVAEBaseline("VAEBaseline"),
	parameterLastVaccinated("lastVaccinated");
	
	private String name;
	public static final double DEFAULT_BASELINE = 0.0;
	public static final boolean DEFAULT_LASTVACC = false;
	
	DataType(String name) {
		this.name = name;
	}
	
	public String toString() {
		return name;
	}
	public boolean equals(DataType mt) {
		if(mt.toString().equals(this.name)) {
			return true;
		}
		return false;
	}
	public static DataType toDataType(String name) {
		for(DataType mt : DataType.values()) {
			if(mt.toString().equals(name)) {
				return mt;
			}
		}
		return null;
	
	}
}
