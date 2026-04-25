FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml ./
COPY src ./src

RUN mvn -q -DskipTests package

FROM python:3.10-slim-bookworm

ENV DEBIAN_FRONTEND=noninteractive \
    PYTHONUNBUFFERED=1 \
    MPLBACKEND=Agg \
    NUMBER_RECO_PYTHON_EXECUTABLE=python3 \
    NUMBER_RECO_MODEL_DIR=/app/model \
    NUMBER_RECO_IMAGE_DIR=/app/img \
    NUMBER_RECO_TRAINING_DEFAULT_IMAGE_PATH=/app/img/default/default.png \
    NUMBER_RECO_TRAINING_PLOT_PATH=/app/img/model_image/accuracy_plot.png

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        openjdk-17-jre-headless \
        libglib2.0-0 \
        libsm6 \
        libxext6 \
        libxrender1 \
        libgl1 \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY --from=build /app/target/*.jar ./app.jar
COPY src/main/java/com/example/demoapp/cnn.py ./src/main/java/com/example/demoapp/cnn.py
# 仓库中的默认权重（与 Main.DEFAULT_MODEL_NAME 一致）；构建前请确保该文件存在
COPY model/model_Mnist10.pth ./model/model_Mnist10.pth

RUN mkdir -p /app/model /app/img/default /app/img/model_image /app/data

EXPOSE 8080

CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
