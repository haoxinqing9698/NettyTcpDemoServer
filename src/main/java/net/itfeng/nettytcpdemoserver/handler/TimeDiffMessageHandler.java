package net.itfeng.nettytcpdemoserver.handler;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import net.itfeng.nettytcpdemoserver.protocol.DataTransPackageOuterClass;
import net.itfeng.nettytcpdemoserver.protocol.TestDataTransOuterClass;
import net.itfeng.nettytcpdemoserver.service.ClientTimeDiffCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * 测试数据传输消息处理
 *
 * @author itfeng
 * @since 2024/1/8 15:46
 */
@Slf4j
@Service
public class TimeDiffMessageHandler implements MyMessageHandler {

    private static final DataTransPackageOuterClass.DataType DATA_TYPE = DataTransPackageOuterClass.DataType.CLIENT_TIME_DIFF;

    @Autowired
    private ClientTimeDiffCalculator clientTimeDiffCalculator;


    public void handle(byte[] messageByte) {
        long now = System.currentTimeMillis();
        TestDataTransOuterClass.ClientTimeDiff clientTimeDiff;
        try {
            clientTimeDiff = TestDataTransOuterClass.ClientTimeDiff.parseFrom(messageByte);
        } catch (InvalidProtocolBufferException e) {
            log.error("解析数据传输消息失败", e);
            throw new RuntimeException(e);
        }
        TestDataTransOuterClass.ClientTimeDiff clientTimeDiffNew = TestDataTransOuterClass.ClientTimeDiff.newBuilder()
                .setClientId(clientTimeDiff.getClientId())
                .setMsgId(clientTimeDiff.getMsgId())
                .setStartTimeMillis(clientTimeDiff.getStartTimeMillis())
                .setTimeMillis(clientTimeDiff.getTimeMillis())
                .setEndTimeMillis(now)
                .build();
        // 交给下一个流程执行时间比较，计算时差
        clientTimeDiffCalculator.addTimeDiff(clientTimeDiffNew.getClientId(), clientTimeDiffNew);

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
