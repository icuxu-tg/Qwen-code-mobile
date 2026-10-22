# -*- coding: utf-8 -*-
"""XposedHelpers-совместимая заглушка.

Плагин может импортировать класс и даже объявить функцию; ошибка появится
только при реальном вызове, с понятным текстом.
"""


class XposedBridgeUnavailable(RuntimeError):
    pass


def _no(*args, **kwargs):
    raise XposedBridgeUnavailable(
        "XposedHelpers недоступен: Qwen Mobile не использует Xposed и "
        "не содержит классов Telegram."
    )


class XposedHelpers(object):
    getIntField = staticmethod(_no)
    getLongField = staticmethod(_no)
    getBooleanField = staticmethod(_no)
    getObjectField = staticmethod(_no)
    setIntField = staticmethod(_no)
    setLongField = staticmethod(_no)
    setBooleanField = staticmethod(_no)
    setObjectField = staticmethod(_no)
    callMethod = staticmethod(_no)
    callStaticMethod = staticmethod(_no)
    findClass = staticmethod(_no)
    findMethod = staticmethod(_no)
    findField = staticmethod(_no)
    newInstance = staticmethod(_no)
