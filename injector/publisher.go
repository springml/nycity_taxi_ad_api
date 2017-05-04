package main

import (
	"context"
	"expvar"
	"log"
	"strconv"
	"sync"
	"time"

	"cloud.google.com/go/pubsub"
)

// bufferedPubsubPublisher enables message publish pooling
type bufferedPubsubPublisher struct {
	client       *pubsub.Client
	topic        string
	maxBatchSize int
	flushCycle   time.Duration

	debugLog debugging
	donec    chan struct{}

	bufMu sync.Mutex
	buf   []*pubsub.Message
}

var (
	pubDebugCounter = expvar.NewMap("publisherCounters")
)

const (
	sentMsgsKey       = "sentmessages"
	failedMsgsKey     = "failedmessages"
	publishBacklogKey = "publishbacklog"
)

func (p *bufferedPubsubPublisher) run(ch <-chan *pubsub.Message) {
	p.debugLog.Printf("bufferedPubsubPublisher started...")
	p.donec = make(chan struct{})
	p.buf = make([]*pubsub.Message, 0, p.maxBatchSize+100)
	time.AfterFunc(p.flushCycle, p.flush) // kick off flush
	go func() {
		for msg := range ch {
			if n := p.appendBuf(msg); n > p.maxBatchSize {
				p.flush()
			}
		}
		p.publish(p.buf)
		close(p.donec)
	}()
}

func (p *bufferedPubsubPublisher) done() <-chan struct{} {
	return p.donec
}

func (p *bufferedPubsubPublisher) appendBuf(msg *pubsub.Message) int {
	p.bufMu.Lock()
	defer p.bufMu.Unlock()
	p.buf = append(p.buf, msg)
	return len(p.buf)
}

func (p *bufferedPubsubPublisher) flush() {
	p.bufMu.Lock()
	defer p.bufMu.Unlock()
	if n := len(p.buf); n > 0 {
		if n > p.maxBatchSize {
			n = p.maxBatchSize
		}
		buf := make([]*pubsub.Message, n)
		copy(buf, p.buf[:n])
		go p.publish(buf)
		p.buf = p.buf[n:]
	}
	time.AfterFunc(p.flushCycle, p.flush)
}

func (p *bufferedPubsubPublisher) publish(buf []*pubsub.Message) {
	if len(buf) == 0 {
		return
	}

	ctx := context.Background()

	var results []*pubsub.PublishResult
	for _, m := range buf {
		r := p.client.Topic(p.topic).Publish(ctx, m)
		results = append(results, r)
		pubDebugCounter.Add(publishBacklogKey, 1)
	}

	for _, r := range results {
		if id, err := r.Get(ctx); err != nil {
			log.Printf("Error publishing message %s to pubsub: %v", id, err)
			pubDebugCounter.Add(failedMsgsKey, 1)
		}
		pubDebugCounter.Add(publishBacklogKey, -1)
		pubDebugCounter.Add(sentMsgsKey, 1)
	}
}

func messagesSent() int {
	ms := pubDebugCounter.Get(sentMsgsKey)
	if ms == nil {
		return 0
	}
	msi, err := strconv.Atoi(ms.String())
	if err != nil {
		log.Printf("Error getting messages sent count. %v", err)
		return 0
	}
	return msi
}

func messagesFailed() int {
	mf := pubDebugCounter.Get(failedMsgsKey)
	if mf == nil {
		return 0
	}
	mfi, err := strconv.Atoi(mf.String())
	if err != nil {
		log.Printf("Error getting messages failed count. %v", err)
		return 0
	}
	return mfi
}

func publishBacklog() int {
	bl := pubDebugCounter.Get(publishBacklogKey)
	if bl == nil {
		return 0
	}
	bli, err := strconv.Atoi(bl.String())
	if err != nil {
		log.Printf("Error getting messages failed count. %v", err)
		return 0
	}
	return bli
}
