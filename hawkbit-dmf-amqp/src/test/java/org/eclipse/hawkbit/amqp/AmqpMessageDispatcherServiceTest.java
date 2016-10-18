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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.hawkbit.api.ArtifactUrl;
import org.eclipse.hawkbit.api.ArtifactUrlHandler;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifact;
import org.eclipse.hawkbit.dmf.amqp.api.EventTopic;
import org.eclipse.hawkbit.dmf.amqp.api.MessageHeaderKey;
import org.eclipse.hawkbit.dmf.amqp.api.MessageType;
import org.eclipse.hawkbit.dmf.json.model.DownloadAndUpdateRequest;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.event.local.CancelTargetAssignmentEvent;
import org.eclipse.hawkbit.repository.event.local.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.LocalArtifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.repository.test.util.AbstractIntegrationTest;
import org.eclipse.hawkbit.util.IpUtil;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.AbstractJavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.ActiveProfiles;

import com.google.common.collect.Lists;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@ActiveProfiles({ "test" })
@Features("Component Tests - Device Management Federation API")
@Stories("AmqpMessage Dispatcher Service Test")
@SpringApplicationConfiguration(classes = { org.eclipse.hawkbit.RepositoryApplicationConfiguration.class })
public class AmqpMessageDispatcherServiceTest extends AbstractIntegrationTest {

    private static final String TENANT = "default";
    private static final Long TENANT_ID = 4711L;

    private static final URI AMQP_URI = IpUtil.createAmqpUri("vHost", "mytest");

    private static final String TEST_TOKEN = "testToken";

    private AmqpMessageDispatcherService amqpMessageDispatcherService;

    private RabbitTemplate rabbitTemplate;

    private DefaultAmqpSenderService senderService;

    private SystemManagement systemManagement;

    private static final String CONTROLLER_ID = "1";

    private Target testTarget;

    @Override
    public void before() throws Exception {
        super.before();
        testTarget = entityFactory.generateTarget(CONTROLLER_ID, TEST_TOKEN);
        testTarget.getTargetInfo().setAddress(AMQP_URI.toString());

        this.rabbitTemplate = Mockito.mock(RabbitTemplate.class);
        when(rabbitTemplate.getMessageConverter()).thenReturn(new Jackson2JsonMessageConverter());

        senderService = Mockito.mock(DefaultAmqpSenderService.class);

        final ArtifactUrlHandler artifactUrlHandlerMock = Mockito.mock(ArtifactUrlHandler.class);
        when(artifactUrlHandlerMock.getUrls(anyObject(), anyObject()))
                .thenReturn(Lists.newArrayList(new ArtifactUrl("http", "download", "http://mockurl")));

        systemManagement = Mockito.mock(SystemManagement.class);
        final TenantMetaData tenantMetaData = Mockito.mock(TenantMetaData.class);
        when(tenantMetaData.getId()).thenReturn(TENANT_ID);
        when(tenantMetaData.getTenant()).thenReturn(TENANT);

        when(systemManagement.getTenantMetadata()).thenReturn(tenantMetaData);

        amqpMessageDispatcherService = new AmqpMessageDispatcherService(rabbitTemplate, senderService,
                artifactUrlHandlerMock, systemSecurityContext, systemManagement);

    }

    @Test
    @Description("Verfies that download and install event with no software modul works")
    public void testSendDownloadRequesWithEmptySoftwareModules() {
        final TargetAssignDistributionSetEvent targetAssignDistributionSetEvent = new TargetAssignDistributionSetEvent(
                TENANT, testTarget, 1L, new ArrayList<SoftwareModule>());
        amqpMessageDispatcherService.targetAssignDistributionSet(targetAssignDistributionSetEvent);
        final Message sendMessage = createArgumentCapture(
                targetAssignDistributionSetEvent.getTarget().getTargetInfo().getAddress());
        final DownloadAndUpdateRequest downloadAndUpdateRequest = assertDownloadAndInstallMessage(sendMessage);
        assertThat(downloadAndUpdateRequest.getTargetSecurityToken()).isEqualTo(TEST_TOKEN);
        assertTrue("No softwaremmodule should be contained in the request",
                downloadAndUpdateRequest.getSoftwareModules().isEmpty());
    }

