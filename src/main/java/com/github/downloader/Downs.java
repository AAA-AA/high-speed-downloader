package com.github.downloader;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @Author: renhongqiang
 * @Date: 2020/12/15 19:14
 **/
@Slf4j
public final class Downs {
    /**
     * 获取远程文件尺寸
     */
    public static long getRemoteFileSize(String remoteFileUrl) throws IOException {
        HttpURLConnection httpConnection = (HttpURLConnection) new URL(remoteFileUrl).openConnection();
        //使用HEAD方法
        return httpConnection.getHeaderFieldLong("Content-Length",-1);
    }

    public static String findShortName(String remoteFileUrl) {
        try {
            String fullFileName = new URL(remoteFileUrl).getFile();
            return fullFileName.substring(fullFileName.lastIndexOf("/") + 1).replace("%20", " ");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }


}
