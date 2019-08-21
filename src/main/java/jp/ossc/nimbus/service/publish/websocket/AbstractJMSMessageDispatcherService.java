/*
 * This software is distributed under following license based on modified BSD
 * style license.
 * ----------------------------------------------------------------------
 *
 * Copyright 2003 The Nimbus Project. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE NIMBUS PROJECT ``AS IS'' AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN
 * NO EVENT SHALL THE NIMBUS PROJECT OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of the Nimbus Project.
 */
package jp.ossc.nimbus.service.publish.websocket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.jms.Connection;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;

import jp.ossc.nimbus.core.ServiceBase;
import jp.ossc.nimbus.core.ServiceManagerFactory;
import jp.ossc.nimbus.core.ServiceName;
import jp.ossc.nimbus.service.jms.JMSMessageConsumerFactory;
import jp.ossc.nimbus.service.queue.DistributedQueueHandlerContainerService;
import jp.ossc.nimbus.service.queue.DistributedQueueSelector;
import jp.ossc.nimbus.service.queue.QueueHandler;
import jp.ossc.nimbus.service.queue.QueueHandlerContainer;
import jp.ossc.nimbus.service.websocket.ExceptionHandlerMappingService;

/**
 * JMSメッセージを受信するためのメッセージディスパッチャーサービス抽象クラス。
 * <p>
 *
 * @author M.Ishida
 */
public abstract class AbstractJMSMessageDispatcherService extends ServiceBase implements MessageDispatcher, MessageListener, AbstractJMSMessageDispatcherServiceMBean {

    private static final long serialVersionUID = -6407124068654707320L;
    
    protected ServiceName messageListenerQueueHandlerContainerServiceName;
    protected ServiceName messageListenerQueueSelectorServiceName;
    protected ServiceName messageSendQueueHandlerContainerServiceName;
    protected ServiceName messageSendQueueSelectorServiceName;
    protected ServiceName[] jmsMessageConsumerFactoryServiceNames;

    protected boolean isStartReceiveOnStart = true;
    protected int messageSendParameterRecycleListSize = DEFAULT_MESSAGE_SEND_PARAMETER_RECYCLE_LIST_SIZE;

    protected QueueHandlerContainer messageListenerQueueHandlerContainer;
    protected DistributedQueueHandlerContainerService messageSendQueueHandlerContainer;
    protected DistributedQueueSelector messageSendDistributedQueueSelector;
    protected JMSMessageConsumerFactory[] jmsMessageConsumerFactory;

    protected List messageSendParameterRecycleList;

    protected long messageReceiveCount;

    public ServiceName getMessageListenerQueueHandlerContainerServiceName() {
        return messageListenerQueueHandlerContainerServiceName;
    }

    public void setMessageListenerQueueHandlerContainerServiceName(ServiceName name) {
        this.messageListenerQueueHandlerContainerServiceName = name;
    }

    public ServiceName getMessageSendQueueHandlerContainerServiceName() {
        return messageSendQueueHandlerContainerServiceName;
    }

    public void setMessageSendQueueHandlerContainerServiceName(ServiceName name) {
        messageSendQueueHandlerContainerServiceName = name;
    }

    public ServiceName getMessageListenerQueueSelectorServiceName() {
        return messageListenerQueueSelectorServiceName;
    }

    public void setMessageListenerQueueSelectorServiceName(ServiceName name) {
        messageListenerQueueSelectorServiceName = name;
    }

    public ServiceName getMessageSendQueueSelectorServiceName() {
        return messageSendQueueSelectorServiceName;
    }

    public void setMessageSendQueueSelectorServiceName(ServiceName name) {
        messageSendQueueSelectorServiceName = name;
    }

    public int getMessageSendParameterRecycleListSize() {
        return messageSendParameterRecycleListSize;
    }

    public void setMessageSendParameterRecycleListSize(int size) {
        messageSendParameterRecycleListSize = size;
    }

    public ServiceName[] getJmsMessageConsumerFactoryServiceNames() {
        return jmsMessageConsumerFactoryServiceNames;
    }

    public void setJmsMessageConsumerFactoryServiceNames(ServiceName[] names) {
        this.jmsMessageConsumerFactoryServiceNames = names;
    }

    public boolean isStartReceiveOnStart() {
        return isStartReceiveOnStart;
    }

    public void setStartReceiveOnStart(boolean isStart) {
        isStartReceiveOnStart = isStart;
    }

    public long getMessageReceiveCount() {
        return messageReceiveCount;
    }

    protected void preCreateService() throws Exception {
        super.preCreateService();
        messageSendParameterRecycleList = Collections.synchronizedList(new ArrayList());
    }
    
