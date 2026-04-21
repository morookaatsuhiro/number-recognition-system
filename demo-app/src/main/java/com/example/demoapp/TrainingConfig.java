package com.example.demoapp;


import jakarta.persistence.*;

@Entity
@Table(name = "training_config", uniqueConstraints = {@UniqueConstraint(columnNames = "saveModel")})
public class TrainingConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int batchSize;
    private float learningRate;
    private float momentum;
    private int epochs;
    private int stride;
    private int padding;
    private int kernelSize;
    private int outChannel;
    private int inChannel;
    @Column(unique = true)  // 添加唯一性约束
    private String saveModel;

    //------------------------------------------------------------------------------
    @Lob  // 使用 @Lob 注解可以存储较大的文本内容
    private String trainingResult;

    public String getTrainingResult() {
        return trainingResult;
    }

    public void setTrainingResult(String trainingResult) {
        this.trainingResult = trainingResult;
    }
//------------------------------------------------------------------------------




// Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public float getLearningRate() {
        return learningRate;
    }

    public void setLearningRate(float learningRate) {
        this.learningRate = learningRate;
    }

    public float getMomentum() {
        return momentum;
    }

    public void setMomentum(float momentum) {
        this.momentum = momentum;
    }

    public int getEpochs() {
        return epochs;
    }

    public void setEpochs(int epochs) {
        this.epochs = epochs;
    }

    public int getStride() {
        return stride;
    }

    public void setStride(int stride) {
        this.stride = stride;
    }

    public int getPadding() {
        return padding;
    }

    public void setPadding(int padding) {
        this.padding = padding;
    }

    public int getKernelSize() {
        return kernelSize;
    }

    public void setKernelSize(int kernelSize) {
        this.kernelSize = kernelSize;
    }

    public int getOutChannel() {
        return outChannel;
    }

    public void setOutChannel(int outChannel) {
        this.outChannel = outChannel;
    }

    public int getInChannel() {
        return inChannel;
    }

    public void setInChannel(int inChannel) {
        this.inChannel = inChannel;
    }

    public String getSaveModel() {
        return saveModel;
    }

    public void setSaveModel(String saveModel) {
        this.saveModel = saveModel;
    }


}