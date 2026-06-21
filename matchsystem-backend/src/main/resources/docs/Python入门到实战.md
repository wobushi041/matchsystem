# Python 入门到实战

## 一、Python 基础

### 1.1 语法基础
- 变量与数据类型：int、float、str、list、dict、tuple、set
- 控制流：if-elif-else、for、while、break/continue
- 函数：def、参数类型（位置、关键字、默认、可变）、lambda 表达式
- 推导式：列表推导式、字典推导式、生成器表达式
- 异常处理：try-except-finally、自定义异常

### 1.2 面向对象
- 类与对象：__init__、self、类变量 vs 实例变量
- 继承与多态：单继承、多继承、MRO 方法解析顺序
- 魔术方法：__str__、__repr__、__len__、__getitem__
- 属性管理：@property、__slots__
- 抽象类与接口：abc 模块

### 1.3 高级特性
- 迭代器与生成器：__iter__、__next__、yield
- 装饰器：函数装饰器、类装饰器、装饰器链
- 上下文管理器：with 语句、__enter__、__exit__
- 类型注解：typing 模块、TypeVar、Protocol

## 二、常用标准库

### 2.1 文件与IO
- 文件操作：open、read/write、with 语句
- CSV 读写：csv 模块
- JSON 处理：json 序列化与反序列化
- 路径操作：pathlib vs os.path

### 2.2 并发编程
- 多线程：threading、GIL 的影响
- 多进程：multiprocessing、进程池
- 异步编程：asyncio、async/await、aiohttp
- 协程 vs 线程 vs 进程的选择

### 2.3 正则表达式
- re 模块：match、search、findall、sub
- 常用正则模式：邮箱、手机号、URL、IP 地址
- 分组与贪婪/非贪婪匹配

## 三、Web 开发

### 3.1 Flask
- 路由与视图函数
- 模板渲染：Jinja2
- 表单处理与数据验证
- SQLAlchemy ORM
- Flask-RESTful API 开发

### 3.2 Django
- MTV 架构：Model、Template、View
- ORM 查询：QuerySet API
- Django REST Framework
- 中间件与信号
- Admin 后台管理

### 3.3 FastAPI
- 路径参数与查询参数
- 请求体与 Pydantic 模型验证
- 依赖注入系统
- 异步接口支持
- 自动生成 API 文档（Swagger）

## 四、数据处理与爬虫

### 4.1 数据分析
- NumPy：数组操作、广播机制
- Pandas：DataFrame、数据清洗、分组聚合
- Matplotlib/Seaborn：数据可视化

### 4.2 爬虫技术
- requests 库：HTTP 请求
- BeautifulSoup：HTML 解析
- Scrapy 框架：Spider、Pipeline、中间件
- Selenium：浏览器自动化
- 反爬策略应对：User-Agent、代理IP、验证码
