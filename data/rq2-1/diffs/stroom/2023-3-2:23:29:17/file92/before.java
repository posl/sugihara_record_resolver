package stroom.dashboard.expression.v1;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.util.function.BiConsumer;
import java.util.function.Function;

public final class ValSerialiser {

    public static final Val[] EMPTY_VALUES = new Val[0];
    private static final Serialiser[] SERIALISERS = new Serialiser[20];

    static {
        SERIALISERS[Type.NULL.getId()] = new Serialiser(
                input -> ValNull.INSTANCE,
                (output, value) -> {
                });
        SERIALISERS[Type.BOOLEAN.getId()] = new Serialiser(
                input -> ValBoolean.create(input.readBoolean()),
                (output, value) -> output.writeBoolean(value.toBoolean()));
        SERIALISERS[Type.DOUBLE.getId()] = new Serialiser(
                input -> ValDouble.create(input.readDouble()),
                (output, value) -> output.writeDouble(value.toDouble()));
        SERIALISERS[Type.INTEGER.getId()] = new Serialiser(
                input -> ValInteger.create(input.readInt()),
                (output, value) -> output.writeInt(value.toInteger()));
        SERIALISERS[Type.LONG.getId()] = new Serialiser(
                input -> ValLong.create(input.readLong()),
                (output, value) -> output.writeLong(value.toLong()));
        SERIALISERS[Type.STRING.getId()] = new Serialiser(
                input -> ValString.create(input.readString()),
                (output, value) -> output.writeString(value.toString()));
        SERIALISERS[Type.ERR.getId()] = new Serialiser(
                input -> ValErr.create(input.readString()),
                (output, value) -> output.writeString(((ValErr) value).getMessage()));
    }

    static Val read(final Input input) {
        final int id = input.readByte();
        final Serialiser serialiser = SERIALISERS[id];
        return serialiser.reader.apply(input);
    }

    public static void write(final Output output, final Val val) {
        final byte id = val.type().getId();
        output.writeByte(id);
        final Serialiser serialiser = SERIALISERS[id];
        serialiser.writer.accept(output, val);
    }

    public static Val[] readArray(final Input input) {
        Val[] values = EMPTY_VALUES;

        final int valueCount = input.readByteUnsigned();
        if (valueCount > 0) {
            values = new Val[valueCount];
            for (int i = 0; i < valueCount; i++) {
                values[i] = read(input);
            }
        }

        return values;
    }

