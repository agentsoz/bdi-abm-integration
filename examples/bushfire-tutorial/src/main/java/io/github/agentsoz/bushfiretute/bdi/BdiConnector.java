package io.github.agentsoz.bushfiretute.bdi;

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

import io.github.agentsoz.util.evac.jackhelper.IBdiConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.agentsoz.bushfiretute.Config;


public class BdiConnector implements IBdiConnector {

	final Logger logger = LoggerFactory.getLogger("");
	
    public BdiConnector(){
    }
    
    public int getMaxDistanceToRelatives() { 
    	return Config.getMaxDistanceToRelatives();
    	
    }
    
    public int getPickupTime() { 
    	return Config.getPickUpTime();
    }
    
    public double getHighPanicThreshold() { 
    	return Config.getHighPanicThreshold();
    }
    
    public double getMedPanicThreshold() { 
    	return Config.getMedPanicThreshold();
    }
    
    public double getLowPanicThreshold() { 
    	return Config.getLowPanicThreshold();
    }
    
    public int getDiffTurn() { 
    	return Config.getDiffturn();
    }
    
	public void print_S2JACKModelConfigs() { 
		
		logger.info(" S2 JACK Model Configs"  
		+ " \n MaxDistanceToRelatives: " + this.getMaxDistanceToRelatives()
		+ " \n Random pick up time: " + this.getPickupTime()
		+ " \n Kids Proportion: " +  Config.getProportionWithKids()
		+ " \n Reltaives Proportion : " + Config.getProportionWithRelatives()  
		+ " \n maxDistanceToSchool : " +  Config.getmaxDistanceToSchool()
		+ " \n departure_Trigger_Time : " +  Config.getDepartureTriggerTime()
		);
	}
    
}
