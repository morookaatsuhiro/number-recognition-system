# 默认识别模型

将 **`model_Mnist10.pth`** 放在此目录。项目里 `Main.DEFAULT_MODEL_NAME` 已设为该文件名，无需改代码。

把训练导出的权重复制为上述文件名后提交到 Git，本地启动即可直接识别。

部署用的 **`demo-app/Dockerfile` 已包含** `COPY model/model_Mnist10.pth`。请同时保证 **`demo-app/.dockerignore` 里没有忽略整个 `model/` 目录**，否则构建上下文里没有该文件，线上会一直报「模型文件未找到」。
