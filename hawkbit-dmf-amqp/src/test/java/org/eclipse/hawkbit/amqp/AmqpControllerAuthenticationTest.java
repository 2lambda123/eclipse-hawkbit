/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.amqp;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URL;

import org.eclipse.hawkbit.api.HostnameResolver;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifact;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifactHash;
import org.eclipse.hawkbit.dmf.amqp.api.MessageHeaderKey;
import org.eclipse.hawkbit.dmf.amqp.api.MessageType;
import org.eclipse.hawkbit.dmf.json.model.DownloadResponse;
import org.eclipse.hawkbit.dmf.json.model.TenantSecurityToken;
import org.eclipse.hawkbit.dmf.json.model.TenantSecurityToken.FileResource;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.jpa.JpaEntityFactory;
import org.eclipse.hawkbit.repository.jpa.model.JpaLocalArtifact;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.model.LocalArtifact;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.security.DdiSecurityProperties;
import org.eclipse.hawkbit.security.DdiSecurityProperties.Authentication.Anonymous;
import org.eclipse.hawkbit.security.DdiSecurityProperties.Rp;
import org.eclipse.hawkbit.security.SecurityContextTenantAware;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationKey;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.cache.Cache;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 *
 * Test Amqp controller authentication.
 */
@Features("Component Tests - Device Management Federation API")
@Stories("AmqpController Authentication Test")
@RunWith(MockitoJUnitRunner.class)
public class AmqpControllerAuthenticationTest {

    private static final String SHA1 = "12345";
    private static final Long ARTIFACT_ID = 1123L;
    private static final Long ARTIFACT_SIZE = 6666L;
    private static final String TENANT = "DEFAULT";
    private static final Long TENANT_ID = 123L;
    private static final String CONTROLLER_ID = "123";
    private static final Long TARGET_ID = 123L;
    private AmqpMessageHandlerService amqpMessageHandlerService;
    private AmqpAuthenticationMessageHandler amqpAuthenticationMessageHandlerService;

    private MessageConverter messageConverter;

    private AmqpControllerAuthentication authenticationManager;

    @Mock
    private TenantConfigurationManagement tenantConfigurationManagementMock;

    @Mock
    private SystemManagement systemManagement;

    @Mock
    private Cache cacheMock;

    @Mock
    private HostnameResolver hostnameResolverMock;

    @Mock
    private ArtifactManagement artifactManagementMock;

    @Mock
    private ControllerManagement controllerManagementMock;

    @Mock
    private Target targteMock;

    private static final TenantConfigurationValue<Boolean> CONFIG_VALUE_FALSE = TenantConfigurationValue
            .<Boolean> builder().value(Boolean.FALSE).build();

    private static final TenantConfigurationValue<Boolean> CONFIG_VALUE_TRUE = TenantConfigurationValue
            .<Boolean> builder().value(Boolean.TRUE).build();

