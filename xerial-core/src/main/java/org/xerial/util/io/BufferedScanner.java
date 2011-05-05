/*--------------------------------------------------------------------------
 *  Copyright 2011 Taro L. Saito
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *--------------------------------------------------------------------------*/
//--------------------------------------
// XerialJ
//
// BufferedScanner.java
// Since: 2011/04/30 15:36:32
//
// $URL$
// $Author$
//--------------------------------------
package org.xerial.util.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.xerial.core.XerialErrorCode;
import org.xerial.core.XerialException;

/**
 * A character scanner which support character look-ahead and mark
 * functionalities.
 * 
 * @author leo
 * 
 */
public class BufferedScanner {
    private static interface Buffer {
        int length();

        int get(int index);

        int feed(int offset, int length) throws IOException;

        String toString(int offset, int length);

        void close() throws IOException;

        /**
         * Slide the buffer contents [offset, offset+len) to [0,.. len)
         * 
         * @param offset
         * @param len
         */
        void slide(int offset, int len);
    }

    private static class ByteBuffer implements Buffer {
        InputStream source;
        byte[] buffer;

        public ByteBuffer(InputStream source, int bufferSize) {
            this.source = source;
            this.buffer = new byte[bufferSize];
        }

        public ByteBuffer(byte[] buffer) {
            this.buffer = buffer;
        }

        @Override
        public int get(int index) {
            return buffer[index];
        }

        @Override
        public int length() {
            return buffer.length;
        }

        @Override
        public String toString(int offset, int length) {
            return new String(buffer, offset, length);
        }

        @Override
        public int feed(int offset, int length) throws IOException {
            if (source == null)
                return -1;
            return source.read(buffer, offset, length);
        }

        @Override
        public void close() throws IOException {
            if (source != null)
                source.close();

        }

        @Override
        public void slide(int offset, int len) {
            System.arraycopy(buffer, offset, buffer, 0, len);
        }
    }

    private static class CharBuffer implements Buffer {
        Reader source;
        char[] buffer;

        public CharBuffer(Reader source, int bufferSize) {
            this.source = source;
            this.buffer = new char[bufferSize];
        }

        @Override
        public int get(int index) {
            return buffer[index];
        }

        @Override
        public int length() {
            return buffer.length;
        }

        @Override
        public String toString(int offset, int length) {
            return new String(buffer, offset, length);
        }

        @Override
        public int feed(int offset, int length) throws IOException {
            if (source == null)
                return -1;
            return source.read(buffer, offset, length);
        }

        @Override
        public void close() throws IOException {
            if (source != null)
                source.close();

        }

        @Override
        public void slide(int offset, int len) {
            System.arraycopy(buffer, offset, buffer, 0, len);
        }

    }

    public final static int EOF = -1;

    private Buffer buffer;
    private int bufferLimit = 0;
    private boolean reachedEOF = false;

    private static class ScannerState {
        public int cursor = 0;
        public int lineNumber = 1;
        public int posInLine = 0;

        public ScannerState() {}

        public ScannerState(ScannerState m) {
            this.cursor = m.cursor;
            this.lineNumber = m.lineNumber;
            this.posInLine = m.posInLine;
        }
    }

    private ScannerState mark;
    private ScannerState current = new ScannerState();

    public BufferedScanner(InputStream in) {
        this(in, 8 * 1024); // 8kb buffer
    }

    public BufferedScanner(InputStream in, int bufferSize) {
        if (in == null)
            throw new NullPointerException("input is null");
        buffer = new ByteBuffer(in, bufferSize);
    }

    public BufferedScanner(Reader in) {
        this(in, 8 * 1024); // 8kb buffer
    }

    public BufferedScanner(Reader in, int bufferSize) {
        if (in == null)
            throw new NullPointerException("input is null");
        buffer = new CharBuffer(in, bufferSize);
    }

