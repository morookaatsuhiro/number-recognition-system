
import torch
import os
import pickle
import cv2

import numpy as np
import matplotlib
matplotlib.use('Agg')
from matplotlib import pyplot as plt
from torch.utils.data import DataLoader
from torchvision import transforms
from torchvision import datasets
import torch.nn.functional as F
import matplotlib.image as mpimg  # 用于加载手动输入的图像
from PIL import Image, ImageOps
from torch.utils.data import random_split
from scipy.ndimage import median_filter



import sys


# #统一中文编码格式utf-8
sys.stdout.reconfigure(encoding='utf-8', line_buffering=True)



#用户可手动输入
batch_size = int(sys.argv[1])                 #4个选项16\32\64\128
lr = int(sys.argv[2])                   #学习率0.01或0.001
learning_rate = lr/1000
m = int(sys.argv[3])                    #动量0.5-0.9
momentum = m/100
EPOCH = int(sys.argv[4])                #模型训练的轮数自定义
#卷积部分手动配置
convstride = int(sys.argv[5])         #步长
conv2dpadding  = int(sys.argv[6])      #边界填充
Pool2dkernel_size = int(sys.argv[7])   #池化内核
conv2dkernel_size = int(sys.argv[8])   #卷积内核
conv2doutchannels = int(sys.argv[9])   #输出通道
conv2dinchannels = int(sys.argv[10])    #输入通道


#图片绝对路径手动配置
image_path = sys.argv[11]

# #模型存储路径手动配置
u_m_name = sys.argv[12]
s_m_name = sys.argv[13]
#是否训练模型
flag = sys.argv[14]


model_dir = os.getenv('NUMBER_RECO_MODEL_DIR') or os.path.join('.', 'model')
image_dir = os.getenv('NUMBER_RECO_IMAGE_DIR') or os.path.join('.', 'img')
training_plot_path = os.getenv('NUMBER_RECO_TRAINING_PLOT_PATH')
# 分割后的单字调试图（必须用 os.path.join；Windows 的 ".\\image\\..." 在 Linux 上 dirname 为空会报错）
segment_image_dir = os.path.join(image_dir, 'segment_debug')


def resolve_image_path(path):
    if os.path.isabs(path):
        return path
    return os.path.join(image_dir, path)


image_path = resolve_image_path(image_path)
model_path_u = os.path.join(model_dir, u_m_name)  # 将文件夹和文件名组合成完整路径
model_path_s = os.path.join(model_dir, s_m_name)
use_model_path = model_path_u
save_train_model_path = model_path_s


os.makedirs(model_dir, exist_ok=True)
os.makedirs(image_dir, exist_ok=True)
os.makedirs(segment_image_dir, exist_ok=True)
if training_plot_path:
    plot_parent_dir = os.path.dirname(training_plot_path)
    if plot_parent_dir:
        os.makedirs(plot_parent_dir, exist_ok=True)
else:
    training_plot_path = os.path.join(image_dir, 'model_image', 'accuracy_plot.png')
    os.makedirs(os.path.dirname(training_plot_path), exist_ok=True)

window = int(((((28 - conv2dkernel_size + conv2dpadding * 2)/convstride + 1)/Pool2dkernel_size - conv2dkernel_size + 2 * conv2dpadding)/convstride + 1)/Pool2dkernel_size)



# 噪声函数
"""在图像中添加随机噪声"""
def add_noise(image, noise_level=0.1):
    image_array = np.array(image)
    noise = np.random.normal(0, noise_level, image_array.shape)
    noisy_image = image_array + noise * 255
    noisy_image = np.clip(noisy_image, 0, 255).astype(np.uint8)  # 保证值在0-255范围内
    return Image.fromarray(noisy_image)
# 归一化，softmax归一化指数函数,其中0.1307是mean均值和0.3081是std标准差
transform_train = transforms.Compose([
    transforms.RandomRotation(15),  # 随机旋转角度，模拟手写字体倾斜
    transforms.RandomAffine(0, translate=(0.1, 0.1)),  # 随机平移，模拟手写字体位置变化
    transforms.RandomResizedCrop(28, scale=(0.9, 1.1)),  # 随机裁剪和缩放
    transforms.Lambda(lambda img: add_noise(img, noise_level=0.05)),  # 添加噪声
    transforms.ToTensor(),
    transforms.Normalize((0.1307,), (0.3081,))])


