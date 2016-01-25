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

public class AgentDataTuple {
	
	private String name;
	private double VPDBaseline = DataType.DEFAULT_BASELINE;
	private double VAEBaseline = DataType.DEFAULT_BASELINE;
	private boolean lastVaccinated = DataType.DEFAULT_LASTVACC;

	
	//constructor sets values to default if none are entered (null)
	public AgentDataTuple(String name, double VPDBaseline, double VAEBaseline, boolean lastVaccinated) {
		this.name = name;
		this.VPDBaseline = VPDBaseline;
		this.VAEBaseline = VAEBaseline;
		this.lastVaccinated = lastVaccinated;
		
	}
	public AgentDataTuple(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	public double getVPDBaseline() {
		return VPDBaseline;
	}
	public double getVAEBaseline() {
		return VAEBaseline;
	}
	public boolean getLastVaccinated() {
		return lastVaccinated;
	}
}
