package com.github.downloader;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.CountDownLatch;

/**
 * @Author: renhongqiang
 * @Date: 2020/11/17 15:29
 **/
@Slf4j
public class DownloadThreadTask implements Runnable {

    private TaskDao taskDao;
    /**
     * 待下载的文件
     */
    private String url = null;

    /**
     * 本地文件名
     */
    private String fileName = null;

    /**
     * 偏移量
     */
    private long offset = 0;

    /**
     * 分配给本线程的下载字节数
     */
    private long length = 0;

    private CountDownLatch end;
    private CloseableHttpClient httpClient;
    private HttpContext context;

    /**
     * @param url    下载文件地址
     * @param file   另存文件名
     * @param offset 本线程下载偏移量
     * @param length 本线程下载长度
     */
    public DownloadThreadTask(String url, String file, long offset, long length, CountDownLatch end, CloseableHttpClient httpClient, TaskDao taskDao) {
        this.url = url;
        this.fileName = file;
        this.offset = offset;
        this.length = length;
        this.end = end;
        this.httpClient = httpClient;
        this.context = new BasicHttpContext();
        this.taskDao = taskDao;
    }

    public void run() {
        try {
            HttpGet httpGet = new HttpGet(this.url);
            httpGet.addHeader("Range", "bytes=" + this.offset + "-" + (this.offset + this.length - 1));
            CloseableHttpResponse response = httpClient.execute(httpGet, context);
            BufferedInputStream bis = new BufferedInputStream(response.getEntity().getContent());
            byte[] buff = new byte[1024];
            int bytesRead;
            File newFile = new File(fileName);
            RandomAccessFile raf = new RandomAccessFile(newFile, "rw");
            TaskInfo taskInfo = ContextHolder.get();
            while ((bytesRead = bis.read(buff, 0, buff.length)) != -1) {
                raf.seek(this.offset);
                raf.write(buff, 0, bytesRead);
                taskInfo.getDownSize().addAndGet(bytesRead);
                this.offset = this.offset + bytesRead;
                if (Randoms.isScoreAHit(1, 1000)) {
                    updateDown(taskInfo);
                }
            }
            raf.close();
            bis.close();
        } catch (ClientProtocolException e) {
            log.error("DownloadThread exception msg:{}", ExceptionUtils.getStackTrace(e));
        } catch (IOException e) {
            log.error("DownloadThread exception msg:{}", ExceptionUtils.getStackTrace(e));
        } finally {
            end.countDown();
            log.info("{} is go on!", end.getCount());
        }
    }

    private void updateDown(TaskInfo taskInfo) {
        this.taskDao.update(taskInfo);
        log.info("当前下载{},总文件大小：{}, 已下载文件大小：{}", String.format("%.2f%s", (taskInfo.getDownSize().get() * 100.0 / taskInfo.getTotalSize()), "%"), taskInfo.getTotalSize(), taskInfo.getDownSize().get());

    }
}
