/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package compiler.vectorapi.reshape.utils;

import compiler.lib.ir_framework.ForceInline;
import compiler.lib.ir_framework.IRNode;
import compiler.lib.ir_framework.TestFramework;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Array;
import java.nio.ByteOrder;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jdk.incubator.vector.*;
import jdk.test.lib.Asserts;

public class VectorReshapeHelper {
    public static final int INVOCATIONS = 10_000;

    public static final VectorSpecies<Byte>    BSPEC64  =   ByteVector.SPECIES_64;
    public static final VectorSpecies<Short>   SSPEC64  =  ShortVector.SPECIES_64;
    public static final VectorSpecies<Integer> ISPEC64  =    IntVector.SPECIES_64;
    public static final VectorSpecies<Long>    LSPEC64  =   LongVector.SPECIES_64;
    public static final VectorSpecies<Float>   FSPEC64  =  FloatVector.SPECIES_64;
    public static final VectorSpecies<Double>  DSPEC64  = DoubleVector.SPECIES_64;

    public static final VectorSpecies<Byte>    BSPEC128 =   ByteVector.SPECIES_128;
    public static final VectorSpecies<Short>   SSPEC128 =  ShortVector.SPECIES_128;
    public static final VectorSpecies<Integer> ISPEC128 =    IntVector.SPECIES_128;
    public static final VectorSpecies<Long>    LSPEC128 =   LongVector.SPECIES_128;
    public static final VectorSpecies<Float>   FSPEC128 =  FloatVector.SPECIES_128;
    public static final VectorSpecies<Double>  DSPEC128 = DoubleVector.SPECIES_128;

    public static final VectorSpecies<Byte>    BSPEC256 =   ByteVector.SPECIES_256;
    public static final VectorSpecies<Short>   SSPEC256 =  ShortVector.SPECIES_256;
    public static final VectorSpecies<Integer> ISPEC256 =    IntVector.SPECIES_256;
    public static final VectorSpecies<Long>    LSPEC256 =   LongVector.SPECIES_256;
    public static final VectorSpecies<Float>   FSPEC256 =  FloatVector.SPECIES_256;
    public static final VectorSpecies<Double>  DSPEC256 = DoubleVector.SPECIES_256;

    public static final VectorSpecies<Byte>    BSPEC512 =   ByteVector.SPECIES_512;
    public static final VectorSpecies<Short>   SSPEC512 =  ShortVector.SPECIES_512;
    public static final VectorSpecies<Integer> ISPEC512 =    IntVector.SPECIES_512;
    public static final VectorSpecies<Long>    LSPEC512 =   LongVector.SPECIES_512;
    public static final VectorSpecies<Float>   FSPEC512 =  FloatVector.SPECIES_512;
    public static final VectorSpecies<Double>  DSPEC512 = DoubleVector.SPECIES_512;

    public static final String B2X_NODE  = IRNode.VECTOR_CAST_B2X;
    public static final String S2X_NODE  = IRNode.VECTOR_CAST_S2X;
    public static final String I2X_NODE  = IRNode.VECTOR_CAST_I2X;
    public static final String L2X_NODE  = IRNode.VECTOR_CAST_L2X;
    public static final String F2X_NODE  = IRNode.VECTOR_CAST_F2X;
    public static final String D2X_NODE  = IRNode.VECTOR_CAST_D2X;
    public static final String REINTERPRET_NODE = IRNode.VECTOR_REINTERPRET;

    public static void runMainHelper(Class<?> testClass, Stream<VectorSpeciesPair> testMethods, String... flags) {
        var test = new TestFramework(testClass);
        test.setDefaultWarmup(1);
        test.addHelperClasses(VectorReshapeHelper.class);
        test.addFlags("--add-modules=jdk.incubator.vector");
        test.addFlags(flags);
        String testMethodNames = testMethods
                .filter(p -> p.isp().length() <= VectorSpecies.ofLargestShape(p.isp().elementType()).length())
                .filter(p -> p.osp().length() <= VectorSpecies.ofLargestShape(p.osp().elementType()).length())
                .map(VectorSpeciesPair::format)
                .collect(Collectors.joining(","));
        test.addFlags("-DTest=" + testMethodNames);
        test.start();
    }