    protected void preStartService() throws Exception {
        super.preStartService();
        if (messageListenerQueueHandlerContainerServiceName != null) {
            messageListenerQueueHandlerContainer = (QueueHandlerContainer) ServiceManagerFactory
                    .getServiceObject(messageListenerQueueHandlerContainerServiceName);
        } else {
            if (messageListenerQueueSelectorServiceName != null) {
                messageListenerQueueHandlerContainer = new DistributedQueueHandlerContainerService();
                ((DistributedQueueHandlerContainerService) messageListenerQueueHandlerContainer).create();
                ((DistributedQueueHandlerContainerService) messageListenerQueueHandlerContainer)
                .setDistributedQueueSelectorServiceName(messageListenerQueueSelectorServiceName);
                ((DistributedQueueHandlerContainerService) messageListenerQueueHandlerContainer).start();
            }
        }
        if (messageListenerQueueHandlerContainer != null) {
            messageListenerQueueHandlerContainer.setQueueHandler(new MessageListenerQueueHandler());
        }
        if (messageSendQueueHandlerContainerServiceName != null) {
            messageSendQueueHandlerContainer = (DistributedQueueHandlerContainerService) ServiceManagerFactory
                    .getServiceObject(messageSendQueueHandlerContainerServiceName);
        
        } else {
            if (messageSendQueueSelectorServiceName != null) {
                messageSendQueueHandlerContainer = new DistributedQueueHandlerContainerService();
                messageSendQueueHandlerContainer.create();
                messageSendQueueHandlerContainer
                        .setDistributedQueueSelectorServiceName(messageSendQueueSelectorServiceName);
                messageSendQueueHandlerContainer.start();
            }
        }
        messageSendDistributedQueueSelector = (DistributedQueueSelector) ServiceManagerFactory
                .getServiceObject(messageSendQueueHandlerContainer.getDistributedQueueSelectorServiceName());
        if (messageSendQueueHandlerContainer != null) {
            messageSendQueueHandlerContainer.setQueueHandler(new MessageSendQueueHandler());
            messageSendQueueHandlerContainer.accept();
        }
        if (jmsMessageConsumerFactoryServiceNames == null) {
            throw new IllegalArgumentException("JmsMessageConsumerFactoryServiceNames is null.");
        }
        jmsMessageConsumerFactory = new JMSMessageConsumerFactory[jmsMessageConsumerFactoryServiceNames.length];
        for (int i = 0; i < jmsMessageConsumerFactoryServiceNames.length; i++) {
            jmsMessageConsumerFactory[i] = (JMSMessageConsumerFactory) ServiceManagerFactory
                    .getServiceObject(jmsMessageConsumerFactoryServiceNames[i]);
        }
        
        for (int i = 0; i < jmsMessageConsumerFactory.length; i++) {
            final MessageConsumer consumer = jmsMessageConsumerFactory[i].createConsumer();
            consumer.setMessageListener(this);
            if (isStartReceiveOnStart) {
                final Connection con = jmsMessageConsumerFactory[i].getSessionFactory().getConnection();
                con.start();
            }
        }
    }
    
    protected void postStopService() throws Exception {
        stopReceive();
        if (messageListenerQueueHandlerContainer != null) {
            messageListenerQueueHandlerContainer.stop();
        }
        if (messageSendQueueHandlerContainer != null) {
            messageSendQueueHandlerContainer.stop();
        }
        super.postStopService();
    }
    
    protected void postDestroyService() throws Exception {
        messageListenerQueueHandlerContainer = null;
        messageSendQueueHandlerContainer = null;
        super.postDestroyService();
    }
    
    public void startReceive() throws Exception {
        for (int i = 0; i < jmsMessageConsumerFactory.length; i++) {
            final Connection con = jmsMessageConsumerFactory[i].getSessionFactory().getConnection();
            con.start();
        }
    }

    public void stopReceive() throws Exception {
        for (int i = 0; i < jmsMessageConsumerFactory.length; i++) {
            final Connection con = jmsMessageConsumerFactory[i].getSessionFactory().getConnection();
            con.stop();
        }
    }

    public void onMessage(Message msg) {
        messageReceiveCount++;
        onMessageProcess((Object) msg);
    }
    
    public void addMessageSender(MessageSender sender) {
        addMessageSenderProcess(sender);
    }

    public void removeMessageSender(MessageSender sender) {
        removeMessageSenderProcess(sender);
        if (messageSendDistributedQueueSelector instanceof SessionIdMessageSendDistributedQueueSelectorService) {
            ((SessionIdMessageSendDistributedQueueSelectorService) messageSendDistributedQueueSelector).remove(sender
                    .getSession().getId());
        }
    }

    public void addKey(Object key, MessageSender sender) {
        addKeyProcess(key, sender);
    }

    public void removeKey(Object key, MessageSender sender) {
        removeKeyProcess(key, sender);
    }

