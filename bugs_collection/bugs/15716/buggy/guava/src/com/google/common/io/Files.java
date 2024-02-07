/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.io;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.io.FileWriteMode.APPEND;

import com.google.common.annotations.Beta;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.Checksum;

/**
 * Provides utility methods for working with files.
 *
 * <p>All method parameters must be non-null unless documented otherwise.
 *
 * @author Chris Nokleberg
 * @author Colin Decker
 * @since 1.0
 */
@Beta
public final class Files {

  /** Maximum loop count when creating temp directories. */
  private static final int TEMP_DIR_ATTEMPTS = 10000;

  private Files() {}

  /**
   * Returns a buffered reader that reads from a file using the given
   * character set.
   *
   * @param file the file to read from
   * @param charset the charset used to decode the input stream; see {@link
   *     Charsets} for helpful predefined constants
   * @return the buffered reader
   */
  public static BufferedReader newReader(File file, Charset charset)
      throws FileNotFoundException {
    checkNotNull(file);
    checkNotNull(charset);
    return new BufferedReader(
        new InputStreamReader(new FileInputStream(file), charset));
  }

  /**
   * Returns a buffered writer that writes to a file using the given
   * character set.
   *
   * @param file the file to write to
   * @param charset the charset used to encode the output stream; see {@link
   *     Charsets} for helpful predefined constants
   * @return the buffered writer
   */
  public static BufferedWriter newWriter(File file, Charset charset)
      throws FileNotFoundException {
    checkNotNull(file);
    checkNotNull(charset);
    return new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(file), charset));
  }

  /**
   * Returns a new {@link ByteSource} for reading bytes from the given file.
   *
   * @since 14.0
   */
  public static ByteSource asByteSource(File file) {
    return new FileByteSource(file);
  }

  private static final class FileByteSource extends ByteSource {

    private final File file;

    private FileByteSource(File file) {
      this.file = checkNotNull(file);
    }

    @Override
    public FileInputStream openStream() throws IOException {
      return new FileInputStream(file);
    }

    @Override
    public long size() throws IOException {
      if (!file.isFile()) {
        throw new FileNotFoundException(file.toString());
      }
      return file.length();
    }

    @Override
    public byte[] read() throws IOException {
      long size = file.length();
      // some special files may return size 0 but have content
      // read normally to be sure
      if (size == 0) {
        return super.read();
      }

      // can't initialize a large enough array
      // technically, this could probably be Integer.MAX_VALUE - 5
      if (size > Integer.MAX_VALUE) {
        // OOME is what would be thrown if we tried to initialize the array
        throw new OutOfMemoryError("file is too large to fit in a byte array: "
            + size + " bytes");
      }

      // initialize the array to the current size of the file
      byte[] bytes = new byte[(int) size];

      Closer closer = Closer.create();
      try {
        InputStream in = closer.register(openStream());
        int off = 0;
        int read = 0;

        // read until we've read size bytes or reached EOF
        while (off < size
            && ((read = in.read(bytes, off, (int) size - off)) != -1)) {
          off += read;
        }

        byte[] result = bytes;

        if (off < size) {
          // encountered EOF early; truncate the result
          result = Arrays.copyOf(bytes, off);
        } else if (read != -1) {
          // we read size bytes... if the last read didn't return -1, the file got larger
          // so we just read the rest normally and then create a new array
          ByteArrayOutputStream out = new ByteArrayOutputStream();
          ByteStreams.copy(in, out);
          byte[] moreBytes = out.toByteArray();
          result = new byte[bytes.length + moreBytes.length];
          System.arraycopy(bytes, 0, result, 0, bytes.length);
          System.arraycopy(moreBytes, 0, result, bytes.length, moreBytes.length);
        }
        // normally, off should == size and read should == -1
        // in that case, the array is just returned as is
        return result;
      } catch (Throwable e) {
        throw closer.rethrow(e);
      } finally {
        closer.close();
      }
    }

    @Override
    public String toString() {
      return "Files.newByteSource(" + file + ")";
    }
  }

  /**
   * Returns a new {@link ByteSink} for writing bytes to the given file. The
   * given {@code modes} control how the file is opened for writing. When no
   * mode is provided, the file will be truncated before writing. When the
   * {@link FileWriteMode#APPEND APPEND} mode is provided, writes will
   * append to the end of the file without truncating it.
   *
   * @since 14.0
   */
  public static ByteSink asByteSink(File file, FileWriteMode... modes) {
    return new FileByteSink(file, modes);
  }

  private static final class FileByteSink extends ByteSink {

    private final File file;
    private final ImmutableSet<FileWriteMode> modes;

    private FileByteSink(File file, FileWriteMode... modes) {
      this.file = checkNotNull(file);
      this.modes = ImmutableSet.copyOf(modes);
    }

    @Override
    public FileOutputStream openStream() throws IOException {
      return new FileOutputStream(file, modes.contains(APPEND));
    }

    @Override
    public String toString() {
      return "Files.newByteSink(" + file + ", " + modes + ")";
    }
  }

  /**
   * Returns a new {@link CharSource} for reading character data from the given
   * file using the given character set.
   *
   * @since 14.0
   */
  public static CharSource asCharSource(File file, Charset charset) {
    return asByteSource(file).asCharSource(charset);
  }

  /**
   * Returns a new {@link CharSink} for writing character data to the given
   * file using the given character set. The given {@code modes} control how
   * the file is opened for writing. When no mode is provided, the file
   * will be truncated before writing. When the
   * {@link FileWriteMode#APPEND APPEND} mode is provided, writes will
   * append to the end of the file without truncating it.
   *
   * @since 14.0
   */
  public static CharSink asCharSink(File file, Charset charset,
      FileWriteMode... modes) {
    return asByteSink(file, modes).asCharSink(charset);
  }

  /**
   * Returns a factory that will supply instances of {@link FileInputStream}
   * that read from a file.
   *
   * @param file the file to read from
   * @return the factory
   */
  public static InputSupplier<FileInputStream> newInputStreamSupplier(
      final File file) {
    return ByteStreams.asInputSupplier(asByteSource(file));
  }

  /**
   * Returns a factory that will supply instances of {@link FileOutputStream}
   * that write to a file.
   *
   * @param file the file to write to
   * @return the factory
   */
  public static OutputSupplier<FileOutputStream> newOutputStreamSupplier(
      File file) {
    return newOutputStreamSupplier(file, false);
  }

  /**
   * Returns a factory that will supply instances of {@link FileOutputStream}
   * that write to or append to a file.
   *
   * @param file the file to write to
   * @param append if true, the encoded characters will be appended to the file;
   *     otherwise the file is overwritten
   * @return the factory
   */
  public static OutputSupplier<FileOutputStream> newOutputStreamSupplier(
      final File file, final boolean append) {
    return ByteStreams.asOutputSupplier(asByteSink(file, modes(append)));
  }

  private static FileWriteMode[] modes(boolean append) {
    return append
        ? new FileWriteMode[]{ FileWriteMode.APPEND }
        : new FileWriteMode[0];
  }

  /**
   * Returns a factory that will supply instances of
   * {@link InputStreamReader} that read a file using the given character set.
   *
   * @param file the file to read from
   * @param charset the charset used to decode the input stream; see {@link
   *     Charsets} for helpful predefined constants
   * @return the factory
   */
  public static InputSupplier<InputStreamReader> newReaderSupplier(File file,
      Charset charset) {
    return CharStreams.asInputSupplier(asCharSource(file, charset));
  }

  /**
   * Returns a factory that will supply instances of {@link OutputStreamWriter}
   * that write to a file using the given character set.
   *
   * @param file the file to write to
   * @param charset the charset used to encode the output stream; see {@link
   *     Charsets} for helpful predefined constants
   * @return the factory
   */
  public static OutputSupplier<OutputStreamWriter> newWriterSupplier(File file,
      Charset charset) {
    return newWriterSupplier(file, charset, false);
  }

  /**
   * Returns a factory that will supply instances of {@link OutputStreamWriter}
   * that write to or append to a file using the given character set.
   *
   * @param file the file to write to
   * @param charset the charset used to encode the output stream; see {@link
   *     Charsets} for helpful predefined constants
   * @param append if true, the encoded characters will be appended to the file;
   *     otherwise the file is overwritten
   * @return the factory
   */
  public static OutputSupplier<OutputStreamWriter> newWriterSupplier(File file,
      Charset charset, boolean append) {
    return CharStreams.asOutputSupplier(asCharSink(file, charset, modes(append)));
  }

  /**
   * Reads all bytes from a file into a byte array.
   *
   * @param file the file to read from
   * @return a byte array containing all the bytes from file
   * @throws IllegalArgumentException if the file is bigger than the largest
   *     possible byte array (2^31 - 1)
   * @throws IOException if an I/O error occurs
   */
  public static byte[] toByteArray(File file) throws IOException {
    return asByteSource(file).read();
  }

  /**
   * Reads all characters from a file into a {@link String}, using the given
   * character set.
   *
   * @param file the file to read from
   * @param charset the charset used to decode the input stream; see {@link
   *     Charsets} for helpful predefined constants
   * @return a string containing all the characters from the file
   * @throws IOException if an I/O error occurs
   */
  public static String toString(File file, Charset charset) throws IOException {
    return asCharSource(file, charset).read();
  }

  /**
   * Copies to a file all bytes from an {@link InputStream} supplied by a
   * factory.
   *
   * @param from the input factory
   * @param to the destination file
   * @throws IOException if an I/O error occurs
   */
  public static void copy(InputSupplier<? extends InputStream> from, File to)
      throws IOException {
    ByteStreams.asByteSource(from).copyTo(asByteSink(to));
  }

  /**
   * Overwrites a file with the contents of a byte array.
   *
   * @param from the bytes to write
   * @param to the destination file
   * @throws IOException if an I/O error occurs
   */
  public static void write(byte[] from, File to) throws IOException {
    asByteSink(to).write(from);
  }

  /**
   * Copies all bytes from a file to an {@link OutputStream} supplied by
   * a factory.
   *
   * @param from the source file
   * @param to the output factory
   * @throws IOException if an I/O error occurs
   */
  public static void copy(File from, OutputSupplier<? extends OutputStream> to)
      throws IOException {
    asByteSource(from).copyTo(ByteStreams.asByteSink(to));
  }

  /**
   * Copies all bytes from a file to an output stream.
   *
   * @param from the source file
   * @param to the output stream
   * @throws IOException if an I/O error occurs
   */
  public static void copy(File from, OutputStream to) throws IOException {
    asByteSource(from).copyTo(to);
  }

  /**
   * Copies all the bytes from one file to another.
   *
   * <p><b>Warning:</b> If {@code to} represents an existing file, that file
   * will be overwritten with the contents of {@code from}. If {@code to} and
   * {@code from} refer to the <i>same</i> file, the contents of that file
   * will be deleted.
   *
   * @param from the source file
   * @param to the destination file
   * @throws IOException if an I/O error occurs
   * @throws IllegalArgumentException if {@code from.equals(to)}
   */
  public static void copy(File from, File to) throws IOException {
    checkArgument(!from.equals(to),
        "Source %s and destination %s must be different", from, to);
    asByteSource(from).copyTo(asByteSink(to));
  }

  /**
   * Copies to a file all characters from a {@link Readable} and
   * {@link Closeable} object supplied by a factory, using the given
   * character set.
   *
   * @param from the readable supplier
   * @param to the destination file
   * @param charset the charset used to encode the output stream; see {@link
   *     Charsets} for helpful predefined constants
   * @throws IOException if an I/O error occurs
   */
  public static <R extends Readable & Closeable> void copy(
      InputSupplier<R> from, File to, Charset charset) throws IOException {
    CharStreams.asCharSource(from).copyTo(asCharSink(to, charset));
  }

  /**
   * Writes a character sequence (such as a string) to a file using the given
   * character set.
   *
   * @param from the character sequence to write
   * @param to the destination file
   * @param charset the charset used to encode the output stream; see {@link
   *     Charsets} for helpful predefined constants
   * @throws IOException if an I/O error occurs
   */
  public static void write(CharSequence from, File to, Charset charset)
      throws IOException {
    asCharSink(to, charset).write(from);
  }

  /**
   * Appends a character sequence (such as a string) to a file using the given
   * character set.
   *
   * @param from the character sequence to append
   * @param to the destination file
   * @param charset the charset used to encode the output stream; see {@link
   *     Charsets} for helpful predefined constants
   * @throws IOException if an I/O error occurs
   */
  public static void append(CharSequence from, File to, Charset charset)
      throws IOException {
    write(from, to, charset, true);
  }

  /**
   * Private helper method. Writes a character sequence to a file,
   * optionally appending.
   *
   * @param from the character sequence to append
   * @param to the destination file
   * @param charset the charset used to encode the output stream; see {@link
   *     Charsets} for helpful predefined constants
   * @param append true to append, false to overwrite
   * @throws IOException if an I/O error occurs
   */
  private static void write(CharSequence from, File to, Charset charset,
      boolean append) throws IOException {
    asCharSink(to, charset, modes(append)).write(from);
  }

  /**
   * Copies all characters from a file to a {@link Appendable} &
   * {@link Closeable} object supplied by a factory, using the given
   * character set.
   *
   * @param from the source file
   * @param charset the charset used to decode the input stream; see {@link
   *     Charsets} for helpful predefined constants
   * @param to the appendable supplier
   * @throws IOException if an I/O error occurs
   */
  public static <W extends Appendable & Closeable> void copy(File from,
      Charset charset, OutputSupplier<W> to) throws IOException {
    asCharSource(from, charset).copyTo(CharStreams.asCharSink(to));
  }

  /**
   * Copies all characters from a file to an appendable object,
   * using the given character set.
   *
   * @param from the source file
   * @param charset the charset used to decode the input stream; see {@link
   *     Charsets} for helpful predefined constants
   * @param to the appendable object
   * @throws IOException if an I/O error occurs
   */
  public static void copy(File from, Charset charset, Appendable to)
      throws IOException {
    asCharSource(from, charset).copyTo(to);
  }

  /**
   * Returns true if the files contains the same bytes.
   *
   * @throws IOException if an I/O error occurs
   */
  public static boolean equal(File file1, File file2) throws IOException {
    checkNotNull(file1);
    checkNotNull(file2);
    if (file1 == file2 || file1.equals(file2)) {
      return true;
    }

    /*
     * Some operating systems may return zero as the length for files
     * denoting system-dependent entities such as devices or pipes, in
     * which case we must fall back on comparing the bytes directly.
     */
    long len1 = file1.length();
    long len2 = file2.length();
    if (len1 != 0 && len2 != 0 && len1 != len2) {
      return false;
    }
    return asByteSource(file1).contentEquals(asByteSource(file2));
  }

  /**
   * Atomically creates a new directory somewhere beneath the system's
   * temporary directory (as defined by the {@code java.io.tmpdir} system
   * property), and returns its name.
   *
   * <p>Use this method instead of {@link File#createTempFile(String, String)}
   * when you wish to create a directory, not a regular file.  A common pitfall
   * is to call {@code createTempFile}, delete the file and create a
   * directory in its place, but this leads a race condition which can be
   * exploited to create security vulnerabilities, especially when executable
   * files are to be written into the directory.
   *
   * <p>This method assumes that the temporary volume is writable, has free
   * inodes and free blocks, and that it will not be called thousands of times
   * per second.
   *
   * @return the newly-created directory
   * @throws IllegalStateException if the directory could not be created
   */
  public static File createTempDir() {
    File baseDir = new File(System.getProperty("java.io.tmpdir"));
    String baseName = System.currentTimeMillis() + "-";

    for (int counter = 0; counter < TEMP_DIR_ATTEMPTS; counter++) {
      File tempDir = new File(baseDir, baseName + counter);
      if (tempDir.mkdir()) {
        return tempDir;
      }
    }
    throw new IllegalStateException("Failed to create directory within "
        + TEMP_DIR_ATTEMPTS + " attempts (tried "
        + baseName + "0 to " + baseName + (TEMP_DIR_ATTEMPTS - 1) + ')');
  }

  /**
   * Creates an empty file or updates the last updated timestamp on the
   * same as the unix command of the same name.
   *
   * @param file the file to create or update
   * @throws IOException if an I/O error occurs
   */
  public static void touch(File file) throws IOException {
    checkNotNull(file);
    if (!file.createNewFile()
        && !file.setLastModified(System.currentTimeMillis())) {
      throw new IOException("Unable to update modification time of " + file);
    }
  }

  /**
   * Creates any necessary but nonexistent parent directories of the specified
   * file. Note that if this operation fails it may have succeeded in creating
   * some (but not all) of the necessary parent directories.
   *
   * @throws IOException if an I/O error occurs, or if any necessary but
   *     nonexistent parent directories of the specified file could not be
   *     created.
   * @since 4.0
   */
  public static void createParentDirs(File file) throws IOException {
    checkNotNull(file);
    File parent = file.getCanonicalFile().getParentFile();
    if (parent == null) {
      /*
       * The given directory is a filesystem root. All zero of its ancestors
       * exist. This doesn't mean that the root itself exists -- consider x:\ on
       * a Windows machine without such a drive -- or even that the caller can
       * create it, but this method makes no such guarantees even for non-root
       * files.
       */
      return;
    }
    parent.mkdirs();
    if (!parent.isDirectory()) {
      throw new IOException("Unable to create parent directories of " + file);
    }
  }

  /**
   * Moves the file from one path to another. This method can rename a file or
   * move it to a different directory, like the Unix {@code mv} command.
   *
   * @param from the source file
   * @param to the destination file
   * @throws IOException if an I/O error occurs
   * @throws IllegalArgumentException if {@code from.equals(to)}
   */
  public static void move(File from, File to) throws IOException {
    checkNotNull(from);
    checkNotNull(to);
    checkArgument(!from.equals(to),
        "Source %s and destination %s must be different", from, to);

    if (!from.renameTo(to)) {
      copy(from, to);
      if (!from.delete()) {
        if (!to.delete()) {
          throw new IOException("Unable to delete " + to);
        }
        throw new IOException("Unable to delete " + from);
      }
    }
  }

  /**
   * Reads the first line from a file. The line does not include
   * line-termination characters, but does include other leading and
   * trailing whitespace.
   *
   * @param file the file to read from
   * @param charset the charset used to decode the input stream; see {@link
   *     Charsets} for helpful predefined constants
   * @return the first line, or null if the file is empty
   * @throws IOException if an I/O error occurs
   */
  public static String readFirstLine(File file, Charset charset)
      throws IOException {
    return asCharSource(file, charset).readFirstLine();
  }

  /**
   * Reads all of the lines from a file. The lines do not include
   * line-termination characters, but do include other leading and
   * trailing whitespace.
   *
   * @param file the file to read from
   * @param charset the charset used to decode the input stream; see {@link
   *     Charsets} for helpful predefined constants
   * @return a mutable {@link List} containing all the lines
   * @throws IOException if an I/O error occurs
   */
  public static List<String> readLines(File file, Charset charset)
      throws IOException {
    return CharStreams.readLines(Files.newReaderSupplier(file, charset));
  }

  /**
   * Streams lines from a {@link File}, stopping when our callback returns
   * false, or we have read all of the lines.
   *
   * @param file the file to read from
   * @param charset the charset used to decode the input stream; see {@link
   *     Charsets} for helpful predefined constants
   * @param callback the {@link LineProcessor} to use to handle the lines
   * @return the output of processing the lines
   * @throws IOException if an I/O error occurs
   */
  public static <T> T readLines(File file, Charset charset,
      LineProcessor<T> callback) throws IOException {
    return CharStreams.readLines(newReaderSupplier(file, charset), callback);
  }

  /**
   * Process the bytes of a file.
   *
   * <p>(If this seems too complicated, maybe you're looking for
   * {@link #toByteArray}.)
   *
   * @param file the file to read
   * @param processor the object to which the bytes of the file are passed.
   * @return the result of the byte processor
   * @throws IOException if an I/O error occurs
   */
  public static <T> T readBytes(File file, ByteProcessor<T> processor)
      throws IOException {
    return ByteStreams.readBytes(newInputStreamSupplier(file), processor);
  }

  /**
   * Computes and returns the checksum value for a file.
   * The checksum object is reset when this method returns successfully.
   *
   * @param file the file to read
   * @param checksum the checksum object
   * @return the result of {@link Checksum#getValue} after updating the
   *     checksum object with all of the bytes in the file
   * @throws IOException if an I/O error occurs
   * @deprecated Use {@code hash} with the {@code Hashing.crc32()} or
   *     {@code Hashing.adler32()} hash functions. This method is scheduled
   *     to be removed in Guava 15.0.
   */
  @Deprecated
  public static long getChecksum(File file, Checksum checksum)
      throws IOException {
    return ByteStreams.getChecksum(newInputStreamSupplier(file), checksum);
  }

  /**
   * Computes the hash code of the {@code file} using {@code hashFunction}.
   *
   * @param file the file to read
   * @param hashFunction the hash function to use to hash the data
   * @return the {@link HashCode} of all of the bytes in the file
   * @throws IOException if an I/O error occurs
   * @since 12.0
   */
  public static HashCode hash(File file, HashFunction hashFunction)
      throws IOException {
    return asByteSource(file).hash(hashFunction);
  }

  /**
   * Fully maps a file read-only in to memory as per
   * {@link FileChannel#map(java.nio.channels.FileChannel.MapMode, long, long)}.
   *
   * <p>Files are mapped from offset 0 to its length.
   *
   * <p>This only works for files <= {@link Integer#MAX_VALUE} bytes.
   *
   * @param file the file to map
   * @return a read-only buffer reflecting {@code file}
   * @throws FileNotFoundException if the {@code file} does not exist
   * @throws IOException if an I/O error occurs
   *
   * @see FileChannel#map(MapMode, long, long)
   * @since 2.0
   */
  public static MappedByteBuffer map(File file) throws IOException {
    checkNotNull(file);
    return map(file, MapMode.READ_ONLY);
  }

  /**
   * Fully maps a file in to memory as per
   * {@link FileChannel#map(java.nio.channels.FileChannel.MapMode, long, long)}
   * using the requested {@link MapMode}.
   *
   * <p>Files are mapped from offset 0 to its length.
   *
   * <p>This only works for files <= {@link Integer#MAX_VALUE} bytes.
   *
   * @param file the file to map
   * @param mode the mode to use when mapping {@code file}
   * @return a buffer reflecting {@code file}
   * @throws FileNotFoundException if the {@code file} does not exist
   * @throws IOException if an I/O error occurs
   *
   * @see FileChannel#map(MapMode, long, long)
   * @since 2.0
   */
  public static MappedByteBuffer map(File file, MapMode mode)
      throws IOException {
    checkNotNull(file);
    checkNotNull(mode);
    if (!file.exists()) {
      throw new FileNotFoundException(file.toString());
    }
    return map(file, mode, file.length());
  }

  /**
   * Maps a file in to memory as per
   * {@link FileChannel#map(java.nio.channels.FileChannel.MapMode, long, long)}
   * using the requested {@link MapMode}.
   *
   * <p>Files are mapped from offset 0 to {@code size}.
   *
   * <p>If the mode is {@link MapMode#READ_WRITE} and the file does not exist,
   * it will be created with the requested {@code size}. Thus this method is
   * useful for creating memory mapped files which do not yet exist.
   *
   * <p>This only works for files <= {@link Integer#MAX_VALUE} bytes.
   *
   * @param file the file to map
   * @param mode the mode to use when mapping {@code file}
   * @return a buffer reflecting {@code file}
   * @throws IOException if an I/O error occurs
   *
   * @see FileChannel#map(MapMode, long, long)
   * @since 2.0
   */
  public static MappedByteBuffer map(File file, MapMode mode, long size)
      throws FileNotFoundException, IOException {
    checkNotNull(file);
    checkNotNull(mode);

    Closer closer = Closer.create();
    try {
      RandomAccessFile raf = closer.register(
          new RandomAccessFile(file, mode == MapMode.READ_ONLY ? "r" : "rw"));
      return map(raf, mode, size);
    } catch (Throwable e) {
      throw closer.rethrow(e);
    } finally {
      closer.close();
    }
  }

  private static MappedByteBuffer map(RandomAccessFile raf, MapMode mode,
      long size) throws IOException {
    Closer closer = Closer.create();
    try {
      FileChannel channel = closer.register(raf.getChannel());
      return channel.map(mode, 0, size);
    } catch (Throwable e) {
      throw closer.rethrow(e);
    } finally {
      closer.close();
    }
  }

  /**
   * Returns the lexically cleaned form of the path name, <i>usually</i> (but
   * not always) equivalent to the original. The following heuristics are used:
   *
   * <ul>
   * <li>empty string becomes .
   * <li>. stays as .
   * <li>fold out ./
   * <li>fold out ../ when possible
   * <li>collapse multiple slashes
   * <li>delete trailing slashes (unless the path is just "/")
   * </ul>
   *
   * These heuristics do not always match the behavior of the filesystem. In
   * particular, consider the path {@code a/../b}, which {@code simplifyPath}
   * will change to {@code b}. If {@code a} is a symlink to {@code x}, {@code
   * a/../b} may refer to a sibling of {@code x}, rather than the sibling of
   * {@code a} referred to by {@code b}.
   *
   * @since 11.0
   */
  public static String simplifyPath(String pathname) {
    checkNotNull(pathname);
    if (pathname.length() == 0) {
      return ".";
    }

    // split the path apart
    Iterable<String> components =
        Splitter.on('/').omitEmptyStrings().split(pathname);
    List<String> path = new ArrayList<String>();

    // resolve ., .., and //
    for (String component : components) {
      if (component.equals(".")) {
        continue;
      } else if (component.equals("..")) {
        if (path.size() > 0 && !path.get(path.size() - 1).equals("..")) {
          path.remove(path.size() - 1);
        } else {
          path.add("..");
        }
      } else {
        path.add(component);
      }
    }

    // put it back together
    String result = Joiner.on('/').join(path);
    if (pathname.charAt(0) == '/') {
      result = "/" + result;
    }

    while (result.startsWith("/../")) {
      result = result.substring(3);
    }
    if (result.equals("/..")) {
      result = "/";
    } else if ("".equals(result)) {
      result = ".";
    }

    return result;
  }

  /**
   * Returns the <a href="http://en.wikipedia.org/wiki/Filename_extension">file
   * extension</a> for the given file name, or the empty string if the file has
   * no extension.  The result does not include the '{@code .}'.
   *
   * @since 11.0
   */
  public static String getFileExtension(String fullName) {
    checkNotNull(fullName);
    String fileName = new File(fullName).getName();
    int dotIndex = fileName.lastIndexOf('.');
    return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
  }

  /**
   * Returns the file name without its
   * <a href="http://en.wikipedia.org/wiki/Filename_extension">file extension</a> or path. This is
   * similar to the {@code basename} unix command. The result does not include the '{@code .}'.
   *
   * @param file The name of the file to trim the extension from. This can be either a fully
   *     qualified file name (including a path) or just a file name.
   * @return The file name without its path or extension.
   * @since 14.0
   */
  public static String getNameWithoutExtension(String file) {
    checkNotNull(file);
    String fileName = new File(file).getName();
    int dotIndex = fileName.lastIndexOf('.');
    return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
  }
}