package server.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import java.nio.charset.StandardCharsets;

import static command.Commands.DOWNLOAD;

public class FileOutputHandler extends ChannelOutboundHandlerAdapter {
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        String answer = (String) msg;
        if(answer.startsWith(DOWNLOAD)){
            answer.substring(DOWNLOAD.length()).trim().getBytes(StandardCharsets.UTF_8);

        }
    }
}
