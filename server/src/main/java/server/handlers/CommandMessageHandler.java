package server.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import static command.Commands.AUTH;
import static command.Commands.END;

public class CommandMessageHandler extends SimpleChannelInboundHandler<String> {
    String userName = "framzik";

    public static final ConcurrentLinkedQueue<SocketChannel> channels = new ConcurrentLinkedQueue<>();
    private Path root = Path.of("cloud");

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
            System.out.println("Client connected: " + ctx.channel());
            channels.add((SocketChannel) ctx.channel());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        System.out.println("Message from client: " + msg);
        if( msg.startsWith(END)){
            ctx.close();
        }else if(msg.startsWith(AUTH)){
            Path rootPath = Path.of("cloud", userName);
            createDirectory(ctx, rootPath);
            ctx.writeAndFlush("/root: " + rootPath + " " + Files.list(rootPath).collect(Collectors.toList()));
        }
    }

    private void createDirectory(ChannelHandlerContext ctx, Path defaultRoot) {
        if (!Files.exists(defaultRoot)) {
            root = Path.of("cloud", userName);
            try {
                Files.createDirectories(root);
            } catch (IOException e) {
                ctx.writeAndFlush("wrong command");
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client disconnected: " + ctx.channel());

    }

}
