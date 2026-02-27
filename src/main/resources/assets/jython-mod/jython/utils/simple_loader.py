# -*- coding: utf-8 -*-
"""
简化版模块加载器示例
"""
from module_loader import ModuleLoader


# ==================== 使用方法 ====================

# 1. 创建加载器，指定要扫描的目录
loader = ModuleLoader("./path/to/your/modules")  # 修改为你的目录路径

# 2. 加载所有模块（.py 和 .zip）
all_modules = loader.load_all()

# 3. 获取所有模块
modules_list = loader.get_modules()  # 模块对象列表
module_names = loader.get_module_names()  # 模块名称列表

# 4. 使用导入的模块
for module in modules_list:
    # 调用模块中的函数
    if hasattr(module, 'some_function'):
        module.some_function()

    # 访问模块中的类
    if hasattr(module, 'SomeClass'):
        obj = module.SomeClass()

# 5. 重新加载所有模块（热重载）
all_modules = loader.reload_all()

# 6. 查看加载结果
print(f"成功加载 {len(all_modules)} 个模块")
print("模块列表:", module_names)
