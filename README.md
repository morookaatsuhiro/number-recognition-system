# number_reco

基于 `Spring Boot + MySQL + PyTorch CNN + Vite` 的手写数字识别项目。

这个仓库是一个前后端分离项目，但前端的真实运行方式有一个很重要的特点：

- `demo-app` 是后端，负责登录注册、验证码、模型管理、训练任务调度、图片上传和调用 Python 脚本
- `vue-demo` 用 `Vite` 提供开发服务器和打包能力
- 当前业务页面实际写在 `vue-demo/public/*.html` 里，并通过本地脚本 `vue-demo/public/js/vue.js` 与 `vue-demo/public/js/axios-0.18.0.js` 运行
- `vue-demo/src/` 里仍保留了 Vite 默认示例代码，当前业务页面并不从 `App.vue` 进入

如果你只是想把项目在新电脑上重新跑起来，看 `本地启动` 这一节即可。

## 项目功能

- 用户注册、登录、验证码校验
- 模型列表查看、模型详情查看、当前模型切换
- PyTorch CNN 模型训练
- 训练进度条、训练日志轮询、取消训练
- 训练结果保存到 MySQL
- 训练结果图展示 `Accuracy + F1 + Loss`
- 手写画布识别和本地图片上传识别
- 识别图片用 UUID 文件名保存，支持多个用户并行识别

## 技术栈

| 层级 | 当前实现 |
| --- | --- |
| 前端开发服务器 | `Vite 5` |
| 前端页面逻辑 | `Vue.js v2.6.10` + `Axios 0.18.0`（通过 `public/js/*.js` 引入） |
| 后端框架 | `Spring Boot 3.3.5` |
| Java 版本 | `Java 17` |
| 数据访问 | `Spring Data JPA` |
| 数据库 | `MySQL 8+` |
| AI 训练/推理 | `PyTorch` |
| Python 调用方式 | Java `ProcessBuilder` 调用 `cnn.py` |

## 目录结构

```text
number_reco/
├─ demo-app/
│  ├─ mvnw
│  ├─ mvnw.cmd
│  ├─ pom.xml
│  └─ src/main/
│     ├─ java/com/example/demoapp/
│     │  ├─ DemoAppApplication.java
│     │  ├─ AuthController.java
│     │  ├─ CaptchaController.java
│     │  ├─ SomeController.java
│     │  ├─ TrainingController.java
│     │  ├─ TrainingJobService.java
│     │  ├─ JavaToPy.java
│     │  ├─ Main.java
│     │  └─ cnn.py
│     └─ resources/
│        └─ application.properties
├─ vue-demo/
│  ├─ index.html                 # 入口页，当前只负责跳转到 /login.html
│  ├─ vite.config.js
│  ├─ public/
│  │  ├─ login.html
│  │  ├─ register.html
│  │  ├─ dashboard.html
│  │  ├─ train.html
│  │  ├─ results.html
│  │  ├─ phone.html
│  │  └─ js/
│  │     ├─ vue.js
│  │     └─ axios-0.18.0.js
│  └─ src/                       # Vite 默认示例代码，当前业务不走这里
├─ repo-assets/                  # 需要提交到 GitHub 的精选大文件目录
├─ .gitignore
└─ .gitattributes
```

## 当前关键配置

下面这些配置是当前仓库里真实存在、并且直接影响运行的内容。

