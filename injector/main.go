package main

import (
	"fmt"
	"io/ioutil"
	"log"
	"math"
	"math/rand"
	"os"
	"sync"
	"time"

	"github.com/namsral/flag"

	"golang.org/x/net/context"
	"golang.org/x/oauth2"
	"golang.org/x/oauth2/google"

	"cloud.google.com/go/pubsub"
	"google.golang.org/api/option"
	storage "google.golang.org/api/storage/v1"
)

const (
	gcsScope                   = storage.DevstorageReadOnlyScope
	inputDatasetDateTimeLayout = "2006-01-02 15:04:05" // format of dataset TPepPickupDatetime 2015-01-04 20:01:44
	outputDateTimeLayout       = "2006-01-02T15:04:05.99999Z07:00"
	ridesTimeLocation          = "America/New_York"
	serviceAccountJSONFile     = "service-account.json"
	publishFlushCycle          = 100 * time.Millisecond
)

var (
	projectID       = flag.String("project", "", "Your cloud project ID.")
	bucketName      = flag.String("bucket", "", "The name of the bucket within your project.")
	filePrefix      = flag.String("filePrefix", "", "FilePrefix path for data to load.")
	topicName       = flag.String("pubsubTopic", "", "Name to PubSub topic to publish records to")
	pubsubBatchSize = flag.Int("pubsubBatchSize", 500, "Set batch size to send to pubsub")
	fileType        = flag.String("t", "json", "FileType 'csv' or 'json'. Default 'json'.")
	datasetName     = flag.String("dataset", "advertising", "Dataset with campaign mapping table")
	tableName       = flag.String("table", "campaign", "Table with campaign mapping data")
	flLoop          = flag.Bool("loop", false, "Loop through input files forever")
	flDebug         = flag.Bool("debug", false, "Enable debug output to stdout")
	speedupFactor   = flag.Int("speedup", 1, "Factor to speedup push to pubsub. 60 pushes 1h of data in 1 minute.")
	skipRidesMod    = flag.Int("skipRides", 1, "Only send every mod n'th ride to pubsub to lower qps")
	skipFileMod     = flag.Int("skipFiles", 1, "Skip every mod n'th file from input file list")
	routeInfoFreq   = flag.Int("routeInfoFreq", 500, "Send route_info in every n'th ride point")

	timeLocation *time.Location

	version string // set by linker -X
)

func fatalf(service *storage.Service, errorMessage string, args ...interface{}) {
	log.Fatalf("Dying with error:\n"+errorMessage, args...)
}

