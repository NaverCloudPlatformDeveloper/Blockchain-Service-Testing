#!/bin/bash

set -e

docker run -v `pwd`:/testing -w /testing -it hyperledger/fabric-tools:1.4.9 /testing/cds_script.sh
