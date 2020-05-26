/*
 * This software is distributed under following license based on modified BSD
 * style license.
 * ----------------------------------------------------------------------
 *
 * Copyright 2009 The Nimbus2 Project. All rights reserved.
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
 * policies, either expressed or implied, of the Nimbus2 Project.
 */
package jp.ossc.nimbus.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jp.ossc.nimbus.beans.PropertyAccess;
import jp.ossc.nimbus.beans.ServiceNameEditor;
import jp.ossc.nimbus.beans.StringArrayEditor;
import jp.ossc.nimbus.core.ServiceManagerFactory;
import jp.ossc.nimbus.core.ServiceName;
import jp.ossc.nimbus.service.journal.Journal;
import jp.ossc.nimbus.service.journal.editorfinder.EditorFinder;
import jp.ossc.nimbus.service.keepalive.KeepAliveChecker;
import jp.ossc.nimbus.service.sequence.Sequence;
import jp.ossc.nimbus.service.websocket.AuthResult;
import jp.ossc.nimbus.service.websocket.AuthenticateException;
import jp.ossc.nimbus.service.websocket.Authenticator;
import jp.ossc.nimbus.service.websocket.ExceptionHandlerMappingService;
import jp.ossc.nimbus.service.websocket.NimbusConfigurator;
import jp.ossc.nimbus.service.websocket.NimbusServerApplicationConfig;
import jp.ossc.nimbus.util.converter.StreamConverter;
import jp.ossc.nimbus.util.converter.StreamStringConverter;

/**
 * WebSocket接続時の事前認証リクエストを処理するサーブレット。
 * <p>
 * リクエストを受け付けて、{@link Authenticator}を呼び出す。<br>
 * Acceptレスポンス変換マッピングを設定することで、認証結果{@link AuthResult}を自動変換し、レスポンスに設定する。<br>
 * Acceptフォワードパス変換マッピングが設定されている場合は、変換処理は行わず、対象パスにフォワードする。<br>
 * どちらも設定されていない場合は、レスポンスヘッダに認証結果を設定する。<br>
 * <p>
 * 以下に、サーブレットのweb.xml定義例を示す。<br>
 *
 * <pre>
 * &lt;servlet&gt;
 *     &lt;servlet-name&gt;WebSocketAuthServlet&lt;/servlet-name&gt;
 *     &lt;servlet-class&gt;jp.ossc.nimbus.servlet.WebSocketAuthServlet&lt;/servlet-class&gt;
 *     &lt;init-param&gt;
 *         &lt;param-name&gt;AuthenticatorServiceName&lt;/param-name&gt;
 *         &lt;param-value&gt;PublishScribeServer.WebSocketPublish#AuthenticatorService&lt;/param-value&gt;
 *     &lt;/init-param&gt;
 *     &lt;init-param&gt;
 *         &lt;param-name&gt;ConfiguratorServiceName&lt;/param-name&gt;
 *         &lt;param-value&gt;PublishScribeServer.WebSocketPublish#DefaultConfiguratorService&lt;/param-value&gt;
 *     &lt;/init-param&gt;
 *     &lt;init-param&gt;
 *         &lt;param-name&gt;WebSocketAccessJournalServiceName&lt;/param-name&gt;
 *         &lt;param-value&gt;PublishScribeServer.Publish.AccessJournal#AccessJournal&lt;/param-value&gt;
 *     &lt;/init-param&gt;
 *     &lt;init-param&gt;
 *         &lt;param-name&gt;SequenceServiceName&lt;/param-name&gt;
 *         &lt;param-value&gt;PublishScribeServer.Publish.AccessJournal#Sequence&lt;/param-value&gt;
 *     &lt;/init-param&gt;
 *     &lt;init-param&gt;
 *         &lt;param-name&gt;ConverterMapDefinition&lt;/param-name&gt;
 *         &lt;param-value&gt;
 *             application/json=PublishScribeServer.WebSocketPublish#BeanJSONConverter
 *         &lt;/param-value&gt;
 *     &lt;/init-param&gt;
 *     &lt;load-on-startup&gt;1&lt;/load-on-startup&gt;
 * &lt;/servlet&gt;
 * &lt;servlet-mapping&gt;
 *     &lt;servlet-name&gt;WebSocketAuthServlet&lt;/servlet-name&gt;
 *     &lt;url-pattern&gt;/wsauth&lt;/url-pattern&gt;
 * &lt;/servlet-mapping&gt;
 * </pre>
 *
 * @author M.Ishida
 */
public class WebSocketAuthServlet extends HttpServlet {

    /**
     * {@link Authenticator}サービス名の初期化パラメータ名。
     * <p>
     */
    protected static final String INIT_PARAM_NAME_AUTHENTICATOR_SERVICE_NAME = "AuthenticatorServiceName";

    /**
     * {@link NimbusConfigurator}サービス名の初期化パラメータ名。
     * <p>
     */
    protected static final String INIT_PARAM_NAME_CONFIGURATOR_SERVICE_NAME = "ConfiguratorServiceName";

    /**
     * {@link Journal}サービス名の初期化パラメータ名。
     * <p>
     */
    protected static final String INIT_PARAM_NAME_ACCESS_JOURNAL_SERVICE_NAME = "WebSocketAccessJournalServiceName";

