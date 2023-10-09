import it.auties.whatsapp.serialization.ControllerProvider;

open module it.auties.whatsappweb4j {
    requires static lombok;
    requires transitive java.desktop;

    requires jakarta.websocket;
    requires java.net.http;

    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jdk8;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires com.fasterxml.jackson.dataformat.smile;

    requires it.auties.protobuf.base;
    requires it.auties.protobuf.serializer;
    requires it.auties.map;

    requires com.google.zxing;
    requires com.google.zxing.javase;
    requires it.auties.qr;

    requires io.github.classgraph;

    requires org.bouncycastle.provider;
    requires it.auties.bytebuffer;
    requires it.auties.curve;
    requires com.github.benmanes.caffeine;
    requires it.auties.linkpreview;
    requires ez.vcard;

    exports it.auties.whatsapp.api;
    exports it.auties.whatsapp.controller;
    exports it.auties.whatsapp.model.action;
    exports it.auties.whatsapp.model.business;
    exports it.auties.whatsapp.model.button;
    exports it.auties.whatsapp.model.chat;
    exports it.auties.whatsapp.model.contact;
    exports it.auties.whatsapp.model.info;
    exports it.auties.whatsapp.model.media;
    exports it.auties.whatsapp.model.message.server;
    exports it.auties.whatsapp.model.message.model;
    exports it.auties.whatsapp.model.message.payment;
    exports it.auties.whatsapp.model.message.standard;
    exports it.auties.whatsapp.model.product;
    exports it.auties.whatsapp.model.setting;
    exports it.auties.whatsapp.model.request;
    exports it.auties.whatsapp.model.response;
    exports it.auties.whatsapp.model.payment;
    exports it.auties.whatsapp.model.message.button;
    exports it.auties.whatsapp.listener;
    exports it.auties.whatsapp.serialization;
    exports it.auties.whatsapp.exception;
    exports it.auties.whatsapp.util;
    exports it.auties.whatsapp.model.privacy;
    exports it.auties.whatsapp.model.location;
    exports it.auties.whatsapp.model.poll;

    uses ControllerProvider;
}