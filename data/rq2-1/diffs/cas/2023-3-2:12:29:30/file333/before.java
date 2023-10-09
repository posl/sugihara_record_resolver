package org.apereo.cas;

import org.apereo.cas.web.flow.AuthenticationViaFormActionTests;
import org.apereo.cas.web.flow.ConfirmLogoutActionTests;
import org.apereo.cas.web.flow.CreateTicketGrantingTicketActionTests;
import org.apereo.cas.web.flow.FetchTicketGrantingTicketActionTests;
import org.apereo.cas.web.flow.FinishLogoutActionTests;
import org.apereo.cas.web.flow.FlowExecutionExceptionResolverTests;
import org.apereo.cas.web.flow.FrontChannelLogoutActionTests;
import org.apereo.cas.web.flow.GatewayServicesManagementCheckActionTests;
import org.apereo.cas.web.flow.GenerateServiceTicketActionTests;
import org.apereo.cas.web.flow.GenericSuccessViewActionTests;
import org.apereo.cas.web.flow.InitialFlowSetupActionTests;
import org.apereo.cas.web.flow.InitializeLoginActionTests;
import org.apereo.cas.web.flow.LogoutActionTests;
import org.apereo.cas.web.flow.LogoutViewSetupActionTests;
import org.apereo.cas.web.flow.PopulateSpringSecurityContextActionTests;
import org.apereo.cas.web.flow.RedirectUnauthorizedServiceUrlActionTests;
import org.apereo.cas.web.flow.RenderLoginActionTests;
import org.apereo.cas.web.flow.SendTicketGrantingTicketActionTests;
import org.apereo.cas.web.flow.ServiceAuthorizationCheckActionTests;
import org.apereo.cas.web.flow.ServiceAuthorizationCheckMockitoActionTests;
import org.apereo.cas.web.flow.ServiceWarningActionTests;
import org.apereo.cas.web.flow.SessionStoreTicketGrantingTicketActionTests;
import org.apereo.cas.web.flow.SetServiceUnauthorizedRedirectUrlActionTests;
import org.apereo.cas.web.flow.SingleSignOnSessionCreatedActionTests;
import org.apereo.cas.web.flow.TerminateSessionActionTests;
import org.apereo.cas.web.flow.TerminateSessionConfirmingActionTests;
import org.apereo.cas.web.flow.TicketGrantingTicketCheckActionTests;
import org.apereo.cas.web.flow.VerifyRequiredServiceActionTests;
import org.apereo.cas.web.flow.account.AccountProfileRemoveSingleSignOnSessionTests;
import org.apereo.cas.web.flow.account.PrepareAccountProfileViewActionTests;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * This is {@link AllActionsTestsSuite}.
 *
 * @author Misagh Moayyed
 * @since 4.2.0
 */
@SelectClasses({
    AuthenticationViaFormActionTests.class,
    FrontChannelLogoutActionTests.class,
    GenerateServiceTicketActionTests.class,
    GenericSuccessViewActionTests.class,
    InitialFlowSetupActionTests.class,
    LogoutActionTests.class,
    FinishLogoutActionTests.class,
    SessionStoreTicketGrantingTicketActionTests.class,
    RedirectUnauthorizedServiceUrlActionTests.class,
    RenderLoginActionTests.class,
    AccountProfileRemoveSingleSignOnSessionTests.class,
    FlowExecutionExceptionResolverTests.class,
    InitializeLoginActionTests.class,
    PopulateSpringSecurityContextActionTests.class,
    SendTicketGrantingTicketActionTests.class,
    ServiceAuthorizationCheckMockitoActionTests.class,
    CreateTicketGrantingTicketActionTests.class,
    TicketGrantingTicketCheckActionTests.class,
    ServiceWarningActionTests.class,
    FetchTicketGrantingTicketActionTests.class,
    PrepareAccountProfileViewActionTests.class,
    ConfirmLogoutActionTests.class,
    SingleSignOnSessionCreatedActionTests.class,
    LogoutViewSetupActionTests.class,
    TerminateSessionActionTests.class,
    VerifyRequiredServiceActionTests.class,
    SetServiceUnauthorizedRedirectUrlActionTests.class,
    TerminateSessionConfirmingActionTests.class,
    GatewayServicesManagementCheckActionTests.class,
    ServiceAuthorizationCheckActionTests.class
})
@Suite
public class AllActionsTestsSuite {
}
