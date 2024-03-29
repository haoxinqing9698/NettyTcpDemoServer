package net.itfeng.nettytcpdemoserver.util;

import java.util.UUID;

/**
 * 生成唯一ID
 *
 * @author itfeng
 * @since 2024/1/27 11:56
 */
public class MessageIdUtil {
    public static String getMessageId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
