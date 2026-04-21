# repo-assets

这个目录用于保存需要提交到 GitHub 的大模型和示例图片。

## 使用规则

- `repo-assets/models/`：手动挑选后需要长期保留的模型文件
- `repo-assets/images/`：手动挑选后需要长期保留的示例图片

## 不要直接提交这些运行时目录

下面这些目录是程序运行时自动生成的，本仓库默认忽略：

- `demo-app/model/`
- `demo-app/img/`
- `demo-app/image/`

这些目录适合本地运行，不适合直接作为 GitHub 仓库存储。

## 推荐做法

1. 训练完成后，从 `demo-app/model/` 里挑选你真正想保留的模型
2. 把它复制到 `repo-assets/models/`
3. 从 `demo-app/img/` 里挑选你想保留的示例图片
4. 把它复制到 `repo-assets/images/`
5. 使用 Git LFS 提交这些文件
