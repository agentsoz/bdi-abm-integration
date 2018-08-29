---
title: "Plans algorithm documentation"
author: "Dhirendra Singh, Joel Robertson"
date: "17/07/2018"
output:
  html_document:
    keep_md: yes
---

[Surf Coast Shire](https://www.openstreetmap.org/relation/3290432) is unique in its population makeup due to the high number of visitors to townships around the [Great Ocean Road](https://www.openstreetmap.org/relation/6592912). On a given summer day for instance, Angleasea that has a [resident population around `2600`](http://www.censusdata.abs.gov.au/census_services/getproduct/census/2016/quickstat/SSC20045) can have as many as `15000` persons in the township.  In looking to construct a synthetic population for Surf Coast Shire for the purposes of evacuation modelling, it is therefore important to consider the numbers as well as behaviours of the significant transient population in the region.


## Population subgroups

Within the model, we will account for the following groups of people (based on input from regional stakeholders):

* `resident` : as captured by the [ABS census data](http://www.censusdata.abs.gov.au/census_services/getproduct/census/2016/quickstat/LGA26490); several methods exist for creating a synthetic population for this cohort, and one that could be readily applied here is the [algorithm from Wicramasinghe et al.](https://github.com/agentsoz/synthetic-population) from RMIT University.
* `part-time resident` : people that own a property and spend an extended period of time in the region, but are not permanently based there.  (<mark>How will we define these as seperate from regular visitors (assuming they are derived from the same source)?</mark>)
* `regular visitor` : people that regularly visit the region during the summer months, camping or in *holiday homes*, and have a working knowledge of local roads and destinations; some information on this cohort could be derived from [VISTA data](https://transport.vic.gov.au/data-and-research/vista/). (<mark>Any other dataset that might give stats on this group?</mark>)
* `overnight visitor` : people that are visiting and staying the region for a short period of time in accommodation but do not have any knowledge of the area; some information on this cohort could be derived from [VISTA data](https://transport.vic.gov.au/data-and-research/vista/).
* `day visitor` : people that visit the region for the day or on a short-stay visit, and generally do not know the area well; some information on this cohort could be derived from [VISTA data](https://transport.vic.gov.au/data-and-research/vista/). (<mark>Any other dataset that might give stats on this group?</mark>)

## Activity types

The [initial work done by Surf Cost Shire Council](https://github.com/agentsoz/bdi-abm-integration/blob/ees/examples/bushfire/scenarios/surf-coast-shire/data/from-scsc-201804/analysis-data-from-scsc-201804.md#surf-coast-shire-trips-scscsvgz) looked at the following types of activites (counts): 
`Base`(144456)
`Beach`(5578)
`Business`(39399)
`Camp`(189)
`Caravan`(7986)
`EvacZone`(48370)
`Golf`(1508)
`Hotel`(2057)
`Kindergarten`(333)
`Primary`(1123)
`Secondary`(972)
`Shops`(36193)
`Tafe`(378)
`University`(370)

In the new model, these different activities can be consolidated into broad types. The granularity here is adjustable as desired, but to work in conjunction with MATSim it is better to maintain a small and constant set of activity types. The actual locations of these activities for each population subgroup can be varied instead, allowing flexibility and variation in the possible types of trips.

As an example, here are some possible activity types:

Activity | Description
---------- | -------------------------------------------------------------------
**`home`** |  assigns the person a home location; at the moment this is a requirement dictated by MATSim. These locations should be different for each population subgroup e.g. `resident` homes would be mapped to `EvacZone`, whereas `day visitors` would be mapped to somewhere outside the region (Geelong or Colac)  <mark>other suggestions welcome</mark>;
**`work`** |  at locations designated as work areas in the region (<mark>supplied by Surf Coast Shire Council</mark>); persons will be assigned arbitrary work location coordinates in these areas; the proportion of the resident population that forms the working cohort will be based on census data for the region (`ABS 2016: SCS had 90.6% employed of which 66% drive to work`). at present `work` is a specified string which ensures activities of this type are of double duration; 
**`shop`** | at locations that represent retail and grocery shops as well as dining places; <mark>supplied by Surf Coast Shire Council</mark> 
**`beach`** |  at areas designated as beach destinations along the coast (<mark>supplied by Surf Coast Shire Council</mark>); the population will have equal preference for all beaches; 
**`other`** | at arbitrary locations other than those above (not including commuting); will be used as needed to make daily plans coherent.

## Model description

The purpose of the model is to allow users to specify the makeup of the population for specific situations, such as `Typical summer weekday/weekend`, `Falls Festival day with FFDI=100`, and so on. The intent is to:

* make all inputs and assumptions about the underlying population explicit so that they can be more easily critiqued, debated, and agreed upon;
* allow differences between populations of different scenarios to be easily understood and described;
* allow users to generate populations for different scenarios easily and automatically; and
* formalise the method of producing such populations, so that they can be accurately reproduced.

## Model assumptions

1. Overall, individuals in the full population will differ in the makeup of their daily activity plans with respect to which activities they perform, when, for how long, and in which order.

1. All activities are assumed to be repeatable and can have a minimum duration of 2 hours (with the exception of `work`, which has a minimum duration of 4 hours).

1. Currently in the model *persons are synonomous with vehicles*. In other words, all vehicles accommodate a single person (the driver) and drivers are assummed to be co-located with their vehicles. For SCS, it *might be important to model persons walking to activities from their parked vehicles and back at the end of the activity*. This might be important for the `beach` activity in particular, where the time spent in walking from/to the parked vehicle might be significant; <mark>Discuss with working group</mark>.

1. Departure times for activities are randomly distributed within the two hour time block they are allocated to. This ensures that that traffic is dispersed throughout the day, rather than in centralised pulses, but might not fully capture peak traffic events (e.g. main traffic influx clustered around 9am/5am for work). Could potentially tie departure to expected time to destination, but would need some form of routing. 

1. Home locations are assigned randomly from the selected location options. Locations for each other activity are then selected probabilistically based on their Euclidean distance from the home node. This process could eventually be refined so that probabilities are established as an input for each location node, as per the [initial work done by Surf Cost Shire Council](https://github.com/agentsoz/bdi-abm-integration/blob/ees/examples/bushfire/scenarios/surf-coast-shire/data/from-scsc-201804/analysis-data-from-scsc-201804.md#surf-coast-shire-trips-scscsvgz).

## Model inputs

*For each situation, for each population subgroup, users specify three inputs*:

* the number of agents;
* the distribution of activites through the day;
* the locations that each specified activity maps to.


For instance, on a "typical summer weekday", the distribution input for the `resident` subgroup might look like:



```
##  home,90,90,85,75,30,20,15,10,25,50,80,85
##  work, 5, 5,10,15,50,60,60,50,40,30,10,10
## beach, 0, 0, 0, 0, 5, 5,10,15, 5, 0, 0, 0
## shops, 0, 0, 0, 5,10,10,10,20,25,15, 5, 0
## other, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5
```

![](Plan-algorithm_files/figure-html/unnamed-chunk-2-1.png)<!-- -->

and the location mappings input for `resident` would be established by:


```
## home,Residential,Isolated Property
## work,Business District
## beach,Beach
## shops,Shops
## other,School
```

## Model outputs

The *output of the process is a MATSim population (XML) file*, similar to what is currently used as input to the DSS, that describes the daily activity-plan for every individual in the population. An [example output XML file is here](./plans.xml).
