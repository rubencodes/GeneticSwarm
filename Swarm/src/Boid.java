/*
 * 
 /* 
 * Stephen Majercik
 * Frank Mauceri
 * last updated 20 May 2013
 * 
 * Each Boid has its own  parameters, i.e. whatever parameters the boid was created with can
 * be modified on an individual basis.  this seems to be very important from the standpoint 
 * of creating interesting behaviors 
 * 
 * Methods that take care of the motion and rendering of a boid:
 * 	1) calculate the new velocity of a boid
 * 	2) move the boid
 * 	3) render the boid
 * 
 * In the process of moving the boids:
 * 	1) "collisions" (when two voids come closer to each other than the "proximityThreshold" may be reported to Max
 * 	2) lines may be drawn between boids in the same flock in the same neighborhood
 * 
 * 
 */


// we use Open Sound Control for communication with Max/MSP
// http://opensoundcontrol.org/
import oscP5.*;
import netP5.*;

// Processing classes 
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PVector;

// data structure for the flock
import java.util.List;
import java.util.Random;
import java.util.Vector;

public class Boid {
	// make components of initial velocity very small 
	private static final float INIT_VELOCITY_DIMENSION_MAGNITUDE = 1.0f;
	// if a random component is added to the motion, keep it small
	private static final float RANDOM_MOTION_DIMENSION_MAGNITUDE = 1.0f;
	// how close to the boundary does a Boid need to be before it "bounces back"?
	private static final int BOUDARY_SENSING_THRESHOLD = 5;

	// IDs for Boid and Flock
	private int flockID;
	private int boidID;

	// Boid parameters
	private float velocityScale;           // not sure what this is for; have to ask Frank...
	private float maxSpeed;                // speed limit
	private float normalSpeed;             // pacekeeping (see below), when used, tries to keep boid to this speed
	private float neighborRadius;          // determines which other Boids are neighbors of a given Boid    
	private float separationWeight;        // how strongly do Boids move away from every neighbor: range = [0.0, 100.0]
	private float alignmentWeight;         // how strongly do Boids align match average velocity of their neighbors: range = [0.0, 1.0]
	private float cohesionWeight;          // how strongly do Boids move toward the average position of their neighbors: range = [0.0, 1.0]
	private float pacekeepingWeight;       // how strongly do Boids stick to the normalSpeed: range = [0.0, 1.0]
	private float randomMotionProbability; // probability that a small random component is introduced into the motion (see Boid class for more info)
	private float proximityThreshold;      // distance defining a "collision" between two Boids
	private float numNeighborsOwnFlock = 0;
	private float numNeighborsAllFlocks = 0;

	private int age;                       // for purposes of removing boid if mortality being used
	
	// boid's behavior pattern
	private Behavior b;
	// boid's location and velocity
	private PVector location;  
	private PVector velocity;
	private PVector nextVelocity;  

	// needed for access to rendering methods
	private PApplet parent;

	// osc communication objects
	private OscP5 oscP5;
	private NetAddress myRemoteLocation;

	// for fooling around with various types of motion (not all of which are actually predator/prey)  ;-)
	private int myPreyID = 0;
	private Boid preyBoid = null;
	
	// constructor
	public Boid(int flockID, int boidID, PVector location, float velocityScale, 
			float maxSpeed, float normalSpeed, float neighborRadius, float separationWeight, float alignWeight, 
			float cohesionWeight, float pacekeepingWeight, float randomMotionProbability, float proximityThreshold,
			Behavior b, PApplet parent, OscP5 oscP5, NetAddress myRemoteLocation) {

		this.flockID = flockID;
		this.boidID = boidID;
		this.location = location.get();         // make a copy
		this.velocityScale = velocityScale;
		this.maxSpeed = maxSpeed;
		this.normalSpeed= normalSpeed;
		this.neighborRadius = neighborRadius;
		this.separationWeight = separationWeight;
		this.alignmentWeight = alignWeight;
		this.cohesionWeight = cohesionWeight;
		this.pacekeepingWeight = pacekeepingWeight;
		this.randomMotionProbability = randomMotionProbability;
		this.proximityThreshold = proximityThreshold;
		this.age = 0;
		this.b = b;
		// start with small random velocity
		velocity = new PVector(MusicSwarm.rand.nextFloat() * (2.0f * INIT_VELOCITY_DIMENSION_MAGNITUDE) - INIT_VELOCITY_DIMENSION_MAGNITUDE, 
				MusicSwarm.rand.nextFloat() * (2.0f * INIT_VELOCITY_DIMENSION_MAGNITUDE) - INIT_VELOCITY_DIMENSION_MAGNITUDE,
				MusicSwarm.rand.nextFloat() * (2.0f * INIT_VELOCITY_DIMENSION_MAGNITUDE) - INIT_VELOCITY_DIMENSION_MAGNITUDE);
		nextVelocity = velocity.get();  

		myPreyID = 0; 

		// for rendering
		this.parent = parent;  	
		
		// for communication with Max
		this.oscP5 = oscP5;
		this.myRemoteLocation = myRemoteLocation;

	}


	// calculate the new velocity, move, and render
	void run(Flock[] allFlocks) {

		// getting older....
		++age;

		// all of the other flavors of calcNewVelocity are experiments and can be ignored
		// (although calcNewVelocityNiceBehaviorFromWorkshop, which was created at the
		// Performamatics workshop, is quite nice...)
//		calcNewVelocityStandard(allFlocks);
		calcNewVelocityNiceBehaviorFromWorkshop(allFlocks);
//		calcNewVelocityPlayful(allFlocks);       
//		calcNewVelocityPredatorPrey(allFlocks);
//		calcNewVelocityFishSchools(allFlocks);
//		calcNewVelocityGreenSnake(allFlocks);

		// we want to be able to create a collection of behaviors and change the behaviors in response
		// to external stimuli, rather than making changes at the parameter level; 
		// this is a handcrafted test of stringing defined behaviors together
		//				if (MusicSwarm.timeStep <= 10) {
		//					neighborRadius = 950;
		//					cohesionWeight = 0.2f;
		//					separationWeight = 80;
		//					normalSpeed = 6;
		//					maxSpeed = 6;
		//				}
		//				else if (MusicSwarm.timeStep <= 20) {
		//					neighborRadius = 200;
		//					cohesionWeight = 0.05f;
		//					separationWeight = 100;
		//					normalSpeed = 12;
		//					maxSpeed = 10;
		//				}
		//				else if (MusicSwarm.timeStep <= 600) {
		//					calcNewVelocityNiceBehaviorFromWorkshop(allFlocks);
		//				}
		//				else if (MusicSwarm.timeStep >= 602 && MusicSwarm.timeStep <= 1000) {
		//					calcNewVelocityPredatorPrey(allFlocks);
		//				}
		//				else if (MusicSwarm.timeStep >= 1002) {
		//					calcNewVelocityNiceBehaviorFromWorkshop(allFlocks);
		//				}
		//		
		//				else if (MusicSwarm.timeStep  == 601 || MusicSwarm.timeStep == 1001){
		//					neighborRadius = 0;
		//					maxSpeed = 4.0f;
		//					normalSpeed = 4.0f;
		//					pacekeepingWeight = 1.0f;
		//					calcNewVelocityStandard(allFlocks);
		//				}


		// reset the Boid's velocity and move the Boid
		velocity.set(nextVelocity.x, nextVelocity.y, nextVelocity.z);
		location.add(velocity);

		// render the Boid graphically
		render();

	}


	
	// calculate the new velocity of the Boid;
	// also detects and reports proximity events, but this has not been used for quite a while (as of 5/15/13)
	void calcNewVelocityStandard(Flock[] allFlocks) {

		// the new acceleration
		PVector acceleration = new PVector(0.0f,0.0f,0.0f);

		// need sum of locations of Boids in the neighborhood for acceleration due to cohesion,
		// since cohesion = acceleration toward the average location of Boids in the neighborhood
		PVector sumNeighborLocations = new PVector(0.0f,0.0f,0.0f);
		
		// need sum of velocities of Boids in the neighborhood for acceleration due to alignment,
		// since alignment = acceleration toward the average velocity of Boids in the neighborhood
		PVector sumNeighborVelocities = new PVector(0.0f,0.0f,0.0f);

		int numNeighbors = 0;

		// need to get info on all Boids in all Flocks in the neighborhood
		for (int nextFlockID = 1 ; nextFlockID < allFlocks.length ; nextFlockID++) {

			synchronized(allFlocks[nextFlockID].boids) {

				// go to next iteration of the loop if this flock is empty
				if (allFlocks[nextFlockID].flockEmpty())
					continue;

				List<Boid> boids = allFlocks[nextFlockID].boids;

				// get info for all Boids in this Flock
				for (int i = 0 ; i < boids.size(); i++) {

					Boid otherBoid = (Boid) boids.get(i);
					float dist = location.dist(otherBoid.location);

					// dist > 0 so that a Boid does not count itself as a Boid in the neighborhood
					if (dist > 0 && dist <= neighborRadius) {

						// keep count of neighbors 
						++numNeighbors;

						// sum locations for cohesion calculation after all neighbors have been processed
						sumNeighborLocations.add(otherBoid.location); 

						// sum velocities for alignment calculation after all neighbors have been processed
						sumNeighborVelocities.add(otherBoid.velocity);

						// for separation:
						// calculate and weight vector pointing away from neighbor; add to acceleration
						PVector vectorToThisBoid = PVector.sub(location, otherBoid.location);
						// separation force is inversely proportional to the square of the distance
						vectorToThisBoid.div(dist*dist);  
						// but some experiments indicate to me that we might want to consider reducing the
						// denominator to dist^1.5, or possibly even dist. using dist^2 seems to weaken the 
						// separation force to an extent that makes it very difficult for separation to have 
						// any impact when the cohesion is at its max; I would think that even when cohesion 
						// is high, if the separation weight is >50, it should loosen tight clusters significantly,
						// which ddoes not happen currently 
//						vectorToThisBoid.div((float) Math.pow(dist, 1.5));  
//						vectorToThisBoid.div(dist);  
						vectorToThisBoid.mult(separationWeight);
						acceleration.add(vectorToThisBoid);  

						// draw lines between Boids from the same flock in the same neighborhood
						if (flockID == nextFlockID)
							connectBoids(otherBoid);

						// report proximity events to osc
						if (dist < proximityThreshold)
							reportCollision(otherBoid);

					}
				} 
			}
		}

		if (numNeighbors > 0) {
			// cohesion steering: steer in the direction of the average location of your neighbors
			PVector cohesionVector = PVector.div(sumNeighborLocations, (float) numNeighbors);        
			cohesionVector.sub(location);
			cohesionVector.mult(cohesionWeight);
			acceleration.add(cohesionVector);

			// alignment steering: steer so as to align your velocity with the average velocity of your neighbors
			PVector alignmentVector = PVector.div(sumNeighborVelocities, (float) numNeighbors);
			alignmentVector.sub(velocity);
			alignmentVector.mult(alignmentWeight);
			acceleration.add(alignmentVector);
		}


		// with the probability specified by the parameter randomMotionProbability, introduce a small
		// random perturbation (magnitude defined by RANDOM_MOTION_DIMENSION_MAGNITUDE) into each 
		// acceleration component
		if (MusicSwarm.rand.nextFloat() < randomMotionProbability) {
			acceleration.add(new PVector(MusicSwarm.rand.nextFloat() * (2.0f * RANDOM_MOTION_DIMENSION_MAGNITUDE) - RANDOM_MOTION_DIMENSION_MAGNITUDE, 
					MusicSwarm.rand.nextFloat() * (2.0f * RANDOM_MOTION_DIMENSION_MAGNITUDE) - RANDOM_MOTION_DIMENSION_MAGNITUDE,
					MusicSwarm.rand.nextFloat() * (2.0f * RANDOM_MOTION_DIMENSION_MAGNITUDE) - RANDOM_MOTION_DIMENSION_MAGNITUDE));
		} 

		// update velocity
		nextVelocity.add(acceleration);

		// add wind
		nextVelocity.add(MusicSwarm.windVector);

		// make sure we don't exceed maxSpeed
		if (nextVelocity.mag() > maxSpeed) {
			nextVelocity.mult(maxSpeed / nextVelocity.mag());
		}

		// pacekeeping (stick to normalSpeed to the extent indicated by pacekeepingWeight)
		PVector pacekeeping = 
			PVector.mult(nextVelocity, ((normalSpeed - nextVelocity.mag()) / nextVelocity.mag() * pacekeepingWeight));
		nextVelocity.add(pacekeeping);

		// bounce back from the boundaries of the space
		PVector boundaryAcc = new PVector(0,0,0);
		if (location.x < (-MusicSwarm.WINDOW_HEIGHT/2) + BOUDARY_SENSING_THRESHOLD)         
			boundaryAcc.x = maxSpeed;	
		else if (location.x > MusicSwarm.WINDOW_HEIGHT/2 - BOUDARY_SENSING_THRESHOLD) 
			boundaryAcc.x = -maxSpeed;
		if (location.y < (-MusicSwarm.WINDOW_HEIGHT/2) + BOUDARY_SENSING_THRESHOLD)
			boundaryAcc.y = maxSpeed;	
		else if (location.y > MusicSwarm.WINDOW_HEIGHT/2 - BOUDARY_SENSING_THRESHOLD) 
			boundaryAcc.y = -maxSpeed;	  
		if (location.z < (-MusicSwarm.WINDOW_HEIGHT/2) + BOUDARY_SENSING_THRESHOLD)
			boundaryAcc.z = maxSpeed;	
		else if (location.z > MusicSwarm.WINDOW_DEPTH/2 - BOUDARY_SENSING_THRESHOLD) 
			boundaryAcc.z = -maxSpeed;	     
		nextVelocity.add(boundaryAcc);     

		// velocity scale from Max
		// not sure why we have this....
		nextVelocity.mult(velocityScale);     

	}