    /**
     * {@link Sequence}サービス名の初期化パラメータ名。
     * <p>
     */
    protected static final String INIT_PARAM_NAME_SEQUENCE_SERVICE_NAME = "SequenceServiceName";

    /**
     * {@link ExceptionHandlerMappingService}サービス名の初期化パラメータ名。
     * <p>
     */
    protected static final String INIT_PARAM_NAME_EXCEPTION_HANDLER_MAPPING_SERVICE_NAME = "ExceptionHandlerMappingServiceName";

    /**
     * {@link EditorFinder}サービス名の初期化パラメータ名。
     * <p>
     */
    protected static final String INIT_PARAM_NAME_EDITOR_FINDER_SERVICE_NAME = "EditorFinderServiceName";

    /**
     * {@link HostSelector}サービス名の初期化パラメータ名。
     * <p>
     */
    protected static final String INIT_PARAM_NAME_HOST_SELECTOR_SERVICE_NAME = "HostSelectorServiceName";

    /**
     * Acceptレスポンス変換マッピング用の初期化パラメータ名。
     * <p>
     */
    protected static final String INIT_PARAM_NAME_CONVERTER_MAP_DEF = "ConverterMapDefinition";

    /**
     * Acceptフォワードパス変換マッピング用の初期化パラメータ名。
     * <p>
     */
    protected static final String INIT_PARAM_NAME_FORWARD_PATH_MAP_DEF = "ForwardPathMapDefinition";

    /**
     * 認証結果BeanをRequestパラメータに格納する際のキーの名初期化パラメータ名。
     * <p>
     */
    protected static final String INIT_PARAM_NAME_AUTH_RESULT_KEY = "AuthRsultKey";

    /**
     * 認証結果IDをResponseヘッダに格納する際のキー用の初期化パラメータ名。
     * <p>
     */
    protected static final String INIT_PARAM_NAME_AUTH_RESULT_ID = "ResponseHeaderWebSocketIdKey";

    /**
     * 認証結果チケットをResponseヘッダに格納する際のキー用の初期化パラメータ名。
     * <p>
     */
    protected static final String INIT_PARAM_NAME_AUTH_RESULT_TICKET = "ResponseHeaderWebSocketTicketKey";

    /**
     * 認証結果をResponseヘッダに格納する際のキー用の初期化パラメータ名。
     * <p>
     */
    protected static final String INIT_PARAM_NAME_AUTH_RESULT_AUTH_RESULT = "ResponseHeaderWebSocketAuthResultKey";

    /**
     * 認証結果URLをResponseヘッダに格納する際のキー用の初期化パラメータ名。
     * <p>
     */
    protected static final String INIT_PARAM_NAME_AUTH_RESULT_URL = "ResponseHeaderWebSocketURLKey";

    /**
     * リクエストジャーナルのルートステップのキーの初期化パラメータ名。
     * <p>
     */
    protected static final String INIT_PARAM_NAME_ACCESS_JOURNAL_KEY = "AccessJournalKey";

    /**
     * ジャーナルのHttpセッションIDのキーの初期化パラメータ名。
     * <p>
     */
    protected static final String INIT_PARAM_NAME_HTTP_SESSION_ID_JOURNAL_KEY = "HttpSessionIdJournalKey";

    /**
     * ジャーナルのパスのキーの初期化パラメータ名。
     * <p>
     */
    protected static final String INIT_PARAM_NAME_PATH_JOURNAL_KEY = "PathJournalKey";

    /**
     * ジャーナルのIPのキーの初期化パラメータ名。
     * <p>
     */
    protected static final String INIT_PARAM_NAME_IP_JOURNAL_KEY = "IpJournalKey";

    /**
     * ジャーナルのポートのキーの初期化パラメータ名。
     * <p>
     */
    protected static final String INIT_PARAM_NAME_PORT_JOURNAL_KEY = "PortJournalKey";

    /**
     * ジャーナルのリクエストヘッダのキーの初期化パラメータ名。
     * <p>
     */
    protected static final String INIT_PARAM_NAME_HEADER_JOURNAL_KEY = "HeaderJournalKey";

    /**
     * ジャーナルのリクエストパラメータのキーの初期化パラメータ名。
     * <p>
     */
    protected static final String INIT_PARAM_NAME_PARAMETER_JOURNAL_KEY = "ParameterJournalKey";

    /**
     * ジャーナルのAuthResultのキーの初期化パラメータ名。
     * <p>
     */
    protected static final String INIT_PARAM_NAME_AUTH_RESULT_JOURNAL_KEY = "AuthResultJournalKey";

    /**
     * ジャーナルの例外のキーの初期化パラメータ名。
     * <p>
     */
    protected static final String INIT_PARAM_NAME_EXCEPTION_JOURNAL_KEY = "ExceptionJournalKey";

    /**
     * URLスキーマの初期化パラメータ名。
     * <p>
     */
    protected static final String INIT_PARAM_NAME_URL_SCHEMA = "UrlSchema";

    /**
     * リモートIPの初期化パラメータ名。
     * <p>
     */
    protected static final String REMOTE_IP_PROPERTY = "RemoteIpProperty";

