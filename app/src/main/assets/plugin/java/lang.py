# -*- coding: utf-8 -*-
"""Заглушки java.lang для плагинов Margelet."""


class Object(object):
    pass


class String(str):
    pass


class Runnable(object):
    def run(self):
        raise NotImplementedError


class CharSequence(str):
    pass


class Integer(int):
    pass


class Boolean(object):
    pass


class System(object):
    @staticmethod
    def currentTimeMillis():
        import time
        return int(time.time() * 1000)

    @staticmethod
    def getProperty(name, fallback=None):
        import os
        return os.environ.get(str(name), fallback)
