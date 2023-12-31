// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: java_class.proto

package io.activej.dataflow.proto;

public final class JavaClassProto {
  private JavaClassProto() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  public interface JavaClassOrBuilder extends
      // @@protoc_insertion_point(interface_extends:dataflow.JavaClass)
      com.google.protobuf.MessageOrBuilder {

    /**
     * <code>string class_name = 1;</code>
     * @return The className.
     */
    java.lang.String getClassName();
    /**
     * <code>string class_name = 1;</code>
     * @return The bytes for className.
     */
    com.google.protobuf.ByteString
        getClassNameBytes();
  }
  /**
   * Protobuf type {@code dataflow.JavaClass}
   */
  public static final class JavaClass extends
      com.google.protobuf.GeneratedMessageV3 implements
      // @@protoc_insertion_point(message_implements:dataflow.JavaClass)
      JavaClassOrBuilder {
  private static final long serialVersionUID = 0L;
    // Use JavaClass.newBuilder() to construct.
    private JavaClass(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }
    private JavaClass() {
      className_ = "";
    }

    @java.lang.Override
    @SuppressWarnings({"unused"})
    protected java.lang.Object newInstance(
        UnusedPrivateParameter unused) {
      return new JavaClass();
    }

    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet
    getUnknownFields() {
      return this.unknownFields;
    }
    private JavaClass(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      this();
      if (extensionRegistry == null) {
        throw new java.lang.NullPointerException();
      }
      com.google.protobuf.UnknownFieldSet.Builder unknownFields =
          com.google.protobuf.UnknownFieldSet.newBuilder();
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            case 10: {
              java.lang.String s = input.readStringRequireUtf8();

              className_ = s;
              break;
            }
            default: {
              if (!parseUnknownField(
                  input, unknownFields, extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(
            e).setUnfinishedMessage(this);
      } finally {
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return io.activej.dataflow.proto.JavaClassProto.internal_static_dataflow_JavaClass_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return io.activej.dataflow.proto.JavaClassProto.internal_static_dataflow_JavaClass_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              io.activej.dataflow.proto.JavaClassProto.JavaClass.class, io.activej.dataflow.proto.JavaClassProto.JavaClass.Builder.class);
    }

    public static final int CLASS_NAME_FIELD_NUMBER = 1;
    private volatile java.lang.Object className_;
    /**
     * <code>string class_name = 1;</code>
     * @return The className.
     */
    @java.lang.Override
    public java.lang.String getClassName() {
      java.lang.Object ref = className_;
      if (ref instanceof java.lang.String) {
        return (java.lang.String) ref;
      } else {
        com.google.protobuf.ByteString bs = 
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        className_ = s;
        return s;
      }
    }
    /**
     * <code>string class_name = 1;</code>
     * @return The bytes for className.
     */
    @java.lang.Override
    public com.google.protobuf.ByteString
        getClassNameBytes() {
      java.lang.Object ref = className_;
      if (ref instanceof java.lang.String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        className_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    private byte memoizedIsInitialized = -1;
    @java.lang.Override
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1) return true;
      if (isInitialized == 0) return false;

      memoizedIsInitialized = 1;
      return true;
    }

    @java.lang.Override
    public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(className_)) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 1, className_);
      }
      unknownFields.writeTo(output);
    }

    @java.lang.Override
    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1) return size;

      size = 0;
      if (!com.google.protobuf.GeneratedMessageV3.isStringEmpty(className_)) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, className_);
      }
      size += unknownFields.getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object obj) {
      if (obj == this) {
       return true;
      }
      if (!(obj instanceof io.activej.dataflow.proto.JavaClassProto.JavaClass)) {
        return super.equals(obj);
      }
      io.activej.dataflow.proto.JavaClassProto.JavaClass other = (io.activej.dataflow.proto.JavaClassProto.JavaClass) obj;

      if (!getClassName()
          .equals(other.getClassName())) return false;
      if (!unknownFields.equals(other.unknownFields)) return false;
      return true;
    }

    @java.lang.Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      hash = (37 * hash) + CLASS_NAME_FIELD_NUMBER;
      hash = (53 * hash) + getClassName().hashCode();
      hash = (29 * hash) + unknownFields.hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static io.activej.dataflow.proto.JavaClassProto.JavaClass parseFrom(
        java.nio.ByteBuffer data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static io.activej.dataflow.proto.JavaClassProto.JavaClass parseFrom(
        java.nio.ByteBuffer data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static io.activej.dataflow.proto.JavaClassProto.JavaClass parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static io.activej.dataflow.proto.JavaClassProto.JavaClass parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static io.activej.dataflow.proto.JavaClassProto.JavaClass parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static io.activej.dataflow.proto.JavaClassProto.JavaClass parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static io.activej.dataflow.proto.JavaClassProto.JavaClass parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input);
    }
    public static io.activej.dataflow.proto.JavaClassProto.JavaClass parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input, extensionRegistry);
    }
    public static io.activej.dataflow.proto.JavaClassProto.JavaClass parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseDelimitedWithIOException(PARSER, input);
    }
    public static io.activej.dataflow.proto.JavaClassProto.JavaClass parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }
    public static io.activej.dataflow.proto.JavaClassProto.JavaClass parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input);
    }
    public static io.activej.dataflow.proto.JavaClassProto.JavaClass parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input, extensionRegistry);
    }

    @java.lang.Override
    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }
    public static Builder newBuilder(io.activej.dataflow.proto.JavaClassProto.JavaClass prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }
    @java.lang.Override
    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE
          ? new Builder() : new Builder().mergeFrom(this);
    }

    @java.lang.Override
    protected Builder newBuilderForType(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }
    /**
     * Protobuf type {@code dataflow.JavaClass}
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
        // @@protoc_insertion_point(builder_implements:dataflow.JavaClass)
        io.activej.dataflow.proto.JavaClassProto.JavaClassOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
          getDescriptor() {
        return io.activej.dataflow.proto.JavaClassProto.internal_static_dataflow_JavaClass_descriptor;
      }

      @java.lang.Override
      protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
          internalGetFieldAccessorTable() {
        return io.activej.dataflow.proto.JavaClassProto.internal_static_dataflow_JavaClass_fieldAccessorTable
            .ensureFieldAccessorsInitialized(
                io.activej.dataflow.proto.JavaClassProto.JavaClass.class, io.activej.dataflow.proto.JavaClassProto.JavaClass.Builder.class);
      }

      // Construct using io.activej.dataflow.proto.JavaClassProto.JavaClass.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(
          com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }
      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessageV3
                .alwaysUseFieldBuilders) {
        }
      }
      @java.lang.Override
      public Builder clear() {
        super.clear();
        className_ = "";

        return this;
      }

      @java.lang.Override
      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return io.activej.dataflow.proto.JavaClassProto.internal_static_dataflow_JavaClass_descriptor;
      }

      @java.lang.Override
      public io.activej.dataflow.proto.JavaClassProto.JavaClass getDefaultInstanceForType() {
        return io.activej.dataflow.proto.JavaClassProto.JavaClass.getDefaultInstance();
      }

      @java.lang.Override
      public io.activej.dataflow.proto.JavaClassProto.JavaClass build() {
        io.activej.dataflow.proto.JavaClassProto.JavaClass result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      @java.lang.Override
      public io.activej.dataflow.proto.JavaClassProto.JavaClass buildPartial() {
        io.activej.dataflow.proto.JavaClassProto.JavaClass result = new io.activej.dataflow.proto.JavaClassProto.JavaClass(this);
        result.className_ = className_;
        onBuilt();
        return result;
      }

      @java.lang.Override
      public Builder clone() {
        return super.clone();
      }
      @java.lang.Override
      public Builder setField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          java.lang.Object value) {
        return super.setField(field, value);
      }
      @java.lang.Override
      public Builder clearField(
          com.google.protobuf.Descriptors.FieldDescriptor field) {
        return super.clearField(field);
      }
      @java.lang.Override
      public Builder clearOneof(
          com.google.protobuf.Descriptors.OneofDescriptor oneof) {
        return super.clearOneof(oneof);
      }
      @java.lang.Override
      public Builder setRepeatedField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          int index, java.lang.Object value) {
        return super.setRepeatedField(field, index, value);
      }
      @java.lang.Override
      public Builder addRepeatedField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          java.lang.Object value) {
        return super.addRepeatedField(field, value);
      }
      @java.lang.Override
      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof io.activej.dataflow.proto.JavaClassProto.JavaClass) {
          return mergeFrom((io.activej.dataflow.proto.JavaClassProto.JavaClass)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(io.activej.dataflow.proto.JavaClassProto.JavaClass other) {
        if (other == io.activej.dataflow.proto.JavaClassProto.JavaClass.getDefaultInstance()) return this;
        if (!other.getClassName().isEmpty()) {
          className_ = other.className_;
          onChanged();
        }
        this.mergeUnknownFields(other.unknownFields);
        onChanged();
        return this;
      }

      @java.lang.Override
      public final boolean isInitialized() {
        return true;
      }

      @java.lang.Override
      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        io.activej.dataflow.proto.JavaClassProto.JavaClass parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (io.activej.dataflow.proto.JavaClassProto.JavaClass) e.getUnfinishedMessage();
          throw e.unwrapIOException();
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }

      private java.lang.Object className_ = "";
      /**
       * <code>string class_name = 1;</code>
       * @return The className.
       */
      public java.lang.String getClassName() {
        java.lang.Object ref = className_;
        if (!(ref instanceof java.lang.String)) {
          com.google.protobuf.ByteString bs =
              (com.google.protobuf.ByteString) ref;
          java.lang.String s = bs.toStringUtf8();
          className_ = s;
          return s;
        } else {
          return (java.lang.String) ref;
        }
      }
      /**
       * <code>string class_name = 1;</code>
       * @return The bytes for className.
       */
      public com.google.protobuf.ByteString
          getClassNameBytes() {
        java.lang.Object ref = className_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = 
              com.google.protobuf.ByteString.copyFromUtf8(
                  (java.lang.String) ref);
          className_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }
      /**
       * <code>string class_name = 1;</code>
       * @param value The className to set.
       * @return This builder for chaining.
       */
      public Builder setClassName(
          java.lang.String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  
        className_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>string class_name = 1;</code>
       * @return This builder for chaining.
       */
      public Builder clearClassName() {
        
        className_ = getDefaultInstance().getClassName();
        onChanged();
        return this;
      }
      /**
       * <code>string class_name = 1;</code>
       * @param value The bytes for className to set.
       * @return This builder for chaining.
       */
      public Builder setClassNameBytes(
          com.google.protobuf.ByteString value) {
        if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
        
        className_ = value;
        onChanged();
        return this;
      }
      @java.lang.Override
      public final Builder setUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.setUnknownFields(unknownFields);
      }

      @java.lang.Override
      public final Builder mergeUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.mergeUnknownFields(unknownFields);
      }


      // @@protoc_insertion_point(builder_scope:dataflow.JavaClass)
    }

    // @@protoc_insertion_point(class_scope:dataflow.JavaClass)
    private static final io.activej.dataflow.proto.JavaClassProto.JavaClass DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new io.activej.dataflow.proto.JavaClassProto.JavaClass();
    }

    public static io.activej.dataflow.proto.JavaClassProto.JavaClass getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<JavaClass>
        PARSER = new com.google.protobuf.AbstractParser<JavaClass>() {
      @java.lang.Override
      public JavaClass parsePartialFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
        return new JavaClass(input, extensionRegistry);
      }
    };

    public static com.google.protobuf.Parser<JavaClass> parser() {
      return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<JavaClass> getParserForType() {
      return PARSER;
    }

    @java.lang.Override
    public io.activej.dataflow.proto.JavaClassProto.JavaClass getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  private static final com.google.protobuf.Descriptors.Descriptor
    internal_static_dataflow_JavaClass_descriptor;
  private static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_dataflow_JavaClass_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\020java_class.proto\022\010dataflow\"\037\n\tJavaClas" +
      "s\022\022\n\nclass_name\030\001 \001(\tB-\n\031io.activej.data" +
      "flow.protoB\016JavaClassProtoP\000b\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        });
    internal_static_dataflow_JavaClass_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_dataflow_JavaClass_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_dataflow_JavaClass_descriptor,
        new java.lang.String[] { "ClassName", });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