func main() {
	if len(os.Args) > 1 && os.Args[1] == "version" {
		fmt.Println(version)
		return
	}
	flag.String(flag.DefaultConfigFlagname, "", "path to config file")
	flag.Parse()

	debugLog := debugging(*flDebug)
	if debugLog {
		fmt.Println("Debugging enabled - ")
		fmt.Printf("Running feeder version %v\n", version)
	}

	if *bucketName == "" {
		log.Fatalf("Bucket argument is required. See --help.")
	}

	if *projectID == "" {
		log.Fatalf("Project argument is required. See --help.")
	}

	if *filePrefix == "" {
		log.Fatalf("FilePrefix argument is required. See --help.")
	}

	if *fileType != "csv" && *fileType != "json" {
		log.Fatalf("Only 'json' or 'csv' supported for FileType")
	}

	if *speedupFactor <= 0 {
		log.Fatalf("Invalid speedup factor of value %v", *speedupFactor)
	}

	var err error
	timeLocation, err = time.LoadLocation(ridesTimeLocation)
	if err != nil {
		log.Fatalf("Can't lookup time location: %v", err)
	}

	// set dataset start time to time.Now().In(timeLocation) and calculate offset to original
	// dataset start time "2015-01-04 20:00:00"
	datasetStartDateTime, err := time.ParseInLocation(inputDatasetDateTimeLayout, "2015-01-04 20:00:00", timeLocation)
	if err != nil {
		log.Fatalf("Unable to parse time in location %v: %v", timeLocation, err)
	}

	// *** BEGIN - Initialize Cloud Storage Client ***
	// Authentication is provided by the gcloud tool when running locally, and
	// by the associated service account when running on Compute Engine.
	storageService, err := storageClient(context.Background(), gcsScope)
	if err != nil {
		log.Fatalf("Unable to create storage service: %v", err)
	}
	// *** END - Initialize Cloud Storage Client ***

	// *** BEGIN - Initialize PubSub Client ***
	pubsubService, err := pubsubClient(context.Background())
	if err != nil {
		log.Fatalf("Unable to create pubsub service: %v", err)
	}

	topicExists, err := pubsubService.Topic(*topicName).Exists(context.Background())
	if err != nil {
		log.Fatalf("Error checking for topic %v to exist: %v", *topicName, err)
	}
	if !topicExists {
		log.Fatalf("Topic %v doesn't exist", *topicName)
	}

	// setup buffered publishing to pubsub
	pubsubMessagesCh := make(chan *pubsub.Message, *pubsubBatchSize)
	bpsp := &bufferedPubsubPublisher{
		client:       pubsubService,
		topic:        *topicName,
		maxBatchSize: *pubsubBatchSize,
		flushCycle:   publishFlushCycle,
		debugLog:     debugLog,
	}
	bpsp.run(pubsubMessagesCh)
	// *** END - Initialize PubSub Client ***

	debugLog.Printf("Reading files from bucket %v:\n", *bucketName+"/"+*filePrefix)
	inputFiles, err := listFiles(storageService, *bucketName, *filePrefix)
	if err != nil {
		log.Fatalf("Unable to get list of files for bucket: %v/%v: %v", *bucketName, *filePrefix, err)
	}

	// start periodic debug statistic printout
	debugLog.printDebugStats()

	// WaitGroup to push ride points to pubsub
	var wg sync.WaitGroup

	// defered load of inputFiles
	for {
		newRefDSStartTime := time.Now()
		timeOffset := newRefDSStartTime.Sub(datasetStartDateTime)

		for i, inputFile := range inputFiles {
			if (i+1)%*skipFileMod != 0 {
				continue
			}

			file, err := storageService.Objects.Get(*bucketName, inputFile).Download()
			if err != nil {
				log.Fatalf("Unable to get file from bucket: "+*bucketName+"/"+inputFile, err)
			}
			switch *fileType {
			case "json":
				taxiRides, err := ridesFromJSONRawIOReader(file.Body)
				if err != nil {
					log.Fatalf("Error in reading %v from GCS: %v", inputFile, err)
				}

				debugLog.Printf("Reading %v rides from %v", len(taxiRides.Rides), inputFile)
				for j, taxiRide := range taxiRides.Rides {
					if (j+1)%*skipRidesMod != 0 {
						continue
					}
					randInt := rand.Intn(10)
					if validRide(&taxiRide) {
						// schedule the taxi ride points to be published to pubsub
						rps := &ridePointScheduler{
							taxiRide:      taxiRide,
							refTime:       newRefDSStartTime,
							timeOffset:    timeOffset,
							speedupFactor: *speedupFactor,
							debugLog:      debugLog,
							likedAd:       randInt%2 == 0,
							routeInfoFreq: *routeInfoFreq,
						}

						// start pushing ride points
						wg.Add(1)
						go func(rps *ridePointScheduler) {
							rps.run(pubsubMessagesCh)
							wg.Done()
						}(rps)
					}
				}

				// Rides in input files are in chronological order. To save memory usage we load them deferred
				// take the last ride in input file and use pickupTime to calculate sleep time till load of next
				// input file from list
				pt, _ := taxiRides.Rides[len(taxiRides.Rides)-1].pickupTime()
				// calculate time till next file to load with speedup considered
				rtd := time.Now().Sub(newRefDSStartTime)
				d := int64(pt.Add(timeOffset).Sub(newRefDSStartTime).Nanoseconds()/int64(*speedupFactor)) - rtd.Nanoseconds()
				time.Sleep(time.Duration(d) - (10 * time.Second))
			default:
				log.Fatalf("Unsupported file input format")
			}
		}
		if !*flLoop {
			break
		}
	}
	debugLog.Printf("Waiting for rides queue to drain... ")
	// wait till all points are scheduled for publishing
	wg.Wait()
	close(pubsubMessagesCh)
	// wait for the publisher buffer to be drained?
	<-bpsp.done()
	log.Printf("Good bye! - Done pushing ride points to pubsub")
}

