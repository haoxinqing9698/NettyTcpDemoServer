package net.itfeng.nettytcpdemoserver.context;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 时间校准差值数据上下文
 * 用于记录当前服务与各个客户端的时间校准差
 * 该差值为：服务器时间-客户端时间
 *
 * @author itfeng
 * @since 2024/1/8 15:57
 */
public class TimeDiffContext {

    public static Cache<String, Long> TIME_DIFF_CACHE = CacheBuilder
            .newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).maximumSize(5000).build();

    public static long getTimeDiff(String sn) {
        Long diff = TIME_DIFF_CACHE.getIfPresent(sn);
        return Objects.requireNonNullElse(diff,0L);
    }

    public static void putTimeDiff(String sn, long timeDiff) {
        TIME_DIFF_CACHE.put(sn, timeDiff);
    }
}