# 测试数据集保持不变，不添加数据增强
transform_test = transforms.Compose([
    transforms.ToTensor(),
    transforms.Normalize((0.1307,), (0.3081,))
])

train_loader = None
test_loader = None
val_loader = None

if flag == 'train':
    # 下载/获取数据集，其中root为数据集存放路径，train=True即训练集否则为测试集。
    train_dataset = datasets.MNIST(root='./data/mnist', train=True, transform=transform_train, download=True)
    test_dataset = datasets.MNIST(root='./data/mnist', train=False, transform=transform_test, download=True)

    train_size = int(0.8 * len(train_dataset))  # 80% 作为训练集
    val_size = len(train_dataset) - train_size  # 剩下的作为验证集
    train_dataset, val_dataset = random_split(train_dataset, [train_size, val_size])
    val_loader = DataLoader(val_dataset, batch_size=batch_size, shuffle=False)
    # 实例化一个dataset后，然后用Dataloader包起来。
    train_loader = DataLoader(train_dataset, batch_size=batch_size, shuffle=True)
    test_loader = DataLoader(test_dataset, batch_size=batch_size, shuffle=False)

#卷积神经网络cnn核心算法
class Net(torch.nn.Module):
    def __init__(self):
        super(Net, self).__init__()
        # 说明：创建第一个卷积层模块 conv1，包括一个卷积操作、一个 ReLU 激活函数和一个池化操作。
        self.conv1 = torch.nn.Sequential(
            # 1：输入通道数。表示输入的是单通道图像（如灰度图像，通常用于 MNIST 数据集），每张图片的输入尺寸为 (batch_size, 1, 28, 28)。
            # 10：输出通道数，表示经过卷积后得到 10 个特征图。
            # kernel_size=5：卷积核的大小为 5x5。
            torch.nn.Conv2d(in_channels=conv2dinchannels, out_channels=conv2doutchannels, kernel_size=conv2dkernel_size , padding = conv2dpadding , stride = convstride),
            # torch.nn.ReLU()：ReLU 激活函数，将负值置零，正值保持不变。
            torch.nn.ReLU(),
            torch.nn.Dropout(p=0.3),
            # kernel_size=2：池化窗口大小为 2x2，进行最大池化操作，缩小图像尺寸，将每 2x2 的区域压缩为一个最大值。
            torch.nn.MaxPool2d(kernel_size=Pool2dkernel_size)

        )
        #连接上面的输出通道作为第二次卷积的输入接口
        secondconv2doutchannels = conv2doutchannels*2
        self.conv2 = torch.nn.Sequential(
            torch.nn.Conv2d(in_channels=conv2doutchannels, out_channels=secondconv2doutchannels, kernel_size=conv2dkernel_size,padding = conv2dpadding , stride = convstride),
            torch.nn.ReLU(),
            torch.nn.Dropout(p=0.3),
            torch.nn.MaxPool2d(kernel_size=Pool2dkernel_size)

        )


        # 说明：创建全连接层 fc，由两层线性变换组成。
        self.fc = torch.nn.Sequential(
            # 320：输入的特征数，来自卷积层的展平输出大小（320 = 20 * 4 * 4，20 是上一层的输出通道数，4x4 是经过池化后的特征图尺寸）。
            # 50：输出的神经元个数，即第一层全连接层的输出维度。
            torch.nn.Linear(secondconv2doutchannels*window*window, secondconv2doutchannels),
            # 50：输入维度来自上一层。
            # 10：输出维度为 10，对应 0~9 的数字分类。
            torch.nn.Linear(secondconv2doutchannels, 10)

        )

    def forward(self, x):
        # 说明：forward 定义了模型的前向传播逻辑。
        # batch_size = x.size(0)：获取输入的批量大小，x.size(0) 返回批量维度的大小。
        batch_size = x.size(0)

        x = self.conv1(x)  # 一层卷积层,一层池化层,一层激活层(图是先卷积后激活再池化，差别不大)
        x = self.conv2(x)  # 再来一次

        # flatten 变成全连接网络需要的输入 (batch, 20,4,4) ==> (batch,320), -1 此处自动算出的是320
        x = x.view(batch_size, -1)


        #传入全连接层
        x = self.fc(x)
        return x  # 最后输出的是维度为10的，也就是（对应数学符号的0~9）

