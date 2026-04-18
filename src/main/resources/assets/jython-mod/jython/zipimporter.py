# -*- coding: utf-8 -*-
import zipimport
from net.luojiayuan.jython.mod import Jythonmod
from org.python.core import codecs
codecs.setDefaultEncoding('utf-8')
LOGGER = Jythonmod.LOGGER

class ModImporter:
    def __init__(self, env, path):
        self.env = env
        self.path=path

    def Load(self):
        
        try:
            importer = zipimport.zipimporter(self.path)
            Mod = importer.load_module(self.env)
            Mod.main()
        except zipimport.ZipImportError, e:
            LOGGER.warn("Skip...Is \""+self.path+"\" a folder?"+str(e))
        except AttributeError, e:
            LOGGER.warn("Skip...The Mod \""+self.path+"\" don't have method "+self.env)

