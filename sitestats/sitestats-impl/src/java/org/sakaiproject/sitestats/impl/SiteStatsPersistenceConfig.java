package org.sakaiproject.sitestats.impl;

import com.zaxxer.hikari.HikariDataSource;
import org.hibernate.SessionFactory;
import org.jfree.util.Log;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.springframework.orm.hibernate.AdditionalHibernateMappings;
import org.sakaiproject.springframework.orm.hibernate.impl.AdditionalHibernateMappingsImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.orm.hibernate4.HibernateTransactionManager;
import org.springframework.orm.hibernate4.LocalSessionFactoryBuilder;
import org.springframework.transaction.PlatformTransactionManager;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.io.IOException;
import java.util.Properties;

public class SiteStatsPersistenceConfig {

    @Lazy
    @Resource(name = "org.sakaiproject.springframework.orm.hibernate.GlobalSessionFactory")
    private SessionFactory globalSessionFactory;

    @Lazy
    @Resource(name = "org.sakaiproject.springframework.orm.hibernate.GlobalTransactionManager")
    private PlatformTransactionManager globalTransactionManager;

    @Lazy
    @Resource
    private ServerConfigurationService serverConfigurationService;

    private PlatformTransactionManager siteStatsTransactionManager;
    private SessionFactory siteStatsSessionFactory;

    @Bean(name = "org.sakaiproject.sitestats.SiteStatsTransactionManager")
    public PlatformTransactionManager getSiteStatsTransactionManager() throws IOException {
        if (siteStatsTransactionManager == null) {
            String whichDb = serverConfigurationService.getString("sitestats.db", "internal");
            if ("external".equals(whichDb)) {
                HibernateTransactionManager tx = new HibernateTransactionManager();
                tx.setSessionFactory(getSiteStatsSessionFactory());
                siteStatsTransactionManager = tx;
            } else {
                siteStatsTransactionManager = globalTransactionManager;
            }
        }
        return siteStatsTransactionManager;
    }

    @Bean(name = "org.sakaiproject.sitestats.SiteStatsSessionFactory")
    public SessionFactory getSiteStatsSessionFactory() throws IOException {
        if (siteStatsSessionFactory == null) {
            String whichDb = serverConfigurationService.getString("sitestats.db", "internal");
            if ("external".equals(whichDb)) {
                LocalSessionFactoryBuilder sfb = new LocalSessionFactoryBuilder(getExternalDataSource());
                getAdditionalHibernateMappings().processAdditionalMappings(sfb);
                sfb.addProperties(getHibernateExternalProperties());
                siteStatsSessionFactory = sfb.buildSessionFactory();
            } else {
                siteStatsSessionFactory = globalSessionFactory;
            }
        }
        return siteStatsSessionFactory;
    }

    @Bean(name = "org.sakaiproject.springframework.orm.hibernate.AdditionalHibernateMappings.sitestats")
    public AdditionalHibernateMappings getAdditionalHibernateMappings() {

        AdditionalHibernateMappings hibernateMappings = new AdditionalHibernateMappingsImpl();
        hibernateMappings.setMappingResources(getHibernateMappings());
        return hibernateMappings;
    }

    @Bean(name = "org.sakaiproject.sitestats.HibernateMappings")
    public String[] getHibernateMappings() {
        return new String[] {
                "org/sakaiproject/sitestats/impl/hbm/PrefsImpl.hbm.xml",
                "org/sakaiproject/sitestats/impl/hbm/EventStatImpl.hbm.xml",
                "org/sakaiproject/sitestats/impl/hbm/LessonBuilderStatImpl.hbm.xml",
                "org/sakaiproject/sitestats/impl/hbm/ResourceStatImpl.hbm.xml",
                "org/sakaiproject/sitestats/impl/hbm/SiteVisitsImpl.hbm.xml",
                "org/sakaiproject/sitestats/impl/hbm/SiteActivityImpl.hbm.xml",
                "org/sakaiproject/sitestats/impl/hbm/SitePresenceImpl.hbm.xml",
                "org/sakaiproject/sitestats/impl/hbm/SitePresenceTotalImpl.hbm.xml",
                "org/sakaiproject/sitestats/impl/hbm/JobRunImpl.hbm.xml",
                "org/sakaiproject/sitestats/impl/hbm/ReportDef.hbm.xml",
                "org/sakaiproject/sitestats/impl/hbm/ServerStat.hbm.xml",
                "org/sakaiproject/sitestats/impl/hbm/UserStat.hbm.xml"
        };
    }

    private Properties getHibernateExternalProperties() {
        Properties p = new Properties();
        p.setProperty("hibernate.dialect", serverConfigurationService.getString("sitestats.externalDb.hibernate.dialect", "org.hibernate.dialect.HSQLDialect"));
        String autoDdl = serverConfigurationService.getString("sitestats.externalDb.auto.ddl", "update");
        if ("true".equals(autoDdl)) {
        	autoDdl = "update";
        	Log.info("Auto DDL has been set to update based on old value of true, please update the property sitestats.externalDb.auto.ddl");
        }
        else if ("false".equals(autoDdl)) {
        	autoDdl = "validate";
        	Log.info("Auto DDL has been set to validate based on old value of false, please update the property sitestats.externalDb.auto.ddl");
        }

        p.setProperty("hibernate.hbm2ddl.auto", autoDdl);
        p.setProperty("hibernate.show_sql", serverConfigurationService.getString("sitestats.externalDb.hibernate.show_sql", "false"));
        p.setProperty("hibernate.query.substitutions", "true 1, false 0, yes 'Y', no 'N'");
        p.setProperty("hibernate.jdbc.use_streams_for_binary", "true");
        p.setProperty("hibernate.cache.use_query_cache", "true");
        p.setProperty("hibernate.cache.region.factory_class", "org.hibernate.cache.SingletonEhCacheRegionFactory");
        return p;
    }

    private DataSource getExternalDataSource() {

        HikariDataSource hds = new HikariDataSource();
        hds.setUsername(serverConfigurationService.getString("sitestats.externalDb.username", serverConfigurationService.getString("username@org.sakaiproject.sitestats.externalDbDataSource", "sa")));
        hds.setPassword(serverConfigurationService.getString("sitestats.externalDb.password", serverConfigurationService.getString("password@org.sakaiproject.sitestats.externalDbDataSource", "")));
        hds.setJdbcUrl(serverConfigurationService.getString("sitestats.externalDb.jdbcUrl", serverConfigurationService.getString("url@org.sakaiproject.sitestats.externalDbDataSource","jdbc:hsqldb:mem:sitestats_db")));
        hds.setDriverClassName(serverConfigurationService.getString("sitestats.externalDb.driverClassName", serverConfigurationService.getString("driverClassName@org.sakaiproject.sitestats.externalDbDataSource","org.hsqldb.jdbcDriver")));
        hds.setConnectionTestQuery(serverConfigurationService.getString("sitestats.externalDb.connectionTestQuery", "SELECT 1"));
        hds.setPoolName(serverConfigurationService.getString("sitestats.externalDb.poolName", "externalDBCP"));
        return hds;
    }
}
