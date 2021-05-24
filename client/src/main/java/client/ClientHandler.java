package client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static command.Commands.AUTH;


public class ClientHandler extends SimpleChannelInboundHandler<String> {
    private String userName = "framzik";
    public static final Map<String, String> channels = new ConcurrentHashMap<>();
    public static final Map<String, List<FileInfo>> files = new ConcurrentHashMap<>();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(AUTH);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        if (msg.startsWith("/root: ")) {
            String[] commands = msg.split(" ");
            List<FileInfo> fileInfoList = new ArrayList<>();
            for (int i = 2; i < commands.length; i++) {
                fileInfoList.add(new FileInfo(Paths.get(commands[i])));
            }
            channels.put(userName, commands[1]);
            files.put(userName, fileInfoList);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        cause.printStackTrace();
    }

}
