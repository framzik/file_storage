package server.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class RemoveHandler extends SimpleChannelInboundHandler<String> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String msg) throws Exception {
        if (msg.startsWith("rm ")){
            removeFile(msg);
        }
    }

    private void removeFile(String command)  {
//        String[] commands = command.split(" ");
//        Path newPath = Path.of(startPath, commands[1]);
//        try{
//            if (Files.exists(newPath)) {
//                if (!Files.isDirectory(newPath)) {
//                    Files.delete(newPath);
//                    sendMessage("file was deleted\n", selector, client);
//                } else {
//                    Files.walkFileTree(newPath, new SimpleFileVisitor<Path>() {
//                        @Override
//                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//                            Files.delete(file);
//                            return FileVisitResult.CONTINUE;
//                        }
//
//                        @Override
//                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
//                            Files.delete(dir);
//                            return FileVisitResult.CONTINUE;
//                        }
//                    });
//                    sendMessage("directory was deleted\n", selector, client);
//                }
//            } else sendMessage("directory/file doesn't exists\n", selector, client);
//        }catch (IOException e){
//            sendMessage("wrong command\n", selector, client);
//        }
    }
}
