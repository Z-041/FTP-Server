# Java FTP Server

一个完整的FTP服务器实现，完全遵循RFC 959标准及相关扩展规范。

## 功能特性

### FTP协议实现
- 完整支持FTP核心命令集（USER、PASS、LIST、RETR、STOR、DELE、MKD、RMD、CWD等）
- 主动模式(PORT)和被动模式(PASV)数据传输
- 文件上传、下载、删除、重命名、创建目录等操作
- 用户认证与权限管理
- ASCII和二进制传输模式支持

### FTP服务器程序
- 多线程架构，支持多客户端同时连接
- 服务器配置管理（端口、根目录、最大连接数等）
- 完整的日志记录功能
- 连接超时管理和资源释放

### 用户界面(UI)
- 显示服务器运行状态（启动/停止、监听端口、连接数）
- 服务器启动/停止控制
- 实时显示连接的客户端信息
- 用户管理界面（添加/删除/编辑用户，设置权限）
- 服务器配置参数修改与保存
- 日志查看、筛选和搜索功能

## 项目结构

```
src/main/java/com/ftpserver/
├── Main.java                 # 主入口类
├── command/
│   └── CommandHandler.java   # FTP命令处理器
├── config/
│   └── ServerConfig.java     # 服务器配置管理
├── data/
│   ├── DataConnection.java   # 数据连接抽象类
│   ├── ActiveDataConnection.java  # 主动模式连接
│   └── PassiveDataConnection.java # 被动模式连接
├── server/
│   └── FtpServer.java        # FTP服务器核心
├── ui/
│   └── MainFrame.java        # GUI主界面
├── user/
│   ├── User.java             # 用户模型
│   └── UserManager.java      # 用户管理器
└── util/
    └── Logger.java           # 日志系统
```

## 编译与运行

### 环境要求
- Java 17 或更高版本
- Maven 3.6+

### 编译项目
```bash
mvn clean compile
```

### 打包项目
```bash
mvn clean package
```

### 运行程序
```bash
# 使用Maven运行
mvn exec:java -Dexec.mainClass="com.ftpserver.Main"

# 或运行打包后的JAR
java -jar target/ftp-server-1.0.0.jar
```

## 使用说明

### 初次启动
1. 运行程序后会自动打开GUI界面
2. 默认监听端口为2121
3. 默认根目录为用户主目录下的ftp_root文件夹
4. 需要先添加用户才能连接

### 添加用户
1. 点击"Users"标签
2. 点击"Add User"按钮
3. 填写用户名、密码、主目录（可选）
4. 配置用户权限
5. 点击"OK"保存

### 启动服务器
1. 点击顶部的"Start Server"按钮
2. 等待状态变为"Running on port 2121"
3. 服务器即可接受客户端连接

### 连接服务器
使用任意FTP客户端（如FileZilla、WinSCP等）连接：
- 主机：localhost 或服务器IP
- 端口：2121
- 用户名/密码：你在UI中设置的用户

## 支持的FTP命令

| 命令 | 描述 |
|------|------|
| USER | 用户名 |
| PASS | 密码 |
| QUIT | 退出连接 |
| SYST | 系统类型 |
| FEAT | 特性列表 |
| PWD | 显示当前目录 |
| CWD | 改变工作目录 |
| CDUP | 返回上级目录 |
| MKD | 创建目录 |
| RMD | 删除目录 |
| DELE | 删除文件 |
| RNFR | 重命名源文件 |
| RNTO | 重命名目标文件 |
| LIST | 列出目录内容 |
| NLST | 列出文件名 |
| RETR | 下载文件 |
| STOR | 上传文件 |
| TYPE | 设置传输类型（A/I） |
| PORT | 主动模式 |
| PASV | 被动模式 |
| EPSV | 扩展被动模式 |
| NOOP | 空操作 |

## 配置文件

配置文件位于 `config/server.properties`：
- port: 监听端口
- rootDirectory: 根目录
- maxConnections: 最大连接数
- dataPortRangeStart/End: 被动模式端口范围
- enablePassiveMode/enableActiveMode: 模式开关

用户数据存储在 `config/users.json`

## 许可证

本项目仅供学习和研究使用。
