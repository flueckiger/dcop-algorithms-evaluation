# /usr/bin perl

open (IN, "$ARGV[0]") or die "Can't open input file $ARGV[0]";
open (THRESH, "$ARGV[1]") or die "Can't open thresholds file $ARGV[1]";
open (PROB, ">Problem-MeetingSchedule") or die "Can't open Problem-MeetingSchedule";

# number of participants
$num_parts = 0;
# counter used for creating ids for the events
$event_id = 0;

# sum of all possible free time and reward values.  a number calculated to be larger than any
# possible utility of a schedule as a workaround because this problem is phrased in terms of 
# rewards and ADOPT requires non-negative costs.
$offset = 1;

# read in the thresholds.
$threshindex = 0;
while ($line2 = <THRESH>) 
{   
	@elements2 = split(" ", $line2);
	#print ("$elements2[0]");
   	$thresholds[$threshindex] = $elements2[0];
	$threshindex++;
}

# read in the input file.
while ($line = <IN>) 
{
   @elements = split(" ", $line);

   # specifies the number of timeslots 
   # line: TIMESLOTS <# of timeslots> 
   if ($elements[0] eq "TIMESLOTS") 
   {
      $slots = $elements[1];                  
   }
   # specifies the name of the participant and the values he/she places on his/her free time,
   # or espressed differently, the cost to that person of giving up each timeslot for a 
   # meeting.
   # line: PARTICIPANT <participant-id> [ <values-for-each-timeslot> ]
   elsif ($elements[0] eq "PARTICIPANT") 
   {
      # store the participant's ID 
      $parts{$elements[1]} = $num_parts;
      $num_parts++;

      # create an array of the participant's values for free time and increment the offset
      for ($x = 0; $x < $slots; $x++)
      {
         $arr[$x] = $elements[$x + 3];
         $offset += $elements[$x + 3];
      }
      # add the array to a hash referenced by the agent's id.
      $val_time{$elements[1]} = [@arr];
      @arr = ();
   }
   # specifies an event and the length, participants and rewards of attending for each attendee.
   # line: EVENT <length in slots> [ <participant ids> ] [ <rewards per participant> ]
   else 
   {
      # store the length of the meeting, indexed by the event_id, which is an integer that counts
      # from 0.  this will be used as the variable name representing that meeting in the ADOPT 
      # specification.
      $event_lens{$event_id} = $elements[1];

      # starting at the third element of the line until hitting the first ']' add the participant
      # ids to an array of attendees.
      $cnt = 3;
      while ($elements[$cnt] ne "]")
      {
         $arr1[$cnt - 3] = $elements[$cnt];
         $cnt++;
      }
      # add the array to a hash indexed by the meeting id.
      $meet_atts{$event_id} = [@arr1];
      @arr1 = ();

      # starting at the first non-bracket element after leaving off reading the line and until 
      # hitting the next ']' add the player rewards to an array.  also, increment the offset with
      # each reward.
      $cnt = $cnt + 2;
      for($x = $cnt; $x < $#elements; $x++)
      {
         $arr2[$x - $cnt] = $elements[$x];
         $offset += $elements[$x];
      }
      # assign the array of awards to a hash indexed by the event id.
      $meet_rews{$event_id} = [@arr2];
      @arr2 = ();

      # increment the event id counter now that we have finished creating this particular event.
      $event_id++;
   }
}

# Iterate through all possible pairs of events to see if they have any participants in common and
# thus need to have a constraint between them.
for ($x = 0; $x < $event_id; $x++)
{
   for ($y = 0; $y < $event_id; $y++)
   {
      # Obviously don't have a constraint between an event and itself.
      if ($x != $y) 
      {
         # flag to prevent double counting if meetings have more than one participant in common.
         $br = 0;

         # Iterate through each pair of participants in these two meetings checking to see if the
         # same person shows up in both meetings.
         foreach $a (@{$meet_atts{$x}})
         {
            foreach $b (@{$meet_atts{$y}})
            {
               # If the same person is in found in both meetings and we haven't already added a 
               # constraint between this pair of meetings, set the matrix entry consts[x][y] to 1
               # and:

               # Either add a new entry to the number of constraints on that meeting
               if (($a eq $b) && ($num_consts{$x} == null) && !$br)
               {
                  $consts[$x][$y] = 1;
		  $consts[$y][$x] = 1;
                  $num_consts{$x} = 1;
                  $br = 1;
               }
               # Or increment it.
               if (($a eq $b) && ($num_consts{$x} != null) && !$br)
               {
		  $consts[$x][$y] = 1;
                  $consts[$y][$x] = 1;
                  $num_consts{$x}++;
                  $br = 1;
               }
            }
         }
      }
   }
}

# Calculate an infinite cost as being the cube of the offset, which is a huge number in comparison
# to all potential utilities of states.
$inf = $offset * $offset;

# Major WORKAROUND ... needs addressing
$adj = 1;
$total_consts = 0;
foreach $k (keys %num_consts) {
    $adj = $adj * $num_consts{$k};
    $total_consts = $total_consts + $num_consts{$k};
}
$adj = 420;

#Print out transformation information as comments in the problem file.
$term = $offset * $total_consts / 2;
#print ("offset = $offset, total consts = $total_consts\n");
print ("#to untransform ADOPT implementation's optimal solution into meeting scheduling terms,\n");
print ("#divide by $adj, subtract $term, and reverse the sign\n");
print ("#to transform an optimal meeting scheduling solution back into ADOPT implementation terms,\n");
print ("#subtract $term, multiply by $adj, and reverse the sign\n");


# Print out the agents to the problem file.
print (PROB "AGENT 1\n");


# Each event is a variable so print out a variable declaration for each event, the variable number
# x is going to be the same as the event_id assigned to the event and y is the agent that owns x
# which is just x + 1 because 0 can't be an agent id.
for ($x = 0; $x < $event_id; $x++) 
{
   $y = $x + 1;
   $z = $slots + 1;
   #print ("event $x, threshold $thresholds[$x], consts $num_consts{$x}\n");
   #$thresh = -$adj*($thresholds[$x] - $offset*$num_consts{$x}/2);
   print (PROB "VARIABLE $x 1 $z\n");
}


# Iterate through each pair of events and add a constraint if necessary as indicated by the
# constr[x][y] being set to 1 and x < y to prevent double printing.
for ($x = 0; $x < $event_id; $x++)
{
   for ($y = 0; $y < $event_id; $y++)
   {
       #print("x: $x  y: $y  consts: $consts[$x][$y]\n");
      if (($x < $y) && ($consts[$x][$y] == 1)) {
      	 $thresh = $adj*($offset - $thresholds[$x]/$num_consts{$x} - $thresholds[$y]/$num_consts{$y});
         print (PROB "CONSTRAINT $x $y $thresh\n");

         # Calculate the costs for each pair of values that could be taken by x and y.
         for($a = 0; $a <= $slots; $a++) {
            for ($b = 0; $b <= $slots; $b++) {

               # If they pick timeslot 0, which is effectively unscheduled then just have a cost
               # of the offset.
               if ($a == 0 && $b == 0) {
$off = int($adj * $offset); #delete me
                  print (PROB "F $a $b $off\n"); #change me back
               }

	       # If either meeting is being scheduled for a slot that will cause it to run over
	       # the end of the workday, then assign an infinite cost.
	       elsif (($a + $event_lens{$x} - 1) > $slots || ($b + $event_lens{$y} - 1 > $slots)) {
$f = $adj * $inf; #delete me
           if (($f - int($f)) > .5)
           {
                $f = int($f+1);
           }
		   print (PROB "F $a $b $f\n"); #adjust me
	       }

               # If x is unscheduled but y is scheduled, subtract a fraction of the reward for
               # attending from the offset because negative reward roughly equals cost, and add
               # the value of the time given up by each attendee as an additional cost of 
               # scheduling.
               elsif ($a == 0) {
                  $f = $offset;
                  foreach $i (@{$meet_rews{$y}})
                  {
                     $f = $f - ((1 / $num_consts{$y}) * $i);
                  }
                  foreach $i (@{$meet_atts{$y}})
                  {
                     @array1 = @{$val_time{$i}};
                     $f = $f + ((1 / $num_consts{$y}) * $array1[$b - 1]);

                  }
$f = $adj * $f; #delete me
                  if (($f - int($f)) > .5)
                  {
                  	$f = int($f+1);
                  }
                   $f = int($f);
                  print (PROB "F $a $b $f\n");
               }

               # As before but for x being scheduled and y unscheduled.
               elsif ($b == 0) {
                  $f = $offset;
                  foreach $i (@{$meet_rews{$x}})
                  {
                     $f = $f - ((1 / $num_consts{$x}) * $i);
                  }
                  foreach $i (@{$meet_atts{$x}})
                  {
                     @array1 = @{$val_time{$i}};
                     $f = $f + ((1 / $num_consts{$x}) * $array1[$a - 1]);
                  }
$f = $adj * $f; #delete me
                  if (($f - int($f)) > .5)
                  {
                  	$f = int($f+1);
                  }
                  $f = int($f);
                  print (PROB "F $a $b $f\n");

               }

               # If they are trying to select the same time, assign an infinite cost.
               elsif ( (($a >= $b) && ($a < $b + $event_lens{$y}))||
                       (($b >= $a) && ($b < $a + $event_lens{$x}))
                     ) {
$f = $adj * $inf; #delete me
                  if (($f - int($f)) > .5)
                  {
                  	$f = int($f+1);
                  }
                 $f = int($f);
                  print (PROB "F $a $b $f\n"); #adjust me
               }

               # combine the calculations of the previous two cases for both being scheduled.
               else {
                  $f = $offset;
                  foreach $i (@{$meet_rews{$y}})
                  {
                     $f = $f - ((1 / $num_consts{$y}) * $i);
                  }
                  foreach $i (@{$meet_atts{$y}})
                  {
                     @array1 = @{$val_time{$i}};
                     $f = $f + ((1 / $num_consts{$y}) * $array1[$b - 1]);
                  }
                  foreach $i (@{$meet_rews{$x}})
                  {
                     $f = $f - ((1 / $num_consts{$x}) * $i);
                  }
                  foreach $i (@{$meet_atts{$x}})
                  {
                     @array1 = @{$val_time{$i}};
                     $f = $f + ((1 / $num_consts{$x}) * $array1[$a - 1]);
                  }
     
$f = $adj * $f; #delete me
                  if (($f - int($f)) > .5)
                  {
                  	$f = int($f+1);
                  }
                  $f = int($f);
                  print (PROB "F $a $b $f\n");
               }

               # reset the array.
               @array1 = ();
            }
         }

      }
   }
}
