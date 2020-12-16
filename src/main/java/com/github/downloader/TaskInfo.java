package com.github.downloader;

import lombok.Data;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @Author: renhongqiang
 * @Date: 2020/12/15 16:10
 **/
@Data
public class TaskInfo {
    private Integer id;
    private String downloadUrl;
    private String taskName;
    private String filename;
    private String savePath;
    private String checkMd5;
    private String actualMd5;
    private Integer status;
    private String msg;
    private AtomicLong downSize;
    private long totalSize;
}
