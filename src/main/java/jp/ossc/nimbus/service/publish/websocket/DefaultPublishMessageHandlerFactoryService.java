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

import java.nio.ByteBuffer;

import jp.ossc.nimbus.core.Service;
import jp.ossc.nimbus.service.websocket.SessionProperties;

/**
 * WebSocketを使ったメッセージハンドラファクトリサービスデフォルト実装クラス。
 * MessageDispatcherに対して、配信してほしいメッセージのキーを登録することで、対象メッセージをクライアントに配信する。
 * <p>
 *
 * @author M.Ishida
 */
public class DefaultPublishMessageHandlerFactoryService extends AbstractPublishMessageHandlerFactoryService implements DefaultPublishMessageHandlerFactoryServiceMBean {
    
    protected String messageParseErrorId = "WS___00007";
    protected String messageKeyAddErrorId = "WS___00008";
    protected String messageKeyRemoveErrorId = "WS___00009";
    
    protected String messageEncoding = "UTF-8";
    protected String addKeyString = "ADD";
    protected String removeKeyString = "REMOVE";
    protected String messageSeparatorString = ",";
    
    public String getMessageParseErrorId() {
        return messageParseErrorId;
    }

    public void setMessageParseErrorId(String id) {
        messageParseErrorId = id;
    }

    public String getMessageKeyAddErrorId() {
        return messageKeyAddErrorId;
    }

    public void setMessageKeyAddErrorId(String id) {
        messageKeyAddErrorId = id;
    }

    public String getMessageKeyRemoveErrorId() {
        return messageKeyRemoveErrorId;
    }

    public void setMessageKeyRemoveErrorId(String id) {
        messageKeyRemoveErrorId = id;
    }

    public String getMessageEncoding() {
        return messageEncoding;
    }

    public void setMessageEncoding(String encoding) {
        messageEncoding = encoding;
    }
    
    public String getAddKeyString() {
        return addKeyString;
    }

    public void setAddKeyString(String string) {
        addKeyString = string;
    }

    public String getRemoveKeyString() {
        return removeKeyString;
    }

    public void setRemoveKeyString(String string) {
        removeKeyString = removeKeyString;
    }

    public String getMessageSeparatorString() {
        return messageSeparatorString;
    }

    public void setMessageSeparatorString(String string) {
        messageSeparatorString = string;
    }

    protected Service createServiceInstance() throws Exception {
        return new DefaultPublishMessageHandlerService();
    }
    
    public void startService() {
        if(addKeyString == null) {
            throw new IllegalArgumentException("AddKeyString is null.");
        }
        if(removeKeyString == null) {
            throw new IllegalArgumentException("RemoveKeyString is null.");
        }
        if(messageSeparatorString == null) {
            throw new IllegalArgumentException("MessageSeparatorString is null.");
        }
    }
    
    public class DefaultPublishMessageHandlerService extends AbstractPublishMessageHandlerService {
        
        protected void onMessageProcess(String message) {
            String[] args = message.split(messageSeparatorString);
            if (args.length < 2) {
                getLogger().write(messageParseErrorId, new Object[] {message, SessionProperties.getSessionProperty(session)});
            }
            if(addKeyString.equals(args[0])) {
                try {
                    if(args.length == 2) {
                        dispatcher.addKey(args[1], this);
                    } else {
                        String[] keys = new String[args.length - 2];
                        System.arraycopy(args, 2, keys, 0, keys.length);
                        dispatcher.addKey(new Object[] {args[1], keys}, this);
                    }
                } catch (Exception e) {
                    if(messageKeyAddErrorId != null) {
                        getLogger().write(messageKeyAddErrorId, new Object[] {message, SessionProperties.getSessionProperty(session)});
                    }
                }
            } else if(removeKeyString.equals(args[0])) {
                try {
                    if(args.length == 2) {
                        dispatcher.removeKey(args[1], this);
                    } else {
                        String[] keys = new String[args.length - 2];
                        System.arraycopy(args, 2, keys, 0, keys.length);
                        dispatcher.removeKey(new Object[] {args[1], keys}, this);
                    }
                } catch (Exception e) {
                    if(messageKeyRemoveErrorId != null) {
                        getLogger().write(messageKeyRemoveErrorId, new Object[] {message, SessionProperties.getSessionProperty(session)});
                    }
                }
            }
        }
        
        public void sendMessageProcess(Object msg) throws Exception {
            byte[] bytes = messageEncoding == null ? msg.toString().getBytes() : msg.toString().getBytes(messageEncoding);
            session.getBasicRemote().sendBinary(ByteBuffer.wrap(bytes));
        }
    }
}
