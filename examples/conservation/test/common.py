import os.path
import gzip

def opengz_maybe(path):
        if os.path.isfile(path + '.gz'):
                return gzip.open(path + '.gz')
        else:
                return open(path)

def gz_maybe(path):
        if os.path.isfile(path + '.gz'):
                return path + '.gz'
        else:
                return path
