package io.github.agentsoz.bdiabm.data;

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

public class ActionPerceptContainer implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -5415958031296122423L;
	
	private ActionContainer actionContainer;
	private PerceptContainer perceptContainer;
	
	public ActionPerceptContainer ()
	{
		actionContainer = new ActionContainer();
		perceptContainer = new PerceptContainer();
	}
	
//	public void replace (ActionPerceptContainer apContainer)
//	{
//		setActionContainer (apContainer.actionContainer);
//		setPerceptContainer (apContainer.perceptContainer);
//	}
//	
//	public void setActionContainer (ActionContainer newContainer)
//	{
//		this.actionContainer = newContainer;
//	}
	
	/**
	 * @return
	 * The action container
	 */
	public ActionContainer getActionContainer ()
	{
		return actionContainer;
	}
	
//	public void setPerceptContainer (PerceptContainer perceptContainer) {
//		// this _is_ actually used once (to clear the container)
//		// yyyy but I don't like it.  giving a totally new address means that everything that kept a handle on the old
//		// container is now confused.   Rather put a "clear()" method into PerceptContainer.  kai, nov'17
//		this.perceptContainer = perceptContainer;
//	}

	
	/**
	 * @return
	 * The percept container
	 */
	public PerceptContainer getPerceptContainer()
	{
		return perceptContainer;
	}
	
	public boolean isEmpty() {
		return perceptContainer.isEmpty() && actionContainer.isEmpty();
	}

	@Override
	public String toString() {
		return (isEmpty()) ? "{}" : new Gson().toJson(this);
	}
}
