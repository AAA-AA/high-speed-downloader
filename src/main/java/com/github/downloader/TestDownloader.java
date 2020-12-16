package com.github.downloader;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @Author: renhongqiang
 * @Date: 2020/12/16 17:32
 **/
@Slf4j
public class TestDownloader {
    public static void main(String[] args) throws IOException {
        //要下载的url
        String remoteFileUrl = "https://mirrors.tuna.tsinghua.edu.cn/apache/kafka/2.6.0/kafka-2.6.0-src.tgz";
        String localPath = "/Users/renhongqiang/Downloads";
        String md5 = "";
        String down = "https://desktop.docker.com/mac/stable/Docker.dmg";
        TaskInfo taskInfo1 = getTaskInfo(remoteFileUrl, localPath, md5);
        TaskInfo taskInfo2 = getTaskInfo(down, localPath, "");
        List<TaskInfo> list = Lists.newArrayList(taskInfo1, taskInfo2);
        DownLoadManager downLoadManager = new DownLoadManager(new DefaultTaskDao());

        list.parallelStream().forEach(e -> {
            //设置任务上下文，下载的子线程均可拿到该上下文，便于更新
            ContextHolder.setInfo(e);
            try {
                downLoadManager.doDownload(e, () -> update(e));
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });
    }

    private static TaskInfo getTaskInfo(String remoteFileUrl, String localPath, String md5) throws IOException {
        TaskInfo taskInfo = new TaskInfo();
        taskInfo.setDownloadUrl(remoteFileUrl);
        taskInfo.setSavePath(localPath);
        taskInfo.setCheckMd5(md5);
        taskInfo.setDownSize(new AtomicLong());
        return taskInfo;
    }

    /**
     * 可拓展为数据库更新
     * @param taskInfo
     */
    public static void update(TaskInfo taskInfo) {
        log.info("当前下载{},总文件大小：{}, 已下载文件大小：{}", String.format("%.2f%s", (taskInfo.getDownSize().get() * 100.0 / taskInfo.getTotalSize()), "%"), taskInfo.getTotalSize(), taskInfo.getDownSize().get());
    }
}
