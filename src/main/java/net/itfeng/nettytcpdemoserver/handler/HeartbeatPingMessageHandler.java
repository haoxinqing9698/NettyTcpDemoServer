package net.itfeng.nettytcpdemoserver.handler;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import net.itfeng.nettytcpdemoserver.context.ClientStatusContext;
import net.itfeng.nettytcpdemoserver.protocol.DataTransPackageOuterClass;
import net.itfeng.nettytcpdemoserver.protocol.TestDataTransOuterClass;
import net.itfeng.nettytcpdemoserver.service.MessageAsyncPublishService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


/**
 * 时间校准消息处理器
 *
 * @author itfeng
 * @since 2024/1/8 15:46
 */
@Slf4j
@Service
public class HeartbeatPingMessageHandler implements MyMessageHandler {

    private static final DataTransPackageOuterClass.DataType DATA_TYPE = DataTransPackageOuterClass.DataType.HEARTBEAT_PING;
    @Autowired
    private MessageAsyncPublishService messageAsyncPublishService;

    @Async("mqttMessageHandlerPool")
    public void handle(byte[] messageBytes) {
        TestDataTransOuterClass.HeartBeatPing heartBeatPing = null;
        try {
            heartBeatPing = TestDataTransOuterClass.HeartBeatPing.parseFrom(messageBytes);
        } catch (InvalidProtocolBufferException e) {
            log.error("解析心跳响应消息失败", e);
        }
        if (heartBeatPing == null) {
            return;
        }
        // 收到心跳记录状态
        ClientStatusContext.putClient(heartBeatPing.getClientId());
        log.info("收到心跳ping消息, msg_id:{}, client_id:{},sendTime:{}, timestamp:{}", heartBeatPing.getMsgId(), heartBeatPing.getClientId(), heartBeatPing.getStartTimeMillis(), System.currentTimeMillis());
        // 回复pong
        TestDataTransOuterClass.HeartBeatPong heartBeatPong = TestDataTransOuterClass.HeartBeatPong.newBuilder()
                .setMsgId(heartBeatPing.getMsgId())
                .setReceivedTimeMillis(System.currentTimeMillis())
                .build();
        messageAsyncPublishService.publish(DataTransPackageOuterClass.DataType.HEARTBEAT_PONG, heartBeatPong.toByteArray(), heartBeatPing.getClientId());

    }

    @Override
    public DataTransPackageOuterClass.DataType getDataType() {
        return DATA_TYPE;
    }


    @Override
    public boolean isSupport(DataTransPackageOuterClass.DataType dataType) {
        return dataType == DATA_TYPE;
    }
}
