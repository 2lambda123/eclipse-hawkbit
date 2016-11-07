/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.test.util;

import static org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions.CONTROLLER_ROLE;
import static org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions.SYSTEM_ROLE;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.ExcludePathAwareShallowETagFilter;
import org.eclipse.hawkbit.TestConfiguration;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.RepositoryConstants;
import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorAction;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditionBuilder;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditions;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetWithActionType;
import org.eclipse.hawkbit.security.DosFilter;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.MethodRule;
import org.junit.rules.TestWatchman;
import org.junit.runner.RunWith;
import org.junit.runners.model.FrameworkMethod;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cloud.bus.ServiceMatcher;
import org.springframework.cloud.stream.test.binder.TestSupportBinderAutoConfiguration;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.google.common.collect.Lists;

import de.flapdoodle.embed.mongo.MongodExecutable;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ActiveProfiles({ "test" })
@WithUser(principal = "bumlux", allSpPermissions = true, authorities = { CONTROLLER_ROLE, SYSTEM_ROLE })
@SpringApplicationConfiguration(classes = { TestConfiguration.class, TestSupportBinderAutoConfiguration.class })
// destroy the context after each test class because otherwise we get problem
// when context is
// refreshed we e.g. get two instances of CacheManager which leads to very
// strange test failures.
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@TestPropertySource(properties = { "spring.data.mongodb.port=0", "spring.mongodb.embedded.version=3.2.7" })
public abstract class AbstractIntegrationTest implements EnvironmentAware {
    protected static Logger LOG = null;

    protected static final Pageable pageReq = new PageRequest(0, 400);

    /**
     * Constant for MediaType HAL with encoding UTF-8. Necessary since Spring
     * version 4.3.2 @see https://jira.spring.io/browse/SPR-14577
     */
    protected static final String APPLICATION_JSON_HAL_UTF = MediaTypes.HAL_JSON + ";charset=UTF-8";

    /**
     * Number of {@link DistributionSetType}s that exist in every test case. One
     * generated by using
     * {@link TestdataFactory#findOrCreateDefaultTestDsType()} and two
     * {@link SystemManagement#getTenantMetadata()};
     */
    protected static final int DEFAULT_DS_TYPES = RepositoryConstants.DEFAULT_DS_TYPES_IN_TENANT + 1;

    @Autowired
    protected EntityFactory entityFactory;

    @Autowired
    protected SoftwareManagement softwareManagement;

    @Autowired
    protected DistributionSetManagement distributionSetManagement;

    @Autowired
    protected ControllerManagement controllerManagement;

    @Autowired
    protected TargetManagement targetManagement;

    @Autowired
    protected TargetFilterQueryManagement targetFilterQueryManagement;

    @Autowired
    protected TagManagement tagManagement;

    @Autowired
    protected DeploymentManagement deploymentManagement;

    @Autowired
    protected ArtifactManagement artifactManagement;

    @Autowired
    protected WebApplicationContext context;

    @Autowired
    protected ControllerManagement controllerManagament;

    @Autowired
    protected AuditingHandler auditingHandler;

    @Autowired
    protected TenantAware tenantAware;

    @Autowired
    protected SystemManagement systemManagement;

    @Autowired
    protected TenantConfigurationManagement tenantConfigurationManagement;

    @Autowired
    protected RolloutManagement rolloutManagement;

    @Autowired
    protected RolloutGroupManagement rolloutGroupManagement;

    @Autowired
    protected SystemSecurityContext systemSecurityContext;

    @Autowired
    protected TestRepositoryManagement testRepositoryManagement;

    protected MockMvc mvc;

    protected SoftwareModuleType osType;
    protected SoftwareModuleType appType;
    protected SoftwareModuleType runtimeType;

    protected DistributionSetType standardDsType;

    @Autowired
    protected TestdataFactory testdataFactory;

    @Autowired
    protected GridFsOperations operations;

    @Autowired
    protected MongodExecutable mongodExecutable;

    @Autowired
    protected ServiceMatcher serviceMatcher;

    @Rule
    public final WithSpringAuthorityRule securityRule = new WithSpringAuthorityRule();

    protected Environment environment = null;

    @Override
    public void setEnvironment(final Environment environment) {
        this.environment = environment;
    }

    protected DistributionSetAssignmentResult assignDistributionSet(final Long dsID, final String controllerId) {
        return deploymentManagement.assignDistributionSet(dsID,
                Lists.newArrayList(new TargetWithActionType(controllerId, ActionType.FORCED,
                        org.eclipse.hawkbit.repository.model.RepositoryModelConstants.NO_FORCE_TIME)));
    }

    protected DistributionSetAssignmentResult assignDistributionSet(final DistributionSet pset,
            final List<Target> targets) {
        return deploymentManagement.assignDistributionSet(pset.getId(),
                targets.stream().map(Target::getTargetWithActionType).collect(Collectors.toList()));
    }

    protected DistributionSetAssignmentResult assignDistributionSet(final DistributionSet pset, final Target target) {
        return assignDistributionSet(pset, Lists.newArrayList(target));
    }

    protected DistributionSetMetadata createDistributionSetMetadata(final Long dsId, final MetaData md) {
        return distributionSetManagement.createDistributionSetMetadata(dsId, Collections.singletonList(md)).get(0);
    }

