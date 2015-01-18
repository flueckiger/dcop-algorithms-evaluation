# /usr/bin perl

open (IN, "$ARGV[0]") or die "Can't open input file $ARGV[0]";
open (THRESH, "$ARGV[1]") or die "Can't open thresholds file $ARGV[1]";
open (PROB, ">PEAV-Problem-MeetingSchedule") or die "Can't open Problem-MeetingSchedule";
# number of participants
$num_parts = 0;
# counter used for creating ids for the events
$event_id = 0;

# sum of all possible free time and reward values.  a number calculated to be larger than any
# possible utility of a schedule as a workaround because this problem is phrased in terms of 
# rewards and ADOPT requires non-negative costs.
$offset = 1;



$num_constraints = 0;

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
         print ("offset is $offset\n");
      }
      # add the array to a hash referenced by the agent's id.
      $val_time{$elements[1]} = [@arr];
      @arr = ();
      
      $line2 = <THRESH>;
      @elements2 = split(" ", $line2);
      $c = 1;
      $d = 0;
      while ($elements2[$c] ne "]") {
      	$part_thresholds[$elements[1]][$c-1] = $elements2[$c];
      	$meetingTotalThresh += $elements2[$c];
      	#print ("$part_thresholds[$elements[1]][$c-1] ");
      	$c++;
      }
      #print("\n");
      
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
      # ids to an array of attendees and add the event_id to the hash of meetings involving that
      # participant.
      $cnt = 3;
      while ($elements[$cnt] ne "]")
      {
	 $j = $#{@{$part_meets{$parts{$elements[$cnt]}}}};
         if ($j == -1) {
	    ${@{$part_meets{$parts{$elements[$cnt]}}}}[0] = $event_id;
	 }
         else {
            ${@{$part_meets{$parts{$elements[$cnt]}}}}[$j + 1] = $event_id;
         }
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
      for ($part = 0; $part < $#{@{$meet_atts{$event_id}}} + 1; $part++) {
	  $meet_rews{$event_id." ".${@{$meet_atts{$event_id}}}[$part]} = $elements[$cnt];
          $cnt++;
      }

      # increment the event id counter now that we have finished creating this particular event.
      $event_id++;
   }
}

# Calculate an infinite cost as being the cube of the offset, which is a huge number in comparison
# to all potential utilities of states.
$inf = $offset * $offset;