    /**
     * HttpResponseHeaderに設定するCache-Control用の初期化パラメータ名。
     * <p>
     */
    protected static final String INIT_PARAM_NAME_CACHE_CONTRO = "Cache-Control";

    /**
     * 認証結果BeanをRequestパラメータに格納する際のキーのデフォルト値。
     * <p>
     */
    protected static final String DEFAULT_AUTH_RESULT_KEY = "AuthRsult";

    /**
     * 認証結果IDをResponseヘッダに格納する際のキーのデフォルト値。
     * <p>
     */
    protected static final String DEFAULT_AUTH_RESULT_ID_HEADER_KEY = "X-nimbus-websocket-id";

    /**
     * 認証結果チケットをResponseヘッダに格納する際のキーのデフォルト値。
     * <p>
     */
    protected static final String DEFAULT_AUTH_RESULT_TICKET_HEADER_KEY = "X-nimbus-websocket-ticket";

    /**
     * 認証結果をResponseヘッダに格納する際のキーのデフォルト値。
     * <p>
     */
    protected static final String DEFAULT_AUTH_RESULT_AUTH_RESULT_HEADER_KEY = "X-nimbus-websocket-auth-result";

    /**
     * 認証結果URLをResponseヘッダに格納する際のキーのデフォルト値。
     * <p>
     */
    protected static final String DEFAULT_AUTH_RESULT_URL_HEADER_KEY = "X-nimbus-websocket-url";

    /**
     * リクエストジャーナルのルートステップのキーのデフォルト値。
     * <p>
     */
    protected static final String DEFAULT_ACCESS_JOURNAL_KEY = "Access";

    /**
     * ジャーナルのHttpセッションIDのキーのデフォルト値。
     * <p>
     */
    protected static final String DEFAULT_HTTP_SESSION_ID_JOURNAL_KEY = "HttpSessionId";

    /**
     * ジャーナルのパスのキーのデフォルト値。
     * <p>
     */
    protected static final String DEFAULT_PATH_JOURNAL_KEY = "Path";

    /**
     * ジャーナルのIPのキーのデフォルト値。
     * <p>
     */
    protected static final String DEFAULT_IP_JOURNAL_KEY = "Ip";

    /**
     * ジャーナルのポートのキーのデフォルト値。
     * <p>
     */
    protected static final String DEFAULT_PORT_JOURNAL_KEY = "Port";

    /**
     * ジャーナルのリクエストヘッダのキーのデフォルト値。
     * <p>
     */
    protected static final String DEFAULT_HEADER_JOURNAL_KEY = "Header";

    /**
     * ジャーナルのリクエストパラメータのキーのデフォルト値。
     * <p>
     */
    protected static final String DEFAULT_PARAMETER_JOURNAL_KEY = "Parameter";

    /**
     * ジャーナルのAuthResultのキーのデフォルト値。
     * <p>
     */
    protected static final String DEFAULT_AUTH_RESULT_JOURNAL_KEY = "AuthResult";

    /**
     * ジャーナルの例外のキーのデフォルト値。
     * <p>
     */
    protected static final String DEFAULT_EXCEPTION_JOURNAL_KEY = "Exception";

    /**
     * HTTPヘッダAcceptキー。
     * <p>
     */
    protected static final String HTTP_HEADER_NAME_ACCEPT = "Accept";

    /**
     * HTTPヘッダAccept-Charsetキー。
     * <p>
     */
    protected static final String HTTP_HEADER_NAME_ACCEPT_CHARSET = "Accept-Charset";

    protected Authenticator authenticator;
    protected Journal accessJournal;
    protected EditorFinder editorFinder;
    protected Sequence sequence;
    protected ExceptionHandlerMappingService exceptionHandler;
    protected KeepAliveChecker hostSelector;

    protected String configWebsocketPath;
    protected String urlSchema;
    protected String responseHeaderCacheControl;
    protected String responseHeaderWebSocketIdKey = DEFAULT_AUTH_RESULT_ID_HEADER_KEY;
    protected String responseHeaderWebSocketTicketKey = DEFAULT_AUTH_RESULT_TICKET_HEADER_KEY;
    protected String responseHeaderWebSocketAuthResultKey = DEFAULT_AUTH_RESULT_AUTH_RESULT_HEADER_KEY;
    protected String responseHeaderWebSocketURLKey = DEFAULT_AUTH_RESULT_URL_HEADER_KEY;
    protected String accessJournalKey = DEFAULT_ACCESS_JOURNAL_KEY;
    protected String httpSessionIdJournalKey = DEFAULT_HTTP_SESSION_ID_JOURNAL_KEY;
    protected String pathJournalKey = DEFAULT_PATH_JOURNAL_KEY;
    protected String ipJournalKey = DEFAULT_IP_JOURNAL_KEY;
    protected String portJournalKey = DEFAULT_PORT_JOURNAL_KEY;
    protected String headerJournalKey = DEFAULT_HEADER_JOURNAL_KEY;
    protected String parameterJournalKey = DEFAULT_PARAMETER_JOURNAL_KEY;
    protected String authResultJournalKey = DEFAULT_AUTH_RESULT_JOURNAL_KEY;
    protected String exceptionJournalKey = DEFAULT_EXCEPTION_JOURNAL_KEY;
    protected String remoteIpProperty;

