package io.github.agentsoz.util;

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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;

public class NetworkGenerator {

	public static String osmfile = null;
	public static String xmlfile = null;

	/**
	 * The MATSim library uses opengis to perform coordinate transformation
	 * Opengis accepts a "well known text" (wkt) string to define a coordinate
	 * system. See http://www.geoapi.org/3.0/javadoc/org/opengis/referencing
	 * /doc-files/WKT.html To get the correct wkt string for a region in
	 * Australia, you must know the UTM zone. You can find that here
	 * http://spatialreference.org/ref/epsg/28354/ This also gives you a tool
	 * (menu in lower left) to generate the ESRI WKT string that you can paste
	 * below.
	 * 
	 * Note, VIC straddles two zones 54H and 55H.
	 * 
	 * It is crucial when combining GIS data from multiple sources that they use
	 * the same projection.
	 * 
	 * The default value is set to the ESRI WKT of MGA zone 54
	 */
	private static String esriWkt = "PROJCS[\"GDA94 / MGA zone 54\","
			+ "GEOGCS[\"GDA94\"," + "DATUM[\"D_GDA_1994\","
			+ "SPHEROID[\"GRS_1980\",6378137,298.257222101]],"
			+ "PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]],"
			+ "PROJECTION[\"Transverse_Mercator\"],"
			+ "PARAMETER[\"latitude_of_origin\",0],"
			+ "PARAMETER[\"central_meridian\",141],"
			+ "PARAMETER[\"scale_factor\",0.9996],"
			+ "PARAMETER[\"false_easting\",500000],"
			+ "PARAMETER[\"false_northing\",10000000]," + "UNIT[\"Meter\",1]]";

	public static void main(String[] args) {

		// Parse the command line arguments
		parse(args);

		Config config = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(config);
		Network net = sc.getNetwork();
		/*
		 * CoordinateTransformation ct = TransformationFactory
		 * .getCoordinateTransformation(TransformationFactory.WGS84,
		 * TransformationFactory.WGS84);
		 */
		CoordinateTransformation ct = TransformationFactory
				.getCoordinateTransformation(TransformationFactory.WGS84,
						esriWkt);
		OsmNetworkReader onr = new OsmNetworkReader(net, ct);
		onr.setKeepPaths(true);
		onr.parse(osmfile);
		new NetworkCleaner().run(net);
		new NetworkWriter(net).write(xmlfile);
	}

	private static String usage() {
		return "usage: "
				+ NetworkGenerator.class.getName()
				+ "  -i <input_osm_file> -o <output_xml_file> -wkt <coordinate transformation string>\n"
				+ "  -i <input_osm_file>   simulation configuration file\n"
				+ "  -o <output_xml_file>  arguments to pass to MATSim\n"
				+ "  -wkt <coordinate transformation string> the ESRI well known string \n "
				+ "		based on the UTM zone of the network. For Australia, the ESRI WKT \n "
				+ "		can be generated using menu in lower left of \n"
				+ "		http://spatialreference.org/ref/epsg/28354/ page.\n"
				+ "		Default value is set to MGA zone 54\n";
	}

	private static void parse(String[] args) {
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "-i":
				if (i + 1 < args.length) {
					i++;
					osmfile = args[i];
				}
				break;
			case "-o":
				if (i + 1 < args.length) {
					i++;
					xmlfile = args[i];
				}
				break;
			case "-wkt":
				if (i + 1 < args.length) {
					i++;
					esriWkt = args[i];
				}
				break;
			}
		}
		// Abort if required args were not given
		if (osmfile == null || xmlfile == null) {
			abort("Some required options were not given");
		}

	}

	private static void abort(String err) {
		System.err.println("\nERROR: " + err + "\n");
		System.out.println(usage());
		System.exit(0);
	}

}
