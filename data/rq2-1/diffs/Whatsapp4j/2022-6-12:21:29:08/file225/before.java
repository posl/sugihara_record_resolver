package it.auties.whatsapp.model.signal.keypair;

import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.util.BytesHelper;
import lombok.NonNull;

import java.util.NoSuchElementException;
import java.util.Optional;

public record SignalSignedKeyPair(int id, @NonNull SignalKeyPair keyPair, byte[] signature) implements ISignalKeyPair{
    public static SignalSignedKeyPair of(int id, @NonNull SignalKeyPair identityKeyPair){
        var keyPair = SignalKeyPair.random();
        var signature = Curve25519.sign(identityKeyPair.privateKey(), keyPair.encodedPublicKey(), true);
        return new SignalSignedKeyPair(id, keyPair, signature);
    }

    public static Optional<SignalSignedKeyPair> of(Node node){
        if(node == null){
            return Optional.empty();
        }

        var id = node.findNode("id")
                .map(Node::bytes)
                .map(bytes -> BytesHelper.bytesToInt(bytes, 3))
                .orElseThrow(() -> new NoSuchElementException("Missing id in SignalSignedKeyPair"));
        var publicKey = node.findNode("value")
                .map(Node::bytes)
                .orElseThrow(() -> new NoSuchElementException("Missing publicKey in SignalSignedKeyPair"));
        var keyPair = new SignalKeyPair(publicKey, null);
        var signature = node.findNode("signature")
                .map(Node::bytes)
                .orElse(null);
        return Optional.of(new SignalSignedKeyPair(id, keyPair, signature));
    }

    @Override
    public SignalKeyPair toGenericKeyPair() {
        return keyPair();
    }

    @Override
    public byte[] publicKey() {
        return keyPair.publicKey();
    }

    @Override
    public byte[] privateKey() {
        return keyPair.privateKey();
    }

    public Node toNode(){
        return Node.withChildren("skey",
                Node.with("id", encodedId()),
                Node.with("value", publicKey()),
                Node.with("signature", signature())
        );
    }
}