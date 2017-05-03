package main

import (
	"encoding/json"
	"expvar"
	"log"
	"strconv"
	"time"

	"cloud.google.com/go/pubsub"
)

// TaxiRidePointPusher is responsible to push Points of a taxi ride according to their timestamps to pubsub
type ridePointScheduler struct {
	taxiRide      taxiRide
	refTime       time.Time
	timeOffset    time.Duration
	speedupFactor int
	points        []*taxiRidePoint
	idxCount      int
	debugLog      debugging
	likedAd       bool
	routeInfoFreq int
}

var (
	schedDebugCounter = expvar.NewMap("schedulerCounters")
)

const (
	ridesLoadedKey     = "ridesloaded"
	ridesProcessedKey  = "ridesprocessed"
	ridesInvalidKey    = "ridesinvalid"
	pointsLoadedKey    = "pointsloaded"
	pointsScheduledKey = "pointsscheduled"
	pointsFailedKey    = "pointsfailed"
)

func (rps *ridePointScheduler) run(ch chan<- *pubsub.Message) {
	defer func() {
		schedDebugCounter.Add(pointsFailedKey, int64(len(rps.points)))
		schedDebugCounter.Add(ridesProcessedKey, 1)
	}()
	// delay expanding of taxi ride polyline to points till ride starts
	pt, err := rps.taxiRide.pickupTime()
	if err != nil {
		log.Printf("Error scheduling ride for publish: %v ", err)
		return
	}

	// real time passed since refTime
	rtd := time.Now().Sub(rps.refTime)
	// time duration from reftime with speedup considered
	d := int64(pt.Add(rps.timeOffset).Sub(rps.refTime).Nanoseconds() / int64(rps.speedupFactor))
	time.Sleep(time.Duration(d - rtd.Nanoseconds()))

	rps.points, err = rps.taxiRide.ridePoints(rps.refTime, rps.timeOffset, timeLocation, rps.speedupFactor, rps.likedAd, rps.routeInfoFreq)
	if err != nil {
		log.Printf("Unable to generate taxi ride points: %v", err)
		return
	}

	schedDebugCounter.Add(ridesLoadedKey, 1)
	schedDebugCounter.Add(pointsLoadedKey, int64(len(rps.points)))
	for {
		// if no more point in list exit
		if len(rps.points) == 0 {
			return
		}

		// calculate time to wait till next ride point to push to pubsub
		timestamp, err := parseTimeString(rps.points[0].Timestamp)
		if err != nil {
			// log error and exit this ride push
			log.Printf("Error parsing timestamp of taxi ride point. RideID: %v, Point idx: %v, Error: %v",
				rps.points[0].RideID, rps.idxCount, err)
			return
		}
		time.Sleep(timestamp.Sub(time.Now()))
		if rps.debugLog {
			schedDebugCounter.Add(pointsScheduledKey, 1)
		}

		pointJSON, err := json.Marshal(rps.points[0])
		if err != nil {
			log.Printf("Error creating json for taxiRide: %v", err)
			return
		}
		// send message on publish channel
		attributes := map[string]string{"ts": rps.points[0].Timestamp}
		// attributes := map[string]string{"ts": time.Now().Format(outputDateTimeLayout)}

		ch <- &pubsub.Message{Data: pointJSON, Attributes: attributes}
		rps.idxCount++
		rps.points = rps.points[1:]
	}
}

func ridesLoaded() int {
	rl := schedDebugCounter.Get(ridesLoadedKey)
	if rl == nil {
		return 0
	}
	rli, err := strconv.Atoi(rl.String())
	if err != nil {
		log.Printf("Error getting rides loaded count. %v", err)
		return 0
	}
	return rli
}

func ridesProcessed() int {
	rp := schedDebugCounter.Get(ridesProcessedKey)
	if rp == nil {
		return 0
	}
	rpi, err := strconv.Atoi(rp.String())
	if err != nil {
		log.Printf("Error getting rides processed count. %v", err)
		return 0
	}
	return rpi
}

func ridesInvalid() int {
	ri := schedDebugCounter.Get(ridesInvalidKey)
	if ri == nil {
		return 0
	}
	rii, err := strconv.Atoi(ri.String())
	if err != nil {
		log.Printf("Error getting rides invalid count. %v", err)
		return 0
	}
	return rii
}

func pointsLoaded() int {
	pl := schedDebugCounter.Get(pointsLoadedKey)
	if pl == nil {
		return 0
	}
	pli, err := strconv.Atoi(pl.String())
	if err != nil {
		log.Printf("Error getting points loaded count. %v", err)
		return 0
	}
	return pli
}

func pointsScheduled() int {
	pp := schedDebugCounter.Get(pointsScheduledKey)
	if pp == nil {
		return 0
	}
	ppi, err := strconv.Atoi(pp.String())
	if err != nil {
		log.Printf("Error getting points scheduled count. %v", err)
		return 0
	}
	return ppi
}

func pointsFailed() int {
	pf := schedDebugCounter.Get(pointsFailedKey)
	if pf == nil {
		return 0
	}
	pfi, err := strconv.Atoi(pf.String())
	if err != nil {
		log.Printf("Error getting points failed count. %v", err)
		return 0
	}
	return pfi
}

func parseTimeString(timestamp string) (time.Time, error) {
	return time.ParseInLocation(outputDateTimeLayout, timestamp, timeLocation)
}
