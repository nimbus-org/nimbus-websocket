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
package jp.ossc.nimbus.service.websocket;

import jp.ossc.nimbus.core.ServiceBaseMBean;
import jp.ossc.nimbus.core.ServiceName;

/**
 * {@link AbstractMessageHandlerFactoryService}のMBeanインタフェース。
 * <p>
 *
 * @author M.Ishida
 */
public interface WebSocketAccessJournalServiceMBean extends ServiceBaseMBean {

    /**
     * リクエストジャーナルのルートステップのキーのデフォルト値。
     * <p>
     */
    public static final String DEFAULT_ACCESS_JOURNAL_KEY = "Access";

    /**
     * ジャーナルのIDのキーのデフォルト値。
     * <p>
     */
    public static final String DEFAULT_ID_JOURNAL_KEY = "Id";

    /**
     * ジャーナルのチケットのキーのデフォルト値。
     * <p>
     */
    public static final String DEFAULT_TICKET_JOURNAL_KEY = "Ticket";

    /**
     * ジャーナルのWebSocketセッションIDのキーのデフォルト値。
     * <p>
     */
    public static final String DEFAULT_WEBSOCKET_SESSION_ID_JOURNAL_KEY = "WebSocketSessionId";

    /**
     * ジャーナルのHttpセッションIDのキーのデフォルト値。
     * <p>
     */
    public static final String DEFAULT_HTTP_SESSION_ID_JOURNAL_KEY = "HttpSessionId";

    /**
     * ジャーナルのパスのキーのデフォルト値。
     * <p>
     */
    public static final String DEFAULT_PATH_JOURNAL_KEY = "Path";

    /**
     * ジャーナルのIPのキーのデフォルト値。
     * <p>
     */
    public static final String DEFAULT_IP_JOURNAL_KEY = "Ip";

    /**
     * ジャーナルのポートのキーのデフォルト値。
     * <p>
     */
    public static final String DEFAULT_PORT_JOURNAL_KEY = "Port";

    /**
     * ジャーナルのリクエストヘッダのキーのデフォルト値。
     * <p>
     */
    public static final String DEFAULT_HEADER_JOURNAL_KEY = "Header";

    /**
     * ジャーナルのリクエストパラメータのキーのデフォルト値。
     * <p>
     */
    public static final String DEFAULT_PARAMETER_JOURNAL_KEY = "Parameter";

    /**
     * ジャーナルのリクエストメッセージのキーのデフォルト値。
     * <p>
     */
    public static final String DEFAULT_REQUEST_MESSAGE_JOURNAL_KEY = "Message";

    /**
     * ジャーナルのCloseReasonのキーのデフォルト値。
     * <p>
     */
    public static final String DEFAULT_CLOSE_REASON_JOURNAL_KEY = "CloseReason";

    /**
     * ジャーナルのAuthResultのキーのデフォルト値。
     * <p>
     */
    public static final String DEFAULT_AUTH_RESULT_JOURNAL_KEY = "AuthResult";

    /**
     * ジャーナルの例外のキーのデフォルト値。
     * <p>
     */
    public static final String DEFAULT_EXCEPTION_JOURNAL_KEY = "Exception";

    /**
     * ジャーナルのサービス名を取得する。
     * <p>
     *
     * @return ジャーナルのサービス名
     */
    public ServiceName getJournalServiceName();

    /**
     * ジャーナルのサービス名を設定する。
     * <p>
     *
     * @param name ジャーナルのサービス名
     */
    public void setJournalServiceName(ServiceName name);

    /**
     * ジャーナリングする際のEditorFinderサービスのサービス名を設定する。
     * <p>
     *
     * @param name EditorFinderサービスのサービス名
     */
    public void setEditorFinderServiceName(ServiceName name);

    /**
     * ジャーナリングする際のEditorFinderサービスのサービス名を取得する。
     * <p>
     *
     * @return EditorFinderサービスのサービス名
     */
    public ServiceName getEditorFinderServiceName();

    /**
     * ジャーナリングする際のジャーナルキー名を設定する。
     * <p>
     * デフォルトは、{@link #DEFAULT_ACCESS_JOURNAL_KEY}。<br>
     *
     * @param key ジャーナルキー名
     */
    public void setAccessJournalKey(String key);

    /**
     * ジャーナリングする際のジャーナルキー名を取得する。
     * <p>
     *
     * @return ジャーナルキー名
     */
    public String getAccessJournalKey();

    /**
     * クライアントIDをジャーナリングする際のジャーナルキー名を設定する。
     * <p>
     * デフォルトは、{@link #DEFAULT_ID_JOURNAL_KEY}。<br>
     *
     * @param key ジャーナルキー名
     */
    public void setIdJournalKey(String key);

    /**
     * クライアントIDをジャーナリングする際のジャーナルキー名を取得する。
     * <p>
     *
     * @return ジャーナルキー名
     */
    public String getIdJournalKey();

    /**
     * チケットをジャーナリングする際のジャーナルキー名を設定する。
     * <p>
     * デフォルトは、{@link #DEFAULT_TICKET_JOURNAL_KEY}。<br>
     *
     * @param key ジャーナルキー名
     */
    public void setTicketJournalKey(String key);

    /**
     * チケットをジャーナリングする際のジャーナルキー名を取得する。
     * <p>
     *
     * @return ジャーナルキー名
     */
    public String getTicketJournalKey();

