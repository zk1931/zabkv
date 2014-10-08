import httplib
import time
import json

class KVClient(object):
  '''
  The client interface for Zabkv. For testing purpose.
  '''
  def __init__(self, addr):
    ip, port = addr.split(":")
    self.conn = httplib.HTTPConnection(ip, port)
    self.addr = addr

  def put(self, key, value):
    '''
    Store the given key-value pairs.
    '''
    self.conn.request("PUT", "",  json.dumps({key : value}))
    response = self.conn.getresponse()
    response.read()
    assert response.status == 200

  def get(self, key):
    '''
    Return the valud of the key.
    '''
    self.conn.request("GET", "/%s" % (key,))
    response = self.conn.getresponse()
    response.getheaders()
    assert response.status == 200
    return response.read()

  def getAll(self):
    '''
    Return the state of the KV server as a single String.
    '''
    self.conn.request("GET", "")
    response = self.conn.getresponse()
    response.getheaders()
    assert response.status == 200
    content = response.read()
    return content

  def getAddr(self):
    return self.addr