    @ForceInline
    public static <T, U> void vectorCast(VectorOperators.Conversion<T, U> cop,
                                         VectorSpecies<T> isp, VectorSpecies<U> osp, byte[] input, byte[] output) {
        isp.fromByteArray(input, 0, ByteOrder.nativeOrder())
                .convertShape(cop, osp, 0)
                .intoByteArray(output, 0, ByteOrder.nativeOrder());
    }

    public static <T, U> void runCastHelper(VectorOperators.Conversion<T, U> castOp,
                                            VectorSpecies<T> isp, VectorSpecies<U> osp) throws Throwable {
        var random = RandomGenerator.getDefault();
        boolean isUnsignedCast = castOp.name().startsWith("ZERO");
        String testMethodName = VectorSpeciesPair.makePair(isp, osp).format();
        var caller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass();
        var testMethod = MethodHandles.lookup().findStatic(caller,
                testMethodName,
                MethodType.methodType(void.class, byte.class.arrayType(), byte.class.arrayType()));
        byte[] input = new byte[isp.vectorByteSize()];
        byte[] output = new byte[osp.vectorByteSize()];
        for (int iter = 0; iter < INVOCATIONS; iter++) {
            // We need to generate arrays with NaN or very large values occasionally
            boolean normalArray = random.nextBoolean();
            var abnormalValue = List.of(Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, -1e30, 1e30);
            for (int i = 0; i < isp.length(); i++) {
                switch (isp.elementType().getName()) {
                    case "byte"   -> setByte(input, i, (byte)random.nextInt());
                    case "short"  -> setShort(input, i, (short)random.nextInt());
                    case "int"    -> setInt(input, i, random.nextInt());
                    case "long"   -> setLong(input, i, random.nextLong());
                    case "float"  -> {
                        if (normalArray || random.nextBoolean()) {
                            setFloat(input, i, random.nextFloat(Byte.MIN_VALUE, Byte.MAX_VALUE));
                        } else {
                            setFloat(input, i, abnormalValue.get(random.nextInt(abnormalValue.size())).floatValue());
                        }
                    }
                    case "double" -> {
                        if (normalArray || random.nextBoolean()) {
                            setDouble(input, i, random.nextDouble(Byte.MIN_VALUE, Byte.MAX_VALUE));
                        } else {
                            setDouble(input, i, abnormalValue.get(random.nextInt(abnormalValue.size())));
                        }
                    }
                    default -> throw new AssertionError();
                }
            }

            testMethod.invokeExact(input, output);

            for (int i = 0; i < osp.length(); i++) {
                Number expected, actual;
                if (i < isp.length()) {
                    Number initial = switch (isp.elementType().getName()) {
                        case "byte"   -> getByte(input, i);
                        case "short"  -> getShort(input, i);
                        case "int"    -> getInt(input, i);
                        case "long"   -> getLong(input, i);
                        case "float"  -> getFloat(input, i);
                        case "double" -> getDouble(input, i);
                        default -> throw new AssertionError();
                    };
                    expected = switch (osp.elementType().getName()) {
                        case "byte" -> initial.byteValue();
                        case "short" -> {
                            if (isUnsignedCast) {
                                yield (short) (initial.longValue() & ((1L << isp.elementSize()) - 1));
                            } else {
                                yield initial.shortValue();
                            }
                        }
                        case "int" -> {
                            if (isUnsignedCast) {
                                yield (int) (initial.longValue() & ((1L << isp.elementSize()) - 1));
                            } else {
                                yield initial.intValue();
                            }
                        }
                        case "long" -> {
                            if (isUnsignedCast) {
                                yield (long) (initial.longValue() & ((1L << isp.elementSize()) - 1));
                            } else {
                                yield initial.longValue();
                            }
                        }
                        case "float" -> initial.floatValue();
                        case "double" -> initial.doubleValue();
                        default -> throw new AssertionError();
                    };
                } else {
                    expected = switch (osp.elementType().getName()) {
                        case "byte"   -> (byte)0;
                        case "short"  -> (short)0;
                        case "int"    -> (int)0;
                        case "long"   -> (long)0;
                        case "float"  -> (float)0;
                        case "double" -> (double)0;
                        default -> throw new AssertionError();
                    };
                }
                actual = switch (osp.elementType().getName()) {
                    case "byte"   -> getByte(output, i);
                    case "short"  -> getShort(output, i);
                    case "int"    -> getInt(output, i);
                    case "long"   -> getLong(output, i);
                    case "float"  -> getFloat(output, i);
                    case "double" -> getDouble(output, i);
                    default -> throw new AssertionError();
                };
                Asserts.assertEquals(expected, actual);
            }
        }
    }

