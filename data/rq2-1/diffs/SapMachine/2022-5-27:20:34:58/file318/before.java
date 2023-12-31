/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *   Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */

/*
 * @test
 * @enablePreview
 * @requires ((os.arch == "amd64" | os.arch == "x86_64") & sun.arch.data.model == "64") | os.arch == "aarch64"
 * @run testng/othervm --enable-native-access=ALL-UNNAMED TestVarArgs
 */

import java.lang.foreign.Addressable;
import java.lang.foreign.Linker;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.lang.foreign.ValueLayout;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;

import static java.lang.foreign.MemoryLayout.PathElement.*;
import static org.testng.Assert.assertEquals;

public class TestVarArgs extends NativeTestHelper {

    static final MemoryLayout ML_CallInfo = MemoryLayout.structLayout(
            C_POINTER.withName("writeback"), // writeback
            C_POINTER.withName("argIDs")); // arg ids

    static final VarHandle VH_CallInfo_writeback = ML_CallInfo.varHandle(groupElement("writeback"));
    static final VarHandle VH_CallInfo_argIDs = ML_CallInfo.varHandle(groupElement("argIDs"));

    static final VarHandle VH_IntArray = C_INT.arrayElementVarHandle();

    static final Linker abi = Linker.nativeLinker();
    static {
        System.loadLibrary("VarArgs");
    }

    static final Addressable VARARGS_ADDR = findNativeOrThrow("varargs");

    static final int WRITEBACK_BYTES_PER_ARG = 8;

    @Test(dataProvider = "args")
    public void testVarArgs(List<VarArg> args) throws Throwable {
        try (MemorySession session = MemorySession.openConfined()) {
            MemorySegment writeBack = MemorySegment.allocateNative(args.size() * WRITEBACK_BYTES_PER_ARG, WRITEBACK_BYTES_PER_ARG, session);
            MemorySegment callInfo = MemorySegment.allocateNative(ML_CallInfo, session);
            MemorySegment argIDs = MemorySegment.allocateNative(MemoryLayout.sequenceLayout(args.size(), C_INT), session);

            MemoryAddress callInfoPtr = callInfo.address();

            VH_CallInfo_writeback.set(callInfo, writeBack.address());
            VH_CallInfo_argIDs.set(callInfo, argIDs.address());

            for (int i = 0; i < args.size(); i++) {
                VH_IntArray.set(argIDs, (long) i, args.get(i).id.ordinal());
            }

            List<MemoryLayout> argLayouts = new ArrayList<>();
            argLayouts.add(C_POINTER); // call info
            argLayouts.add(C_INT); // size

            FunctionDescriptor desc = FunctionDescriptor.ofVoid(argLayouts.stream().toArray(MemoryLayout[]::new))
                    .asVariadic(args.stream().map(a -> a.layout).toArray(MemoryLayout[]::new));

            List<Class<?>> carriers = new ArrayList<>();
            carriers.add(MemoryAddress.class); // call info
            carriers.add(int.class); // size
            args.forEach(a -> carriers.add(a.carrier));

            MethodType mt = MethodType.methodType(void.class, carriers);

            MethodHandle downcallHandle = abi.downcallHandle(VARARGS_ADDR, desc);

            List<Object> argValues = new ArrayList<>();
            argValues.add(callInfoPtr); // call info
            argValues.add(args.size());  // size
            args.forEach(a -> argValues.add(a.value));

            downcallHandle.invokeWithArguments(argValues);

            for (int i = 0; i < args.size(); i++) {
                VarArg a = args.get(i);
                MemorySegment writtenPtr = writeBack.asSlice(i * WRITEBACK_BYTES_PER_ARG);
                Object written = a.vh.get(writtenPtr);
                assertEquals(written, a.value);
            }
        }
    }

    @DataProvider
    public static Object[][] args() {
        return new Object[][] {
            new Object[] { List.of(VarArg.intArg(5), VarArg.intArg(10), VarArg.intArg(15)) },
            new Object[] { List.of(VarArg.doubleArg(5), VarArg.doubleArg(10), VarArg.doubleArg(15)) },
            new Object[] { List.of(VarArg.intArg(5), VarArg.doubleArg(10), VarArg.intArg(15)) },
        };
    }

    private static final class VarArg {
        final NativeType id;
        final Object value;
        final ValueLayout layout;
        final Class<?> carrier;
        final VarHandle vh;

        private VarArg(NativeType id, ValueLayout layout, Class<?> carrier, Object value) {
            this.id = id;
            this.value = value;
            this.layout = layout;
            this.carrier = carrier;
            this.vh = layout.varHandle();
        }

        static VarArg intArg(int value) {
            return new VarArg(VarArg.NativeType.INT, C_INT, int.class, value);
        }

        static VarArg doubleArg(double value) {
            return new VarArg(VarArg.NativeType.DOUBLE, C_DOUBLE, double.class, value);
        }

        enum NativeType {
            INT,
            DOUBLE
        }
    }

}
