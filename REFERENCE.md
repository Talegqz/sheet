# The Sheet Reference

Sheet 是一门编程语言。

## 词法结构

### 标识符

标识符是满足如下条件的非空字符串：

* 第一个字符是下划线、`$`符号、`@`符号或英文字母
* 剩余字符是下划线、`$`符号、`@`符号、英文字母数字或字符串
* 不在关键字列表里

### 注释

沿袭了 C 风格语言的注释。单行注释以`//`开始；多行注释以`/*`开始并且以`*/`结束，并且支持嵌套。

### 空白字符

以下字符被定义为空白字符：

* `U+0020` (space, `' '`)
* `U+0009` (tab, `'\t'`)
* `U+000A` (LF, `'\n'`)
* `U+000D` (CR, `'\r'`)

空白字符串为仅有空白字符的字符串，空白字符（串）只用于分割词法单元，没有任何语义。

### 词法单元

词法单元是语法分析中最小的不可分割的基本元素。

#### 直接值（literals, immediate value）

##### 字符和字符串

###### 字符

字符是使用英文单引号包围的单个字符。

###### 字符串

字符串是使用英文双引号包围的零个、一个或多个字符构成的序列。

##### 转义字符

###### 字节转义

* 8 位的字符编码（`\x7F`）
* 换行符（`\n`）
* 回车（`\r`）
* 制表符（`\t`）
* 反斜杠（`\\`）

###### Unicode 转义

* 24 位 Unicode 字符编码（`\u{7FFF}`）

##### 数字

|数字格式*|示例|是否带有指数|后缀|
|:-:|:-:|:-:|:-:|:-:|
|十进制整数|`10086`|无|整数后缀|
|十六进制整数|`0xfff`|无|整数后缀|
|八进制整数|`0o777`|无|整数后缀|
|二进制整数|`0b1111_0000`|无|整数后缀|
|浮点数|3.14159E+5|可选|浮点数后缀|

\* 数字中可用下划线（`_`）作分隔符。

有以下几种数字后缀：

* 整数后缀：`u8`、`i8`、`u16`、`i16`、`u32`、`i32`、`u64`和`i64`
* 浮点数后缀：`f32`和`f64`



## 类型

类型分为两类：基础类型（primitive type）和复合类型（compound type）。

基础类型有以下几种：

1. 整数（8 位`byte`、16 位`short`、32 位`int`和 64 位`long`以及`unsigned`修饰符）
2. 浮点数（单精度`float`和双精度`double`）
3. UCS16 字符（`char`）
4. 用于指定函数无返回值的类型（`void`）
5. 布尔类型（`bool`）

复合类型有两种：内置复合类型（built-in compound type）和用户定义复合类型（user-defined compound type）。内置复合类型为用户无法定义、但是具有复合属性的基础类型，有以下几种：

1. 数组（`array`）
2. 多元组（`tuple`）
3. UCS16 字符串（`string`）

用户定义复合类型为用户使用`class`关键字定义出来的类型。

## 语句和表达式

### 语句

#### 声明语句

声明语句包含类型声明语句和变量声明语句。

#### 表达式语句

表达式语句是指只由表达式构成的语句。

### 表达式

#### 字面值
#### 多元组
#### 结构体
#### 函数调用
#### 成员访问
#### 数组
#### 索引访问
#### 一元运算
#### 二元运算
#### Lambda 表达式
#### 

## 运算符

运算符（operator）的分类，按照优先级从低到高排序：

1. 赋值运算符（assignment operator）
2. 条件选择运算符（conditionally selective operator）
3. 逻辑运算符（logical operator）
4. 比较运算符（comparative operator）
4. 位运算符（bitwise operator）
5. 数值运算符（arithmetic operator）
6. 一元运算符（unary operator）

### 赋值运算符

复制运算符包括`=`和所有二元运算符与赋值运算符复合出的运算符。

### 条件选择运算符

类似于 C 风格语言的`? :`，我们使用`if cond then turly else falsy`。

### 逻辑运算符

* 一元：`!`代表逻辑非。
* 二元：`&&`、`||`和`^^`分别代表逻辑与、逻辑或和逻辑异或。

### 比较运算符

* 二元：`==`、`!=`、`<`、`<=`、`>`和`>=`分别代表相等、不等、小于、小于等于、大于和大于等于。

### 位运算符

* 一元：`~`代表逐位取反操作。
* 二元：`<<`、`>>`、`&`、`|`和`^`分别代表左移、右移、位与、位或和按位异或。

### 数值运算符

* 一元：`+`和`-`分别代表取正和取负。
* 低优先级二元：`+`和`-`。
* 高优先级二元：`*`、`/`和`%`。


