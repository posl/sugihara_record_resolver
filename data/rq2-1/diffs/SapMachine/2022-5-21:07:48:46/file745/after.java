/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8262891
 * @summary Verify pattern switches work properly when the set of enum constant changes.
 * @compile --enable-preview -source ${jdk.version} EnumTypeChanges.java
 * @compile --enable-preview -source ${jdk.version} EnumTypeChanges2.java
 * @run main/othervm --enable-preview EnumTypeChanges
 */

import java.util.function.Function;
import java.util.Objects;

public class EnumTypeChanges {

    public static void main(String... args) throws Exception {
        new EnumTypeChanges().run();
    }

    void run() throws Exception {
        doRun(this::statementEnum);
        doRun(this::expressionEnum);
        doRunExhaustive(this::expressionEnumExhaustive);
        doRunExhaustive(this::statementEnumExhaustive);
    }

    void doRun(Function<EnumTypeChangesEnum, String> c) throws Exception {
        assertEquals("A", c.apply(EnumTypeChangesEnum.A));
        assertEquals("D", c.apply(EnumTypeChangesEnum.valueOf("C")));
    }

    void doRunExhaustive(Function<EnumTypeChangesEnum, String> c) throws Exception {
        try {
            c.apply(EnumTypeChangesEnum.valueOf("C"));
            throw new AssertionError();
        } catch (IncompatibleClassChangeError e) {
            //expected
        }
    }

    String statementEnum(EnumTypeChangesEnum e) {
        switch (e) {
            case A -> { return "A"; }
            case B -> { return "B"; }
            case EnumTypeChangesEnum e1 when false -> throw new AssertionError();
            default -> { return "D"; }
        }
    }

    String expressionEnum(EnumTypeChangesEnum e) {
        return switch (e) {
            case A -> "A";
            case B -> "B";
            case EnumTypeChangesEnum e1 when false -> throw new AssertionError();
            default -> "D";
        };
    }

    String statementEnumExhaustive(EnumTypeChangesEnum e) {
        switch (e) {
            case A -> { return "A"; }
            case B -> { return "B"; }
            case EnumTypeChangesEnum x when e == EnumTypeChangesEnum.A -> throw new AssertionError();
        }
        return "";
    }

    String expressionEnumExhaustive(EnumTypeChangesEnum e) {
        return switch (e) {
            case A -> "A";
            case B -> "B";
            case EnumTypeChangesEnum x when e == EnumTypeChangesEnum.A -> throw new AssertionError();
        };
    }

    private static void assertEquals(Object o1, Object o2) {
        if (!Objects.equals(o1, o2)) {
            throw new AssertionError();
        }
    }
}

enum EnumTypeChangesEnum {
    A,
    B;
}
