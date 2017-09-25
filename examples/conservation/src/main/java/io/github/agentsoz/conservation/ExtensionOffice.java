package io.github.agentsoz.conservation;

/*
 * #%L BDI-ABM Integration Package %% Copyright (C) 2014 - 2017 by its authors. See AUTHORS file. %%
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/lgpl-3.0.html>. #L%
 */

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.agentsoz.bdiabm.data.AgentDataContainer;
import io.github.agentsoz.conservation.jill.agents.Landholder;

/**
 * Extension Office keeps track of its extension officers and their visits to the landholders given
 * constraints/policies.
 * 
 * @author dsingh
 *
 */
public class ExtensionOffice {

  final private Logger logger = LoggerFactory.getLogger(Main.LOGGER_NAME);

  /**
   * The types of landholders that will be coverd by extension officer visits. <ul> <li> NONE: No
   * visits are ever conducted <li >SUCCESSFUL_ONLY: Only landholders with active contracts are
   * visited <li> SUCCESSFUL_AND_UNSUCCESSFUL_ONLY: Only landholders with active contracts and those
   * who were unsuccessful in the last round are visited <li> ALL: All landholders are visited </ul>
   *
   */
  public enum CoverageType {
    NONE, SUCCESSFUL_ONLY, SUCCESSFUL_AND_UNSUCCESSFUL_ONLY, SUCCESSFUL_AND_UNSUCCESSFUL_MIDBAND, ALL, 
  }

  private static final int maxVisitsPerLandholderPerRound = 5;
  
  // Handle to the global agent data container
  private AgentDataContainer adc;

  // Keep track of visits per agent
  private HashMap<String, Integer> visits;
  
  // Keeps count of total number of visits in the last round (for 
  private int visitsLastCycle = 0;

  private static CoverageType coverageType = CoverageType.NONE;
  private static double visitPercentageOfEligibleLandholders = 0.0;
  private static double visitPercentagePerLandholder = 0.0;

  public ExtensionOffice() {
    visits = new HashMap<String, Integer>();
  }

  /**
   * Sets the percentage of the population that should be visited (probabilistically). This
   * percentage is applied to the set of agents first seleted by the {@link #setCoverageType(int)}
   * filter.
   * 
   * @param visitPercentage value in range {@code [0.0, 100.0]}
   */
  public static void setVisitPercentageOfEligibleLandholders(double visitPercentage) {
    visitPercentageOfEligibleLandholders = visitPercentage;
  }

  public static double getVisitPercentageOfEligibleLandholders() {
    return visitPercentageOfEligibleLandholders;
  }

  /**
   * Restricts the number of vists per landholder to this percentage of the maximum allowed, which
   * in turn is given by the constant {@link #visitPercentagePerLandholder}.
   * 
   * @param visitPercentage value in range {@code [0.0, 100.0]}
   */
  public static void setVisitPercentagePerLandholder(double visitPercentage) {
    visitPercentagePerLandholder = visitPercentage;
  }

  public static double getVisitPercentagePerLandholder() {
    return visitPercentagePerLandholder;
  }

  /**
   * Sets the coverage type for visits using the given type index in {@link CoverageType}. The value
   * is un changed if the index is invalid.
   * 
   * @param typeIndex value in range {@code [0, CoverageType.values().length]}
   */
  public static void setCoverageType(int typeIndex) {
    CoverageType[] types = CoverageType.values();
    if (typeIndex > 0 && typeIndex < types.length) {
      coverageType = types[typeIndex];
    }
  }

  public static int getCoverageType() {
    return coverageType.ordinal();
  }
  
  public int getVisitsLastCycle() {
    return visitsLastCycle;
  }
  