device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
print(f"Using device: {device}")
# 训练集乱序，测试集有序
model = Net().to(device)

#定义了损失函数
# 作用：     CrossEntropyLoss 是一种常用的分类任务损失函数，适用于多类别分类问题。它结合了 LogSoftmax 和 Negative Log Likelihood 损失，适用于没有经过 softmax 的模型输出。
# 使用场景： 在分类问题中，例如 MNIST 手写数字识别任务，交叉熵损失常用于计算预测类别与真实类别之间的误差。
# 参数：     reduction：默认是 'mean'，它会对批次中的所有样本的损失取均值。如果设置为 'sum'，它将对所有样本的损失求和。
criterion = torch.nn.CrossEntropyLoss().to(device)  # 交叉熵损失

#定义了优化器
# 作用：SGD 代表 随机梯度下降（Stochastic Gradient Descent），是用于更新模型参数的优化算法。优化器通过梯度下降法更新模型权重，最小化损失函数。
# model.parameters()：该参数将模型的所有参数传递给优化器，优化器会在训练过程中自动更新这些参数。
# lr=learning_rate：学习率 (learning_rate) 决定了每次更新模型参数时步长的大小。较大的学习率会使模型参数更新较快，但可能不稳定；较小的学习率则更新较慢，但更稳定。
# momentum=momentum：冲量（Momentum）用于加速梯度下降并减少振荡。它通过引入上一次的更新方向来平滑当前的更新。典型的值通常在 0.5 到 0.9 之间。
# 参数详细解释：
# 学习率 (lr)：控制每次更新时的步长大小。一般的学习率值在 0.01 或 0.001 左右。
# 冲量 (momentum)：加速优化过程，减少震荡。引入动量的 SGD 会记住上一次的更新方向并结合当前的梯度进行更新，能够加快收敛速度并减少局部振荡。
optimizer = torch.optim.SGD(model.parameters(), lr=learning_rate, momentum=momentum)  # lr学习率，momentum冲量
# 交叉熵损失函数 (CrossEntropyLoss)：用于分类任务，衡量预测结果与真实标签之间的差异。
# SGD 优化器 (torch.optim.SGD)：随机梯度下降优化算法，使用学习率和冲量来更新模型参数。

