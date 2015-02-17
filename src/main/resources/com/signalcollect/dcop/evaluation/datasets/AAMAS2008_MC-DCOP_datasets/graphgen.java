//
//graphgen.java
//graphgen

//Created by Jonathan Pearce on 3/17/07.
//Copyright (c) 2007 __MyCompanyName__. All rights reserved.

import java.util.*;
import java.io.*;
//need to add something to add in orphaned agents, ie agents that don't randomly get an edge, so the for loops in jumper can work propperly
//+ fixed(when using RANDOM_PAIR), if the density is above 1, then it will randomly find a partner for each agent sequentially, skipping those randomly already assigned as the second agent
//+ and then will use up the remaining density in random order the same way it was done in Jon's original code
//+ DONE - if density is at least .5 (ratio of 2 agents per 1 f-constraint) this code will first randomly pair up all agents, without duplication, and then randomly create more f-constraints until the density is fulfilled

//*** *** *** need to add a way to add g-budgets to only a select few, so that garentees can be made as to the min quality of the k-optimal solution
//+ probably have another arg that indicates the minimum k of the MC-MGM-k for the graph, in other words max cluster of agents with a g-constraint
//+ DONE

//*** *** *** need to figure out how to make sparse gBudgets meaningful. So far, i have it make only graphs with a garaunteed solution, 
//+ but this current way the gBudgets are never big enough to actually restrict movement!!! 
//+ DONE

//*** can now adjust tightness of the gbudget: so that each agent has a set number of impossible or possible options, 
//+ or a random distrobution of possible options within certain bounds, which can be varied for interesting stats

//!!! NOTE: current "jumper" MC-MGM code (as of 20070612) only works if there are either loopback or binary g-constraints but NOT both in the same graph file

public class graphgen {
	public static boolean reparsing;
	//for methodType
	public static final int RANDOM_PAIR = 1;
	public static final int RANDOM_ATTACH = 2;
	public static final int PREF_ATTACH = 3;
	public static final int CLUSTER = 4;
	//for gBudgetCreationMethod gCostCreationMethod
	public static final int STATIC = 0;
	public static final int RANDOM = 11;
	public static final int RANDOM_ASCENDING = 22;
	public static final int RANDOM_DECENDING = 33;
	public static final int RANDOM_ASCENDING_OR_DECENDING = 44;

	public static HashSet<Integer> agentsWithGConstraint = new HashSet<Integer>(); //*ip
	public static ArrayList<Integer>[] neighbors; //*ip should be one entry per agent, the ArrayList it holds is the list of all that agent's neighbors
	public static int gConstraintTightness=1; // NOT USED - this must be some value between 1 and domainSize-1, must be at least 1 invalid value, and at least one valid value
	// *later add option to do random tightness, perhaps random tightness within adjustable bounds
	public static int overBudgetGoal=1;
	public static int underBudgetGoal=1;	
	public static int numGConstraints;
	public static int k=1; // this is the maximum number of agents in the g-constraint-only subgraph
	

	public static HashSet<Integer> agentsInGSubgraph; 
	
	public static int numAgents;
	public static int domainSize;
	public static int methodType;
	public static double density;
	public static int clusterSize;
	public static int gBudgetRange;
	public static int gBudgetCreationMethod = RANDOM;
	public static int gCostCreationMethod = RANDOM;
	public static double gCostScaler;
	public static boolean usingMC = false;
	public static int numEdges;
	public static ArrayList<Integer> agentsInGraph = new ArrayList<Integer>();
	public static ArrayList<Vector> edges = new ArrayList<Vector>();
	public static ArrayList<Vector> gEdges = new ArrayList<Vector>();
	public static ArrayList<Integer>[] gConstraintNeighbors; 
	public static Random r = new Random();
	public static int[] agentGBudgets;

