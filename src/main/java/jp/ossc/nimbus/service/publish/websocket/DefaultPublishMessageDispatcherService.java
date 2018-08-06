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

import jp.ossc.nimbus.service.publish.MessageListener;

/**
 * デフォルトメッセージディスパッチャーサービスクラス。 MessageReceiverを使用し受信した配信メッセージを配信が必要なメッセージ送信対象に送信する。
 * 
 * @author m-ishida
 *
 */
public class DefaultPublishMessageDispatcherService extends AbstractPublishMessageDispatcherService {

    public void addKeyProcess(Object key, MessageListener listener) throws Exception {
        if(key instanceof String) {
            messageReceiver.addSubject(listener, (String)key);
            return;
        } else if(key.getClass().isArray()) {
            Object[] keys = (Object[])key;
            if(keys.length == 1 && keys[0] instanceof String) {
                messageReceiver.addSubject(listener, (String)keys[0]);
                return;
            } else if(keys.length == 2 && keys[0] instanceof String && keys[1] instanceof String[]) {
                messageReceiver.addSubject(listener, (String)keys[0], (String[])keys[1]);
                return;
            }
        }
        throw new IllegalArgumentException("key object type is not support. key=" + key);
    }

    public void removeKeyProcess(Object key, MessageListener listener) throws Exception {
        if(key instanceof String) {
            messageReceiver.removeSubject(listener, (String)key);
            return;
        } else if(key.getClass().isArray()) {
            Object[] keys = (Object[])key;
            if(keys.length == 1 && keys[0] instanceof String) {
                messageReceiver.removeSubject(listener, (String)keys[0]);
                return;
            } else if(keys.length == 2 && keys[0] instanceof String && keys[1] instanceof String[]) {
                messageReceiver.removeSubject(listener, (String)keys[0], (String[])keys[1]);
                return;
            }
        }
        throw new IllegalArgumentException("key object type is not support. key=" + key);
    }
    
}
