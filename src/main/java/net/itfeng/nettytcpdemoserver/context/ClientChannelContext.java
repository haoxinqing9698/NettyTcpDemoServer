package net.itfeng.nettytcpdemoserver.context;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.netty.channel.Channel;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 用于保存客户端ID与Channel的对应关系，这里仅做一个简单的对应关系，并没有做通道的断开重连维护
 *
 * @author itfeng
 * @since 2024/2/25 09:43
 */
public class ClientChannelContext {
    public static Cache<String, Channel> CLIENT_CHANNEL_CACHE = CacheBuilder
            .newBuilder().expireAfterWrite(15, TimeUnit.SECONDS).maximumSize(5000).build();

    public static void put(String clientId, Channel channel) {
        CLIENT_CHANNEL_CACHE.put(clientId, channel);
    }

    public static void remove(Channel channel) {
        Map<String, Channel> map = new HashMap<>(CLIENT_CHANNEL_CACHE.asMap());
        map.forEach((k, v) -> {
            if (channel == v) {
                CLIENT_CHANNEL_CACHE.invalidate(k);
            }
        });

    }

    public static Channel getChannel(String clientId) {
        return CLIENT_CHANNEL_CACHE.getIfPresent(clientId);
    }

    /**
     * 主动关闭所有在线通道
     */
    public static void closeAll() {
        CLIENT_CHANNEL_CACHE.asMap().forEach((k, v) -> v.close());
    }
}