	public static void createEdges() {
		numEdges = (int)(density * numAgents);
//		new
		if (methodType == RANDOM_PAIR) {
			if (density >=0.5){
				for (int i=0; i < numAgents; i++) {
					if (!agentsInGraph.contains(i)) {
						Vector<Integer> edge = new Vector<Integer>();
						int firstVar = i;
						int secondVar = r.nextInt(numAgents);
						while (firstVar == secondVar || agentsInGraph.contains(secondVar))
							secondVar = r.nextInt(numAgents);
						if (firstVar < secondVar) {	
							edge.add(new Integer(firstVar));
							edge.add(new Integer(secondVar));
						} else {
							edge.add(new Integer(secondVar));
							edge.add(new Integer(firstVar));
						}
						addEdge(edge);
					}
				}
				//done giving every agent at least 1 connection, now add left overs
				for (int i=0; i < numEdges - numAgents; i++) {
					boolean done = false;
					while (!done) {
						Vector<Integer> edge = new Vector();
						int firstVar = r.nextInt(numAgents);
						int secondVar = r.nextInt(numAgents);
						while (firstVar == secondVar)
							secondVar = r.nextInt(numAgents);
						if (firstVar < secondVar) {	
							edge.add(new Integer(firstVar));
							edge.add(new Integer(secondVar));
						} else {
							edge.add(new Integer(secondVar));
							edge.add(new Integer(firstVar));
						}
						done = addEdge(edge);
					}
				}
			} else{
				for (int i=0; i < numEdges; i++) {
					boolean done = false;
					while (!done) {
						Vector<Integer> edge = new Vector();
						int firstVar = r.nextInt(numAgents);
						int secondVar = r.nextInt(numAgents);
						while (firstVar == secondVar)
							secondVar = r.nextInt(numAgents);
						if (firstVar < secondVar) {	
							edge.add(new Integer(firstVar));
							edge.add(new Integer(secondVar));
						} else {
							edge.add(new Integer(secondVar));
							edge.add(new Integer(firstVar));
						}
						done = addEdge(edge);
					}
				}
			}
		} else if (methodType == RANDOM_ATTACH) { // NOTE: this methodType gives NO GUARANTEE that every agent will have at least one f-constraint, may make unusable graphs!

			Integer firstAgent = new Integer(r.nextInt(numAgents));
			agentsInGraph.add(firstAgent);

			for (int i=0; i < numEdges; i++) {
				boolean done = false;
				while (!done) {
					int index = r.nextInt(agentsInGraph.size());
					Vector<Integer> edge = new Vector<Integer>(2);
					int firstVar = (agentsInGraph.get(index)).intValue();
					int secondVar = r.nextInt(numAgents);
					if (firstVar < secondVar) {
						edge.add(new Integer(firstVar));
						edge.add(new Integer(secondVar));
					} else {
						edge.add(new Integer(secondVar));
						edge.add(new Integer(firstVar));
					}
					done = addEdge(edge);
				}
			}

		} else if (methodType == PREF_ATTACH) {

			//

		} else if (methodType == CLUSTER) {

			//

		}
		/*		//here we pick up the agents without any edge - because they cannot have gains, cannot be relevant
		// *** another option is to give these agents a loopback edge, so they only have their own gCosts, rewards, gBudgets
		//+    but I haven't tested my MCMGM code with loopback edges yet, they may or may not work
		for (int i=0; i < numAgents; i++) {
			if (!(agentsInGraph.contains(i))){ //if this agent has no edge yet
				boolean done = false;
				while (!done) {
					Vector<Integer> edge = new Vector<Integer>();
					int secondVar = r.nextInt(numAgents);
					while (i == secondVar)
						secondVar = r.nextInt(numAgents);
					if (i < secondVar) {	//this if/else ordering is needed so that there aren't 2 edges between the same 2 agents 
						edge.add(new Integer(i));
						edge.add(new Integer(secondVar));
					} else {
						edge.add(new Integer(secondVar));
						edge.add(new Integer(i));
					}
					done = addEdge(edge);
				}
			}
		}
		 */	

	}	

	public static boolean addEdge(Vector<Integer> edge) {
		int a=edge.get(0);
		int b=edge.get(1);
		boolean done = false;
		if (!(edges.contains(edge))) {
			edges.add(edge);
			done = true;
		}
		neighbors[a].add(b);
		neighbors[b].add(a);
		if (!(agentsInGraph.contains(a)))
			agentsInGraph.add(a);
		if (!(agentsInGraph.contains(b)))
			agentsInGraph.add(b);
		return done;
	}		