	// nice behavior handcrafted at PERFORMAMATICS Workshop (January, 2013)
	// to calculate motions of Boids and detect proximity events
	void calcNewVelocityNiceBehaviorFromWorkshop(Flock[] allFlocks) {
		Random r = new Random(); //playing
		// the new acceleration
		PVector acceleration = new PVector(0.0f,0.0f,0.0f);

		// need sum of locations of Boids in the neighborhood for acceleration due to cohesion,
		// since cohesion = acceleration toward the average location of Boids in the neighborhood,
		// but for this behavior a Boid only coheres with Boids in its own flock
		PVector sumNeighborLocationsOwnFlock = new PVector(0,0,0);   
		
		// need sum of velocities of Boids in the neighborhood for acceleration due to alignment,
		// since alignment = acceleration toward the average velocity of Boids in the neighborhood,
		// but for this behavior a Boid only aligns with Boids in its own flock
		PVector sumNeighborVelocitiesOwnFlock = new PVector(0.0f,0.0f,0.0f);

		// so we need to count boids in the boid's own flock as well as boids in all the flocks 
		// (in the neighborhood)
		numNeighborsOwnFlock = 0;
		numNeighborsAllFlocks = 0;

		// need to get info on all Boids in all Flocks in the neighborhood
		for (int nextFlockID = 1 ; nextFlockID < allFlocks.length ; nextFlockID++) {

			synchronized(allFlocks[nextFlockID].boids) {

				// go to next iteration of the loop if this flock is empty
				if (allFlocks[nextFlockID].flockEmpty())
					continue;

				List<Boid> boids = allFlocks[nextFlockID].boids;

				// get info for all Boids in this Flock
				for (int i = 0 ; i < boids.size(); i++) {

					Boid otherBoid = (Boid) boids.get(i);
					float dist = location.dist(otherBoid.location);

					// dist > 0 so that a Boid does not count itself as a Boid in the neighborhood
					if (dist > 0 && dist <= neighborRadius) {

						// for cohesion and alignment, but only for boids in the same flock
						if (nextFlockID == flockID) {
							sumNeighborLocationsOwnFlock.add(otherBoid.location); 
							sumNeighborVelocitiesOwnFlock.add(otherBoid.velocity);
						}

						// calculate and weight vector pointing away from a neighbor in any flock
						PVector vectorToThisBoid = PVector.sub(location, otherBoid.location);
						vectorToThisBoid.div(dist*dist);  
						vectorToThisBoid.mult(separationWeight);
						acceleration.add(vectorToThisBoid);  

						// keep count of neighbors in the same flock
						if (nextFlockID == flockID)
							++numNeighborsOwnFlock; 

						// also keep count of neighbors in all flocks
						++numNeighborsAllFlocks;

						// draw lines between Boids from the same flock in the same neighborhood
						if (flockID == nextFlockID)
							connectBoids(otherBoid);

						// report proximity events to osc
						if (dist < proximityThreshold)
							reportCollision(otherBoid);

					}
				} 
			}
		}
		
		// a boid coheres and aligns with others ONLY IN ITS OWN FLOCK 
		if (numNeighborsOwnFlock > 0) {
			// cohesion steering: steer in the direction of the average location of your neighbors
			PVector cohesionVector = PVector.div(sumNeighborLocationsOwnFlock, (float) numNeighborsOwnFlock);        
			cohesionVector.sub(location);
			cohesionVector.mult(cohesionWeight);
			acceleration.add(cohesionVector);

			// alignment steering: steer so as to align your velocity with the average velocity of your neighbors
			PVector alignmentVector = PVector.div(sumNeighborVelocitiesOwnFlock, (float) numNeighborsOwnFlock);
			alignmentVector.sub(velocity);
			alignmentVector.mult(alignmentWeight);
			acceleration.add(alignmentVector);
		}

		// handcrafted rules to obtain "interesting behavior found at PERFORMAMATICS Workshop (January, 2013)

		
//		Vector<Integer> actionVarsIDs = new Vector<Integer>();
//		actionVarsIDs.add(3);
//		actionVarsIDs.add(6);
//		actionVarsIDs.add(2);
//		actionVarsIDs.add(1);
//		
//		Vector<Integer> actionIDs = new Vector<Integer>();
//		actionIDs.add(0);
//		actionIDs.add(2);
//		actionIDs.add(2);
//		actionIDs.add(2);
//		
//		Vector<Integer> nullActionIDs = new Vector<Integer>();
//		nullActionIDs.add(1);
//		nullActionIDs.add(2);
//		nullActionIDs.add(2);
//		nullActionIDs.add(2);
//		
//		Vector<Float> numBank = new Vector<Float>();
//		numBank.add(1f);
//		numBank.add(0.9f);
//		numBank.add(6f);
//		numBank.add(6f);
//		
//		Vector<Float> nullNumBank = new Vector<Float>();
//		nullNumBank.add(1f);
//		nullNumBank.add(0.1f);
//		nullNumBank.add(24f);
//		nullNumBank.add(24f);
//		
//		Behavior b = new Behavior(this, 1, 10, 20, actionIDs, actionVarsIDs, nullActionIDs, actionVarsIDs, numBank, nullNumBank);
//		b.execute();
		
		
		b.execute(this);

		// with the probability specified by the parameter randomMotionProbability, introduce a small
		// random perturbation (magnitude defined by RANDOM_MOTION_DIMENSION_MAGNITUDE) into each 
		// acceleration component
		if (MusicSwarm.rand.nextFloat() < randomMotionProbability) {
			acceleration.add(new PVector(MusicSwarm.rand.nextFloat() * (2.0f * RANDOM_MOTION_DIMENSION_MAGNITUDE) - RANDOM_MOTION_DIMENSION_MAGNITUDE, 
					MusicSwarm.rand.nextFloat() * (2.0f * RANDOM_MOTION_DIMENSION_MAGNITUDE) - RANDOM_MOTION_DIMENSION_MAGNITUDE,
					MusicSwarm.rand.nextFloat() * (2.0f * RANDOM_MOTION_DIMENSION_MAGNITUDE) - RANDOM_MOTION_DIMENSION_MAGNITUDE));
		} 

		// update velocity
		nextVelocity.add(acceleration);

		// add wind
		nextVelocity.add(MusicSwarm.windVector);

		// make sure we don't exceed maxSpeed
		if (nextVelocity.mag() > maxSpeed) {
			float normalizationFactor = maxSpeed / nextVelocity.mag();
			nextVelocity.mult(normalizationFactor);
		}

		// pacekeeping (stick to normalSpeed to the extent indicated by pacekeepingWeight)
		PVector pacekeeping = 
			PVector.mult(nextVelocity, ((normalSpeed - nextVelocity.mag()) / nextVelocity.mag() * pacekeepingWeight));
		nextVelocity.add(pacekeeping);

		// bounce back from the boundaries of the space
		PVector boundaryAcc = new PVector(0,0,0);
		if (location.x < (-MusicSwarm.WINDOW_HEIGHT/2) + BOUDARY_SENSING_THRESHOLD)         
			boundaryAcc.x = maxSpeed;	
		else if (location.x > MusicSwarm.WINDOW_HEIGHT/2 - BOUDARY_SENSING_THRESHOLD) 
			boundaryAcc.x = -maxSpeed;
		if (location.y < (-MusicSwarm.WINDOW_HEIGHT/2) + BOUDARY_SENSING_THRESHOLD)
			boundaryAcc.y = maxSpeed;	
		else if (location.y > MusicSwarm.WINDOW_HEIGHT/2 - BOUDARY_SENSING_THRESHOLD) 
			boundaryAcc.y = -maxSpeed;	  
		if (location.z < (-MusicSwarm.WINDOW_HEIGHT/2) + BOUDARY_SENSING_THRESHOLD)
			boundaryAcc.z = maxSpeed;	
		else if (location.z > MusicSwarm.WINDOW_DEPTH/2 - BOUDARY_SENSING_THRESHOLD) 
			boundaryAcc.z = -maxSpeed;	     
		nextVelocity.add(boundaryAcc);     

	}




