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

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;

import jp.ossc.nimbus.core.Service;
import jp.ossc.nimbus.core.ServiceManagerFactory;
import jp.ossc.nimbus.core.ServiceName;
import jp.ossc.nimbus.service.websocket.AbstractMessageHandlerFactoryService;
import jp.ossc.nimbus.service.websocket.ExceptionHandlerMappingService;
import jp.ossc.nimbus.service.websocket.SessionProperties;

/**
 * WebSocketを使ったメッセージハンドラファクトリサービス抽象クラス。
 * MessageDispatcherに対して、配信してほしいメッセージのキーを登録することで、対象メッセージをクライアントに配信する。
 * <p>
 *
 * @author M.Ishida
 */
public abstract class AbstractPublishMessageHandlerFactoryService extends AbstractMessageHandlerFactoryService implements AbstractPublishMessageHandlerFactoryServiceMBean {

    private static final long serialVersionUID = -6832747437783344769L;
    
    protected ServiceName messageDispatcherServiceName;
    protected ServiceName messageSendExceptionHandlerMappingServiceName;

    protected ExceptionHandlerMappingService messageSendExceptionHandler;
    protected MessageDispatcher dispatcher;

    protected long messageSendCount;
    
    public ServiceName getMessageDispatcherServiceName() {
        return messageDispatcherServiceName;
    }

    public void setMessageDispatcherServiceName(ServiceName name) {
        messageDispatcherServiceName = name;
    }

    public ServiceName getMessageSendExceptionHandlerMappingServiceName() {
        return messageSendExceptionHandlerMappingServiceName;
    }

    public void setMessageSendExceptionHandlerMappingServiceName(ServiceName serviceName) {
        messageSendExceptionHandlerMappingServiceName = serviceName;
    }

    public long getMessageSendCount() {
        return messageSendCount;
    }

    protected void preStartService() throws Exception {
        super.preStartService();
        if (messageDispatcherServiceName == null) {
            throw new IllegalArgumentException("MessageDispatcherServiceName is null.");
        }
        dispatcher = (MessageDispatcher) ServiceManagerFactory.getServiceObject(messageDispatcherServiceName);
        if (messageSendExceptionHandlerMappingServiceName != null) {
            messageSendExceptionHandler = (ExceptionHandlerMappingService) ServiceManagerFactory.getServiceObject(messageSendExceptionHandlerMappingServiceName);
        }
    }

    protected abstract Service createServiceInstance() throws Exception;

    /**
     * メッセージ送受信用MessageHandlerサービスクラス。
     * クライアントから受信したメッセージに応じて、MessageDispatcherに対して配信登録を行う。
     *
     * @author m-ishida
     *
     */
    public abstract class AbstractPublishMessageHandlerService extends AbstractMessageHandlerService implements MessageSender {

        private static final long serialVersionUID = 5053359912274095886L;

        protected void onOpenProcess(Session session, EndpointConfig config) throws Exception {
            dispatcher.addMessageSender(this);
        }

        protected void onCloseProcess(Session session, CloseReason closeReason) {
            dispatcher.removeMessageSender(this);
        }

        protected void onErrorProcess(Session session, Throwable thr) {
        }

        protected abstract void onMessageProcess(String message) throws Exception ;

        public void sendMessage(Object msg) {
            try {
                sendMessageProcess(msg);
                messageSendCount++;
                SessionProperties.getSessionProperty(session).addSendMessageCount();
            } catch (Exception e) {
                if (messageSendExceptionHandler != null) {
                    try {
                        messageSendExceptionHandler.handleException(session, e);
                    } catch (Throwable thr) {
                    }
                }
            }
        }

        public abstract void sendMessageProcess(Object msg) throws Exception;
        
        public Session getSession() {
            return session;
        }
    }
}
