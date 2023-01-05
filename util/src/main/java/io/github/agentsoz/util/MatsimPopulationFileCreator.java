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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

/**
 * A simple tool to randomly distribute residents within a bound rectangle and
 * create a matsim population file Used for testing the bushfire simulation
 */
public class MatsimPopulationFileCreator {
	/*
	 * example values (for halls) gap are: top left: 142.5177, -37.13657 top
	 * right: 142.525, -37.13664 bottom left: 142.5171, -37.14509 bottom right:
	 * 142.5256, -37.14532
	 */

	// set upper right and lower left coord of rectangle
	private Double urx = null;
	private Double ury = null;
	private Double llx = null;
	private Double lly = null;

	// how many people do we need
	private int population = 0;

	/**
	 * Output file
	 */
	private String outputFile = null;

	public static void main(String[] args) {
		new MatsimPopulationFileCreator(args);
	}

	public MatsimPopulationFileCreator(String[] args) {

		if (!parseCommandLine(args)) {
			return;
		}

		// calculate delta to use in random calculation
		double dx = urx - llx;
		double dy = ury - lly;

		BufferedWriter writer = null;
		try {
			File logFile = new File(outputFile);

			System.out.println("\nCreating file " + logFile.getCanonicalPath()
					+ "\n");

			writer = new BufferedWriter(new FileWriter(logFile));
			writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
			writer.write("<!DOCTYPE population SYSTEM \"http://www.matsim.org/files/dtd/population_v5.dtd\">\n");
			writer.write("<population>\n");
			writer.write("<!-- ====================================================================== -->\n");
			writer.write("\n");

			for (int i = 0; i < population; i++) {
				double x = llx + (Math.random() * dx);
				double y = lly + (Math.random() * dy);
				writer.write("	<person id=\"" + (i + 1)
						+ "\" employed=\"no\">\n");
				writer.write("		<plan selected=\"yes\">\n");
				writer.write("			<act type=\"home\"\n");
				writer.write("				x=\"" + x + "\"\n");
				writer.write("				y=\"" + y + "\"\n");
				writer.write("				end_time=\"06:00:00\"\n");
				writer.write("			/>\n");
				writer.write("			<leg mode=\"car\">\n");
				writer.write("			</leg>\n");
				writer.write("			<act type=\"home\"\n");
				writer.write("				x=\"" + x + "\"\n");
				writer.write("				y=\"" + y + "\"\n");
				writer.write("			/>\n");
				writer.write("		</plan>\n");
				writer.write("	</person>\n");
			}
			writer.write("<!-- ====================================================================== -->\n");
			writer.write("</population>\n");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (Exception e) {
			}
		}
	}

	public boolean parseCommandLine(String[] args) {

		boolean result = true;

		String s = null;

		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "-ur":
				if (i + 2 < args.length) {
					i++;
					s = args[i];
					try {
						urx = Double.parseDouble(s);
					} catch (Exception e) {
					}
					i++;
					s = args[i];
					try {
						ury = Double.parseDouble(s);
					} catch (Exception e) {
					}
				}
				break;
			case "-ll":
				if (i + 2 < args.length) {
					i++;
					s = args[i];
					try {
						llx = Double.parseDouble(s);
					} catch (Exception e) {
					}
					i++;
					s = args[i];
					try {
						lly = Double.parseDouble(s);
					} catch (Exception e) {
					}
				}
				break;
			case "-p":
				if (i + 1 < args.length) {
					i++;
					s = args[i];
					population = Integer.parseInt(s);
				}
				break;
			case "-o":
				if (i + 1 < args.length) {
					i++;
					outputFile = args[i];
				}
				break;
			}
		}
		if (urx == null)
			result = false;
		if (ury == null)
			result = false;
		if (llx == null)
			result = false;
		if (lly == null)
			result = false;
		if (outputFile == null)
			result = false;
		if (population <= 0)
			result = false;

		if (!result) {
			System.out.println("Some required options were not given \n"
					+ usage);
		}

		return result;
	}

	private String usage = "usage:\njava -cp <path> MakeResidents -p <population> -ur <urx> <ury> -ll <llx> <lly> -o <output_file>\n"
			+ "where <path> points to the location of the Matsim library\n"
			+ "<population> is the number of random residents to generate"
			+ "<output_file> is the file to write matsim population \n"
			+ "the coordinates <urx> and <ury> define the upper right (UTM) and\n"
			+ "the coordinates <llx> and <lly> define the lower left (UTM) of an orthogonal rectangle containing the residents";

}