    /**
     * WebSocketセッションIDをジャーナリングする際のジャーナルキー名を設定する。
     * <p>
     * デフォルトは、{@link #DEFAULT_WEBSOCKET_SESSION_ID_JOURNAL_KEY}。<br>
     *
     * @param key ジャーナルキー名
     */
    public void setWebSocketSessionIdJournalKey(String key);

    /**
     * WebSocketセッションIDをジャーナリングする際のジャーナルキー名を取得する。
     * <p>
     *
     * @return ジャーナルキー名
     */
    public String getWebSocketSessionIdJournalKey();

    /**
     * HTTPセッションIDをジャーナリングする際のジャーナルキー名を設定する。
     * <p>
     * デフォルトは、{@link #DEFAULT_HTTP_SESSION_ID_JOURNAL_KEY}。<br>
     *
     * @param key ジャーナルキー名
     */
    public void setHttpSessionIdJournalKey(String key);

    /**
     * HTTPセッションIDをジャーナリングする際のジャーナルキー名を取得する。
     * <p>
     *
     * @return ジャーナルキー名
     */
    public String getHttpSessionIdJournalKey();

    /**
     * WebSocketのパスをジャーナリングする際のジャーナルキー名を設定する。
     * <p>
     * デフォルトは、{@link #DEFAULT_PATH_JOURNAL_KEY}。<br>
     *
     * @param key ジャーナルキー名
     */
    public void setPathJournalKey(String key);

    /**
     * WebSocketのパスをジャーナリングする際のジャーナルキー名を取得する。
     * <p>
     *
     * @return ジャーナルキー名
     */
    public String getPathJournalKey();

    /**
     * クライアントIPをジャーナリングする際のジャーナルキー名を設定する。
     * <p>
     * デフォルトは、{@link #DEFAULT_SESSION_IP_JOURNAL_KEY}。<br>
     *
     * @param key ジャーナルキー名
     */
    public void setIpJournalKey(String key);

    /**
     * クライアントIPをジャーナリングする際のジャーナルキー名を取得する。
     * <p>
     *
     * @return ジャーナルキー名
     */
    public String getIpJournalKey();

    /**
     * クライアントポートをジャーナリングする際のジャーナルキー名を設定する。
     * <p>
     * デフォルトは、{@link #DEFAULT_SESSION_PORT_JOURNAL_KEY}。<br>
     *
     * @param key ジャーナルキー名
     */
    public void setPortJournalKey(String key);

    /**
     * クライアントポートをジャーナリングする際のジャーナルキー名を取得する。
     * <p>
     *
     * @return ジャーナルキー名
     */
    public String getPortJournalKey();

    /**
     * メッセージをジャーナリングする際のジャーナルキー名を設定する。
     * <p>
     * デフォルトは、{@link #DEFAULT_REQUEST_MESSAGE_JOURNAL_KEY}。<br>
     *
     * @param key ジャーナルキー名
     */
    public void setRequestMessageJournalKey(String key);

    /**
     * メッセージをジャーナリングする際のジャーナルキー名を取得する。
     * <p>
     *
     * @return ジャーナルキー名
     */
    public String getRequestMessageJournalKey();

    /**
     * CloseReasonをジャーナリングする際のジャーナルキー名を設定する。
     * <p>
     * デフォルトは、{@link #DEFAULT_CLOSE_REASON_JOURNAL_KEY}。<br>
     *
     * @param key ジャーナルキー名
     */
    public void setCloseReasonJournalKey(String key);

    /**
     * CloseReasonをジャーナリングする際のジャーナルキー名を取得する。
     * <p>
     *
     * @return ジャーナルキー名
     */
    public String getCloseReasonJournalKey();

    /**
     * AuthResultをジャーナリングする際のジャーナルキー名を設定する。
     * <p>
     * デフォルトは、{@link #DEFAULT_AUTH_RESULT_JOURNAL_KEY}。<br>
     *
     * @param key ジャーナルキー名
     */
    public void setAuthResultJournalKey(String key);

    /**
     * AuthResultをジャーナリングする際のジャーナルキー名を取得する。
     * <p>
     *
     * @return ジャーナルキー名
     */
    public String getAuthResultJournalKey();

    /**
     * リクエスト及び送信時の例外をジャーナリングする際のジャーナルキー名を設定する。
     * <p>
     * デフォルトは、{@link #DEFAULT_EXCEPTION_JOURNAL_KEY}。<br>
     *
     * @param key ジャーナルキー名
     */
    public void setExceptionJournalKey(String key);

    /**
     * リクエスト及び送信時の例外をジャーナリングする際のジャーナルキー名を取得する。
     * <p>
     *
     * @return ジャーナルキー名
     */
    public String getExceptionJournalKey();

    /**
     * リクエストをジャーナリングする際にリクエスト通番を発行するSequenceサービスのサービス名を設定する。
     * <p>
     *
     * @param name Sequenceサービスのサービス名
     */
    public void setSequenceServiceName(ServiceName name);

    /**
     * リクエストをジャーナリングする際にリクエスト通番を発行するSequenceサービスのサービス名を取得する。
     * <p>
     *
     * @return Sequenceサービスのサービス名
     */
    public ServiceName getSequenceServiceName();
}