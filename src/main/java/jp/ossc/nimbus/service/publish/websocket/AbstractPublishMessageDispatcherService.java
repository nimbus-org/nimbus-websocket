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

import java.util.Collections;
import java.util.Map;

import org.apache.commons.collections.map.HashedMap;

import jp.ossc.nimbus.core.ServiceBase;
import jp.ossc.nimbus.core.ServiceManagerFactory;
import jp.ossc.nimbus.core.ServiceName;
import jp.ossc.nimbus.service.publish.Message;
import jp.ossc.nimbus.service.publish.MessageException;
import jp.ossc.nimbus.service.publish.MessageListener;
import jp.ossc.nimbus.service.publish.MessageReceiver;
import jp.ossc.nimbus.service.publish.MessageSendException;
import jp.ossc.nimbus.service.websocket.SessionProperties;

/**
 * メッセージディスパッチャーサービス抽象クラス。 受信した配信メッセージを配信が必要なメッセージ送信対象に送信する。
 * <p>
 *
 * @author M.Ishida
 */
public abstract class AbstractPublishMessageDispatcherService extends ServiceBase implements MessageDispatcher, AbstractPublishMessageDispatcherServiceMBean {

    private static final long serialVersionUID = -9042584038972690681L;
    
    protected ServiceName messageReceiverServiceName;
    protected String sendErrorMessageId = DEFAULT_SEND_ERROR_MESSAGE_ID;
    
    protected MessageReceiver messageReceiver;
    
    protected Map senderListenerMap;
    
    protected long messageReceiveCount;

    public ServiceName getMessageReceiverServiceName() {
        return messageReceiverServiceName;
    }

    public void setMessageReceiverServiceName(ServiceName serviceName) {
        messageReceiverServiceName = serviceName;
    }

    public String getSendErrorMessageId() {
        return sendErrorMessageId;
    }

    public void setSendErrorMessageId(String messageId) {
        sendErrorMessageId = messageId;
    }

    public long getMessageReceiveCount() {
        return messageReceiveCount;
    }

    protected void preCreateService() throws Exception {
        super.preCreateService();
        senderListenerMap = Collections.synchronizedMap(new HashedMap());
    }
    
    protected void preStartService() throws Exception {
        super.preStartService();
        if (messageReceiverServiceName != null) {
            messageReceiver = (MessageReceiver) ServiceManagerFactory.getServiceObject(messageReceiverServiceName);
        }
        if(messageReceiver == null) {
            throw new IllegalArgumentException("MessageReceiver is null.");
        }
        messageReceiver.connect();
        messageReceiver.startReceive();
    }

    protected void preStopService() throws Exception {
        messageReceiver.stopReceive();
        messageReceiver.close();
    }
    
    public void addMessageSender(MessageSender sender) {
        if(!senderListenerMap.containsKey(sender.getSession().getId())) {
            MessageListener listener = new PublishMessageListener(sender);
            synchronized(senderListenerMap) {
                senderListenerMap.put(sender.getSession().getId(), listener);
            }
        }
    }

    public void removeMessageSender(MessageSender sender) {
        if(senderListenerMap.containsKey(sender.getSession().getId())) {
            MessageListener listener = (MessageListener)senderListenerMap.remove(sender.getSession().getId());
            try {
                messageReceiver.removeMessageListener(listener);
            } catch(MessageSendException e) {
            }
        }
    }

    public void addKey(Object key, MessageSender sender) throws Exception{
        if(senderListenerMap.containsKey(sender.getSession().getId())) {
            addKeyProcess(key, (MessageListener)senderListenerMap.get(sender.getSession().getId()));
        }
    }

    public void removeKey(Object key, MessageSender sender) throws Exception {
        if(senderListenerMap.containsKey(sender.getSession().getId())) {
            removeKeyProcess(key, (MessageListener)senderListenerMap.get(sender.getSession().getId()));
        }
    }
    
    /**
     * 配信メッセージに対するキーオブジェクトに対してメッセージ送信対象を追加する
     *
     * @param key 配信メッセージに対するキーオブジェクト
     * @param sender メッセージ送信対象
     */
    public abstract void addKeyProcess(Object key, MessageListener listener) throws Exception;

    /**
     * 配信メッセージに対するキーオブジェクトに対してメッセージ送信対象を削除する
     *
     * @param key 配信メッセージに対するキーオブジェクト
     * @param sender メッセージ送信対象
     */
    public abstract void removeKeyProcess(Object key, MessageListener listener) throws Exception;
    
    /**
     * MessageReceiverからMessageを受信するためのMessageListener
     * 
     * @author m-ishida
     *
     */
    protected class PublishMessageListener implements MessageListener {

        private MessageSender sender;
        
        protected PublishMessageListener(MessageSender sender) {
            this.sender = sender;
        }
        
        public void onMessage(Message message) {
            messageReceiveCount++;
            try {
                sender.sendMessage(message.getObject());
            } catch(MessageException e) {
                getLogger().write(sendErrorMessageId, SessionProperties.getSessionProperty(sender.getSession()), e);
            }
        }
    }
}
