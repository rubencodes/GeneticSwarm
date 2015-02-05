/* 
 * Stephen Majercik
 * Frank Mauceri
 * last updated 20 May 2013
 * 
 * 
 * Each flock is created with either default parameters or randomly generated parameters;
 * note that these parameters can be changed at the individual Boid level, if desired
 * 
 * Methods in this class:
 * 	1) call run method in Boid class to run each boid (update velocity and position, and render)
 * 	2) change the flock size, when necessary, including removing dead boids (if mortality is being used)
 * 	3) calculate some statistics for the flock and send them to Max
 * 		- calculates mean location and velocity for each flock
 * 		- average deviation for location and velocity for each flock
 *	 	- velocity magnitude (mean and average deviation)
 * 
 */


// we use Open Sound Control for communication with Max/MSP
// http://opensoundcontrol.org/
import oscP5.*;
import netP5.*;

// Processing classes 
import processing.core.PApplet;
import processing.core.PVector;

// data structures for the flock
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Random;
import java.util.Vector;


public class Flock {

	// make the Flock a synchronizedList so we can use this as a way to limit access to the Flocks; 
	// see the code in the Flock constructor to see how this is done
	// this is necessary so that the program doesn't crash when Max changes a Flock size in the 
	// middle of Processing doing something to every Boid in a Flock in a for-loop;
	// the underlying data structure that actually holds the Boids is an ArrayList
	private ArrayList<Boid> backingBoids;  
	public List<Boid> boids;
	private int flockID = 0;

	// default flock characteristics 
	private float velocityScale = 1.0f;           // not sure what this is for; have to ask Frank...
	private float maxSpeed = 12.0f;               // speed limit
	private float normalSpeed = 10.0f;            // pacekeeping (see below), when used, tries to keep boid to this speed 
	private float neighborRadius = 200.0f;        // determines which other Boids are neighbors of a given Boid    
	private float separationWeight = 100.0f;      // how strongly do Boids move away from every neighbor: range = [0.0, 100.0]
	private float alignmentWeight = 0.5f;         // how strongly do Boids align match average velocity of their neighbors: range = [0.0, 1.0]
	private float cohesionWeight = 0.5f;          // how strongly do Boids move toward the average position of their neighbors: range = [0.0, 1.0]
	private float pacekeepingWeight = 0.5f;       // how strongly do Boids stick to the normalSpeed: range = [0.0, 1.0]
	private float randomMotionProbability = 0.0f; // probability that a small random component is introduced into the motion (see Boid class for more info)
	private float proximityThreshold = 10.0f;     // distance defining a "collision" between two Boids

	// do boids in this flock have a limited lifespan?
	private static boolean boidMortality = false;
	private static final int BOID_LIFESPAN = 200;

	// there can be a wind that affects the Boids' movements
	PVector wind; 

	// not needed in this class, but must be sent to the Boid
	// constructor; needed for rendering calls in that class
	private PApplet parent;

	private Behavior behavior;
	
	// osc communication objects (communication with Max)
	private OscP5 oscP5;
	private NetAddress myRemoteLocation;

