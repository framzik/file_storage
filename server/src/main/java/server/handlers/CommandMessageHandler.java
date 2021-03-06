package server.handlers;

import com.google.gson.Gson;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import server.DbAuthService;
import server.FileInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import static command.Commands.*;

public class CommandMessageHandler extends SimpleChannelInboundHandler<Object> {
    private String userName = "";
    private byte[] fileBytes = new byte[0];
    private byte[] fromFile;
    public static final ConcurrentLinkedQueue<SocketChannel> channels = new ConcurrentLinkedQueue<>();
    private Path root = Paths.get(CLOUD);
    private DbAuthService dbService = new DbAuthService();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client connected: " + ctx.channel());
        channels.add((SocketChannel) ctx.channel());
    }

    /**
     * Обработка комманд от клиента
     *
     * @param ctx ChannelHandlerContext
     * @param obj Object
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object obj) throws Exception {
        byte[] incomingBytes = (byte[]) obj;
        String msg = new String(incomingBytes, StandardCharsets.UTF_8);

        if (msg.startsWith("/")) {
            if (msg.startsWith(CON)) {
                ctx.writeAndFlush((CON + OK).getBytes(StandardCharsets.UTF_8));
            } else if (msg.startsWith(END)) {
                dbService.disconnect();
                ctx.close();
            } else if (msg.startsWith(REG)) {
                String[] token = msg.split(" ", 4);
                if (token.length < 4) {
                    ctx.writeAndFlush((WRONG + "Введите логин, пароль и никнейм").getBytes(StandardCharsets.UTF_8));
                    return;
                }
                boolean regSuccess = dbService.registration(token[1], token[2], token[3]);
                if (regSuccess) {
                    ctx.writeAndFlush((REG + OK).getBytes(StandardCharsets.UTF_8));
                } else {
                    ctx.writeAndFlush((WRONG + "Логин занят").getBytes(StandardCharsets.UTF_8));
                }
            } else if (msg.startsWith(AUTH)) {
                String[] token = msg.split(" ", 3);
                if (token.length < 3) {
                    ctx.writeAndFlush((WRONG + "Введите логин и пароль").getBytes(StandardCharsets.UTF_8));
                    return;
                }

                String newNick = dbService.getNicknameByLoginAndPassword(token[1], token[2]);
                if (newNick != null) {
                    userName = newNick;
                    Path rootPath = Paths.get(CLOUD, userName);
                    createMainDir(ctx, rootPath);
                    ctx.writeAndFlush((ROOT + rootPath + " " + FILE_INFO + getFileInfoList(rootPath)).getBytes(StandardCharsets.UTF_8));
                } else ctx.writeAndFlush((WRONG + "Такой пользователь не найден").getBytes(StandardCharsets.UTF_8));
            } else if (msg.startsWith(TOUCH)) {
                createDir(ctx, msg);
            } else if (msg.startsWith(REMOVE)) {
                removeFile(ctx, msg);
            } else if (msg.startsWith(CD)) {
                navigation(ctx, msg);
            } else if (msg.startsWith(DOWNLOAD)) {
                String[] commands = msg.split(" ");
                Path srcPath = Paths.get(commands[1]);
                if (Files.exists(srcPath)) {
                    if (!Files.isDirectory(srcPath)) {
                        sendFile(ctx, srcPath);
                    } else {
                        ctx.writeAndFlush((WRONG + "This is Dir").getBytes(StandardCharsets.UTF_8));
                    }
                }
            } else if (msg.startsWith(UPLOAD)) {
                String response = msg.substring(UPLOAD.length());
                if (!response.equals("[]")) {
                    fileBytes = new byte[incomingBytes.length - UPLOAD.getBytes(StandardCharsets.UTF_8).length];
                    System.arraycopy(incomingBytes, UPLOAD.getBytes(StandardCharsets.UTF_8).length, fileBytes, 0, fileBytes.length);
                } else {
                    fromFile = " ".getBytes(StandardCharsets.UTF_8);
                    ctx.writeAndFlush((UPLOAD + OK).getBytes(StandardCharsets.UTF_8));
                }
            } else if (msg.startsWith(END_FILE)) {
                fromFile = fileBytes;
                Path dstPath = Paths.get(msg.substring(END_FILE.length()));
                Files.write(dstPath, fromFile);
                ctx.writeAndFlush((UPLOAD + OK + FILE_INFO + getFileInfoList(dstPath.getParent())).getBytes(StandardCharsets.UTF_8));
            }
        } else {
            byte[] newFileByte = new byte[fileBytes.length + incomingBytes.length];
            System.arraycopy(incomingBytes, 0, newFileByte, fileBytes.length, incomingBytes.length);
            System.arraycopy(fileBytes, 0, newFileByte, 0, fileBytes.length);
            fileBytes = newFileByte;
        }

    }

    /**
     * отправка файла
     *
     * @param ctx  ChannelHandlerContext
     * @param file Path
     * @throws IOException
     */
    private void sendFile(ChannelHandlerContext ctx, Path file) throws IOException {
        byte[] readFileBytes = Files.readAllBytes(file);
        byte[] downloadByte = DOWNLOAD.getBytes(StandardCharsets.UTF_8);
        byte[] msg = new byte[readFileBytes.length + downloadByte.length];

        System.arraycopy(downloadByte, 0, msg, 0, downloadByte.length);
        System.arraycopy(readFileBytes, 0, msg, downloadByte.length, readFileBytes.length);
        ctx.write(msg);
        ctx.flush();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ctx.writeAndFlush(END_FILE.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Навигация
     *
     * @param ctx ChannelHandlerContext
     * @param msg String
     * @throws IOException
     */
    private void navigation(ChannelHandlerContext ctx, String msg) throws IOException {
        if (msg.substring(CD.length()).startsWith(UP)) {
            String currPath = msg.split(" ")[2];
            Path parentPath = Paths.get(currPath).getParent();
            root = Paths.get("cloud", userName);
            if (Paths.get(currPath).equals(root)) {
                ctx.writeAndFlush((CD + Paths.get(currPath) + " " + getFileInfoList(Paths.get(currPath))).getBytes(StandardCharsets.UTF_8));
            } else
                ctx.writeAndFlush((CD + parentPath + " " + getFileInfoList(parentPath)).getBytes(StandardCharsets.UTF_8));
        } else {
            String currPath = msg.split(" ")[1];
            String fileName = msg.split(" ")[2];
            Path newPath = Paths.get(currPath, fileName);
            ctx.writeAndFlush((CD + newPath + " " + getFileInfoList(newPath)).getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Удаления файла, или всей директории
     *
     * @param ctx ChannelHandlerContext
     * @param msg String
     */
    private void removeFile(ChannelHandlerContext ctx, String msg) {
        String[] commands = msg.split(" ");
        String currPath = commands[1];
        String dirName = commands[2];
        Path newPath = Paths.get(currPath, dirName);
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
                ctx.writeAndFlush((REMOVE + OK + getFileInfoList(Paths.get(currPath))).getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            ctx.writeAndFlush((WRONG + newPath.getFileName() + " can't delete").getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Создание директории
     *
     * @param ctx ChannelHandlerContext
     * @param msg String
     * @throws IOException
     */
    private void createDir(ChannelHandlerContext ctx, String msg) throws IOException {
        String[] commands = msg.split(" ");
        String currPath = commands[1];
        String dirName = commands[2];
        Path newPath = Paths.get(currPath, dirName);
        if (!Files.exists(newPath)) {
            try {
                Files.createDirectory(newPath);
            } catch (IOException e) {
                ctx.writeAndFlush((WRONG + "Cannot create dir, change name").getBytes(StandardCharsets.UTF_8));
            }
        }
        ctx.writeAndFlush((TOUCH + OK + getFileInfoList(Paths.get(currPath))).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Получение строки, содержащей информацию о файлах
     *
     * @param dstPath Path
     * @return List<String>
     * @throws IOException
     */
    private List<String> getFileInfoList(Path dstPath) throws IOException {
        if (Files.isDirectory(dstPath)) {
            Gson g = new Gson();
            return Files.list(dstPath)
                    .map(FileInfo::new)
                    .map(g::toJson)
                    .map(f -> f + "/")
                    .collect(Collectors.toList());
        } else {
            List<String> fileInfoList = new ArrayList<>();
            fileInfoList.add(new FileInfo(dstPath) + "/");
            return fileInfoList;
        }
    }

    /**
     * Создание главной директории директории
     *
     * @param ctx ChannelHandlerContext
     * @param defaultRoot String
     * @throws IOException
     */
    private void createMainDir(ChannelHandlerContext ctx, Path defaultRoot) {
        if (!Files.exists(defaultRoot)) {
            root = Paths.get("cloud", userName);
            try {
                Files.createDirectories(root);
            } catch (IOException e) {
                ctx.writeAndFlush((WRONG + "can't create dir").getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client disconnected: " + ctx.channel());
    }

}
