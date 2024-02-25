package net.itfeng.nettytcpdemoserver.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.util.NettyRuntime;
import io.netty.util.internal.SystemPropertyUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.itfeng.nettytcpdemoserver.context.ClientChannelContext;
import net.itfeng.nettytcpdemoserver.handler.MyMessageHandler;
import net.itfeng.nettytcpdemoserver.protocol.DataTransPackageOuterClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * TCP长连服务端启动类
 *
 * @author itfeng
 * @since 2024/2/24 17:27
 */
@Slf4j
@Component
public class TcpServerBootstrap {
    private static final int BOSS_GROUP_THREAD_COUNT = 2;

    @Value("${netty.tcp.port}")
    private int port;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    @Getter
    private boolean epoll;

    @Autowired
    private List<MyMessageHandler> myMessageHandlers;


    /**
     * 启动服务端
     */
    @PostConstruct
    public void start() {
        init();
        start(port);
    }

    @PreDestroy
    public void stop() {
        ClientChannelContext.closeAll();
        if (this.bossGroup != null) {
            this.bossGroup.shutdownGracefully().syncUninterruptibly();
        }
        log.info("bossGroup  关闭");
        // 主线程等待worker关闭
        if (this.workerGroup != null) {
            this.workerGroup.shutdownGracefully().syncUninterruptibly();
        }
        log.info("workerGroup  关闭");
    }

    private void start(int port) {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(isEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) {
                        channel.pipeline()
                                .addLast(new ProtobufVarint32FrameDecoder())
                                .addLast(new ProtobufDecoder(DataTransPackageOuterClass.DataTransPackage.getDefaultInstance()))
                                .addLast(new ProtobufVarint32LengthFieldPrepender())
                                .addLast(new ProtobufEncoder())
                                .addLast("channelManageHandler", new SimpleChannelInboundHandler<DataTransPackageOuterClass.DataTransPackage>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext channelHandlerContext, DataTransPackageOuterClass.DataTransPackage dataTransPackage)  {
                                        // 维护客户端ID和channel的对应关系
                                        ClientChannelContext.put(dataTransPackage.getClientId(), channelHandlerContext.channel());
                                        channelHandlerContext.fireChannelRead(dataTransPackage);
                                    }
                                })
                                .addLast(new SimpleChannelInboundHandler<DataTransPackageOuterClass.DataTransPackage>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext channelHandlerContext, DataTransPackageOuterClass.DataTransPackage dataTransPackage) {
                                        if (dataTransPackage.getPbData().isEmpty()) {
                                            log.warn("接收到的消息没有消息体 ，clientId: {} , dataType: {} ", dataTransPackage.getClientId(), dataTransPackage.getDataType());
                                            return;
                                        }
                                        myMessageHandlers.stream()
                                                .filter(myMessageHandler -> myMessageHandler.isSupport(dataTransPackage.getDataType()))
                                                .forEach(myMessageHandler -> myMessageHandler.handle(dataTransPackage.getPbData().toByteArray()));
                                    }
                                });
                    }

                    @Override
                    public void channelInactive(ChannelHandlerContext ctx) {
                        // 客户端断开连接
                        ClientChannelContext.remove(ctx.channel());
                        log.info("客户端下线： channel: {}", ctx.channel().id());
                    }
                });
        try {
            serverBootstrap.bind(port).addListener(future -> {
                if (future.isSuccess()) {
                    log.info("ServerBootstrap 启动成功，port: {}", port);
                } else {
                    log.error("ServerBootstrap 启动失败，port: {}", port);
                }
            });
        } catch (Exception e) {
            log.error("TcpServerBootstrap 启动失败，port: {}", port);
            throw e;
        }

    }

    private void init() {
        int workerGroupThreadCount = 0;
        // 获取可用的CPU核数，如果系统有配置则使用系统配置的线程数，没有配置则使用默认的CPU核数
        int availableProcessors = NettyRuntime.availableProcessors();
        // 系统配置的netty线程数，没有配置则默认使用0
        int eventLoopThreads = SystemPropertyUtil.getInt("io.netty.eventLoopThreads", 0);
        log.info("  availableProcessors:{} io.netty.eventLoopThreads:{} ", availableProcessors, eventLoopThreads);
        if (eventLoopThreads > 0) {
            // 系统配置的线程数大于0，则使用系统配置的线程数
            workerGroupThreadCount = eventLoopThreads;
        } else {
            // 默认线程数为CPU核数*2
            workerGroupThreadCount = availableProcessors * 2;
        }

        // 判断是否支持epoll
        this.epoll = Epoll.isAvailable();
        if (epoll) {
            log.info("  epoll is available");
            try {
                // 确保 Netty 的 epoll 本地代码库被加载到 JVM 中，以便 Netty 可以使用 epoll 传输来提高网络通信的性能
                Class.forName("io.netty.channel.epoll.Native");
            } catch (Throwable error) {
                this.epoll = false;
                log.warn("can not load netty epoll, switch nio model.");
            }
        } else {
            log.info("  epoll is not available");
        }
        // 初始化eventLoop 线程组
        if (isEpoll()) {
            this.bossGroup = new EpollEventLoopGroup(BOSS_GROUP_THREAD_COUNT);
            this.workerGroup = new EpollEventLoopGroup(workerGroupThreadCount);
        } else {
            this.bossGroup = new NioEventLoopGroup(BOSS_GROUP_THREAD_COUNT);
            this.workerGroup = new NioEventLoopGroup(workerGroupThreadCount);
        }
    }
}