	// constructor
	Flock (int flockID, int numBoids, int flockType, PApplet parent, 
			OscP5 oscP5, NetAddress myRemoteLocation, Behavior behavior) {

		// make the Boids a synchronizedList so we can use this as a way to limit access to the Flocks; 
		// this is necessary so that the program doesn't crash when Max changes a Flock size in the 
		// middle of Processing doing something to every Boid in a Flock in a for-loop;
		// the underlying data structure that actually holds the Boids is an ArrayList
		backingBoids = new ArrayList<Boid>(numBoids);  
		boids = Collections.synchronizedList(backingBoids);
		this.behavior = behavior;
		
		// generate random flock if so indicated
		// BUT NOT proximityThreshold. which is set above
		if (flockType == MusicSwarm.RANDOM_FLOCK) {   
			velocityScale = Math.max(MusicSwarm.rand.nextFloat(), 0.1f);            // range = [0.0, 0.1]
			maxSpeed = Math.max(MusicSwarm.rand.nextFloat() * 10.0f, 2.0f);         // range = [2.0, 10.0]
			normalSpeed = Math.max(MusicSwarm.rand.nextFloat() * maxSpeed, 1.0f);   // range = [1.0, maxSpeed]
			neighborRadius = MusicSwarm.rand.nextInt(91) + 10;                      // range = [10.0, 100.0]
			separationWeight = MusicSwarm.rand.nextFloat() * 100.0f;                // range = [0.0, 100.0]
			alignmentWeight = MusicSwarm.rand.nextFloat();                          // range = [0.0, 1.0]
			cohesionWeight = MusicSwarm.rand.nextFloat();                           // range = [0.0, 1.0]
			pacekeepingWeight = MusicSwarm.rand.nextFloat();                        // range = [0.0, 1.0]
			randomMotionProbability = MusicSwarm.rand.nextFloat() / 2.0f;           // range = [0.0, 0.5]
		}

		// create the Boids
		for (int boidID = 0 ; boidID < numBoids; boidID++) {

			PVector boidLocation = new PVector(MusicSwarm.rand.nextInt(MusicSwarm.WINDOW_WIDTH+1) - MusicSwarm.WINDOW_WIDTH/2, 
											   MusicSwarm.rand.nextInt(MusicSwarm.WINDOW_HEIGHT+1) - MusicSwarm.WINDOW_HEIGHT/2, 
											   MusicSwarm.rand.nextInt(MusicSwarm.WINDOW_DEPTH+1) - MusicSwarm.WINDOW_DEPTH/2);
			Boid boid = new Boid(flockID, boidID, boidLocation, velocityScale, maxSpeed, normalSpeed, neighborRadius, 
					separationWeight, alignmentWeight, cohesionWeight, pacekeepingWeight, randomMotionProbability, proximityThreshold, 
					behavior, parent, oscP5, myRemoteLocation);
			backingBoids.add(boid); 
		}

		// need to save the parent and communication (with Max) information 
		// to send to the Boid constructor if we create new Boids later 
		this.parent = parent;
		this.flockID = flockID;
		this.oscP5 = oscP5;
		this.myRemoteLocation = myRemoteLocation;
	}


	// update the velocity and location of each boid in the flock;
	// calculate some flock statistics and send them to Max
	void run(Flock[] allFlocks) {

		synchronized(boids) {

			if (flockEmpty())
				return;

			for (int i = 0; i < backingBoids.size(); i++) {
				Boid b = (Boid) backingBoids.get(i);  

				// need to send all the flocks to the run method in the boid class, 
				// so we can calculate all the boids in every boid's neighborhood
				b.run(allFlocks);

			}
			
			//report stats for this flock to Max
			flockAnalysis();
			
		}
	}
	
	void setBehavior(Behavior behavior) {
		//change behavior in each boid in flock
		for (int i = 0; i < backingBoids.size(); i++) {
			Boid b = (Boid) backingBoids.get(i);  
			b.setBehavior(behavior);
		}
	}

	
	// sets the flock size; can be larger or smaller than current size
	void setFlockSize(int newSize) {

		synchronized(boids) {

			if (newSize == backingBoids.size())
				return;

			int oldSize = backingBoids.size();

			// add boids, if necessary
			if (newSize > oldSize) {
				int numNewBoids = newSize - oldSize;
				for (int i = 1 ; i <= numNewBoids ; i++) {
					PVector location = new PVector(MusicSwarm.rand.nextInt(MusicSwarm.WINDOW_WIDTH+1) - MusicSwarm.WINDOW_WIDTH/2, 
												   MusicSwarm.rand.nextInt(MusicSwarm.WINDOW_HEIGHT+1) - MusicSwarm.WINDOW_HEIGHT/2, 
												   MusicSwarm.rand.nextInt(MusicSwarm.WINDOW_DEPTH+1) - MusicSwarm.WINDOW_DEPTH/2);
					addNewBoid(location);
				}
			}

			// remove boids, if necessary
			else if (newSize < oldSize) {
				int numBoidsDestroyed = oldSize - newSize;
				for(int i = 1 ; i <= numBoidsDestroyed ; ++i) {
					// remove the one with the highest index
					backingBoids.remove(backingBoids.size() - 1);
				}
			}     
		}
	}