    @Test
    @Description("Verfies that download and install event with 3 software moduls and no artifacts works")
    public void testSendDownloadRequesWithSoftwareModulesAndNoArtifacts() {
        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        final TargetAssignDistributionSetEvent targetAssignDistributionSetEvent = new TargetAssignDistributionSetEvent(
                TENANT, testTarget, 1L, dsA.getModules());
        amqpMessageDispatcherService.targetAssignDistributionSet(targetAssignDistributionSetEvent);
        final Message sendMessage = createArgumentCapture(
                targetAssignDistributionSetEvent.getTarget().getTargetInfo().getAddress());
        final DownloadAndUpdateRequest downloadAndUpdateRequest = assertDownloadAndInstallMessage(sendMessage);
        assertEquals("Expecting a size of 3 software modules in the reuqest", 3,
                downloadAndUpdateRequest.getSoftwareModules().size());
        assertThat(downloadAndUpdateRequest.getTargetSecurityToken()).isEqualTo(TEST_TOKEN);
        for (final org.eclipse.hawkbit.dmf.json.model.SoftwareModule softwareModule : downloadAndUpdateRequest
                .getSoftwareModules()) {
            assertTrue("Artifact list for softwaremodule should be empty", softwareModule.getArtifacts().isEmpty());
            for (final SoftwareModule softwareModule2 : dsA.getModules()) {
                assertNotNull("Sofware module ID should be set", softwareModule.getModuleId());
                if (!softwareModule.getModuleId().equals(softwareModule2.getId())) {
                    continue;
                }
                assertEquals(
                        "Software module type in event should be the same as the softwaremodule in the distribution set",
                        softwareModule.getModuleType(), softwareModule2.getType().getKey());
                assertEquals(
                        "Software module version in event should be the same as the softwaremodule in the distribution set",
                        softwareModule.getModuleVersion(), softwareModule2.getVersion());
            }
        }
    }

    @Test
    @Description("Verfies that download and install event with software moduls and artifacts works")
    public void testSendDownloadRequest() {
        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        final SoftwareModule module = dsA.getModules().iterator().next();
        final List<DbArtifact> receivedList = new ArrayList<>();
        for (final Artifact artifact : testdataFactory.createLocalArtifacts(module.getId())) {
            module.addArtifact((LocalArtifact) artifact);
            receivedList.add(new DbArtifact());
        }

        Mockito.when(rabbitTemplate.convertSendAndReceive(any())).thenReturn(receivedList);

        final TargetAssignDistributionSetEvent targetAssignDistributionSetEvent = new TargetAssignDistributionSetEvent(
                TENANT, testTarget, 1L, dsA.getModules());
        amqpMessageDispatcherService.targetAssignDistributionSet(targetAssignDistributionSetEvent);
        final Message sendMessage = createArgumentCapture(
                targetAssignDistributionSetEvent.getTarget().getTargetInfo().getAddress());
        final DownloadAndUpdateRequest downloadAndUpdateRequest = assertDownloadAndInstallMessage(sendMessage);
        assertEquals("DownloadAndUpdateRequest event should contains 3 software modules", 3,
                downloadAndUpdateRequest.getSoftwareModules().size());
        assertThat(downloadAndUpdateRequest.getTargetSecurityToken()).isEqualTo(TEST_TOKEN);

        for (final org.eclipse.hawkbit.dmf.json.model.SoftwareModule softwareModule : downloadAndUpdateRequest
                .getSoftwareModules()) {
            if (!softwareModule.getModuleId().equals(module.getId())) {
                continue;
            }
            assertThat(softwareModule.getArtifacts().size()).isEqualTo(module.getArtifacts().size()).isGreaterThan(0);

            module.getArtifacts().forEach(dbArtifact -> {
                final Optional<org.eclipse.hawkbit.dmf.json.model.Artifact> found = softwareModule.getArtifacts()
                        .stream().filter(dmfartifact -> dmfartifact.getFilename()
                                .equals(((LocalArtifact) dbArtifact).getFilename()))
                        .findFirst();

                assertTrue("The artifact should exist in message", found.isPresent());
                assertThat(found.get().getSize()).isEqualTo(dbArtifact.getSize());
                assertThat(found.get().getHashes().getMd5()).isEqualTo(dbArtifact.getMd5Hash());
                assertThat(found.get().getHashes().getSha1()).isEqualTo(dbArtifact.getSha1Hash());
            });
        }
    }

