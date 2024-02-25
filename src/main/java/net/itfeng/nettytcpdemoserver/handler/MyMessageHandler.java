package net.itfeng.nettytcpdemoserver.handler;

import net.itfeng.nettytcpdemoserver.protocol.DataTransPackageOuterClass;

/**
 * 消息处理接口
 *
 * @author itfeng
 * @since 2024/1/26 17:41
 */
public interface MyMessageHandler {
    void handle(byte[] messageBytes);

    DataTransPackageOuterClass.DataType getDataType();

    /**
     * 根据业务pb数据类型判断该类型的数据是否由当前的handler处理
     *
     * @param dataType 业务数据类型
     * @return 是否需要使用当前处理器处理
     */
    boolean isSupport(DataTransPackageOuterClass.DataType dataType);
}
