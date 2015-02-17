//
//  peavthresh.java
//  peavthresh
//
//  Created by Jonathan Pearce on Tue Jan 06 2004.
//  Copyright (c) 2004 __MyCompanyName__. All rights reserved.
//
import java.util.*;
import java.io.*;

public class peavthresh {

	int [][] participantTimeValues;
	int [][] participantMeetingRewards;
	int currentParticipantId;
	int currentMeetingId;
	int numTimeSlots = 8;

	public void readInput(String fName) {
        participantTimeValues = new int[50][numTimeSlots];
        participantMeetingRewards = new int[50][50];
        for (int x=0;x<50;x++)
			for (int y=0;y<50;y++)
				participantMeetingRewards[x][y] = 0;
		currentParticipantId = 0;
		currentMeetingId = 0;
		
        
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
                    for (int i=0; i<numTimeSlots; i++) {
                        int timeValue = Integer.parseInt(t.nextToken());
                        participantTimeValues[currentParticipantId][i] = timeValue;
                        //System.out.print(participantTimeValues[currentParticipantId][i]);

                    }
					currentParticipantId++;
                } catch(Exception e){
                    System.out.println("  Error in PARTICIPANT line : " + e);
                }
            }

            if(line.startsWith("EVENT")){
                try {
                    //System.out.println ("reading event line");

                    t.nextToken();
                    t.nextToken();
                    t.nextToken();
                    String s = t.nextToken();
					ArrayList participantsInMeeting = new ArrayList();
					ArrayList rewardsForMeeting = new ArrayList();
					Integer participant = new Integer(0);
                    while (!s.equals("]")) {
                        int p = Integer.parseInt(s);
                        participantsInMeeting.add(new Integer(p));
                        s = t.nextToken();
                    }
                    t.nextToken();
                    s = t.nextToken();
					Integer meetingValue = new Integer(0);
                    while (!s.equals("]")) {
						int m = Integer.parseInt(s);
                        rewardsForMeeting.add(new Integer(m));
                        s = t.nextToken();
                    }
					for (int index=0; index<participantsInMeeting.size();index++) {
						int p = ((Integer)participantsInMeeting.get(index)).intValue();
						int m = ((Integer)rewardsForMeeting.get(index)).intValue();
						participantMeetingRewards[p][currentMeetingId] = m;
						//System.out.println("Participant " + p + " has meeting " + currentMeetingId + " with value " + m);
					}
					currentMeetingId++;
						
					
                } catch(Exception e){
                    System.out.println("  Error in EVENT line : " + e);
                }
            }
		}
	}

	public void produceOutput() {
		int i;
        for (i=0;i<currentParticipantId;i++) {
            int[] sortedArray = new int[numTimeSlots];
            int j;
            //System.out.print ("participant " + i + "'s times are ");
            for (j=0;j<numTimeSlots;j++) {
                //System.out.print(participantTimeValues[i][j] + ", ");
                sortedArray[j] = participantTimeValues[i][j];
            }
			//System.out.println();
			Arrays.sort(sortedArray);
            //System.out.print ("participant " + i + "'s sorted times are ");
            for (j=0;j<numTimeSlots;j++) {
                //System.out.print(sortedArray[j] + ", ");
                participantTimeValues[i][j] = sortedArray[j];
            }
						//System.out.println();

		}
        for (i=0;i<currentParticipantId;i++) {
			int nextBestTimeSlot = 0;
			System.out.print("[ ");
			for (int j=0;j<currentMeetingId;j++) {
				int m = participantMeetingRewards[i][j];
				if (m > 0) {
					int netGain = m - participantTimeValues[i][nextBestTimeSlot];
					System.out.print(netGain + " ");
					//System.out.println("net gain for participant " + i + " in meeting " + j + " is " + m + " - " + participantTimeValues[i][nextBestTimeSlot] + " = " + netGain);
					//nextBestTimeSlot++;
				}
			}
			System.out.println("]");
		}
	}


    public static void main (String args[]) {
        // insert code here...
		String fName = args[0];
        peavthresh c = new peavthresh();
        c.readInput(fName);
		c.produceOutput();
    }
}
