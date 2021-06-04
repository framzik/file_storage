package client;

import com.google.gson.Gson;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static client.Controller.fromFile;
import static command.Commands.*;


public class ClientHandler extends SimpleChannelInboundHandler<Object> {
    private AnswerFromServer onMessageReceivedAnswer;
    private byte[] fileBytes = new byte[0];
    public static boolean isRegistered = false;
    public static String wrong = "";

    public ClientHandler(AnswerFromServer onMessageReceivedAnswer) {
        this.onMessageReceivedAnswer = onMessageReceivedAnswer;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(CON.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Метод обрабатывает команды,которые приходят от сервера
     * @param ctx
     * @param obj
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object obj) throws Exception {
        if (onMessageReceivedAnswer != null) {
            byte[] incomingBytes = (byte[]) obj;
            String msg = new String(incomingBytes, StandardCharsets.UTF_8);
            if (msg.startsWith("/")) {
                if(msg.startsWith(WRONG)){
                    wrong = msg.substring(WRONG.length());
                    onMessageReceivedAnswer.answer(getFileInfos(""));
                }
                if (msg.startsWith(CON + OK)) {
                    onMessageReceivedAnswer.answer(getFileInfos(""));
                } else if (msg.startsWith(REG + OK)) {
                    isRegistered = true;
                    onMessageReceivedAnswer.answer(getFileInfos(""));
                } else if (msg.startsWith(ROOT)) {
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
                } else if (msg.startsWith(CD)) {
                    String newPath = msg.split(" ")[1];
                    String jsonString = msg.substring((CD).length()).substring(newPath.length()).trim();
                    onMessageReceivedAnswer.answer(getFileInfos(jsonString));
                } else if (msg.startsWith(DOWNLOAD)) {
                    String response = msg.substring(DOWNLOAD.length());
                    if (!response.equals("[]")) {
                        fileBytes = new byte[incomingBytes.length - DOWNLOAD.getBytes(StandardCharsets.UTF_8).length];
                        System.arraycopy(incomingBytes, DOWNLOAD.getBytes(StandardCharsets.UTF_8).length, fileBytes, 0, fileBytes.length);
                    } else {
                        fromFile = " ".getBytes(StandardCharsets.UTF_8);
                        onMessageReceivedAnswer.answer(getFileInfos(""));
                    }
                } else if (msg.equals(END_FILE)) {
                    fromFile = fileBytes;
                    onMessageReceivedAnswer.answer(getFileInfos(""));
                } else if (msg.startsWith(UPLOAD + OK)) {
                    String jsonString = msg.substring((UPLOAD + OK + FILE_INFO).length()).trim();
                    onMessageReceivedAnswer.answer(getFileInfos(jsonString));
                }
            } else {
                byte[] newFileByte = new byte[fileBytes.length + incomingBytes.length];
                System.arraycopy(incomingBytes, 0, newFileByte, fileBytes.length, incomingBytes.length);
                System.arraycopy(fileBytes, 0, newFileByte, 0, fileBytes.length);
                fileBytes = newFileByte;
            }
        }
    }

    /**
     * Преобразуем информацию о файлах с сервера в объект FileInfo
     * @param jsonString строка с сервера с информацией о файлах
     * @return
     */
    private List<FileInfo> getFileInfos(String jsonString) {
        Gson g = new Gson();
        List<FileInfo> fileInfoList = new CopyOnWriteArrayList<>();
        String[] fileInfos = jsonString.split("/");
        FileInfo fileInfo;
        for (int i = 0; i < fileInfos.length - 1; i++) {
            fileInfo = g.fromJson(fileInfos[i].substring(1).trim(), FileInfo.class);
            fileInfoList.add(fileInfo);
        }
        if (fileInfoList.isEmpty()) {
            fileInfoList.add(new FileInfo("Directory is Empty", FileInfo.FileType.EMPTY, 0, LocalDateTime.now()));
        }
        return fileInfoList;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        cause.printStackTrace();
    }
}
