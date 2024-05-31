package io.github.agentsoz.bdiabm.v2;

/*-
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2022 by its authors. See AUTHORS file.
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

import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.bdiabm.data.PerceptContent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * A thread-safe alternative for {@code io.github.agentsoz.bdiabm.data.AgentDataContainer}
 */
public class AgentDataContainer {
    private final ConcurrentSkipListMap<String, AgentData> agentsData = new ConcurrentSkipListMap<>();
    private final Map<String, Object> locks = new HashMap<>();

    public AgentDataContainer() {}

    /**
     * Gets or creates a synchronisation lock for a given agent
     * @param agentId the agent's ID
     * @return the synchronisation lock for the given agent
     */
    synchronized private Object getOrCreateLock(String agentId) {
        if (!locks.containsKey(agentId)) {
            locks.put(agentId, new Object());
        }
        return locks.get(agentId);
    }


    public Iterator<String> getAgentIdIterator() {
        return agentsData.keySet().iterator();
    }
    /**
     * Gets the requested action's content from the given agent's container.
     * @param agentId the agent's ID
     * @param actionId the action's ID
     * @return the action's content
     */
    public ActionContent getAction(String agentId, String actionId) {
        ActionContent action;
        synchronized (getOrCreateLock(agentId)) {
            action = !agentsData.containsKey(agentId) ? null :
                    agentsData.get(agentId).getAction(actionId);

        }
        return action;
    }

    /**
     * Gets the requested percept's content from the given agent's container.
     * @param agentId the agent's ID
     * @param perceptId the percept's ID
     * @return the percept's content
     */
    public PerceptContent getPercept(String agentId, String perceptId) {
        PerceptContent percept;
        synchronized (getOrCreateLock(agentId)) {
            percept = !agentsData.containsKey(agentId) ? null :
                    agentsData.get(agentId).getPercept(perceptId);

        }
        return percept;
    }

    /**
     * Puts the given action in the given agent's container
     * @param agentId the agent's ID
     * @param actionId the action's ID
     * @param content the action's content
     */
    public void putAction(String agentId, String actionId, ActionContent content) {
        synchronized (getOrCreateLock(agentId)) {
            if (!agentsData.containsKey(agentId)) {
                agentsData.put(agentId, new AgentData());
            }
            agentsData.get(agentId).putAction(actionId, content);
        }
        
    }

    /**
     * Puts the given percept in the given agent's container
     * @param agentId  the agent's ID
     * @param perceptId the percept's ID
     * @param content the percept's conent
     */
    public void putPercept(String agentId, String perceptId, PerceptContent content) {
        synchronized (getOrCreateLock(agentId)) {
            if (!agentsData.containsKey(agentId)) {
                agentsData.put(agentId, new AgentData());
            }
            agentsData.get(agentId).putPercept(perceptId, content);

        }

    }

    /**
     * Retrieves a copy of this agent's actions from the container
     * @param agentId the agent's ID
     * @return map of actionId to {@link ActionContent}
     */
    public Map<String, ActionContent> getAllActionsCopy(String agentId) {
        final Map<String, ActionContent> content = new HashMap<>();
        synchronized (getOrCreateLock(agentId)) {
            if (agentsData.containsKey(agentId)) {
                for (Map.Entry<String, ActionContent> entry : agentsData.get(agentId).getAllActions(agentId)) {
                    content.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return content;
    }

    /**
     * Retrieves a copy of this agent's percepts from the container
     * @param agentId the agent's ID
     * @return map of percept ID to {@link PerceptContent}
     */
    public Map<String, PerceptContent> getAllPerceptsCopy(String agentId) {
        final Map<String, PerceptContent> content = new HashMap<>();
        synchronized (getOrCreateLock(agentId)) {
            if (agentsData.containsKey(agentId)) {
                for (Map.Entry<String, PerceptContent> entry : agentsData.get(agentId).getAllPercepts(agentId)) {
                    content.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return content;
    }

    public void clear() {
        for (String agentId : locks.keySet()) {
            synchronized (locks.get(agentId)) {
                //agentsData.get(agentId).clear();
                agentsData.remove(agentId);
            }
        }
    }


    /**
     * A private container for each agent's actions and percepts
     */
    private class AgentData {
        private final ConcurrentMap<String, ActionContent> actions = new ConcurrentHashMap<>();
        private final ConcurrentMap<String, PerceptContent> percepts = new ConcurrentHashMap<>();

        ActionContent getAction(String actionId) {
            return actions.get(actionId);
        }

        PerceptContent getPercept(String perceptId) {
            return percepts.get(perceptId);
        }

        Set<Map.Entry<String, ActionContent>> getAllActions(String agentId) {
            return actions.entrySet();
        }

        Set<Map.Entry<String, PerceptContent>> getAllPercepts(String agentId) {
            return percepts.entrySet();
        }

        void putAction(String actionId, ActionContent content) {
            actions.put(actionId, content);
        }

        void putPercept(String perceptId, PerceptContent content) {
            percepts.put(perceptId, content);
        }

        public void clear() {
            actions.clear();
            percepts.clear();
        }
    }


}