	// this seems like a less successful version of the nice 
	// behavior handcrafted at PERFORMAMATICS Workshop (January, 2013);
	void calcNewVelocityPlayful(Flock[] allFlocks) {

		// the new acceleration
		PVector acceleration = new PVector(0.0f,0.0f,0.0f);

		// need sum of locations of Boids in the neighborhood for acceleration due to cohesion,
		// since cohesion = acceleration toward the average location of Boids in the neighborhood,
		// but for this behavior a Boid only coheres with Boids in its own flock
		PVector sumNeighborLocationsOwnFlock = new PVector(0,0,0);   
		
		// need sum of velocities of Boids in the neighborhood for acceleration due to alignment,
		// since alignment = acceleration toward the average velocity of Boids in the neighborhood,
		// but for this behavior a Boid only aligns with Boids in its own flock
		PVector sumNeighborVelocitiesOwnFlock = new PVector(0.0f,0.0f,0.0f);

		// so we need to count boids in the boid's own flock as well as boids in all the flocks 
		// (in the neighborhood)
		int numNeighborsOwnFlock = 0;
		int numNeighborsAllFlocks = 0;

		// need to get info on all Boids in all Flocks
		for (int nextFlockID = 1 ; nextFlockID < allFlocks.length ; nextFlockID++) {

			synchronized(allFlocks[nextFlockID].boids) {

				// go to next iteration of the loop if this flock is empty
				if (allFlocks[nextFlockID].flockEmpty())
					continue;

				List<Boid> boids = allFlocks[nextFlockID].boids;

				// get info for all Boids in this Flock
				for (int i = 0 ; i < boids.size(); i++) {

					Boid otherBoid = (Boid) boids.get(i);
					float dist = location.dist(otherBoid.location);

					// dist > 0 so that a boid does not count itself as another boid in the neighborhood
					if (dist > 0 && dist <= neighborRadius) {

						// for cohesion and alignment, but only for boids in the same flock
						if (nextFlockID == flockID) {
							sumNeighborLocationsOwnFlock.add(otherBoid.location); 
							sumNeighborVelocitiesOwnFlock.add(otherBoid.velocity);
						}

						// calculate and weight vector pointing away from neighbor, for all boids in all flocks
						PVector vectorToThisBoid = PVector.sub(location, otherBoid.location);
						vectorToThisBoid.div(dist*dist);  
						vectorToThisBoid.mult(separationWeight);
						acceleration.add(vectorToThisBoid);  

						// keep count of boids in the same flock
						if (nextFlockID == flockID)
							++numNeighborsOwnFlock; 

						// also keep count of neighbors in all flocks
						++numNeighborsAllFlocks;

						// draw connected components
						if (flockID == nextFlockID)
							connectBoids(otherBoid);

						// report proximity events to osc
						if (dist < proximityThreshold)
							reportCollision(otherBoid);

					}
				} 
			}
		}

		// a boid coheres and aligns with others ONLY IN ITS OWN FLOCK 
		if (numNeighborsOwnFlock > 0) {
			// cohesion steering: steer in the direction of the average location of your neighbors
			PVector cohesionVector = PVector.div(sumNeighborLocationsOwnFlock, (float) numNeighborsOwnFlock);        
			cohesionVector.sub(location);
			cohesionVector.mult(cohesionWeight);
			acceleration.add(cohesionVector);

			// alignment steering: steer so as to align your velocity with the average velocity of your neighbors
			PVector alignmentVector = PVector.div(sumNeighborVelocitiesOwnFlock, (float) numNeighborsOwnFlock);
			alignmentVector.sub(velocity);
			alignmentVector.mult(alignmentWeight);
			acceleration.add(alignmentVector);
		}

		// behavior mechanism
		if (MusicSwarm.timeStep % 100 == 0) {
			if (numNeighborsOwnFlock < 5) {
				neighborRadius += 10;
				if (neighborRadius > 500)
					neighborRadius = 500;
				cohesionWeight = 0.9f;
			}
			else {
				neighborRadius -= 10;			
				if (neighborRadius < 0)
					neighborRadius = 0;
				cohesionWeight = 0.1f;
			}
			//			if (numNeighborsAllFlocks < 2) {
			//				flock.addNewBoid(location.x, location.y, location.z, 1);
			//			}
			//			else if (numNeighborsAllFlocks > 10){
			//				flock.setFlockSize(flock.size()-1);
			//			}
		}



		// with the probability specified by the parameter randomMotionProbability, introduce a small
		// random perturbation (magnitude defined by RANDOM_MOTION_DIMENSION_MAGNITUDE) into each 
		// acceleration component
		if (MusicSwarm.rand.nextFloat() < randomMotionProbability) {
			acceleration.add(new PVector(MusicSwarm.rand.nextFloat() * (2.0f * RANDOM_MOTION_DIMENSION_MAGNITUDE) - RANDOM_MOTION_DIMENSION_MAGNITUDE, 
					MusicSwarm.rand.nextFloat() * (2.0f * RANDOM_MOTION_DIMENSION_MAGNITUDE) - RANDOM_MOTION_DIMENSION_MAGNITUDE,
					MusicSwarm.rand.nextFloat() * (2.0f * RANDOM_MOTION_DIMENSION_MAGNITUDE) - RANDOM_MOTION_DIMENSION_MAGNITUDE));
		} 

		// update velocity
		nextVelocity.add(acceleration);

		// add wind
		nextVelocity.add(MusicSwarm.windVector);

		// make sure we don't exceed maxSpeed
		if (nextVelocity.mag() > maxSpeed) {
			float normalizationFactor = maxSpeed / nextVelocity.mag();
			nextVelocity.mult(normalizationFactor);
		}

		// pacekeeping (stick to normalSpeed to the extent indicated by pacekeepingWeight)
		PVector pacekeeping = 
			PVector.mult(nextVelocity, ((normalSpeed - nextVelocity.mag()) / nextVelocity.mag() * pacekeepingWeight));
		nextVelocity.add(pacekeeping);

		// bounce back from the boundaries of the space
		PVector boundaryAcc = new PVector(0,0,0);
		if (location.x < (-MusicSwarm.WINDOW_HEIGHT/2) + BOUDARY_SENSING_THRESHOLD)         
			boundaryAcc.x = maxSpeed;	
		else if (location.x > MusicSwarm.WINDOW_HEIGHT/2 - BOUDARY_SENSING_THRESHOLD) 
			boundaryAcc.x = -maxSpeed;
		if (location.y < (-MusicSwarm.WINDOW_HEIGHT/2) + BOUDARY_SENSING_THRESHOLD)
			boundaryAcc.y = maxSpeed;	
		else if (location.y > MusicSwarm.WINDOW_HEIGHT/2 - BOUDARY_SENSING_THRESHOLD) 
			boundaryAcc.y = -maxSpeed;	  
		if (location.z < (-MusicSwarm.WINDOW_HEIGHT/2) + BOUDARY_SENSING_THRESHOLD)
			boundaryAcc.z = maxSpeed;	
		else if (location.z > MusicSwarm.WINDOW_DEPTH/2 - BOUDARY_SENSING_THRESHOLD) 
			boundaryAcc.z = -maxSpeed;	     
		nextVelocity.add(boundaryAcc);     

		// velocity scale from Max
		nextVelocity.mult(velocityScale);     

	}



	
	// there is a single flock
	// the single prey is boid 0;
	// the rest of the boids are the predators
	// to calculate motions of Boids and detect proximity events
	void calcNewVelocityPredatorPrey(Flock[] allFlocks) {

		// we want these parameters to have these values regardless of how they started out
		maxSpeed = 20.0f;              
		normalSpeed = 18.0f;
		pacekeepingWeight = 1.0f;//1.0f;//0.5f;

		// the new acceleration
		PVector acceleration = new PVector(0.0f,0.0f,0.0f);

		// need sum of locations of Boids in the neighborhood for acceleration due to cohesion,
		// since cohesion = acceleration toward the average location of Boids in the neighborhood,
		// but for this behavior a Boid only coheres with Boids in its own flock
		PVector sumNeighborLocationsOwnFlock = new PVector(0,0,0);   
		
		// need sum of velocities of Boids in the neighborhood for acceleration due to alignment,
		// since alignment = acceleration toward the average velocity of Boids in the neighborhood,
		// but for this behavior a Boid only aligns with Boids in its own flock
		PVector sumNeighborVelocitiesOwnFlock = new PVector(0.0f,0.0f,0.0f);

		// so we need to count boids in the boid's own flock as well as boids in all the flocks 
		// (in the neighborhood)
		int numNeighborsOwnFlock = 0;
		int numNeighborsAllFlocks = 0;

		// will need this later to create a vector toward the prey for all the predators
		Boid preyBoid = null;


		// need to get info on all Boids in all Flocks in the neighborhood
		for (int nextFlockID = 1 ; nextFlockID < allFlocks.length ; nextFlockID++) {

			synchronized(allFlocks[nextFlockID].boids) {

				// go to next iteration of the loop if this flock is empty
				if (allFlocks[nextFlockID].flockEmpty())
					continue;

				List<Boid> boids = allFlocks[nextFlockID].boids;

				// get info for all Boids in this Flock
				for (int i = 0 ; i < boids.size(); i++) {

					Boid otherBoid = (Boid) boids.get(i);
					float dist = location.dist(otherBoid.location);

					// save a reference to the prey
					if (i == 0)
						preyBoid = otherBoid;

					// dist > 0 so that a boid does not count itself as another boid in the neighborhood
					if (dist > 0 && dist <= neighborRadius) {

						// for cohesion and alignment, but only for boids in the same flock
						if (nextFlockID == flockID) {
							sumNeighborLocationsOwnFlock.add(otherBoid.location); 
							sumNeighborVelocitiesOwnFlock.add(otherBoid.velocity);
						}

						// calculate and weight vector pointing away from neighbor, for all boids in all flocks
						PVector vectorToThisBoid = PVector.sub(location, otherBoid.location);
						vectorToThisBoid.div(dist*dist);  
						// prey boid (boid 0) should separate as strongly as possible from all other boids
						//					if (boidID == 0)
						//						vectorToThisBoid.mult(10.0f);
						//					else
						vectorToThisBoid.mult(separationWeight);
						acceleration.add(vectorToThisBoid);  

						// keep count of boids in the same flock
						if (nextFlockID == flockID)
							++numNeighborsOwnFlock; 

						// also keep count of neighbors in all flocks
						++numNeighborsAllFlocks;

						// draw lines between Boids from the same flock in the same neighborhood
						if (flockID == nextFlockID)
							connectBoids(otherBoid);

						// report proximity events to osc
						if (dist < proximityThreshold)
							reportCollision(otherBoid);

					}

				}
			}
		}

		// only for predators:
		if (boidID != 0) {

			// only predators cohere and align
			if (numNeighborsOwnFlock > 0) {
				// cohesion steering: steer in the direction of the average location of your neighbors
				PVector cohesionVector = PVector.div(sumNeighborLocationsOwnFlock, (float) numNeighborsOwnFlock);        
				cohesionVector.sub(location);
				cohesionVector.mult(cohesionWeight);
				acceleration.add(cohesionVector);

				// alignment steering: steer so as to align your velocity with the average velocity of your neighbors
				PVector alignmentVector = PVector.div(sumNeighborVelocitiesOwnFlock, (float) numNeighborsOwnFlock);
				alignmentVector.sub(velocity);
				alignmentVector.mult(alignmentWeight);
				acceleration.add(alignmentVector);
			}

			// add a vector to the predators that accelerates them toward the prey (boid 0)
			// preyBoid was found in the loop above that goes through all the boids
			//
			// turns out to be really important to make this vector have a *very* small
			// magnitude; otherwise the predators are drawn so strongly to the prey
			// that they end up forming a tight clump that follows the prey even if
			// they have max separation and no cohesion or alignment with each other
			PVector toPrey = PVector.sub(preyBoid.location, location);
			toPrey.mult(0.001f);
			acceleration.add(toPrey);

		}


		// with the probability specified by the parameter randomMotionProbability, introduce a small
		// random perturbation (magnitude defined by RANDOM_MOTION_DIMENSION_MAGNITUDE) into each 
		// acceleration component
		if (MusicSwarm.rand.nextFloat() < randomMotionProbability) {
			acceleration.add(new PVector(MusicSwarm.rand.nextFloat() * (2.0f * RANDOM_MOTION_DIMENSION_MAGNITUDE) - RANDOM_MOTION_DIMENSION_MAGNITUDE, 
					MusicSwarm.rand.nextFloat() * (2.0f * RANDOM_MOTION_DIMENSION_MAGNITUDE) - RANDOM_MOTION_DIMENSION_MAGNITUDE,
					MusicSwarm.rand.nextFloat() * (2.0f * RANDOM_MOTION_DIMENSION_MAGNITUDE) - RANDOM_MOTION_DIMENSION_MAGNITUDE));
		} 

		// update velocity
		nextVelocity.add(acceleration);

		// add wind
		nextVelocity.add(MusicSwarm.windVector);

		// make sure we don't exceed maxSpeed
		if (nextVelocity.mag() > maxSpeed) {
			float normalizationFactor = maxSpeed / nextVelocity.mag();
			nextVelocity.mult(normalizationFactor);
		}

		// pacekeeping (stick to normalSpeed to the extent indicated by pacekeepingWeight)
		PVector pacekeeping = 
			PVector.mult(nextVelocity, ((normalSpeed - nextVelocity.mag()) / nextVelocity.mag() * pacekeepingWeight));
		nextVelocity.add(pacekeeping);

		// bounce back from the boundaries of the space
		PVector boundaryAcc = new PVector(0,0,0);
		if (location.x < (-MusicSwarm.WINDOW_HEIGHT/2) + BOUDARY_SENSING_THRESHOLD)         
			boundaryAcc.x = maxSpeed;	
		else if (location.x > MusicSwarm.WINDOW_HEIGHT/2 - BOUDARY_SENSING_THRESHOLD) 
			boundaryAcc.x = -maxSpeed;
		if (location.y < (-MusicSwarm.WINDOW_HEIGHT/2) + BOUDARY_SENSING_THRESHOLD)
			boundaryAcc.y = maxSpeed;	
		else if (location.y > MusicSwarm.WINDOW_HEIGHT/2 - BOUDARY_SENSING_THRESHOLD) 
			boundaryAcc.y = -maxSpeed;	  
		if (location.z < (-MusicSwarm.WINDOW_HEIGHT/2) + BOUDARY_SENSING_THRESHOLD)
			boundaryAcc.z = maxSpeed;	
		else if (location.z > MusicSwarm.WINDOW_DEPTH/2 - BOUDARY_SENSING_THRESHOLD) 
			boundaryAcc.z = -maxSpeed;	     
		nextVelocity.add(boundaryAcc);     

		// velocity scale from Max
		// scale up leader boid velocity
		if (boidID == 0)
			nextVelocity.mult(2.0f);     
		else
			nextVelocity.mult(velocityScale);     

	}



	
	// with 50 boids per flock (2 flocks), the green flock behavior is sometimes very interesting --
	// clusters of green boids "roll over each other in a ball" like fish schooling
	// found this while trying to make calcNewVelocityPredatorPreyWholeFlocks work
	//	// default characteristics of flock
	//	float velocityScale = 1.0f;         
	//	float maxSpeed = 12.0f;              
	//	float normalSpeed = 10.0f;
	//	float neighborRadius = 100.0f;        
	//	float separationWeight = 100f;
	//	float alignWeight = 1.0f;//0.25f;
	//	float cohesionWeight = 0.05f;//0.25f;
	//	float pacekeepingWeight = 1.0f;//1.0f;//0.5f;
	//	float randomMotionProbability = 1.0f;//0.2f;  
	//	float proximityThreshold = 10.0f;
	//
	// NOTE: this was developed from the predator/prey behavior and there are still 
	//       references to predator and prey
	void calcNewVelocityFishSchools(Flock[] allFlocks) {

		// we want these parameters to have these values regardless of how they started out
		maxSpeed = 20.0f;              
		normalSpeed = 18.0f;
		pacekeepingWeight = 1.0f;//1.0f;//0.5f;

		// the new acceleration
		PVector acceleration = new PVector(0.0f,0.0f,0.0f);

		// need sum of locations of Boids in the neighborhood for acceleration due to cohesion,
		// since cohesion = acceleration toward the average location of Boids in the neighborhood,
		// but for this behavior a Boid only coheres with Boids in its own flock
		PVector sumNeighborLocationsOwnFlock = new PVector(0,0,0);   
		
		// need sum of velocities of Boids in the neighborhood for acceleration due to alignment,
		// since alignment = acceleration toward the average velocity of Boids in the neighborhood,
		// but for this behavior a Boid only aligns with Boids in its own flock
		PVector sumNeighborVelocitiesOwnFlock = new PVector(0.0f,0.0f,0.0f);

		// but we also need these for boids in all the flocks (in the neighborhood)
		// for cohesion
		PVector sumNeighborLocationsAllFlocks = new PVector(0,0,0);   
		// for alignment
		PVector sumNeighborVelocitiesAllFlocks = new PVector(0.0f,0.0f,0.0f);

		int numNeighborsOwnFlock = 0;
		int numNeighborsAllFlocks = 0;

		// need to get info on all Boids in all Flocks in the neighborhood
		for (int nextFlockID = 1 ; nextFlockID < allFlocks.length ; nextFlockID++) {

			synchronized(allFlocks[nextFlockID].boids) {

				// go to next iteration of the loop if this flock is empty
				if (allFlocks[nextFlockID].flockEmpty())
					continue;

				List<Boid> boids = allFlocks[nextFlockID].boids;

				// get info for all Boids in this Flock
				for (int i = 0 ; i < boids.size(); i++) {

					Boid otherBoid = (Boid) boids.get(i);
					float dist = location.dist(otherBoid.location);

					// find prey boid
					if (otherBoid.boidID == myPreyID)
						preyBoid = otherBoid;

					// dist > 0 so that a boid does not count itself as another boid in the neighborhood
					if (dist > 0 && dist <= neighborRadius) {

						// for cohesion and alignment with boids in the same flock
						if (nextFlockID == flockID) {
							sumNeighborLocationsOwnFlock.add(otherBoid.location); 
							sumNeighborVelocitiesOwnFlock.add(otherBoid.velocity);
						}
						
						// for cohesion and alignment with all boids 
						sumNeighborLocationsAllFlocks.add(otherBoid.location); 
						sumNeighborVelocitiesAllFlocks.add(otherBoid.velocity);						

						// calculate and weight vector pointing away from neighbor, for all boids in all flocks,
						// but weight it differently for certain pairs of boids
						PVector vectorToThisBoid = PVector.sub(location, otherBoid.location);
						vectorToThisBoid.div(dist*dist);  
						if (flockID == 1 && nextFlockID == 2) {
							vectorToThisBoid.mult(50);  //100
							acceleration.add(vectorToThisBoid);  
						}
						else {
							vectorToThisBoid.mult(50);
							acceleration.add(vectorToThisBoid);  
						}

						// keep count of boids in the same flock
						if (nextFlockID == flockID)
							++numNeighborsOwnFlock; 

						// also keep count of neighbors in all flocks
						++numNeighborsAllFlocks;

						// draw lines between Boids from the same flock in the same neighborhood
						if (flockID == nextFlockID)
							connectBoids(otherBoid);

						// report proximity events to osc
						if (dist < proximityThreshold)
							reportCollision(otherBoid);

					}

				}
			}
		}

		// cohesion and alignment for flock 1
		if (numNeighborsOwnFlock > 0 && flockID == 1) {
			// cohesion steering: steer in the direction of the average location of your neighbors
			PVector cohesionVector = PVector.div(sumNeighborLocationsOwnFlock, (float) numNeighborsOwnFlock);        
			cohesionVector.sub(location);
			cohesionVector.mult(0.1f);
			acceleration.add(cohesionVector);

			// alignment steering: steer so as to align your velocity with the average velocity of your neighbors
			PVector alignmentVector = PVector.div(sumNeighborVelocitiesOwnFlock, (float) numNeighborsOwnFlock);
			alignmentVector.sub(velocity);
			alignmentVector.mult(0.1f);
			acceleration.add(alignmentVector);
		}

		// cohesion and alignment for flock 2
		else if (numNeighborsOwnFlock > 0 && flockID == 2) {
			// cohesion steering: steer in the direction of the average location of your neighbors
			PVector cohesionVector = PVector.div(sumNeighborLocationsAllFlocks, (float) numNeighborsAllFlocks);        
			cohesionVector.sub(location);
			cohesionVector.mult(0.1f);
			acceleration.add(cohesionVector);

			// alignment steering: steer so as to align your velocity with the average velocity of your neighbors
			PVector alignmentVector = PVector.div(sumNeighborVelocitiesAllFlocks, (float) numNeighborsAllFlocks);
			alignmentVector.sub(velocity);
			alignmentVector.mult(0.5f);
			acceleration.add(alignmentVector);
		}

		// add a vector to the boids in flcok 2 that accelerates them toward the  
		// preyBoid that was found in the loop above that goes through all the boids
		if (flockID == 2) {
			if (preyBoid != null) {
				PVector toPrey = PVector.sub(preyBoid.location, location);
				toPrey.mult(0.1f);
				acceleration.add(toPrey);
			}
		}


		// with the probability specified by the parameter randomMotionProbability, introduce a small
		// random perturbation (magnitude defined by RANDOM_MOTION_DIMENSION_MAGNITUDE) into each 
		// acceleration component
		if (MusicSwarm.rand.nextFloat() < randomMotionProbability) {
			acceleration.add(new PVector(MusicSwarm.rand.nextFloat() * (2.0f * RANDOM_MOTION_DIMENSION_MAGNITUDE) - RANDOM_MOTION_DIMENSION_MAGNITUDE, 
					MusicSwarm.rand.nextFloat() * (2.0f * RANDOM_MOTION_DIMENSION_MAGNITUDE) - RANDOM_MOTION_DIMENSION_MAGNITUDE,
					MusicSwarm.rand.nextFloat() * (2.0f * RANDOM_MOTION_DIMENSION_MAGNITUDE) - RANDOM_MOTION_DIMENSION_MAGNITUDE));
		} 

		// update velocity
		nextVelocity.add(acceleration);

		// add wind
		nextVelocity.add(MusicSwarm.windVector);

		// make sure we don't exceed maxSpeed
		if (nextVelocity.mag() > maxSpeed) {
			float normalizationFactor = maxSpeed / nextVelocity.mag();
			nextVelocity.mult(normalizationFactor);
		}

		// pacekeeping (stick to normalSpeed to the extent indicated by pacekeepingWeight)
		PVector pacekeeping = 
			PVector.mult(nextVelocity, ((normalSpeed - nextVelocity.mag()) / nextVelocity.mag() * pacekeepingWeight));
		nextVelocity.add(pacekeeping);

		// bounce back from the boundaries of the space
		PVector boundaryAcc = new PVector(0,0,0);
		if (location.x < (-MusicSwarm.WINDOW_HEIGHT/2) + BOUDARY_SENSING_THRESHOLD)         
			boundaryAcc.x = maxSpeed;	
		else if (location.x > MusicSwarm.WINDOW_HEIGHT/2 - BOUDARY_SENSING_THRESHOLD) 
			boundaryAcc.x = -maxSpeed;
		if (location.y < (-MusicSwarm.WINDOW_HEIGHT/2) + BOUDARY_SENSING_THRESHOLD)
			boundaryAcc.y = maxSpeed;	
		else if (location.y > MusicSwarm.WINDOW_HEIGHT/2 - BOUDARY_SENSING_THRESHOLD) 
			boundaryAcc.y = -maxSpeed;	  
		if (location.z < (-MusicSwarm.WINDOW_HEIGHT/2) + BOUDARY_SENSING_THRESHOLD)
			boundaryAcc.z = maxSpeed;	
		else if (location.z > MusicSwarm.WINDOW_DEPTH/2 - BOUDARY_SENSING_THRESHOLD) 
			boundaryAcc.z = -maxSpeed;	     
		nextVelocity.add(boundaryAcc);     

		// velocity scale from Max
		// scale up leader boid velocity
		//		if (MusicSwarm.timeStep > startStep && MusicSwarm.timeStep < endStep && boidID == 0)
		//			nextVelocity.mult(2.0f);     
		//		else
		nextVelocity.mult(0.5f);     

	}

	

	
	// with 50 boids per flock (2 flocks), the green boids become a snake-like cluster
	// found this while trying to make calcNewVelocityPredatorPreyWholeFlocks work
	//	// default characteristics of flock
	//	float velocityScale = 1.0f;         
	//	float maxSpeed = 12.0f;              
	//	float normalSpeed = 10.0f;
	//	float neighborRadius = 100.0f;        
	//	float separationWeight = 100f;
	//	float alignWeight = 1.0f;//0.25f;
	//	float cohesionWeight = 0.05f;//0.25f;
	//	float pacekeepingWeight = 1.0f;//1.0f;//0.5f;
	//	float randomMotionProbability = 1.0f;//0.2f;  
	//	float proximityThreshold = 10.0f;
	//
	// NOTE: this was developed from the predator/prey behavior and there are still 
	//       references to predator and prey
	void calcNewVelocityGreenSnake(Flock[] allFlocks) {

		// we want these parameters to have these values regardless of how they started out;
		// at one point, they were different for the two flocks....
		if (flockID == 1) {
			maxSpeed = 20.0f;              
			normalSpeed = 18.0f;
			pacekeepingWeight = 1.0f;//1.0f;//0.5f;			
		}
		else if (flockID == 2) {
			maxSpeed = 20.0f;              
			normalSpeed = 18.0f;
			pacekeepingWeight = 1.0f;//1.0f;//0.5f;			
		}

		// the new acceleration
		PVector acceleration = new PVector(0.0f,0.0f,0.0f);

		// need sum of locations of Boids in the neighborhood for acceleration due to cohesion,
		// since cohesion = acceleration toward the average location of Boids in the neighborhood,
		// but for this behavior a Boid only coheres with Boids in its own flock
		PVector sumNeighborLocationsOwnFlock = new PVector(0,0,0);   
		
		// need sum of velocities of Boids in the neighborhood for acceleration due to alignment,
		// since alignment = acceleration toward the average velocity of Boids in the neighborhood,
		// but for this behavior a Boid only aligns with Boids in its own flock
		PVector sumNeighborVelocitiesOwnFlock = new PVector(0.0f,0.0f,0.0f);

		// but we also need these for boids in all the flocks (in the neighborhood)
		// for cohesion
		PVector sumNeighborLocationsAllFlocks = new PVector(0,0,0);   
		// for alignment
		PVector sumNeighborVelocitiesAllFlocks = new PVector(0.0f,0.0f,0.0f);

		int numNeighborsOwnFlock = 0;
		int numNeighborsAllFlocks = 0;

		// need to get info on all Boids in all Flocks in the neighborhood
		for (int nextFlockID = 1 ; nextFlockID < allFlocks.length ; nextFlockID++) {

			synchronized(allFlocks[nextFlockID].boids) {

				// go to next iteration of the loop if this flock is empty
				if (allFlocks[nextFlockID].flockEmpty())
					continue;

				List<Boid> boids = allFlocks[nextFlockID].boids;

				// get info for all Boids in this Flock
				for (int i = 0 ; i < boids.size(); i++) {

					Boid otherBoid = (Boid) boids.get(i);
					float dist = location.dist(otherBoid.location);

					// find prey boid
					if (flockID == 2 && otherBoid.boidID == myPreyID)
						preyBoid = otherBoid;

					// dist > 0 so that a boid does not count itself as another boid in the neighborhood
					if (dist > 0 && dist <= neighborRadius) {

						// for cohesion and alignment with boids in the same flock
						if (nextFlockID == flockID) {
							sumNeighborLocationsOwnFlock.add(otherBoid.location); 
							sumNeighborVelocitiesOwnFlock.add(otherBoid.velocity);
						}
						
						// for cohesion and alignment with all boids 
						sumNeighborLocationsAllFlocks.add(otherBoid.location); 
						sumNeighborVelocitiesAllFlocks.add(otherBoid.velocity);						

						// calculate and weight vector pointing away from neighbor, for all boids in all flocks,
						// but weight it differently for certain pairs of boids
						PVector vectorToThisBoid = PVector.sub(location, otherBoid.location);
						vectorToThisBoid.div(dist*dist);  
						if (flockID == 1 && nextFlockID == 2) {
							vectorToThisBoid.mult(100);
							acceleration.add(vectorToThisBoid);  
						}
						else {
							vectorToThisBoid.mult(80);
							acceleration.add(vectorToThisBoid);  
						}

						// keep count of boids in the same flock
						if (nextFlockID == flockID)
							++numNeighborsOwnFlock; 

						// also keep count of neighbors in all flocks
						++numNeighborsAllFlocks;

						// draw lines between Boids from the same flock in the same neighborhood
						if (flockID == nextFlockID)
							connectBoids(otherBoid);

						// report proximity events to osc
						if (dist < proximityThreshold)
							reportCollision(otherBoid);

					}

				}
			}
		}


		// cohesion and alignment for flock 1
		if (numNeighborsOwnFlock > 0 && flockID == 1) {
			// cohesion steering: steer in the direction of the average location of your neighbors
			PVector cohesionVector = PVector.div(sumNeighborLocationsOwnFlock, (float) numNeighborsOwnFlock);        
			cohesionVector.sub(location);
			cohesionVector.mult(0.1f);
			acceleration.add(cohesionVector);

			// alignment steering: steer so as to align your velocity with the average velocity of your neighbors
			PVector alignmentVector = PVector.div(sumNeighborVelocitiesOwnFlock, (float) numNeighborsOwnFlock);
			alignmentVector.sub(velocity);
			alignmentVector.mult(0.1f);
			acceleration.add(alignmentVector);
		}
		
		// cohesion and alignment for flock 2
		else if (numNeighborsOwnFlock > 0 && flockID == 2) {
			// cohesion steering: steer in the direction of the average location of your neighbors
			PVector cohesionVector = PVector.div(sumNeighborLocationsAllFlocks, (float) numNeighborsAllFlocks);        
			cohesionVector.sub(location);
			cohesionVector.mult(0.1f);
			acceleration.add(cohesionVector);

			// alignment steering: steer so as to align your velocity with the average velocity of your neighbors
			PVector alignmentVector = PVector.div(sumNeighborVelocitiesAllFlocks, (float) numNeighborsAllFlocks);
			alignmentVector.sub(velocity);
			alignmentVector.mult(0.5f);
			acceleration.add(alignmentVector);
		}

		// add a vector to the boids in flock 2 that accelerates them toward the 
		// preyBoid was found in the loop above that goes through all the boids
		// NOTE: the vector must be quite large
		if (flockID == 2) {
			if (preyBoid != null) {
				PVector toPrey = PVector.sub(preyBoid.location, location);
				toPrey.mult(10.0f);
				acceleration.add(toPrey);
			}
		}


		// with the probability specified by the parameter randomMotionProbability, introduce a small
		// random perturbation (magnitude defined by RANDOM_MOTION_DIMENSION_MAGNITUDE) into each 
		// acceleration component
		if (MusicSwarm.rand.nextFloat() < randomMotionProbability) {
			acceleration.add(new PVector(MusicSwarm.rand.nextFloat() * (2.0f * RANDOM_MOTION_DIMENSION_MAGNITUDE) - RANDOM_MOTION_DIMENSION_MAGNITUDE, 
					MusicSwarm.rand.nextFloat() * (2.0f * RANDOM_MOTION_DIMENSION_MAGNITUDE) - RANDOM_MOTION_DIMENSION_MAGNITUDE,
					MusicSwarm.rand.nextFloat() * (2.0f * RANDOM_MOTION_DIMENSION_MAGNITUDE) - RANDOM_MOTION_DIMENSION_MAGNITUDE));
		} 

		// update velocity
		nextVelocity.add(acceleration);

		// add wind
		nextVelocity.add(MusicSwarm.windVector);

		// make sure we don't exceed maxSpeed
		if (nextVelocity.mag() > maxSpeed) {
			float normalizationFactor = maxSpeed / nextVelocity.mag();
			nextVelocity.mult(normalizationFactor);
		}

		// pacekeeping (stick to normalSpeed to the extent indicated by pacekeepingWeight)
		PVector pacekeeping = 
			PVector.mult(nextVelocity, ((normalSpeed - nextVelocity.mag()) / nextVelocity.mag() * pacekeepingWeight));
		nextVelocity.add(pacekeeping);

		// bounce back from the boundaries of the space
		PVector boundaryAcc = new PVector(0,0,0);
		if (location.x < (-MusicSwarm.WINDOW_HEIGHT/2) + BOUDARY_SENSING_THRESHOLD)         
			boundaryAcc.x = maxSpeed;	
		else if (location.x > MusicSwarm.WINDOW_HEIGHT/2 - BOUDARY_SENSING_THRESHOLD) 
			boundaryAcc.x = -maxSpeed;
		if (location.y < (-MusicSwarm.WINDOW_HEIGHT/2) + BOUDARY_SENSING_THRESHOLD)
			boundaryAcc.y = maxSpeed;	
		else if (location.y > MusicSwarm.WINDOW_HEIGHT/2 - BOUDARY_SENSING_THRESHOLD) 
			boundaryAcc.y = -maxSpeed;	  
		if (location.z < (-MusicSwarm.WINDOW_HEIGHT/2) + BOUDARY_SENSING_THRESHOLD)
			boundaryAcc.z = maxSpeed;	
		else if (location.z > MusicSwarm.WINDOW_DEPTH/2 - BOUDARY_SENSING_THRESHOLD) 
			boundaryAcc.z = -maxSpeed;	     
		nextVelocity.add(boundaryAcc);     

		// velocity scale from Max
		// scale up leader boid velocity
		//		if (MusicSwarm.timeStep > startStep && MusicSwarm.timeStep < endStep && boidID == 0)
		//			nextVelocity.mult(2.0f);     
		//		else
		nextVelocity.mult(0.5f);     

	}



	
	