func validRide(r *taxiRide) bool {
	schedDebugCounter.Add(ridesInvalidKey, 1)
	pt, err := r.pickupTime()
	if err != nil {
		return false
	}

	dt, err := r.dropoffTime()
	if err != nil {
		return false
	}

	if dt.Sub(pt).Hours() > 6 {
		return false
	}
	schedDebugCounter.Add(ridesInvalidKey, -1)
	return true
}

func storageClient(ctx context.Context, scope ...string) (*storage.Service, error) {
	// if no service-account.json use Default Client
	if _, err := os.Stat(serviceAccountJSONFile); err != nil {
		client, err := google.DefaultClient(context.Background(), scope...)
		if err != nil {
			return nil, err
		}
		return storage.New(client)
	}

	serviceAccountJSON, err := ioutil.ReadFile(serviceAccountJSONFile)
	if err != nil {
		return nil, err
	}

	conf, err := google.JWTConfigFromJSON(serviceAccountJSON, scope...)
	if err != nil {
		return nil, err
	}

	client := oauth2.NewClient(ctx, conf.TokenSource(ctx))

	return storage.New(client)
}

func pubsubClient(ctx context.Context) (*pubsub.Client, error) {
	if _, err := os.Stat(serviceAccountJSONFile); err != nil {
		return pubsub.NewClient(ctx, *projectID)
	}
	serviceAccountJSON, err := ioutil.ReadFile(serviceAccountJSONFile)
	if err != nil {
		return nil, err
	}

	conf, err := google.JWTConfigFromJSON(serviceAccountJSON, pubsub.ScopePubSub, pubsub.ScopeCloudPlatform)
	if err != nil {
		return nil, err
	}
	return pubsub.NewClient(ctx, *projectID, option.WithTokenSource(conf.TokenSource(ctx)))
}

// listFiles for passed bucketName filtered by passed FilePrefix
func listFiles(svc *storage.Service, bucketName string, filePrefix string) ([]string, error) {
	// List all objects in a bucket using pagination
	var files []string
	token := ""
	for {
		call := svc.Objects.List(bucketName)
		call.Prefix(filePrefix)
		if token != "" {
			call = call.PageToken(token)
		}
		res, err := call.Do()
		if err != nil {
			return nil, err
		}
		for _, object := range res.Items {
			files = append(files, object.Name)
		}
		if token = res.NextPageToken; token == "" {
			break
		}
	}
	return files, nil
}

var debugStatsFormat = "%15v %12v %9v %4v %16v %8v %11v %8v %8v %10v %10v"

func (d *debugging) startDebugHeaderPrinter() {
	d.Printf(debugStatsFormat, "Rides# queued", "processed", "invalid", "qps", "Points# total", "queued", "scheduled", "failed", "backlog", "Sched QPS", "Pub QPS")
	time.AfterFunc(time.Second*10, d.startDebugHeaderPrinter)
}

func (d *debugging) printDebugStats() {
	if !(*d) {
		return
	}

	// print debug stats table header
	d.startDebugHeaderPrinter()

	lastRidesProcessed := 0
	lastPointsScheduled := 0
	lastMessagesSent := 0
	lastStatisticsUpdateTimestamp := time.Now()
	go func() {
		for {
			time.Sleep(time.Second)
			rl := ridesLoaded()
			rp := ridesProcessed()
			rQueue := rl - rp
			ri := ridesInvalid()
			pl := pointsLoaded()
			pf := pointsFailed() + messagesFailed()
			ps := pointsScheduled()
			bl := publishBacklog()
			pQueue := pl - ps - pf
			ms := messagesSent()

			// d.Printf("Rides# queue: %v processed: %v invalid: %v qps: %v; Points# total: %v, queue: %v, scheduled: %v, failed: %v backlog: %v; Sched QPS: %v, Pub QPS: %v",
			d.Printf(debugStatsFormat, rQueue, rp, ri,
				math.Ceil(float64(rp-lastRidesProcessed)/time.Now().Sub(lastStatisticsUpdateTimestamp).Seconds()),
				pl, pQueue, ps, pf, bl,
				math.Ceil(float64(ps-lastPointsScheduled)/time.Now().Sub(lastStatisticsUpdateTimestamp).Seconds()),
				math.Ceil(float64(ms-lastMessagesSent)/time.Now().Sub(lastStatisticsUpdateTimestamp).Seconds()))
			lastStatisticsUpdateTimestamp = time.Now()
			lastRidesProcessed = rp
			lastPointsScheduled = ps
			lastMessagesSent = ms
		}
	}()
}