    public BufferedScanner(String s) {
        buffer = new ByteBuffer(s.getBytes());
        bufferLimit = buffer.length();
    }

    public void close() throws XerialException {
        try {
            buffer.close();
        }
        catch (IOException e) {
            throw new XerialException(XerialErrorCode.IO_EXCEPTION, e);
        }
    }

    public boolean hasReachedEOF() {
        return current.cursor >= bufferLimit && reachedEOF;
    }

    public void consume() throws XerialException {
        if (current.cursor >= bufferLimit) {
            if (!fill()) {
                // Do nothing
                return;
            }
        }

        int ch = buffer.get(current.cursor++);
        current.posInLine++;
        if (ch == '\n') {
            current.lineNumber++;
            current.posInLine = 0;
        }
    }

    /**
     * Get the next character of lookahead.
     * 
     * @param i
     * @return
     * @throws IOException
     */
    public int LA(int lookahead) throws XerialException {
        if (current.cursor + lookahead - 1 >= bufferLimit) {
            if (!fill()) {
                // No more character
                return EOF;
            }
        }

        return buffer.get(current.cursor + lookahead - 1);
    }

    private boolean fill() throws XerialException {
        if (reachedEOF) {
            return false;
        }

        // Move the [mark ... limit)
        if (mark != null) {
            int lenToPreserve = bufferLimit - mark.cursor;
            if (lenToPreserve < buffer.length()) {
                // Move [mark.cursor, limit) to the [0, ..., mark.cursor)
                if (lenToPreserve > 0)
                    buffer.slide(mark.cursor, lenToPreserve);
                bufferLimit = lenToPreserve;
                current.cursor -= mark.cursor;
                mark.cursor = 0;
            }
            else {
                // The buffer got too big, invalidate the mark
                mark = null;
                bufferLimit = 0;
                current.cursor = 0;
            }
        }
        else {
            bufferLimit = 0;
            current.cursor = 0;
        }

        // Read the data from the stream, and fill the buffer
        int readLen = buffer.length() - bufferLimit;
        try {
            int readBytes = buffer.feed(current.cursor, readLen);
            if (readBytes < readLen) {
                reachedEOF = true;
            }
            if (readBytes == -1)
                return false;
            else {
                bufferLimit += readBytes;
                return true;
            }
        }
        catch (IOException e) {
            throw new XerialException(XerialErrorCode.IO_EXCEPTION, e);
        }
    }

    public String selectedString() {
        if (mark == null)
            return null;

        int size = current.cursor - mark.cursor;
        return buffer.toString(mark.cursor, size);
    }

    public String selectedStringWithTrimming() {
        if (mark == null)
            return null;

        int begin = mark.cursor;
        int end = current.cursor;

        for (; begin < end; begin++) {
            int c = buffer.get(begin);
            if (c != ' ' && c != '\t' && c != '\r' && c != '\n')
                break;
        }
        for (; begin < end; end--) {
            int c = buffer.get(end - 1);
            if (c != ' ' && c != '\t' && c != '\r' && c != '\n')
                break;
        }
        if (begin >= end) {
            begin = end;
        }

        int size = end - begin;
        return buffer.toString(begin, size);
    }

    public String selectedString(int margin) {
        if (mark == null)
            return null;

        int begin = mark.cursor + margin;
        int end = current.cursor - margin;
        int size = end - begin;
        return buffer.toString(begin, size);
    }

    public void mark() {
        mark = new ScannerState(current);
    }

    /**
     * Reset the stream position to the last marker.
     */
    public void rewind() {
        current = mark;
        mark = null;
    }

    /**
     * Get the current line number. The first line number is 1.
     * 
     * @return
     */
    public int getLineNumber() {
        return current.lineNumber;
    }

    /**
     * Get the character position in the current line. The first character
     * position in line is 1.
     * 
     * @return
     */
    public int getPosInLine() {
        return current.posInLine;
    }
}