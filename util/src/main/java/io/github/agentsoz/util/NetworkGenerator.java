package io.github.agentsoz.util;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2023 by its authors. See AUTHORS file.
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
import org.matsim.core.network.algorithms.NetworkSimplifier;
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
//	private static String esriWkt = "PROJCS[\"GDA94 / MGA zone 54\","
//			+ "GEOGCS[\"GDA94\"," + "DATUM[\"D_GDA_1994\","
//			+ "SPHEROID[\"GRS_1980\",6378137,298.257222101]],"
//			+ "PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]],"
//			+ "PROJECTION[\"Transverse_Mercator\"],"
//			+ "PARAMETER[\"latitude_of_origin\",0],"
//			+ "PARAMETER[\"central_meridian\",141],"
//			+ "PARAMETER[\"scale_factor\",0.9996],"
//			+ "PARAMETER[\"false_easting\",500000],"
//			+ "PARAMETER[\"false_northing\",10000000]," + "UNIT[\"Meter\",1]]";
	private static String esriWkt = "EPSG:28355";

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
		System.out.println("Coordinate sytem for output is: " + esriWkt);
		CoordinateTransformation ct = TransformationFactory
				.getCoordinateTransformation(TransformationFactory.WGS84,
						esriWkt);
		OsmNetworkReader onr = new OsmNetworkReader(net, ct);

        /**
         * Defaults last accessed by DS on 13 Feb 2018.
         *
         * For OSM recommendation on Australian defaults see:
         * https://wiki.openstreetmap.org/wiki/OSM_tags_for_routing/Maxspeed#Australia
         * Values pasted below:
         * highway=motorway - 100 km/h usually, 110 km/h only where sign-posted
         * highway=primary - 80 to 90 km/h
         * highway=secondary - 50 to 70 km/h
         * highway=tertiary - 50 km/h
         * highway=residential - 50 km/h, usually 40 km/h in school zones
         *
         * Also the OSM tagging guidelines for Australia
         * https://wiki.openstreetmap.org/wiki/Australian_Tagging_Guidelines#Regional_Roads say:
         *
         * Regional Roads
         * highway=motorway Motorways, freeways, and freeway-like roads.
         *      Divided roads with 2 or 3 lanes in each direction, limited access via interchanges, no traffic lights.
         *      Generally 100 or 110 km/h speed limit. For example: Hume Freeway. In states with the Alphanumeric
         *      system, these are 'M' roads if they are of freeway standard.
         * highway=trunk National highways connecting major population centres.
         *      For example the Bruce Highway north of Cooroy. State strategic road network for example:
         *      Pacific Highway. In states with the Alphanumeric system, these are 'A' roads. 'M' roads which aren't
         *      of freeway standard are also classified as a trunk road. In other states, these are signposted with
         *      a white National Road shield, or a Green National Highway shield.
         * highway=primary State maintained roads linking major population centers to each other and to
         *      the trunk network. In states with the Alphanumeric system, these are 'B' roads. In other states,
         *      these are generally State routes signposted with blue shields.
         * highway=secondary District roads that are generally council maintained roads linking smaller population
         *      centres to each other and to the primary network. In states with the Alphanumeric system,
         *      these are 'C' roads.
         * highway=tertiary Other roads linking towns, villages and Points of Interest to each other and the
         *      secondary network. In South Australia, roads that are classified as a 'D' route under the
         *      Alphanumeric system use this classification.
         * highway=residential Local streets found in and around cities, suburbs and towns as well as in rural areas.
         * highway=unclassified Other named minor roads.
         * highway=track Gravel fire trails, forest drives, 4WD trails and similar roads. Gravel roads connecting
         *      towns etc. should be tagged as appropriate (secondary, tertiary or unclassified), along with
         *      the 'surface=unpaved' tag.
         * highway=service Unnamed access roads. e.g. Entranceways and roads in parks, government properties,
         *      beach access etc. Use a short service road where you may want to mark the entrance to a
         *      private/government area, but not map the interior private roads in detail.
         * Use the surface=unpaved tag to indicate where roads are not sealed.
         * Use the ref=* tag to indicate a route number that is signposted according to the standard below, or
         *      use a route relation. Omit non-signposted, anachronistic or historical route numbers.
         * highway=safe_t_cam Use this tag to mark the position of the NSW and SA Safe - T - Cam system
         *
         * Urban Areas
         * highway=motorway The metropolitan motorway network. 'M' classified roads in cities where they exist.
         * highway=trunk "Metroads" or 'A' classified roads in the cities where they exist, or other similar
         *      cross-city trunk routes in cities where they do not.
         * highway=primary Other main cross city and arterial routes. 'B' classified roads in cities
         *      where they exist. Major connecting roads in larger rural cities.
         * highway=secondary Major through routes within a local area, often connecting neighbouring suburbs.
         * highway=tertiary Minor through routes within a local area, often feeders to residential streets.
         * highway=residential Residential streets.
         * highway=unclassified Other streets. Not generally through routes.
         * highway=service Un-named service and access roads. Also used for small named rear-access lanes.
         *
         */
		onr.setHighwayDefaults(1, "motorway",      2, 110.0/3.6, 1.0, 2000, true);
		onr.setHighwayDefaults(1, "motorway_link", 1, 80.0/3.6, 1.0, 1500, true);
		onr.setHighwayDefaults(2, "trunk",         1, 100.0/3.6, 1.0, 2000);
		onr.setHighwayDefaults(2, "trunk_link",    1, 80.0/3.6, 1.0, 1500);
		onr.setHighwayDefaults(3, "primary",       1, 80.0/3.6, 1.0, 1500);
		onr.setHighwayDefaults(3, "primary_link",  1, 60.0/3.6, 1.0, 1500);
		onr.setHighwayDefaults(4, "secondary",     1, 60.0/3.6, 1.0, 1000);
		onr.setHighwayDefaults(4, "secondary_link",1, 60.0/3.6, 1.0, 1000);
		onr.setHighwayDefaults(5, "tertiary",      1, 50.0/3.6, 1.0,  600);
		onr.setHighwayDefaults(5, "tertiary_link", 1, 50.0/3.6, 1.0,  600);
		onr.setHighwayDefaults(6, "unclassified",  1, 50.0/3.6, 1.0,  600);
		onr.setHighwayDefaults(7, "residential",   1, 50.0/3.6, 1.0,  600);
		onr.setHighwayDefaults(8, "living_street", 1, 50.0/3.6, 1.0,  300);


		onr.setKeepPaths(true);
		onr.parse(osmfile);
		new NetworkCleaner().run(net);
		// Simplify but don't make links greater than 500m, else bends look too straight.
		// This is really just more for asethetics, and does not impact the simulation results.
		// But since we also visualise the results on a map for EES, it is important to find
		// a good balance between detail (accurate looking network but can be VERY big) and
		// speed (simplified network with less nodes and links). DS 7/Feb/18.
		new NetworkSimplifier().run(net,500);
		new NetworkCleaner().run(net);
		new NetworkWriter(net).write(xmlfile);

		System.out.println("Finished writing MATSim network to " + xmlfile);
	}

	private static String usage() {
		return "usage: "
				+ NetworkGenerator.class.getName()
				+ "  -i <input_osm_file> -o <output_xml_file> -wkt <coordinate transformation string>\n"
				+ "  -i <input_osm_file>   simulation configuration file\n"
				+ "  -o <output_xml_file>  arguments to pass to MATSim\n"
				+ "  -wkt <coordinate transformation string> the ESRI well known string \n "
				+ "		based on the UTM zone of the network. See http://spatialreference.org;\n"
				+ "		Default value is "+esriWkt+"\n";
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
