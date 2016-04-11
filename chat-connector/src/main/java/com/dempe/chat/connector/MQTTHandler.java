package com.dempe.chat.connector;

import com.dempe.chat.common.Utils;
import com.dempe.chat.common.messages.*;
import com.dempe.chat.connector.processor.ConnMessageProcessor;
import com.dempe.chat.connector.processor.DisconnectMessageProcessor;
import com.dempe.chat.connector.processor.SubscribeMessageProcessor;
import com.dempe.chat.connector.processor.UnSubscriptionMessageProcessor;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.dempe.chat.common.messages.AbstractMessage.*;

/**
 * Created with IntelliJ IDEA.
 * User: Dempe
 * Date: 2016/4/11
 * Time: 10:38
 * To change this template use File | Settings | File Templates.
 */
public class MQTTHandler extends ChannelHandlerAdapter {


    private static final Logger LOG = LoggerFactory.getLogger(MQTTHandler.class);


    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object message) {
        AbstractMessage msg = (AbstractMessage) message;
        LOG.info("Received a message of type {}", Utils.msgType2String(msg.getMessageType()));
        Channel channel = ctx.channel();
        try {
            switch (msg.getMessageType()) {
                case CONNECT:
                    // 处理connect msg
                    new ConnMessageProcessor().processConnect(channel, (ConnectMessage) msg);
                    break;
                case SUBSCRIBE:
                    new SubscribeMessageProcessor().processSubscribe(channel, (SubscribeMessage) msg);
                    break;
                case UNSUBSCRIBE:
                    new UnSubscriptionMessageProcessor().processUnsubscribe(channel, (UnsubscribeMessage) msg);
                    break;
                case PUBLISH:
                    break;
                case PUBREC:
                    break;
                case PUBCOMP:
                    break;
                case PUBREL:
                    break;
                case PUBACK:
                    break;
                case DISCONNECT:
                    new DisconnectMessageProcessor().processDisconnect(channel);
                    break;
                case PINGREQ:
                    PingRespMessage pingResp = new PingRespMessage();
                    ctx.writeAndFlush(pingResp);
                    break;
            }
        } catch (Exception ex) {
            LOG.error("Bad error in processing the message", ex);
        }
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof CorruptedFrameException) {
            //something goes bad with decoding
            LOG.warn("Error decoding a packet, probably a bad formatted packet, message: " + cause.getMessage());
        } else {
            LOG.error("Ugly error on networking");
        }
        ctx.close();
    }
}
