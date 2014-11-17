zabkv [![Build Status](https://travis-ci.org/zk1931/zabkv.svg?branch=master)](https://travis-ci.org/zk1931/zabkv)
=====

A reference implementation of key-value store for using [Jzab](https://github.com/zk1931/jzab)

Usage
-----

See usage:

    ./bin/zabkv --help

To start a 3-server cluster, run:

    ./bin/zabkv -port 8080 -addr localhost:5000
    ./bin/zabkv -port 8081 -addr localhost:5001 -join localhost:5000
    ./bin/zabkv -port 8082 -addr localhost:5002 -join localhost:5001

Restore server from log directory :

    ./bin/zabkv -port 8080 -dir localhost:5000

To put key-value pairs, do:

    curl localhost:8080 -XPUT -d "{'key1':'value1', 'key2':'value2'}"

To get a value for a given key, do:

    curl localhost:8080/key

To get all the key-value pairs, do:

    curl localhost:8080
