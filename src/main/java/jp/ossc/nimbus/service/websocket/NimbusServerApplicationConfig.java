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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpointConfig;
import javax.websocket.server.ServerEndpointConfig.Builder;
import javax.websocket.server.ServerEndpointConfig.Configurator;

import jp.ossc.nimbus.core.DeploymentException;
import jp.ossc.nimbus.core.ServiceManager;
import jp.ossc.nimbus.core.ServiceManagerFactory;
import jp.ossc.nimbus.core.ServiceMetaData;
import jp.ossc.nimbus.core.ServiceNotFoundException;
import jp.ossc.nimbus.core.Utility;

/**
 * アプリケーション起動時にWebSocketの設定を読み込むために処理されるクラス。
 * <p>
 * WebSocketのEndpointとパスを設定する。<br>
 * アプリケーション起動時に処理されるため、本クラス内で必要なNimbusサービスをロードしている。<br>
 * クラスパス上のservice-definition-xml.defにサービス定義のファイル一覧を記載することでサービスをロードすることが出来る。<br>
 * ロードされたサービス内からConfiguratorサービスを見つけWebSocketのEndpointとパスを設定する。<br>
 * <p>
 *
 * @author M.Ishida
 */
public class NimbusServerApplicationConfig implements ServerApplicationConfig {

    /**
     * サービス定義ファイルの一覧を記載する定義ファイル名。
     * <p>
     */
    public static final String SERVICE_DEFINITION_LIST = "service-definition.list";

    public NimbusServerApplicationConfig() throws IOException, DeploymentException {
        String[] serviceDefinitionFiles = getLoadTargetFileNames();
        for (int i = 0; i < serviceDefinitionFiles.length; i++) {
            boolean result = ServiceManagerFactory.loadManager(serviceDefinitionFiles[i], true, true);
            if (!result) {
                throw new DeploymentException("Service load faild. load file is " + serviceDefinitionFiles[i]);
            }
        }
        boolean isCompleted = ServiceManagerFactory.checkLoadManagerCompleted();
        if (!isCompleted) {
            throw new DeploymentException(
                    "Service load faild. ServiceManagerFactory.checkLoadManagerCompleted is false.");
        }
    }

    @Override
    public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> scanned) {

        Set result = new HashSet();

        ServiceManager[] managers = ServiceManagerFactory.findManagers();
        for (int i = 0; i < managers.length; i++) {
            Set serviceNameSet = managers[i].serviceNameSet();
            Iterator itr = serviceNameSet.iterator();
            while (itr.hasNext()) {
                Object name = itr.next();
                // サービスを生成するのではなく、Metaデータからクラスを取得する。
                // サービスを生成すると使用しない可能性のあるオブジェクトが生成されるため
                ServiceMetaData metaData = null;
                try {
                    metaData = managers[i].getServiceMetaData((String) name);
                } catch (ServiceNotFoundException e) {
                }
                if (metaData != null) {
                    try {
                        // Configuratorとしてサービスに登録されているものを見つける
                        Class serviceClass = Utility.convertStringToClass(metaData.getCode());
                        if (NimbusConfigurator.class.isAssignableFrom(serviceClass)
                                && Configurator.class.isAssignableFrom(serviceClass)) {
                            Object config = managers[i].getServiceObject((String) name);
                            Class endpoint = ((NimbusConfigurator) config).getEndpointClass();
                            if (scanned.contains(endpoint)) {
                                String path = ((NimbusConfigurator) config).getPath();
                                Builder builder = ServerEndpointConfig.Builder.create(endpoint, path);
                                builder.configurator((Configurator) config);
                                result.add(builder.build());
                            }

                        }
                    } catch (ClassNotFoundException e) {
                    }
                }
            }
        }
        return result;
    }

    @Override
    public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> scanned) {
        // アノテーションは未サポート
        return Collections.emptySet();
    }

    /**
     * クラスパス上にある「service-definition.list」ファイルから、サービス定義ファイルの一覧を取得する。
     *
     * @return サービス定義ファイルの一覧
     * @throws IOException
     */
    public static String[] getLoadTargetFileNames() throws IOException {
        List resultList = new ArrayList();
        InputStream is = NimbusServerApplicationConfig.class.getClassLoader().getResourceAsStream(
                SERVICE_DEFINITION_LIST);
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        try {
            String serviceFile = null;
            while ((serviceFile = br.readLine()) != null) {
                String filePath = serviceFile.trim();
                if(!"".equals(filePath) && !filePath.startsWith("#")) {
                    resultList.add(filePath);
                }
            }
        } finally {
            br.close();
            isr.close();
            is.close();
        }
        return (String[]) resultList.toArray(new String[0]);
    }

}
