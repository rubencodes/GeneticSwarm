Genetic Swarm
==========
A project by Steve Majercik, Frank Mauceri, and Ruben Martinez

**This is a swarm genetically programmed to evolve interesting behaviors. It 
has two parts: The Swarm Java which serves as a visualizer for swarm behaviors, 
and the Web Swarm which is used to evaluate and evolve behaviors.**

Web Swarm
----------
The **Web Swarm** is a Ruby on Rails application with a minimal user interface. 
The UI is only used for creating accounts, logging in, and evaluating swarms.
Each new user account causes the creation of 12 swarm behaviors. These 
behaviors can be visualized by the **Swarm** (which interacts with the **Web 
Swarm**'s JSON API), and evaluated on their visual appeal through the web UI.

On the backend, the **Web Swarm** collects these evaluations, and when all 12 
initial swarms have been evaluated, it begins the process of evolving the next 
generation. To do this, the **Web Swarm** first puts the 12 behaviors 
through *Selection*. Specifically, they are put through a tournament select 
based on their fitness (as evaluated per the user). The 6 behaviors that are 
chosen as best automatically survive to the next generation, and are also 
brought to the next phase, *Crossover*. In *Crossover*, two behaviors at a 
time are chosen to mate with each other and are both "split" at single points.
Parts of Behavior A end up in Behavior B, and vice-versa. From this process,
another 6 individuals arise for the next generation, resulting in a total of
12 individuals once again. Finally, *mutation*. During *mutation*, each of a
swarm's peoperties can be, with a low probability, be randomly replaced with 
something else. After *mutation*, the 12 new behaviors are assigned to the 
owner of the original 12, and the process repeats itself.

Java Swarm Visualizer
------
The Java **Swarm Visualizer** runs independently of the **Web Swarm**. It is 
A Java applet that serves to visualize swarm behaviors created by the **Web 
Swarm**. In order to visualize behaviors, first you must create an account 
on the **Web Swarm** UI, run the Applet, and enter your **Web Swarm**
credentials when prompted by the Applet.A swarm behavior is then retrieved 
from the **Web Swarm**'s JSON API using the Java JSON Processing Library. 
The behavior JSON is translated into a set of conditional statements that 
affect the way the swarm moves around by setting limits on its movements 
and controlling some of the swarm's properties.

Current Limitations
-------------
- While the current genetic program's Crossover function is functional, it is
	limited in that it cannot yet crossover sub-behaviors. That is, each swarm
	behavior can have a set of subbehaviors, and these are currently transferred 
	directly from parent to offspring, and are immune to Crossover and Mutation.

- Every time you evaluate a behavior on the **Web Swarm** interface, you are
	presented with the next behavior to evaluate. Currently, you must close and
	re-open the **Java Swarm Visualizer** after every evaluation in order to
	have it take on the next behavior's properties.