    protected String authRsultKey = DEFAULT_AUTH_RESULT_KEY;
    protected Map responseConverterMap;
    protected Map forwardPathMap;
    protected PropertyAccess propertyAccess;

    /**
     * 初期化処理
     */
    public void init() throws ServletException {
        final ServiceName configratorServiceName = getInitialParameterServiceName(INIT_PARAM_NAME_CONFIGURATOR_SERVICE_NAME);
        if (configratorServiceName == null) {
            throw new ServletException("ConfigratorServiceName is null.");
        }
        NimbusConfigurator configrator = (NimbusConfigurator) ServiceManagerFactory
                .getServiceObject(configratorServiceName);
        configWebsocketPath = configrator.getPath();

        final ServiceName authenticatorServiceName = getInitialParameterServiceName(INIT_PARAM_NAME_AUTHENTICATOR_SERVICE_NAME);
        if (authenticatorServiceName == null) {
            throw new ServletException("AuthenticatorServiceName is null.");
        }
        authenticator = (Authenticator) ServiceManagerFactory.getServiceObject(authenticatorServiceName);

        final ServiceName accessJournalServiceName = getInitialParameterServiceName(INIT_PARAM_NAME_ACCESS_JOURNAL_SERVICE_NAME);
        if (accessJournalServiceName != null) {
            accessJournal = (Journal) ServiceManagerFactory.getServiceObject(accessJournalServiceName);
            final ServiceName editorFinderServiceName = getInitialParameterServiceName(INIT_PARAM_NAME_EDITOR_FINDER_SERVICE_NAME);
            if (editorFinderServiceName != null) {
                editorFinder = (EditorFinder) ServiceManagerFactory.getServiceObject(editorFinderServiceName);
            }
            final ServiceName sequenceServiceName = getInitialParameterServiceName(INIT_PARAM_NAME_SEQUENCE_SERVICE_NAME);
            if (sequenceServiceName != null) {
                sequence = (Sequence) ServiceManagerFactory.getServiceObject(sequenceServiceName);
            } else {
                throw new ServletException("SequenceServiceName is null.");
            }
        }


        final ServiceName exceptionHandlerMappingServiceName = getInitialParameterServiceName(INIT_PARAM_NAME_EXCEPTION_HANDLER_MAPPING_SERVICE_NAME);
        if (exceptionHandlerMappingServiceName != null) {
            exceptionHandler = (ExceptionHandlerMappingService) ServiceManagerFactory.getServiceObject(exceptionHandlerMappingServiceName);
        }

        final ServiceName hostSelectorServiceName = getInitialParameterServiceName(INIT_PARAM_NAME_HOST_SELECTOR_SERVICE_NAME);
        if (hostSelectorServiceName != null) {
            hostSelector = (KeepAliveChecker) ServiceManagerFactory.getServiceObject(hostSelectorServiceName);
        }

        String converterMapDefStr = getServletConfig().getInitParameter(INIT_PARAM_NAME_CONVERTER_MAP_DEF);
        if (converterMapDefStr != null && converterMapDefStr.length() != 0) {
            createResponseConverterMap(converterMapDefStr);
        }

        String forwardPathMapDefStr = getServletConfig().getInitParameter(INIT_PARAM_NAME_FORWARD_PATH_MAP_DEF);
        if (forwardPathMapDefStr != null && forwardPathMapDefStr.length() != 0) {
            createForwardPathMap(forwardPathMapDefStr);
        }

        String initAuthRsultKey = getServletConfig().getInitParameter(INIT_PARAM_NAME_AUTH_RESULT_KEY);
        if (initAuthRsultKey != null && initAuthRsultKey.length() > 0) {
            authRsultKey = initAuthRsultKey;
        }

        String initResponseHeaderWebSocketIdKey = getServletConfig().getInitParameter(INIT_PARAM_NAME_AUTH_RESULT_ID);
        if (initResponseHeaderWebSocketIdKey != null && initResponseHeaderWebSocketIdKey.length() > 0) {
            responseHeaderWebSocketIdKey = initResponseHeaderWebSocketIdKey;
        }

        String initResponseHeaderWebSocketTicketKey = getServletConfig().getInitParameter(
                INIT_PARAM_NAME_AUTH_RESULT_TICKET);
        if (initResponseHeaderWebSocketTicketKey != null && initResponseHeaderWebSocketTicketKey.length() > 0) {
            responseHeaderWebSocketTicketKey = initResponseHeaderWebSocketTicketKey;
        }

        String initResponseHeaderWebSocketAuthResultKey = getServletConfig().getInitParameter(
                INIT_PARAM_NAME_AUTH_RESULT_AUTH_RESULT);
        if (initResponseHeaderWebSocketAuthResultKey != null && initResponseHeaderWebSocketAuthResultKey.length() > 0) {
            responseHeaderWebSocketAuthResultKey = initResponseHeaderWebSocketAuthResultKey;
        }

        String initResponseHeaderWebSocketURLKey = getServletConfig().getInitParameter(INIT_PARAM_NAME_AUTH_RESULT_URL);
        if (initResponseHeaderWebSocketURLKey != null && initResponseHeaderWebSocketURLKey.length() > 0) {
            responseHeaderWebSocketURLKey = initResponseHeaderWebSocketURLKey;
        }

        String initAccessJournalKey = getServletConfig().getInitParameter(INIT_PARAM_NAME_ACCESS_JOURNAL_KEY);
        if (initAccessJournalKey != null && initAccessJournalKey.length() > 0) {
            accessJournalKey = initAccessJournalKey;
        }

        String initHttpSessionIdJournalKey = getServletConfig().getInitParameter(
                INIT_PARAM_NAME_HTTP_SESSION_ID_JOURNAL_KEY);
        if (initHttpSessionIdJournalKey != null && initHttpSessionIdJournalKey.length() > 0) {
            httpSessionIdJournalKey = initHttpSessionIdJournalKey;
        }

        String initPathJournalKey = getServletConfig().getInitParameter(INIT_PARAM_NAME_PATH_JOURNAL_KEY);
        if (initPathJournalKey != null && initPathJournalKey.length() > 0) {
            pathJournalKey = initPathJournalKey;
        }

        String initIpJournalKey = getServletConfig().getInitParameter(INIT_PARAM_NAME_IP_JOURNAL_KEY);
        if (initIpJournalKey != null && initIpJournalKey.length() > 0) {
            ipJournalKey = initIpJournalKey;
        }

        String initPortJournalKey = getServletConfig().getInitParameter(INIT_PARAM_NAME_PORT_JOURNAL_KEY);
        if (initPortJournalKey != null && initPortJournalKey.length() > 0) {
            portJournalKey = initPortJournalKey;
        }

        String initHeaderJournalKey = getServletConfig().getInitParameter(INIT_PARAM_NAME_HEADER_JOURNAL_KEY);
        if (initHeaderJournalKey != null && initHeaderJournalKey.length() > 0) {
            headerJournalKey = initHeaderJournalKey;
        }

        String initParameterJournalKey = getServletConfig().getInitParameter(INIT_PARAM_NAME_PARAMETER_JOURNAL_KEY);
        if (initParameterJournalKey != null && initParameterJournalKey.length() > 0) {
            parameterJournalKey = initParameterJournalKey;
        }

        String initAuthResultJournalKey = getServletConfig().getInitParameter(INIT_PARAM_NAME_AUTH_RESULT_JOURNAL_KEY);
        if (initAuthResultJournalKey != null && initAuthResultJournalKey.length() > 0) {
            authResultJournalKey = initAuthResultJournalKey;
        }

        String initExceptionJournalKey = getServletConfig().getInitParameter(INIT_PARAM_NAME_EXCEPTION_JOURNAL_KEY);
        if (initExceptionJournalKey != null && initExceptionJournalKey.length() > 0) {
            exceptionJournalKey = initExceptionJournalKey;
        }

        String initUrlSchema = getServletConfig().getInitParameter(INIT_PARAM_NAME_URL_SCHEMA);
        if (initUrlSchema != null && initUrlSchema.length() > 0) {
            urlSchema = initUrlSchema;
        }
        
        String initRemoteIpProperty = getServletConfig().getInitParameter(REMOTE_IP_PROPERTY);
        if (initRemoteIpProperty != null && initRemoteIpProperty.length() > 0) {
            remoteIpProperty = initRemoteIpProperty;
            propertyAccess = new PropertyAccess();
        }
        
        String initCacheControl = getServletConfig().getInitParameter(INIT_PARAM_NAME_CACHE_CONTRO);
        if (initCacheControl != null && initCacheControl.length() > 0) {
            responseHeaderCacheControl = initCacheControl;
        }
        
    }

