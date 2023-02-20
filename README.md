# arknights-bot
A qq bot about arknights..

**TIPS：**

一个并没有什么实际用途的明日方舟qq群聊bot

PS:  （代码质量低下），本项目当前版本即时部署效率不高（使用springboot+mybatis+mysql8+OPQ，部署服务器为centos7），主要是给沙雕群友整点乐子，后续可能会搞一些比较实用的功能。
 配置见上，OPQ是一款机器人框架，可以将程序挂载在一个qq（建议用小号，防止风控误封）上来实现群聊消息交互（OPQ的交互指令通过lua文件实现，更多指令见[OPQ WIKI](https://github.com/opq-osc/OPQ/wiki)）。

PPS：部分工具类引用自[02大佬](https://github.com/Strelizia02/ArknightsAPI)的工具类封装，特此感谢，游戏数据来源于[Kengxxiao](https://github.com/Kengxxiao/ArknightsGameData)提供的库

PPPS：**特色功能**(然而并不是很好用)：

- ​    寻访记录查询（需要绑token从官网获取）
- ​    干员技能查询（需要调度解析数据文件）

其他的基本没什么用，包括at操作自动回复等都比较简单
这里提一下寻访查询功能，目前可以通过token获取官网的前100条寻访记录，目前token有效时长大概是7天？反正我自己用起来也不是很顺手

另外，原本是想用httpClient去抓取wiki上的数据解析，但发现处理html格式太麻烦了，而且如果网页更新我这功能就没用了（旧代码在测试类里）...于是改为取别人游戏解包后的jsnon文件.

PPPPS：注意，本项目仅供参考，任何人使用本项目相关代码造成任何损失雨我无瓜）

**测试截图等其他详见WIKI**




