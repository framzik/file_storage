package server.handlers;

import com.google.gson.Gson;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import server.FileInfo;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
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
            ctx.writeAndFlush(ROOT + rootPath + " " + FILE_INFO + getFileInfoList(rootPath));
        } else if (msg.startsWith(TOUCH)) {
            createDir(ctx, msg);
        } else if (msg.startsWith(RM)) {
            removeFile(ctx, msg);
        } else if (msg.startsWith(CD)) {
            Path path = Path.of(msg.split(" ")[1]).getParent();
            ctx.writeAndFlush(CD + path + FILE_INFO + getFileInfoList(path));
        }

    }

    private void removeFile(ChannelHandlerContext ctx, String msg) {
        String[] commands = msg.split(" ");
        String currPath = commands[1];
        String dirName = commands[2];
        Path newPath = Path.of(currPath, dirName);
        try {
            if (Files.exists(newPath)) {
                if (!Files.isDirectory(newPath)) {
                    Files.delete(newPath);
                } else {
                    Files.walkFileTree(newPath, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Files.delete(file);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            Files.delete(dir);
                            return FileVisitResult.CONTINUE;
                        }
                    });
                }
                ctx.writeAndFlush(RM + OK + newPath.getFileName() + " removed" + FILE_INFO + getFileInfoList(newPath));
            }
        } catch (IOException e) {
            ctx.writeAndFlush(WRONG + newPath.getFileName() + " can't delete");
        }
    }

    private void createDir(ChannelHandlerContext ctx, String msg) throws IOException {
        String[] commands = msg.split(" ");
        String currPath = commands[1];
        String dirName = commands[2];
        Path newPath = Path.of(currPath, dirName);
        if (!Files.exists(newPath)) {
            try {
                Files.createDirectory(newPath);
            } catch (IOException e) {
                ctx.writeAndFlush(WRONG + "Cannot create dir, change name");
            }
        }
        ctx.writeAndFlush(TOUCH + OK + getFileInfoList(Path.of(currPath)));
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
                ctx.writeAndFlush(WRONG + "can't create dir");
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client disconnected: " + ctx.channel());
    }

}
