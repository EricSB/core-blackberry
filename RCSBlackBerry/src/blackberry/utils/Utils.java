/* *************************************************
 * Copyright (c) 2010 - 2010
 * HT srl,   All rights reserved.
 * Project      : RCS, RCSBlackBerry_lib
 * File         : Utils.java
 * Created      : 26-mar-2010
 * *************************************************/
package blackberry.utils;

import java.io.EOFException;
import java.util.Date;
import java.util.Vector;

import net.rim.device.api.system.GPRSInfo;
import net.rim.device.api.util.Arrays;
import net.rim.device.api.util.DataBuffer;
import net.rim.device.api.util.NumberUtilities;

// TODO: Auto-generated Javadoc
/**
 * The Class Utils.
 */
public final class Utils {

    /** The debug instance. */
    //#debug
    private static Debug debug = new Debug("Utils", DebugLevel.VERBOSE);

    /**
     * ASCII.
     * 
     * @param c
     *            the c
     * @return the char
     */
    public static char ascii(final int c) {
        return (char) ((c) <= 9 ? (c) + '0' : (c) + 'A' - 0xA);
    };

    /**
     * Byte array to char array.
     * 
     * @param message
     *            the message
     * @return the char[]
     */
    public static char[] byteArrayToCharArray(final byte[] message) {
        final char[] payload = new char[message.length];

        for (int i = 0; i < message.length; i++) {
            payload[i] = (char) (message[i] & 0xFF);
        }

        return payload;
    }

    /**
     * Byte array to hex.
     * 
     * @param data
     *            the data
     * @return the string
     */
    public static String byteArrayToHex(final byte[] data) {
        return byteArrayToHex(data, 0, data.length);
    }

