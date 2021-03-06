package jp.ossc.nimbus.service.publish.websocket;

public interface DefaultPublishMessageHandlerFactoryServiceMBean extends AbstractPublishMessageHandlerFactoryServiceMBean {
    
    public String getMessageParseErrorId();

    public void setMessageParseErrorId(String id);

    public String getMessageKeyAddErrorId();

    public void setMessageKeyAddErrorId(String id);

    public String getMessageKeyRemoveErrorId();

    public void setMessageKeyRemoveErrorId(String id);

    public String getMessageEncoding();

    public void setMessageEncoding(String encoding);

    public String getAddKeyString();

    public void setAddKeyString(String string);

    public String getDelKeyString();

    public void setDelKeyString(String string);

    public String getMessageSeparatorString();

    public void setMessageSeparatorString(String string);
}
