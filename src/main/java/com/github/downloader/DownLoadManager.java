package com.github.downloader;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Author: renhongqiang
 * @Date: 2020/11/17 15:29
 **/
@Slf4j
@Service
public class DownLoadManager {

    /**
     * 每个线程下载的字节数
     */
    @Value("${download.unit.size}")
    private Long unitSize = 512000000l;
    private ExecutorService taskExecutor = Executors.newCachedThreadPool();

    private CloseableHttpClient httpClient;

    private TaskDao taskDao;

    public DownLoadManager() {
    }

    public DownLoadManager(TaskDao taskDao) {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(100);
        httpClient = HttpClients.custom().setConnectionManager(cm).build();
        if (unitSize == null) {
            unitSize = 512000000l;
        }
        this.taskDao = taskDao;
    }



    /**
     * 启动多个线程下载文件
     */
    public void doDownload(TaskInfo taskInfo, Runnable callback) throws IOException {
        long start = System.currentTimeMillis();
        resetValue(taskInfo);
        log.info("文件名称：{}", taskInfo.getFilename());
        String localFilePath = this.createFile(taskInfo.getSavePath(), taskInfo.getFilename(), taskInfo.getTotalSize());
        Long threadCount = (taskInfo.getTotalSize() / unitSize) + (taskInfo.getTotalSize() % unitSize != 0 ? 1 : 0);
        log.info("切割分片大小：{}", threadCount);
        long offset = 0;
        CountDownLatch end = new CountDownLatch(threadCount.intValue());
        if (taskInfo.getTotalSize() <= unitSize) {// 如果远程文件尺寸小于等于unitSize
            DownloadThreadTask downloadThread = new DownloadThreadTask(taskInfo.getDownloadUrl(), localFilePath, offset, taskInfo.getTotalSize(), end, httpClient, taskDao);
            taskExecutor.execute(downloadThread);
        } else {// 如果远程文件尺寸大于unitSize
            for (int i = 1; i < threadCount; i++) {
                DownloadThreadTask downloadThread = new DownloadThreadTask(taskInfo.getDownloadUrl(), localFilePath, offset, unitSize, end, httpClient, taskDao);
                taskExecutor.execute(downloadThread);
                offset = offset + unitSize;
            }
            if (taskInfo.getTotalSize() % unitSize != 0) {// 如果不能整除，则需要再创建一个线程下载剩余字节
                DownloadThreadTask downloadThread = new DownloadThreadTask(taskInfo.getDownloadUrl(), localFilePath + taskInfo.getFilename(), offset, taskInfo.getTotalSize() - unitSize * (threadCount - 1), end, httpClient, taskDao);
                taskExecutor.execute(downloadThread);
            }
        }
        try {
            end.await();
        } catch (InterruptedException e) {
            log.error("DownLoadManager exception msg:{}", ExceptionUtils.getStackTrace(e));
            e.printStackTrace();
            taskInfo.setStatus(BizConstants.STATUS_FAILED);
            taskInfo.setMsg(e.getMessage());
        }
        boolean checkResult = checkMd5(localFilePath, taskInfo);
        log.info("{} 下载完成！文件是否完整:{}，耗时：{}", localFilePath, checkResult, (System.currentTimeMillis() - start));
        taskInfo.setStatus(checkResult ? BizConstants.STATUS_SUCCESS : BizConstants.STATUS_FAILED);
        CompletableFuture.runAsync(callback);
    }

    private void resetValue(TaskInfo taskInfo) {
        try {
            URL url = new URL(taskInfo.getDownloadUrl());
            String fullFileName = url.getFile();
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
            taskInfo.setFilename(fullFileName.substring(fullFileName.lastIndexOf("/") + 1).replace("%20", " "));
            taskInfo.setTotalSize(httpConnection.getHeaderFieldLong("Content-Length",-1));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 没有MD5值的，默认校验成功
     * @param localFilePath
     * @param taskInfo
     * @return
     */
    private boolean checkMd5(String localFilePath, TaskInfo taskInfo) {
        if (StringUtils.isEmpty(taskInfo.getCheckMd5())) {
            log.warn("given md5 is empty! do not check");
            return true;
        }
        try {
            InputStream is = new FileInputStream(new File(localFilePath));
            String md5Hex = DigestUtils.md5Hex(is);
            taskInfo.setActualMd5(md5Hex);
            log.info("file {}, calculate md5: {}, given md5: {}, check result: {}", localFilePath, md5Hex, taskInfo.getCheckMd5(), md5Hex.equals(taskInfo.getCheckMd5()));
            return md5Hex.equals(taskInfo.getCheckMd5());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 创建指定大小的文件
     */
    private String createFile(String dirPath, String fileName, long fileSize) throws IOException {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File newFile = new File(dir + File.separator + fileName);
        if (!newFile.exists()) {
            newFile.createNewFile();
        }
        RandomAccessFile raf = new RandomAccessFile(newFile, "rw");
        raf.setLength(fileSize);
        raf.close();
        return newFile.getAbsolutePath();
    }
}
