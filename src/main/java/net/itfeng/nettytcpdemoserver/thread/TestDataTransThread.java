package net.itfeng.nettytcpdemoserver.thread;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.itfeng.nettytcpdemoserver.context.ClientStatusContext;
import net.itfeng.nettytcpdemoserver.context.MessageContext;
import net.itfeng.nettytcpdemoserver.protocol.DataTransPackageOuterClass;
import net.itfeng.nettytcpdemoserver.service.MessageAsyncPublishService;
import net.itfeng.nettytcpdemoserver.util.MockTestDataUtil;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 测试数据传输线程类
 *
 * @author itfeng
 * @since 2024/1/28 14:30
 */
@Slf4j
public class TestDataTransThread implements Runnable {
    private static final long MAX_EXPIRE_TIME = 10 * 1000;

    private static boolean systemRunningStatus = true;

    public static void stopAll() {
        systemRunningStatus = false;
        try {
            TimeUnit.MILLISECONDS.sleep(1000L);
        } catch (InterruptedException e) {
            log.error("停止所有数据发送线程", e);
        }
    }

    @Getter
    private boolean stopped = false;

    private final AtomicInteger i = new AtomicInteger(1);

    private long lastUpdateTime;

    /**
     * 最后更新时间，用于判断是否需要继续下发数据
     * 1. 客户端状态为在线状态，则每3秒更新一次该时间
     * 2. 客户端状态为离线状态，则停止更新该时间
     * 3. 停止更新超过 MAX_EXPIRE_TIME 秒后，停止下发数据
     */
    public void updateLastUpdateTime() {
        lastUpdateTime = System.currentTimeMillis();
    }

    private final String clientId;
    private final int[] pushMsgSize;

    private final int pushMsgCount;

    private final int pushMsgInterval;

    private final MessageAsyncPublishService messageAsyncPublishService;

    public TestDataTransThread(String clientId, int[] pushMsgSize, int pushMsgCount, int pushMsgInterval, MessageAsyncPublishService messageAsyncPublishService) {
        this.clientId = clientId;
        this.pushMsgSize = pushMsgSize;
        this.pushMsgCount = pushMsgCount;
        this.pushMsgInterval = pushMsgInterval;
        this.lastUpdateTime = System.currentTimeMillis();
        this.messageAsyncPublishService = messageAsyncPublishService;
    }

    @Override
    public void run() {
        while (System.currentTimeMillis() - lastUpdateTime < MAX_EXPIRE_TIME && systemRunningStatus && ClientStatusContext.isOnline(clientId)) {
            int ivalue = i.incrementAndGet();
            int index = ivalue / pushMsgCount;
            // 如果计划数据还没发送完毕
            if (index < pushMsgSize.length) {
                int size = pushMsgSize[index];
                // 构造数据
                net.itfeng.nettytcpdemoserver.protocol.TestDataTransOuterClass.TestDataTrans testDataTrans = MockTestDataUtil.buildTestDataTransObject(size, clientId);
                // 发送数据
                messageAsyncPublishService.publish(DataTransPackageOuterClass.DataType.TEST_DATA_TRANS, testDataTrans.toByteArray(), clientId);
                MessageContext.putClient(testDataTrans.getClientId(), testDataTrans.getMsgId(), testDataTrans.getSerializedSize());
            }
            try {
                TimeUnit.MILLISECONDS.sleep(pushMsgInterval);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        log.info("{} 发送数据线程终止  *************************", clientId);
        stopped = true;
    }
}
