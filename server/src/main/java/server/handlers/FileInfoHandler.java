package server.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import server.FileInfo;

public class FileInfoHandler extends SimpleChannelInboundHandler<FileInfo> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, FileInfo fileInfo) throws Exception {
        System.out.println(fileInfo.toString());
    }
}