	// let Max know that two Boids are within a certain distance of each other; 
	// that distance is proximityThreshold and the check is made in the
	// calcNewVelocity method(s) to avoid mkaing unecessary method calls
	void reportCollision(Boid otherBoid) {
		
			OscMessage myMessage = new OscMessage("/collision");
			myMessage.add(location.x);                   // add the x coordinate
			myMessage.add(MusicSwarm.WINDOW_WIDTH);      // add the width for proper x scaling
			myMessage.add(location.y);                   // add the y coordinate
			myMessage.add(MusicSwarm.WINDOW_HEIGHT);     // add the height for proper y scaling
			myMessage.add(flockID);
			myMessage.add(otherBoid.flockID);
			oscP5.send(myMessage, myRemoteLocation); 
	}



	// draw a line between two boids;
	// the method is called in the calcNewVelocity method(s),
	// but only if the two boids are in the same neighborhood and in ths same flock
	void connectBoids(Boid otherBoid) {

		PVector otherBoidLocation;
		if (MusicSwarm.CONNECTED_COMPONENTS) {
			otherBoidLocation = otherBoid.location;
			if (MusicSwarm.renderMethod == MusicSwarm.RENDER_3D) {
				parent.line(location.x,location.y, location.z, otherBoidLocation.x,otherBoidLocation.y, otherBoidLocation.z);
				parent.stroke(MusicSwarm.FLOCKCOLOR[flockID][0],MusicSwarm.FLOCKCOLOR[flockID][1],MusicSwarm.FLOCKCOLOR[flockID][2]);	
			}
			if (MusicSwarm.renderMethod == MusicSwarm.RENDER_2D) {
				parent.line(location.x,location.y, otherBoidLocation.x,otherBoidLocation.y);
				parent.stroke(MusicSwarm.FLOCKCOLOR[flockID][0],MusicSwarm.FLOCKCOLOR[flockID][1],MusicSwarm.FLOCKCOLOR[flockID][2]);	
			}
		}
	}




//	// analyze flock statistics; has been moved to Flock class (05/20/13)
//	void flockAnalysis(Flock thisFlock){
//
//		PVector velocitySum = new PVector(0.0f, 0.0f, 0.0f);
//		PVector locationSum = new PVector(0.0f, 0.0f, 0.0f);
//		float velocityMagnitudeSum = 0.0f;
//
//		synchronized(thisFlock.boids) {
//
//			List boids = thisFlock.boids;
//			int boidsSize = boids.size();
//
//			for (int i = 0 ; i < boidsSize; i++) {
//				Boid boid = (Boid) boids.get(i);
//				locationSum.add(boid.location);
//				velocitySum.add(boid.velocity);
//				velocityMagnitudeSum = velocityMagnitudeSum + boid.velocity.mag();	
//			}
//
//			PVector locationMean = new PVector(0.0f, 0.0f, 0.0f);
//			PVector locationAveDeviation = new PVector(0.0f, 0.0f, 0.0f);
//			float locationDeviationX = 0.0f;
//			float locationDeviationY = 0.0f;
//			float locationDeviationZ = 0.0f;
//
//			PVector velocityMean = new PVector(0.0f, 0.0f, 0.0f);
//			PVector velocityAveDeviation = new PVector(0.0f, 0.0f, 0.0f); 
//			float velocityDeviationX = 0.0f;
//			float velocityDeviationY = 0.0f;
//			float velocityDeviationZ = 0.0f;
//			float velocityMagnitudeMean = 0.0f;
//			float velocityMagnitudeDeviation = 0.0f;
//
//			// find mean location and mean velocity for the flock
//			locationMean = locationSum;
//			locationMean.div(boidsSize);
//
//			velocityMean = velocitySum;
//			velocityMean.div(boidsSize);
//			velocityMagnitudeMean = velocityMagnitudeSum / boidsSize;
//
//			// find average deviation from mean location and velocity
//			for (int i = 0 ; i < boidsSize; i++) {
//				Boid boid = (Boid) boids.get(i);
//				//accumulate sum of deviations in this loop
//				locationDeviationX += PApplet.abs(boid.location.x - locationMean.x);
//				locationDeviationY += PApplet.abs(boid.location.y - locationMean.y);
//				locationDeviationZ += PApplet.abs(boid.location.z - locationMean.z);
//
//				velocityDeviationX += PApplet.abs(boid.velocity.x - velocityMean.x);
//				velocityDeviationY += PApplet.abs(boid.velocity.y - velocityMean.y);
//				velocityDeviationZ += PApplet.abs(boid.velocity.z - velocityMean.z);
//				velocityMagnitudeDeviation += PApplet.abs(boid.velocity.mag() - velocityMagnitudeMean);
//			}
//
//			//divide by boidSize - number of boids in flock - to get the average deviation
//			locationDeviationX /= boidsSize;
//			locationDeviationY /= boidsSize;
//			locationDeviationZ /= boidsSize;
//			locationAveDeviation.set(locationDeviationX, locationDeviationY, locationDeviationZ); 
//
//			velocityDeviationX /= boidsSize;
//			velocityDeviationY /= boidsSize;
//			velocityDeviationZ /= boidsSize;
//			velocityAveDeviation.set(velocityDeviationX, velocityDeviationY, velocityDeviationY);
//			velocityMagnitudeDeviation /= boidsSize;
//
//			//send stats to osc
//			// NEED TO SEND Z AXIS DATA TO OSC !!!!!
//			OscMessage AnalysisMessage = new OscMessage("/FlockAnalysis");
//			AnalysisMessage.add(flockID);
//			AnalysisMessage.add(locationMean.x);
//			AnalysisMessage.add(locationMean.y);
//			AnalysisMessage.add(locationMean.z);
//			AnalysisMessage.add(locationAveDeviation.x);
//			AnalysisMessage.add(locationAveDeviation.y);
//			AnalysisMessage.add(locationAveDeviation.z);
//
//			AnalysisMessage.add(velocityMean.x);
//			AnalysisMessage.add(velocityMean.y);
//			AnalysisMessage.add(velocityMean.z);
//			AnalysisMessage.add(velocityAveDeviation.x);
//			AnalysisMessage.add(velocityAveDeviation.y);
//			AnalysisMessage.add(velocityAveDeviation.z);
//			AnalysisMessage.add(velocityMagnitudeMean);
//			AnalysisMessage.add(velocityMagnitudeDeviation);
//
//			oscP5.send(AnalysisMessage, myRemoteLocation); 
//
//		}
//
//	}






