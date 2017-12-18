package io.github.agentsoz.bushfiretute.datacollection;

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

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScenarioTwoData {


    public static int leaveNow = 0;
    public static int goHome = 0;
	public static int agentsWithKids = 0;
	public static int agentsWithSchools = 0;
	public static int agentsWithKidsNoSchools = 0;
	public static int totPickups = 0;

	public static int agentsWithRels = 0;
//	public static LinkedHashMap<String,Long> connectToDepTimes = new LinkedHashMap<String,Long>();
	public static List connectToAgents = new ArrayList();
	public static List connectToDepTimes = new ArrayList();

	public static String s2DataFile = "./s2Data.txt";
	public static String connectToFile = "./connectToDepTimes.txt";

	final static Logger logger = LoggerFactory.getLogger("");

public static void writeConnectToDepTimesToFile() {

		PrintWriter writer=null;

		try {

			writer = new PrintWriter(connectToFile, "UTF-8");

			writer.println("id" + "\t" + "deptime");
	        for(int i = 0 ; i < connectToAgents.size() ; i++){
	        	String id = (String)connectToAgents.get(i) ;
	        	double time  = (double)connectToDepTimes.get(i);
	        	writer.println(id + "\t" + time);
	            }

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			logger.debug(" connectToFile - FileNotFoundException : {}", e.getMessage());
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			logger.debug(" connectToFile - UnsupportedEncodingException : {}", e.getMessage());
			e.printStackTrace();
		}
		finally {
			writer.close();
		}
}

	public static void writeToFile() {

		PrintWriter writer=null;

		try {

			writer = new PrintWriter(s2DataFile, "UTF-8");

				writer.println(" LeaveNow" + "\t" + leaveNow);
				writer.println(" GoHome" + "\t" + goHome);
				writer.println(" agentsWithKids" + "\t" + agentsWithKids);
				writer.println(" agentsWithSchools" + "\t" + agentsWithSchools);
				writer.println(" agentsWithKidsNoSchools" + "\t" + agentsWithKidsNoSchools);



		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			logger.debug(" s2DataFile - FileNotFoundException : {}", e.getMessage());
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			logger.debug(" s2DataFile - UnsupportedEncodingException : {}", e.getMessage());
			e.printStackTrace();
		}
		finally {
			writer.close();
		}
}

    @SuppressWarnings("unused")
	public void appendToFile(String file, String text) {
    	PrintWriter out=null;
    	try{
    		out = new PrintWriter(new BufferedWriter(new FileWriter(file, true))); {
        	if(out == null) {
        		return ;
        	}
        	out.println(text);

    	}


    	}catch (IOException e) {
    	    //exception handling left as an exercise for the reader
    	}finally {
    		out.close();
    	}
    }


}
