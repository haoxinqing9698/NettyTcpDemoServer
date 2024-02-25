package net.itfeng.nettytcpdemoserver.service;

import lombok.extern.slf4j.Slf4j;
import net.itfeng.nettytcpdemoserver.context.ClientStatusContext;
import net.itfeng.nettytcpdemoserver.thread.TestDataTransThreadManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * 计算服务端与客户端时间差
 *
 * @author itfeng
 * @since 2024/1/8 19:11
 */
@Slf4j
@Service
public class TestDataTransService {
    @Autowired
    private TestDataTransThreadManager testDataTransThreadManager;


    @Value("${tcp.push_msg_size_array}")
    private int[] pushMsgSize;
    @Value("${tcp.push_msg_count}")
    private int pushMsgCount;

    @Value("${tcp.push_msg_interval}")
    private int pushMsgInterval;

    @Scheduled(cron = "*/3 * * * * ?")
    public void run() {
        // 获取在线的客户端
        Set<String> temp = new HashSet<>(ClientStatusContext.getAllOnlineClient());
        temp.forEach(clientId -> {
            // 判断是否正在运行
            boolean isRunning = testDataTransThreadManager.checkRunning(clientId);
            if (isRunning) {
                isRunning = testDataTransThreadManager.updateLastTime(clientId);
            }
            // 未运行，则启动一个线程
            if (!isRunning) {
                testDataTransThreadManager.startThread(clientId, pushMsgSize, pushMsgCount, pushMsgInterval);
                log.info("启动一个线程，clientId:{}", clientId);
            } else {
                log.info("clientId:{} 线程正在运行", clientId);
            }
        });
    }


}
