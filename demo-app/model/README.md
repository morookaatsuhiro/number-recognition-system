# 默认识别模型

将 **`model_Mnist10.pth`** 放在此目录。项目里 `Main.DEFAULT_MODEL_NAME` 已设为该文件名，无需改代码。

把训练导出的权重复制为上述文件名后提交到 Git，本地启动即可直接识别。

若用 **Docker / Railway** 部署且希望镜像内自带该文件：在 `Dockerfile` 里 `COPY cnn.py` 下一行增加  
`COPY model/model_Mnist10.pth ./model/model_Mnist10.pth`  
（仅当仓库里已存在该文件时再添加，否则构建会报错。）