    @ForceInline
    public static <T, U> void vectorExpandShrink(VectorSpecies<T> isp, VectorSpecies<U> osp, byte[] input, byte[] output) {
        isp.fromByteArray(input, 0, ByteOrder.nativeOrder())
                .reinterpretShape(osp, 0)
                .intoByteArray(output, 0, ByteOrder.nativeOrder());
    }

    public static <T, U> void runExpandShrinkHelper(VectorSpecies<T> isp, VectorSpecies<U> osp) throws Throwable {
        var random = RandomGenerator.getDefault();
        String testMethodName = VectorSpeciesPair.makePair(isp, osp).format();
        var caller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass();
        var testMethod = MethodHandles.lookup().findStatic(caller,
                testMethodName,
                MethodType.methodType(void.class, byte.class.arrayType(), byte.class.arrayType()));
        byte[] input = new byte[isp.vectorByteSize()];
        byte[] output = new byte[osp.vectorByteSize()];
        for (int iter = 0; iter < INVOCATIONS; iter++) {
            random.nextBytes(input);

            testMethod.invokeExact(input, output);

            for (int i = 0; i < osp.vectorByteSize(); i++) {
                int expected = i < isp.vectorByteSize() ? input[i] : 0;
                int actual = output[i];
                Asserts.assertEquals(expected, actual);
            }
        }
    }

    @ForceInline
    public static <T, U> void vectorDoubleExpandShrink(VectorSpecies<T> isp, VectorSpecies<U> osp, byte[] input, byte[] output) {
        isp.fromByteArray(input, 0, ByteOrder.nativeOrder())
                .reinterpretShape(osp, 0)
                .reinterpretShape(isp, 0)
                .intoByteArray(output, 0, ByteOrder.nativeOrder());
    }

    public static <T, U> void runDoubleExpandShrinkHelper(VectorSpecies<T> isp, VectorSpecies<U> osp) throws Throwable {
        var random = RandomGenerator.getDefault();
        String testMethodName = VectorSpeciesPair.makePair(isp, osp).format();
        var caller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass();
        var testMethod = MethodHandles.lookup().findStatic(caller,
                testMethodName,
                MethodType.methodType(void.class, byte.class.arrayType(), byte.class.arrayType()));
        byte[] input = new byte[isp.vectorByteSize()];
        byte[] output = new byte[isp.vectorByteSize()];
        for (int iter = 0; iter < INVOCATIONS; iter++) {
            random.nextBytes(input);

            testMethod.invokeExact(input, output);

            for (int i = 0; i < isp.vectorByteSize(); i++) {
                int expected = i < osp.vectorByteSize() ? input[i] : 0;
                int actual = output[i];
                Asserts.assertEquals(expected, actual);
            }
        }
    }

    // All this complication is due to the fact that vector load and store with respect to byte array introduce
    // additional ReinterpretNodes, several ReinterpretNodes back to back being optimized make the number of
    // nodes remaining in the IR becomes unpredictable.
    @ForceInline
    public static <T, U> void vectorRebracket(VectorSpecies<T> isp, VectorSpecies<U> osp, Object input, Object output) {
        var outputVector = isp.fromArray(input, 0).reinterpretShape(osp, 0);
        var otype = osp.elementType();
        if (otype == byte.class) {
            ((ByteVector)outputVector).intoArray((byte[])output, 0);
        } else if (otype == short.class) {
            ((ShortVector)outputVector).intoArray((short[])output, 0);
        } else if (otype == int.class) {
            ((IntVector)outputVector).intoArray((int[])output, 0);
        } else if (otype == long.class) {
            ((LongVector)outputVector).intoArray((long[])output, 0);
        } else if (otype == float.class) {
            ((FloatVector)outputVector).intoArray((float[])output, 0);
        } else if (otype == double.class) {
            ((DoubleVector)outputVector).intoArray((double[])output, 0);
        } else {
            throw new AssertionError();
        }
    }

