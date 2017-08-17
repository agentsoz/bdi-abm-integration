# Conservation Ethics Experiments

## Steps to run the experiments

### Build the distribution

For instructions on how to build the distribution see the conservation [README.md](../README.md).

### Run the experiments

The experiments can only be run on Sarah's old Macbook Pro (MBP) laptop at the moment. That is because the GAMS license we have is tied to that machine. To make running tests easier, we have a script that can launch the tests remotely so that we do not need physical access to the machine. The script assumes that you have SSH configured so that you can login to the machine without a password, or in other words the command `ssh gams-machine` should get you in. You will want to set that up first. Once you have done that, the experiments can be launched from any remote machine. 

* *NOTE: the scripts are not configured to allow experiments to run in parallel, so don't try that!* Run one experiment at a time, then download all the results (instructions on this further below) before running the next experiment.

#### Sensitivity Analysis

Sensitivity Analysis (SA) samples are generated using the Nearly Othogonal Latin Hypercube (NOLH) method. If you wish to add new parameters to the latin hypercube, edit the [NOLH spreadsheet](../test/NOLHdesigns_v5.xls) and then cut and paste the samples that the spreadsheet generates into [samples.txt](../test/output/samples.txt) (*note that the separator is the TAB character, and not spaces*). You will also have to update the number of samples in [config](../test/output/config).

As of commit [5c3a824](https://github.com/agentsoz/bdi-abm-integration/commit/5c3a824f836c46e75b4c7424df1c27fd3282a7c8), the SA experiments are configured with the following six parameters:

| Parameter | Description | 
| :--- | :------------------------------------------------------------------- |
| `numberOfAgents` |  number of landholder agents in the simulation |
| `highCEAgentsPercentage` | Percentage of agents with high conservation ethic |
| `targetPercentage` | Percentage of maximum possible target (if all agents bid on the highest package) that should be assigned as the target  |
| `sigmoidMaxStepX` | Limits the amount by which the sigmoid function value can change in one step by limiting the max change in x by this value |
| `profitMotiveUpdateMultiplier` | Multiplier in range [0.0,1.0] applied to `sigmoidMaxStepX` to calculate an agent's profit motive change|
| `socialNormUpdateMultiplier` | Multiplier in range [0.0,1.0] applied to `sigmoidMaxStepX` to control how quickly an agent's CE gravitates towards the social norm|


To run the SA experiments, change to the [test](examples/conservation/test) directory and do the following:
```
./run-on-gams-machine
```
This will SSH to the GAMS MBP and launch the tests there. The run may take a couple of hours depending on the number of samples, and the number of replicates (repeats) per sample. Roughly, each run takes about a minute, so if you have 17 samples running 20 replicates each, then you'll be looking at something in the order of 17*20 minutes for the experiments to complete.  

#### Two-scenario High Conservation Ethics Scenarios

These experiments compare the outcomes when we have very different starting proportions of High Conservation (HC) agents in the population, and keeping the proportion of High Profit (HP) agents constant. The two populations we compare are as follows:

* 25% HC agents and 50% HP agents
* 75% HC agents and 50% HP agents 

To run these experiments, change to the [test](examples/conservation/test) directory and do the following:
```
./run-on-gams-machine ./run-hi-ce-comparison.sh
```

These experiments take about one hour to run. 


### Analyse the results

1. Download all the results from the GAMS MBP onto your machine. To do that, create a directory to save the results in (name it something meaningful such as `testing-20170801-f5acd62-sa` so that you know what code version the experiments correspond to). Then do something like:
```
rsync -avz gams-macbook:testing/ testing-20170801-f5acd62-sa/
```

2. Now we will post process our results into a database, to make further analysis and plotting easier. To to that, change to the [test](examples/conservation/test) directory and run the `postproc.sh` script, pointing it to the test results directory that contains the `samples.txt` file. For example, if the directory structure is `./testing-20170801-f5acd62-sa/test/output/samples.txt`, then you would do something like:
```
./postproc.sh ./testing-20170801-f5acd62-sa/test/output/
```
For the two-scenario experiments, you might have to do all of the following:
```
./postproc.sh ../testing-20170816-5c3a824-hice/test/run-hi-ce-comparison.sh.output.hice25/
./postproc.sh ../testing-20170816-5c3a824-hice/test/run-hi-ce-comparison.sh.output.hice25.visit/
./postproc.sh ../testing-20170816-5c3a824-hice/test/run-hi-ce-comparison.sh.output.hice75/
./postproc.sh ../testing-20170816-5c3a824-hice/test/run-hi-ce-comparison.sh.output.hice75.visit/
```

## About this report

[This report](./README.md) is written in [Markdown](https://daringfireball.net/projects/markdown/). The HTML version of this report was produced using [Pandoc](http://pandoc.org) with the following command:
```
pandoc -s --toc -f markdown_github -t html5 -c ./github-pandoc.css README.md  > README.html 
```

