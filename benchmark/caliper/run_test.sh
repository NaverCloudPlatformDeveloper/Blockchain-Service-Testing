#!/bin/bash

npx caliper launch manager --caliper-networkconfig generated/network-config.yaml \
	--caliper-benchconfig benchmark-config.yaml \
	--caliper-projectconfig default.yaml \
	--caliper-flow-only-test
