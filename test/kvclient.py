import httplib
import time

class KVClient(object):
  '''
  The client interface for ZabKV. For testing purpose.
  '''
  def __init__(self, host, port):
    self.conn = httplib.HTTPConnection(host, port)

  def put(self, key, value):
    '''
    Store the given key-value pairs.
    '''
    self.conn.request("PUT", "/%s" % (key,), value)
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
