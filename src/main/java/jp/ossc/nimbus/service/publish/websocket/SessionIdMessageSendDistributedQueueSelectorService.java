package jp.ossc.nimbus.service.publish.websocket;

import java.util.Set;

import jp.ossc.nimbus.service.queue.AbstractDistributedQueueSelectorService;

/**
 * メッセージを送信するためのDistributeQueueHandlerContainerのSelectorサービス。
 *
 * @author M.Ishida
 *
 */
public class SessionIdMessageSendDistributedQueueSelectorService extends AbstractDistributedQueueSelectorService
        implements SessionIdMessageSendDistributedQueueSelectorServiceMBean {

    private static final long serialVersionUID = 9116235892276821497L;

    protected Object getKey(Object obj) {
        MessageSendParameter param = (MessageSendParameter) obj;
        return param.getSender().getSession().getId();
    }

    public void remove(String sessionId) {
        synchronized (keyMap) {
            keyMap.remove(sessionId);
            for (int i = 0; i < keySets.length; i++) {
                Set keySet = keySets[i];
                keySet.remove(sessionId);
            }
        }
    }
}