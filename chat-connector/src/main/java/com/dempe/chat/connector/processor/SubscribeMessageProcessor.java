package com.dempe.chat.connector.processor;

import com.dempe.chat.common.messages.AbstractMessage;
import com.dempe.chat.common.messages.SubAckMessage;
import com.dempe.chat.common.messages.SubscribeMessage;
import com.dempe.chat.connector.NettyUtils;
import com.dempe.chat.store.ClientSession;
import com.dempe.chat.store.subscriptions.Subscription;
import com.google.common.collect.Lists;
import io.netty.channel.Channel;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Dempe
 * Date: 2016/4/11
 * Time: 20:28
 * To change this template use File | Settings | File Templates.
 */
public class SubscribeMessageProcessor extends MessageProcessor {

    public void processSubscribe(Channel channel, SubscribeMessage msg) {
        String clientID = NettyUtils.clientID(channel);
        LOGGER.debug("SUBSCRIBE client <{}> packetID {}", clientID, msg.getMessageID());

        ClientSession clientSession = m_sessionsStore.sessionForClient(clientID);
        //ack the client
        SubAckMessage ackMessage = new SubAckMessage();
        ackMessage.setMessageID(msg.getMessageID());

        List<Subscription> newSubscriptions = Lists.newArrayList();
        for (SubscribeMessage.Couple req : msg.subscriptions()) {
            AbstractMessage.QOSType qos = AbstractMessage.QOSType.valueOf(req.qos);
            Subscription newSubscription = new Subscription(clientID, req.topicFilter, qos);
            boolean valid = clientSession.subscribe(req.topicFilter, newSubscription);
            ackMessage.addType(valid ? qos : AbstractMessage.QOSType.FAILURE);
            if (valid) {
                newSubscriptions.add(newSubscription);
            }

            // TODO 存储订阅关系
        }

        //save session, persist subscriptions from session
        LOGGER.debug("SUBACK for packetID {}", msg.getMessageID());

        channel.writeAndFlush(ackMessage);

    }


}
