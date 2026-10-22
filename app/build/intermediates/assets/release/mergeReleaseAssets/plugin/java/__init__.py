# -*- coding: utf-8 -*-
"""Минимальная совместимость с Chaquopy/Java-мостом Margelet.

Реального Android-моста в Qwen Mobile нет: приложение запускает Python как
обычный процесс. Импорт ``java`` не падает, чтобы плагин успел показать
понятную ошибку, но любое обращение к Java-классу заканчивается
RuntimeError с объяснением.
"""

import types


class JavaBridgeUnavailable(RuntimeError):
    pass


class _JavaCallable:
    def __init__(self, name):
        self.name = name

    def __call__(self, *args, **kwargs):
        raise JavaBridgeUnavailable(
            "Java/Android-мост недоступен: %s. "
            "Плагин использует Telegram API, которого нет в Qwen Mobile."
            % self.name
        )

    def __getattr__(self, attr):
        if attr.startswith("__"):
            raise AttributeError(attr)
        return _JavaCallable(self.name + "." + attr)


class JavaClass:
    def __init__(self, name):
        self.name = name

    def __getattr__(self, attr):
        if attr.startswith("__"):
            raise AttributeError(attr)
        return _JavaCallable(self.name + "." + attr)

    def __call__(self, *args, **kwargs):
        raise JavaBridgeUnavailable(
            "Нельзя создать Java-объект %s: моста нет." % self.name
        )

    def __repr__(self):
        return "<JavaClass %s (недоступен)>" % self.name


def jclass(name):
    name = str(name)
    basics = {
        "java.lang.String": str,
        "java.lang.Integer": int,
        "java.lang.Long": int,
        "java.lang.Boolean": bool,
        "java.lang.Object": object,
        "java.util.ArrayList": list,
        "java.util.List": list,
    }
    return basics.get(name, JavaClass(name))


def jarray(element_type):
    return list


def dynamic_proxy(interface):
    """Плагины используют это для Runnable/FetchCallback; создаём пустой класс."""
    return type("JavaDynamicProxy", (object,), {"__init__": lambda self, *a, **k: None})