| 配置项 | 当前值 | 位置 | 说明 |
| --- | --- | --- | --- |
| Python 可执行文件 | `C:\Users\67529\number-reco\python.exe` | `demo-app/src/main/java/com/example/demoapp/JavaToPy.java` | 换电脑后要么保持这个路径，要么改代码 |
| Python 脚本路径 | `src\main\java\com\example\demoapp\cnn.py` | `JavaToPy.java` | 训练和识别都通过它执行 |
| 数据库地址 | `jdbc:mysql://localhost:3306/db1?...` | `demo-app/src/main/resources/application.properties` | 当前固定连本机 MySQL |
| 数据库账号 | `root` | `application.properties` | 可自行改 |
| 数据库密码 | `1234` | `application.properties` | 可自行改 |
| 后端端口 | `8080` | Spring Boot 默认 | 前端接口全部写死到这个端口 |
| 前端端口 | `5173` | `vue-demo/vite.config.js` | 开发模式端口 |
| 默认模型名 | `model_Mnist10.pth` | `Main.java` | 会作为未选择模型时的默认值 |
| 静态资源根目录 | `file:./img/` | `application.properties` | 后端会把 `demo-app/img/` 暴露为静态资源 |
| CORS 白名单 | `localhost:5173`、`127.0.0.1:5173`、`192.168.43.252:5173` | `WebConfig.java` 与部分 Controller | 部署前通常需要修改 |

## 运行环境

推荐环境如下：

- Windows 10/11
- Java 17
- MySQL 8+
- Python 3.10
- Conda
- Node.js 18+
- `pnpm`

说明：

- 后端已经自带 `Maven Wrapper`，所以只要有 Java，不必单独安装 Maven
- 当前项目命令以下面这些 PowerShell 示例为准
- 如果你使用 NVIDIA GPU，`torch` 最好按 PyTorch 官网的 CUDA 命令安装；下面给的是通用 CPU/默认命令

## Python 依赖

`cnn.py` 当前实际使用到的 Python 依赖包括：

- `torch`
- `torchvision`
- `opencv-python`
- `numpy`
- `matplotlib`
- `pillow`
- `scipy`

## 本地启动

### 1. 创建数据库

当前项目不会自动创建数据库本身，但会自动建表。

你只需要先创建 `db1`：

```sql
CREATE DATABASE IF NOT EXISTS db1
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;
```

首次启动后端后，JPA 会自动创建或更新表，例如：

- `users`
- `training_config`

### 2. 创建 Python 环境

当前代码里 Python 路径写成了：

```text
C:\Users\67529\number-reco\python.exe
```

最省事的做法就是直接创建这个路径的 Conda 环境：

```powershell
conda create -p C:\Users\67529\number-reco python=3.10 -y
conda activate C:\Users\67529\number-reco
python -m pip install --upgrade pip
pip install torch torchvision opencv-python numpy matplotlib pillow scipy
```

安装完成后建议验证：

```powershell
conda activate C:\Users\67529\number-reco
python -c "import torch, cv2, numpy, matplotlib, PIL, scipy, torchvision; print('Python环境OK')"
python -c "import sys; print(sys.executable)"
```

如果最后一条输出不是 `C:\Users\67529\number-reco\python.exe`，那就需要改 `JavaToPy.java` 里的路径。

### 3. 启动后端

打开一个 PowerShell：

```powershell
cd "C:\Users\67529\OneDrive\Desktop\number_reco\demo-app"
.\mvnw.cmd spring-boot:run
```

如果你修改了数据库用户名、密码或端口，请同步修改：

`demo-app/src/main/resources/application.properties`

### 4. 启动前端

再打开一个 PowerShell：

```powershell
cd "C:\Users\67529\OneDrive\Desktop\number_reco\vue-demo"
pnpm install
pnpm dev
```

如果本机还没有 `pnpm`，先执行：

```powershell
corepack enable
corepack prepare pnpm@latest --activate
```

当前 `vite.config.js` 已配置 `open: '/login.html'`，启动后会优先打开登录页。

### 5. 访问地址

- 登录页：`http://localhost:5173/login.html`
- 注册页：`http://localhost:5173/register.html`
- 模型管理页：`http://localhost:5173/dashboard.html`
- 训练页：`http://localhost:5173/train.html`
- 训练结果页：`http://localhost:5173/results.html`
- 识别页：`http://localhost:5173/phone.html`

注意：

- `http://localhost:8080/` 返回 404 是正常的，因为当前后端主要提供 API 和静态图片，不负责前端首页
- 真正应该打开的是 `5173` 端口上的前端页面

## 第一次运行前必须知道的事

### 1. 新仓库默认可能没有可用模型