	// redraw the boid
	void render() {

		if (MusicSwarm.renderMethod == MusicSwarm.RENDER_2D) {
			// Draw a triangle rotated in the direction of velocity
			float theta = velocity.heading2D() + (float) Math.PI/2;
			parent.noStroke();
			parent.fill(MusicSwarm.FLOCKCOLOR[flockID][0], MusicSwarm.FLOCKCOLOR[flockID][1], MusicSwarm.FLOCKCOLOR[flockID][2], MusicSwarm.B_ALPHA);
			parent.pushMatrix();
			parent.translate(location.x, location.y);
			parent.rotate(theta);
			parent.beginShape(PConstants.TRIANGLES);
			parent.vertex(0, -MusicSwarm.BOID_SIZE*2);
			parent.vertex(-MusicSwarm.BOID_SIZE, MusicSwarm.BOID_SIZE*2);
			parent.vertex(MusicSwarm.BOID_SIZE, MusicSwarm.BOID_SIZE*2);
			parent.endShape();
			parent.popMatrix();
		}

		//3d render
		else if (MusicSwarm.renderMethod == MusicSwarm.RENDER_3D) {
			parent.pushMatrix();
			parent.translate(location.x,location.y,location.z);
			parent.rotateY(PApplet.atan2(-velocity.z,velocity.x));
			parent.rotateZ(PApplet.asin(velocity.y/velocity.mag()));
			parent.noStroke();
			parent.fill(MusicSwarm.FLOCKCOLOR[flockID][0], MusicSwarm.FLOCKCOLOR[flockID][1], MusicSwarm.FLOCKCOLOR[flockID][2], MusicSwarm.B_ALPHA);

			//drawing boids
			parent.beginShape(PConstants.TRIANGLES);

			parent.vertex(3*MusicSwarm.BOID_SIZE,0,0);
			parent.vertex(-3*MusicSwarm.BOID_SIZE,2*MusicSwarm.BOID_SIZE,0);
			parent.vertex(-3*MusicSwarm.BOID_SIZE,-2*MusicSwarm.BOID_SIZE,0);

			parent.vertex(3*MusicSwarm.BOID_SIZE,0,0);
			parent.vertex(-3*MusicSwarm.BOID_SIZE,2*MusicSwarm.BOID_SIZE,0);
			parent.vertex(-3*MusicSwarm.BOID_SIZE,0,2*MusicSwarm.BOID_SIZE);

			parent.vertex(3*MusicSwarm.BOID_SIZE,0,0);
			parent.vertex(-3*MusicSwarm.BOID_SIZE,0,2*MusicSwarm.BOID_SIZE);
			parent.vertex(-3*MusicSwarm.BOID_SIZE,-2*MusicSwarm.BOID_SIZE,0);

			parent.vertex(-3*MusicSwarm.BOID_SIZE,0,2*MusicSwarm.BOID_SIZE);
			parent.vertex(-3*MusicSwarm.BOID_SIZE,2*MusicSwarm.BOID_SIZE,0);
			parent.vertex(-3*MusicSwarm.BOID_SIZE,-2*MusicSwarm.BOID_SIZE,0);

			parent.endShape();
			parent.popMatrix();
		}

	}

	
	// print current parameters of this boid
	public void printParameters() {

		System.out.printf("               ms = %4.1f ns = %4.1f nr = %5.1f se = %5.1f al = %3.1f  co = %3.1f pk = %3.1f rm = %3.1f\n", 
				maxSpeed,             
				normalSpeed,
				neighborRadius,        
				separationWeight,
				alignmentWeight,
				cohesionWeight,
				pacekeepingWeight,
				randomMotionProbability);

	}


