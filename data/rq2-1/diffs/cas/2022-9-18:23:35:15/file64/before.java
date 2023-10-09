package org.apereo.cas.mfa.accepto.web.flow;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.web.flow.CasWebflowConstants;
import org.apereo.cas.web.flow.actions.StaticEventExecutionAction;
import org.apereo.cas.web.flow.configurer.AbstractCasMultifactorWebflowConfigurer;
import org.apereo.cas.web.flow.configurer.CasMultifactorWebflowCustomizer;

import lombok.val;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.StringUtils;
import org.springframework.webflow.definition.registry.FlowDefinitionRegistry;
import org.springframework.webflow.engine.ActionState;
import org.springframework.webflow.engine.builder.support.FlowBuilderServices;

import java.util.List;
import java.util.Optional;

/**
 * This is {@link AccepttoMultifactorWebflowConfigurer}.
 *
 * @author Misagh Moayyed
 * @since 6.1.0
 */
public class AccepttoMultifactorWebflowConfigurer extends AbstractCasMultifactorWebflowConfigurer {

    /**
     * Webflow event id.
     */
    public static final String MFA_ACCEPTTO_EVENT_ID = "mfa-acceptto";

    public AccepttoMultifactorWebflowConfigurer(final FlowBuilderServices flowBuilderServices,
                                                final FlowDefinitionRegistry loginFlowDefinitionRegistry,
                                                final FlowDefinitionRegistry flowDefinitionRegistry,
                                                final ConfigurableApplicationContext applicationContext,
                                                final CasConfigurationProperties casProperties,
                                                final List<CasMultifactorWebflowCustomizer> mfaFlowCustomizers) {
        super(flowBuilderServices, loginFlowDefinitionRegistry, applicationContext,
            casProperties, Optional.of(flowDefinitionRegistry), mfaFlowCustomizers);
    }

