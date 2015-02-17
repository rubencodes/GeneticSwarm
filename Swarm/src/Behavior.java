import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;
import java.util.Vector;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class Behavior {
    private int comparatorId;         					//ID of comparator; chooses between >, <, or ==
    private int propertyA;	         					//ID of variable used in if()
    private int propertyB;	         					//ID of variable/constant compared to
    private boolean randomPropertyB;	  				//decides if comparison will be to random prop or constant
    public int depthLevel;		      					//current depthLevel
    private Vector < Integer > ifPropertyIDs;	    	//IDs of variables to be acted upon, if true
    private Vector < Integer > ifActionIDs;	            //IDs of actions to be taken on variables, if true
    private Vector < Integer > elsePropertyIDs;			//IDs of variables to be acted upon, if false
    private Vector < Integer > elseActionIDs;	        //IDs of actions to be taken on variables, if false
    private Vector < Float > numberBank;	            //Floats used for setting/incrementing/decrementing actions
    private Vector < Float > nullNumberBank;	        //Floats used for setting/incrementing/decrementing actions
    private Vector < Behavior > subBehaviors;	        //storing any sub-behaviors generated
    
    private Boid boid;									//storing our boid for variable value retrieval
    private int score;									//stores evaluation score
    private static final int ALL_VARS_COUNT = 12;	    //number of variables from boid
    private static final int ALL_ACTIONS_COUNT = 3;	  	//number of possible actions
    private static final int MAX_BEHAVIOR_DEPTH = 4;	//max depth of behavior
    private Random r = new Random();
 
    //stores randoms chosen for behavior
    private float velocityScale;
    private float maxSpeed;
    private float normalSpeed;
    private float neighborhoodRadius;
    private float separationWeight;
    private float alignmentWeight;
    private float cohesionWeight;
    private float pacekeepingWeight;
    private float motionProbability;
    private float numNeighborsOwnFlock;
    private float numNeighborsAllFlocks;
 
    //Auto-Generate Behavior object
    public Behavior() {
    	JsonArray behavior_array = null;
		JsonObject behavior = null;
		try {
			JsonReader rdr = Json.createReader(new StringReader("[{'id':127,'user_id':1,'comparator_id':1,'property_a_id':5,'property_b_id':5,'random_property_b':false,'depth_level':0,'if_property_ids':'8,7,5,8,3,1,8,6','if_action_ids':'1,1,1,0,0,0,0,1','else_property_ids':'4,2,6,5,2,3','else_action_ids':'0,0,1,2,0,0','if_number_bank':'0.055450656728961656,0.02291244140533355,0.6675838011699021,0.3710752054236616,80,7,0.49958131037458864,0.36560481485861795','else_number_bank':'54,3,0.7756854085328937,0.8134226422803801,3,95','subbehavior_ids':'41','created_at':'2015-01-06T09:44:30.723Z','updated_at':'2015-01-06T09:44:30.883Z','velocity_scale':'0.008743848631595585','max_speed':'2.0','normal_speed':'4.0','neighborhood_radius':'91.0','separation_weight':'0.06176711204622852','cohesion_weight':'5.0','pacekeeping_weight':'91.0','rand_motion_probability':null,'alignment_weight':'2.0','rating':0,'name':'Antonette'},{'id':41,'user_id':null,'comparator_id':0,'property_a_id':7,'property_b_id':1,'random_property_b':false,'depth_level':1,'if_property_ids':'6,2,3,3','if_action_ids':'1,1,2,2','else_property_ids':'3,8,2,3,4','else_action_ids':'2,1,1,2,2','if_number_bank':'0.819779585901957,9,79,45','else_number_bank':'32,0.17485271119540569,3,67,71','subbehavior_ids':'40','created_at':'2014-07-06T07:35:11.806Z','updated_at':'2014-07-06T07:35:11.806Z','velocity_scale':'0.06479125088460257','max_speed':'3.0','normal_speed':'10.0','neighborhood_radius':'15.0','separation_weight':'68.0','cohesion_weight':'0.6057896593586818','pacekeeping_weight':'0.3401925873250217','rand_motion_probability':'0.4524618620187331','alignment_weight':'0.6722361430190025','rating':0,'name':'Herman'},{'id':40,'user_id':null,'comparator_id':0,'property_a_id':1,'property_b_id':2,'random_property_b':false,'depth_level':2,'if_property_ids':'','if_action_ids':'','else_property_ids':'3','else_action_ids':'0','if_number_bank':'','else_number_bank':'23','subbehavior_ids':'39','created_at':'2014-07-06T07:35:11.808Z','updated_at':'2014-07-06T07:35:11.808Z','velocity_scale':'0.07952037375935349','max_speed':'10.0','normal_speed':'7.0','neighborhood_radius':'33.0','separation_weight':'0.0','cohesion_weight':'0.7704632650045183','pacekeeping_weight':'0.02020905399070694','rand_motion_probability':'0.01762186770939161','alignment_weight':'0.4073610920037062','rating':0,'name':'Jewel'},{'id':39,'user_id':null,'comparator_id':1,'property_a_id':8,'property_b_id':6,'random_property_b':false,'depth_level':3,'if_property_ids':'7,6,6,1,8,7','if_action_ids':'2,0,0,2,2,2','else_property_ids':'','else_action_ids':'','if_number_bank':'0.7630768568245475,0.41033593856671213,0.6383953286701042,7,0.2998422529204037,0.9964330361696794','else_number_bank':'','subbehavior_ids':null,'created_at':'2014-07-06T07:35:11.809Z','updated_at':'2014-07-06T07:35:11.809Z','velocity_scale':'0.08546438454777604','max_speed':'8.0','normal_speed':'8.0','neighborhood_radius':'63.0','separation_weight':'46.0','cohesion_weight':'0.636079473711015','pacekeeping_weight':'0.518327643843711','rand_motion_probability':'0.4872315521329049','alignment_weight':'0.8448158255493231','rating':0,'name':'Ruby'}]".replaceAll("'", "\"")));
			behavior_array = rdr.readArray();
			behavior = behavior_array.getValuesAs(JsonObject.class).get(0);
		} catch (Exception e) {
			e.printStackTrace();
		} 

    	comparatorId    = behavior.getInt("comparator_id");
    	propertyA      	= behavior.getInt("property_a_id");
    	randomPropertyB = behavior.getBoolean("random_property_b");
    	propertyB       = behavior.getInt("property_b_id");
    	depthLevel      = behavior.getInt("depth_level");
    	
    	//create new vectors for behavior
    	ifActionIDs      = new Vector < Integer  > ();
    	ifPropertyIDs    = new Vector < Integer  > ();
    	elseActionIDs    = new Vector < Integer  > ();
    	elsePropertyIDs  = new Vector < Integer  > ();
    	subBehaviors     = new Vector < Behavior > ();
    	
    	//split array by delimiter, convert to array of integers, and convert array to vector
    	for(String actionVarID : behavior.getString("if_property_ids").split(","))
    		if(!actionVarID.equals(""))
    			ifPropertyIDs.add(Integer.parseInt(actionVarID));
    	for(String actionID : behavior.getString("if_action_ids").split(","))
    		if(!actionID.equals(""))
    			ifActionIDs.add(Integer.parseInt(actionID));
    	for(String nullActionVarID : behavior.getString("else_property_ids").split(","))
    		if(!nullActionVarID.equals(""))
    			elsePropertyIDs.add(Integer.parseInt(nullActionVarID));
    	for(String nullActionID : behavior.getString("else_action_ids").split(","))
    		if(!nullActionID.equals(""))
    			elseActionIDs.add(Integer.parseInt(nullActionID));
		
		velocityScale      = Float.parseFloat(behavior.getString("velocity_scale"));
		maxSpeed           = Float.parseFloat(behavior.getString("max_speed"));
		normalSpeed    	   = Float.parseFloat(behavior.getString("normal_speed"));
		neighborhoodRadius = Float.parseFloat(behavior.getString("neighborhood_radius"));
		separationWeight   = Float.parseFloat(behavior.getString("separation_weight"));
		alignmentWeight    = Float.parseFloat(behavior.getString("alignment_weight"));
		cohesionWeight     = Float.parseFloat(behavior.getString("cohesion_weight"));
		pacekeepingWeight  = Float.parseFloat(behavior.getString("pacekeeping_weight"));
		motionProbability  = Float.parseFloat(behavior.getString("rand_motion_probability", "0"));
		
		for(int i = 1; i < behavior_array.getValuesAs(JsonObject.class).size(); i++) {
			JsonObject subBehavior = behavior_array.getValuesAs(JsonObject.class).get(i);
			subBehaviors.add(new Behavior(subBehavior));
		}
    }
    
    public Behavior(JsonObject behavior) {
    	System.out.println("SubBehavior Read: "+behavior.toString());
    	comparatorId    = behavior.getInt("comparator_id");
    	propertyA      	= behavior.getInt("property_a_id");
    	randomPropertyB = behavior.getBoolean("random_property_b");
    	propertyB       = behavior.getInt("property_b_id");
    	depthLevel      = behavior.getInt("depth_level");
    	
    	//create new vectors for behavior
    	ifActionIDs      = new Vector < Integer  > ();
    	ifPropertyIDs    = new Vector < Integer  > ();
    	elseActionIDs    = new Vector < Integer  > ();
    	elsePropertyIDs  = new Vector < Integer  > ();
    	subBehaviors     = new Vector < Behavior > ();
    	
    	//split array by delimiter, convert to array of integers, and convert array to vector
    	for(String actionVarID : behavior.getString("if_property_ids").split(","))
    		if(!actionVarID.equals(""))
    			ifPropertyIDs.add(Integer.parseInt(actionVarID));
    	for(String actionID : behavior.getString("if_action_ids").split(","))
    		if(!actionID.equals(""))
    			ifActionIDs.add(Integer.parseInt(actionID));
    	for(String nullActionVarID : behavior.getString("else_property_ids").split(","))
    		if(!nullActionVarID.equals(""))
    			elsePropertyIDs.add(Integer.parseInt(nullActionVarID));
    	for(String nullActionID : behavior.getString("else_action_ids").split(","))
    		if(!nullActionID.equals(""))
    			elseActionIDs.add(Integer.parseInt(nullActionID));
		
		velocityScale      = Float.parseFloat(behavior.getString("velocity_scale"));
		maxSpeed           = Float.parseFloat(behavior.getString("max_speed"));
		normalSpeed    	   = Float.parseFloat(behavior.getString("normal_speed"));
		neighborhoodRadius = Float.parseFloat(behavior.getString("neighborhood_radius"));
		separationWeight   = Float.parseFloat(behavior.getString("separation_weight"));
		alignmentWeight    = Float.parseFloat(behavior.getString("alignment_weight"));
		cohesionWeight     = Float.parseFloat(behavior.getString("cohesion_weight"));
		pacekeepingWeight  = Float.parseFloat(behavior.getString("pacekeeping_weight"));
		motionProbability  = Float.parseFloat(behavior.getString("rand_motion_probability"));
    }
    
    //executes Behavior on a boid
    public void execute(Boid boid) {
    	this.boid = boid;	//sets boid to act upon
    	if (compare())		//make a comparison
    	    for (int i = 0; i < ifActionIDs.size(); i++)
    		    actionBank(ifActionIDs.get(i), ifPropertyIDs.get(i), 1);
     
    	else			//if comparison is false
    	    for (int i = 0; i < elseActionIDs.size(); i++)
    		    actionBank(elseActionIDs.get(i), elsePropertyIDs.get(i), 1);
     
    	//execute any sub-behaviors
    	for (int i = 0; i < subBehaviors.size(); i++)
    	    subBehaviors.get(i).execute(boid);
    }
     
        //makes a comparison between two variables
    public boolean compare() {
    	switch (comparatorId) {
    	case 0:
    	    if (varBank(propertyA, false) > varBank(propertyB, randomPropertyB))	//if var1 > var2
    		      return true;
    	    else return false;
    	case 1:
    	    if (varBank(propertyA, false) < varBank(propertyB, randomPropertyB))	//if var1 < var2
    		      return true;
    	    else return false;
    	case 2:
    	    if (varBank(propertyA, false) == varBank(propertyB, randomPropertyB))	//if var1 == var2
    		      return true;
    	    else return false;
    	}
    	return false;
    }

    //bank containing variables, integers, and randoms
    public float varBank(int ID, boolean rand) {
    	switch (ID) {
    	case 0:
    	    if (rand) return velocityScale;
    	    return boid.getVelocityScale();
    	case 1:
    	    if (rand) return maxSpeed;
    	    return boid.getMaxSpeed();
    	case 2:
    	    if (rand) return normalSpeed;
    	    return boid.getNormalSpeed();
    	case 3:
    	    if (rand) return neighborhoodRadius;
    	    return boid.getNeighborRadius();
    	case 4:
    	    if (rand) return separationWeight;
    	    return boid.getSeparationWeight();
    	case 5:
    	    if (rand) return alignmentWeight;
    	    return boid.getAlignmentWeight();
    	case 6:
    	    if (rand) return cohesionWeight;
    	    return boid.getCohesionWeight();
    	case 7:
    	    if (rand) return pacekeepingWeight;
    	    return boid.getPacekeepingWeight();
    	case 8:
    	    if (rand) return motionProbability;
    	    return boid.getRandomMotionProbability();
    	case 9:
    	    if (rand) return numNeighborsOwnFlock;
    	    return boid.getNumNeighborsOwnFlock();
    	case 10:
    	    if (rand) return numNeighborsAllFlocks;
    	    return 1;
    	default:
    	    return ID;
    	}
    }
     
    //bank containing actions for variables
    //int ID: chooses between incrementing a variable by x, decrementing by x, or setting to x
    //int propertyA: decides which variable is going to be acted on in any of the 3 cases
    //float x: what is added, subtracted, or set to a variable
    public void actionBank(int ID, int propertyA, float x) {
    	boolean rand = false;	//allows for randomness when grabbing from var bank; not desired in action bank
    	switch (ID) {
        //add to variable
    	case 0:
    	    float i = varBank(propertyA, rand) + x;
    	    boid.set(propertyA, i);
    	    break;
        //subtract from variable
    	case 1:
    	    float j = varBank(propertyA, rand) - x;
    	    boid.set(propertyA, j);
    	    break;
        //set to value
    	case 2:
    	    boid.set(propertyA, x);
    	    break;
    	}
    }
    
    //print out behavior pseudocode
    public void printBehavior() {
      System.out.print("if (");
      switch(propertyA) {
        case 0: System.out.print("Velocity Scale ");
          break;
        case 1: System.out.print("Max Speed ");
          break;
        case 2: System.out.print("Normal Speed ");
          break;
        case 3: System.out.print("Neighborhood Radius ");
          break;
        case 4: System.out.print("Separation Weight ");
          break;
        case 5: System.out.print("Alignment Weight ");
          break;
        case 6: System.out.print("Cohesion Weight ");
          break;
        case 7: System.out.print("Pacekeeping Weight ");
          break;
        case 8: System.out.print("Random Motion Probability ");
          break;
        case 9: System.out.print("Num Neighbors Own Flock ");
          break;
        case 10: System.out.print("Num Neighbors All Flocks ");
          break;
      }
      switch(comparatorId) {
        case 0: System.out.print("> ");
          break;
        case 1: System.out.print("< ");
          break;
        case 2: System.out.print("== ");
          break;
      }
      if(randomPropertyB) {
        switch(propertyB) {
          case 0: System.out.print(velocityScale+") {");
            break;
          case 1: System.out.print(maxSpeed+") {");
            break;
          case 2: System.out.print(normalSpeed+") {");
            break;
          case 3: System.out.print(neighborhoodRadius+") {");
            break;
          case 4: System.out.print(separationWeight+") {");
            break;
          case 5: System.out.print(alignmentWeight+") {");
            break;
          case 6: System.out.print(cohesionWeight+") {");
            break;
          case 7: System.out.print(pacekeepingWeight+") {");
            break;
          case 8: System.out.print(motionProbability+") {");
            break;
          case 9: System.out.print(numNeighborsOwnFlock+") {");
            break;
          case 10: System.out.print(numNeighborsAllFlocks+") {");
            break;
        }
      }
      else {
        switch(propertyA) {
          case 0: System.out.print("Velocity Scale) {");
            break;
          case 1: System.out.print("Max Speed) {");
            break;
          case 2: System.out.print("Normal Speed) {");
            break;
          case 3: System.out.print("Neighborhood Radius) {");
            break;
          case 4: System.out.print("Separation Weight) {");
            break;
          case 5: System.out.print("Alignment Weight) {");
            break;
          case 6: System.out.print("Cohesion Weight) {");
            break;
          case 7: System.out.print("Pacekeeping Weight) {");
            break;
          case 8: System.out.print("Random Motion Probability) {");
            break;
          case 9: System.out.print("Num Neighbors Own Flock) {");
            break;
          case 10: System.out.print("Num Neighbors All Flocks) {");
            break;
        }
      }
      for(int i = 0; i < ifActionIDs.size(); i++) {
        switch(ifPropertyIDs.get(i)) {
          case 0: System.out.print("\n Velocity Scale ");
            break;
          case 1: System.out.print("\n Max Speed ");
            break;
          case 2: System.out.print("\n Normal Speed ");
            break;
          case 3: System.out.print("\n Neighborhood Radius ");
            break;
          case 4: System.out.print("\n Separation Weight ");
            break;
          case 5: System.out.print("\n Alignment Weight ");
            break;
          case 6: System.out.print("\n Cohesion Weight ");
            break;
          case 7: System.out.print("\n Pacekeeping Weight ");
            break;
          case 8: System.out.print("\n Random Motion Probability ");
            break;
          case 9: System.out.print("\n Num Neighbors Own Flock ");
            break;
          case 10: System.out.print("\n Num Neighbors All Flocks ");
            break;
        }
        switch(ifActionIDs.get(i)) {
          case 0: System.out.print("+1 ");
            break;
          case 1: System.out.print("-1 ");
            break;
          case 2: System.out.print("=1 ");
            break;
        }
      }
      System.out.println("}");
      System.out.println("else {");
      for(int i = 0; i < elseActionIDs.size(); i++) {
        switch(elsePropertyIDs.get(i)) {
          case 0: System.out.print("\n Velocity Scale ");
            break;
          case 1: System.out.print("\n Max Speed ");
            break;
          case 2: System.out.print("\n Normal Speed ");
            break;
          case 3: System.out.print("\n Neighborhood Radius ");
            break;
          case 4: System.out.print("\n Separation Weight ");
            break;
          case 5: System.out.print("\n Alignment Weight ");
            break;
          case 6: System.out.print("\n Cohesion Weight ");
            break;
          case 7: System.out.print("\n Pacekeeping Weight ");
            break;
          case 8: System.out.print("\n Random Motion Probability ");
            break;
          case 9: System.out.print("\n Num Neighbors Own Flock ");
            break;
          case 10: System.out.print("\n Num Neighbors All Flocks ");
            break;
        }
        switch(elseActionIDs.get(i)) {
          case 0: System.out.print("+1 ");
            break;
          case 1: System.out.print("-1 ");
            break;
          case 2: System.out.print("=1 ");
            break;
        }
      }
      System.out.println("}");
    }
    
    //getters and setters
    public int getScore() {
    	return score;
    }
     
     public void setScore(int score) {
    	this.score = score;
    }
     
    public int getVariableID() {
    	return propertyA;
    }
     
    public void setVariableID(int propertyA) {
    	this.propertyA = propertyA;
    }
     
    public int getComparatorID() {
    	return comparatorId;
    }
     
    public void setComparatorID(int comparatorId) {
    	this.comparatorId = comparatorId;
    }
     
    public int getNextNumID() {
    	return propertyB;
    }
     
    public void setNextNumID(int propertyB) {
    	this.propertyB = propertyB;
    }
     
    public Vector < Integer > getActionIDs() {
    	return ifActionIDs;
    }
     
    public void setActionIDs(Vector < Integer > ifActionIDs) {
    	this.ifActionIDs = ifActionIDs;
    }
     
    public Vector < Integer > getActionVariableIDs() {
    	return ifPropertyIDs;
    }
     
    public void setActionVariableIDs(Vector < Integer > ifPropertyIDs) {
    	this.ifPropertyIDs = ifPropertyIDs;
    }
     
    public Vector < Integer > getNullActionIDs() {
    	return elseActionIDs;
    }
     
    public void setNullActionIDs(Vector < Integer > elseActionIDs) {
    	this.elseActionIDs = elseActionIDs;
    }
     
    public Vector < Integer > getNullActionVariableIDs() {
    	return elsePropertyIDs;
    }
     
    public void setNullActionVariableIDs(Vector < Integer > elsePropertyIDs) {
    	this.elsePropertyIDs = elsePropertyIDs;
    }
     
    public Vector < Float > getNumberBank() {
    	return numberBank;
    }
     
    public void setNumberBank(Vector < Float > numberBank) {
    	this.numberBank = numberBank;
    }
     
    public Vector < Float > getNullNumberBank() {
    	return nullNumberBank;
    }
     
    public void setNullNumberBank(Vector < Float > nullNumberBank) {
    	this.nullNumberBank = nullNumberBank;
    }
     
    public Vector < Behavior > getSubBehaviors() {
    	return subBehaviors;
    }
     
    public void setSubBehaviors(Vector < Behavior > subBehaviors) {
    	this.subBehaviors = subBehaviors;
    }
}