    protected Long getOsModule(final DistributionSet ds) {
        return ds.findFirstModuleByType(osType).getId();
    }

    protected Action prepareFinishedUpdate() {
        return prepareFinishedUpdate(TestdataFactory.DEFAULT_CONTROLLER_ID, "", false);
    }

    protected Action prepareFinishedUpdate(final String controllerId, final String distributionSet,
            final boolean isRequiredMigrationStep) {
        final DistributionSet ds = testdataFactory.createDistributionSet(distributionSet, isRequiredMigrationStep);
        Target savedTarget = testdataFactory.createTarget(controllerId);
        savedTarget = assignDistributionSet(ds.getId(), savedTarget.getControllerId()).getAssignedEntity().iterator()
                .next();
        Action savedAction = deploymentManagement.findActiveActionsByTarget(savedTarget).get(0);

        savedAction = controllerManagament.addUpdateActionStatus(
                entityFactory.actionStatus().create(savedAction.getId()).status(Action.Status.RUNNING));

        return controllerManagament.addUpdateActionStatus(
                entityFactory.actionStatus().create(savedAction.getId()).status(Action.Status.FINISHED));
    }

    protected Rollout createRolloutByVariables(final String rolloutName, final String rolloutDescription,
            final int groupSize, final String filterQuery, final DistributionSet distributionSet,
            final String successCondition, final String errorCondition) {
        final RolloutGroupConditions conditions = new RolloutGroupConditionBuilder()
                .successCondition(RolloutGroupSuccessCondition.THRESHOLD, successCondition)
                .errorCondition(RolloutGroupErrorCondition.THRESHOLD, errorCondition)
                .errorAction(RolloutGroupErrorAction.PAUSE, null).build();

        return rolloutManagement.createRollout(entityFactory.rollout().create().name(rolloutName)
                .description(rolloutDescription).targetFilterQuery(filterQuery).set(distributionSet), groupSize,
                conditions);
    }

    @Before
    public void before() throws Exception {
        mvc = createMvcWebAppContext().build();
        final String description = "Updated description to have lastmodified available in tests";

        osType = securityRule
                .runAsPrivileged(() -> testdataFactory.findOrCreateSoftwareModuleType(TestdataFactory.SM_TYPE_OS));
        osType = securityRule.runAsPrivileged(() -> softwareManagement.updateSoftwareModuleType(
                entityFactory.softwareModuleType().update(osType.getId()).description(description)));

        appType = securityRule.runAsPrivileged(
                () -> testdataFactory.findOrCreateSoftwareModuleType(TestdataFactory.SM_TYPE_APP, Integer.MAX_VALUE));
        appType = securityRule.runAsPrivileged(() -> softwareManagement.updateSoftwareModuleType(
                entityFactory.softwareModuleType().update(appType.getId()).description(description)));

        runtimeType = securityRule
                .runAsPrivileged(() -> testdataFactory.findOrCreateSoftwareModuleType(TestdataFactory.SM_TYPE_RT));
        runtimeType = securityRule.runAsPrivileged(() -> softwareManagement.updateSoftwareModuleType(
                entityFactory.softwareModuleType().update(runtimeType.getId()).description(description)));

        standardDsType = securityRule.runAsPrivileged(() -> testdataFactory.findOrCreateDefaultTestDsType());
    }

    @After
    public void after() {
        testRepositoryManagement.clearTestRepository();
    }

    @After
    public void cleanCurrentCollection() {
        operations.delete(new Query());
    }

    protected DefaultMockMvcBuilder createMvcWebAppContext() {
        return MockMvcBuilders.webAppContextSetup(context)
                .addFilter(new DosFilter(100, 10, "127\\.0\\.0\\.1|\\[0:0:0:0:0:0:0:1\\]", "(^192\\.168\\.)",
                        "X-Forwarded-For"))
                .addFilter(new ExcludePathAwareShallowETagFilter(
                        "/rest/v1/softwaremodules/{smId}/artifacts/{artId}/download", "/*/controller/artifacts/**"));
    }

    @Rule
    public MethodRule watchman = new TestWatchman() {
        @Override
        public void starting(final FrameworkMethod method) {
            if (LOG != null) {
                LOG.info("Starting Test {}...", method.getName());
            }
        }

        @Override
        public void succeeded(final FrameworkMethod method) {
            if (LOG != null) {
                LOG.info("Test {} succeeded.", method.getName());
            }
        }

        @Override
        public void failed(final Throwable e, final FrameworkMethod method) {
            if (LOG != null) {
                LOG.error("Test {} failed with {}.", method.getName(), e);
            }
        }
    };

    private static CIMySqlTestDatabase tesdatabase;

    @BeforeClass
    public static void beforeClass() {
        createTestdatabaseAndStart();
    }

    private static synchronized void createTestdatabaseAndStart() {
        if ("MYSQL".equals(System.getProperty("spring.jpa.database"))) {
            tesdatabase = new CIMySqlTestDatabase();
            tesdatabase.before();
        }
    }

    @AfterClass
    public static void afterClass() {
        if (tesdatabase != null) {
            tesdatabase.after();
        }
    }
}
