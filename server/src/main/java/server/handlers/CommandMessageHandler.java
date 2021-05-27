package server.handlers;

import com.google.gson.Gson;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import server.FileInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import static command.Commands.*;

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
        if (msg.startsWith(END)) {
            ctx.close();
        } else if (msg.startsWith(AUTH)) {
            Path rootPath = Path.of("cloud", userName);
            createDirectory(ctx, rootPath);
            ctx.writeAndFlush(ROOT + rootPath+" "+ FILE_INFO + getFileInfoList(rootPath));
        } else if (msg.startsWith(TOUCH)) {
            String[] commands = msg.split(" ");
            String currPath = commands[1];
            String dirName = commands[2];
            Path newPath = Path.of(currPath, dirName);
            if (!Files.exists(newPath)) {
                try {
                    Files.createDirectory(newPath);
                } catch (IOException e) {
                    ctx.writeAndFlush("Cannot create dir, change name");
                }
                ctx.writeAndFlush(TOUCH + OK + getFileInfoList(newPath));
            }
        }
    }

    private List<String> getFileInfoList(Path dstPath) throws IOException {
        Gson g = new Gson();
        return Files.list(dstPath)
                .map(FileInfo::new)
                .map(g::toJson)
                .map(f -> f + "/")
                .collect(Collectors.toList());
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
