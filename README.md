# UNH Robotics Real Time Search Project

## Roadmap

* Implement other algorithms & other domains
    - aWA*
    - Sliding Puzzle
* Develop a meaningful experiment runner environment, with various input and proper results
* Reconsider current code
    - For multiple experiments, do we want different initial states, and if yes, where do we instantiate those?
    - How do we make terminationChecker more general, what type of info do we give it during tests?
    - Reconsider environment/domain structure, how do we generalize over them best?

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

A 2D grid with with dirty and blocked cells. The agent aims to clean all dirty spots
by vacuuming those cells.

State space: (x,y, {dirty locations})
Actions: up, down, left, right, vacuum 


 


