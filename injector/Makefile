# Copyright 2016 Google Inc. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


BINARY_NAME       := feeder
GOOS              := linux

SHA               := $(shell git rev-parse --short HEAD)
DATE              := $(shell TZ=UTC date +%FT%T)Z
VERSION           := $(shell cat VERSION)-$(DATE)-$(SHA)
VERSIONSHORT      := $(shell cat VERSION)-$(SHA)
OUTDIR            := bin

PROJECT           := $(shell gcloud config list --format 'value(core.project)')
GCR               ?= gcr.io
DOCKER_BUILD_FILE := Dockerfile.build
DOCKER_FILE       := Dockerfile.$(GOOS)

SRCS              := $(shell find . -name '*.go')

all: $(OUTDIR)/$(BINARY_NAME) package

$(OUTDIR)/$(BINARY_NAME): $(SRCS) $(DOCKER_BUILD_FILE) VERSION
	# docker behavior workaround - otherwise created under root user
	mkdir -p $(OUTDIR)
	# builds the binary
	docker build --build-arg version=$(VERSION) --build-arg binary_name=$(BINARY_NAME) \
	--build-arg goos=$(GOOS) -t $(BINARY_NAME)-build:$(VERSIONSHORT) -f $(DOCKER_BUILD_FILE) .
	# copies the binary to $(OUTDIR)
	docker run --rm -v $(PWD)/$(OUTDIR):/dest/ $(BINARY_NAME)-build:$(VERSIONSHORT)
	# cleanup image - not needed anymore
	docker rmi $(BINARY_NAME)-build:$(VERSIONSHORT)

package: $(OUTDIR)/$(BINARY_NAME) $(DOCKER_FILE) VERSION
	echo $(VERSION) > $(OUTDIR)/VERSION
	# builds the container encapsulating the binary based on alpine
	docker build --build-arg binary_name=$(BINARY_NAME) -t $(BINARY_NAME):$(VERSIONSHORT) -t $(BINARY_NAME):latest -f $(DOCKER_FILE) .

push-gcr: package
	# push to google cloud container registry
	docker tag $(BINARY_NAME):$(VERSIONSHORT) $(GCR)/$(PROJECT)/$(BINARY_NAME):$(VERSIONSHORT)
	docker tag $(BINARY_NAME):$(VERSIONSHORT) $(GCR)/$(PROJECT)/$(BINARY_NAME):latest
	gcloud docker -- push $(GCR)/$(PROJECT)/$(BINARY_NAME):$(VERSIONSHORT)
	gcloud docker -- push $(GCR)/$(PROJECT)/$(BINARY_NAME):latest

clean:
	rm -rf $(OUTDIR)