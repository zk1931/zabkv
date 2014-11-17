zabkv [![Build Status](https://travis-ci.org/zk1931/zabkv.svg?branch=master)](https://travis-ci.org/zk1931/zabkv)
=====

A reference implementation of key-value store for using [Jzab](https://github.com/zk1931/jzab)

Usage
-----
To start a cluster, run:

    ./bin/zabkv 8080 -DserverId=localhost:5000
    ./bin/zabkv 8081 -DserverId=localhost:5001 -Djoin=localhost:5000
    ./bin/zabkv 8082 -DserverId=localhost:5002 -Djoin=localhost:5001

Restore server from log directory :

    ./bin/zabkv 8080 -Dlogdir=localhost:5000

To put key-value pairs, do:

    curl localhost:8080 -XPUT -d "{'key1':'value1', 'key2':'value2'}"

To get a value for a given key, do:

    curl localhost:8080/key

To get all the key-value pairs, do:

    curl localhost:8080