当前仓库里运行时模型目录 `demo-app/model/` 被 `.gitignore` 忽略，新克隆仓库后通常是空的。

这意味着：

- 如果你直接进入识别页，但 `demo-app/model/` 里没有任何 `.pth` 模型，识别会失败
- 如果你没手动选模型，系统默认会尝试使用 `model_Mnist10.pth`

建议第一次运行这样做：

1. 先在 `train.html` 训练一个模型，保存为新的 `.pth`
2. 或者把你之前备份的模型手动放到 `demo-app/model/`
3. 然后到 `dashboard.html` 里把当前模型切换到你实际存在的模型文件

### 2. 第一次训练会下载 MNIST 数据集

`cnn.py` 会在训练时下载 MNIST 到：

```text
demo-app/data/mnist/
```

第一次训练速度会明显慢一些，这是正常现象。

### 3. 训练参数里有两个字段不是直接输入真实值

训练页面的字段和 Python 实际值之间有一层缩放：

| 页面输入 | Python 中实际值 |
| --- | --- |
| `learningRate = 10` | `0.01` |
| `learningRate = 1` | `0.001` |
| `momentum = 90` | `0.90` |
| `momentum = 50` | `0.50` |

原因是 `cnn.py` 当前写法为：

- `learning_rate = int(argv[2]) / 1000`
- `momentum = int(argv[3]) / 100`

所以不要在页面里直接填 `0.01` 或 `0.9`，要按整数格式填。

## 页面与业务流程

### `login.html`

- 调用 `GET /api/captcha` 获取验证码图片
- 调用 `POST /api/login` 登录
- 登录成功后跳转到 `dashboard.html`
- 依赖 Cookie Session，所以前端已开启 `axios.defaults.withCredentials = true`

### `register.html`

- 调用 `POST /api/register`
- 注册信息包括用户名、邮箱、密码

### `dashboard.html`

- 调用 `GET /api/training/model` 获取模型文件列表
- 调用 `GET /api/training/modelInfo` 查看模型参数和训练结果
- 调用 `POST /api/training/setModel` 设置当前使用模型
- 调用 `GET /api/training/getCurrentModel` 查看当前模型

### `train.html`

- 先调用 `POST /api/training/saveConfig` 保存训练配置
- 再调用 `POST /api/training/start` 异步启动训练
- 通过 `GET /api/training/status` 轮询进度
- 通过 `POST /api/training/cancel` 取消训练
- 页面会用 `sessionStorage` 暂存当前训练任务 ID，刷新后会尝试恢复进度

### `results.html`

- 显示训练参数、训练输出
- 展示训练图 `Accuracy + F1 + Loss`
- 调用 `POST /api/training/updateTrainingResultByModelName` 把训练输出保存到数据库

### `phone.html`

- 支持手写画布识别
- 支持本地图片上传识别
- 识别接口为 `POST /upload`
- 上传文件保存到 `demo-app/img/`
- 后端返回识别结果文本和图片静态访问地址

## 主要后端接口

| 方法 | 路径 | 作用 |
| --- | --- | --- |
| `GET` | `/api/captcha` | 获取验证码图片并写入 Session |
| `POST` | `/api/register` | 用户注册 |
| `POST` | `/api/login?captchaInput=xxxx` | 用户登录 |
| `GET` | `/api/training/model` | 获取模型文件列表 |
| `GET` | `/api/training/modelInfo?modelName=...` | 获取模型详情 |
| `POST` | `/api/training/setModel` | 设置当前使用模型 |
| `GET` | `/api/training/getCurrentModel` | 获取当前模型 |
| `POST` | `/api/training/saveConfig` | 保存训练配置到数据库 |
| `POST` | `/api/training/start` | 异步启动训练任务 |
| `GET` | `/api/training/status?jobId=...` | 获取训练任务状态 |
| `POST` | `/api/training/cancel?jobId=...` | 取消训练任务 |
| `POST` | `/api/training/updateTrainingResultByModelName` | 保存训练结果文本到数据库 |
| `POST` | `/upload` | 上传图片并识别 |