    public static void writeArray(final Output output, final Val[] values) {
        if (values.length > 255) {
            throw new RuntimeException("You can only write a maximum of " + 255 + " values");
        }
        output.writeByte(values.length);
        for (final Val val : values) {
            write(output, val);
        }
    }

//    public static byte[] toBytes(final Val[] values) {
//        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//        try (final Output output = new Output(byteArrayOutputStream)) {
//            if (values.length > 255) {
//                throw new RuntimeException("You can only write a maximum of " + 255 + " values");
//            }
//            output.writeByte(values.length);
//            for (final Val val : values) {
//                write(output, val);
//            }
//        }
//        return byteArrayOutputStream.toByteArray();
//    }
//
//    public static Val[] toVals(final byte[] bytes) {
//        try (final Input input = new Input(new ByteArrayInputStream(bytes))) {
//            return readArray(input);
//        }
//    }

//    private ValSerialisers() {
//        // Utility class.
//    }
//
//    public static void register(final Kryo kryo) {
//        kryo.register(ValBoolean.class, new ValBooleanSerialiser(), Type.BOOLEAN.getId());
//        kryo.register(ValDouble.class, new ValDoubleSerialiser(), Type.DOUBLE.getId());
//        kryo.register(ValInteger.class, new ValIntegerSerialiser(), Type.DOUBLE.getId());
//        kryo.register(ValLong.class, new ValLongSerialiser(), Type.LONG.getId());
//        kryo.register(ValString.class, new ValStringSerialiser(), Type.STRING.getId());
//        kryo.register(ValNull.class, new ValNullSerialiser(), Type.NULL.getId());
//        kryo.register(ValErr.class, new ValErrSerialiser(), Type.ERR.getId());
//        kryo.register(GroupKey.class, new GroupKeySerialiser(), 200);
//    }
//
//    private static final class ValBooleanSerialiser extends Serializer<ValBoolean> {
//        @Override
//        public void write(final Kryo kryo, final Output output, final ValBoolean object) {
//            output.writeBoolean(object.toBoolean());
//        }
//
//        @Override
//        public ValBoolean read(final Kryo kryo, final Input input, final Class<ValBoolean> type) {
//            return ValBoolean.create(input.readBoolean());
//        }
//    }
//
//    private static final class ValDoubleSerialiser extends Serializer<ValDouble> {
//        @Override
//        public void write(final Kryo kryo, final Output output, final ValDouble object) {
//            output.writeDouble(object.toDouble());
//        }
//
//        @Override
//        public ValDouble read(final Kryo kryo, final Input input, final Class<ValDouble> type) {
//            return ValDouble.create(input.readDouble());
//        }
//    }
//
//    private static final class ValIntegerSerialiser extends Serializer<ValInteger> {
//        @Override
//        public void write(final Kryo kryo, final Output output, final ValInteger object) {
//            output.writeInt(object.toInteger(), true);
//        }
//
//        @Override
//        public ValInteger read(final Kryo kryo, final Input input, final Class<ValInteger> type) {
//            return ValInteger.create(input.readInt(true));
//        }
//    }
//
//    private static final class ValLongSerialiser extends Serializer<ValLong> {
//        @Override
//        public void write(final Kryo kryo, final Output output, final ValLong object) {
//            output.writeLong(object.toLong(), true);
//        }
//
//        @Override
//        public ValLong read(final Kryo kryo, final Input input, final Class<ValLong> type) {
//            return ValLong.create(input.readLong(true));
//        }
//    }
//
//    private static final class ValStringSerialiser extends Serializer<ValString> {
//        @Override
//        public void write(final Kryo kryo, final Output output, final ValString object) {
//            output.writeString(object.toString());
//        }
//
//        @Override
//        public ValString read(final Kryo kryo, final Input input, final Class<ValString> type) {
//            return ValString.create(input.readString());
//        }
//    }
//
//    private static final class ValNullSerialiser extends Serializer<ValNull> {
//        @Override
//        public void write(final Kryo kryo, final Output output, final ValNull object) {
//        }
//
//        @Override
//        public ValNull read(final Kryo kryo, final Input input, final Class<ValNull> type) {
//            return ValNull.INSTANCE;
//        }
//    }
//
//    private static final class ValErrSerialiser extends Serializer<ValErr> {
//        @Override
//        public void write(final Kryo kryo, final Output output, final ValErr object) {
//            output.writeString(object.getMessage());
//        }
//
//        @Override
//        public ValErr read(final Kryo kryo, final Input input, final Class<ValErr> type) {
//            final ValErr valErr = ValErr.create(input.readString());
//            if (ValErr.INSTANCE.equals(valErr)) {
//                return ValErr.INSTANCE;
//            }
//            return valErr;
//        }
//    }
//
//    private static final class GroupKeySerialiser extends Serializer<GroupKey> {
//        @Override
//        public void write(final Kryo kryo, final Output output, final GroupKey object) {
//            kryo.writeClassAndObject(output, object.getParent());
//            final int size = object.getValues().size();
//            output.writeInt(size, true);
//            for (final Val val : object.getValues()) {
//                kryo.writeClassAndObject(output, val);
//            }
//        }
//
//        @Override
//        public GroupKey read(final Kryo kryo, final Input input, final Class<GroupKey> type) {
//            final GroupKey parent = (GroupKey) kryo.readClassAndObject(input);
//            final int size = input.readInt(true);
//            if (size == 1) {
//                final Val val = (Val) kryo.readClassAndObject(input);
//                return new GroupKey(parent, val);
//            }
//
//            final List<Val> list = new ArrayList<>(size);
//            for (int i = 0; i < size; i++) {
//                final Val val = (Val) kryo.readClassAndObject(input);
//                list.add(val);
//            }
//            return new GroupKey(parent, list);
//        }
//    }

    private static class Serialiser {

        final Function<Input, Val> reader;
        final BiConsumer<Output, Val> writer;

        public Serialiser(final Function<Input, Val> reader,
                          final BiConsumer<Output, Val> writer) {
            this.reader = reader;
            this.writer = writer;
        }
    }
}
