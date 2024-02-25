package net.itfeng.nettytcpdemoserver.handler;

import com.google.protobuf.InvalidProtocolBufferException;
import net.itfeng.nettytcpdemoserver.context.ClientStatusContext;
import net.itfeng.nettytcpdemoserver.context.MessageContext;
import net.itfeng.nettytcpdemoserver.context.TimeDiffContext;
import net.itfeng.nettytcpdemoserver.protocol.DataTransPackageOuterClass;
import net.itfeng.nettytcpdemoserver.util.TestDataTransTextLogUtil;
import org.springframework.stereotype.Service;


/**
 * 测试数据传输消息处理
 *
 * @author itfeng
 * @since 2024/1/8 15:46
 */
@Service
public class TestDataTransResultMessageHandler implements MyMessageHandler {

    private static final DataTransPackageOuterClass.DataType DATA_TYPE = DataTransPackageOuterClass.DataType.TEST_DATA_TRANS_RESULT;

    public void handle(byte[] messageByte) {
        net.itfeng.nettytcpdemoserver.protocol.TestDataTransOuterClass.TestDataTransResult testDataTransResult;
        try {
            testDataTransResult = net.itfeng.nettytcpdemoserver.protocol.TestDataTransOuterClass.TestDataTransResult.parseFrom(messageByte);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
        String sn = testDataTransResult.getClientId();
        // 记录客户端状态
        ClientStatusContext.putClient(sn);
        // 记录开始时间
        long startTimeMillis = testDataTransResult.getStartTimeMillis();
        long endTimeMillis = testDataTransResult.getReceivedTimeMillis();
        // 获取当前服务与客户端的时间差
        long diff = TimeDiffContext.getTimeDiff(sn);
        // 计算时延
        long latency = endTimeMillis - startTimeMillis + diff;
        Integer size = MessageContext.getLength(testDataTransResult.getClientId(), testDataTransResult.getMsgId());
        if (size == null) {
            size = 0;
        }
        TestDataTransTextLogUtil.log(sn + " " + size + " " + latency + " " + diff);

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