补充说明：

- `TrainingController` 里还保留了旧的同步训练接口 `POST /api/training/train`
- 当前训练页使用的是新的异步接口 `/start`、`/status`、`/cancel`

## 训练与识别实现说明

### Python 训练与识别脚本

核心脚本位于：

`demo-app/src/main/java/com/example/demoapp/cnn.py`

它当前负责：

- 解析 Java 传入的训练参数
- 加载或训练 CNN 模型
- 使用 `cuda` 或 `cpu`
- 输出训练进度标记给 Java
- 生成训练指标图
- 处理单张或多数字图像识别

### Java 到 Python 的桥接

桥接类为：

`demo-app/src/main/java/com/example/demoapp/JavaToPy.java`

当前实现特点：

- 使用 `ProcessBuilder` 启动 Python
- 使用 UTF-8 读取 Python 输出，避免中文乱码
- 训练时会过滤 `TRAIN_*` 结构化标记，交给 `TrainingJobService` 解析

### 并发相关说明

当前已经做过这几项并发优化：

- `SomeController` 上传图片时使用 UUID 文件名，避免多个用户识别时互相覆盖
- 当前模型名通过 Session 保存，而不是全局静态变量
- 训练任务通过 `TrainingJobService` 管理，按 Session 区分任务

但是还存在一个真实限制：

- 训练图固定保存为 `demo-app/img/model_image/accuracy_plot.png`
- 也就是说，多人同时训练时，最终展示的图片文件可能互相覆盖

如果以后要把训练完全做成多用户隔离，训练图也要改成按任务或按模型名生成唯一文件。

## 运行时目录说明

这些目录是项目运行时会读写的真实目录：

| 目录 | 用途 |
| --- | --- |
| `demo-app/model/` | 训练后保存的模型文件 |
| `demo-app/img/` | 上传图片、默认图片、训练图等静态资源 |
| `demo-app/data/mnist/` | 首次训练时下载的 MNIST 数据集 |
| `demo-app/image/` | 旧运行目录，当前仓库已忽略 |

其中：

- `demo-app/img/` 会被后端映射为静态资源根目录
- 例如 `demo-app/img/model_image/accuracy_plot.png` 对应访问 URL 为 `http://localhost:8080/model_image/accuracy_plot.png`

## 本地自检命令

如果你改完配置后想快速确认环境是否正常，可以执行下面这些命令。

### 后端编译

```powershell
cd "C:\Users\67529\OneDrive\Desktop\number_reco\demo-app"
.\mvnw.cmd -q -DskipTests compile
```

### 前端构建

```powershell
cd "C:\Users\67529\OneDrive\Desktop\number_reco\vue-demo"
pnpm build
```

### Python 依赖自检

```powershell
conda activate C:\Users\67529\number-reco
python -c "import torch, cv2, numpy, matplotlib, PIL, scipy, torchvision; print('Python环境OK')"
```

## GitHub 上传

如果当前目录还不是 Git 仓库，可以按下面方式初始化并推送。

### 先在 GitHub 网页创建空仓库

例如：

```text
https://github.com/<你的用户名>/number_reco.git
```

然后在项目根目录执行：

```powershell
cd "C:\Users\67529\OneDrive\Desktop\number_reco"
git init
git branch -M main
git lfs install
git add .
git commit -m "Initial commit"
git remote add origin https://github.com/<你的用户名>/number_reco.git
git push -u origin main
```

如果你已经安装了 `gh`，也可以用：

```powershell
cd "C:\Users\67529\OneDrive\Desktop\number_reco"
git init
git branch -M main
git lfs install
git add .
git commit -m "Initial commit"
gh repo create number_reco --public --source . --remote origin --push
```

## 大模型和大图片的 Git 管理方式

### 当前仓库已做的处理

- `.gitignore` 已忽略运行时目录：
  - `demo-app/model/`
  - `demo-app/img/`
  - `demo-app/image/`
  - `demo-app/data/`
