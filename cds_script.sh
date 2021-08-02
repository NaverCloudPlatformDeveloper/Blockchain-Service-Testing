#!/bin/bash

set -e

rm -rf cds_files
mkdir -p cds_files

# prepare chaincode build

pushd chaincode/fabcar/go
GO111MODULE=on go mod vendor
popd

pushd chaincode/fabcar/java
./gradlew assemble
popd

mkdir -p /opt/gopath/src/fabcar
cp -r chaincode/fabcar/go/* /opt/gopath/src/fabcar

# package cds

peer chaincode package cds_files/fabcar_go.v1.cds -n fabcar_go -v 1.0 -p fabcar
peer chaincode package cds_files/fabcar_go.v2.cds -n fabcar_go -v 2.0 -p fabcar

peer chaincode package cds_files/fabcar_java.v1.cds -n fabcar_java -v 1.0 -p chaincode/fabcar/java -l java
peer chaincode package cds_files/fabcar_java.v2.cds -n fabcar_java -v 2.0 -p chaincode/fabcar/java -l java

peer chaincode package cds_files/fabcar_javascript.v1.cds -n fabcar_javascript -v 1.0 -p chaincode/fabcar/javascript -l node
peer chaincode package cds_files/fabcar_javascript.v2.cds -n fabcar_javascript -v 2.0 -p chaincode/fabcar/javascript -l node

ls -al cds_files
