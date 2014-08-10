zabkv [![Build Status](https://travis-ci.org/ZK-1931/zabkv.svg?branch=master)](https://travis-ci.org/ZK-1931/zabkv)
=====

A reference implementation for using [javazab](https://github.com/ZK-1931/javazab)

Usage
-----
To start a cluster, run:

    ./bin/zabkv 8080 -DserverId=localhost:5000
    ./bin/zabkv 8081 -DserverId=localhost:5001 -Djoin=localhost:5000
    ./bin/zabkv 8082 -DserverId=localhost:5002 -Djoin=localhost:5001

Restore server from log directory :

    ./bin/zabkv 8080 -Dlogdir=localhost:5000

To put a key-value pair, do:

    curl localhost:8080/key -XPUT -d 'value'

To get a value for a given key, do:

    curl localhost:8080/key