    @Test
    @Description("Verfies that send cancel event works")
    public void testSendCancelRequest() {
        final CancelTargetAssignmentEvent cancelTargetAssignmentDistributionSetEvent = new CancelTargetAssignmentEvent(
                testTarget, 1L);
        amqpMessageDispatcherService
                .targetCancelAssignmentToDistributionSet(cancelTargetAssignmentDistributionSetEvent);
        final Message sendMessage = createArgumentCapture(
                cancelTargetAssignmentDistributionSetEvent.getTarget().getTargetInfo().getAddress());
        assertCancelMessage(sendMessage);

    }

    private void assertCancelMessage(final Message sendMessage) {
        assertEventMessage(sendMessage);
        final Long actionId = convertMessage(sendMessage, Long.class);
        assertEquals("Action ID should be 1", actionId, Long.valueOf(1));
        assertEquals("The topc in the message should be a CANCEL_DOWNLOAD value", EventTopic.CANCEL_DOWNLOAD,
                sendMessage.getMessageProperties().getHeaders().get(MessageHeaderKey.TOPIC));

    }

    private DownloadAndUpdateRequest assertDownloadAndInstallMessage(final Message sendMessage) {
        assertEventMessage(sendMessage);
        final DownloadAndUpdateRequest downloadAndUpdateRequest = convertMessage(sendMessage,
                DownloadAndUpdateRequest.class);
        assertEquals("The action ID of the downloadAndUpdateRequest event shuold be 1",
                downloadAndUpdateRequest.getActionId(), Long.valueOf(1));
        assertEquals("The topic of the event shuold contain DOWNLOAD_AND_INSTALL", EventTopic.DOWNLOAD_AND_INSTALL,
                sendMessage.getMessageProperties().getHeaders().get(MessageHeaderKey.TOPIC));
        assertEquals("Security token of target", downloadAndUpdateRequest.getTargetSecurityToken(), TEST_TOKEN);

        return downloadAndUpdateRequest;

    }

    private void assertEventMessage(final Message sendMessage) {
        assertNotNull("The message should not be null", sendMessage);

        assertEquals("The value of the message header THING_ID should be " + CONTROLLER_ID, CONTROLLER_ID,
                sendMessage.getMessageProperties().getHeaders().get(MessageHeaderKey.THING_ID));
        assertEquals("The value of the message header TYPE should be EVENT", MessageType.EVENT,
                sendMessage.getMessageProperties().getHeaders().get(MessageHeaderKey.TYPE));
        assertEquals("The content type message should be " + MessageProperties.CONTENT_TYPE_JSON,
                MessageProperties.CONTENT_TYPE_JSON, sendMessage.getMessageProperties().getContentType());
    }

    protected Message createArgumentCapture(final URI uri) {
        final ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);
        Mockito.verify(senderService).sendMessage(argumentCaptor.capture(), eq(uri));
        return argumentCaptor.getValue();
    }

    @SuppressWarnings("unchecked")
    private <T> T convertMessage(final Message message, final Class<T> clazz) {
        message.getMessageProperties().getHeaders().put(AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME,
                clazz.getTypeName());
        return (T) rabbitTemplate.getMessageConverter().fromMessage(message);
    }

}
