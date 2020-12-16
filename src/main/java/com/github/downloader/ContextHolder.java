package com.github.downloader;

import com.alibaba.ttl.TransmittableThreadLocal;

/**每个任务可以有自己的context，即便在子线程中均可获取该context，使用线程池亦无妨
 * @Author: renhongqiang
 * @Date: 2020/12/15 19:31
 **/
public final class ContextHolder {
    public static TransmittableThreadLocal<TaskInfo> context = new TransmittableThreadLocal<>();

    public static TaskInfo get() {
        return context.get();
    }

    public static void setInfo(TaskInfo taskInfo) {
        context.set(taskInfo);
    }


}
