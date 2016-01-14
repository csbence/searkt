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
* Domains: 

### Search algorithms

The current goal is to compare 2 algorithms, namely Anytime Repairing A* (ARA*) and Learning Real-Time A* (LRTA*). Future research should include the implementation of other planning algorithms.

### Domains

The aim to have multiple domains to test on, ultimately the following:

#### Kinematic Point

State space: (x, y)  
Control space: (v, theta)  

Dynamics: x = v * cos(theta), y = v * sin(theta)  

#### 3D Rigid Body

State space: (x, y, z, alpha, beta, gamma)  
Control space: (x, y, z, alpha, beta, gamma) (velocities)  

#### Simple Pendulum

State space: (theta, diff(theta))  
Control space: (tau)  

Dynamics: diff(diff(theta)) = ( (tau - g * cos(theta) * 0.5) * 2) )  


#### Cart-Pole

State space: (x, theta, diff(x), diff(theta))  
Control space: (f)    
  
Dynamics: 

#### Two-link Acrobot

State space: (theta_1, theta_2, diff(theta_1), diff(theta_2))  
Control space: (tau)  

Dynamics: 


#### Fixed-wing airplane

State space: (x, y, z, v, alpha, beta, theta, omega, tau)  
Control space: (tau_des, alpha_des, beta_des)  

Dynamics: from Paranjape et al. 2013

#### Quadrotor

State space: (x, y, z, alpha, beta, gamma, diff(x), diff(y), diff(z), diff(alpha), diff(beta), diff(gamma))  
Control space: (omega_1, omega_2, omega_3, omega_4)  

Dynamics: from Ai-Omari et al. 2013.


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

## Questions
 
* How to configure logging



