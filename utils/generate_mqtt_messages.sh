#!/bin/bash

[ -z "$HOST" ] && HOST=localhost

publish() {
	TIMESTAMP=`date +%s%3N`
	mosquitto_pub -h $HOST -t 'ball/position/rel' -m "$TIMESTAMP,$1"
	sleep 0.04
}

while [ 1 ]
do
	type=$((RANDOM % 100))
	if [ "$type" -ge 0 -a "$type" -lt 80 ]; then
		times=$((RANDOM % 100))
		for (( c=1; c<=$times; c++ ))
		do  
			x=$((RANDOM % 10000))
			y=$((RANDOM % 10000))
			publish "0.$x,0.$y"
		done
	fi
	if [ "$type" -ge 80 -a "$type" -lt 95 ]; then
		times=$((RANDOM % 100))
		for (( c=1; c<=$times; c++ ))
		do  
			publish "-1.0,-1.0"
		done
	fi
	if [ "$type" -ge 95 -a "$type" -lt 100 ]; then
		publish "-1.0,-1.0"
	fi

done