# Major Workaround ... needs addressing
$adj = 1;
foreach $k (keys %parts) {
    $adj = $adj * ($#{@{$part_meets{$parts{$k}}}} + 1);
}
$adj = 420;

foreach $k (keys %parts) {
   $kid = $parts{$k};
   if ($#{@{$part_meets{$kid}}} == 0) {$consts{$k} = 1;}
   else {$consts{$k} = $#{@{$part_meets{$kid}}};}
}

# Print out the agents to the problem file.
#for ($x = 1; $x <= $num_parts; $x++) 
#{
#   print (PROB "AGENT $x\n");
#}

# Since we are running synchronously for our tests
print (PROB "AGENT 1\n");

#Set up vars and dummy_vars
$y = 0;
$z = $slots + 1;
foreach $x (keys %parts) 
{

    foreach $m (@{$part_meets{$parts{$x}}}) {
	$own = $parts{$x} + 1;
	#print (PROB "VARIABLE $y $own $z\n");
	#print (PROB "participant $x: ");
	#print (PROB "VARIABLE $y 1 $z\n");
	$vars{$x." ".$m} = $y;
	$y++;
	if ($#{@{$part_meets{$parts{$x}}}} == 0) {
	    #print (PROB "VARIABLE $y $own 1\n");
	    #print (PROB "VARIABLE $y 1 1\n");
	    $dummy_vars{$x} = $y;
	    $y++;
	}
    }
}


#count the constraints for each variable
# Intra-agent constraints
foreach $m (keys %vars) {
	$num_intra_constraints[$vars{$m}] = 0;
	$num_inter_constraints[$vars{$m}] = 0;
	$num_dummy_constraints[$vars{$m}] = 0;
}

foreach $m (keys %vars) {
foreach $n (keys %vars) {
   @m_elems = split(" ",$m);
   @n_elems = split(" ",$n);
      
   if (($m_elems[1] < $n_elems[1]) && ($m_elems[0] == $n_elems[0]))
   {
         $num_intra_constraints[$vars{$m}]++;
         $num_intra_constraints[$vars{$n}]++;
   }
	if (($m_elems[1] == $n_elems[1]) && ($m_elems[0] < $n_elems[0]))
   {
      $num_inter_constraints[$vars{$m}]++;
      $num_inter_constraints[$vars{$n}]++;
   }
}
}
foreach $m (keys %dummy_vars) {
    $n = $m." ".${@{$part_meets{$parts{$m}}}}[0];
    @n_elems = split(" ",$n);
    $num_dummy_constraints[$vars{$n}]++;
    $num_dummy_constraints[$dummy_vars{$m}]++;
}

# Each event is a variable so print out a variable declaration for each event, the variable number
# x is going to be the same as the event_id assigned to the event and y is the agent that owns x.
$y = 0;
$z = $slots + 1;

foreach $x (keys %parts) 
{
	$meetingcount = 0;
    foreach $m (@{$part_meets{$parts{$x}}}) {
	$own = $parts{$x} + 1;
	#print (PROB "VARIABLE $y $own $z\n");

	$threshold = $part_thresholds[$x][$meetingcount];
	print ("threshold for participant $x in meeting $m is $threshold\n");
	
	$temp = $num_inter_constraints[$vars{$x." ".$m}];
		#print ("number of inter constraints for var $y is $temp\n");

	$temp = $num_intra_constraints[$vars{$x." ".$m}];
			#print ("number of intra constraints for var $y is $temp\n");

	$temp = $num_dummy_constraints[$vars{$x." ".$m}];
	#print ("number of dummy constraints for var $y is $temp\n");
	
	$thresh = 0;
	if ($num_inter_constraints[$vars{$x." ".$m}] > 0) {
		$threshinter = $adj*$offset*$num_inter_constraints[$vars{$x." ".$m}]/2;
	} else {
		$threshinter = 0;
	}
	if ($num_intra_constraints[$vars{$x." ".$m}] > 0) {
		$threshintra = -$adj*($threshold - $offset*$num_intra_constraints[$vars{$x." ".$m}]/2);
	} else {
		$threshintra = 0;
	}
	if ($num_dummy_constraints[$vars{$x." ".$m}] > 0) {
		$threshdummy = -$adj*($threshold - $offset*$num_dummy_constraints[$vars{$x." ".$m}]/2);
	} else {
		$threshdummy = 0;
	}
	$thresh = $threshinter + $threshintra + $threshdummy;
	$totaladoptthresh += $thresh;
	print (PROB "VARIABLE $y 1 $z\n");
	$splitfactor = $num_intra_constraints[$vars{$x." ".$m}] + $num_dummy_constraints[$vars{$x." ".$m}];

	$meetingthresholds{$y} = $threshold/($splitfactor);
	#		print (PROB "unsplit threshold for $y is $threshold and we split it $splitfactor ways to get $meetingthresholds{$y}.\n");

	#$vars{$x." ".$m} = $y;
	$y++;
	if ($#{@{$part_meets{$parts{$x}}}} == 0) {
	    #print (PROB "VARIABLE $y $own 1\n");
	    $threshfordummy = $adj*$offset/2;
	    $totaladoptthresh += $threshfordummy;
	    print (PROB "VARIABLE $y 1 1\n");
	    #$dummy_vars{$x} = $y;
	    $y++;
	}
	$meetingcount++;
    }
}

# Intra-agent constraints
foreach $m (keys %vars) {
foreach $n (keys %vars) {
   @m_elems = split(" ",$m);
   @n_elems = split(" ",$n);
      
   if (($m_elems[1] < $n_elems[1]) && ($m_elems[0] == $n_elems[0]))
   {
   
   		$x = $vars{$m};
   		$y = $vars{$n};
		$thresh = $adj*($offset - $meetingthresholds{$vars{$m}} - $meetingthresholds{$vars{$n}});
		if (($thresh - int($thresh)) > .5)
           {
              $thresh = int($thresh+1);
           }
        $thresh = int($thresh);
		#print (PROB "threshold for $vars{$m} is $meetingthresholds{$vars{$m}}\n");
		#print (PROB "threshold for $vars{$n} is $meetingthresholds{$vars{$n}}\n");
   
   		$q = $vars{$m};
   		$r = $vars{$n};
   
        print (PROB "CONSTRAINT $q $r $thresh\n");
         $num_consts++;


         # Calculate the costs for each pair of values that could be taken by x and y.
         for($a = 0; $a <= $slots; $a++) {
            for ($b = 0; $b <= $slots; $b++) {

               # If they pick timeslot 0, which is effectively unscheduled then just have a cost
               # of the offset.
               if ($a == 0 && $b == 0) {
$off = $adj * $offset;  # delete me
                  if (($f - int($f)) > .5)
                  {
                  	$f = int($f+1);
                  }
                   $f = int($f);
                  print (PROB "F $a $b $off\n"); #adjust me
               }

               # If they are trying to select the same time, assign an infinite cost.
               elsif ( (($a >= $b) && ($a < $b + $event_lens{$n_elems[1]}))||
                       (($b >= $a) && ($b < $a + $event_lens{$m_elems[1]}))
                     ) {
$in = $adj * $inf; # delete me
                  if (($f - int($f)) > .5)
                  {
                  	$f = int($f+1);
                  }
                   $f = int($f);
                  print (PROB "F $a $b $in\n"); #adjust me
               }

	       # If either meeting is being scheduled for a slot that will cause it to run over the end of the 
	       # workday, then assign an infinite cost.
	       elsif (($a + $event_lens{$m_elems[1]} - 1 > $slots) || 
                      ($b + $event_lens{$n_elems[1]} - 1 > $slots) ) {
$in = $adj * $inf; # delete me
                  if (($f - int($f)) > .5)
                  {
                  	$f = int($f+1);
                  }
                   $f = int($f);
                  print (PROB "F $a $b $in\n"); #adjust me
	       }

               # If x is unscheduled but y is scheduled, subtract a fraction of the reward for
               # attending from the offset because negative reward roughly equals cost, and add
               # the value of the time given up by each attendee as an additional cost of 
               # scheduling.
               elsif ($a == 0) {
               #print (PROB " constraints for n = $consts{$n_elems[0]}, rewards for n = $meet_rews{$n_elems[1].\" \".$n_elems[0]}, value for n = ${@{$val_time{$n_elems[0]}}}[$b - 1]\n");
               
		   $f = $offset - (1 / $consts{$n_elems[0]}) * ($meet_rews{$n_elems[1]." ".$n_elems[0]}) 
                                + (1 / $consts{$n_elems[0]}) * (${@{$val_time{$n_elems[0]}}}[$b - 1]);
$f = $adj * $f; # delete me
                  if (($f - int($f)) > .5)
                  {
                  	$f = int($f+1);
                  }
                   $f = int($f);
                  print (PROB "F $a $b $f\n");
               }

               # As before but for x being scheduled and y unscheduled.
               elsif ($b == 0) {
                  $f = $offset - (1 / $consts{$m_elems[0]}) * ($meet_rews{$m_elems[1]." ".$m_elems[0]}) 
                               + (1 / $consts{$m_elems[0]}) * (${@{$val_time{$m_elems[0]}}}[$a - 1]);
$f = $adj * $f; # delete me
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
$in = $adj * $inf; #delete me
                  if (($f - int($f)) > .5)
                  {
                  	$f = int($f+1);
                  }
                   $f = int($f);
                  print (PROB "F $a $b $in\n"); #adjust me
               }

               # combine the calculations of the previous two cases for both being scheduled.
               else {
               #print (PROB " constraints for m = $consts{$m_elems[0]}, rewards for m = $meet_rews{$m_elems[1].\" \".$m_elems[0]}, value for m = ${@{$val_time{$m_elems[0]}}}[$a - 1]\n");

               #print (PROB " constraints for n = $consts{$n_elems[0]}, rewards for n = $meet_rews{$n_elems[1].\" \".$n_elems[0]}, value for n = ${@{$val_time{$n_elems[0]}}}[$b - 1]\n");

		  $f = $offset - (1 / $consts{$n_elems[0]}) * ($meet_rews{$n_elems[1]." ".$n_elems[0]})
                               + (1 / $consts{$n_elems[0]}) * (${@{$val_time{$n_elems[0]}}}[$b - 1])
                               - (1 / $consts{$m_elems[0]}) * ($meet_rews{$m_elems[1]." ".$m_elems[0]})
                               + (1 / $consts{$m_elems[0]}) * (${@{$val_time{$m_elems[0]}}}[$a - 1]);
$f = $adj * $f; # delete me
                  if (($f - int($f)) > .5)
                  {
                  	$f = int($f+1);
                  }
                   $f = int($f);
                  print (PROB "F $a $b $f\n");
               }

	   }
	}
   }
}
}

print (PROB "inter\n");

# Inter-agent constraints
foreach $m (keys %vars) {
foreach $n (keys %vars) {
   @m_elems = split(" ",$m);
   @n_elems = split(" ",$n);
   if (($m_elems[1] == $n_elems[1]) && ($m_elems[0] < $n_elems[0]))
   {
   
   		$q = $vars{$m};
   		$r = $vars{$n};
   		   		
   		if (defined($mergehash2{$q}))
   		{
   			$q = $mergehash2{$q};
   		}
   		if (defined($mergehash2{$r}))
   		{
   			$r = $mergehash2{$r};
   		}
      $thresh = $adj*$offset;
      print (PROB "CONSTRAINT $vars{$m} $vars{$n} $thresh\n");
      $num_consts++;

      # Calculate the costs for each pair of values that could be taken by x and y.
      for($a = 0; $a <= $slots; $a++) {
         for ($b = 0; $b <= $slots; $b++) {

            # If they pick the same timeslot, it's zero cost.
            if ($a == $b) {
$off = int($adj * $offset);# delete me
               print (PROB "F $a $b $off\n"); #adjust me
            }

            # If they disagree on when the meeting is, then it's infinite cost
            else 
            {
$in = int($adj * $inf); #delete me
               print (PROB "F $a $b $in\n"); #adjust me
            }
         }
      }
   }
}
}

# Dummy Intra-Agent 
print (PROB "dummy\n");

foreach $m (keys %dummy_vars) {

    $n = $m." ".${@{$part_meets{$parts{$m}}}}[0];
    @n_elems = split(" ",$n);
	$thresh = $adj*($offset - $meetingthresholds{$vars{$n}});
	if (($thresh - int($thresh)) > .5)
           {
              $thresh = int($thresh+1);
           }
    $thresh = int($thresh);

    print (PROB "CONSTRAINT $vars{$n} $dummy_vars{$m} $thresh\n");
    $num_consts++;

    # Calculate the costs for each pair of values that could be taken by x and y.
    for($a = 0; $a <= $slots; $a++) {
	# Calculate the costs.
	$f = $offset - (1 / $consts{$n_elems[0]}) * ($meet_rews{$n_elems[1]." ".$n_elems[0]}) 
	             + (1 / $consts{$n_elems[0]}) * (${@{$val_time{$n_elems[0]}}}[$a - 1]);
	
$f = $adj * $f; # delete me
                  if (($f - int($f)) > .5)
                  {
                  	$f = int($f+1);
                  }
                   $f = int($f);
	print (PROB "F $a 0 $f\n");
    }
}

$term = $num_consts * $offset;
print ("to untransform ADOPT implementation's optimal solution into meeting scheduling terms,\n");
print ("divide by $adj, subtract $term, and reverse the sign\n");
print ("to transform an optimal meeting scheduling solution back into ADOPT implementation terms,\n");
print ("subtract $term, multiply by $adj, and reverse the sign\n");
print ("num constraints is $num_consts\n");
print ("offset is $offset\n");
print ("meeting scheduling total threshold = $meetingTotalThresh\n");
print ("adopt total threshold = $totaladoptthresh\n");
