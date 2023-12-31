/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.ml.inference.nlp.tokenizers;

import org.elasticsearch.core.Releasable;
import org.elasticsearch.xpack.core.ml.inference.trainedmodel.BertTokenization;
import org.elasticsearch.xpack.core.ml.inference.trainedmodel.MPNetTokenization;
import org.elasticsearch.xpack.core.ml.inference.trainedmodel.Tokenization;
import org.elasticsearch.xpack.core.ml.utils.ExceptionsHelper;
import org.elasticsearch.xpack.ml.inference.nlp.NlpTask;
import org.elasticsearch.xpack.ml.inference.nlp.Vocabulary;

import java.util.List;
import java.util.OptionalInt;

import static org.elasticsearch.xpack.core.ml.inference.trainedmodel.NlpConfig.TOKENIZATION;
import static org.elasticsearch.xpack.core.ml.inference.trainedmodel.NlpConfig.VOCABULARY;

/**
 * Base tokenization class for NLP models
 */
public interface NlpTokenizer extends Releasable {

    TokenizationResult buildTokenizationResult(List<TokenizationResult.Tokens> tokenizations);

    List<TokenizationResult.Tokens> tokenize(String seq, Tokenization.Truncate truncate, int span, int sequenceId);

    TokenizationResult.Tokens tokenize(String seq1, String seq2, Tokenization.Truncate truncate, int sequenceId);

    NlpTask.RequestBuilder requestBuilder();

    OptionalInt getPadTokenId();

    String getPadToken();

    OptionalInt getMaskTokenId();

    String getMaskToken();

    default int getSpan() {
        return -1;
    }

    static NlpTokenizer build(Vocabulary vocabulary, Tokenization params) {
        ExceptionsHelper.requireNonNull(params, TOKENIZATION);
        ExceptionsHelper.requireNonNull(vocabulary, VOCABULARY);
        if (params instanceof BertTokenization) {
            return BertTokenizer.builder(vocabulary.get(), params).build();
        }
        if (params instanceof MPNetTokenization) {
            return MPNetTokenizer.mpBuilder(vocabulary.get(), params).build();
        }
        throw new IllegalArgumentException("unknown tokenization type [" + params.getName() + "]");
    }
}
