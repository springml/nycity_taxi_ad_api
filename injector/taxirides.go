package main

import (
	"encoding/json"
	"io"
	"math/rand"
	"strconv"
	"time"

	"googlemaps.github.io/maps"
)

//taxiRide json object definition extracted from yellow_tripdata_2015 json files
type taxiRide struct {
	TotalAmount          float32 `json:"total_amount,string"`
	PassengerCount       int32   `json:"passenger_count,string"`
	RateCodeID           string  `json:"RateCodeID"`
	VendorID             string  `json:"VendorID"`
	PickupLongitude      float64 `json:"pickup_longitude,string"`
	PickupLatitude       float64 `json:"pickup_latitude,string"`
	DropoffLongitude     float64 `json:"dropoff_longitude,string"`
	DropoffLatitude      float64 `json:"dropoff_latitude,string"`
	MTATax               string  `json:"mta_tax"`
	TPepPickupDatetime   string  `json:"tpep_pickup_datetime"`
	TPepDropoffDatetime  string  `json:"tpep_dropoff_datetime"`
	StoreAndFwdFlag      string  `json:"store_and_fwd_flag"`
	TipAmount            float32 `json:"tip_amount,string"`
	ImprovementSurcharge float32 `json:"improvement_surcharge,string"`
	TripDistance         string  `json:"trip_distance"`
	PaymentType          int32   `json:"payment_type,string"`
	FareAmount           float32 `json:"fare_amount,string"`
	TollsAmount          float32 `json:"tolls_amount,string"`
	Polyline             string  `json:"polyline"`
	Extra                float32 `json:"extra,string"`
}

type taxiRides struct {
	Rides []taxiRide `json:"rides"`
}

// Point {
// Lat,
// Long,
// Timesamp,
// Ride Id,
// Meter, // $$$
// Meter increment, // $$$
// Flag “pick up”, “drop off” or “en route”
// }
type taxiRidePoint struct {
	RideID              string  `json:"ride_id"`
	PointIdx            int     `json:"point_idx"`
	Latitude            float64 `json:"latitude"`
	Longitude           float64 `json:"longitude"`
	Timestamp           string  `json:"timestamp"`
	RideStatus          string  `json:"ride_status"`
	PassengerCount      int32   `json:"passenger_count"`
	Campaign            int     `json:"campaign"`
	UserLikedAdd        bool    `json:"user_liked_ad"`
	DestinationLocation string  `json:"destination_location"`
	PickupLocation      string  `json:"pickup_location"`
	RouteInfo           string  `json:"route_info"`
	TPepPickupDatetime  string  `json:"tpep_pickup_datetime"`
}

const (
	pickup  = "pickup"
	enroute = "enroute"
	dropoff = "dropoff"
)

func ridesFromJSONRawBytes(jsonRaw []byte) (*taxiRides, error) {
	var taxiRides taxiRides
	err := json.Unmarshal(jsonRaw, &taxiRides)
	if err != nil {
		return nil, err
	}
	return &taxiRides, nil
}

func ridesFromJSONRawIOReader(reader io.ReadCloser) (*taxiRides, error) {
	defer reader.Close()
	jsonReader := json.NewDecoder(reader)
	var taxiRides *taxiRides
	if err := jsonReader.Decode(&taxiRides); err != nil {
		return nil, err
	}
	return taxiRides, nil
}

func (tr *taxiRide) pickupTime() (time.Time, error) {
	pt, err := time.ParseInLocation(inputDatasetDateTimeLayout, tr.TPepPickupDatetime, timeLocation)
	if err != nil {
		return time.Now(), err
	}
	return pt, nil
}

func (tr *taxiRide) dropoffTime() (time.Time, error) {
	dt, err := time.ParseInLocation(inputDatasetDateTimeLayout, tr.TPepDropoffDatetime, timeLocation)
	if err != nil {
		return time.Now(), err
	}
	return dt, nil
}

func randomizePassengerCount(origPassCount int32) int32 {
	randInt := rand.Int31n(20) + 1
	if randInt == 10 {
		return randInt
	}

	return origPassCount
}

