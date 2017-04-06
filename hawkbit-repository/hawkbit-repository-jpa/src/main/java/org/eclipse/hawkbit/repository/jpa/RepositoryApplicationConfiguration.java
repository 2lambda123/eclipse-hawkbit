/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.Map;

import javax.persistence.EntityManager;
import javax.sql.DataSource;

import org.eclipse.hawkbit.ControllerPollProperties;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.ReportManagement;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.TenantStatsManagement;
import org.eclipse.hawkbit.repository.builder.DistributionSetBuilder;
import org.eclipse.hawkbit.repository.builder.DistributionSetTypeBuilder;
import org.eclipse.hawkbit.repository.builder.RolloutBuilder;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleBuilder;
import org.eclipse.hawkbit.repository.builder.TargetFilterQueryBuilder;
import org.eclipse.hawkbit.repository.event.remote.EventEntityManager;
import org.eclipse.hawkbit.repository.event.remote.EventEntityManagerHolder;
import org.eclipse.hawkbit.repository.jpa.aspects.ExceptionMappingAspectHandler;
import org.eclipse.hawkbit.repository.jpa.autoassign.AutoAssignChecker;
import org.eclipse.hawkbit.repository.jpa.autoassign.AutoAssignScheduler;
import org.eclipse.hawkbit.repository.jpa.builder.JpaDistributionSetBuilder;
import org.eclipse.hawkbit.repository.jpa.builder.JpaDistributionSetTypeBuilder;
import org.eclipse.hawkbit.repository.jpa.builder.JpaRolloutBuilder;
import org.eclipse.hawkbit.repository.jpa.builder.JpaSoftwareModuleBuilder;
import org.eclipse.hawkbit.repository.jpa.builder.JpaTargetFilterQueryBuilder;
import org.eclipse.hawkbit.repository.jpa.configuration.MultiTenantJpaTransactionManager;
import org.eclipse.hawkbit.repository.jpa.event.JpaEventEntityManager;
import org.eclipse.hawkbit.repository.jpa.model.helper.AfterTransactionCommitExecutorHolder;
import org.eclipse.hawkbit.repository.jpa.model.helper.EntityInterceptorHolder;
import org.eclipse.hawkbit.repository.jpa.model.helper.SecurityTokenGeneratorHolder;
import org.eclipse.hawkbit.repository.jpa.model.helper.SystemSecurityContextHolder;
import org.eclipse.hawkbit.repository.jpa.model.helper.TenantAwareHolder;
import org.eclipse.hawkbit.repository.jpa.rollout.RolloutScheduler;
import org.eclipse.hawkbit.repository.jpa.rsql.RsqlParserValidationOracle;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.helper.SystemManagementHolder;
import org.eclipse.hawkbit.repository.model.helper.TenantConfigurationManagementHolder;
import org.eclipse.hawkbit.repository.rsql.RsqlValidationOracle;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.eclipse.hawkbit.security.SecurityTokenGenerator;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.orm.jpa.JpaBaseConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.orm.jpa.vendor.AbstractJpaVendorAdapter;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import com.google.common.collect.Maps;

/**
 * General configuration for hawkBit's Repository.
 *
 */
@EnableJpaRepositories(basePackages = { "org.eclipse.hawkbit.repository.jpa" })
@EnableTransactionManagement
@EnableJpaAuditing
@EnableAspectJAutoProxy
@Configuration
@ComponentScan
@EnableConfigurationProperties({ RepositoryProperties.class, ControllerPollProperties.class,
        TenantConfigurationProperties.class })
@EnableScheduling
@EntityScan("org.eclipse.hawkbit.repository.jpa.model")
public class RepositoryApplicationConfiguration extends JpaBaseConfiguration {

