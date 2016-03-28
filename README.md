# UNH Robotics Real Time Search Project

## Roadmap

* develop a meaningful experiment runner environment, with various input and proper results

## TODO

* add sliding tile puzzle to domains in readme
* get tests to run 
* refactor such that domains do not require to implement predecessors
* implement new lsslrta by Dylan (the in-admissible one that maintains a expected error)
* how to store/write results

## Conventions

* Naming
    - iAmGoodVariable
    - IAmGoodClass
    - iAmGoodFunction()
    - No abbreviations
* Style
    - Braces open on same line
    - Braces are always present after loops and if statements
* Use Javadoc on functions and classes, but mind the verbosity
* Logging is done using the different levels:
    - error: Actual errors / wrong stuff
    - warn: Experiment level (i.e. start i'th iteration)
    - info: higher level planners 
    - debug: internal stuff in planners, such as tree building occurrences
    - trace: Used as little as possible, but keep if used during debugging

## Description project
Current implemented features:

### Search algorithms

The current goal is to compare 2 algorithms, namely Anytime Repairing A* (ARA*) and Learning Real-Time A* (LRTA*). Future research should include the implementation of other planning algorithms.

### Domains

The aim to have multiple domains to test on, currently the following are implemented:

#### VacuumWorld

A 2D grid with with dirty and blocked cells. The agent aims to clean all dirty spots by vacuuming those cells.

State space: (x,y, {dirty locations})

Actions: up, down, left, right, vacuum 

#### RaceTrack ([from [1]](#ref-1))

A 2D grid of a track of any shape of cells (representing possible locations of the car) connecting a 
starting and finish line. The goal is for the car to reach the finish line from the start location.

Whenever the car hits a track boundary, it will be put back on a random position on the starting line with 0 velocity.

State space: (x, y, x. [-1,0,1], y. [-1,0,1])

Actions: Acceleration in x and y (x. and y.) of [-1,0,1]

Dynamics:       x(t+1) = x(t) + x.(t) + m(x,t)
                y(t+1) = y(t) + y.(t) + m(y,t)
                x.(t+1) = x.(t) + m(x,t)
                y.(t+1) = y.(t) + m(y,t)

Stochasticity:  p: with probability p the system will ignore the action: (m(x,t),m(y,t)) = (0,0) with probability p 

#### Acrobot  ([from [2]](#ref-2))

The Acrobot is a two link planar robot arm.  The system is underactuated and torque may be applied to the middle joint in order to move the two links.

State space: (\theta_1, \theta_2, \dot\theta_1, \dot\theta_2)

Actions: Torque applied to joint 2

## Benchmark configuration and results

### Type and unit information

 * The default time unit for every value is nanoseconds.
 * Numbers are represented as 64 bit values. (Long/Double)

### General

 * algorithm name ("algorithmName" : String)
     - Name of the algorithm.
 * domain name ("domainName" : String) 
     - Name of the domain.
 * serialized domain ("rawDomain" : String)
 * time limit ("timeLimit" : Long)
 * action duration ("actionDuration": Long)
     - Greater than zero
     - Experiment Bounds: 1 - 1,000,000,000 (ns) steps of 10,000

### Real-time search

 * Time bound type ("timeBoundType" : { "STATIC" | "DYNAMIC" })
 * Single step lookahead ("singleStepLookeahead" : { true | false })

### Anytime search

### Algorithm

#### A*

No extra parameter required

#### Weighted A*

* Weight ("weight" : Double)

#### ARA*

#### RTA*

#### LSS-LRTA*

#### Dynamic f-hat

## References

<a name="ref-1"></a>[1] Barto, Andrew G., Steven J. Bradtke, and Satinder P. Singh. "Learning to act using real-time dynamic programming." Artificial Intelligence 72.1 (1995): 81-138.

<a name="ref-2"></a>[2] R.M. Murray and J. Hauser, “A Case Study in Approximate Linearization:
The Acrobot Example,” Proc. American Control Conference, 1990.