func computeCampaign(loc_lat float64, loc_lon float64) int {
	switch {
	case ((loc_lat > 40.799429) && (loc_lon > -74.018328) && (loc_lon <= -73.931083)):
		return 0
	case ((loc_lat > 40.799429) && (loc_lon > -73.931083)):
		return 1
	case ((loc_lat < 40.799429) && (loc_lat >= 40.765747) && (loc_lon > -74.018328) && (loc_lon <= -73.977816)):
		return 2
	case ((loc_lat < 40.799429) && (loc_lat >= 40.765747) && (loc_lon > -74.977816) && (loc_lon <= -73.931083)):
		return 3
	case ((loc_lat < 40.799429) && (loc_lat >= 40.765747) && (loc_lon > -73.931083)):
		return 4
	case ((loc_lat < 40.765747) && (loc_lat >= 40.740325) && (loc_lon > -74.018328) && (loc_lon <= -73.977816)):
		return 5
	case ((loc_lat < 40.740325) && (loc_lat >= 40.72747) && (loc_lon > -74.018328) && (loc_lon <= -73.977816)):
		return 6
	case ((loc_lat < 40.72747) && (loc_lat >= 40.70297) && (loc_lon > -74.018328) && (loc_lon <= -73.977816)):
		return 7
	case ((loc_lat < 40.765747) && (loc_lat >= 40.70297) && (loc_lon > -73.977816)):
		return 8
	default:
		return 9
	}
}

func getLatLong(loc_lat float64, loc_lon float64) string {
	latStr := strconv.FormatFloat(loc_lat, 'f', -1, 64)
	lonStr := strconv.FormatFloat(loc_lon, 'f', -1, 64)

	return latStr + "," + lonStr
}

// ridePoints decodes taxiRide polyline and adds information to each point according to
// the taxiRidePoint format (a timestamp is calculated from refTime, timeOffset and speedupFactor)
// refTime is the time that represents the new dataset "start" time
// timeOffset is the time between the original dataset "start" time and the refTime
func (tr *taxiRide) ridePoints(refTime time.Time, timeOffset time.Duration, rideTimeLocation *time.Location, speedupFactor int, userLikedAd bool, routeInfoFreq int) ([]*taxiRidePoint, error) {
	points := maps.DecodePolyline(tr.Polyline)
	rideID, err := newUUID()
	if err != nil {
		return nil, err
	}
	parsedPickupTimeStamp, err := time.ParseInLocation(inputDatasetDateTimeLayout, tr.TPepPickupDatetime, rideTimeLocation)
	if err != nil {
		return nil, err
	}
	parsedDropOffTimeStamp, err := time.ParseInLocation(inputDatasetDateTimeLayout, tr.TPepDropoffDatetime, rideTimeLocation)
	if err != nil {
		return nil, err
	}
	var timeStampIncrement int64
	if len(points) < 2 {
		timeStampIncrement = parsedDropOffTimeStamp.Sub(parsedPickupTimeStamp).Nanoseconds() / int64(speedupFactor)
	} else {
		timeStampIncrement = parsedDropOffTimeStamp.Sub(parsedPickupTimeStamp).Nanoseconds() / int64(len(points)-1) / int64(speedupFactor)
	}

	taxiRidePoints := make([]*taxiRidePoint, len(points))
	var rideStatus = pickup

	startTimeStamp := parsedPickupTimeStamp.Add(timeOffset)

	if speedupFactor != 1 {
		// taxi ride start time diff to refTime in realtime
		td := startTimeStamp.Sub(refTime)
		// adjust start time by speedupFactor
		startTimeStamp = startTimeStamp.Add(time.Duration(td.Nanoseconds()/int64(speedupFactor)) - td)
	}

	destLocation := getLatLong(tr.DropoffLatitude, tr.DropoffLongitude)
	pickLocation := getLatLong(tr.PickupLatitude, tr.PickupLongitude)

	passCount := randomizePassengerCount(tr.PassengerCount)
	campaignID := computeCampaign(tr.DropoffLatitude, tr.DropoffLongitude)
	for index, point := range points {
		if index > 0 && index < len(points)-1 {
			rideStatus = enroute
		}
		if index == len(points)-1 {
			rideStatus = dropoff
		}

		trp := &taxiRidePoint{
			RideID:              rideID,
			PointIdx:            index,
			Latitude:            point.Lat,
			Longitude:           point.Lng,
			Timestamp:           startTimeStamp.Add(time.Duration(timeStampIncrement * int64(index))).Format(outputDateTimeLayout),
			RideStatus:          rideStatus,
			PassengerCount:      passCount,
			Campaign:            campaignID,
			DestinationLocation: destLocation,
			PickupLocation:      pickLocation,
			TPepPickupDatetime:  tr.TPepPickupDatetime,
		}

		if (index % routeInfoFreq) == 0 {
			trp.UserLikedAdd = userLikedAd
			trp.RouteInfo = tr.Polyline
		}

		taxiRidePoints[index] = trp
	}
	return taxiRidePoints, nil
}
