from kvclient import KVClient
import time
import random
import socket
from config import servers

# Number of writes for the test.
n_writes = 1000
clients = [KVClient(addr) for addr in servers]

for i in range(n_writes):
  for clt in clients:
    try:
      clt.put(clt.getAddr() + "_" + str(i), i)
    except Exception as ex:
      print "Caught exception when send to %s" % (clt.getAddr(),)
