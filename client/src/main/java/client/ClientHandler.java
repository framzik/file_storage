package client;

import com.google.gson.Gson;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static command.Commands.*;


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
        if (msg.startsWith(ROOT)) {
            String[] commands = msg.split(" ");
            channels.put(userName, commands[1]);
            String jsonString = msg.substring(ROOT.length()).substring(commands[1].length()).substring(FILE_INFO.length()).trim();
            files.put(userName, getFileInfos(jsonString));
        }
    }

    private List<FileInfo> getFileInfos(String jsonString) {
        Gson g = new Gson();
        List<FileInfo> fileInfoList = new CopyOnWriteArrayList<>();
        String[] fileInfos = jsonString.split("/");
        FileInfo fileInfo;
        for (int i = 0; i < fileInfos.length - 1; i++) {
            fileInfo = g.fromJson(fileInfos[i].substring(1).trim(), FileInfo.class);
//            fileInfo.setFilename("s"+fileInfo.getFilename());
            fileInfoList.add(fileInfo);
        }
        System.out.println(fileInfoList);
        return fileInfoList;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        cause.printStackTrace();
    }
}
