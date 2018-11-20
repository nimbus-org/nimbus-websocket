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
package jp.ossc.nimbus.service.test.action;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.PongMessage;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import jp.ossc.nimbus.core.ServiceBase;
import jp.ossc.nimbus.core.ServiceManagerFactory;
import jp.ossc.nimbus.service.http.HttpResponse;
import jp.ossc.nimbus.service.test.ChainTestAction;
import jp.ossc.nimbus.service.test.TestAction;
import jp.ossc.nimbus.service.test.TestActionEstimation;
import jp.ossc.nimbus.service.test.TestContext;

/**
 * WebSocketクライアントを操作するテストアクション。<p>
 * 動作の詳細は、{@link #execute(TestContext, String, Reader)}を参照。<br>
 * 
 * @author M.Ishida
 */
public class WebSocketClientActionService extends ServiceBase implements TestAction, ChainTestAction.TestActionProcess, TestActionEstimation, WebSocketClientActionServiceMBean{
    
    private static final long serialVersionUID = 8262598899952759519L;
    
    protected double expectedCost = Double.NaN;
    protected String encoding = "UTF-8";
    protected boolean isAutoSave = false;
    
    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public boolean isAutoSave() {
        return isAutoSave;
    }

    public void setAutoSave(boolean isAutoSave) {
        this.isAutoSave = isAutoSave;
    }

    public void startService() throws Exception{
        if (!Charset.isSupported(encoding)) {
            throw new IllegalArgumentException(encoding + " is not supported .");
        }
    }
    
    /**
     * リソースの内容を読み込んで、WebSockeのクライアントを操作する。<p>
     * リソースのフォーマットは、以下。<br>
     * <pre>
     * operateType
     * 以降はoperateTypeによって変化する。
     * 
     * operateTypeは、"create","close","send","save"のいずれかを指定する。
     * "create"はWebSocketクライアントを生成する。"close"はWebSocketのセッションをクローズする。"send"はWebSocketサーバにメッセージを送信する。"save"はWebSocketクライアントで受信したメッセージをファイルに保存する。
     * 
     * operateTypeが"create"の場合は以下のフォーマット
     * operateType
     * paramId
     * 
     * operateTypeが"close"の場合は以下のフォーマット
     * operateType
     * clientId
     * 
     * operateTypeが"send"の場合は以下のフォーマット
     * operateType
     * clientId
     * message
     * 
     * operateTypeが"save"の場合は以下のフォーマット
     * operateType
     * clientId
     * 
     * </pre>
     * paramIdは、WebSocket接続時に使用するパラメータオブジェクトを指定する場合に指定するもので、同一テストケース中に、このTestActionより前に、このクラスのTestActionが存在する場合は、そのアクションIDを指定する。また、同一シナリオ中に、このTestActionより前に、このクラスのTestActionが存在する場合は、テストケースIDとアクションIDをカンマ区切りで指定する。パラメータ指定の必要がない場合は、空文字を指定する。<br>
     * clientIdは、WebSocketのクライアント生成するのアクションID指定するもので、同一テストケース中に、このTestActionより前に、このクラスのTestActionが存在する場合は、そのアクションIDを指定する。また、同一シナリオ中に、このTestActionより前に、このクラスのTestActionが存在する場合は、テストケースIDとアクションIDをカンマ区切りで指定する。<br>
     * messageは、サーバに送信したい文字列を記載する。複数ある場合は、改行して指定する<br>
     *
     * @param context コンテキスト
     * @param actionId アクションID
     * @param resource リソース
     * @return "create","send","save"の場合はWebSocketのクライアント、"close"の場合はnull
     */
    public Object execute(TestContext context, String actionId, Reader resource) throws Exception{
        return execute(context, actionId, null, resource);
    }
    
