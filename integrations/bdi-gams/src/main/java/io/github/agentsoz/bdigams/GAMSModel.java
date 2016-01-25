package io.github.agentsoz.bdigams;

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

import io.github.agentsoz.bdiabm.ABMServerInterface;
import io.github.agentsoz.bdiabm.data.AgentDataContainer;

import java.util.ArrayList;
import java.util.HashMap;

import com.gams.api.GAMSGlobals;
import com.gams.api.GAMSJob;
import com.gams.api.GAMSOptions;
import com.gams.api.GAMSWorkspace;
import com.gams.api.GAMSWorkspaceInfo;

public class GAMSModel implements ABMServerInterface {

	private GAMSWorkspaceInfo wsInfo = null;
	private GAMSWorkspace ws = null;
	private GAMSJob job = null;
	
	public GAMSModel(String GAMSDir, String file) {
        wsInfo = new GAMSWorkspaceInfo();
        wsInfo.setSystemDirectory(GAMSDir);
        // create GAMSWorkspace "ws" with user-specified system directory and the default working directory 
        // (the directory named with current date and time under System.getProperty("java.io.tmpdir"))
        ws = new GAMSWorkspace(".", GAMSDir,  GAMSGlobals.DebugLevel.KEEP_FILES);
        // create GAMSJob from file
        job = ws.addJobFromFile(file);
 	}
	
    public void run(HashMap<String,String> opts, ArrayList<String> input, ArrayList<String> output) {
    	// run GAMSJob
        try {
            GAMSOptions opt = ws.addOptions();
            for (String key: opts.keySet()) {
                opt.defines(key, opts.get(key));
            }
        	job.run(opt);
        } catch (Exception e) {
        	e.printStackTrace();
        }
    }

    @Override
	public void takeControl(AgentDataContainer agentDataContainer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Object queryPercept(String agentID, String perceptID) {
		// TODO Auto-generated method stub
		return null;
	}
}
