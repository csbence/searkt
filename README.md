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

* Figure out experiment framework
    * How to store/write results
    * What parameters are interesting
* Figure out how to do the logging >> conventions

## Conventions

* Naming
    - iAmGoodVariable
    - IAmGoodClass
    - iAmGoodFunction()
    - Braces open on same line
    - No abbreviations
* Use Javadoc on functions and classes, but mind the verbosity
* Logging is done using the different levels:
    - error: Actual errors / wrong stuff
    - warn: Experiment level (i.e. start i'th iteration)
    - info: higher level planners 
    - debug: internal stuff in planners, such as tree building occurrences
    - trace: Used as little as possible, but keep if used during debugging

## Questions
 
* How to configure logging