    public void destroy() {
        String[] serviceDefinitionFiles = null;
        try {
            serviceDefinitionFiles = NimbusServerApplicationConfig.getLoadTargetFileNames();
        } catch (IOException e) {
            // 起動時に読み込んでいるのでエラーは出ないはず。
        }
        if (serviceDefinitionFiles != null) {
            for (int i = serviceDefinitionFiles.length -1; i > -1; i--) {
                ServiceManagerFactory.unloadManager(serviceDefinitionFiles[i]);
            }
        }
    }

    /**
     * GETメソッド呼び出しを処理する。
     * <p>
     *
     * @param req HTTPリクエスト
     * @param resp HTTPレスポンス
     * @exception ServletException
     * @exception IOException
     */
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        auth(req, res);
    }

    /**
     * POSTメソッド呼び出しを処理する。
     * <p>
     *
     * @param req HTTPリクエスト
     * @param resp HTTPレスポンス
     * @exception ServletException
     * @exception IOException
     */
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        auth(req, res);
    }

    /**
     * 認証処理。
     * <p>
     *
     * @param req HTTPリクエスト
     * @param resp HTTPレスポンス
     * @exception ServletException
     * @exception IOException
     */
    protected void auth(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        try {
            if (accessJournal != null) {
                accessJournal.startJournal(accessJournalKey, editorFinder);
                accessJournal.setRequestId(sequence.increment());
                Map paramMap = new HashMap();
                Iterator itr = req.getParameterMap().entrySet().iterator();
                while (itr.hasNext()) {
                    Entry entry = (Entry) itr.next();
                    String[] values = (String[]) entry.getValue();
                    StringBuilder value = new StringBuilder();
                    for (int i = 0; i < values.length; i++) {
                        if (value.length() > 0) {
                            value.append(", ");
                        }
                        value.append(values[i]);
                    }
                    paramMap.put(entry.getKey(), value.toString());
                }
                accessJournal.addInfo(parameterJournalKey, paramMap);

                if (req.getSession(false) != null) {
                    accessJournal.addInfo(httpSessionIdJournalKey, req.getSession().getId());
                }
                Map headers = new HashMap();
                Enumeration e = req.getHeaderNames();
                while (e.hasMoreElements()) {
                    String headerName = (String) e.nextElement();
                    headers.put(headerName, req.getHeader(headerName));
                }
                accessJournal.addInfo(headerJournalKey, headers);
                String remoteAddress = req.getRemoteAddr();
                if(remoteIpProperty != null) {
                    try {
                        remoteAddress = remoteIpProperty == null ? req.getRemoteAddr() : (String)propertyAccess.get(req, remoteIpProperty);
                    } catch(Exception ex) {
                        // NOP
                    }
                }
                accessJournal.addInfo(ipJournalKey, remoteAddress);
                accessJournal.addInfo(portJournalKey, req.getRemotePort());
                accessJournal.addInfo(pathJournalKey, req.getRequestURI());
            }
            AuthResult result = new AuthResult();
            try {
                result = authenticator.login(req, res);
            } catch (AuthenticateException e) {
                result.setResult(false);
                if (accessJournal != null) {
                    accessJournal.addInfo(exceptionJournalKey, e);
                }
            }
            if(hostSelector != null){
                try{
                    Object hostInfo = hostSelector.getHostInfo();
                    if(hostInfo instanceof URI) {
                        URI uri = (URI)hostInfo;
                        if(urlSchema != null) {
                            result.setUrlSchema(urlSchema);
                        } else {
                            result.setUrlSchema(uri.getScheme());
                        }
                        result.setHost(uri.getHost());
                        result.setPort(uri.getPort());
                    } else if(hostInfo instanceof InetSocketAddress) {
                        InetSocketAddress address = (InetSocketAddress)hostInfo;
                        if(urlSchema != null) {
                            result.setUrlSchema(urlSchema);
                        } else {
                            if(req.isSecure()) {
                                result.setUrlSchema("wss");
                            } else {
                                result.setUrlSchema("ws");
                            }
                        }
                        String host = address.toString();
                        if(host.startsWith("/")) {
                            host = host.substring(1);
                            if(host.indexOf(":") != -1) {
                                host = host.substring(0, host.indexOf(":"));
                            }
                        } else {
                            host = address.getHostName();
                        }
                        result.setHost(host);
                        result.setPort(address.getPort());
                    } else {
                        throw new UnsupportedOperationException("HostInfo is not support response. HostInfo=" + hostInfo);
                    }
                } catch(Exception e){
                    result.setResult(false);
                    if (accessJournal != null) {
                        accessJournal.addInfo(exceptionJournalKey, e);
                    }
                }
            } else {
                if(urlSchema != null) {
                    result.setUrlSchema(urlSchema);
                } else {
                    if(req.isSecure()) {
                        result.setUrlSchema("wss");
                    } else {
                        result.setUrlSchema("ws");
                    }
                }
                result.setHost(req.getServerName());
                result.setPort(req.getServerPort());
            }
            result.setUrl(req, configWebsocketPath);
            if (accessJournal != null) {
                accessJournal.addInfo(authResultJournalKey, result);
            }
            if(responseHeaderCacheControl != null) {
                res.setHeader("Cache-Control", responseHeaderCacheControl);
            }
            if (forwardPathMap != null) {
                if (forward(req, res, result)) {
                    return;
                }
            }
            if (responseConverterMap != null) {
                setAcceptCharset(req, res);
                if (writeResponseBody(req, res, result)) {
                    return;
                }
            }
            res.setHeader(responseHeaderWebSocketIdKey, result.getId());
            res.setHeader(responseHeaderWebSocketTicketKey, result.getTicket());
            res.setHeader(responseHeaderWebSocketAuthResultKey, String.valueOf(result.isResult()));
            res.setHeader(responseHeaderWebSocketURLKey, result.getUrl());

        } catch(ServletException e){
            if (exceptionHandler != null) {
                try {
                    exceptionHandler.handleException(null, e);
                } catch (Throwable th) {
                }
            }
            throw e;
        } catch(IOException e){
            if (exceptionHandler != null) {
                try {
                    exceptionHandler.handleException(null, e);
                } catch (Throwable th) {
                }
            }
            throw e;
        } catch(RuntimeException e){
            if (exceptionHandler != null) {
                try {
                    exceptionHandler.handleException(null, e);
                } catch (Throwable th) {
                }
            }
            throw e;
        } finally {
            if (accessJournal != null) {
                accessJournal.endJournal();
            }

        }

    }

    /**
     * リクエストヘッダで要求されたAcceptCharsetのチェックを行い、レスポンスのCharacterEncodingに設定する。
     * <p>
     *
     * @param req HTTPリクエスト
     * @param res HTTPレスポンス
     */
    protected void setAcceptCharset(HttpServletRequest req, HttpServletResponse res) {
        String acceptCharsetStr = req.getHeader(HTTP_HEADER_NAME_ACCEPT_CHARSET);
        if (acceptCharsetStr != null) {
            AcceptCharset acceptCharset = null;
            try {
                acceptCharset = new AcceptCharset(acceptCharsetStr);
            } catch (IllegalArgumentException e) {
                ServiceManagerFactory.getLogger().write("WSSV_00003", acceptCharsetStr, e);
                return;
            }
            boolean isSupported = false;
            for (int i = 0; i < acceptCharset.charsetRanges.size(); i++) {
                CharsetRange cr = (CharsetRange) acceptCharset.charsetRanges.get(i);
                if (Charset.isSupported(cr.getCharset())) {
                    isSupported = true;
                    res.setCharacterEncoding(Charset.forName(cr.getCharset()).name());
                    break;
                }
            }
            if (!isSupported) {
                ServiceManagerFactory.getLogger().write("WSSV_00004", acceptCharsetStr);
            }
        }
    }

    /**
     * リクエストヘッダで要求されたAcceptのチェックを行い、フォワードパスのマッピング内にキーが存在する場合は対象パスにフォワードする。
     * <p>
     *
     * @param req HTTPリクエスト
     * @param res HTTPレスポンス
     * @param result 認証結果
     * @return 処理結果
     * @throws ServletException
     * @throws IOException
     */
    protected boolean forward(HttpServletRequest req, HttpServletResponse res, AuthResult result)
            throws ServletException, IOException {
        String acceptStr = req.getHeader(HTTP_HEADER_NAME_ACCEPT);
        String forwardPath = null;
        Accept accept = null;
        try {
            accept = new Accept(acceptStr);
        } catch (IllegalArgumentException e) {
            ServiceManagerFactory.getLogger().write("WSSV_00002", acceptStr, e);
            return false;
        }
        for (int i = 0; i < accept.mediaRanges.size(); i++) {
            MediaRange mr = (MediaRange) accept.mediaRanges.get(i);
            forwardPath = (String) forwardPathMap.get(mr.getMediaType());
            if (forwardPath != null) {
                break;
            }
        }
        if (acceptStr == null || forwardPath == null) {
            return false;
        }
        if (result.isResult()) {
            req.setAttribute(authRsultKey, result);
            RequestDispatcher dispatch = req.getRequestDispatcher(forwardPath);
            dispatch.forward(req, res);
        } else {
            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
        }
        return true;
    }

    /**
     * リクエストヘッダで要求されたAcceptのチェックを行い、
     * レスポンスデータコンバータのマッピング内にキーが存在する場合は認証結果をコンバートしレスポンスに出力する。
     * <p>
     *
     * @param req HTTPリクエスト
     * @param res HTTPレスポンス
     * @param result 認証結果
     * @return 処理結果
     * @throws IOException
     */
    protected boolean writeResponseBody(HttpServletRequest req, HttpServletResponse res, AuthResult result)
            throws IOException {

        String acceptStr = req.getHeader(HTTP_HEADER_NAME_ACCEPT);
        StreamConverter responseConverter = null;
        String mediaType = null;
        if (acceptStr == null) {
            return false;
        }

        Accept accept = null;
        try {
            accept = new Accept(acceptStr);
        } catch (IllegalArgumentException e) {
            ServiceManagerFactory.getLogger().write("WSSV_00002", acceptStr, e);
            return false;
        }
        for (int i = 0; i < accept.mediaRanges.size(); i++) {
            MediaRange mr = (MediaRange) accept.mediaRanges.get(i);
            responseConverter = (StreamConverter) responseConverterMap.get(mr.getMediaType());
            if (responseConverter != null) {
                mediaType = mr.getMediaType();
                break;
            }
        }
        if (mediaType == null) {
            return false;
        }

        if (responseConverter instanceof StreamStringConverter) {
            if (res.getCharacterEncoding() != null
                    && !res.getCharacterEncoding().equals(
                            ((StreamStringConverter) responseConverter).getCharacterEncodingToStream())) {
                responseConverter = ((StreamStringConverter) responseConverter).cloneCharacterEncodingToStream(res
                        .getCharacterEncoding());
            }
        }
        res.setContentType(new ContentType(mediaType, res.getCharacterEncoding()).toString());
        InputStream is = responseConverter.convertToStream(result);
        ServletOutputStream sos = res.getOutputStream();
        byte[] bytes = new byte[2048];
        int readLen = 0;
        while ((readLen = is.read(bytes)) != -1) {
            sos.write(bytes, 0, readLen);
        }
        return true;
    }

    private ServiceName getInitialParameterServiceName(String name) {
        final ServletConfig config = getServletConfig();
        final String serviceNameStr = config.getInitParameter(name);
        if (serviceNameStr == null) {
            return null;
        }
        final ServiceNameEditor editor = new ServiceNameEditor();
        editor.setAsText(serviceNameStr);
        return (ServiceName) editor.getValue();
    }

    private void createResponseConverterMap(String initialParam) {
        responseConverterMap = new HashMap();
        final StringArrayEditor arrayEditor = new StringArrayEditor();
        arrayEditor.setAsText(initialParam);
        String[] converters = (String[]) arrayEditor.getValue();
        for (int i = 0; i < converters.length; i++) {
            String[] converter = converters[i].split("=");
            final ServiceNameEditor editor = new ServiceNameEditor();
            editor.setAsText(converter[1]);
            responseConverterMap.put(converter[0],
                    ServiceManagerFactory.getServiceObject((ServiceName) editor.getValue()));
        }

    }

    private void createForwardPathMap(String initialParam) {
        forwardPathMap = new HashMap();
        final StringArrayEditor arrayEditor = new StringArrayEditor();
        arrayEditor.setAsText(initialParam);
        String[] converters = (String[]) arrayEditor.getValue();
        for (int i = 0; i < converters.length; i++) {
            String[] converter = converters[i].split("=");
            forwardPathMap.put(converter[0], converter[1]);
        }
    }

    protected static class HeaderValue {
        protected String value;
        protected Map parameters;
        protected int hashCode;

        public HeaderValue() {
        }

        public HeaderValue(String header) {
            String[] types = header.split(";");
            value = types[0].trim();
            hashCode = value.hashCode();
            if (types.length > 1) {
                parameters = new HashMap();
                for (int i = 1; i < types.length; i++) {
                    String parameter = types[i].trim();
                    final int index = parameter.indexOf('=');
                    if (index != -1) {
                        parameters.put(parameter.substring(0, index).toLowerCase(), parameter.substring(index + 1)
                                .toLowerCase());
                    } else {
                        parameters.put(parameter.toLowerCase(), null);
                    }
                }
                hashCode += parameters.hashCode();
            }
        }

        public String getValue() {
            return value;
        }

        public void setValue(String val) {
            value = val;
        }

        public String getParameter(String name) {
            return parameters == null ? null : (String) parameters.get(name);
        }

        public void setParameter(String name, String value) {
            if (parameters == null) {
                parameters = new HashMap();
            }
            parameters.put(name, value);
        }

        public String toString() {
            StringBuilder buf = new StringBuilder();
            buf.append(value);
            if (parameters != null) {
                Iterator entries = parameters.entrySet().iterator();
                while (entries.hasNext()) {
                    Map.Entry entry = (Map.Entry) entries.next();
                    buf.append(';').append(entry.getKey()).append('=').append(entry.getValue());
                }
            }
            return buf.toString();
        }

        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof HeaderValue)) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            HeaderValue cmp = (HeaderValue) obj;
            if (!value.equals(cmp.value)) {
                return false;
            }
            if ((parameters == null && cmp.parameters != null) || (parameters != null && cmp.parameters == null)
                    || (parameters != null && !parameters.equals(cmp.parameters))) {
                return false;
            }
            return true;
        }

        public int hashCode() {
            return hashCode;
        }
    }

    protected static class MediaType extends HeaderValue {
        public MediaType() {
        }

        public MediaType(String header) {
            super(header);
        }

        public String getMediaType() {
            return getValue();
        }

        public void setMediaType(String type) {
            setValue(type);
        }
    }

    protected static class ContentType extends MediaType {
        public ContentType(String mediaType, String charset) {
            setMediaType(mediaType);
            setCharset(charset);
        }

        public String getCharset() {
            return getParameter("charset");
        }

        public void setCharset(String charset) {
            setParameter("charset", charset);
        }
    }

    protected static class MediaRange extends MediaType {
        protected float q = 1.0f;

        public MediaRange(String header) throws IllegalArgumentException {
            super(header);
            String qvalue = getParameter("q");
            if (qvalue != null) {
                try {
                    q = Float.parseFloat(qvalue);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("qvalue is illegal. q=" + qvalue);
                }
            }
        }
    }

    protected static class CharsetRange extends HeaderValue {
        protected float q = 1.0f;

        public CharsetRange(String header) throws IllegalArgumentException {
            super(header);
            String qvalue = getParameter("q");
            if (qvalue != null) {
                try {
                    q = Float.parseFloat(qvalue);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("qvalue is illegal. q=" + qvalue);
                }
            }
        }

        public String getCharset() {
            return getValue();
        }

        public void setCharset(String charset) {
            setValue(charset);
        }
    }

    protected static class Accept {
        public final List mediaRanges;

        public Accept(String header) throws IllegalArgumentException {
            String[] mediaRangeArray = header.split(",");
            mediaRanges = new ArrayList(mediaRangeArray.length);
            for (int i = 0; i < mediaRangeArray.length; i++) {
                mediaRanges.add(new MediaRange(mediaRangeArray[i]));
            }
            Collections.sort(mediaRanges, new Comparator() {
                public int compare(Object o1, Object o2) {
                    return ((MediaRange) o1).q == ((MediaRange) o2).q ? 0
                            : ((MediaRange) o1).q > ((MediaRange) o2).q ? -1 : 1;
                }
            });
        }
    }

    protected static class AcceptCharset {
        public final List charsetRanges;

        public AcceptCharset(String header) throws IllegalArgumentException {
            String[] charsetRangeArray = header.split(",");
            charsetRanges = new ArrayList(charsetRangeArray.length);
            for (int i = 0; i < charsetRangeArray.length; i++) {
                charsetRanges.add(new CharsetRange(charsetRangeArray[i]));
            }
            Collections.sort(charsetRanges, new Comparator() {
                public int compare(Object o1, Object o2) {
                    return ((CharsetRange) o1).q == ((CharsetRange) o2).q ? 0
                            : ((CharsetRange) o1).q > ((CharsetRange) o2).q ? -1 : 1;
                }
            });
        }
    }

}