    public static <T, U> void runRebracketHelper(VectorSpecies<T> isp, VectorSpecies<U> osp) throws Throwable {
        var random = RandomGenerator.getDefault();
        String testMethodName = VectorSpeciesPair.makePair(isp, osp).format();
        var caller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass();
        var testMethod = MethodHandles.lookup().findStatic(caller,
                    testMethodName,
                    MethodType.methodType(void.class, isp.elementType().arrayType(), osp.elementType().arrayType()))
                .asType(MethodType.methodType(void.class, Object.class, Object.class));
        Object input = Array.newInstance(isp.elementType(), isp.length());
        Object output = Array.newInstance(osp.elementType(), osp.length());
        long ibase = UnsafeUtils.arrayBase(isp.elementType());
        long obase = UnsafeUtils.arrayBase(osp.elementType());
        for (int iter = 0; iter < INVOCATIONS; iter++) {
            for (int i = 0; i < isp.vectorByteSize(); i++) {
                UnsafeUtils.putByte(input, ibase, i, random.nextInt());
            }

            testMethod.invokeExact(input, output);

            for (int i = 0; i < osp.vectorByteSize(); i++) {
                int expected = i < isp.vectorByteSize() ? UnsafeUtils.getByte(input, ibase, i) : 0;
                int actual = UnsafeUtils.getByte(output, obase, i);
                Asserts.assertEquals(expected, actual);
            }
        }
    }

    public static byte getByte(byte[] array, int index) {
        return (byte)BYTE_ACCESS.get(array, index * Byte.BYTES);
    }

    public static short getShort(byte[] array, int index) {
        return (short)SHORT_ACCESS.get(array, index * Short.BYTES);
    }

    public static int getInt(byte[] array, int index) {
        return (int)INT_ACCESS.get(array, index * Integer.BYTES);
    }

    public static long getLong(byte[] array, int index) {
        return (long)LONG_ACCESS.get(array, index * Long.BYTES);
    }

    public static float getFloat(byte[] array, int index) {
        return (float)FLOAT_ACCESS.get(array, index * Float.BYTES);
    }

    public static double getDouble(byte[] array, int index) {
        return (double)DOUBLE_ACCESS.get(array, index * Double.BYTES);
    }

    public static void setByte(byte[] array, int index, byte value) {
        BYTE_ACCESS.set(array, index * Byte.BYTES, value);
    }

    public static void setShort(byte[] array, int index, short value) {
        SHORT_ACCESS.set(array, index * Short.BYTES, value);
    }

    public static void setInt(byte[] array, int index, int value) {
        INT_ACCESS.set(array, index * Integer.BYTES, value);
    }

    public static void setLong(byte[] array, int index, long value) {
        LONG_ACCESS.set(array, index * Long.BYTES, value);
    }

    public static void setFloat(byte[] array, int index, float value) {
        FLOAT_ACCESS.set(array, index * Float.BYTES, value);
    }

    public static void setDouble(byte[] array, int index, double value) {
        DOUBLE_ACCESS.set(array, index * Double.BYTES, value);
    }

    private static final VarHandle BYTE_ACCESS   = MethodHandles.arrayElementVarHandle(byte.class.arrayType());
    private static final VarHandle SHORT_ACCESS  = MethodHandles.byteArrayViewVarHandle(short.class.arrayType(),  ByteOrder.nativeOrder());
    private static final VarHandle INT_ACCESS    = MethodHandles.byteArrayViewVarHandle(int.class.arrayType(),    ByteOrder.nativeOrder());
    private static final VarHandle LONG_ACCESS   = MethodHandles.byteArrayViewVarHandle(long.class.arrayType(),   ByteOrder.nativeOrder());
    private static final VarHandle FLOAT_ACCESS  = MethodHandles.byteArrayViewVarHandle(float.class.arrayType(),  ByteOrder.nativeOrder());
    private static final VarHandle DOUBLE_ACCESS = MethodHandles.byteArrayViewVarHandle(double.class.arrayType(), ByteOrder.nativeOrder());
}