    /**
     * メッセージ送信対象を追加する
     *
     * @param sender メッセージ送信対象
     */
    public abstract void addMessageSenderProcess(MessageSender sender);

    /**
     * メッセージ送信対象を削除する
     *
     * @param sender メッセージ送信対象
     */
    public abstract void removeMessageSenderProcess(MessageSender sender);

    /**
     * 配信メッセージに対するキーオブジェクトに対してメッセージ送信対象を追加する
     *
     * @param key 配信メッセージに対するキーオブジェクト
     * @param sender メッセージ送信対象
     */
    public abstract void addKeyProcess(Object key, MessageSender sender);

    /**
     * 配信メッセージに対するキーオブジェクトに対してメッセージ送信対象を削除する
     *
     * @param key 配信メッセージに対するキーオブジェクト
     * @param sender メッセージ送信対象
     */
    public abstract void removeKeyProcess(Object key, MessageSender sender);

    /**
     * 配信メッセージの受信処理
     *
     * @param message メッセージ
     */
    protected void onMessageProcess(Object message) {
        if (message != null) {
            if (messageListenerQueueHandlerContainer == null) {
                Set<MessageSender> senders = getMessageSendTarget(message);
                if (senders != null && senders.size() != 0) {
                    sendMessageSenders(senders, message);
                }
            } else {
                messageListenerQueueHandlerContainer.push(message);
            }
        }
    }

    /**
     * 配信メッセージからメッセージ送信対象を取得する。
     *
     * @param message 配信メッセージ
     * @return メッセージ送信対象のList
     */
    protected abstract Set<MessageSender> getMessageSendTarget(Object message);

    /**
     * メッセージ送信対象にメッセージを送信する。 メッセージ送信のQueueHandlerContainerが存在する場合は、キューイングする。
     *
     * @param senders メッセージ送信対象
     * @param message 配信メッセージ
     */
    protected void sendMessageSenders(Set<MessageSender> senders, Object message) {
        for (MessageSender sender:senders) {
            if (messageSendQueueHandlerContainer == null) {
                sendMessage(sender, message);
            } else {
                messageSendQueueHandlerContainer.push(getSendParamObject(sender, message));
            }
        }
    }

    /**
     * メッセージ送信対象にメッセージを送信する。
     *
     * @param sender メッセージ送信対象
     * @param message 配信メッセージ
     */
    protected void sendMessage(MessageSender sender, Object message) {
        sender.sendMessage(message);
    }

    /**
     * メッセージ配信を受信するためのQueueHandlerクラス。
     *
     * @author m-ishida
     */
    protected class MessageListenerQueueHandler implements QueueHandler {
        public void handleDequeuedObject(Object message) throws Throwable {
            if (message == null) {
                return;
            }
            Set<MessageSender> senders = getMessageSendTarget(message);
            if (senders != null && senders.size() != 0) {
                sendMessageSenders(senders, message);
            }
        }

        public boolean handleError(Object obj, Throwable th) throws Throwable {
            return false;
        }

        public void handleRetryOver(Object obj, Throwable th) throws Throwable {
        }
    }

    /**
     * メッセージを送信するためのQueueHandlerクラス。
     *
     * @author m-ishida
     */
    protected class MessageSendQueueHandler implements QueueHandler {
        public void handleDequeuedObject(Object obj) throws Throwable {
            if (obj == null) {
                return;
            }
            MessageSendParameter param = (MessageSendParameter) obj;
            sendMessage(param.getSender(), param.getMessage());
            recycleSendParamObject(param);
        }

        public boolean handleError(Object obj, Throwable th) throws Throwable {
            MessageSendParameter param = (MessageSendParameter) obj;
            return param.getSender().getSession().isOpen();
        }

        public void handleRetryOver(Object obj, Throwable th) throws Throwable {
            MessageSendParameter param = (MessageSendParameter) obj;
            recycleSendParamObject(param);
        }
    }

    protected MessageSendParameter getSendParamObject(MessageSender sender, Object message) {
        MessageSendParameter obj = null;
        if(messageSendParameterRecycleList.isEmpty()){
            obj = new MessageSendParameter();
        }else{
            synchronized(messageSendParameterRecycleList){
                if(messageSendParameterRecycleList.isEmpty()){
                    obj = new MessageSendParameter();
                }else{
                    obj = (MessageSendParameter) messageSendParameterRecycleList.remove(0);
                }
            }
        }
        obj.setSender(sender);
        obj.setMessage(message);
        return obj;
    }

    protected void recycleSendParamObject(MessageSendParameter param) {
        if (messageSendParameterRecycleList.size() < messageSendParameterRecycleListSize) {
            param.clear();
            synchronized(messageSendParameterRecycleList){
                messageSendParameterRecycleList.add(param);
            }
        }
    }
}
