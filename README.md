# 客户下单系统

一个简洁高效的项目需求提交平台，基于 Spring Boot 构建，支持表单提交、附件上传、邮件通知、数据导出等功能。

## 功能特性

### 核心功能
- 📋 **多维度表单** - 支持分类多选、产品类型、项目规模等多种选项
- 📎 **附件上传** - 支持 PDF、Word、Excel、图片等文件上传，单文件最大 50MB
- 📧 **邮件通知** - 提交成功后自动发送确认邮件给客户
- 📝 **提交记录** - 记录列表展示、分页浏览、详情查看
- ⬇️ **文件下载** - 附件在线预览/下载
- 📊 **数据导出** - 一键导出所有记录为 Excel 文件
- 🔒 **安全防护** - 路径穿越防护、输入验证

### 技术栈
| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.5 | 核心框架 |
| MyBatis-Plus | 3.5.5 | ORM 持久层 |
| H2 Database | - | 内嵌数据库，开箱即用 |
| Thymeleaf | - | 模板引擎 |
| Java Mail | - | 邮件发送 |
| Apache POI | 5.2.5 | Excel 导出 |
| Lombok | - | 简化代码 |

## 快速开始

### 环境要求
- JDK 17+
- Maven 3.6+

### 运行项目

```bash
# 克隆项目
git clone <repository-url>
cd submitform

# 打包
mvn clean package -DskipTests

# 运行
java -jar target/submitform-1.0.0.jar
```

访问 http://localhost:8080 即可使用。

### 开发模式

```bash
mvn spring-boot:run
```

热部署已配置，修改代码后自动重启。

## 项目结构

```
submitform/
├── src/main/java/com/pcnx/submitform/
│   ├── SubmitFormApplication.java    # 应用入口
│   ├── config/                        # 配置类
│   │   ├── MetaObjectHandlerConfig.java
│   │   └── MybatisPlusConfig.java
│   ├── controller/                    # 控制器
│   │   └── FormSubmissionController.java
│   ├── entity/                       # 实体类
│   │   └── FormSubmission.java
│   ├── mapper/                       # 数据访问层
│   │   └── FormSubmissionMapper.java
│   └── service/                      # 业务逻辑层
│       ├── FormSubmissionService.java
│       ├── MailService.java
│       └── impl/
│           └── FormSubmissionServiceImpl.java
├── src/main/resources/
│   ├── db/                           # 数据库脚本
│   ├── static/                       # 静态资源
│   │   ├── css/style.css
│   │   └── js/main.js
│   ├── templates/                    # 页面模板
│   │   ├── index.html               # 提交表单页
│   │   ├── manage.html              # 记录列表页
│   │   ├── detail.html              # 详情页
│   │   └── submit-email.html        # 邮件模板
│   └── application.yml              # 应用配置
├── uploads/                          # 上传文件存储目录
└── pom.xml
```

## 配置说明

### 数据库

默认使用 H2 内嵌数据库，数据存储在 `./data/submitform.mv.db`。

如需切换到 MySQL，修改 `application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/submitform?useSSL=false&serverTimezone=Asia/Shanghai
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: root
    password: your_password
```

### 邮件配置

在 `application.yml` 中配置邮箱信息：

```yaml
spring:
  mail:
    host: smtp.qq.com
    port: 465
    username: your-email@qq.com
    password: your-authorization-code  # QQ邮箱授权码
    protocol: smtps
```

**获取 QQ 邮箱授权码：**
1. 登录 QQ 邮箱网页版
2. 设置 → 账户 → POP3/IMAP/SMTP/Exchange/CardDAV/CalDAV服务
3. 开启 POP3/SMTP 服务
4. 按提示发送短信获取授权码

### H2 控制台

开发环境下可访问 H2 控制台查看数据库：

- 地址：http://localhost:8080/h2-console
- JDBC URL：`jdbc:h2:file:./data/submitform`
- 用户名：`sa`
- 密码：（留空）

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/` | 首页 - 表单提交 |
| POST | `/submit` | 提交表单 |
| GET | `/manage` | 提交记录列表（分页） |
| GET | `/detail/{id}` | 记录详情 |
| GET | `/download` | 下载附件 |
| GET | `/export` | 导出所有数据为 Excel |
| GET | `/h2-console` | H2 数据库控制台 |

## 页面说明

### 提交表单页 (`/`)
- 选择分类（多选）：网站、App、小程序、原生App、游戏
- 选择产品类型（单选）：后台管理系统、公司业务系统、AI应用系统等
- 计划书选项（可选）
- 项目规模（单选）
- 项目资料（可选，支持富文本和文件上传）
- 留言服务（必填）
- 客户邮箱（必填，用于发送确认邮件）

### 提交记录页 (`/manage`)
- 展示所有提交记录
- 显示状态：待处理、已查看、已回复
- 分页浏览
- 点击查看详情
- **导出 Excel**：一键导出所有数据

### 详情页 (`/detail/{id}`)
- 完整展示提交信息
- 附件下载
- 客户邮箱展示
- 提交 IP 和时间信息

## 注意事项

1. **文件上传目录**：`./uploads/`，请确保目录存在且有写入权限
2. **数据持久化**：重启应用后 H2 数据自动保留在本地文件
3. **邮件功能**：需正确配置邮箱授权码才能发送邮件
4. **安全提醒**：生产环境请修改默认配置，关闭 H2 控制台外部访问

## 许可证

MIT License
