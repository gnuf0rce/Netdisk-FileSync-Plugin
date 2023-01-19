# [Netdisk FileSync Plugin](https://github.com/gnuf0rce/Netdisk-FileSync-Plugin)

> 基于 [Mirai Console](https://github.com/mamoe/mirai-console) 的 文件同步/备份 插件

[![Release](https://img.shields.io/github/v/release/gnuf0rce/Netdisk-FileSync-Plugin)](https://github.com/gnuf0rce/Netdisk-FileSync-Plugin/releases)
[![Downloads](https://img.shields.io/github/downloads/gnuf0rce/Netdisk-FileSync-Plugin/total)](https://repo1.maven.org/maven2/io/github/gnuf0rce/netdisk-filesync-plugin/)
[![MiraiForum](https://img.shields.io/badge/post-on%20MiraiForum-yellow)](https://mirai.mamoe.net/topic/765)
[![maven-central](https://img.shields.io/maven-central/v/io.github.gnuf0rce/netdisk-filesync-plugin)](https://search.maven.org/artifact/io.github.gnuf0rce/netdisk-filesync-plugin)
 

本插件可以将接收到的`群文件`同步到百度网盘  
在 `2.14.0` 后可将接收到的`秒传码`或者`分享链接`保存到百度网盘  

备份的文件在 `/apps/${app_name}/${current_date}/`, 其中的 `apps` 在百度网盘中显示为 [我的应用数据](https://pan.baidu.com/disk/main#/index?category=all&path=%2Fapps)

本插件也可作为前置插件为其他插件提供百度云上传的API

## 指令

### OAUTH指令

| 指令                   | 描述          |
|:---------------------|:------------|
| `/<baidu> <oauth>`   | 默认百度账户的绑定   |
| `/<baidu> <bind>`    | 为当前用户绑定百度账户 |
| `/<baidu> <host>`    | 刷新HOST      |
| `/<baidu> <user>`    | 刷新当前账号信息    |

PS: since `2.14.0`, 添加 `/baidu bind`, 用于`用户/群`绑定, 原 `/baidu oauth` 作为默认账号使用
1.  `/baidu bind` 具体用法/作用为: 私聊时，绑定到用户，群聊时，如果操作人是`管理员/群主`，则绑定到群，否则绑定到用户  
2.  `/baidu oauth` 具体用法/作用为: 绑定后提供一个默认网盘
3.  `群文件` 同步时优先选择 `群聊绑定的网盘`, 其次 `默认网盘`
4.  `秒传码` 保存时优先选择 `用户绑定的网盘`, 其次 `群聊绑定的网盘`, 最后 `默认网盘`

## 设置

### oauth.yml

插件上传文件功能需要百度网盘API支持。  
请到 <https://pan.baidu.com/union/main/application/personal> 申请应用，并将获得的APP信息填入  
信息只在启动时读取，修改后需重启

### upload.yml

*   `https` 使用Https协议下载文件
*   `reply` 同步后回复消息
*   `log` 插件启动时上传日志文件

## 在插件项目中引用

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    compileOnly("io.github.gnuf0rce:netdisk-filesync-plugin:${version}")
}

mirai {
    jvmTarget = JavaVersion.VERSION_11
}
``` 

### 示例代码

```kotlin
try {
    io.github.gnuf0rce.mirai.NetDisk.getUserInfo()
} catch (error: NoClassDefFoundError) { 
    logger.warning { "相关类加载失败，请安装 https://github.com/gnuf0rce/Netdisk-FileSync-Plugin $error" }
    throw error
}
```

## 安装

### MCL 指令安装

`./mcl --update-package io.github.gnuf0rce:netdisk-filesync-plugin --channel maven-stable --type plugin`

### 手动安装

1.  从 [Releases](https://github.com/gnuf0rce/Netdisk-FileSync-Plugin/releases) 或者 [Maven](https://repo1.maven.org/maven2/io/github/gnuf0rce/netdisk-filesync-plugin/) 下载 `mirai2.jar`
2.  将其放入 `plugins` 文件夹中