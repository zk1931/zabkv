zabkv [![Build Status](https://travis-ci.org/ZK-1931/zabkv.svg?branch=master)](https://travis-ci.org/ZK-1931/zabkv)
=====

A reference implementation for using [javazab](https://github.com/ZK-1931/javazab)

Usage
-----
To start the servers, run:

    ./bin/zabkv 8080 -DserverId=localhost:5000 -Dservers="localhost:5000;localhost:5001;localhost:5002"
    ./bin/zabkv 8081 -DserverId=localhost:5001 -Dservers="localhost:5000;localhost:5001;localhost:5002"
    ./bin/zabkv 8082 -DserverId=localhost:5002 -Dservers="localhost:5000;localhost:5001;localhost:5002"

To put a key-value pair, do:

    curl localhost:8080/key -XPUT -d 'value'

To get a value for a given key, do:

    curl localhost:8080/key

