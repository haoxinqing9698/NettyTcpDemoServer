package net.itfeng.nettytcpdemoserver.handler;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import net.itfeng.nettytcpdemoserver.context.ClientStatusContext;
import net.itfeng.nettytcpdemoserver.protocol.DataTransPackageOuterClass;
import net.itfeng.nettytcpdemoserver.protocol.TestDataTransOuterClass;
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
public class ClientOnlineMessageHandler implements MyMessageHandler {


    private static final DataTransPackageOuterClass.DataType DATA_TYPE = DataTransPackageOuterClass.DataType.ONLINE_EVENT;

    @Async("mqttMessageHandlerPool")
    public void handle(byte[] messageBytes) {
        TestDataTransOuterClass.OnlineEvent onlineEvent = null;
        try {
            onlineEvent = TestDataTransOuterClass.OnlineEvent.parseFrom(messageBytes);
        } catch (InvalidProtocolBufferException e) {
            log.error("解析心跳响应消息失败", e);
        }
        if (onlineEvent == null) {
            return;
        }
        ClientStatusContext.putClient(onlineEvent.getClientId());
        log.info("收到上线 OnlineEvent 消息, msg_id:{}, clinet_id:{}", onlineEvent.getMsgId(), onlineEvent.getClientId());
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