# 把单独的一轮一环封装在函数类里
def train(epoch):

    # 初始化本轮（epoch）的累计损失 running_loss，每次 epoch 开始时将其重置为 0。
    running_loss = 0.0  # 这整个epoch的loss清零

    # 初始化本轮中处理的样本总数 running_total，用于计算准确率，每次 epoch 开始时重置。
    running_total = 0

    # 初始化本轮中预测正确的样本数量 running_correct，用于计算准确率，每次 epoch 开始时重置。
    running_correct = 0

    # 通过 enumerate 对训练数据加载器 train_loader 进行批次遍历。batch_idx 是当前批次的索引，data 是当前批次的数据，包含输入 (inputs) 和目标 (target)。
    # train_loader 是 PyTorch 的数据加载器，负责从数据集中按批加载数据。
    total_batches = len(train_loader)
    progress_interval = max(1, total_batches // 100)

    for batch_idx, data in enumerate(train_loader, 0):

        # 将批次数据 data 解包为 inputs（模型输入）和 target（标签）。inputs 是输入特征，target 是实际类别标签。
        # 将输入和标签数据都移动到 GPU 上
        inputs, target = data[0].to(device), data[1].to(device)

        # 清零优化器中的梯度。每次反向传播后，梯度会累加，所以需要在每次更新前清零已累积的梯度。
        optimizer.zero_grad()

        # forward + backward + update
        # 前向传播。将输入 inputs 传入模型 model，计算输出 outputs。model 是之前定义的神经网络。
        outputs = model(inputs)

        # 计算损失值。使用损失函数 criterion 比较模型的预测 outputs 和真实标签 target。criterion 通常是交叉熵损失（CrossEntropyLoss）或均方误差（MSELoss），具体取决于任务。
        loss = criterion(outputs, target)

        # 反向传播。根据损失 loss 计算模型参数的梯度，这一步会通过链式法则将损失的梯度传播回每个参数。
        loss.backward()

        # 优化器更新模型参数。通过计算得到的梯度更新模型的权重，optimizer 是 PyTorch 的优化器，如 SGD、Adam 等。
        optimizer.step()

        # 把运行中的loss累加起来，为了下面300次一除
        # 将当前批次的损失值 loss.item() 加入 running_loss，用于计算 300 次迭代的平均损失。
        running_loss += loss.item()


        # 把运行中的准确率acc算出来

        _, predicted = torch.max(outputs.data, dim=1)  # 计算模型的预测结果。torch.max 函数返回张量的最大值及其索引（在此处最大值的索引就是模型的预测类别）。dim=1 表示在类别维度上选择最大值。
        running_total += inputs.shape[0]   #记录当前批次的样本数量 inputs.shape[0]，将其加到总样本数 running_total，用于计算准确率。
        # 计算当前批次中模型预测正确的样本数，并将其累加到 running_correct。
        # predicted == target 生成一个布尔张量，表示每个样本是否预测正确，sum() 计算预测正确的样本总数。
        running_correct += (predicted == target).sum().item()

        if batch_idx % 300 == 299:  # 不想要每一次都出loss，浪费时间，选择每300次出一个平均损失,和准确率
            print('[%d, %5d]: loss: %.3f , acc: %.2f %%'
                  % (epoch + 1, batch_idx + 1, running_loss / 300, 100 * running_correct / running_total),
                  flush=True)
            running_loss = 0.0  # 这小批300的loss清零
            running_total = 0
            running_correct = 0  # 这小批300的acc清零

        if batch_idx % progress_interval == 0 or batch_idx == total_batches - 1:
            progress_percent = ((epoch + (batch_idx + 1) / total_batches) / EPOCH) * 100
            print(
                f"TRAIN_PROGRESS|epoch={epoch + 1}|epochs={EPOCH}|batch={batch_idx + 1}|batches={total_batches}|percent={progress_percent:.2f}",
                flush=True
            )

        # torch.save(model.state_dict(), './model_Mnist.pth')
        # torch.save(optimizer.state_dict(), './optimizer_Mnist.pth')

#推试集
def calculate_macro_f1(labels, predictions, num_classes=10):
    labels = np.array(labels)
    predictions = np.array(predictions)
    f1_scores = []

    for class_idx in range(num_classes):
        true_positive = np.sum((labels == class_idx) & (predictions == class_idx))
        false_positive = np.sum((labels != class_idx) & (predictions == class_idx))
        false_negative = np.sum((labels == class_idx) & (predictions != class_idx))

        precision = true_positive / (true_positive + false_positive) if (true_positive + false_positive) > 0 else 0.0
        recall = true_positive / (true_positive + false_negative) if (true_positive + false_negative) > 0 else 0.0
        f1_score = 2 * precision * recall / (precision + recall) if (precision + recall) > 0 else 0.0
        f1_scores.append(f1_score)

    return float(np.mean(f1_scores))


def test():
    correct = 0
    total = 0
    test_loss = 0.0
    all_labels = []
    all_predictions = []
    #禁用梯度计算。在测试阶段不需要反向传播和梯度计算，因此用 torch.no_grad() 来节省内存和计算资源。
    with torch.no_grad():  # 测试集不用算梯度

        #通过 for 循环遍历测试数据集 test_loader
        for data in test_loader:
            #将测试数据批次中的 images 和 labels 解包。
            images, labels = data[0].to(device), data[1].to(device)
            #将测试输入 images 传入模型 model 进行前向传播，得到模型的预测输出 outputs。
            outputs = model(images)
            loss = criterion(outputs, labels)
            test_loss += loss.item()
            _, predicted = torch.max(outputs.data, dim=1)  # dim = 1 列是第0个维度，行是第1个维度，沿着行(第1个维度)去找  1.最大值  2.最大值的下标
            # 累加当前批次的样本数量到 total。labels.size(0) 表示当前批次中有多少个样本。
            total += labels.size(0)  # 张量之间的比较运算
            # 将模型预测正确的样本数量累加到 correct 中。
            # predicted == labels 会返回一个布尔张量，表示每个样本是否被正确预测，sum() 计算预测正确的样本数。
            correct += (predicted == labels).sum().item()
            all_labels.extend(labels.cpu().numpy().tolist())
            all_predictions.extend(predicted.cpu().numpy().tolist())
    acc = correct / total
    avg_test_loss = test_loss / len(test_loader)
    macro_f1 = calculate_macro_f1(all_labels, all_predictions)
    print('[%d / %d]: Accuracy on test set: %.1f %% , F1: %.4f , Loss: %.4f'
          % (epoch+1, EPOCH, 100 * acc, macro_f1, avg_test_loss))  # 求测试的准确率，正确数/总数
    return acc, macro_f1, avg_test_loss


def denoise_image(image):
    """使用中值滤波去噪"""
    image_array = np.array(image)
    denoised_image = median_filter(image_array, size=2)  # 使用中值滤波，size 控制平滑程度
    return Image.fromarray(denoised_image)


# 在图像四周添加黑色边距
def add_padding(image, padding=2):
    """
    在图像四周增加黑色边距
    padding：边距的大小（像素）
    """
    image_with_padding = ImageOps.expand(image, border=padding, fill='black')
    return image_with_padding


def preprocess_image(image):
    """
    转换为灰度并进行二值化处理，增强对比度。
    """
    image = ImageOps.grayscale(image)
    image = ImageOps.invert(image)
    image = image.point(lambda p: p > 128 and 255)  # 二值化处理
    image = ImageOps.autocontrast(image)  # 自动调整对比度
    image = denoise_image(image)  # 去噪



    return image



def segment_lines(image):
    """
    对图像进行行分割，返回每一行的图像列表。
    """
     # 应用预处理步骤
    image = preprocess_image(image)

    binary_image = np.array(image)
    # 对图像进行水平投影（对每一行的像素值求和）
    horizontal_projection = np.sum(binary_image, axis=1)
    line_images = []
    start = None
    threshold = 5  # 设置阈值，判断行的分割点

    for i, value in enumerate(horizontal_projection):
        if value > threshold and start is None:
            start = i
        elif value <= threshold and start is not None:
            line = binary_image[start:i, :]  # 提取该行
            if line.shape[0] > 5:  # 忽略过小的行（可能是噪声）
                line_image = Image.fromarray(line)
                line_images.append(line_image)
            start = None

    return line_images



def segment_columns(line_image, inner_size=20, padding=4):
    """
    对一行图像进行列分割，返回该行的每个数字图像。
    """
    binary_image = np.array(line_image)

    # 对图像进行垂直投影（对每一列的像素值求和）
    vertical_projection = np.sum(binary_image, axis=0)
    digit_images = []
    start = None
    threshold = 10  # 设置阈值，判断列的分割点

    for i, value in enumerate(vertical_projection):
        if value > threshold and start is None:
            start = i
        elif value <= threshold and start is not None:
            digit = binary_image[:, start:i]  # 提取该列（数字）
            if digit.shape[1] > 5:  # 忽略小的噪点
                digit_image = Image.fromarray(digit).resize((inner_size, inner_size))  # 将每个数字调整为28x28
                digit_image = ImageOps.expand(digit_image, border=padding, fill='black')

                #                 # 标准化处理
                # transform = transforms.Compose([
                #     transforms.ToTensor(),  # 转换为张量
                #     transforms.Normalize((0.1307,), (0.3081,))  # 归一化
                # ])
                # 调整最终图像大小为28x28
                digit_image = digit_image.resize((28, 28))



                # image_tensor = transform(digit_image).unsqueeze(0)  # 转换为Tensor并加上batch维度
                # digit_images.append(image_tensor)
                digit_images.append(digit_image)  # 将结果添加到数字图像列表中
            start = None

    return digit_images



def save_image(image, file_path):
    file_path = os.path.normpath(file_path)
    folder = os.path.dirname(file_path)
    if folder:
        os.makedirs(folder, exist_ok=True)

    # 将张量转换为二维或三维数组
    if isinstance(image, torch.Tensor):
        image = image.squeeze()  # 移除多余的维度，将形状 (1, 1, 28, 28) 转换为 (28, 28)
        image = image.numpy()  # 转换为 numpy 数组

    # 保存图片
    plt.imshow(image, cmap='gray')
    plt.axis('off')  # 隐藏坐标轴
    plt.savefig(file_path)  # 保存文件
    plt.close()  # 关闭当前的 plot 以释放内存


def add_test_noise(image, noise_level=0.05):
    """为测试图像添加随机噪声以模拟现实情况"""
    image_array = np.array(image)
    noise = np.random.normal(0, noise_level, image_array.shape)
    noisy_image = image_array + noise * 255
    noisy_image = np.clip(noisy_image, 0, 255).astype(np.uint8)
    return Image.fromarray(noisy_image)


def predict_digits_in_image(model, image_path):
    """
    处理整张图像，对每行每列的数字进行识别，并打印每个分割的图像及其概率分布。
    """
    # 打开图像并转换为灰度
    image = Image.open(image_path).convert('L')
    image = add_test_noise(image, noise_level=0.02)  # 模拟噪声

    # 分割出每一行的图像
    line_images = segment_lines(image)

    model.eval()  # 设置模型为评估模式

    all_predictions = []
    inference_transform = transforms.Compose([
        transforms.ToTensor(),
        transforms.Normalize((0.1307,), (0.3081,))
    ])

    for line_idx, line_image in enumerate(line_images):
        # 分割出该行的每个数字
        digit_images = segment_columns(line_image)

        # 逐个识别该行的数字
        line_predictions = []
        for col_idx, digit_image in enumerate(digit_images):
            # 确保 digit_image 是张量
            if isinstance(digit_image, Image.Image):
                image_tensor = inference_transform(digit_image).unsqueeze(0).to(device)
            else:
                image_tensor = digit_image.to(device)  # 如果已经是张量，直接使用

            # 进行推测
            with torch.no_grad():
                output = model(image_tensor)
                probabilities = torch.softmax(output, dim=1).cpu()  # 计算每个类别的概率
                _, predicted = torch.max(output.data, 1)  # 获取最高概率的类别

            line_predictions.append(predicted.item())  # 保存预测结果

            # 保存分割后的图像（路径与 Java 的 NUMBER_RECO_IMAGE_DIR 一致，且跨平台）
            file_path = os.path.join(segment_image_dir, f"line_{line_idx + 1}_digit_{col_idx + 1}.png")
            save_image(digit_image, file_path)

            # # 显示分割后的图像
            # # digit_image_np = digit_image.squeeze().numpy()  # 移除多余的维度，将形状 (1, 1, 28, 28) 转换为 (28, 28)
            # digit_image_np = np.array(digit_image)  # 将 PIL 图像转换为 numpy 数组
            # plt.imshow(digit_image_np, cmap='gray')
            # plt.title(f"行 {line_idx + 1} 列 {col_idx + 1} 的数字")
            # plt.show()

            # 打印每个类别的概率分布
            print(f"行 {line_idx + 1} 列 {col_idx + 1} 的预测结果为 {predicted.item()}，各类别的概率分布如下：")
            for i, prob in enumerate(probabilities[0]):
                print(f"类别 {i} 的概率: {prob.item() * 100:.2f}%")

        all_predictions.append(line_predictions)

    return all_predictions


def _model_path_looks_like_git_lfs_pointer(path) -> bool:
    """未拉取 LFS 时，仓库里的 .pth 可能只是几行文本，PyTorch 会报 invalid load key 'v'（首字为 version）。"""
    try:
        with open(path, "rb") as f:
            head = f.read(200)
    except OSError:
        return False
    return head.startswith(b"version https://git-lfs.github.com/") or (
        b"git-lfs" in head and head.lstrip().startswith(b"version")
    )


# 修改手动推测函数，识别图像中每一行、每一列的所有数字
def manual_prediction_multiple_digits_in_rows_and_columns():
    model_path = use_model_path

    if _model_path_looks_like_git_lfs_pointer(model_path):
        print(
            "错误：当前路径下是 Git LFS 指针文本，不是真实 .pth 权重文件，无法加载。"
            "请在能访问 LFS 的环境执行: git lfs install && git lfs pull；"
            "或取消对该文件的 Git LFS 跟踪后重新提交真实二进制；"
            "或把已下载的真实 model_Mnist10.pth 放入服务器 model 目录后再部署。"
        )
        return

    # 检查模型是否已训练并保存
    def _load_checkpoint(path):
        # PyTorch 2.6+ 默认 weights_only=True，部分 .pth（旧训练脚本/非纯张量）会 UnpicklingError
        # 项目内模型来自自训/自有仓库，可回退为 weights_only=False
        try:
            return torch.load(path, map_location=device, weights_only=True)
        except FileNotFoundError:
            raise
        except TypeError:
            # 旧版 PyTorch 无 weights_only 参数
            return torch.load(path, map_location=device)
        except Exception:
            return torch.load(path, map_location=device, weights_only=False)

    try:
        model.load_state_dict(_load_checkpoint(model_path))
        print(f"当前使用模型: {os.path.basename(model_path)}")
        print("模型加载成功，准备进行手动推测。")
    except FileNotFoundError:
        print("模型文件未找到。请确保模型已经训练并保存。")
        return
    except pickle.UnpicklingError as e:
        if "invalid load key" in str(e) and "'v'" in str(e):
            print(
                "错误：模型文件不是有效 PyTorch 二进制（常见：Git LFS 只提交了指针，线上仍是文本「version...」）。"
                "请执行 git lfs pull 后重新部署，或不用 LFS、直接提交真实 .pth。"
            )
        else:
            raise
        return

    # 使用新的函数对图像中的所有行和列的数字进行识别
    predicted_classes = predict_digits_in_image(model, image_path)
    print(f"模型推测结果: {predicted_classes}")


#评估模型
def validate():
    model.eval()  # 设置模型为评估模式
    val_loss = 0.0
    correct = 0
    total = 0
    with torch.no_grad():
        for data in val_loader:
            images, labels = data[0].to(device), data[1].to(device)
            outputs = model(images)
            loss = criterion(outputs, labels)
            val_loss += loss.item()
            _, predicted = torch.max(outputs.data, dim=1)
            total += labels.size(0)
            correct += (predicted == labels).sum().item()
    acc = correct / total
    print(f'Validation Accuracy: {100 * acc:.1f}%')
    return val_loss / len(val_loader)








if __name__ == '__main__':

    if flag == 'train':
        #训练模型
        acc_list_test = []
        f1_list_test = []
        val_loss_list = []
        for epoch in range(EPOCH):
            train(epoch)
            acc_test, f1_test, test_loss = test()
            acc_list_test.append(acc_test)
            f1_list_test.append(f1_test)
            val_loss = validate()  # 验证集损失
            val_loss_list.append(val_loss)
            print(
                f"TRAIN_EPOCH_SUMMARY|epoch={epoch + 1}|epochs={EPOCH}|testAcc={100 * acc_test:.2f}|testF1={f1_test:.4f}|testLoss={test_loss:.4f}|valLoss={val_loss:.4f}",
                flush=True
            )
        # 保存训练后的模型
        print("TRAIN_SAVING|percent=99.00", flush=True)
        torch.save(model.state_dict(), save_train_model_path)
        # 在同一张图中显示准确率、F1 和损失变化
        epochs_axis = list(range(1, len(acc_list_test) + 1))
        accuracy_percent = [acc * 100 for acc in acc_list_test]
        f1_percent = [f1 * 100 for f1 in f1_list_test]
        accuracy_epochs = [epoch - 0.02 for epoch in epochs_axis]
        f1_epochs = [epoch + 0.02 for epoch in epochs_axis]

        fig, ax1 = plt.subplots(figsize=(10, 6))
        accuracy_line = ax1.plot(
            accuracy_epochs,
            accuracy_percent,
            color='blue',
            marker='o',
            linewidth=2,
            linestyle='-',
            markersize=6,
            zorder=3,
            label='Accuracy (%)'
        )
        f1_line = ax1.plot(
            f1_epochs,
            f1_percent,
            color='red',
            marker='s',
            linewidth=2,
            linestyle='--',
            markersize=6,
            zorder=2,
            label='F1 (%)'
        )
        ax1.set_xlabel('Epoch')
        ax1.set_ylabel('Accuracy / F1 (%)')
        ax1.set_ylim(0, 100)
        ax1.set_xticks(epochs_axis)
        ax1.grid(True, linestyle='--', alpha=0.3)

        ax2 = ax1.twinx()
        loss_line = ax2.plot(epochs_axis, val_loss_list, color='black', marker='^', label='Loss')
        ax2.set_ylabel('Loss')

        lines = accuracy_line + f1_line + loss_line
        labels = [line.get_label() for line in lines]
        ax1.legend(lines, labels, loc='best')
        plt.title('Training Metrics')
        fig.tight_layout()
        plt.savefig(training_plot_path)  # 保存图像
        plt.close(fig)
        print(f"TRAIN_DONE|percent=100.00|model={s_m_name}", flush=True)
#         plt.show()

    elif flag == 'use':
        # 手动输入并进行推测（包含多个数字的图像）
        manual_prediction_multiple_digits_in_rows_and_columns()




















