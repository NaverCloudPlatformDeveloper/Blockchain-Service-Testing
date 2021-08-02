#!/bin/bash

set -e

if [ ! -f download/user.json ] || [ ! -f download/connection_profile.json ]; then
	echo "Error: file does not exist. necessary files: download/user.json, download/connection_profile.json"
  exit 1
fi

if ! [ -x "$(command -v envsubst)" ]; then
  echo "Error: envsubst is not installed."
  exit 1
fi

if ! [ -x "$(command -v jq)" ]; then
  echo "Error: jq is not installed."
  exit 1
fi

mkdir -p generated
jq -r .key download/user.json | base64 -d > generated/user.key
jq -r .cert download/user.json | base64 -d > generated/user.cert

export USERID=$(jq -r .name download/user.json)
org=$(jq -r .client.organization download/connection_profile.json)
export MSPID=$(jq -r '.organizations."'$org'".mspid' download/connection_profile.json)

envsubst < network-config.yaml > generated/network-config.yaml
echo "Test preparation done. Generated files are in ./generated"
