/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @summary Unit test for ProcessTools.startProcess()
 * @library /test/lib
 * @compile ProcessToolsStartProcessTest.java
 * @run main ProcessToolsStartProcessTest
 */

import java.util.function.Consumer;
import java.io.File;

import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.Utils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class ProcessToolsStartProcessTest {
    static final int NUM_LINES = 50;
    static String output = "";

    private static Consumer<String> outputConsumer = s -> {
        output += s + "\n";
    };

    static boolean testStartProcess(boolean withConsumer) throws Exception {
        boolean success = true;
        Process p;
        JDKToolLauncher launcher = JDKToolLauncher.createUsingTestJDK("java");
        launcher.addToolArg("-cp");
        launcher.addToolArg(Utils.TEST_CLASSES);
        launcher.addToolArg("ProcessToolsStartProcessTest");
        launcher.addToolArg("test"); // This one argument triggers producing the output
        ProcessBuilder pb = new ProcessBuilder();
        pb.command(launcher.getCommand());

        System.out.println("DEBUG: Test with withConsumer=" + withConsumer);
        System.out.println("DEBUG: about to start process.");
        if (withConsumer) {
            p = ProcessTools.startProcess("java", pb, outputConsumer);
        } else {
            p = ProcessTools.startProcess("java", pb);
        }
        OutputAnalyzer out = new OutputAnalyzer(p);

        System.out.println("DEBUG: process started.");
        p.waitFor();
        if (p.exitValue() != 0) {
            throw new RuntimeException("Bad exit value: " + p.exitValue());
        }

        if (withConsumer) {
            int numLines = output.split("\n").length;
            if (numLines != NUM_LINES ) {
                System.out.print("FAILED: wrong number of lines in Consumer output\n");
                success = false;
            }
            System.out.println("DEBUG: Consumer output: got " + numLines + " lines , expected "
                               + NUM_LINES  + ". Output follow:");
            System.out.print(output);
            System.out.println("DEBUG: done with Consumer output.");
        }

        int numLinesOut = out.getStdout().split("\n").length;
        if (numLinesOut != NUM_LINES) {
            System.out.print("FAILED: wrong number of lines in OutputAnalyzer output\n");
            success = false;
        }
        System.out.println("DEBUG: OutputAnalyzer output: got " + numLinesOut + " lines, expected "
                           + NUM_LINES  + ". Output follows:");
        System.out.print(out.getStdout());
        System.out.println("DEBUG: done with OutputAnalyzer stdout.");

        return success;
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            for (int i = 0; i < NUM_LINES; i++) {
                System.out.println("A line on stdout " + i);
            }
        } else {
            try {
                boolean test1Result = testStartProcess(false);
                boolean test2Result = testStartProcess(true);
                if (!test1Result || !test2Result) {
                    throw new RuntimeException("One or more tests failed. See output for details.");
                }
            } catch (RuntimeException re) {
                re.printStackTrace();
                System.out.println("Test ERROR");
                throw re;
            } catch (Exception ex) {
                ex.printStackTrace();
                System.out.println("Test ERROR");
                throw new RuntimeException(ex);
            }
        }
    }
}
