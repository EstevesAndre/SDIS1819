#! /usr/bin/env bash

echo "Started..."
echo "Hello, $USER!"

v=$1
nPeers=$2

if [ $# -ne 2 ]; then
    echo "Usage: $0 <version> <number of peers>"
    exit 1
fi

for ((i = 1020 ; i <= 1020+$nPeers*100 ; i++)); do
    gnome-terminal -x sh -c "java project/service/Peer $v $i RemoteInterface \"230.0.0.0 9876\" \"230.0.0.1 9877\" \"230.0.0.2 9878\""
done