    /**
     * WebSocketのクライアントを生成する。<p>
     *
     * @param context コンテキスト
     * @param actionId アクションID
     * @param preResult 1つ前のアクションの戻り値
     * @param resource リソース
     * @return "create","send","save"の場合はWebSocketのクライアント、"close"の場合はnull
     */
    public Object execute(TestContext context, String actionId, Object preResult, Reader resource) throws Exception{
        BufferedReader br = new BufferedReader(resource);
        try{
            final String operateType = br.readLine();
            if("create".equals(operateType)) {
                Map connectParam = null;
                final String paramId = br.readLine();
                if(paramId == null || paramId.length() == 0){
                    if(preResult != null && (preResult instanceof Map)){
                        connectParam = (Map)preResult;
                    }
                } else {
                    Object actionResult = null;
                    if(paramId.indexOf(",") == -1){
                        actionResult = context.getTestActionResult(paramId);
                    }else{
                        String[] ids = paramId.split(",");
                        if(ids.length != 2){
                            throw new Exception("Illegal paramId format. id=" + paramId);
                        }
                        actionResult = context.getTestActionResult(ids[0], ids[1]);
                    }
                    if(actionResult == null){
                        throw new Exception("TestActionResult not found. id=" + paramId);
                    }
                    if(!(actionResult instanceof Map)){
                        throw new Exception("TestActionResult is not Map. result=" + actionResult);
                    }
                    HttpResponse response = (HttpResponse)((Map)actionResult).get("response");
                    connectParam = (Map)response.getObject();
                }
                if(connectParam == null) {
                    throw new Exception("WebSocketAuthResult not found.");
                } 
                String url = (String)connectParam.get("url");
                String id = (String)connectParam.get("id");
                String ticket = (String)connectParam.get("ticket");
                WebSocketContainer container = ContainerProvider.getWebSocketContainer();
                WSClient client = new WSClient(encoding);
                Session session = container.connectToServer(client, new URI(url + "?id=" + id + "&ticket=" + ticket));
                return client;
            } else if("close".equals(operateType) || "send".equals(operateType) || "save".equals(operateType)) {
                WSClient wsClient = null;
                final String clientId = br.readLine();
                if(clientId == null || clientId.length() == 0){
                    if(preResult != null && (preResult instanceof WSClient)){
                        wsClient = (WSClient)preResult;
                    }
                } else {
                    Object actionResult = null;
                    if(clientId.indexOf(",") == -1){
                        actionResult = context.getTestActionResult(clientId);
                    }else{
                        String[] ids = clientId.split(",");
                        if(ids.length != 2){
                            throw new Exception("Illegal clientId format. id=" + clientId);
                        }
                        actionResult = context.getTestActionResult(ids[0], ids[1]);
                    }
                    if(actionResult == null){
                        throw new Exception("TestActionResult not found. id=" + clientId);
                    }
                    if(!(actionResult instanceof WSClient)){
                        throw new Exception("TestActionResult is not WSClient. result=" + actionResult);
                    }
                    wsClient = (WSClient)actionResult;
                }
                if(wsClient == null) {
                    throw new Exception("WSClient not found.");
                }
                if("close".equals(operateType)) {
                    if(wsClient.getSession().isOpen()) {
                        wsClient.getSession().close();
                    }
                    wsClient = null;
                }else if("send".equals(operateType)) {
                    String message = null;
                    while((message = br.readLine()) != null && message.length() != 0){
                        wsClient.sendMessage(message);
                    }
                }else if("save".equals(operateType)) {
                    wsClient.writeReceiveMessages(new File(context.getCurrentDirectory(),actionId + ".receive"));
                }
                return wsClient;
            } else {
                throw new Exception("Illegal operateType. operateType=" + operateType);
            }
        }finally{
            br.close();
        }
    }
    
    public void setExpectedCost(double cost) {
        expectedCost = cost;
    }
    
    public double getExpectedCost() {
        return expectedCost;
    }

    @ClientEndpoint
    public static class WSClient {

        private Session session;
        private String encoding;
        private Throwable throwable;
        private CloseReason closeReason;
        
        List receiveMesssageList;
        List pongMesssageList;

        public Session getSession() {
            return session;
        }

        public String getEncoding() {
            return encoding;
        }

        public Throwable getThrowable() {
            return throwable;
        }

        public CloseReason getCloseReason() {
            return closeReason;
        }

        public List getReceiveMesssageList() {
            return receiveMesssageList;
        }

        public List getPongMesssageList() {
            return pongMesssageList;
        }

        public WSClient(String encoding) {
            this.encoding = encoding;
        }
        
        @OnOpen
        public void onOpen(Session session) {
            this.session = session;
            receiveMesssageList = new ArrayList();
            pongMesssageList = new ArrayList();
        }

        @OnMessage
        public void onMessage(PongMessage message) {
            pongMesssageList.add(new String(message.getApplicationData().array()));
        }

        @OnMessage
        public void onMessage(String message) {
            receiveMesssageList.add(message);
        }

        @OnMessage
        public void onMessage(ByteBuffer message) {
            try {
                String messageStr = new String(message.array(), encoding);
                receiveMesssageList.add(messageStr);
            } catch (UnsupportedEncodingException e) {
                // startServiceでチェックしているので起こらない。
            }
        }

        @OnError
        public void onError(Throwable throwable) {
            this.throwable = throwable;
        }

        @OnClose
        public void onClose(Session session, CloseReason reason) {
            this.closeReason = closeReason;
        }
        
        public void sendMessage(String message) throws Exception {
            if(session != null && session.isOpen()) {
                session.getBasicRemote().sendBinary(ByteBuffer.wrap(message.getBytes(encoding)));
            } else {
                throw new Exception("WebSocketSession is closed. closeReason=" + closeReason);
            }
        }

        public void writeReceiveMessages(File file) {
            PrintWriter pw = null;
            try{
                pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file))));
                for(int i = 0; i < receiveMesssageList.size(); i++) {
                    String message = (String)receiveMesssageList.get(i);
                    pw.println(message);
                }
                pw.flush();
            } catch(FileNotFoundException e) {
                ServiceManagerFactory.getLogger().write("TA___00001", new Object[] {file.getAbsolutePath()}, e);
            }finally{
                pw.close();
                pw = null;
            }
        }
    }

}
