package client;

import com.google.gson.Gson;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static command.Commands.*;


public class ClientHandler extends SimpleChannelInboundHandler<String> {
    private String userName = "framzik";
    private AnswerFromServer onMessageReceivedAnswer;

    public ClientHandler(AnswerFromServer onMessageReceivedAnswer) {
        this.onMessageReceivedAnswer = onMessageReceivedAnswer;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(AUTH);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        if (onMessageReceivedAnswer != null) {
            if (msg.startsWith(ROOT)) {
                String[] commands = msg.split(" ");
                String rootPath = commands[1];
                String jsonString = msg.substring(ROOT.length()).substring(rootPath.length()).substring(FILE_INFO.length()).trim();
                onMessageReceivedAnswer.answer(getFileInfos(jsonString));
            } else if (msg.startsWith(TOUCH + OK)) {
                String jsonString = msg.substring((TOUCH + OK).length()).trim();
                onMessageReceivedAnswer.answer(getFileInfos(jsonString));
            } else if (msg.startsWith(REMOVE + OK)) {
                String jsonString = msg.substring((REMOVE + OK).length()).trim();
                onMessageReceivedAnswer.answer(getFileInfos(jsonString));
            }else if (msg.startsWith(CD)) {
                String newPath = msg.split(" ")[1];
                String jsonString = msg.substring((CD).length()).substring(newPath.length()).trim();
                onMessageReceivedAnswer.answer(getFileInfos(jsonString));
            }
        }
    }

    private List<FileInfo> getFileInfos(String jsonString) {
        Gson g = new Gson();
        List<FileInfo> fileInfoList = new CopyOnWriteArrayList<>();
        String[] fileInfos = jsonString.split("/");
        FileInfo fileInfo;
        for (int i = 0; i < fileInfos.length - 1; i++) {
            fileInfo = g.fromJson(fileInfos[i].substring(1).trim(), FileInfo.class);
//            fileInfo.setFilename("s" + fileInfo.getFilename());
            fileInfoList.add(fileInfo);
        }
        if(fileInfoList.isEmpty()){
            fileInfoList.add(new FileInfo("Directory is Empty", FileInfo.FileType.EMPTY,0, LocalDateTime.now()));
        }
        return fileInfoList;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        cause.printStackTrace();
    }
}
