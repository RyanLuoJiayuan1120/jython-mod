# -*- coding: utf-8 -*-
# JythonModApi 示例：展示 Python 侧调用其它 Java 模组注册的 API。
#
# 对应的 Java 示例模组 examples/api-demo-mod 注册了：
#   "apidemo" -> "TradeApi"       实例（自动类名 key）
#   "apidemo" -> "TradeApiClass"  Class 对象（可构造实例）
#   "apidemo" -> "Calculator"     Class 对象（静态方法/字段/嵌套 Builder）
#   "apidemo" -> "trade"          实例（自定义 key）
#
# 注：LOGGER / API 等全局变量由加载器注入（zipimporter 的 ModImporter 在
# 调用 main() 前会把它们拷贝到本模块命名空间），main() 内可直接使用。


def main():
    LOGGER.info("=== JythonModApi demo start ===")

    # ---- 方式一：全局 API 直取（dict 风格，只读活引用） ----
    # 实例 API：直接调用实例方法
    api = API["apidemo"]["TradeApi"]
    LOGGER.info("greet: {}", api.greet("Python"))
    LOGGER.info("addTrade: {}", api.addTrade(5))
    LOGGER.info("tradeCount: {}", api.getTradeCount())

    # Class 对象：调用静态方法
    calc = API["apidemo"]["Calculator"]
    LOGGER.info("calc.add: {}", calc.add(1, 2))

    # Class 对象：读取静态字段
    LOGGER.info("calc.PI: {}", calc.PI)

    # Class 对象：构造实例（注册的是 TradeApi.class）
    cls = API["apidemo"]["TradeApiClass"]
    inst = cls("构造的实例")
    LOGGER.info("constructed greet: {}", inst.greet("ctor"))

    # Class 对象：嵌套类 + 链式 Builder
    b = calc.Builder()
    LOGGER.info("builder.set(21).build(): {}", b.set(21).build())

    # 自定义 key
    LOGGER.info("custom key greet: {}", API["apidemo"]["trade"].greet("custom"))

    # ---- 方式二：import 钩子（直取式） ----
    from jython_api.apidemo import TradeApi as DirectApi
    LOGGER.info("import direct greet: {}", DirectApi.greet("import"))

    # ---- 方式二b：import 钩子（模块式） ----
    from jython_api import apidemo
    LOGGER.info("module style calc.add: {}", apidemo.Calculator.add(3, 4))

    # ---- 错误处理演示 ----
    try:
        API["不存在的模组"]
    except KeyError as e:
        LOGGER.info("KeyError caught: {}", e)

    try:
        from jython_api.不存在的模组 import X
    except ImportError as e:
        LOGGER.info("ImportError caught: {}", e)

    try:
        API["apidemo"]["不存在的api"]
    except KeyError as e:
        LOGGER.info("KeyError caught: {}", e)

    LOGGER.info("=== JythonModApi demo end ===")