- `.gitattributes` 已配置 Git LFS：
  - `*.pth`
  - `*.pt`
  - `*.ckpt`
  - `*.onnx`
  - `repo-assets/images/**`

### 推荐做法

运行时文件不要直接提交。

正确方式是：

1. 程序运行时继续把模型保存在 `demo-app/model/`
2. 程序运行时继续把图片保存在 `demo-app/img/`
3. 真正想长期保留的模型，手动复制到 `repo-assets/models/`
4. 真正想长期保留的示例图片，手动复制到 `repo-assets/images/`
5. 再通过 Git LFS 提交

示例：

```powershell
cd "C:\Users\67529\OneDrive\Desktop\number_reco"
Copy-Item "demo-app\model\your-model.pth" "repo-assets\models\your-model.pth"
Copy-Item "demo-app\img\your-sample.png" "repo-assets\images\your-sample.png"
git add .gitattributes repo-assets
git commit -m "Add curated assets via Git LFS"
git push
```

## Vercel 部署说明

### 能部署什么

这个项目当前**适合把前端部署到 Vercel**，但**不适合把整个后端原样部署到 Vercel**。

原因是后端依赖：

- Spring Boot
- MySQL
- 本地 Python
- PyTorch
- 长时间训练任务
- 本地持久化目录 `model/`、`img/`

这些都不符合 Vercel 直接托管这个后端的典型方式。

### 推荐部署方案

- `vue-demo`：部署到 Vercel
- `demo-app`：部署到 Railway、Render、云服务器、Docker 主机等支持 Java + Python + 持久文件的环境

### 把前端部署到 Vercel 之前必须改的东西

当前前端页面里大量接口仍然写死为：

```text
http://localhost:8080
```

部署前至少要处理这些文件：

- `vue-demo/public/login.html`
- `vue-demo/public/register.html`
- `vue-demo/public/dashboard.html`
- `vue-demo/public/train.html`
- `vue-demo/public/results.html`
- `vue-demo/public/phone.html`

同时还要修改后端 CORS：

- `demo-app/src/main/java/com/example/demoapp/WebConfig.java`
- `demo-app/src/main/java/com/example/demoapp/AuthController.java`
- `demo-app/src/main/java/com/example/demoapp/TrainingController.java`
- `demo-app/src/main/java/com/example/demoapp/SomeController.java`

### 使用 Vercel CLI 部署前端

```powershell
npm i -g vercel
vercel login
cd "C:\Users\67529\OneDrive\Desktop\number_reco\vue-demo"
vercel
vercel --prod
```

如果你用 Vercel 网页导入 GitHub 仓库，建议配置：

- Framework Preset：`Vite`
- Root Directory：`vue-demo`
- Install Command：`pnpm install`
- Build Command：`pnpm build`
- Output Directory：`dist`

### 为什么这个前端能部署到 Vercel

虽然业务页面写在 `public/*.html`，但 `vite build` 会把这些文件原样拷贝到 `dist/`，所以部署后仍然可以通过这些页面访问：

- `/login.html`
- `/dashboard.html`
- `/train.html`
- `/results.html`
- `/phone.html`

## 已知限制

当前项目还有下面这些限制，README 这里明确写出来，避免后面接手的人踩坑：

1. Python 可执行文件路径写死在 `JavaToPy.java`
2. 数据库地址、账号、密码写死在 `application.properties`
3. 前端 API 地址写死在多个 HTML 文件里
4. CORS 只放行了几个本地域名
5. 训练图使用固定文件名 `img/model_image/accuracy_plot.png`
6. `vue-demo/src/` 仍是默认示例，不是当前业务入口

## 后续重构建议

如果后面要继续维护这个项目，优先级最高的改造建议如下：

1. 把 Python 路径改成配置项，而不是写死在 Java 代码里
2. 把数据库配置改成环境变量
3. 把前端 `localhost:8080` 提取成统一配置
4. 把训练图改成按任务或按模型文件名生成唯一文件
5. 把当前多页面 HTML 逐步迁移成统一的 Vue 组件结构
