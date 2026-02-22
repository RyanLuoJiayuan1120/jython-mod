# -*- coding: utf-8 -*-
import sys
from net.luojiayuan.jython.mod.utils import path
def set_path(dev=True):
    path_ = path().get()
    path_ += "/assets/jython-mod/jython"
    sys.path.append(path_)
set_path()
print(sys.path)
