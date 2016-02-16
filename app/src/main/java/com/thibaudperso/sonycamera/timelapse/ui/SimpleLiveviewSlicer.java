/*
 * Copyright 2013 Sony Corporation
 */

package com.thibaudperso.sonycamera.timelapse.ui;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * A parser class for Liveview data Packet defined by Camera Remote API
 */
public class SimpleLiveviewSlicer {

    /**
     * Payload data class. See also Camera Remote API specification document to
     * know the data structure.
     */
    public static class Payload {
        /** jpeg data container */
        final public byte[] jpegData;

        /** padding data container */
        final public byte[] paddingData;

        /**
         * Constructor
         */
        private Payload(byte[] jpegData, byte[] paddingData) {
            this.jpegData = jpegData;
            this.paddingData = paddingData;
        }
    }

    private static final int CONNECTION_TIMEOUT = 2000; // [msec]

    private HttpURLConnection mHttpConn;
    private InputStream mInputStream;

    /**
     * Opens Liveview HTTP GET connection and prepares for reading Packet data.
     *
     * @param liveviewUrl Liveview data url that is obtained by DD.xml or result
     *            of startLiveview API.
     * @throws IOException generic errors or exception.
     */
    public void open(String liveviewUrl) throws IOException {
        if (mInputStream != null || mHttpConn != null) {
            throw new IllegalStateException("Slicer is already open.");
        }

        final URL _url = new URL(liveviewUrl);
        mHttpConn = (HttpURLConnection) _url.openConnection();
        mHttpConn.setRequestMethod("GET");
        mHttpConn.setConnectTimeout(CONNECTION_TIMEOUT);
        mHttpConn.connect();

        if (mHttpConn.getResponseCode() == HttpURLConnection.HTTP_OK) {
            mInputStream = mHttpConn.getInputStream();
        }
        if (mInputStream == null) {
            throw new IOException("open error: " + liveviewUrl);
        }
    }

    /**
     * Closes the connection.
     *
     * @throws IOException generic errors or exception.
     */
    public void close() throws IOException {
        if (mInputStream != null) {
            mInputStream.close();
            mInputStream = null;
        }
        if (mHttpConn != null) {
            mHttpConn.disconnect();
            mHttpConn = null;
        }
    }

    /**
     * Reads liveview stream and slice one Packet. If server is not ready for
     * liveview data, this API calling will be blocked until server returns next
     * data.
     *
     * @return Payload data of sliced Packet
     * @throws IOException generic errors or exception.
     */
    public Payload nextPayload() throws IOException {
        if (mInputStream != null) {
            // Common Header
            int readLength = 1 + 1 + 2 + 4;
            byte[] commonHeader = readBytes(mInputStream, readLength);
            if (commonHeader == null || commonHeader.length != readLength) {
                throw new IOException("Cannot read stream for common header.");
            }
            if (commonHeader[0] != (byte) 0xFF) {
                throw new IOException("Unexpected data format. (Start byte)");
            }
            if (commonHeader[1] != (byte) 0x01) {
                throw new IOException("Unexpected data format. (Payload byte)");
            }

            // Payload Header
            readLength = 4 + 3 + 1 + 4 + 1 + 115;
            byte[] payloadHeader = readBytes(mInputStream, readLength);
            if (payloadHeader == null || payloadHeader.length != readLength) {
                throw new IOException("Cannot read stream for payload header.");
            }
            if (payloadHeader[0] != (byte) 0x24
                    || payloadHeader[1] != (byte) 0x35
                    || payloadHeader[2] != (byte) 0x68
                    || payloadHeader[3] != (byte) 0x79) {
                throw new IOException("Unexpected data format. (Start code)");
            }
            int jpegSize = bytesToInt(payloadHeader, 4, 3);
            int paddingSize = bytesToInt(payloadHeader, 7, 1);

            // Payload Data
            byte[] jpegData = readBytes(mInputStream, jpegSize);
            byte[] paddingData = readBytes(mInputStream, paddingSize);

            return new Payload(jpegData, paddingData);
        }
        return null;
    }

    // Converts byte array to int.
    private static int bytesToInt(byte[] byteData, int startIndex, int count) {
        int ret = 0;
        for (int i = startIndex; i < startIndex + count; i++) {
            ret = (ret << 8) | (byteData[i] & 0xff);
        }
        return ret;
    }

    // Reads byte array from the indicated input stream.
    private static byte[] readBytes(InputStream in, int length)
            throws IOException {
        ByteArrayOutputStream tmpByteArray = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        while (true) {
            int trialReadlen = Math.min(buffer.length,
                    length - tmpByteArray.size());
            int readlen = in.read(buffer, 0, trialReadlen);
            if (readlen < 0) {
                break;
            }
            tmpByteArray.write(buffer, 0, readlen);
            if (length <= tmpByteArray.size()) {
                break;
            }
        }
        byte[] ret = tmpByteArray.toByteArray();
        tmpByteArray.close();
        return ret;
    }
}
