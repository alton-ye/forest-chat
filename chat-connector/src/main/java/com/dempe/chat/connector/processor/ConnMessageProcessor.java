package com.dempe.chat.connector.processor;

import com.dempe.chat.common.messages.AbstractMessage;
import com.dempe.chat.common.messages.ConnAckMessage;
import com.dempe.chat.common.messages.ConnectMessage;
import com.dempe.chat.common.messages.WillMessage;
import com.dempe.chat.connector.ConnectionDescriptor;
import com.dempe.chat.connector.NettyUtils;
import com.google.common.collect.Maps;
import io.netty.channel.Channel;
import org.apache.commons.lang3.StringUtils;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentMap;

/**
 * Created with IntelliJ IDEA.
 * User: Dempe
 * Date: 2016/4/11
 * Time: 15:57
 * To change this template use File | Settings | File Templates.
 */
public class ConnMessageProcessor extends MessageProcessor {

    private ConcurrentMap<String, Boolean> m_session = Maps.newConcurrentMap();

    public void processConnect(Channel channel, ConnectMessage msg) throws Exception {
        LOGGER.debug("CONNECT for client <{}>", msg.getClientID());
        // version not support
        if (msg.getProtocolVersion() != 3 && msg.getProtocolVersion() != 4) {
            connAck(channel, ConnAckMessage.UNNACEPTABLE_PROTOCOL_VERSION);
            channel.close();
            return;
        }

        // reject null clientID
        if (msg.getClientID() == null || msg.getClientID().length() == 0) {
            connAck(channel, ConnAckMessage.IDENTIFIER_REJECTED);
            return;
        }


        //handle user authentication
        if (msg.isUserFlag() && msg.isPasswordFlag() && StringUtils.isNotBlank(msg.getUsername())) {
            byte[] pwd = msg.getPassword();
            String username = msg.getUsername();
            // 登录逻辑
            LOGGER.info("username:{},pwd:{}", username, pwd);

        } else {
            connAck(channel, ConnAckMessage.BAD_USERNAME_OR_PASSWORD);
            return;
        }

        //if an old client with the same ID already exists close its session.
        if (m_clientIDs.containsKey(msg.getClientID())) {
            LOGGER.info("Found an existing connection with same client ID <{}>, forcing to close", msg.getClientID());
            //clean the subscriptions if the old used a cleanSession = true
            Channel oldChannel = m_clientIDs.get(msg.getClientID()).channel;
            oldChannel.close();
            LOGGER.debug("Existing connection with same client ID <{}>, forced to close", msg.getClientID());
        }

        ConnectionDescriptor connDescr = new ConnectionDescriptor(msg.getClientID(), channel, msg.isCleanSession());
        m_clientIDs.put(msg.getClientID(), connDescr);

        int keepAlive = msg.getKeepAlive();
        LOGGER.debug("Connect with keepAlive {} s", keepAlive);
        NettyUtils.keepAlive(channel, keepAlive);
        //session.attr(NettyUtils.ATTR_KEY_CLEANSESSION).set(msg.isCleanSession());
        NettyUtils.cleanSession(channel, msg.isCleanSession());
        //used to track the client in the subscription and publishing phases.
        //session.attr(NettyUtils.ATTR_KEY_CLIENTID).set(msg.getClientID());
        NettyUtils.clientID(channel, msg.getClientID());
        LOGGER.debug("Connect create session <{}>", channel);

        setIdleTime(channel.pipeline(), Math.round(keepAlive * 1.5f));

        //Handle will flag
        if (msg.isWillFlag()) {
            AbstractMessage.QOSType willQos = AbstractMessage.QOSType.valueOf(msg.getWillQos());
            byte[] willPayload = msg.getWillMessage();
            ByteBuffer bb = (ByteBuffer) ByteBuffer.allocate(willPayload.length).put(willPayload).flip();
            //save the will testament in the clientID store
            WillMessage will = new WillMessage(msg.getWillTopic(), bb, msg.isWillRetain(), willQos);
            m_willStore.put(msg.getClientID(), will);
        }

        ConnAckMessage okResp = new ConnAckMessage();
        okResp.setReturnCode(ConnAckMessage.CONNECTION_ACCEPTED);


        if (!msg.isCleanSession()) {
            //force the republish of stored QoS1 and QoS2
            republishStoredInSession();
        }
        LOGGER.info("CONNECT processed");
    }


    private void connAck(Channel channel, byte returnCode) {
        ConnAckMessage okResp = new ConnAckMessage();
        okResp.setReturnCode(returnCode);
        channel.writeAndFlush(okResp);
    }

    /**
     * 发送离线消息
     */
    private void republishStoredInSession() {

    }
}
