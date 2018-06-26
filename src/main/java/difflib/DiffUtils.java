/*
 * SPDX-License-Identifier: Apache-1.1
 *
 * ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2003 The Apache Software Foundation.
 * Copyright (c) 2010 Dmitry Naumenko (dm.naumenko@gmail.com)
 * Copyright (c) 2015-2016 Brenden Kromhout and contributors to java-diff-utils
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package difflib;

import difflib.myers.Equalizer;
import difflib.myers.MyersDiff;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implements the difference and patching engine
 * @author <a href="dm.naumenko@gmail.com">Dmitry Naumenko</a>
 * @version 0.4.1
 */
public class DiffUtils {

    private static final Pattern unifiedDiffChunkRe = Pattern
            .compile("^@@\\s+-(?:(\\d+)(?:,(\\d+))?)\\s+\\+(?:(\\d+)(?:,(\\d+))?)\\s+@@$");

    private List<String> readLines(@Nonnull File file) {
        List<String> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), Utils.UTF_8))) {
            for (String line = br.readLine(); line != null; line = br.readLine()) list.add(line);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Nonnull
    public Patch<String> diff(@Nonnull File original, @Nonnull File revised) throws IOException {
        return diff(readLines(original), readLines(revised));
    }

    @Nonnull
    public Patch<String> diff(@Nonnull File original, @Nonnull File revised,
                              @Nonnull DiffAlgorithm<String> algorithm) throws IOException {
        return diff(readLines(original), readLines(revised), algorithm);
    }

    @Nonnull
    public Patch<String> diff(@Nonnull File original, @Nonnull File revised,
                              @Nullable Equalizer<String> equalizer) throws IOException {
        return diff(readLines(original), readLines(revised), equalizer);
    }

    /**
     * Computes the difference between the original and revised list of elements with default diff algorithm
     * @param original The original text. Must not be {@code null}.
     * @param revised  The revised text. Must not be {@code null}.
     * @return The patch describing the difference between the original and revised sequences. Never {@code null}.
     */
    @Nonnull
    public static <T> Patch<T> diff(List<T> original, List<T> revised) {
        return DiffUtils.diff(original, revised, new MyersDiff<T>());
    }

    /**
     * Computes the difference between the original and revised list of elements with default diff algorithm
     * @param original  The original text. Must not be {@code null}.
     * @param revised   The revised text. Must not be {@code null}.
     * @param equalizer the equalizer object to replace the default compare algorithm (Object.equals). If {@code null}
     *                  the default equalizer of the default algorithm is used..
     * @return The patch describing the difference between the original and revised sequences. Never {@code null}.
     */
    @Nonnull
    public static <T> Patch<T> diff(List<T> original, List<T> revised,
                                    Equalizer<T> equalizer) {
        if (equalizer != null) {
            return DiffUtils.diff(original, revised,
                    new MyersDiff<T>(equalizer));
        }
        return DiffUtils.diff(original, revised, new MyersDiff<T>());
    }

    /**
     * Computes the difference between the original and revised list of elements with default diff algorithm
     * @param original  The original text. Must not be {@code null}.
     * @param revised   The revised text. Must not be {@code null}.
     * @param algorithm The diff algorithm. Must not be {@code null}.
     * @return The patch describing the difference between the original and revised sequences. Never {@code null}.
     */
    @Nonnull
    public static <T> Patch<T> diff(List<T> original, List<T> revised,
                                    DiffAlgorithm<T> algorithm) {
        if (original == null) {
            throw new IllegalArgumentException("original must not be null");
        }
        if (revised == null) {
            throw new IllegalArgumentException("revised must not be null");
        }
        if (algorithm == null) {
            throw new IllegalArgumentException("algorithm must not be null");
        }
        return algorithm.diff(original, revised);
    }

    /**
     * Patch the original text with given patch
     * @param original the original text
     * @param patch    the given patch
     * @return the revised text
     * @throws PatchFailedException if can't apply patch
     */
    @Nonnull
    public static <T> List<T> patch(List<T> original, Patch<T> patch)
            throws PatchFailedException {
        return patch.applyTo(original);
    }

    /**
     * Unpatch the revised text for a given patch
     * @param revised the revised text
     * @param patch   the given patch
     * @return the original text
     */
    public static <T> List<T> unpatch(List<T> revised, Patch<T> patch) {
        return patch.restore(revised);
    }

    /**
     * Parse the given text in unified format and creates the list of deltas for it.
     * @param diff the text in unified format
     * @return the patch with deltas.
     */
    public static Patch<String> parseUnifiedDiff(List<String> diff) {
        boolean inPrelude = true;
        List<String[]> rawChunk = new ArrayList<String[]>();
        Patch<String> patch = new Patch<String>();

        int old_ln = 0, new_ln = 0;
        String tag;
        String rest;
        for (String line : diff) {
            // Skip leading lines until after we've seen one starting with '+++'
            if (inPrelude) {
                if (line.startsWith("+++")) {
                    inPrelude = false;
                }
                continue;
            }
            Matcher m = unifiedDiffChunkRe.matcher(line);
            if (m.find()) {
                // Process the lines in the previous chunk
                processRawChunk(rawChunk, patch, old_ln, new_ln);

                // Parse the @@ header
                old_ln = m.group(1) == null ? 1 : Integer.parseInt(m.group(1));
                new_ln = m.group(3) == null ? 1 : Integer.parseInt(m.group(3));

                if (old_ln == 0) {
                    old_ln += 1;
                }
                if (new_ln == 0) {
                    new_ln += 1;
                }
            } else {
                if (line.length() > 0) {
                    tag = line.substring(0, 1);
                    rest = line.substring(1);
                    if (tag.equals(" ") || tag.equals("+") || tag.equals("-")) {
                        rawChunk.add(new String[] {tag, rest});
                    }
                } else {
                    rawChunk.add(new String[] {" ", ""});
                }
            }
        }

        // Process the lines in the last chunk
        processRawChunk(rawChunk, patch, old_ln, new_ln);

        return patch;
    }

    public static void processRawChunk(List<String[]> rawChunk, Patch patch, int old_ln, int new_ln) {
        String tag;
        String rest;

        if (rawChunk.size() != 0) {
            List<String> oldChunkLines = new ArrayList<String>();
            List<String> newChunkLines = new ArrayList<String>();

            for (String[] raw_line : rawChunk) {
                tag = raw_line[0];
                rest = raw_line[1];
                if (tag.equals(" ") || tag.equals("-")) {
                    oldChunkLines.add(rest);
                }
                if (tag.equals(" ") || tag.equals("+")) {
                    newChunkLines.add(rest);
                }
            }

            if (oldChunkLines.isEmpty()) {
                patch.addDelta(new InsertDelta<String>(new Chunk<String>(old_ln, oldChunkLines),
                        new Chunk<String>(new_ln - 1, newChunkLines)));
            } else if (newChunkLines.isEmpty()) {
                patch.addDelta(new DeleteDelta<String>(new Chunk<String>(old_ln - 1, oldChunkLines),
                        new Chunk<String>(new_ln, newChunkLines)));
            } else {
                patch.addDelta(new ChangeDelta<String>(new Chunk<String>(old_ln - 1, oldChunkLines),
                        new Chunk<String>(new_ln - 1, newChunkLines)));
            }
            rawChunk.clear();
        }
    }

    /**
     * generateUnifiedDiff takes a Patch and some other arguments, returning the Unified Diff format text representing
     * the Patch.
     * @param original      - Filename of the original (unrevised file)
     * @param revised       - Filename of the revised file
     * @param originalLines - Lines of the original file
     * @param patch         - Patch created by the diff() function
     * @param contextSize   - number of lines of context output around each difference in the file.
     * @return List of strings representing the Unified Diff representation of the Patch argument.
     * @author Bill James (tankerbay@gmail.com)
     */
    public static List<String> generateUnifiedDiff(String original,
                                                   String revised, List<String> originalLines, Patch<String> patch,
                                                   int contextSize) {
        if (!patch.getDeltas().isEmpty()) {
            List<String> ret = new ArrayList<String>();
            ret.add("--- " + original);
            ret.add("+++ " + revised);

            List<Delta<String>> patchDeltas = new ArrayList<Delta<String>>(
                    patch.getDeltas());

            // code outside the if block also works for single-delta issues.
            List<Delta<String>> deltas = new ArrayList<Delta<String>>(); // current
            // list
            // of
            // Delta's to
            // process
            Delta<String> delta = patchDeltas.get(0);
            deltas.add(delta); // add the first Delta to the current set
            // if there's more than 1 Delta, we may need to output them together
            if (patchDeltas.size() > 1) {
                for (int i = 1; i < patchDeltas.size(); i++) {
                    int position = delta.getOriginal().getPosition(); // store
                    // the
                    // current
                    // position
                    // of
                    // the first Delta

                    // Check if the next Delta is too close to the current
                    // position.
                    // And if it is, add it to the current set
                    Delta<String> nextDelta = patchDeltas.get(i);
                    if ((position + delta.getOriginal().size() + contextSize) >= (nextDelta
                            .getOriginal().getPosition() - contextSize)) {
                        deltas.add(nextDelta);
                    } else {
                        // if it isn't, output the current set,
                        // then create a new set and add the current Delta to
                        // it.
                        List<String> curBlock = processDeltas(originalLines,
                                deltas, contextSize);
                        ret.addAll(curBlock);
                        deltas.clear();
                        deltas.add(nextDelta);
                    }
                    delta = nextDelta;
                }

            }
            // don't forget to process the last set of Deltas
            List<String> curBlock = processDeltas(originalLines, deltas,
                    contextSize);
            ret.addAll(curBlock);
            return ret;
        }
        return new ArrayList<String>();
    }

    /**
     * processDeltas takes a list of Deltas and outputs them together in a single block of Unified-Diff-format text.
     * @param origLines   - the lines of the original file
     * @param deltas      - the Deltas to be output as a single block
     * @param contextSize - the number of lines of context to place around block
     * @return
     * @author Bill James (tankerbay@gmail.com)
     */
    private static List<String> processDeltas(List<String> origLines,
                                              List<Delta<String>> deltas, int contextSize) {
        List<String> buffer = new ArrayList<String>();
        int origTotal = 0; // counter for total lines output from Original
        int revTotal = 0; // counter for total lines output from Original
        int line;

        Delta<String> curDelta = deltas.get(0);

        // NOTE: +1 to overcome the 0-offset Position
        int origStart = curDelta.getOriginal().getPosition() + 1 - contextSize;
        if (origStart < 1) {
            origStart = 1;
        }

        int revStart = curDelta.getRevised().getPosition() + 1 - contextSize;
        if (revStart < 1) {
            revStart = 1;
        }

        // find the start of the wrapper context code
        int contextStart = curDelta.getOriginal().getPosition() - contextSize;
        if (contextStart < 0) {
            contextStart = 0; // clamp to the start of the file
        }

        // output the context before the first Delta
        for (line = contextStart; line < curDelta.getOriginal().getPosition(); line++) { //
            buffer.add(" " + origLines.get(line));
            origTotal++;
            revTotal++;
        }

        // output the first Delta
        buffer.addAll(getDeltaText(curDelta));
        origTotal += curDelta.getOriginal().getLines().size();
        revTotal += curDelta.getRevised().getLines().size();

        int deltaIndex = 1;
        while (deltaIndex < deltas.size()) { // for each of the other Deltas
            Delta<String> nextDelta = deltas.get(deltaIndex);
            int intermediateStart = curDelta.getOriginal().getPosition()
                    + curDelta.getOriginal().getLines().size();
            for (line = intermediateStart; line < nextDelta.getOriginal()
                                                           .getPosition(); line++) {
                // output the code between the last Delta and this one
                buffer.add(" " + origLines.get(line));
                origTotal++;
                revTotal++;
            }
            buffer.addAll(getDeltaText(nextDelta)); // output the Delta
            origTotal += nextDelta.getOriginal().getLines().size();
            revTotal += nextDelta.getRevised().getLines().size();
            curDelta = nextDelta;
            deltaIndex++;
        }

        // Now output the post-Delta context code, clamping the end of the file
        contextStart = curDelta.getOriginal().getPosition()
                + curDelta.getOriginal().getLines().size();
        for (line = contextStart; (line < (contextStart + contextSize))
                && (line < origLines.size()); line++) {
            buffer.add(" " + origLines.get(line));
            origTotal++;
            revTotal++;
        }

        // In case of empty chunk and context
        if (origTotal == 0 && origStart > 1)
                --origStart;

        // In case of empty chunk and context
        if (revTotal == 0 && revStart > 1)
                --revStart;

        // Create and insert the block header, conforming to the Unified Diff
        // standard
        StringBuffer header = new StringBuffer();
        header.append("@@ -");
        header.append(origStart);
        header.append(",");
        header.append(origTotal);
        header.append(" +");
        header.append(revStart);
        header.append(",");
        header.append(revTotal);
        header.append(" @@");
        buffer.add(0, header.toString());

        return buffer;
    }

    /**
     * getDeltaText returns the lines to be added to the Unified Diff text from the Delta parameter
     * @param delta - the Delta to output
     * @return list of String lines of code.
     * @author Bill James (tankerbay@gmail.com)
     */
    private static List<String> getDeltaText(Delta<String> delta) {
        List<String> buffer = new ArrayList<String>();
        for (String line : delta.getOriginal().getLines()) {
            buffer.add("-" + line);
        }
        for (String line : delta.getRevised().getLines()) {
            buffer.add("+" + line);
        }
        return buffer;
    }
}