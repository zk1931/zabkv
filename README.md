zabkv [![Build Status](https://travis-ci.org/ZK-1931/zabkv.svg?branch=master)](https://travis-ci.org/ZK-1931/zabkv)
=====

A reference implementation for using javazab

Usage
-----
To start the server, run:

    ./bin/zabkv 8080

To put a key-value pair, do:

    curl localhost:8080/key -XPUT -d 'value'

To get a value for a given key, do:

    curl localhost:8080/key

