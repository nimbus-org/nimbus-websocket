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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jp.ossc.nimbus.core.ServiceBase;
import jp.ossc.nimbus.util.crypt.CryptParameters;
import jp.ossc.nimbus.util.crypt.FalsifiedParameterException;
import jp.ossc.nimbus.util.crypt.OverLimitExpiresException;

/**
 * 認証サービス抽象クラス。
 * <p>
 *
 * @author M.Ishida
 */
public abstract class AbstractAuthenticatorService extends ServiceBase implements Authenticator, AbstractAuthenticatorServiceMBean {

    private static final long serialVersionUID = -1385665087771556138L;
    
    protected String idKey = DEFAULT_ID_KEY;
    protected String ticketKey = DEFAULT_TICKET_KEY;
    protected String wsTicketKey = DEFAULT_WS_TICKET_KEY;

    protected byte[] key = DEFAULT_KEY;
    protected String algorithm = DEFAULT_ALGORITHM;
    protected String transformation = DEFAULT_TRANSFORMATION;
    protected int ivLength = DEFAULT_IVLENGTH;
    protected String provider;
    protected String hashKey = DEFAULT_HASHKEY;
    protected long overLimitTime = -1;

    protected CryptParameters wsCipher;

    public String getIdKey() {
        return idKey;
    }

    public void setIdKey(String key) {
        idKey = key;
    }

    public String getTicketKey() {
        return ticketKey;
    }

    public void setTicketKey(String key) {
        ticketKey = key;
    }
    
    public String getWsTicketKey() {
        return wsTicketKey;
    }

    public void setWsTicketKey(String key) {
        wsTicketKey = key;
    }

    public byte[] getKey() {
        return key;
    }

    public void setKey(byte[] keyBytes) {
        key = keyBytes;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String paramAlgorithm) {
        algorithm = paramAlgorithm;
    }

    public String getTransformation() {
        return transformation;
    }

    public void setTransformation(String paramTransformation) {
        transformation = paramTransformation;
    }

    public int getIvLength() {
        return ivLength;
    }

    public void setIvLength(int length) {
        ivLength = length;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String paramProvider) {
        provider = paramProvider;
    }

    public String getHashKey() {
        return hashKey;
    }

    public void setHashKey(String hash) {
        hashKey = hash;
    }

    public long getOverLimitTime() {
        return overLimitTime;
    }

    public void setOverLimitTime(long time) {
        overLimitTime = time;
    }

    public void startService() throws Exception {
        wsCipher = new CryptParameters(key, algorithm, transformation, ivLength, provider, hashKey);
    }

    public AuthResult login(HttpServletRequest req, HttpServletResponse res) throws AuthenticateException {

        // 結果オブジェクト
        AuthResult result = new AuthResult();

        String id = req.getParameter(idKey);
        result.setId(id);
        String ticket = req.getParameter(ticketKey);
        // ID,チケットがnullの場合はNG
        if (id == null) {
            throw new AuthenticateException("id is null");
        }
        if (ticket == null) {
            throw new AuthenticateException("ticket is null");
        }
        Map map = wsCipher.createParametersMap();
        map.put(idKey, id);
        map.put(ticketKey, ticket);
        String wsTicket = wsCipher.encrypt(null, map);
        boolean loginResult = false;
        try {
            loginResult = login(id, ticket, wsTicket);
        } catch(Exception e) {
            throw new AuthenticateException(e);
        }
        if (!loginResult) {
            throw new AuthenticateException("Did not authenticated : " + id);
        }
        try {
            wsTicket = URLEncoder.encode(wsTicket, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new AuthenticateException(e);
        }
        result.setTicket(wsTicket);
        result.setResult(true);
        return result;
    }

    public boolean handshake(String id, String ticket) throws AuthenticateException {
        try {
            // ID,チケットがnullの場合はNG
            if (id == null) {
                throw new AuthenticateException("id is null");
            }
            if (ticket == null) {
                throw new AuthenticateException("ticket is null");
            }
            Map map = null;
            if (overLimitTime != -1) {
                map = wsCipher.decrypt(null, ticket, overLimitTime);
            } else {
                map = wsCipher.decrypt(null, ticket);
            }
            String mapId = (String) map.get(idKey);
            if (!id.equals(mapId)) {
                throw new AuthenticateException("ticket is incorrect value. id:" + id + " ticket:" + ticket);
            }
        } catch (OverLimitExpiresException e) {
            throw new AuthenticateException(e);
        } catch (FalsifiedParameterException e) {
            throw new AuthenticateException(e);
        }
        return true;
    }

    public void logout(String id, String ticket, boolean isForce) throws AuthenticateException {
        try {
            logout(id,ticket);
        } catch (Exception e) {
            throw new AuthenticateException(e);
        }
    }
    
    protected abstract boolean login(String id, String ticket, String wsTicket) throws Exception;
    
    protected abstract void logout(String id, String ticket) throws Exception;

}