    @Override
    protected void doInitialize() {
        multifactorAuthenticationFlowDefinitionRegistries.forEach(registry -> {
            val flow = getFlow(registry, MFA_ACCEPTTO_EVENT_ID);
            flow.getStartActionList().add(createEvaluateAction(CasWebflowConstants.ACTION_ID_INITIAL_FLOW_SETUP));

            val initLoginFormState = createActionState(flow, CasWebflowConstants.STATE_ID_INIT_LOGIN_FORM,
                createEvaluateAction(CasWebflowConstants.ACTION_ID_INIT_LOGIN_ACTION));
            createTransitionForState(initLoginFormState, CasWebflowConstants.TRANSITION_ID_SUCCESS, "fetchUserAccountStatus");
            setStartState(flow, initLoginFormState);

            createEndState(flow, CasWebflowConstants.STATE_ID_SUCCESS);
            createEndState(flow, CasWebflowConstants.STATE_ID_UNAVAILABLE);
            createEndState(flow, CasWebflowConstants.STATE_ID_DENY);

            val fetchAccountState = createActionState(flow, "fetchUserAccountStatus",
                createEvaluateAction(CasWebflowConstants.ACTION_ID_ACCEPTTO_DETERMINE_USER_ACCOUNT_STATUS));
            createTransitionForState(fetchAccountState, CasWebflowConstants.TRANSITION_ID_UNAVAILABLE, CasWebflowConstants.STATE_ID_MFA_FAILURE);
            createTransitionForState(fetchAccountState, CasWebflowConstants.TRANSITION_ID_DENY, CasWebflowConstants.STATE_ID_DENY);
            createTransitionForState(fetchAccountState, CasWebflowConstants.TRANSITION_ID_REGISTER, CasWebflowConstants.STATE_ID_REGISTER_DEVICE);
            createTransitionForState(fetchAccountState, CasWebflowConstants.TRANSITION_ID_SUCCESS, "authenticateAndFetchChannel");
            createTransitionForState(fetchAccountState, CasWebflowConstants.TRANSITION_ID_APPROVE, CasWebflowConstants.STATE_ID_REAL_SUBMIT);

            val fetchChannelState = createActionState(flow, "authenticateAndFetchChannel",
                createEvaluateAction(CasWebflowConstants.ACTION_ID_ACCEPTTO_FETCH_CHANNEL));
            createTransitionForState(fetchChannelState, CasWebflowConstants.TRANSITION_ID_SUCCESS, "redirectToAcceptto");

            val validateState = createActionState(flow, "validateUser",
                createEvaluateAction(CasWebflowConstants.ACTION_ID_ACCEPTTO_VALIDATE_USER_DEVICE_REGISTRATION));
            createTransitionForState(validateState, CasWebflowConstants.TRANSITION_ID_FINALIZE, CasWebflowConstants.STATE_ID_REAL_SUBMIT);
            createTransitionForState(validateState, CasWebflowConstants.TRANSITION_ID_ERROR, CasWebflowConstants.STATE_ID_DENY);
            createTransitionForState(validateState, CasWebflowConstants.TRANSITION_ID_DENY, CasWebflowConstants.STATE_ID_DENY);

            val realSubmitState = createActionState(flow, CasWebflowConstants.STATE_ID_REAL_SUBMIT,
                createEvaluateAction(CasWebflowConstants.ACTION_ID_ACCEPTTO_FINALIZE_AUTHENTICATION));
            createTransitionForState(realSubmitState, CasWebflowConstants.TRANSITION_ID_SUCCESS, CasWebflowConstants.STATE_ID_SUCCESS);
            createTransitionForState(realSubmitState, CasWebflowConstants.TRANSITION_ID_ERROR, CasWebflowConstants.STATE_ID_VIEW_LOGIN_FORM);

            val redirectState = createViewState(flow, "redirectToAcceptto",
                createExternalRedirectViewFactory("requestScope.accepttoRedirectUrl"));
            createStateDefaultTransition(redirectState, CasWebflowConstants.STATE_ID_SUCCESS);

            val registerState = createViewState(flow, CasWebflowConstants.STATE_ID_REGISTER_DEVICE, "acceptto/casAccepttoRegistrationView");
            createTransitionForState(registerState, CasWebflowConstants.TRANSITION_ID_SUCCESS, "validateUser");
        });


        val flow = getLoginFlow();
        if (flow != null) {
            registerMultifactorProviderAuthenticationWebflow(flow, MFA_ACCEPTTO_EVENT_ID,
                casProperties.getAuthn().getMfa().getAcceptto().getId());
            val startState = getStartState(flow);
            addActionsToActionStateExecutionListAt(flow, startState.getId(), 0,
                createEvaluateAction(CasWebflowConstants.ACTION_ID_ACCEPTTO_VALIDATE_CHANNEL));

            createTransitionForState(startState,
                CasWebflowConstants.TRANSITION_ID_FINALIZE, "accepttoFinalizeAuthentication");
            val finalizeAuthN = createActionState(flow, "accepttoFinalizeAuthentication", StaticEventExecutionAction.SUCCESS);
            createTransitionForState(finalizeAuthN,
                CasWebflowConstants.TRANSITION_ID_SUCCESS, CasWebflowConstants.STATE_ID_CREATE_TICKET_GRANTING_TICKET);

            val acceptto = casProperties.getAuthn().getMfa().getAcceptto();
            if (acceptto.isQrLoginEnabled()) {
                val state = getState(flow, CasWebflowConstants.STATE_ID_INIT_LOGIN_FORM, ActionState.class);
                val applicationId = casProperties.getAuthn().getMfa().getAcceptto().getApplicationId();
                val setAction = createSetAction("flowScope.accepttoApplicationId", StringUtils.quote(applicationId));
                state.getEntryActionList().add(setAction);

                val qrSubmission = getState(flow, CasWebflowConstants.STATE_ID_VIEW_LOGIN_FORM);
                createTransitionForState(qrSubmission, "accepttoQRLogin", "validateWebSocketChannel");

                val validateAction = createActionState(flow, "validateWebSocketChannel", CasWebflowConstants.ACTION_ID_ACCEPTTO_QR_CODE_VALIDATE_CHANNEL);
                createTransitionForState(validateAction, CasWebflowConstants.TRANSITION_ID_FINALIZE, CasWebflowConstants.STATE_ID_REAL_SUBMIT);
            }
        }

    }
}