	// create a new Boid at the specified location
	void addNewBoid(PVector location) {

		synchronized(boids) {
			
			backingBoids.ensureCapacity(backingBoids.size() + 1); 
			Boid boid = new Boid(flockID, backingBoids.size(), location, velocityScale, maxSpeed,  normalSpeed,  neighborRadius, 
					separationWeight, alignmentWeight, cohesionWeight, pacekeepingWeight, randomMotionProbability, proximityThreshold, 
					behavior, parent, oscP5, myRemoteLocation);
			backingBoids.add(boid); 
		}
	}


	// test age of Boids and remove dead boids from arraylist
	void removeDeadBoids() {

		// Boids might not have a fixed lifespan
		if (!boidMortality)
			return;

		synchronized(boids) {

			for (int i = 0; i < backingBoids.size(); i++) {				
				Boid b = (Boid) backingBoids.get(i);  
				if (b.getAge() > BOID_LIFESPAN)
					backingBoids.remove(i);
			}
		}		
	}


	// analyze flock statistics
	void flockAnalysis(){

		PVector velocitySum = new PVector(0.0f, 0.0f, 0.0f);
		PVector locationSum = new PVector(0.0f, 0.0f, 0.0f);
		float velocityMagnitudeSum = 0.0f;

		synchronized(this.boids) {

			List<Boid> boids = this.boids;
			int boidsSize = boids.size();

			for (int i = 0 ; i < boidsSize; i++) {
				Boid boid = (Boid) boids.get(i);
				locationSum.add(boid.getLocation());
				velocitySum.add(boid.getVelocity());
				velocityMagnitudeSum = velocityMagnitudeSum + boid.getVelocity().mag();	
			}

			PVector locationMean = new PVector(0.0f, 0.0f, 0.0f);
			PVector locationAveDeviation = new PVector(0.0f, 0.0f, 0.0f);
			float locationDeviationX = 0.0f;
			float locationDeviationY = 0.0f;
			float locationDeviationZ = 0.0f;

			PVector velocityMean = new PVector(0.0f, 0.0f, 0.0f);
			PVector velocityAveDeviation = new PVector(0.0f, 0.0f, 0.0f); 
			float velocityDeviationX = 0.0f;
			float velocityDeviationY = 0.0f;
			float velocityDeviationZ = 0.0f;
			float velocityMagnitudeMean = 0.0f;
			float velocityMagnitudeDeviation = 0.0f;

			// find mean location and mean velocity for the flock
			locationMean = locationSum;
			locationMean.div(boidsSize);

			velocityMean = velocitySum;
			velocityMean.div(boidsSize);
			velocityMagnitudeMean = velocityMagnitudeSum / boidsSize;

			// find average deviation from mean location and velocity
			for (int i = 0 ; i < boidsSize; i++) {
				Boid boid = (Boid) boids.get(i);
				//accumulate sum of deviations in this loop
				locationDeviationX += PApplet.abs(boid.getLocation().x - locationMean.x);
				locationDeviationY += PApplet.abs(boid.getLocation().y - locationMean.y);
				locationDeviationZ += PApplet.abs(boid.getLocation().z - locationMean.z);

				velocityDeviationX += PApplet.abs(boid.getVelocity().x - velocityMean.x);
				velocityDeviationY += PApplet.abs(boid.getVelocity().y - velocityMean.y);
				velocityDeviationZ += PApplet.abs(boid.getVelocity().z - velocityMean.z);
				velocityMagnitudeDeviation += PApplet.abs(boid.getVelocity().mag() - velocityMagnitudeMean);
			}

			//divide by boidSize - number of boids in flock - to get the average deviation
			locationDeviationX /= boidsSize;
			locationDeviationY /= boidsSize;
			locationDeviationZ /= boidsSize;
			locationAveDeviation.set(locationDeviationX, locationDeviationY, locationDeviationZ); 

			velocityDeviationX /= boidsSize;
			velocityDeviationY /= boidsSize;
			velocityDeviationZ /= boidsSize;
			velocityAveDeviation.set(velocityDeviationX, velocityDeviationY, velocityDeviationY);
			velocityMagnitudeDeviation /= boidsSize;

			//send stats to osc
			// NEED TO SEND Z AXIS DATA TO OSC !!!!!
			OscMessage AnalysisMessage = new OscMessage("/FlockAnalysis");
			AnalysisMessage.add(flockID);
			AnalysisMessage.add(locationMean.x);
			AnalysisMessage.add(locationMean.y);
			AnalysisMessage.add(locationMean.z);
			AnalysisMessage.add(locationAveDeviation.x);
			AnalysisMessage.add(locationAveDeviation.y);
			AnalysisMessage.add(locationAveDeviation.z);

			AnalysisMessage.add(velocityMean.x);
			AnalysisMessage.add(velocityMean.y);
			AnalysisMessage.add(velocityMean.z);
			AnalysisMessage.add(velocityAveDeviation.x);
			AnalysisMessage.add(velocityAveDeviation.y);
			AnalysisMessage.add(velocityAveDeviation.z);
			AnalysisMessage.add(velocityMagnitudeMean);
			AnalysisMessage.add(velocityMagnitudeDeviation);

//			oscP5.send(AnalysisMessage, myRemoteLocation); 

		}

	}


	
	// getters
	