	// getters
	
	public PVector getLocation() {
		return location;
	}

	public PVector getVelocity() {
		return velocity;
	}
	
	public int getAge() {
		return age;
	}

	// setters
	
	void set(int ID, float val) {
		switch(ID) {
		case 0:
			if(val > 0.1) this.velocityScale = 0.1f;
			else if(val < 0) this.velocityScale = 0f;
			else this.velocityScale = val;
			break;
		case 1:
			if(val > 10) this.maxSpeed = 10;
			else if( val < 2) this.maxSpeed = 2;
			else this.maxSpeed = val;
			break;
		case 2:
			if(val > maxSpeed) this.normalSpeed = maxSpeed;
			else if (val < 1) this.normalSpeed = 1;
			else this.normalSpeed = val;
			break;
		case 3:
			if(val > 100) this.neighborRadius = 100;
			else if(val < 10) this.neighborRadius = 10;
			else this.neighborRadius = val;
			break;
		case 4:
			if(val > 100) this.separationWeight = 100;
			else if(val < 0) this.separationWeight = 0;
			else this.separationWeight = val;
			break;
		case 5:
			if(val > 1) this.alignmentWeight = 1;
			else if (val < 0) this.alignmentWeight = 0;
			else this.alignmentWeight = val;
			break;
		case 6:
			if(val > 1) this.cohesionWeight = 1;
			else if(val < 0) this.cohesionWeight = 0;
			else this.cohesionWeight = val;
			break;
		case 7:
			if(val > 1) this.pacekeepingWeight = 1;
			else if(val < 0) this.pacekeepingWeight = 0;
			else this.pacekeepingWeight = val;
			break;
		case 8:
			if(val > 0.5) this.randomMotionProbability = 0.5f;
			else if(val < 0) this.randomMotionProbability = 0;
			else this.randomMotionProbability = val;
			break;
		case 9:
			this.numNeighborsOwnFlock = val;
			break;
		case 10:
			this.numNeighborsAllFlocks = val;
			break;
		}
	}
	
