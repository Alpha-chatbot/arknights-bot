# arknights-bot
A qq bot about arknights..

**TIPS：**

一个并没有什么实际用途的明日方舟qq群聊bot

PS:  （代码质量低下），本项目当前版本即时部署效率不高（使用springboot+mybatis+mysql8+OPQ，部署服务器为centos7），主要是给沙雕群友整点乐子，后续可能会搞一些比较实用的功能。
 配置见上，OPQ是一款机器人框架，可以将程序挂载在一个qq（建议用小号，防止风控误封）上来实现群聊消息交互（OPQ的交互指令通过lua文件实现，更多指令见[OPQ WIKI](https://github.com/opq-osc/OPQ/wiki)）。

PPS：部分工具类引用自[02大佬](https://github.com/Strelizia02/ArknightsAPI)的工具类封装，特此感谢

PPPS：**特色功能**(然而并不是很好用)：

- ​    寻访记录查询（需要绑token从官网获取）
- ​    干员技能查询（需要调度解析数据文件）

其他的基本没什么用，包括at操作自动回复等都比较简单
这里提一下寻访查询功能，目前可以通过token获取官网的前100条寻访记录，目前token有效时长大概是7天？反正我自己用起来也不是很顺手

另外，原本是想用httpClient去抓取wiki上的数据解析，但发现处理html格式太麻烦了，而且如果网页更新我这功能就没用了（旧代码在测试类里）...于是改为取别人游戏解包后的jsnon文件.

PPPPS：注意，本项目仅供参考，任何人使用本项目相关代码造成任何损失雨我无瓜）

**测试截图**
菜单与寻访查询
![菜单与寻访查询](https://user-images.githubusercontent.com/60766110/217429122-aa1f4093-4d70-4641-ad24-de4f5da02510.jpg)
寻访图:
![寻访生成图](https://user-images.githubusercontent.com/60766110/217429269-66bd27ac-49eb-42b6-8e4a-0c4870011d27.jpg)
技能查询:
![技能查询](https://user-images.githubusercontent.com/60766110/217429297-344d561e-3c9c-4339-9295-c977f63e3fcd.jpg)
技能图：
![技能生成图](https://user-images.githubusercontent.com/60766110/217429369-00179b04-5805-4eb8-801d-44c6765a6d2b.jpg)


**关于OPQ机器人框架部署：**

```shell
# https://github.com/opq-osc/OPQ/

# 下载
# 查看linux系统的架构是amd还是arm 
# arch
# 若返回x86_64就是amd的

# 启动
# 启动需要到Gitter获取token并填入CoreConf.conf文件
https://developer.gitter.im/apps 
# 初次登录点sign就可以获得token
# 如果需要更改API的端口请更改Port (例如更改成2333端口,就改成Port = "0.0.0.0:2333")，默认为8888
# Linux 进入到文件目录 执行命令 ./OPQBot
# 若提示无权限，-bash: ./OPQBot: Permission denied 执行如下语句
[root@iZbp1hwh629hd4xz80i1z0Z OPQBot_6.7.5-20220624_linux_amd64]# chmod +755 OPQBot
# 然后执行./OPQBot
# ./OPQBot  为交互运行
# 不过这边建议后台运行
[root@iZbp1hwh629hd4xz80i1z0Z OPQBot_6.7.5-20220624_linux_amd64]# nohup ./OPQBot > OPQ.log 2>&1 &

# 登录
# 启动成功后再浏览器访问 http://IP:PORT/v1/Login/GetQRcode
# 手机qq扫码登录后即远程pc登录。

```

#### 关于服务器部署

```txt
1.需要字体配置：楷体文件(simkai.ttl)复制到linux服务器如下路径下，不然文字转图片会出现乱码：
/usr/local/java/jre/lib/fonts
2.楷体文件在resources下的Font里有，想换成其他字体自行百度即可（替换方法相同）
3.发版相关
配置文件中修改根据需求修改localhostip地址，OPQ绑定qq号，数据解析文件路径等
打包上传至指定位置然后执行:
nohup java -Xms180M -Xmx180M -jar /zoe/arknights-bot/jar/arknights-bot.jar > /zoe/arknights-bot/jar/logs/arknights-bot.log 2>&1 &
4.目前是手动上传json数据来定时解析，路径自定义即可，配置变量见pathConfig.path
json游戏数据取自https://github.com/Kengxxiao/ArknightsGameData 后续研究github库远程自动下载办法
```

#### Lua配置

**于请求信息中的sendToType:**

**为1时为好友会话,这时的toUser为对方的QQ号,groupid保持0就行**

**为2时是群聊,这时的toUser为群号,groupid填不填貌似都可以**

**为3时是临时会话,这时的toUser为对方的QQ号,groupid为发起临时会话的群号**

```lua
-- 私聊交互
function ReceiveFriendMsg(CurrentQQ, data)
    local body =
    {
        text = data.Content,
        qq = data.FromUin
    }
    response, error_message =
    http.post("" .. url .. "/private/chat",
        {
            body = json.encode(body),
            headers =
            {
                ["Accept"] = "*/*",
                ["Content-Type"] = "application/json"
            }
        })
    return 1
end

-- 群聊交互
function ReceiveGroupMsg(CurrentQQ, data)
    local body =
    {
        -- 驼峰格式的对应你群聊消息实体字段：如nickName为群聊中你的昵称
        content = data.Content,
        qq = data.FromUserId,
        nickName = data.FromNickName,
        groupId = data.FromGroupId
    }
    response, error_message =
    http.post("" .. url .. "/groups/general-message",
        {
            body = json.encode(body),
            headers =
            {
                ["Accept"] = "*/*",
                ["Content-Type"] = "application/json"
            }
        })
    return 1
end

```

#### 打包并启动服务

```shell
nohup java -Xms50M -Xmx50M -jar /zoe/arknights-bot/jar/arknights-bot.jar > /zoe/arknights-bot/jar/logs/arknights-bot.log 2>&1 &
```



#### 开发遇到的问题

```xml
1. 本地启动正常，但mvn install package后在服务器启动报错
Error creating bean with name 'groupsChatServiceImpl': Lookup method resolution failed; nested exception is java.lang.IllegalStateException: Failed to introspect Class [com.arknights.bot.app.service.impl.GroupsChatServiceImpl] from ClassLoader [org.springframework.boot.
## 本地运行正常，但mvn install package后服务器运行报错
一般来说本地能运行打包后服务器运行报错，可能是打包时依赖有问题，可以排查下有没有<optional></optional>或者<scope></scope>标签的依赖出现，这种很容易造成代码里运行时依赖取依赖A，但打包时取依赖B，就会报错
例如我遇到的这个：
       <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>
我们一般JSONObject依赖是用org.json.JSONObject,但是恰好上面这个依赖也可以导入JSONObject方法，而且本地启动是不会看出异常的，而optional为true表示又会造成依赖冲突时此依赖可选，这时打包里是另外一个JSONObject依赖（org.json.JSONObject），就会出现问题。
总结来说这时服务器代码里导包语句是import A，但是，maven打的包中对应依赖jar是B，就会报错，而且从报错信息中其实看不到有用的信息，只能怀疑是pom依赖问题，我也是挨个看pom依赖列表，注释后本地运行报错才发现这个问题。
2. 关于文字Font在centos中口口乱码:
https://blog.csdn.net/qq_39648029/article/details/112985928

```



#### 测试Lua与服务交互

```shell
2023-01-21 10:22:37.820  INFO 15253 --- [nio-8086-exec-8] c.a.b.a.s.impl.GroupsChatServiceImpl     : 测试输出群聊消息对应实体内容:GroupsEventInfo(msgType=null, groupId=593818810, qq=1486991950, nickName=皮卡丘, eventData=null, content=w查询)
2023-01-21 10:22:37.820  INFO 15253 --- [nio-8086-exec-8] c.a.b.a.s.impl.GroupsChatServiceImpl     : 当前接受消息内容:w查询
2023-01-21 10:22:37.839  INFO 15253 --- [nio-8086-exec-8] c.a.b.a.s.impl.GroupsChatServiceImpl     : 消息文本内容text:w查询

```

其对应lua函数如下：

```lua
function ReceiveGroupMsg(CurrentQQ, data)
    local body =
    {
        content = data.Content,
        qq = data.FromUserId,
        nickName = data.FromNickName,
        groupId = data.FromGroupId
    }
    response, error_message =
    http.post("" .. url .. "/groups/general-message",
        {
            body = json.encode(body),
            headers =
            {
                ["Accept"] = "*/*",
                ["Content-Type"] = "application/json"
            }
        })
    return 1
end
```

content，qq，nickName，groupId这几个驼峰命名的就是对应实体类字段