	public static boolean neighborsHaveGConstraintK4(int id, int k){ //!!! *** ONLY works with k=4 (and maybe 5 ???) right now!!!
		agentsInGSubgraph.add(id);
		if (k<3) System.exit(1);//this should only be called if k>2, to keep the graph generating code for the previous tests consistant
		if (gConstraintNeighbors[id].size()==0){
			return true; //you have no g-constraints! 
		}else{
			for (int i=0; i<gConstraintNeighbors[id].size(); i++){
				traceGNeighbors(i);
			}
		}
		return true;
	}
	public static boolean traceGNeighbors(int id){
		if (agentsInGSubgraph.contains(id)) return true; //if the passed id is already considered, no need to retrace it
		agentsInGSubgraph.add(id);
		Iterator e = gConstraintNeighbors[id].iterator();
		while (e.hasNext()){
			traceGNeighbors(((Integer)e.next()).intValue());
//		for (int i=0; i<gConstraintNeighbors[id].size(); i++){
			/*int thisNeighbor = gConstraintNeighbors[id].get(i);
			if (gConstraintNeighbors[thisNeighbor].size()==1){ // if the g-constraint neighbor only has 1 g-constraint, that constraint must be with you!
				agentsInGSubgraph.add(thisNeighbor);
			}else{*/
//				traceGNeighbors(gConstraintNeighbors[id].get(i));
/*				for (int j=0; j<gConstraintNeighbors[thisNeighbor].size(); j++){
					if (gConstraintNeighbors[thisNeighbor].get(j) == thisNeighbor){
						agentsInGSubgraph.add(gConstraintNeighbors[thisNeighbor].get(j));
					}else{
						neighborsHaveGConstraintK4(thisNeighbor, k);
					}
//	old				agentsInGSubgraph.add(gConstraintNeighbors[gConstraintNeighbors[id].get(i)].get(j)); //add the current neighbor (aka j) of the the current neighbor(aka i) of the current agent (aka id)
				}*/
			//}
		}
		
		return false;
	}
	public static int neighborsofNeighborsGConstraints(int id){
		int returnable = 0;
// the below neighbor checking was used for test 4 and 5 		
		ArrayList<Integer> al = neighbors[id];
		for (int i=0; i<al.size(); i++){
			int currentNeighbor = al.get(i).intValue();
			if (agentsWithGConstraint.contains(currentNeighbor)){
				returnable++;
			}
		}
		return returnable;
	}
	public static void outputEdges(String outputFile) {
		agentGBudgets = new int[numAgents];
		try {

			PrintWriter out = new PrintWriter(new FileWriter(outputFile));
			out.println("#numAgents	"+numAgents+ "	domainSize	"+domainSize+ "	density	"+density+ "	methodType	"+methodType+ "	gBudgetRange	"+gBudgetRange+ "	gCostScaler	"+gCostScaler+ "	gBudgetCreationMethod	"+gBudgetCreationMethod+ "	gCostCreationMethod	"+gCostCreationMethod+ "	k	"+k+ "	numGConstraints	"+numGConstraints);
			if (gBudgetCreationMethod == RANDOM){
				for (int i=0; i < numAgents; i++){
					if (agentsInGraph.contains(i)){
						agentGBudgets[i] = r.nextInt(gBudgetRange);
						double minBudgetScaler = .4;
						while ( agentGBudgets[i] <= (int)( gBudgetRange*minBudgetScaler) || agentGBudgets[i] == 0 /* || ( gCost < (int)(agentGBudgets[i]*minCostScaler) || gCost <= (int)(agentGBudgets[j]*minCostScaler) )*/ ) agentGBudgets[i] = r.nextInt(gBudgetRange); 

						out.println("VARIABLE " + i + " 1 " + domainSize + " " + agentGBudgets[i]);
					}
				}
			}
			if (gBudgetCreationMethod == STATIC){
				for (int i=0; i < numAgents; i++){
					if (agentsInGraph.contains(i)){
						agentGBudgets[i] = gBudgetRange;
						out.println("VARIABLE " + i + " 1 " + domainSize + " " + gBudgetRange);
					}
				}
			}
			if (gCostCreationMethod == RANDOM){
				for (Vector<Integer> edge : edges) {
					out.println("CONSTRAINT " + edge.get(0).intValue() + " " + edge.get(1).intValue());
					for (int i=0; i < domainSize; i++) 
						for (int j=0; j < domainSize; j++) 
							if (i == j)
								out.println("F " + i + " " + j + " " + 0);
							else
								out.println("F " + i + " " + j + " " + 1);					
				}
				if (k>2){
					int count=0;
					gConstraintNeighbors= new ArrayList[numAgents];
					for (int i=0; i < numAgents; i++){gConstraintNeighbors[i]= new ArrayList<Integer>((int)(density*1.5));}
					while (numGConstraints > count){						
						int index = r.nextInt(edges.size());
						Vector<Integer> edge = edges.get(index);
						int a = edge.get(0).intValue();
						int b = edge.get(1).intValue();
						agentsInGSubgraph =  new HashSet<Integer>((int)(k*1.5)); //reset this Set, which holds the IDs of the agents within a g-constraint subgraph
						traceGNeighbors(a);
						traceGNeighbors(b);
						if (gConstraintNeighbors[a].contains(b)) continue; //if there is already a constraint between these two, try again
						agentsInGSubgraph.remove(null); //just in case
						if (agentsInGSubgraph.size() <= k){ // (*** seems broken at the moment, can't see why) check that their are not too many neighbors in the cluster this will join, if any  
							out.println("CONSTRAINT " + a + " " + b);
							out.println("# agentsInGSubgraph: "+agentsInGSubgraph.size()+" ");
							int overBudgetCount=0;//*ip
							int underBudgetCount=0;//*ip
							int higherGBudget;
							if (agentGBudgets[a] > agentGBudgets[b])
								higherGBudget = agentGBudgets[a];
							else
								higherGBudget = agentGBudgets[b];
							int maxGCost = (int)(Math.ceil(higherGBudget * gCostScaler)); //this value will be the max range for a random number generator, which creates a gCost garaunteed to be able to exceed at least one agent's gBudget 
							int gCost[][] = new int[domainSize][domainSize];
							while(overBudgetCount < overBudgetGoal || underBudgetCount < underBudgetGoal){//*ip
								overBudgetCount=0;//*ip
								underBudgetCount=0;//*ip
								for (int i=0; i < domainSize; i++) 
									for (int j=0; j < domainSize; j++){
										gCost[i][j] = r.nextInt(maxGCost); 
										//*** this while can be varied to create graphs with tight gBudgets vs. loose gBudgets
										//eg: add    || (gCost < agentGBudgets[i]*minCostScaler || gCost <= agentGBudgets[j]*minCostScaler) 
										//+ to make it a tighter gCost to gBudget ratio, but if I do that, probably should put a limit on number of edges per agent
										double minCostScaler = .0;
										//the following should probably be related to the how many neighboring agents agent i and agent j have
										//double maxCostScaler = 0.5;	//if you set this to 1/numNeighbors of either i or j (whichever has more neighbors)
										//+ then you are garenteed to have a solution, but it wouldn't be a very tightly bounded solution
										double maxCostScaler = (double)1/(double)k; //k is the number of agents in a group (aka the min MC-MGM-k you must use)
										//***	*ip	unfortunately the above will, along with ensuring there is a solution, also ensure the g-budget is useless as all combinations of values will fall within the g-budget
										//*old*/ while ( (gCost >= (int)( agentGBudgets[i]*maxCostScaler) || gCost >= (int)(agentGBudgets[j]*maxCostScaler) ) || gCost == 0 /* || ( gCost < (int)(agentGBudgets[i]*minCostScaler) || gCost <= (int)(agentGBudgets[j]*minCostScaler) )*/ )
										while ( gCost[i][j] == 0 /* || ( gCost < (int)(agentGBudgets[i]*minCostScaler) || gCost <= (int)(agentGBudgets[j]*minCostScaler) )*/ )	
											gCost[i][j] = (int)(r.nextInt(maxGCost));										
										if (gCost[i][j] > agentGBudgets[a] || gCost[i][j] > agentGBudgets[b])//*ip
											overBudgetCount++;
										else if (gCost[i][j] <= agentGBudgets[a] && gCost[i][j] <= agentGBudgets[b])//*ip
											underBudgetCount++;
									}
							}//end while
							for (int i=0; i < domainSize; i++) 
								for (int j=0; j < domainSize; j++)
									out.println("G " + i + " " + j + " " + gCost[i][j]);
							count++; // *** *ip this needs to be added to whatever spots I will reach whenever there is a g-constraint successfully added
							out.flush();
							agentsWithGConstraint.add(a);
							agentsWithGConstraint.add(b);
							gConstraintNeighbors[a].add(b);
							gConstraintNeighbors[b].add(a);
/*							Vector<Integer> gEdge = new Vector<Integer>(2);
							if (a < b) {
								gEdge.add(new Integer(a));
								gEdge.add(new Integer(b));
							} else {
								gEdge.add(new Integer(b));
								gEdge.add(new Integer(a));
							}
							gEdges.add(gEdge);*/
							
						}
					}//end while numGConstraints is not satisfied
/*	//obsoleted, combined with alg above				
				}else if (k>2){ //see graphgen backup for what used to be here
				}
*/					
				}else if (k==2){
					int count=0;
					while (numGConstraints > count){
						int index = r.nextInt(edges.size());
						Vector<Integer> edge = edges.get(index);
						int a = edge.get(0).intValue();
						int b = edge.get(1).intValue();
						if (!agentsWithGConstraint.contains(a) && !agentsWithGConstraint.contains(b)){ // (*** won't work for k > 2) check that their are not too many neighbors in the cluster this will join, if any  
							out.println("CONSTRAINT " + a + " " + b);
							int overBudgetCount=0;//*ip
							int underBudgetCount=0;//*ip
							int higherGBudget;
							if (agentGBudgets[a] > agentGBudgets[b])
								higherGBudget = agentGBudgets[a];
							else
								higherGBudget = agentGBudgets[b];
							int maxGCost = (int)(Math.ceil(higherGBudget * gCostScaler)); //this value will be the max range for a random number generator, which creates a gCost garaunteed to be able to exceed at least one agent's gBudget 
							int gCost[][] = new int[domainSize][domainSize];
							while(overBudgetCount < overBudgetGoal || underBudgetCount < underBudgetGoal){//*ip
								overBudgetCount=0;//*ip
								underBudgetCount=0;//*ip
								for (int i=0; i < domainSize; i++) 
									for (int j=0; j < domainSize; j++){
										gCost[i][j] = r.nextInt(maxGCost); 
										//*** this while can be varied to create graphs with tight gBudgets vs. loose gBudgets
										//eg: add    || (gCost < agentGBudgets[i]*minCostScaler || gCost <= agentGBudgets[j]*minCostScaler) 
										//+ to make it a tighter gCost to gBudget ratio, but if I do that, probably should put a limit on number of edges per agent
										double minCostScaler = .0;
										//the following should probably be related to the how many neighboring agents agent i and agent j have
										//double maxCostScaler = 0.5;	//if you set this to 1/numNeighbors of either i or j (whichever has more neighbors)
										//+ then you are garenteed to have a solution, but it wouldn't be a very tightly bounded solution
										double maxCostScaler = (double)1/(double)k; //k is the number of agents in a group (aka the min MC-MGM-k you must use)
										//***	*ip	unfortunately the above will, along with ensuring there is a solution, also ensure the g-budget is useless as all combinations of values will fall within the g-budget
										//*old*/ while ( (gCost >= (int)( agentGBudgets[i]*maxCostScaler) || gCost >= (int)(agentGBudgets[j]*maxCostScaler) ) || gCost == 0 /* || ( gCost < (int)(agentGBudgets[i]*minCostScaler) || gCost <= (int)(agentGBudgets[j]*minCostScaler) )*/ )
										while ( gCost[i][j] == 0 /* || ( gCost < (int)(agentGBudgets[i]*minCostScaler) || gCost <= (int)(agentGBudgets[j]*minCostScaler) )*/ )	
											gCost[i][j] = (int)(r.nextInt(maxGCost));										
										if (gCost[i][j] > agentGBudgets[a] || gCost[i][j] > agentGBudgets[b])//*ip
											overBudgetCount++;
										else if (gCost[i][j] <= agentGBudgets[a] && gCost[i][j] <= agentGBudgets[b])//*ip
											underBudgetCount++;
									}
							}//end while
							for (int i=0; i < domainSize; i++) 
								for (int j=0; j < domainSize; j++)
									out.println("G " + i + " " + j + " " + gCost[i][j]);
							count++; // *** *ip this needs to be added to whatever spots I will reach whenever there is a g-constraint successfully added
							agentsWithGConstraint.add(a);
							agentsWithGConstraint.add(b);
													
						}
					}//end while numGConstraints is not satisfied
				}else if (k==1){ //*** *ip INCOMPLETE ... this one should be for MC-MGM-1 minimum, meaning only create loopback g-constraints, only have to check the current agent doesn't have a g-constraint already... maybe?????
					int count=0;
					while (numGConstraints > count){// *** *ip NEED to replace this with something to determine how many times this should be done
						int index = r.nextInt(edges.size());
						Vector<Integer> edge = edges.get(index);
						int a = edge.get(0).intValue();
						if (!agentsWithGConstraint.contains(a)){ //check that their are not too many neighbors in the cluster this will join, if any  
							out.println("CONSTRAINT " + a + " " + a);
							int overBudgetCount=0;//*ip
							int underBudgetCount=0;//*ip							
							int maxGCost = (int)(Math.ceil(agentGBudgets[a] * /*gCostScaler*/1.3)); //this value will be the max range for a random number generator, which creates a gCost garaunteed to be able to exceed at least one agent's gBudget 
							int gCost[] = new int[domainSize];
							while(overBudgetCount < overBudgetGoal || underBudgetCount < underBudgetGoal){//*ip
								overBudgetCount=0;//*ip
								underBudgetCount=0;//*ip
								for (int i=0; i < domainSize; i++){
									gCost[i] = r.nextInt(maxGCost); 
									//*** this while can be varied to create graphs with tight gBudgets vs. loose gBudgets
									//eg: add    || (gCost < agentGBudgets[i]*minCostScaler || gCost <= agentGBudgets[j]*minCostScaler) 
									//+ to make it a tighter gCost to gBudget ratio, but if I do that, probably should put a limit on number of edges per agent
									double minCostScaler = .0;
									//the following should probably be related to the how many neighboring agents agent i and agent j have
									//double maxCostScaler = 0.5;	//if you set this to 1/numNeighbors of either i or j (whichever has more neighbors)
									//+ then you are garenteed to have a solution, but it wouldn't be a very tightly bounded solution
									double maxCostScaler = (double)1/(double)k; //k is the number of agents in a group (aka the min MC-MGM-k you must use)
									//***	*ip	unfortunately the above will, along with ensuring there is a solution, also ensure the g-budget is useless as all combinations of values will fall within the g-budget
//									*old*/while ( (gCost >= (int)( agentGBudgets[i]*maxCostScaler) || gCost >= (int)(agentGBudgets[j]*maxCostScaler) ) || gCost == 0 /* || ( gCost < (int)(agentGBudgets[i]*minCostScaler) || gCost <= (int)(agentGBudgets[j]*minCostScaler) )*/ )
									while ( gCost[i] == 0 /* || ( gCost < (int)(agentGBudgets[i]*minCostScaler) || gCost <= (int)(agentGBudgets[j]*minCostScaler) )*/ )
										gCost[i] = (int)(r.nextInt(maxGCost));
									if (gCost[i] > agentGBudgets[a])//*ip
										overBudgetCount++;
									else if (gCost[i] <= agentGBudgets[a])//*ip
										underBudgetCount++;
								}
							}
							for (int i=0; i < domainSize; i++)
								out.println("G " + i + " " + i + " " + gCost[i]);
							count++; // *** *ip this needs to be added to whatever spots I will reach whenever there is a g-constraint successfully added
							agentsWithGConstraint.add(a);
						}
					}
				}

			}


			out.close();
		} catch(Exception e) {
			System.out.println(e);
		}
	}		