    @Autowired
    RepositoryApplicationConfiguration(final DataSource dataSource, final JpaProperties jpaProperties,
            final ObjectProvider<JtaTransactionManager> jtaTransactionManagerProvider) {
        super(dataSource, jpaProperties, jtaTransactionManagerProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    RsqlValidationOracle rsqlValidationOracle() {
        return new RsqlParserValidationOracle();
    }

    /**
     * @param distributionSetManagement
     *            to loading the {@link DistributionSetType}
     * @param softwareManagement
     *            for loading {@link DistributionSet#getModules()}
     * @return DistributionSetBuilder bean
     */
    @Bean
    DistributionSetBuilder distributionSetBuilder(final DistributionSetManagement distributionSetManagement,
            final SoftwareManagement softwareManagement) {
        return new JpaDistributionSetBuilder(distributionSetManagement, softwareManagement);
    }

    /**
     * @param softwareManagement
     *            for loading
     *            {@link DistributionSetType#getMandatoryModuleTypes()} and
     *            {@link DistributionSetType#getOptionalModuleTypes()}
     * @return DistributionSetTypeBuilder bean
     */
    @Bean
    DistributionSetTypeBuilder distributionSetTypeBuilder(final SoftwareManagement softwareManagement) {
        return new JpaDistributionSetTypeBuilder(softwareManagement);
    }

    /**
     * @param softwareManagement
     *            for loading {@link SoftwareModule#getType()}
     * @return SoftwareModuleBuilder bean
     */
    @Bean
    SoftwareModuleBuilder softwareModuleBuilder(final SoftwareManagement softwareManagement) {
        return new JpaSoftwareModuleBuilder(softwareManagement);
    }

    /**
     * @param distributionSetManagement
     *            for loading {@link Rollout#getDistributionSet()}
     * @return RolloutBuilder bean
     */
    @Bean
    RolloutBuilder rolloutBuilder(final DistributionSetManagement distributionSetManagement) {
        return new JpaRolloutBuilder(distributionSetManagement);
    }

    /**
     * @param distributionSetManagement
     *            for loading
     *            {@link TargetFilterQuery#getAutoAssignDistributionSet()}
     * @return TargetFilterQueryBuilder bean
     */
    @Bean
    TargetFilterQueryBuilder targetFilterQueryBuilder(final DistributionSetManagement distributionSetManagement) {
        return new JpaTargetFilterQueryBuilder(distributionSetManagement);
    }

    /**
     * @return the {@link SystemSecurityContext} singleton bean which make it
     *         accessible in beans which cannot access the service directly,
     *         e.g. JPA entities.
     */
    @Bean
    SystemSecurityContextHolder systemSecurityContextHolder() {
        return SystemSecurityContextHolder.getInstance();
    }

    /**
     * @return the {@link TenantConfigurationManagement} singleton bean which
     *         make it accessible in beans which cannot access the service
     *         directly, e.g. JPA entities.
     */
    @Bean
    TenantConfigurationManagementHolder tenantConfigurationManagementHolder() {
        return TenantConfigurationManagementHolder.getInstance();
    }

    /**
     * @return the {@link SystemManagementHolder} singleton bean which holds the
     *         current {@link SystemManagement} service and make it accessible
     *         in beans which cannot access the service directly, e.g. JPA
     *         entities.
     */
    @Bean
    SystemManagementHolder systemManagementHolder() {
        return SystemManagementHolder.getInstance();
    }

    /**
     * @return the {@link TenantAwareHolder} singleton bean which holds the
     *         current {@link TenantAware} service and make it accessible in
     *         beans which cannot access the service directly, e.g. JPA
     *         entities.
     */
    @Bean
    TenantAwareHolder tenantAwareHolder() {
        return TenantAwareHolder.getInstance();
    }

    /**
     * @return the {@link SecurityTokenGeneratorHolder} singleton bean which
     *         holds the current {@link SecurityTokenGenerator} service and make
     *         it accessible in beans which cannot access the service via
     *         injection
     */
    @Bean
    SecurityTokenGeneratorHolder securityTokenGeneratorHolder() {
        return SecurityTokenGeneratorHolder.getInstance();
    }

    /**
     * @return the singleton instance of the {@link EntityInterceptorHolder}
     */
    @Bean
    EntityInterceptorHolder entityInterceptorHolder() {
        return EntityInterceptorHolder.getInstance();
    }

    /**
     *
     * @return the singleton instance of the
     *         {@link AfterTransactionCommitExecutorHolder}
     */
    @Bean
    AfterTransactionCommitExecutorHolder afterTransactionCommitExecutorHolder() {
        return AfterTransactionCommitExecutorHolder.getInstance();
    }

    /**
     * Defines the validation processor bean.
     *
     * @return the {@link MethodValidationPostProcessor}
     */
    @Bean
    public MethodValidationPostProcessor methodValidationPostProcessor() {
        return new MethodValidationPostProcessor();
    }

    /**
     * @return {@link ExceptionMappingAspectHandler} aspect bean
     */
    @Bean
    ExceptionMappingAspectHandler createRepositoryExceptionHandlerAdvice() {
        return new ExceptionMappingAspectHandler();
    }

    @Override
    protected AbstractJpaVendorAdapter createJpaVendorAdapter() {
        return new EclipseLinkJpaVendorAdapter();
    }

    @Override
    protected Map<String, Object> getVendorProperties() {

        final Map<String, Object> properties = Maps.newHashMapWithExpectedSize(4);
        // Turn off dynamic weaving to disable LTW lookup in static weaving mode
        properties.put("eclipselink.weaving", "false");
        // needed for reports
        properties.put("eclipselink.jdbc.allow-native-sql-queries", "true");
        // flyway
        properties.put("eclipselink.ddl-generation", "none");
        // Embeed into hawkBit logging
        properties.put("eclipselink.logging.logger", "JavaLogger");

        return properties;
    }

    /**
     * {@link MultiTenantJpaTransactionManager} bean.
     *
     * @see org.springframework.boot.autoconfigure.orm.jpa.JpaBaseConfiguration#transactionManager()
     * @return a new {@link PlatformTransactionManager}
     */
    @Override
    @Bean
    public PlatformTransactionManager transactionManager() {
        return new MultiTenantJpaTransactionManager();
    }

    /**
     * {@link JpaSystemManagement} bean.
     *
     * @return a new {@link SystemManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    SystemManagement systemManagement() {
        return new JpaSystemManagement();
    }

    /**
     * {@link JpaReportManagement} bean.
     *
     * @return a new {@link ReportManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    ReportManagement reportManagement() {
        return new JpaReportManagement();
    }

    /**
     * {@link JpaDistributionSetManagement} bean.
     *
     * @return a new {@link DistributionSetManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    DistributionSetManagement distributionSetManagement() {
        return new JpaDistributionSetManagement();
    }

    /**
     * {@link JpaTenantStatsManagement} bean.
     *
     * @return a new {@link TenantStatsManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    TenantStatsManagement tenantStatsManagement() {
        return new JpaTenantStatsManagement();
    }

    /**
     * {@link JpaTenantConfigurationManagement} bean.
     *
     * @return a new {@link TenantConfigurationManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    TenantConfigurationManagement tenantConfigurationManagement() {
        return new JpaTenantConfigurationManagement();
    }

    /**
     * {@link JpaTenantConfigurationManagement} bean.
     *
     * @return a new {@link TenantConfigurationManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    TargetManagement targetManagement() {
        return new JpaTargetManagement();
    }

    /**
     * {@link JpaTargetFilterQueryManagement} bean.
     * 
     * @param targetFilterQueryRepository
     *            to query entity access
     * @param virtualPropertyReplacer
     *            for RSQL handling
     * @param distributionSetManagement
     *            for auto assign DS access
     *
     * @return a new {@link TargetFilterQueryManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    TargetFilterQueryManagement targetFilterQueryManagement(
            final TargetFilterQueryRepository targetFilterQueryRepository,
            final VirtualPropertyReplacer virtualPropertyReplacer,
            final DistributionSetManagement distributionSetManagement) {
        return new JpaTargetFilterQueryManagement(targetFilterQueryRepository, virtualPropertyReplacer,
                distributionSetManagement);
    }

    /**
     * {@link JpaTagManagement} bean.
     *
     * @return a new {@link TagManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    TagManagement tagManagement() {
        return new JpaTagManagement();
    }

    /**
     * {@link JpaSoftwareManagement} bean.
     *
     * @return a new {@link SoftwareManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    SoftwareManagement softwareManagement() {
        return new JpaSoftwareManagement();
    }

    @Bean
    @ConditionalOnMissingBean
    RolloutManagement rolloutManagement(final TargetManagement targetManagement,
            final DeploymentManagement deploymentManagement, final RolloutGroupManagement rolloutGroupManagement,
            final DistributionSetManagement distributionSetManagement, final ApplicationContext context,
            final ApplicationEventPublisher eventPublisher, final VirtualPropertyReplacer virtualPropertyReplacer,
            final PlatformTransactionManager txManager, final TenantAware tenantAware,
            final LockRegistry lockRegistry) {
        return new JpaRolloutManagement(targetManagement, deploymentManagement, rolloutGroupManagement,
                distributionSetManagement, context, eventPublisher, virtualPropertyReplacer, txManager, tenantAware,
                lockRegistry);
    }

    /**
     * {@link JpaRolloutGroupManagement} bean.
     *
     * @return a new {@link RolloutGroupManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    RolloutGroupManagement rolloutGroupManagement() {
        return new JpaRolloutGroupManagement();
    }

    /**
     * {@link JpaDeploymentManagement} bean.
     *
     * @return a new {@link DeploymentManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    DeploymentManagement deploymentManagement() {
        return new JpaDeploymentManagement();
    }

    /**
     * {@link JpaControllerManagement} bean.
     *
     * @return a new {@link ControllerManagement}
     */
    @Bean
    @ConditionalOnMissingBean
    ControllerManagement controllerManagement() {
        return new JpaControllerManagement();
    }

    /**
     * {@link JpaArtifactManagement} bean.
     *
     * @return a new {@link ArtifactManagement}
     */

    @Bean
    @ConditionalOnMissingBean
    ArtifactManagement artifactManagement() {
        return new JpaArtifactManagement();
    }

    /**
     * {@link JpaEntityFactory} bean.
     *
     * @return a new {@link EntityFactory}
     */
    @Bean
    @ConditionalOnMissingBean
    public EntityFactory entityFactory() {
        return new JpaEntityFactory();
    }

    /**
     * {@link EventEntityManagerHolder} bean.
     *
     * @return a new {@link EventEntityManagerHolder}
     */
    @Bean
    @ConditionalOnMissingBean
    EventEntityManagerHolder eventEntityManagerHolder() {
        return EventEntityManagerHolder.getInstance();
    }

    /**
     * {@link EventEntityManager} bean.
     *
     * @param aware
     *            the tenant aware
     * @param entityManager
     *            the entitymanager
     * @return a new {@link EventEntityManager}
     */
    @Bean
    @ConditionalOnMissingBean
    EventEntityManager eventEntityManager(final TenantAware aware, final EntityManager entityManager) {
        return new JpaEventEntityManager(aware, entityManager);
    }

    /**
     * {@link AutoAssignChecker} bean.
     *
     * @param targetFilterQueryManagement
     *            to get all target filter queries
     * @param targetManagement
     *            to get targets
     * @param deploymentManagement
     *            to assign distribution sets to targets
     * @param transactionManager
     *            to run transactions
     * @return a new {@link AutoAssignChecker}
     */
    @Bean
    @ConditionalOnMissingBean
    AutoAssignChecker autoAssignChecker(final TargetFilterQueryManagement targetFilterQueryManagement,
            final TargetManagement targetManagement, final DeploymentManagement deploymentManagement,
            final PlatformTransactionManager transactionManager) {
        return new AutoAssignChecker(targetFilterQueryManagement, targetManagement, deploymentManagement,
                transactionManager);
    }

    /**
     * {@link AutoAssignScheduler} bean.
     * 
     * Note: does not activate in test profile, otherwise it is hard to test the
     * auto assign functionality.
     *
     * @param tenantAware
     *            to run as specific tenant
     * @param systemManagement
     *            to find all tenants
     * @param systemSecurityContext
     *            to run as system
     * @param autoAssignChecker
     *            to run a check as tenant
     * @param lockRegistry
     *            to lock the tenant for auto assignment
     * @return a new {@link AutoAssignChecker}
     */
    @Bean
    @ConditionalOnMissingBean
    // don't active the auto assign scheduler in test, otherwise it is hard to
    // test
    @Profile("!test")
    @ConditionalOnProperty(prefix = "hawkbit.autoassign.scheduler", name = "enabled", matchIfMissing = true)
    AutoAssignScheduler autoAssignScheduler(final TenantAware tenantAware, final SystemManagement systemManagement,
            final SystemSecurityContext systemSecurityContext, final AutoAssignChecker autoAssignChecker,
            final LockRegistry lockRegistry) {
        return new AutoAssignScheduler(tenantAware, systemManagement, systemSecurityContext, autoAssignChecker,
                lockRegistry);
    }

    /**
     * {@link RolloutScheduler} bean.
     * 
     * Note: does not activate in test profile, otherwise it is hard to test the
     * rollout handling functionality.
     * 
     * @param tenantAware
     *            to run as specific tenant
     * @param systemManagement
     *            to find all tenants
     * @param rolloutManagement
     *            to run the rollout handler
     * @param systemSecurityContext
     *            to run as system
     * @return a new {@link RolloutScheduler} bean.
     */
    @Bean
    @ConditionalOnMissingBean
    @Profile("!test")
    @ConditionalOnProperty(prefix = "hawkbit.rollout.scheduler", name = "enabled", matchIfMissing = true)
    RolloutScheduler rolloutScheduler(final TenantAware tenantAware, final SystemManagement systemManagement,
            final RolloutManagement rolloutManagement, final SystemSecurityContext systemSecurityContext) {
        return new RolloutScheduler(tenantAware, systemManagement, rolloutManagement, systemSecurityContext);
    }
}
