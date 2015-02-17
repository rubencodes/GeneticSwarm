import processing.core.*; 
import processing.xml.*; 

import java.net.Authenticator; 
import java.net.PasswordAuthentication; 
import java.util.Random; 
import javax.swing.*; 

import javax.json.*; 
import org.glassfish.json.*; 
import javax.json.stream.*; 
import javax.json.spi.*; 

import java.applet.*; 
import java.awt.Dimension; 
import java.awt.Frame; 
import java.awt.event.MouseEvent; 
import java.awt.event.KeyEvent; 
import java.awt.event.FocusEvent; 
import java.awt.Image; 
import java.io.*; 
import java.net.*; 
import java.text.*; 
import java.util.*; 
import java.util.zip.*; 
import java.util.regex.*; 

public class MusicSwarm extends PApplet {

/* 
 * Stephen Majercik
 * Frank Mauceri
 * last updated 14 May 2013
 * 
 * 
 * MusicSwarm.java
 * NOTE:  the following are required by Processing:
 * 	1) MusicSwarm must extend the PApplet class
 * 	2) the must be a setup method to specify code that is executed when the applet starts up
 * 	3) there must be a draw method to specify code that is executed on each iteration
 * 
 * Contains constants,variables, and methods related to:
 * 	1) communication between Processing and Max;
 * 		- sets up infrastructure
 * 		- defines methods that Max calls to change various values in the flocks
 * 	2) graphics
 * 		- sets up graphics environment
 * 		- includes code that implements a movable camera view of the simulation
 * 	3) flocks
 * 		- creation
 * 		- running (calls run method in Flock class)
 * 		
 */



// for random numbers



public boolean insideButton = false;
// ****************  GRAPHICS  ******************

// window dimensions
public static final int WINDOW_WIDTH = 800;
public static final int WINDOW_HEIGHT = 800;
public static final int WINDOW_DEPTH = 800;

// number of dimensions in rendering 
public static final int RENDER_2D = 1;
public static final int RENDER_3D = 2;
public static int renderMethod = RENDER_3D;

// camera movement
// to smooth out yaw and pitch changes
private static final float SCALE_DOWN_YAW_RATE_OF_CHANGE= 0.05f;
private static final float SCALE_DOWN_PITCH_RATE_OF_CHANGE= 0.05f;
// to set zoom scale and range
private static final int ZOOM_SCALING_FACTOR = 150;
private static final int ZOOM_RANGE = 10;
// accept messages from Max that control camera movement
private float pitch;  //rotate around X
private float yaw;	  //rotate around Y
private float zoom;

// can introduce wind into the simulation; not used for quite a while (as of 5/14/13)
public static PVector windVector = new PVector(0.0f, 0.0f, 0.0f);

// boid-specific rendering
public static final int BOID_SIZE = 5;
// opacity of the fill for Boids
public static final int B_ALPHA = 150;
// are the boids in the same neighborhood connected by a line graphically?
public static final boolean CONNECTED_COMPONENTS = false;


// ****************  FLOCKS  ******************

// different modes of flock creation
public static final int DEFAULT_FLOCK = 1; // creates flock with the same default parameters, which are specified in the Flock class
public static final int RANDOM_FLOCK = 2;  // creates each flock with its own random parameters, generated in the Flock class
private static int flockType = DEFAULT_FLOCK;	

// flocks
// ************************************************************************************************
// NOTE: flock indices start at 1, not 0, because Max numbers them this way 
// ************************************************************************************************
private static final int NUM_FLOCKS = 6;                      
private static Flock[] allFlocks = new Flock[NUM_FLOCKS+1];   
// ************************************************************************************************
// NOTE (5/14/13): ALL 6 flocks are always created: 
// if all flocks are being used and all are the same size, set useDefaultFlockSize to true 
//     and set defaultFlockSize to desired size
// if flock sizes are different or some flocks are not being used, set useDefaultFlockSize to false
//     and set flock sizes in nonDefaultInitialFlockSizes (0 if flock not being used)
//     NOTE: flock size of *any* flock, including initially empty flocks, can be changed by Max
// ************************************************************************************************
private static boolean useDefaultFlockSize = false;
private static int defaultFlockSize = 50;
private static int[] nonDefaultInitialFlockSizes = {
  0, 200, 200, 200, 0, 0, 0
};

// flock colors
static final int[][] FLOCKCOLOR = { 	
  {
    0, 0, 0
  }
  , 
  {
    255, 0, 0
  }
  , 
  {
    0, 255, 0
  }
  , 
  {
    0, 0, 255
  }
  , 
  {
    255, 255, 0
  }
  , 
  {
    255, 0, 255
  }
  , 
  {
    0, 255, 255
  }
};


// ****************  MISCELLANEOUS  ******************

// for random numbers
// static so accessible from other classes (not actually used in this class)
static Random rand = new Random();
//rand.setSeed(43453445);        

// for delimiting behaviors over time
public static int timeStep = 0;

public int buttonColor = 209;

public String behaviorString;

public static void main(String args[]) {
  PApplet.main(new String[] { 
    "--present", "MusicSwarm"
  }
  );
}
//
public void run() {
  behaviorString = getParameter("behavior").replaceAll("'", "\"");
  System.out.println(behaviorString);
  super.run();
}

// setting up the simulation
public void setup() {
  // window size and graphics mode
  size(800, 800, P3D); 

  timeStep = 0;
  System.out.println(behaviorString);
  // create the Flocks  
  for (int flockID = 1; flockID <= NUM_FLOCKS; flockID++) {    
    int flockSize = useDefaultFlockSize? defaultFlockSize: nonDefaultInitialFlockSizes[flockID];
    allFlocks[flockID] = new Flock(flockID, flockSize, flockType, this, new Behavior(behaviorString));
  }
}

// the "loop forever" method in processing
public void draw() {
  ++timeStep;

  // background is black
  // (need to completely redraw the simulation at each time step)
  background(0);

  // set the camera point of view
  float zoomZ = (zoom - ZOOM_SCALING_FACTOR) * ZOOM_RANGE;
  translate(WINDOW_WIDTH/2, WINDOW_HEIGHT/2, zoomZ);
  rotateY(yaw * SCALE_DOWN_YAW_RATE_OF_CHANGE);
  rotateX(pitch * SCALE_DOWN_PITCH_RATE_OF_CHANGE);
  // draw the cube defining the flock space
  // show lines
  stroke(200, 200);
  // but don't fill the cube 
  noFill();
  box(WINDOW_HEIGHT);	

  // update all the Flocks
  // NOTE: need to send all the Flocks, so we have access to all the Boids in every Boid's neighborhood
  for (int flockID = 1; flockID <= NUM_FLOCKS; flockID++) {
    allFlocks[flockID].run(allFlocks);
  }

  // remove dead Boids
  for (int flockID = 1; flockID <= NUM_FLOCKS; flockID++) {
    allFlocks[flockID].removeDeadBoids();
  }
}

// Max calls these to change the parameters of the Flocks  ---------------------------------------

public void setFlockSize(int flockSize, int flockID) {
  allFlocks[flockID].setFlockSize(flockSize);
}

public void setVelocityScale(float velocityScale, int flockID) {
  allFlocks[flockID].setVelocityScale(velocityScale);
}    

public void setMaxSpeed(int maxSpeed, int flockID) {
  allFlocks[flockID].setMaxSpeed(maxSpeed);
}

public void setNormalSpeed(int normalSpeed, int flockID) {
  allFlocks[flockID].setNormalSpeed(normalSpeed);
}

public void setNeighborRadius(float radius, int flockID) {
  allFlocks[flockID].setNeighborRadius(radius);
}

public void setSeparationWeight(float separationWeight, int flockID) {
  allFlocks[flockID].setSeparationWeight(separationWeight);
}

public void setAlignWeight (float alignWeight, int flockID) {
  allFlocks[flockID].setAlignWeight(alignWeight);
}

public void setCohesionWeight(float cohesionWeight, int flockID) {
  allFlocks[flockID].setCohesionWeight(cohesionWeight);
}

public void setPacekeepingWeight(float paceKeepingWeight, int flockID) {
  allFlocks[flockID].setPacekeepingWeight(paceKeepingWeight);
}

public void setRandomMotionProbability(float randomMotionProbability, int flockID) {
  allFlocks[flockID].setRandomMotionProbability(randomMotionProbability);
}

public void setProximityThreshold(int proximityThreshold, int flockID) {
  allFlocks[flockID].setProximityThreshold(proximityThreshold);
}

public void setMortality(int mortality, int flockID) {
  allFlocks[flockID].setMortality(mortality == 1);
}

public void setWindVector(int windX, int windY, int windZ, int flockID) {
  windVector = new PVector (windX, windY, windZ);
}

// 5/14/13
// trackX and trackY appear not to be used anymore; removed from code
// can be removed in Max as parameters that are sent to this method
public void setCameraMove(float yaw, float roll, float pitch, float trackX, float trackY, float zoom) {
  this.yaw = yaw; 
  this.pitch = pitch;
  this.zoom = zoom;
}

// 5/14/13
// the parameter makeBoid appears not to be used in addNewBoid, so no longer sent 
public void setAddNewBoid(int locationX, int locationY, int locationZ, int flockID, int makeBoid) {
  PVector boidLocation = new PVector(locationX, locationY, locationZ);
  allFlocks[flockID].addNewBoid(boidLocation);
}

}