	public static void inputEdges(String fileName) {
		BufferedReader instream = null;
		boolean done = false;
		String line = null;
		StringTokenizer t;
		try{
			instream = new BufferedReader(new FileReader(fileName));
		} catch(Exception e) {
			System.out.println(e);
			System.out.println("Could not open " + fileName);
			System.exit(0);
		}
		
		
		
//DIRECTLY FROM DCOP - partially edited		
		while(!done) {
			try {
				line = instream.readLine();
			} catch(Exception e) {
				System.out.println(e);
				done = true;
			}

			if (done || line == null)	{
				done = true;
				//System.out.println("  End of file reached.");
				break;
			}

			if(line.startsWith("#") || line.startsWith("\n"))
				continue;
		
			t = new StringTokenizer(line);
			if(t.hasMoreElements())
				t.nextToken(); // get rid of first token
			else
				continue;

/*			if(line.startsWith("VARIABLE")) {
				try {
					int vid = new Integer(t.nextToken()).intValue();
					t.nextToken(); // get rid of second token, which Jon added for some reason unknown to me
					int domainSize = new Integer(t.nextToken()).intValue();	
					
					//if/else to enable MCMGM or MGM formated dcop*.dat files
					variable v;
					if(t.hasMoreElements()){
						int gBudget = new Integer(t.nextToken()).intValue();
						v = new variable(vid, domainSize, gBudget);
					}else{
						v = new variable(vid, domainSize);
					}
					//end if/else to enable MCMGM or MGM formated dcop*.dat files
					vars.put(new Integer(vid), v);
					//System.out.println ("read variable " + v.id + " " + v.domainSize);
				} catch(Exception e) {
					System.out.print ("Error in VARIABLE line : " + e);
					System.out.println("    " + line);
				}
			} else */if(line.startsWith("CONSTRAINT")) {	//edited for GraphGen
				try {
					Vector<Integer> edge = new Vector();
					int firstVar = new Integer(t.nextToken()).intValue();
					int secondVar = new Integer(t.nextToken()).intValue();
					if (firstVar < secondVar) {	
						edge.add(new Integer(firstVar));
						edge.add(new Integer(secondVar));
					}else{
						edge.add(new Integer(secondVar));
						edge.add(new Integer(firstVar));
					}
					addEdge(edge);					
					
				} catch(Exception e) {
					System.out.print ("Error in CONSTRAINT line : " + e);
					System.out.println("    " + line);
				}
			}
		}
		
//END DIRECTLY FROM DCOP
		
		
		
	}
	