    @Before
    public void before() throws Exception {
        messageConverter = new Jackson2JsonMessageConverter();
        final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        when(rabbitTemplate.getMessageConverter()).thenReturn(messageConverter);

        final DdiSecurityProperties secruityProperties = mock(DdiSecurityProperties.class);
        final Rp rp = mock(Rp.class);
        final org.eclipse.hawkbit.security.DdiSecurityProperties.Authentication ddiAuthentication = mock(
                org.eclipse.hawkbit.security.DdiSecurityProperties.Authentication.class);
        final Anonymous anonymous = mock(Anonymous.class);
        when(secruityProperties.getRp()).thenReturn(rp);
        when(rp.getSslIssuerHashHeader()).thenReturn("X-Ssl-Issuer-Hash-%d");
        when(secruityProperties.getAuthentication()).thenReturn(ddiAuthentication);
        when(ddiAuthentication.getAnonymous()).thenReturn(anonymous);
        when(anonymous.isEnabled()).thenReturn(false);

        when(tenantConfigurationManagementMock.getConfigurationValue(any(), eq(Boolean.class)))
                .thenReturn(CONFIG_VALUE_FALSE);

        final ControllerManagement controllerManagement = mock(ControllerManagement.class);
        when(controllerManagement.findByControllerId(anyString())).thenReturn(targteMock);
        when(controllerManagement.findByTargetId(any(Long.class))).thenReturn(targteMock);

        when(targteMock.getSecurityToken()).thenReturn(CONTROLLER_ID);
        when(targteMock.getControllerId()).thenReturn(CONTROLLER_ID);

        final SecurityContextTenantAware tenantAware = new SecurityContextTenantAware();
        final SystemSecurityContext systemSecurityContext = new SystemSecurityContext(tenantAware);

        final TenantMetaData tenantMetaData = mock(TenantMetaData.class);
        when(tenantMetaData.getTenant()).thenReturn(TENANT);
        when(systemManagement.getTenantMetadata(TENANT_ID)).thenReturn(tenantMetaData);

        authenticationManager = new AmqpControllerAuthentication(systemManagement, controllerManagement,
                tenantConfigurationManagementMock, tenantAware, secruityProperties, systemSecurityContext);

        authenticationManager.postConstruct();

        final LocalArtifact testArtifact = new JpaLocalArtifact("afilename", "afilename", new JpaSoftwareModule(
                new JpaSoftwareModuleType("a key", "a name", null, 1), "a name", null, null, null));

        when(artifactManagementMock.findLocalArtifact(ARTIFACT_ID)).thenReturn(testArtifact);
        when(artifactManagementMock.findFirstLocalArtifactsBySHA1(SHA1)).thenReturn(testArtifact);

        final DbArtifact artifact = new DbArtifact();
        artifact.setSize(ARTIFACT_SIZE);
        artifact.setHashes(new DbArtifactHash("sha1 test", "md5 test"));
        when(artifactManagementMock.loadLocalArtifactBinary(testArtifact)).thenReturn(artifact);

        amqpMessageHandlerService = new AmqpMessageHandlerService(rabbitTemplate,
                mock(AmqpMessageDispatcherService.class), controllerManagementMock, new JpaEntityFactory());

        amqpAuthenticationMessageHandlerService = new AmqpAuthenticationMessageHandler(rabbitTemplate,
                authenticationManager, artifactManagementMock, cacheMock, hostnameResolverMock,
                controllerManagementMock);

        when(hostnameResolverMock.resolveHostname()).thenReturn(new URL("http://localhost"));

        when(controllerManagementMock.hasTargetArtifactAssigned(TARGET_ID, testArtifact)).thenReturn(true);
        when(controllerManagementMock.hasTargetArtifactAssigned(CONTROLLER_ID, testArtifact)).thenReturn(true);
    }

    @Test
    @Description("Tests authentication manager without principal")
    public void testAuthenticationeBadCredantialsWithoutPricipal() {
        final TenantSecurityToken securityToken = new TenantSecurityToken(TENANT, TENANT_ID, CONTROLLER_ID, TARGET_ID,
                FileResource.createFileResourceBySha1(SHA1));
        try {
            authenticationManager.doAuthenticate(securityToken);
            fail("BadCredentialsException was excepeted since principal was missing");
        } catch (final BadCredentialsException exception) {
            // test ok - exception was excepted
        }

    }

    @Test
    @Description("Tests authentication manager without wrong credential")
    public void testAuthenticationBadCredantialsWithWrongCredential() {
        final TenantSecurityToken securityToken = new TenantSecurityToken(TENANT, TENANT_ID, CONTROLLER_ID, TARGET_ID,
                FileResource.createFileResourceBySha1(SHA1));
        when(tenantConfigurationManagementMock.getConfigurationValue(
                eq(TenantConfigurationKey.AUTHENTICATION_MODE_TARGET_SECURITY_TOKEN_ENABLED), eq(Boolean.class)))
                        .thenReturn(CONFIG_VALUE_TRUE);
        securityToken.getHeaders().put(TenantSecurityToken.AUTHORIZATION_HEADER, "TargetToken 12" + CONTROLLER_ID);
        try {
            authenticationManager.doAuthenticate(securityToken);
            fail("BadCredentialsException was excepeted due to wrong credential");
        } catch (final BadCredentialsException exception) {
            // test ok - exception was excepted
        }

    }

