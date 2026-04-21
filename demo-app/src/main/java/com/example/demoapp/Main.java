package com.example.demoapp;

import java.io.IOException;

public class Main {
    public static final String DEFAULT_MODEL_NAME = "model_Mnist10.pth";

    private int batch_size = 32;
    private int learning_rate = 10;
    private int momentum = 90;
    private int EPOCH = 1;
    private int convstride = 1;
    private int conv2dpadding = 1;
    private int Pool2dkernel_size = 2;
    private int conv2dkernel_size = 3;
    private int conv2doutchannels = 32;
    private int conv2dinchannels = 1;
    private String flag = "use";
    private String u_model_name = DEFAULT_MODEL_NAME;
    private String s_model_name = "model_Mnist2.pth";
    private String img_path = "img/test2.jpg";

    public Main(String imgPath) {
        this.img_path = imgPath;
    }

    public Main() {
    }

    public String getImg_path() {
        return img_path;
    }

    public void setImg_path(String img_path) {
        this.img_path = img_path;
    }

    public String runApp() throws IOException, InterruptedException {
        return JavaToPy.getFile(
                String.valueOf(batch_size),
                String.valueOf(learning_rate),
                String.valueOf(momentum),
                String.valueOf(EPOCH),
                String.valueOf(convstride),
                String.valueOf(conv2dpadding),
                String.valueOf(Pool2dkernel_size),
                String.valueOf(conv2dkernel_size),
                String.valueOf(conv2doutchannels),
                String.valueOf(conv2dinchannels),
                getImg_path(),
                u_model_name,
                s_model_name,
                flag
        );
    }

    public int getBatch_size() {
        return batch_size;
    }

    public void setBatch_size(int batch_size) {
        this.batch_size = batch_size;
    }

    public int getLearning_rate() {
        return learning_rate;
    }

    public void setLearning_rate(int learning_rate) {
        this.learning_rate = learning_rate;
    }

    public int getMomentum() {
        return momentum;
    }

    public void setMomentum(int momentum) {
        this.momentum = momentum;
    }

    public int getEPOCH() {
        return EPOCH;
    }

    public void setEPOCH(int EPOCH) {
        this.EPOCH = EPOCH;
    }

    public int getConvstride() {
        return convstride;
    }

    public void setConvstride(int convstride) {
        this.convstride = convstride;
    }

    public int getConv2dpadding() {
        return conv2dpadding;
    }

    public void setConv2dpadding(int conv2dpadding) {
        this.conv2dpadding = conv2dpadding;
    }

    public int getPool2dkernel_size() {
        return Pool2dkernel_size;
    }

    public void setPool2dkernel_size(int pool2dkernel_size) {
        Pool2dkernel_size = pool2dkernel_size;
    }

    public int getConv2dkernel_size() {
        return conv2dkernel_size;
    }

    public void setConv2dkernel_size(int conv2dkernel_size) {
        this.conv2dkernel_size = conv2dkernel_size;
    }

    public int getConv2doutchannels() {
        return conv2doutchannels;
    }

    public void setConv2doutchannels(int conv2doutchannels) {
        this.conv2doutchannels = conv2doutchannels;
    }

    public int getConv2dinchannels() {
        return conv2dinchannels;
    }

    public void setConv2dinchannels(int conv2dinchannels) {
        this.conv2dinchannels = conv2dinchannels;
    }

    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

    public String getU_model_name() {
        return u_model_name;
    }

    public void setU_model_name(String u_model_name) {
        this.u_model_name = u_model_name;
    }

    public String getS_model_name() {
        return s_model_name;
    }

    public void setS_model_name(String s_model_name) {
        this.s_model_name = s_model_name;
    }

    public String runTraining() throws IOException, InterruptedException {
        img_path = "img/default/default.png";
        return JavaToPy.getFile(
                String.valueOf(batch_size),
                String.valueOf(learning_rate),
                String.valueOf(momentum),
                String.valueOf(EPOCH),
                String.valueOf(convstride),
                String.valueOf(conv2dpadding),
                String.valueOf(Pool2dkernel_size),
                String.valueOf(conv2dkernel_size),
                String.valueOf(conv2doutchannels),
                String.valueOf(conv2dinchannels),
                img_path,
                u_model_name,
                s_model_name,
                flag
        );
    }
}
