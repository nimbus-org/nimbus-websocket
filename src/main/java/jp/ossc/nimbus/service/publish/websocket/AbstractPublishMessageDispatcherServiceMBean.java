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

import jp.ossc.nimbus.core.ServiceBaseMBean;
import jp.ossc.nimbus.core.ServiceName;

/**
 * {@link AbstractPublishMessageDispatcherService}のMBeanインタフェース
 * <p>
 *
 * @author M.Ishida
 */
public interface AbstractPublishMessageDispatcherServiceMBean extends ServiceBaseMBean {

    /**
     * データ送信時にエラーが発生した際に出力するメッセージIDのデフォルト値。
     * <p>
     */
    public static final String DEFAULT_SEND_ERROR_MESSAGE_ID = "WS___00006";

    /**
     * {@link jp.ossc.nimbus.service.publish.Message Message}を受信する{@link jp.ossc.nimbus.service.publish.MessageReceiver MessageReceiver}サービスのサービス名を取得する。<p>
     *
     * @return MessageReceiverサービスのサービス名
     */
    public ServiceName getMessageReceiverServiceName() ;

    /**
     * {@link jp.ossc.nimbus.service.publish.Message Message}を受信する{@link jp.ossc.nimbus.service.publish.MessageReceiver MessageReceiver}サービスのサービス名を設定する。<p>
     *
     * @param name MessageReceiverサービスのサービス名
     */
    public void setMessageReceiverServiceName(ServiceName serviceName) ;

    /**
     * データ送信時にエラーが発生した際に出力するメッセージIDを取得する。
     *
     * @return メッセージID
     */
    public String getSendErrorMessageId();

    /**
     * データ送信時にエラーが発生した際に出力するメッセージIDを設定する。デフォルトは
     * {@link #DEFAULT_SEND_ERROR_MESSAGE_ID} 。
     *
     * @param messageId メッセージID
     */
    public void setSendErrorMessageId(String messageId);
    
    /**
     * メッセージの受信件数を取得する。
     *
     * @return メッセージの受信件数
     */
    public long getMessageReceiveCount() ;
}
