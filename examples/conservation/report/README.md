# Conservation Ethics Experiments Report

This report is valid for commit [5c3a824](https://github.com/agentsoz/bdi-abm-integration/commit/5c3a824f836c46e75b4c7424df1c27fd3282a7c8).


# Experiments

## Sensitivity Analysis (SA)


The SA experiments are configured with the following six parameters:

| Parameter | Description | 
| :--- | :------------------------------------------------------------------- |
| `numberOfAgents` |  number of landholder agents in the simulation |
| `highCEAgentsPercentage` | Percentage of agents with high conservation ethic |
| `targetPercentage` | Percentage of maximum possible target (if all agents bid on the highest package) that should be assigned as the target  |
| `sigmoidMaxStepX` | Limits the amount by which the sigmoid function value can change in one step by limiting the max change in x by this value |
| `profitMotiveUpdateMultiplier` | Multiplier in range [0.0,1.0] applied to `sigmoidMaxStepX` to calculate an agent's profit motive change|
| `socialNormUpdateMultiplier` | Multiplier in range [0.0,1.0] applied to `sigmoidMaxStepX` to control how quickly an agent's CE gravitates towards the social norm|

[**TO DO**] *Explain SA and the results.* The following figure shows the correlation results:

![](testing-20170801-f5acd62-sa/test/output/correlation_diagram.pdf.p0.png)


## Results for starting populations with 25% and 75% HC agents  

Our first set of simulation results show the emergence of motivational crowding out, where monetary reward (profit) can negatively impact the intrinsic motivation to conserve (conservation ethic). The figure below shows how the percentage of high-conservation (HC) and high-profit (HP) agents changes over successive auctions, in two different initial populations: 
* **HC25** that starts with 25% HC agents and has 50% HP agents, and 
* **HC75** that starts with 75% HC agents and also has 50% HP agents. 

In HC25, the relatively higher starting proportion of HP agents leads to greater profit chasing and overall the conservation ethic in the population declines, from a combination of unsuccessful bids leading to lower participation in the HC group, and the negative effect on CE from observed profits of others. In contrast, for HC75 where the relative proportion of HC agents in the population is high to start with, the population as a whole establishes a high conservation ethic, largely from the pull towards the established (higher) conservation norm in the society.

| HC25 | HC75 |
| :---: | :---: | 
| ![a](testing-20170816-5c3a824-hice/test/run-hi-ce-comparison.sh.output.hice25/number_of_agents_in_highlow_categories.pdf.p0.png)| ![b](testing-20170816-5c3a824-hice/test/run-hi-ce-comparison.sh.output.hice75/number_of_agents_in_highlow_categories.pdf.p0.png) |
| ![a](testing-20170816-5c3a824-hice/test/run-hi-ce-comparison.sh.output.hice25/number_of_agents_in_each_category.pdf.p0.png)| ![b](testing-20170816-5c3a824-hice/test/run-hi-ce-comparison.sh.output.hice75/number_of_agents_in_each_category.pdf.p0.png) |

[**TO DO**] *We also find significant and interesting differences in the two populations with respect to the auction participation rate over time. In HC75 the participation rate starts at ~65% and quickly rises and settles at ~80%, the change representative of the increasing participation from HC landholders. In contrast, in HC25 the initial participation rate is significantly lower at ~45%. Moreover it drops sharply to well below half that in the first few rounds, before gradually climbing to ~20% by 100 auctions, driven predominantly by HP landholders gradually getting more involved.*

Next we look at the difference in cost to the agency between the two sample populations, and how this cost varies over time. The Figure below shows the result. We find that in HC25, as the high profit seeking behaviour increases over time, the cost to the agency steadily increases over successive auction rounds. In comparison, in HC75, the cost starts off slightly lower, and stays relatively constant over time. This is because the latter has more landholders  with high CE, who tend to bid with moderate to low profit margins. *At the end of 30 auction rounds, the cost of auctions for HC25 is more than double that of HC75.*

| HC25 | HC75 |
| :---: | :---: | 
| ![a](testing-20170816-5c3a824-hice/test/run-hi-ce-comparison.sh.output.hice25/cost_of_auction_cycles.pdf.p0.png)| ![b](testing-20170816-5c3a824-hice/test/run-hi-ce-comparison.sh.output.hice75/cost_of_auction_cycles.pdf.p0.png) |


[**TO DO**] *Finally, we explored the impact of social cohesion–the degree to which the CE of a landholder is influenced by the conservation norm. To do this, we created two versions each of our HC25 and HC75 populations—one with low social cohesion and one with high–giving us four populations, and repeated our experiments. Figure 3 shows how social cohesion impacts cost. Compared to the experiment in Figure 2–where the social cohesion was moderate–we find that an increase in social cohesion leads to an overall increase in HC landholders due to the stronger influence from the conservation norm. This in turn results in overall costs being substantially less than in the earlier case with moderate social cohesion.*

# About the Experiments

The experiments can only be run on Sarah's old Macbook Pro (MBP) laptop at the moment. That is because the GAMS license we have is tied to that machine. To make running tests easier, we have a script that can launch the tests remotely so that we do not need physical access to the machine. The script assumes that you have SSH configured so that you can login to the machine without a password, or in other words the command `ssh gams-machine` should get you in. You will want to set that up first. Once you have done that, the experiments can be launched from any remote machine. 

The following sections describe the steps involved in running the experiments.


## Building the distribution

For instructions on how to build the distribution see the conservation [README.md](../README.md).

## Running the experiments

* *NOTE: the scripts are not configured to allow experiments to run in parallel, so don't try that!* Run one experiment at a time, then download all the results (instructions on this further below) before running the next experiment.

### Sensitivity Analysis

Sensitivity Analysis (SA) samples are generated using the Nearly Othogonal Latin Hypercube (NOLH) method. If you wish to add new parameters to the latin hypercube, edit the [NOLH spreadsheet](../test/NOLHdesigns_v5.xls) and then cut and paste the samples that the spreadsheet generates into [samples.txt](../test/output/samples.txt) (*note that the separator is the TAB character, and not spaces*). You will also have to update the number of samples in [config](../test/output/config).


To run the SA experiments, change to the [test](../test) directory and do the following:
```
./run-on-gams-machine
```
This will SSH to the GAMS MBP and launch the tests there. The run may take a couple of hours depending on the number of samples, and the number of replicates (repeats) per sample. Roughly, each run takes about a minute, so if you have 17 samples running 20 replicates each, then you'll be looking at something in the order of 17*20 minutes for the experiments to complete.  

### High Conservation Ethics: Two Scenarios

These experiments compare the outcomes when we have very different starting proportions of High Conservation (HC) agents in the population, and keeping the proportion of High Profit (HP) agents constant. The two populations we compare are as follows:

* 25% HC agents and 50% HP agents
* 75% HC agents and 50% HP agents 

To run these experiments, change to the [test](../test) directory and do the following:
```
./run-on-gams-machine ./run-hi-ce-comparison.sh
```

These experiments take about one hour to run. 


## Analysing the results

1. Download all the results from the GAMS MBP onto your machine. To do that, create a directory to save the results in (name it something meaningful such as `testing-20170801-f5acd62-sa` so that you know what code version the experiments correspond to). Then do something like:
```
rsync -avz gams-macbook:testing/ testing-20170801-f5acd62-sa/
```

2. Now we will post process our results into a database, to make further analysis and plotting easier. To to that, change to the [test](../test) directory and run the `postproc.sh` script, pointing it to the test results directory that contains the `samples.txt` file. For example, if the directory structure is `./testing-20170801-f5acd62-sa/test/output/samples.txt`, then you would do something like:
```
./postproc.sh ./testing-20170801-f5acd62-sa/test/output/
```
This will create the database `./testing-20170801-f5acd62-sa/test/output/output.db`
You have to do this for each scenario that was run. For instance, for the two-scenario experiments, you will have to do all of the following:
```
./postproc.sh ../testing-20170816-5c3a824-hice/test/run-hi-ce-comparison.sh.output.hice25/
./postproc.sh ../testing-20170816-5c3a824-hice/test/run-hi-ce-comparison.sh.output.hice25.visit/
./postproc.sh ../testing-20170816-5c3a824-hice/test/run-hi-ce-comparison.sh.output.hice75/
./postproc.sh ../testing-20170816-5c3a824-hice/test/run-hi-ce-comparison.sh.output.hice75.visit/
```
3. Finally, you can plot the results. To do that, change to the [new RScripts](../test/RScripts/new) directory and try the following kinds of commands to plot various results (they all get saved in the same directory as the `output.db` database):
```
./correlation.R ../../testing-20170801-f5acd62-sa/test/output/ 30
./plot_number_of_agents_in_each_category.R ../../../testing-20170801-f5acd62-hice/test/run-hi-ce-comparison.sh.output.hice75.visit/ 30
./plot_number_of_agents_in_highlow_category.R ../../../testing-20170801-f5acd62-hice/test/run-hi-ce-comparison.sh.output.hice25/ 30
```
Here the parameter `30` refers to the number of auction cycles that were run.

# About this report

[This report](./README.md) is written in [Markdown](https://daringfireball.net/projects/markdown/). The [HTML version of this report](http://htmlpreview.github.io/?https://github.com/agentsoz/bdi-abm-integration/blob/master/examples/conservation/report/README.html) was produced using [Pandoc](http://pandoc.org) with the following command:
```
pandoc -s --toc -f markdown_github -t html5 -c ./github-pandoc.css README.md  > README.html 
```

Any questions, just ask [me](https://github.com/dhixsingh).