    /**
     * Converte un array di byte in una stringa che ne rappresenta il contenuto
     * in formato esadecimale.
     * 
     * @param data
     *            the data
     * @param offset
     *            the offset
     * @param length
     *            the length
     * @return the string
     */
    public static String byteArrayToHex(final byte[] data, final int offset,
            final int length) {
        final StringBuffer buf = new StringBuffer();
        for (int i = offset; i < offset + length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int twohalfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) {
                    buf.append((char) ('0' + halfbyte));
                } else {
                    buf.append((char) ('a' + (halfbyte - 10)));
                }
                halfbyte = data[i] & 0x0F;
            } while (twohalfs++ < 1);
        }
        return buf.toString();
    }

    /**
     * Byte array to int.
     * 
     * @param buffer
     *            the buffer
     * @param offset
     *            the offset
     * @return the int
     */
    public static int byteArrayToInt(final byte[] buffer, final int offset) {

        // #ifdef DBC
        Check.requires(buffer.length >= offset + 4, "short buffer");
        // #endif

        final DataBuffer databuffer = new DataBuffer(buffer, offset, 4, false);
        int value = 0;

        try {
            value = databuffer.readInt();
        } catch (final EOFException e) {
            // #debug
            debug.error("Cannot read int from buffer at offset:" + offset);
        }

        return value;

    }

    /**
     * Byte array to long.
     * 
     * @param buffer
     *            the buffer
     * @param offset
     *            the offset
     * @return the long
     */
    public static long byteArrayToLong(final byte[] buffer,
            final int offset) {

        // #ifdef DBC
        Check.requires(buffer.length >= offset + 8, "short buffer");
        // #endif

        final DataBuffer databuffer = new DataBuffer(buffer, offset, 8, false);
        long value = 0;

        try {
            value = databuffer.readLong();
        } catch (final EOFException e) {
            // #debug
            debug.error("Cannot read int from buffer at offset:" + offset);
        }

        return value;

    }

    /**
     * Char array to byte array.
     * 
     * @param message
     *            the message
     * @return the byte[]
     */
    public static byte[] charArrayToByteArray(final char[] message) {
        final byte[] payload = new byte[message.length];

        for (int i = 0; i < message.length; i++) {
            payload[i] = (byte) (message[i] & 0xFF);
        }

        return payload;
    }

    /**
     * Copy.
     * 
     * @param dest
     *            the dest
     * @param src
     *            the src
     * @param len
     *            the len
     */
    public static void copy(final byte[] dest, final byte[] src, final int len) {
        copy(dest, 0, src, 0, len);
    }

    /**
     * Copy.
     * 
     * @param dest
     *            the dest
     * @param offsetDest
     *            the offset dest
     * @param src
     *            the src
     * @param offsetSrc
     *            the offset src
     * @param len
     *            the len
     */
    public static void copy(final byte[] dest, final int offsetDest,
            final byte[] src, final int offsetSrc, final int len) {
        // #ifdef DBC
        Check.requires(dest.length >= offsetDest + len, "wrong dest len");
        Check.requires(src.length >= offsetSrc + len, "wrong src len");
        // #endif

        for (int i = 0; i < len; i++) {
            dest[i + offsetDest] = src[i + offsetSrc];
        }
    }

    /**
     * Crc.
     * 
     * @param buffer
     *            the buffer
     * @return the int
     */
    public static int crc(final byte[] buffer) {
        return crc(buffer, 0, buffer.length);
    }

    /**
     * Crc.
     * 
     * @param buffer
     *            the buffer
     * @param start
     *            the start
     * @param len
     *            the len
     * @return the int
     */
    public static int crc(final byte[] buffer, final int start, final int len) {
        // CRC
        int confHash;
        long tempHash = 0;

        for (int i = start; i < (len - start); i++) {
            tempHash++;

            final byte b = buffer[i];

            if (b != 0) {
                tempHash *= b;
            }

            confHash = (int) (tempHash >> 32);

            tempHash = tempHash & 0xFFFFFFFFL;
            tempHash ^= confHash;
            tempHash = tempHash & 0xFFFFFFFFL;

            //debug.trace(i+" b: "+b+" temphash:" + tempHash);
        }

        confHash = (int) tempHash;
        //#debug debug
        debug.trace("confhash:" + confHash);
        return confHash;
    }

    /**
     * Crc.
     * 
     * @param buffer
     *            the buffer
     * @return the int
     */
    public static int crc(final char[] buffer) {
        return crc(charArrayToByteArray(buffer), 0, buffer.length);
    }

    /**
     * Date diff.
     * 
     * @param after
     *            the after
     * @param before
     *            the before
     * @return the long
     */
    public static long dateDiff(final Date after, final Date before) {
        final long ret = after.getTime() - before.getTime();
        return ret;
    }

    /**
     * Dbg trace.
     * 
     * @param e
     *            the e
     */
    public static void dbgTrace(final Exception e) {
        e.printStackTrace();
    }

    /**
     * Gets the bit.
     * 
     * @param value
     *            the value
     * @param i
     *            the i
     * @return the bit
     */
    public static boolean getBit(final int value, final int i) {
        final boolean ret = ((value >> i) & 0x01) == 1;
        return ret;
    }

    /**
     * Gets the index.
     * 
     * @param buffer
     *            the buffer
     * @param token
     *            the token
     * @return the index
     */
    public static int getIndex(final byte[] buffer, final byte[] token) {
        int pos = -1;

        for (int i = 0; i < buffer.length; i++) {
            if (Arrays.equals(buffer, i, token, 0, token.length)) {
                pos = i;
                break;
            }
        }

        return pos;
    }

    /**
     * Gets the index.
     * 
     * @param buffer
     *            the buffer
     * @param message
     *            the message
     * @return the index
     */
    public static int getIndex(final char[] buffer, final String message) {
        final char[] token = new char[message.length()];

        message.getChars(0, message.length(), token, 0);

        int pos = -1;

        for (int i = 0; i < buffer.length; i++) {
            if (Arrays.equals(buffer, i, token, 0, token.length)) {
                pos = i;
                break;
            }
        }

        return pos;
    }

    /**
     * Gets the time.
     * 
     * @return the time
     */
    public static long getTime() {
        return System.currentTimeMillis();
    }

    /**
     * Definizione delle funzioni helper comuni.
     * 
     * @param c
     *            the c
     * @return the int
     */
    public static int hex(final char c) {
        final int ret = (char) ((c) <= '9' ? (c) - '0'
                : (c) <= 'F' ? (c) - 'A' + 0xA : (c) - 'a' + 0xA);
        return ret;
    }

    /**
     * Hex string to byte array.
     * 
     * @param wchar
     *            the wchar
     * @return the byte[]
     */
    public static byte[] hexStringToByteArray(final String wchar) {
        // #ifdef DBC
        Check.requires(wchar.length() % 2 == 0, "Odd input");
        // #endif
        final byte[] ret = new byte[wchar.length() / 2];

        for (int i = 0; i < ret.length; i++) {
            final char first = wchar.charAt(i * 2);
            final char second = wchar.charAt(i * 2 + 1);

            int value = NumberUtilities.hexDigitToInt(first) << 4;
            value += NumberUtilities.hexDigitToInt(second);

            // #ifdef DBC
            Check.asserts(value >= 0 && value < 256,
                    "HexStringToByteArray: wrong value");
            // #endif

            ret[i] = (byte) value;
        }

        return ret;
    }

    /**
     * Imei to string.
     * 
     * @param imei
     *            the imei
     * @return the string
     */
    public static String imeiToString(final byte[] imei) {
        final String imeiString = GPRSInfo.imeiToString(imei);
        return imeiString.replace('.', '0');
    }

    /**
     * Int to byte array.
     * 
     * @param value
     *            the value
     * @return the byte[]
     */
    public static byte[] intToByteArray(final int value) {
        final byte[] result = new byte[4];
        final DataBuffer databuffer = new DataBuffer(result, 0, 4, false);
        databuffer.writeInt(value);
        return result;
    }

    /**
     * Int to char array.
     * 
     * @param value
     *            the value
     * @return the char[]
     */
    public static char[] intToCharArray(final int value) {
        return byteArrayToCharArray(intToByteArray(value));
    }

    /**
     * Join string.
     * 
     * @param nodes
     *            the nodes
     * @return the string
     */
    public static String joinString(final Vector nodes) {
        final StringBuffer sb = new StringBuffer();
        final int nsize = nodes.size();
        for (int i = 0; i < nsize; i++) {
            sb.append((String) nodes.elementAt(i));
        }
        return sb.toString();
    }

    /**
     * Long to byte array.
     * 
     * @param value
     *            the value
     * @return the byte[]
     */
    public static byte[] longToByteArray(final long value) {
        final byte[] result = new byte[8];
        final DataBuffer databuffer = new DataBuffer(result, 0, 8, false);
        databuffer.writeLong(value);
        return result;
    }

    /**
     * Sleep.
     * 
     * @param millis
     *            the millis
     */
    public static void sleep(final int millis) {
        try {
            //Date timestamp = new Date();
            Thread.sleep(millis);
            //long elapsed = (new Date()).getTime() - timestamp.getTime();

            /*
             * if (elapsed > millis * 2) {
             * debug.error("slept " + elapsed + " instead of:" + millis +
             * " thread: " + Thread.currentThread().getName()); }
             */

            //Thread.yield();
        } catch (final InterruptedException e) {
            // #debug
            debug.error("sleep interrupted!");
        }
    }

    /**
     * Split string.
     * 
     * @param original
     *            the original
     * @param separators
     *            the separators
     * @return the vector
     */
    public static Vector splitString(String original, final String separators) {
        final Vector nodes = new Vector();
        // Parse nodes into vector
        int index = original.indexOf(separators);
        while (index >= 0) {
            nodes.addElement(original.substring(0, index));
            original = original.substring(index + separators.length());
            index = original.indexOf(separators);
        }
        // Get the last node
        nodes.addElement(original);

        return nodes;
    }

    private Utils() {
    }

}