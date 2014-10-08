from kvclient import KVClient
from config import servers

clients = [KVClient(addr) for addr in servers]
states = {}

for clt in clients:
  try :
    states[clt.getAddr()] = clt.getAll()
  except Exception as ex:
    print "Caught exception for %s" % (clt.getAddr(),)

prevAddr = None
for addr in states:
  if prevAddr is not None:
    if states[prevAddr] != states[addr]:
      print "ERROR : %s and %s have different state" % (prevAddr, addr)
    else:
      print "%s and %s have the same state" % (prevAddr, addr)
  prevAddr = addr

print "Verify end."