    @Test
    @Description("Tests authentication successfull")
    public void testSuccessfullAuthentication() {
        final TenantSecurityToken securityToken = new TenantSecurityToken(TENANT, TENANT_ID, CONTROLLER_ID, TARGET_ID,
                FileResource.createFileResourceBySha1(SHA1));
        when(tenantConfigurationManagementMock.getConfigurationValue(
                eq(TenantConfigurationKey.AUTHENTICATION_MODE_TARGET_SECURITY_TOKEN_ENABLED), eq(Boolean.class)))
                        .thenReturn(CONFIG_VALUE_TRUE);
        securityToken.getHeaders().put(TenantSecurityToken.AUTHORIZATION_HEADER, "TargetToken " + CONTROLLER_ID);
        final Authentication authentication = authenticationManager.doAuthenticate(securityToken);
        assertThat(authentication).isNotNull();
    }

    @Test
    @Description("Tests authentication message without principal")
    public void testAuthenticationMessageBadCredantialsWithoutPricipal() {
        final MessageProperties messageProperties = createMessageProperties(null);

        final TenantSecurityToken securityToken = new TenantSecurityToken(TENANT, TENANT_ID, CONTROLLER_ID, TARGET_ID,
                FileResource.createFileResourceBySha1(SHA1));
        final Message message = amqpMessageHandlerService.getMessageConverter().toMessage(securityToken,
                messageProperties);

        // test
        final Message onMessage = amqpAuthenticationMessageHandlerService.onAuthenticationRequest(message);

        // verify
        final DownloadResponse downloadResponse = (DownloadResponse) messageConverter.fromMessage(onMessage);
        assertThat(downloadResponse).isNotNull();
        assertThat(downloadResponse.getResponseCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @Description("Tests authentication message without wrong credential")
    public void testAuthenticationMessageBadCredantialsWithWrongCredential() {
        final MessageProperties messageProperties = createMessageProperties(null);
        final TenantSecurityToken securityToken = new TenantSecurityToken(TENANT, TENANT_ID, CONTROLLER_ID, TARGET_ID,
                FileResource.createFileResourceBySha1(SHA1));
        when(tenantConfigurationManagementMock.getConfigurationValue(
                eq(TenantConfigurationKey.AUTHENTICATION_MODE_TARGET_SECURITY_TOKEN_ENABLED), eq(Boolean.class)))
                        .thenReturn(CONFIG_VALUE_TRUE);
        securityToken.getHeaders().put(TenantSecurityToken.AUTHORIZATION_HEADER, "TargetToken 12" + CONTROLLER_ID);
        final Message message = amqpMessageHandlerService.getMessageConverter().toMessage(securityToken,
                messageProperties);

        // test
        final Message onMessage = amqpAuthenticationMessageHandlerService.onAuthenticationRequest(message);

        // verify
        final DownloadResponse downloadResponse = (DownloadResponse) messageConverter.fromMessage(onMessage);
        assertThat(downloadResponse).isNotNull();
        assertThat(downloadResponse.getResponseCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @Description("Tests authentication message successfull")
    public void successfullMessageAuthentication() {
        final MessageProperties messageProperties = createMessageProperties(null);
        final TenantSecurityToken securityToken = new TenantSecurityToken(TENANT, null, CONTROLLER_ID, null,
                FileResource.createFileResourceBySha1(SHA1));
        when(tenantConfigurationManagementMock.getConfigurationValue(
                eq(TenantConfigurationKey.AUTHENTICATION_MODE_TARGET_SECURITY_TOKEN_ENABLED), eq(Boolean.class)))
                        .thenReturn(CONFIG_VALUE_TRUE);
        securityToken.getHeaders().put(TenantSecurityToken.AUTHORIZATION_HEADER, "TargetToken " + CONTROLLER_ID);
        final Message message = amqpMessageHandlerService.getMessageConverter().toMessage(securityToken,
                messageProperties);

        // test
        final Message onMessage = amqpAuthenticationMessageHandlerService.onAuthenticationRequest(message);

        // verify
        final DownloadResponse downloadResponse = (DownloadResponse) messageConverter.fromMessage(onMessage);
        assertThat(downloadResponse).isNotNull();
        assertThat(downloadResponse.getDownloadUrl()).isNotNull();
        assertThat(downloadResponse.getResponseCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(SecurityContextHolder.getContext()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getClass().getName())
                .isEqualTo(PreAuthenticatedAuthenticationToken.class.getName());

    }

    @Test
    @Description("Tests authentication message successfull with targetId intead of controllerId provided and artifactId instead of SHA1.")
    public void successfullMessageAuthenticationWithTargetIdAndArtifactId() {
        final MessageProperties messageProperties = createMessageProperties(null);
        final TenantSecurityToken securityToken = new TenantSecurityToken(TENANT, null, null, TARGET_ID,
                FileResource.createFileResourceByArtifactId(ARTIFACT_ID));
        when(tenantConfigurationManagementMock.getConfigurationValue(
                eq(TenantConfigurationKey.AUTHENTICATION_MODE_TARGET_SECURITY_TOKEN_ENABLED), eq(Boolean.class)))
                        .thenReturn(CONFIG_VALUE_TRUE);
        securityToken.getHeaders().put(TenantSecurityToken.AUTHORIZATION_HEADER, "TargetToken " + CONTROLLER_ID);
        final Message message = amqpMessageHandlerService.getMessageConverter().toMessage(securityToken,
                messageProperties);

        // test
        final Message onMessage = amqpAuthenticationMessageHandlerService.onAuthenticationRequest(message);

        // verify
        final DownloadResponse downloadResponse = (DownloadResponse) messageConverter.fromMessage(onMessage);
        assertThat(downloadResponse).isNotNull();
        assertThat(downloadResponse.getDownloadUrl()).isNotNull();
        assertThat(downloadResponse.getResponseCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(SecurityContextHolder.getContext()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getClass().getName())
                .isEqualTo(PreAuthenticatedAuthenticationToken.class.getName());

    }

    @Test
    @Description("Tests authentication message successfull")
    public void successfullMessageAuthenticationWithTenantid() {
        final MessageProperties messageProperties = createMessageProperties(null);
        final TenantSecurityToken securityToken = new TenantSecurityToken(null, TENANT_ID, CONTROLLER_ID, TARGET_ID,
                FileResource.createFileResourceBySha1(SHA1));
        when(tenantConfigurationManagementMock.getConfigurationValue(
                eq(TenantConfigurationKey.AUTHENTICATION_MODE_TARGET_SECURITY_TOKEN_ENABLED), eq(Boolean.class)))
                        .thenReturn(CONFIG_VALUE_TRUE);
        securityToken.getHeaders().put(TenantSecurityToken.AUTHORIZATION_HEADER, "TargetToken " + CONTROLLER_ID);
        final Message message = amqpMessageHandlerService.getMessageConverter().toMessage(securityToken,
                messageProperties);

        // test
        final Message onMessage = amqpAuthenticationMessageHandlerService.onAuthenticationRequest(message);

        // verify
        final DownloadResponse downloadResponse = (DownloadResponse) messageConverter.fromMessage(onMessage);
        assertThat(downloadResponse).isNotNull();
        assertThat(downloadResponse.getDownloadUrl()).isNotNull();
        assertThat(downloadResponse.getResponseCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(SecurityContextHolder.getContext()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getClass().getName())
                .isEqualTo(PreAuthenticatedAuthenticationToken.class.getName());

    }

    private MessageProperties createMessageProperties(final MessageType type) {
        return createMessageProperties(type, "MyTest");
    }

    private MessageProperties createMessageProperties(final MessageType type, final String replyTo) {
        final MessageProperties messageProperties = new MessageProperties();
        if (type != null) {
            messageProperties.setHeader(MessageHeaderKey.TYPE, type.name());
        }
        messageProperties.setHeader(MessageHeaderKey.TENANT, TENANT);
        messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        messageProperties.setReplyTo(replyTo);
        return messageProperties;
    }

}
