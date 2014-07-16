'''
Put a bunch of key-value pairs to cluster and make sure all the servers in
cluster have the same state.

First you need to start 3 zabkv servers in given addresses and ports:

./bin/zabkv 8080 -DserverId=localhost:5000 -Dservers="localhost:5000;localhost:5001;localhost:5002"
./bin/zabkv 8081 -DserverId=localhost:5001 -Dservers="localhost:5000;localhost:5001;localhost:5002"
./bin/zabkv 8082 -DserverId=localhost:5002 -Dservers="localhost:5000;localhost:5001;localhost:5002"

Then :
python test/simple_test.py
'''
from kvclient import KVClient
import time
import random

clt1 = KVClient("localhost", 8080)
clt2 = KVClient("localhost", 8081)
clt3 = KVClient("localhost", 8082)

PUT_COUNT = 100

for i in range(PUT_COUNT):
  print "Putting keys to cluster..."
  clt1.put("clt1_%s" % (i,), random.choice("abcde"))
  print "Putting keys to cluster..."
  clt2.put("clt2_%s" % (i,), random.choice("abcde"))
  print "Putting keys to cluster..."
  clt3.put("clt3_%s" % (i,), random.choice("abcde"))

time.sleep(1)

content1 = clt1.getAll()
content2 = clt2.getAll()
content3 = clt3.getAll()
print "Verifying the state of servers..."
assert content1 == content2
assert content1 == content3
print "Test Passed."