	void setLocation(PVector location){   
		this.location = location.get();
	}
	
	public float getMaxSpeed() {
		return maxSpeed;
	}
	public float getNormalSpeed() {
		return normalSpeed;
	}
	public float getNeighborRadius() {
		return neighborRadius;
	}
	public float getCohesionWeight() {
		return cohesionWeight;
	}
	public float getVelocityScale() {
		return velocityScale;
	}
	public float getSeparationWeight() {
		return separationWeight;
	}
	public float getAlignmentWeight() {
		return alignmentWeight;
	}
	public float getPacekeepingWeight() {
		return pacekeepingWeight;
	}
	public float getRandomMotionProbability() {
		return randomMotionProbability;
	}
	public float getProximityThreshold() {
		return proximityThreshold;
	}
	public float getNumNeighborsOwnFlock() {
		return numNeighborsOwnFlock;
	}
	public float getNumNeighborsAllFlock() {
		return numNeighborsAllFlocks;
	}
	
	// setters
	void setVelocityScale(float velocityScale){    
		this.velocityScale = velocityScale;
	}

	void setMaxSpeed(float maxSpeed){    
		this.maxSpeed = maxSpeed;  
	}  

	void setNormalSpeed(float normalSpeed){    
		this.normalSpeed = normalSpeed;  
	}

	void setNeighborRadius(float neighborRadius){   
		this.neighborRadius = neighborRadius;
	}

	void setSeparationWeight(float separationWeight){    
		this.separationWeight = separationWeight;
	}

	void setAlignWeight(float alignWeight){    
		this.alignmentWeight = alignWeight;
	}

	void setCohesionWeight(float cohesionWeight){    
		this.cohesionWeight = cohesionWeight;
	}

	void setPacekeepingWeight(float pacekeepingWeight){    
		this.pacekeepingWeight = pacekeepingWeight;
	}

	void setRandomMotionProbability(float randomMotionProbability){    
		this.randomMotionProbability = randomMotionProbability;
	}

	void setProximityThresehold(int proximityThreshold){
		this.proximityThreshold = proximityThreshold;
	}
	
}



//*****************************************************************************************************
//*****************************************************************************************************
//*****************************************************************************************************
// CODE IN PROGRESS
//*****************************************************************************************************
//*****************************************************************************************************
//*****************************************************************************************************


// not working
//void calcNewVelocityPredatorPreyWholeFlocks(Flock[] allFlocks) {
//
//	if (flockID == 1) {
//		maxSpeed = 20.0f;              
//		normalSpeed = 18.0f;
//		pacekeepingWeight = 1.0f;//1.0f;//0.5f;		
//		neighborRadius = 500;
//	}
//	else if (flockID == 2) {
//		maxSpeed = 20.0f;              
//		normalSpeed = 18.0f;
//		pacekeepingWeight = 1.0f;//1.0f;//0.5f;		
//		neighborRadius = 200;
//	}
//
//	// the new acceleration
//	PVector acceleration = new PVector(0.0f,0.0f,0.0f);
//
//	// for cohesion
//	PVector sumNeighborLocationsOwnFlock = new PVector(0,0,0);   
//	// for alignment
//	PVector sumNeighborVelocitiesOwnFlock = new PVector(0.0f,0.0f,0.0f);
//
//	// for cohesion
//	PVector sumNeighborLocationsAllFlocks = new PVector(0,0,0);   
//	// for alignment
//	PVector sumNeighborVelocitiesAllFlocks = new PVector(0.0f,0.0f,0.0f);
//
//	int numNeighborsOwnFlock = 0;
//	int numNeighborsAllFlocks = 0;
//
//	// need to get info on all Boids in all Flocks
//	for (int nextFlockID = 1 ; nextFlockID < allFlocks.length ; nextFlockID++) {
//
//		synchronized(allFlocks[nextFlockID].boids) {
//
//			// go to next iteration of the loop if this flock is empty
//			if (allFlocks[nextFlockID].flockEmpty())
//				continue;
//
//			List boids = allFlocks[nextFlockID].boids;
//
//			// get info for all Boids in this Flock
//			for (int i = 0 ; i < boids.size(); i++) {
//
//				Boid otherBoid = (Boid) boids.get(i);
//				float dist = location.dist(otherBoid.location);
//
//				// find prey boid
//				if (flockID == 2 && nextFlockID == 1 && otherBoid.boidID == myPreyID)
//					preyBoid = otherBoid;
//
//				// dist > 0 so that a boid does not count itself as another boid in the neighborhood
//				if (dist > 0 && dist <= neighborRadius) {
//
//					// for cohesion and alignment, but only for boids in the same flock
//					if (nextFlockID == flockID) {
//						sumNeighborLocationsOwnFlock.add(otherBoid.location); 
//						sumNeighborVelocitiesOwnFlock.add(otherBoid.velocity);
//					}
//					sumNeighborLocationsAllFlocks.add(otherBoid.location); 
//					sumNeighborVelocitiesAllFlocks.add(otherBoid.velocity);						
//
//					// calculate and weight vector pointing away from neighbor, for all boids in all flocks
//					PVector vectorToThisBoid = PVector.sub(location, otherBoid.location);
//					vectorToThisBoid.div(dist*dist);  
//					if (flockID == 1 && nextFlockID == 2) {
//						vectorToThisBoid.mult(200);
//						acceleration.add(vectorToThisBoid);  
//					}
//					else {
//						vectorToThisBoid.mult(80);
//						acceleration.add(vectorToThisBoid);  
//					}
//
//					// keep count of boids in the same flock
//					if (nextFlockID == flockID)
//						++numNeighborsOwnFlock; 
//
//					// also keep count of neighbors in all flocks
//					++numNeighborsAllFlocks;
//
//					// draw connected components
//					if (flockID == nextFlockID)
//						connectBoids(otherBoid);
//
//					// report proximity events to osc
//					if (dist < proximityThreshold)
//						reportCollision(otherBoid);
//
//				}
//
//			}
//		}
//	}
//
//	int startStep = 0;
//	int endStep = 2000000000;
//
//
//	if (numNeighborsOwnFlock > 0 && flockID == 1) {
//		//cohesion steering ------------------------
//		PVector cohesionVector = PVector.div(sumNeighborLocationsOwnFlock, (float) numNeighborsOwnFlock);        
//		cohesionVector.sub(location);
//		cohesionVector.mult(0.1f);
//		acceleration.add(cohesionVector);
//
//		//alignment steering ------------------------
//		PVector alignmentVector = PVector.div(sumNeighborVelocitiesOwnFlock, (float) numNeighborsOwnFlock);
//		alignmentVector.sub(velocity);
//		alignmentVector.mult(0.0f);
//		acceleration.add(alignmentVector);
//	}
//	else if (numNeighborsOwnFlock > 0 && flockID == 2) {
//		//cohesion steering ------------------------
//		PVector cohesionVector = PVector.div(sumNeighborLocationsAllFlocks, (float) numNeighborsAllFlocks);        
//		cohesionVector.sub(location);
//		cohesionVector.mult(0.3f);
//		acceleration.add(cohesionVector);
//
//		//alignment steering ------------------------
//		PVector alignmentVector = PVector.div(sumNeighborVelocitiesAllFlocks, (float) numNeighborsAllFlocks);
//		alignmentVector.sub(velocity);
//		alignmentVector.mult(0.3f);
//		acceleration.add(alignmentVector);
//	}
//
//	// add a vector to the predators that accelerates them toward the prey (boid 0)
//	// preyBoid was found in the loop above that goes through all the boids
//	//
//	// turns out to be really important to make this vector have a *very* small
//	// magnitude; otherwise the predators are drawn so strongly to the prey
//	// that they end up forming a tight clump that follows the prey even if
//	// they have max separation and no cohesion or alignment with each other
//	if (flockID == 2) {
//		if (preyBoid != null && MusicSwarm.timeStep > startStep && MusicSwarm.timeStep < endStep) {
//			PVector toPrey = PVector.sub(preyBoid.location, location);
//			toPrey.mult(0.1f);
//			acceleration.add(toPrey);
//		}
//	}
//
//
//	// possible random motion component
//	if (MusicSwarm.rand.nextFloat() < randomMotionProbability) {
//		acceleration.add(new PVector(MusicSwarm.rand.nextFloat() * (2.0f * RANDOM_MOTION_DIMENSION_MAGNITUDE) - RANDOM_MOTION_DIMENSION_MAGNITUDE, 
//				MusicSwarm.rand.nextFloat() * (2.0f * RANDOM_MOTION_DIMENSION_MAGNITUDE) - RANDOM_MOTION_DIMENSION_MAGNITUDE,
//				MusicSwarm.rand.nextFloat() * (2.0f * RANDOM_MOTION_DIMENSION_MAGNITUDE) - RANDOM_MOTION_DIMENSION_MAGNITUDE));
//	} 
//
//	// update velocity
//	nextVelocity.add(acceleration);
//
//	//add wind
//	nextVelocity.add(MusicSwarm.windVector);
//
//	// make sure we don't exceed maxSpeed
//	if (nextVelocity.mag() > maxSpeed) {
//		float normalizationFactor = maxSpeed / nextVelocity.mag();
//		nextVelocity.mult(normalizationFactor);
//	}
//
//	// pacekeeping (try to stick to normalSpeed)
//	PVector pacekeeping = 
//		PVector.mult(nextVelocity, ((normalSpeed - nextVelocity.mag()) / nextVelocity.mag() * pacekeepingWeight));
//	nextVelocity.add(pacekeeping);
//
//	// bounce back from boundary
//	PVector boundaryAcc = new PVector(0,0,0);
//	if (location.x < (-MusicSwarm.WINDOW_HEIGHT/2) + BOUDARY_SENSING_THRESHOLD)         
//		boundaryAcc.x = maxSpeed;	
//	else if (location.x > MusicSwarm.WINDOW_HEIGHT/2 - BOUDARY_SENSING_THRESHOLD) 
//		boundaryAcc.x = -maxSpeed;
//	if (location.y < (-MusicSwarm.WINDOW_HEIGHT/2) + BOUDARY_SENSING_THRESHOLD)
//		boundaryAcc.y = maxSpeed;	
//	else if (location.y > MusicSwarm.WINDOW_HEIGHT/2 - BOUDARY_SENSING_THRESHOLD) 
//		boundaryAcc.y = -maxSpeed;	  
//	if (location.z < (-MusicSwarm.WINDOW_HEIGHT/2) + BOUDARY_SENSING_THRESHOLD)
//		boundaryAcc.z = maxSpeed;	
//	else if (location.z > MusicSwarm.WINDOW_DEPTH/2 - BOUDARY_SENSING_THRESHOLD) 
//		boundaryAcc.z = -maxSpeed;	     
//	nextVelocity.add(boundaryAcc);     
//
//	// velocity scale from Max
//	// scale up leader boid velocity
//	//		if (MusicSwarm.timeStep > startStep && MusicSwarm.timeStep < endStep && boidID == 0)
//	//			nextVelocity.mult(2.0f);     
//	//		else
//	nextVelocity.mult(0.5f);     
//
//}




