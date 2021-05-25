package client;

import com.google.gson.Gson;
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
            channels.put(userName, commands[1]);
        }
        if(msg.startsWith("/file_nfo ")){
           String jsonString= msg.substring("/file_nfo ".length());
           getFileInfos(jsonString);
        }
    }

    private List<FileInfo> getFileInfos(String jsonString) {
        Gson g = new Gson();
        String[] fileInfos = jsonString.split("$$");
        for (int i =0; i < fileInfos.length; i++) {
            FileInfo fileInfo = g.fromJson(fileInfos[i].replace("$$",""), FileInfo.class);
            System.out.println(fileInfo);
        }
        return null;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        cause.printStackTrace();
    }

}
