# [Netdisk FileSync Plugin](https://github.com/gnuf0rce/Netdisk-FileSync-Plugin)

> 基于 [Mirai Console](https://github.com/mamoe/mirai-console) 的 文件同步/备份 插件

[![Release](https://img.shields.io/github/v/release/gnuf0rce/Netdisk-FileSync-Plugin)](https://github.com/gnuf0rce/Netdisk-FileSync-Plugin/releases)
[![Downloads](https://img.shields.io/github/downloads/gnuf0rce/Netdisk-FileSync-Plugin/total)](https://shields.io/category/downloads)
[![MiraiForum](https://img.shields.io/badge/post-on%20MiraiForum-yellow)](https://mirai.mamoe.net/topic/765)

本插件可以将接收到的群文件消息同步到百度网盘  
备份的文件在 `/apps/${app_name}/${group_id}/`

## 指令

### Pixiv相关操作指令

| 指令             | 描述                       |
|:-----------------|:---------------------------|
| `/<baidu-oauth>` | 按照指令完成百度账户的登陆 |

## 设置

### oauth.yml

插件上传文件功能需要百度网盘API支持。  
请到 <https://pan.baidu.com/union/main/application/personal> 申请应用，并将获得的APP信息填入  
信息只在启动时读取，修改后需重启，并使用 `/baidu-oauth` 认证百度账号