// DOES NOT APPEAR TO BE WORKING
// assumes there are only two flocks and the "leader" is the single boid in flock 1
// to calculate motions of Boids and detect proximity events
//void calcNewVelocityFollowTheLeader(Flock[] allFlocks) {
//
//	// the new acceleration
//	PVector acceleration = new PVector(0.0f,0.0f,0.0f);
//
//	// for cohesion
//	PVector sumNeighborLocations = new PVector(0,0,0);   
//	// for alignment
//	PVector sumNeighborVelocities = new PVector(0.0f,0.0f,0.0f);
//
//	int numNeighbors = 0;
//
//	// single boid in Flock 1 is acting on its own, so just calculate flocking info for Flock 2
//	if (flockID == 2) {
//
//		// need to get info on all Boids in all Flocks
//		for (int nextFlockID = 1 ; nextFlockID < allFlocks.length ; nextFlockID++) {
//
//			synchronized(allFlocks[nextFlockID].boids) {
//
//				// go to next iteration of the loop if this flock is empty
//				if (allFlocks[nextFlockID].flockEmpty())
//					continue;
//
//				List boids = allFlocks[nextFlockID].boids;
//
//				// get info for all Boids in this Flock
//				for (int i = 0 ; i < boids.size(); i++) {
//
//					Boid otherBoid = (Boid) boids.get(i);
//					float dist = location.dist(otherBoid.location);
//
//					// dist > 0 so that a boid does not count itself as another boid in the neighborhood
//					if (dist > 0 && dist <= neighborRadius) {
//
//						if (flockID == 2 && nextFlockID == 1) {
//
//							// for cohesion, 
//							sumNeighborLocations.add(otherBoid.location); 
//
//							// for alignment
//							sumNeighborVelocities.add(otherBoid.velocity);
//
//							// also keep count of neighbors 
//							++numNeighbors;
//						}
//
//						// calculate and weight vector pointing away from neighbor
//						// use separationWeight for boids in Flock 2
//						if (flockID == 2 && nextFlockID == 2) {
//							PVector vectorToThisBoid = PVector.sub(location, otherBoid.location);
//							vectorToThisBoid.div(dist*dist);  
//							vectorToThisBoid.mult(separationWeight);
//							acceleration.add(vectorToThisBoid);  
//						}
//
//						// calculate and weight vector pointing away from neighbor
//						// use maximum separation weight (100) for leader boid in Flock 1
//						if (flockID == 1 && nextFlockID == 2) {
//							PVector vectorToThisBoid = PVector.sub(location, otherBoid.location);
//							vectorToThisBoid.div(dist*dist);  
//							vectorToThisBoid.mult(100);
//							acceleration.add(vectorToThisBoid);  
//						}
//
//						// draw connected components
//						if (flockID == nextFlockID)
//							connectBoids(otherBoid);
//
//						// report proximity events to osc
//						if (dist < proximityThreshold)
//							reportCollision(otherBoid);
//
//					}
//				} 
//			}
//		}
//
//		// 
//		if (numNeighbors > 0) {
//			//cohesion steering ------------------------
//			PVector cohesionVector = PVector.div(sumNeighborLocations, (float) numNeighbors);        
//			cohesionVector.sub(location);
//			cohesionVector.mult(cohesionWeight);
//			acceleration.add(cohesionVector);
//
//			//alignment steering ------------------------
//			PVector alignmentVector = PVector.div(sumNeighborVelocities, (float) numNeighbors);
//			alignmentVector.sub(velocity);
//			alignmentVector.mult(alignmentWeight);
//			acceleration.add(alignmentVector);
//		}
//	}
//
//	// possible random motion component
//	if (MusicSwarm.rand.nextFloat() < randomMotionProbability) {
//		acceleration.add(new PVector(MusicSwarm.rand.nextFloat() * (2.0f * RANDOM_MOTION_DIMENSION_MAGNITUDE) - RANDOM_MOTION_DIMENSION_MAGNITUDE, 
//				MusicSwarm.rand.nextFloat() * (2.0f * RANDOM_MOTION_DIMENSION_MAGNITUDE) - RANDOM_MOTION_DIMENSION_MAGNITUDE,
//				MusicSwarm.rand.nextFloat() * (2.0f * RANDOM_MOTION_DIMENSION_MAGNITUDE) - RANDOM_MOTION_DIMENSION_MAGNITUDE));
//	} 
//
//	// update velocity
//	nextVelocity.add(acceleration);
//
//	//add wind
//	nextVelocity.add(MusicSwarm.windVector);
//
//	// make sure we don't exceed maxSpeed
//	if (nextVelocity.mag() > maxSpeed) {
//		float normalizationFactor = maxSpeed / nextVelocity.mag();
//		nextVelocity.mult(normalizationFactor);
//	}
//
//	// pacekeeping (try to stick to normalSpeed)
//	PVector pacekeeping = 
//		PVector.mult(nextVelocity, ((normalSpeed - nextVelocity.mag()) / nextVelocity.mag() * pacekeepingWeight));
//	nextVelocity.add(pacekeeping);
//
//	// bounce back from boundary
//	PVector boundaryAcc = new PVector(0,0,0);
//	if (location.x < (-MusicSwarm.WINDOW_HEIGHT/2) + BOUDARY_SENSING_THRESHOLD)         
//		boundaryAcc.x = maxSpeed;	
//	else if (location.x > MusicSwarm.WINDOW_HEIGHT/2 - BOUDARY_SENSING_THRESHOLD) 
//		boundaryAcc.x = -maxSpeed;
//	if (location.y < (-MusicSwarm.WINDOW_HEIGHT/2) + BOUDARY_SENSING_THRESHOLD)
//		boundaryAcc.y = maxSpeed;	
//	else if (location.y > MusicSwarm.WINDOW_HEIGHT/2 - BOUDARY_SENSING_THRESHOLD) 
//		boundaryAcc.y = -maxSpeed;	  
//	if (location.z < (-MusicSwarm.WINDOW_HEIGHT/2) + BOUDARY_SENSING_THRESHOLD)
//		boundaryAcc.z = maxSpeed;	
//	else if (location.z > MusicSwarm.WINDOW_DEPTH/2 - BOUDARY_SENSING_THRESHOLD) 
//		boundaryAcc.z = -maxSpeed;	     
//	nextVelocity.add(boundaryAcc);     
//
//	// velocity scale from Max
//	nextVelocity.mult(velocityScale);     
//
//}



