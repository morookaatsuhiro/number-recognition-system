# 默认识别模型

将 **`model_Mnist10.pth`** 放在此目录。项目里 `Main.DEFAULT_MODEL_NAME` 已设为该文件名，无需改代码。

把训练导出的权重复制为上述文件名后提交到 Git，本地启动即可直接识别。

部署用的 **`demo-app/Dockerfile` 已包含** `COPY model/model_Mnist10.pth`。请同时保证 **`demo-app/.dockerignore` 里没有忽略整个 `model/` 目录**，否则构建上下文里没有该文件，线上会一直报「模型文件未找到」。

**Git LFS：** 若对 `.pth` 使用了 LFS，克隆/CI 时若**未**执行 `git lfs pull`，仓库里该路径仍是几 KB 的**文本指针**（以 `version https://git-lfs...` 开头），PyTorch 会报 `invalid load key, 'v'`。解决任选一：在构建机安装 `git lfs` 并拉取真实对象；或**取消**对该文件的 LFS 跟踪、改为直接提交真实二进制；或将权重放到对象存储/OCI 制品再在 Dockerfile 中下载/复制。
