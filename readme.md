# [Netdisk FileSync Plugin](https://github.com/gnuf0rce/Netdisk-FileSync-Plugin)

> 基于 [Mirai Console](https://github.com/mamoe/mirai-console) 的 文件同步/备份 插件

[![Release](https://img.shields.io/github/v/release/gnuf0rce/Netdisk-FileSync-Plugin)](https://github.com/gnuf0rce/Netdisk-FileSync-Plugin/releases)
[![Downloads](https://img.shields.io/github/downloads/gnuf0rce/Netdisk-FileSync-Plugin/total)](https://repo1.maven.org/maven2/io/github/gnuf0rce/netdisk-filesync-plugin/)
[![MiraiForum](https://img.shields.io/badge/post-on%20MiraiForum-yellow)](https://mirai.mamoe.net/topic/765)
[![maven-central](https://img.shields.io/maven-central/v/io.github.gnuf0rce/netdisk-filesync-plugin)](https://search.maven.org/artifact/io.github.gnuf0rce/netdisk-filesync-plugin)

本插件可以将接收到的群文件消息同步到百度网盘  
备份的文件在 `/apps/${app_name}/${group_id}/`

本插件也可作为前置插件为其他插件提供百度云上传的API

## 指令

### OAUTH指令

| 指令                   | 描述          |
|:---------------------|:------------|
| `/<baidu> <oauth>`   | 百度账户的登陆     |
| `/<baidu> <refresh>` | 使用 token 刷新 |

## 设置

### oauth.yml

插件上传文件功能需要百度网盘API支持。  
请到 <https://pan.baidu.com/union/main/application/personal> 申请应用，并将获得的APP信息填入  
信息只在启动时读取，修改后需重启，并使用 `/baidu oauth` 认证百度账号

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
    } catch (exception: NoClassDefFoundError) { 
        logger.warning { "相关类加载失败，请安装 https://github.com/gnuf0rce/Netdisk-FileSync-Plugin $exception" }
        throw exception
    }
```

## 安装

### MCL 指令安装

`./mcl --update-package io.github.gnuf0rce:netdisk-filesync-plugin --channel maven-stable --type plugin`

### 手动安装

1.  从 [Releases](https://github.com/gnuf0rce/Netdisk-FileSync-Plugin/releases) 或者 [Maven](https://repo1.maven.org/maven2/io/github/gnuf0rce/netdisk-filesync-plugin/) 下载 `mirai2.jar`
2.  将其放入 `plugins` 文件夹中