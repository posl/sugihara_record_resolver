/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.security.authc.jwt;

import org.elasticsearch.xpack.core.security.authc.jwt.JwtRealmSettings;
import org.junit.Before;

import java.text.ParseException;

import static org.hamcrest.Matchers.containsString;

public class JwtAuthenticatorIdTokenTypeTests extends JwtAuthenticatorTests {

    private String fallbackSub;
    private String fallbackAud;

    @Before
    public void beforeTest() {
        doBeforeTest();
        fallbackSub = null;
        fallbackAud = null;
    }

    @Override
    protected JwtRealmSettings.TokenType getTokenType() {
        return JwtRealmSettings.TokenType.ID_TOKEN;
    }

    public void testSubjectIsRequired() throws ParseException {
        final IllegalArgumentException e = doTestSubjectIsRequired(buildJwtAuthenticator(fallbackSub, fallbackAud));
        assertThat(e.getMessage(), containsString("missing required string claim [sub]"));
    }

    public void testInvalidIssuerIsCheckedBeforeAlgorithm() throws ParseException {
        doTestInvalidIssuerIsCheckedBeforeAlgorithm(buildJwtAuthenticator(fallbackSub, fallbackAud));
    }
}
