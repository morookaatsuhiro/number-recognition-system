# number_reco

基于 `Spring Boot + MySQL + PyTorch CNN + Vite` 的手写数字识别系统。

## 在线演示（示例站点）

以下为已部署的前端入口，便于在浏览器中直接体验（实际后端地址由 `runtime-config.js` 与部署环境决定）：

| 端 | 说明 | 链接 |
| --- | --- | --- |
| PC 端 | 登录后进入控制台，可进行模型管理与训练等 | [login.html](https://number-recognition-system.vercel.app/login.html) |
| 手机端 | 手写画布 / 上传图片，使用当前模型做识别 | [phone.html](https://number-recognition-system.vercel.app/phone.html) |

## CNN 原理参考视频

卷积神经网络（CNN）工作原理的可视化讲解可参考 bilibili 视频：[CNN 工作原理（bilibili）](https://www.bilibili.com/video/BV1gz67YrE7H/)。

**说明：** 该视频**并非本仓库作者制作**，也未参与本项目的开发与上传；仅为作者在网上检索到的公开科普 / 可视化内容，转载至此 README **仅供学习参考**，版权归原视频发布者所有。若原链接失效或版权方有异议，请通过 issue 等方式联系本项目维护者以便调整或删除链接。

## 项目说明
- `demo-app`：Spring Boot 后端，负责登录注册、验证码、模型管理、训练任务、图片上传和调用 `cnn.py`
- `vue-demo`：前端工程，当前业务页面位于 `public/*.html`，由 `Vite` 提供开发和构建能力
- `cnn.py`：负责模型训练和图片识别，识别结果会输出当前使用的模型名称

## 主要功能
- 用户注册、登录、验证码
- 模型列表、模型详情、当前模型切换
- CNN 模型训练、进度轮询、取消训练
- 手写画布识别、本地图片上传识别
- 训练结果图展示与识别结果返回

## 项目结构
```text
number_reco/
├─ demo-app/
│  ├─ Dockerfile
│  ├─ requirements.txt
│  ├─ pom.xml
│  └─ src/main/
│     ├─ java/com/example/demoapp/
│     │  ├─ AuthController.java
│     │  ├─ SomeController.java
│     │  ├─ TrainingController.java
│     │  ├─ TrainingJobService.java
│     │  ├─ NumberRecoRuntimeConfig.java
│     │  ├─ JavaToPy.java
│     │  ├─ Main.java
│     │  └─ cnn.py
│     └─ resources/
│        └─ application.properties
├─ vue-demo/
│  ├─ public/
│  │  ├─ login.html
│  │  ├─ register.html
│  │  ├─ dashboard.html
│  │  ├─ train.html
│  │  ├─ results.html
│  │  ├─ phone.html
│  │  └─ js/runtime-config.js
│  ├─ src/
│  ├─ package.json
│  └─ vite.config.js
├─ .gitignore
└─ .gitattributes
```

## 环境要求
- Java 17
- MySQL 8+
- Python 3.10
- Node.js 18+
- `pnpm`

## 常用配置

| 项目 | 位置 | 说明 |
| --- | --- | --- |
| 前端后端地址 | `vue-demo/public/js/runtime-config.js` | 默认 `http://localhost:8080` |
| 数据库 | `demo-app/src/main/resources/application.properties` | 支持 `SPRING_DATASOURCE_URL`、`SPRING_DATASOURCE_USERNAME`、`SPRING_DATASOURCE_PASSWORD` |
| Python 路径 | `demo-app/src/main/java/com/example/demoapp/NumberRecoRuntimeConfig.java` | 支持 `NUMBER_RECO_PYTHON_EXECUTABLE`，本地默认值是 Windows 路径 |
| 模型目录 | `NumberRecoRuntimeConfig.java` | 支持 `NUMBER_RECO_MODEL_DIR`，默认 `model` |
| 图片目录 | `NumberRecoRuntimeConfig.java` | 支持 `NUMBER_RECO_IMAGE_DIR`，默认 `img` |
| 训练默认图 | `NumberRecoRuntimeConfig.java` | `NUMBER_RECO_TRAINING_DEFAULT_IMAGE_PATH`，默认 `default/default.png` |
| 训练结果图 | `NumberRecoRuntimeConfig.java` | `NUMBER_RECO_TRAINING_PLOT_PATH`，默认 `model_image/accuracy_plot.png` |
| CORS 白名单 | `NumberRecoRuntimeConfig.java` | `NUMBER_RECO_CORS_ALLOWED_ORIGINS` |
| Session Cookie | `application.properties` | `SERVER_SESSION_COOKIE_SECURE`、`SERVER_SESSION_COOKIE_SAME_SITE` |
| 服务端口 | `application.properties` | `PORT`，默认 `8080` |

## 本地启动

以下命令以 PowerShell 为例。

### 1. 创建数据库
```sql
CREATE DATABASE IF NOT EXISTS db1
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;
```

### 2. 安装 Python 依赖
```powershell
cd demo-app
python -m pip install -r requirements.txt
```

如果你的 Python 不在默认 Windows 路径下，启动后端前先设置：

```powershell
$env:NUMBER_RECO_PYTHON_EXECUTABLE="python"
```

### 3. 启动后端
```powershell
cd demo-app
.\mvnw.cmd spring-boot:run
```

### 4. 启动前端
```powershell
cd vue-demo
pnpm install
pnpm dev
```

### 5. 访问页面
- `http://localhost:5173/login.html`
- `http://localhost:5173/register.html`
- `http://localhost:5173/dashboard.html`
- `http://localhost:5173/train.html`
- `http://localhost:5173/results.html`
- `http://localhost:5173/phone.html`

## 首次运行注意
- 仓库默认不会提交运行时目录：`demo-app/img/`、`demo-app/data/`；`demo-app/model/` 中仅 **`model_Mnist10.pth`** 可随仓库提供，作为默认识别权重（与 `Main.DEFAULT_MODEL_NAME` 一致）。
- 若你克隆后没有该文件，请先训练一次导出同名模型，或从团队/Release 取得后放入 `demo-app/model/`。
- 首次训练会自动下载 MNIST 到 `demo-app/data/mnist/`
- 训练页面里的 `learningRate=10` 实际表示 `0.01`，`momentum=90` 实际表示 `0.90`
- 当前训练结果图默认文件名是 `img/model_image/accuracy_plot.png`

## 部署

### 前端部署到 Vercel
- Root Directory：`vue-demo`
- Install Command：`pnpm install`
- Build Command：`pnpm build`
- Output Directory：`dist`

部署前把 `vue-demo/public/js/runtime-config.js` 中的 `apiBaseUrl` 改成你的后端地址。

### 后端部署到 Railway
`demo-app` 已包含 `Dockerfile` 和 `requirements.txt`，可以直接作为 Railway 服务根目录部署。

至少需要配置这些环境变量：

```text
SPRING_DATASOURCE_URL=jdbc:mysql://<host>:3306/<db>?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME=<username>
SPRING_DATASOURCE_PASSWORD=<password>
NUMBER_RECO_CORS_ALLOWED_ORIGINS=https://<your-vercel-domain>
SERVER_SESSION_COOKIE_SECURE=true
SERVER_SESSION_COOKIE_SAME_SITE=none
```

如果你希望模型文件和上传图片在重部署后仍然保留，给 Railway 挂载 Volume，并把 `NUMBER_RECO_MODEL_DIR` 和 `NUMBER_RECO_IMAGE_DIR` 指向挂载目录下的子目录。

**约 1GB 内存时（避免 OOM / 被平台警告）：** 应用由 **JVM + MySQL 连接 + 训练时 Python 子进程（PyTorch、MNIST）** 共同占用。`Dockerfile` 已设 `JAVA_OPTS`（`MaxRAMPercentage=32%` 等）和 `OMP_NUM_THREADS=1` 等；`application.properties` 已收紧 Hikari 与 Tomcat 线程。训练页**尽量把 batch size 调到 8～16**，且不要多开并发训练。若仍顶满，可在 Railway 为服务设置环境变量 `JAVA_OPTS` 将 `MaxRAMPercentage` 再降到 `28.0` 或升级实例内存。

## 已知限制
- 当前业务页面仍在 `vue-demo/public/*.html`，还没有完全迁移到统一的 Vue SPA
- `vue-demo/src/` 仍保留 Vite 默认示例，不是当前业务入口
- 本地未设置 `NUMBER_RECO_PYTHON_EXECUTABLE` 时，默认 Python 路径仍是 Windows 本机路径
- 训练结果图默认使用固定文件名，多个训练任务可能覆盖同一张图
