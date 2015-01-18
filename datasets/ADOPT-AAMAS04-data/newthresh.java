//
//  newthresh.java
//  newthresh
//
//  Created by Jonathan Pearce on Tue Dec 23 2003.
//  Copyright (c) 2003 __MyCompanyName__. All rights reserved.
//
import java.util.*;
import java.io.*;

public class newthresh {

    public class Event {
        ArrayList participants = new ArrayList();
        ArrayList values = new ArrayList();
    }

    int [][] participantTimes;
    int participantCount;
    ArrayList events;

    public void readInput(String fName) {
        participantTimes = new int[50][8];
		participantCount = 0;
        events = new ArrayList();
		
        BufferedReader instream = null;
        boolean done = false;
        String line = null;
        StringTokenizer t;
        
        try { 
            instream = new BufferedReader(new FileReader(fName)); 
        } catch(Exception e){
            System.out.println(e);
            System.out.println("Could not open " + fName);
            System.exit(0);
        }

    while(!done) {
            try {
                line = instream.readLine();
            } catch(Exception e) {
                System.out.println(e);
                done = true;
            }
      
            if (done || line == null)	{
                done = true;
                //System.out.println("End of file reached.");
                break;
            } 

            if(line.startsWith("#") || line.startsWith("\n") ){
                continue;
            }
      
            t = new StringTokenizer(line);
                
            if(line.startsWith("PARTICIPANT")){
                try {
                    //System.out.println ("reading participant line");
                    t.nextToken();
                    t.nextToken();
                    t.nextToken();
                    for (int i=0; i<8; i++) {
                        int timeValue = Integer.parseInt(t.nextToken());
                        participantTimes[participantCount][i] = timeValue;
                        //System.out.print(participantTimes[participantCount][i]);

                    }
                    participantCount++;
                } catch(Exception e){
                    System.out.println("  Error in PARTICIPANT line : " + e);
                }
            }

            if(line.startsWith("EVENT")){
                try {
                    //System.out.println ("reading event line");
					Event e = new Event();
                    t.nextToken();
                    t.nextToken();
                    t.nextToken();
                    String s = t.nextToken();
					//System.out.print ("event's participants are ");
                    while (!s.equals("]")) {
                        int participant = Integer.parseInt(s);
						e.participants.add(new Integer(participant));
                        //System.out.print(participant + ", ");
                        s = t.nextToken();
                    }
					//System.out.print (" and values are ");
                    t.nextToken();
                    s = t.nextToken();
                    while (!s.equals("]")) {
                        int meetingValue = Integer.parseInt(s);
						e.values.add(new Integer(meetingValue));
						//System.out.print(meetingValue + ", ");
                        s = t.nextToken();
                    }
					events.add(e);
					//System.out.println();
                } catch(Exception e){
                    System.out.println("  Error in EVENT line : " + e);
                }
            }
        }
    }

	public void produceOutput() {
	
		int numTimeSlots = 8;
		int masterSum = 0;
		for (int i=0; i<events.size(); i++) {
			int eventSum = 0;
			//System.out.print ("For event " + i + ":  ");
			Event e = (Event)events.get(i);
			int[] sumForTimeSlots = new int[numTimeSlots];
			for (int j=0;j<numTimeSlots;j++) {
				sumForTimeSlots[j] = 0;
				for (int k=0; k<e.participants.size(); k++) {
					Integer participant = (Integer)e.participants.get(k);
					sumForTimeSlots[j] += participantTimes[participant.intValue()][j];
				}   
				//System.out.print ("t" + j + "=" + sumForTimeSlots[j] + ", ");
			}
			//System.out.println();
			Arrays.sort(sumForTimeSlots);
			int sumOfRewards = 0;
			for (int l=0;l<e.values.size(); l++) {
				sumOfRewards += ((Integer)e.values.get(l)).intValue();
			}   
			eventSum = sumOfRewards - sumForTimeSlots[0];
			//System.out.println(eventSum + " = " + sumOfRewards + " - " + sumForTimeSlots[0]);
			System.out.println(eventSum);
			masterSum += eventSum;
		}
		//System.out.println(masterSum);
	}		

    public static void main (String args[]) {
        String fName = args[0];
        newthresh c = new newthresh();
        c.readInput(fName);
        c.produceOutput();
        //System.out.println("Done!");
    }
}