  public void conductVisits(int cycle) {
    visitsLastCycle = 0;
    int landholdersVisited = 0;
    logger.debug("Will conduct visits to {}% of landholders of type {}", visitPercentageOfEligibleLandholders,
        coverageType);

    // Winning and in-contract land holders will be visited by an extension officer
    for (String name : visits.keySet()) {
      Landholder agent = Main.getLandholder(name);
      int active = agent.getContracts().activeCount(); // count active contracts for this agent
      boolean shouldVisit = false;
      switch(coverageType) {
        case ALL:
          shouldVisit = true;
          break;
        case SUCCESSFUL_ONLY:
          shouldVisit = active > 0;
          break;
        case SUCCESSFUL_AND_UNSUCCESSFUL_ONLY:
          if (active > 0) { 
            // cover the successful ones
            shouldVisit = true;
          } else if (agent.getCurrentAuctionRound() != null &&
              agent.getCurrentAuctionRound().isParticipated() && 
              !agent.getCurrentAuctionRound().isWon()) {
            // cover the unsuccessful ones
            shouldVisit = true;
          }
          break;
        case SUCCESSFUL_AND_UNSUCCESSFUL_MIDBAND:
          if (active > 0) { 
            // cover the successful ones
            shouldVisit = true;
          } else if (agent.getCurrentAuctionRound() != null &&
              agent.getCurrentAuctionRound().isParticipated() && 
              !agent.getCurrentAuctionRound().isWon()) {
            // cover the unsuccessful ones
            shouldVisit = true;
          }
          if (shouldVisit) {
            // Direct visits to landholders in the middle band of the S-curve.
            // We do this by calculating how far they are from the mid point of 50. 
            // Then the closer they are to 50, the more likely they are to be visited.
            double visitLikelihood = (50 - Math.abs(50 - agent.getConservationEthicBarometer()))/50.0;
            if (ConservationUtils.getGlobalRandom().nextDouble() > visitLikelihood) {
              shouldVisit = false;
            }
          }
          break;
        default:
          break;
      }
      /*
      if (coverageType == CoverageType.ALL) {
        shouldVisit = true;
      } else if (coverageType == CoverageType.SUCCESSFUL_ONLY && active > 0) {
        shouldVisit = true;
      } else if (coverageType == CoverageType.SUCCESSFUL_AND_UNSUCCESSFUL_ONLY) {
        if (active > 0) { 
          // cover the successful ones
          shouldVisit = true;
        } else if (agent.getCurrentAuctionRound() != null &&
            agent.getCurrentAuctionRound().isParticipated() && 
            !agent.getCurrentAuctionRound().isWon()) {
          // cover the unsuccessful ones
          shouldVisit = true;
        }
      }
      */
      boolean willVisit = shouldVisit && visitPercentageOfEligibleLandholders >= ConservationUtils.getGlobalRandom().nextDouble()*100;
      int numVisits = (int) (Math.round((getVisitPercentagePerLandholder()/100.0) * maxVisitsPerLandholderPerRound));
      //numVisits = (numVisits == 0) ? 0 : ConservationUtils.getGlobalRandom().nextInt(numVisits);
      
      if (willVisit && numVisits > 0) {
        logger.debug("Agent " + name + " with contracts " + agent.getContracts()
            + " visited {} times by extension officers in this round", numVisits);
        adc.getOrCreate(name).getPerceptContainer()
            .put(Global.percepts.EXTENSION_OFFICER_VISIT.toString(), (1.0*numVisits/maxVisitsPerLandholderPerRound));
        // Record the visit
        visits.put(name, visits.get(name) + 1);
        visitsLastCycle += numVisits;
        landholdersVisited++;
      }
      // Update the contracts
      if (active > 0) {
        // Decrement the contracts remaining time
        agent.getContracts().decrementYearsLeftOnAllContracts();
      }
    }
    logger.info("Conducted {} visits to {} landholders of type {}", visitsLastCycle, landholdersVisited, coverageType);
  }

  public void init(AgentDataContainer adc, String[] agents) {
    this.adc = adc;

    // Nothing else to do if no agents were given
    if (agents == null || agents.length == 0) {
      return;
    }
    // Initialise visits to all agents
    for (String agent : agents) {
      visits.put(agent, 0);
    }
  }


}