	public boolean flockEmpty() {
		return backingBoids.size() == 0;
	}

	
	// setters

	public void setVelocityScale(float vscale){

		synchronized(boids) {
			velocityScale = vscale;
			for (int i = 0; i < backingBoids.size(); i++) {
				Boid b = (Boid) backingBoids.get(i);  
				b.setVelocityScale(vscale);
			}
		}
	}


	void setMaxSpeed(int maxSpeed){

		synchronized(boids) {
			this.maxSpeed = maxSpeed;
			for (int i = 0; i < backingBoids.size(); i++) {
				Boid b = (Boid) backingBoids.get(i);  
				b.setMaxSpeed(maxSpeed);
			}
		}
	}


	void setNormalSpeed(int normalSpeed){

		synchronized(boids) {

			this.normalSpeed = normalSpeed;
			for (int i = 0; i < backingBoids.size(); i++) {
				Boid b = (Boid) backingBoids.get(i);  
				b.setNormalSpeed(normalSpeed);
			}
		}
	}

	
	void setNeighborRadius(float neighborRadius){

		synchronized(boids) {

			this.neighborRadius = neighborRadius;
			for (int i = 0; i < backingBoids.size(); i++) {
				Boid b = (Boid) backingBoids.get(i);  
				b.setNeighborRadius(neighborRadius);
			}
		}
	}



	void setSeparationWeight(float separationWeight){

		synchronized(boids) {

			this.separationWeight = separationWeight;
			for (int i = 0; i < backingBoids.size(); i++) {
				Boid b = (Boid) backingBoids.get(i);  
				b.setSeparationWeight(separationWeight);
			}
		}
	}


	void setAlignWeight(float alignmentWeight){

		synchronized(boids) {

			this.alignmentWeight = alignmentWeight;
			for (int i = 0; i < backingBoids.size(); i++) {
				Boid b = (Boid) backingBoids.get(i);  
				b.setAlignWeight(alignmentWeight);
			}
		}
	}


	void setCohesionWeight(float cohesionWeight){

		synchronized(boids) {

			this.cohesionWeight = cohesionWeight;
			for (int i = 0; i < backingBoids.size(); i++) {
				Boid b = (Boid) backingBoids.get(i);  
				b.setCohesionWeight(cohesionWeight);
			}
		}
	}



	void setPacekeepingWeight(float pacekeepingWeight){

		synchronized(boids) {

			this.pacekeepingWeight = pacekeepingWeight;
			for (int i = 0; i < backingBoids.size(); i++) {
				Boid b = (Boid) backingBoids.get(i);  
				b.setPacekeepingWeight(pacekeepingWeight);
			}
		}
	}


	void setRandomMotionProbability(float randomMotionProbability){

		synchronized(boids) {
			
			this.randomMotionProbability = randomMotionProbability;
			for (int i = 0; i < backingBoids.size(); i++) {
				Boid b = (Boid) backingBoids.get(i);  
				b.setRandomMotionProbability(randomMotionProbability);
			}
		}
	}

	void setMortality(boolean boidMortality){

		synchronized(boids) {
			this.boidMortality = boidMortality;
		}
	}


	void setProximityThreshold(int proximityThreshold){

		synchronized(boids) {

			this.proximityThreshold = proximityThreshold;
			for (int i = 0; i < backingBoids.size(); i++){
				Boid b = (Boid) backingBoids.get(i);
				b.setProximityThresehold(proximityThreshold);
			}
		}
	}




}

