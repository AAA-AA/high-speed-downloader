package com.github.downloader;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: renhongqiang
 * @Date: 2020/12/16 17:35
 **/
@Slf4j
public class DefaultTaskDao implements TaskDao{
    @Override
    public void update(TaskInfo taskInfo) {
        log.info("update task info!");
    }
}