	public static void outputGEdges(String outputFile) { //obsoleted, see backups for what used to be here
		System.exit(2); //you should never reach this
	}
	
	public static void main (String args[]) {
		if (args.length < 4){
			System.out.println("Syntax: java graphgen output_filename " +
"numAgents domainSize density methodType [gBudgetRange gCostScaler gBudgetCreationMethod gCostCreationMethod k numGConstraints [input_filename]]");
			System.out.println("Available methodType values: 1 = RANDOM_PAIR; 2 = RANDOM_ATTACH");
			System.out.println("Available gBudgetCreationMethod and gCostCreationMethod values: 1 = RANDOM");
			System.exit(1);
		}
		String outputFile = args[0];
		numAgents = new Integer(args[1]).intValue();
		if (numAgents%2 != 0){
			System.out.println("numAgents must be even!");
			System.exit(1);
		}
		domainSize = new Integer(args[2]).intValue();
		density = new Double(args[3]).doubleValue();
		methodType = new Integer(args[4]).intValue();
		if (args.length > 5){
			neighbors = new ArrayList[numAgents]; 
			for(int i=0; i<numAgents; i++) neighbors[i] = new ArrayList<Integer>(); //unneeded, maybe??
			gBudgetRange = new Integer(args[5]).intValue();
			gCostScaler = new Double(args[6]).doubleValue();
			usingMC = true;
			gBudgetCreationMethod = new Integer(args[7]).intValue();
			k = new Integer(args[9]).intValue();
			agentsInGSubgraph = new HashSet<Integer>((int)(k*1.5)); //create a HashSet to be used later for checking whether a g-constraint can be added
			if (k>5 || k<1){
				System.out.println("supported values of k are only 1-5 at the moment!");
				System.exit(1);
			}
			numGConstraints = new Integer(args[10]).intValue();
		}
		
		if (args.length > 11){
			String inputFile = args[11]; //*ip
			inputEdges(inputFile);
//			outputGEdges(outputFile);
			outputEdges(outputFile);
			///
			///			*** This HACK only works when the f-constraint values are consistant, as in the initial if val1==val2 then f=0, if != then f=1
			///
			
			
			reparsing=true;
		}else{
			//if (args[4] != null) //old - jon's
			//	clusterSize = new Integer(args[5]).intValue(); //old - jon's
			createEdges();
			outputEdges(outputFile);
		}
		System.out.println("Created Successfully (it seems)!");
	}
}
