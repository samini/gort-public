/*
   Copyright 2014 Shahriyar Amini

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.cmuchimps.gort.modules.dataobject;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.exception.ConstraintViolationException;

/**
 *
 * @author shahriyar
 */
public class GortEntityManager {
    public static final Integer INVALID_ID = -1;
    
    private static final String PERSISTENCE_UNIT_NAME = "HibernateJPA-";
    
    private static Set<EntityManager> managers = null;
    private static Set<EntityManagerFactory> factories = null;
    
    private static int unitIndex = 0;
    
    static {
        managers = new HashSet<EntityManager>();
        factories = new HashSet<EntityManagerFactory>();
    }
    
    private final String dbName;
    private final String unitName;
    private final Properties properties;
    private final String url;
    
    private EntityManagerFactory factory;
    
    // use a default entity manager for one shot queries
    private EntityManager defaultEntityManager;
    
    // Information on how to configure hibernate programmatically
    public GortEntityManager(String dbName) {
        this.dbName = dbName;
        
        StringBuilder sb = new StringBuilder();
        sb.append(PERSISTENCE_UNIT_NAME);
        sb.append(unitIndex);
        
        unitName = sb.toString();
        
        url = String.format("jdbc:postgresql://localhost/%s", this.dbName);
        
        properties = new Properties();
        properties.put(PersistenceUnitProperties.TRANSACTION_TYPE, "RESOURCE_LOCAL");
        properties.put("hibernate.connection.driver_class", "org.postgresql.Driver");
        properties.put("hibernate.connection.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.put("hibernate.connection.url", url);
        properties.put("hibernate.connection.username", "gort");
        //properties.put("hibernate.connection.password", "");
        properties.put("hibernate.hbm2ddl.auto", "update");
        
        // set up connection pool properties
        // https://docs.jboss.org/hibernate/core/4.1/devguide/en-US/html/ch01.html#d5e150
        // description available at: http://www.sos-berlin.com/mediawiki/index.php/Hibernate:_How_to_enable_connection_pooling_with_hibernate_using_c3po
        // and https://access.redhat.com/site/documentation/en-US/JBoss_Enterprise_Web_Platform/5/html/Hibernate_Core_Reference_Guide/configuration-hibernatejdbc.html
        // use c3p0 instead of default connection pool
        // some installation of postgres have ~20 max connections. Do not use a 
        // large minimum
        properties.put("hibernate.c3p0.min_size", "2");
        // also do not use a high maximum as at load time maximum could be opened
        properties.put("hibernate.c3p0.max_size", "8");
        // number of seconds the connections stay open while idle
        properties.put("hibernate.c3p0.timeout", "600");
        properties.put("hibernate.c3p0.max_statements", "50");
        
        /*
        properties.put("hibernate.transaction.factory_class", 
                "org.hibernate.transaction.JTATransactionFactory");*/
        
        // show the SQL statements to console
        //properties.put("hibernate.show_sql", "true");
        properties.put("hibernate.show_sql", "false");
        
        // increase the unitIndex for the next hibernate connection
        unitIndex++;
    }
    
    public EntityManager getEntityManager() {
        EntityManager manager = getEntityManagerFactory().createEntityManager();
        managers.add(manager);
        return manager;
    }
    
    // make sure to add all entities as annotated classes
    public synchronized EntityManagerFactory getEntityManagerFactory() {
        if (factory == null) {
            //factory = Persistence.createEntityManagerFactory(unitName, properties);
            
            // removed the reliance on hibernate.cfg.xml completely as we sometimes
            // hibernate could not fine the file
            // .configure("org/cmuchimps/gort/modules/dataobject/hibernate.cfg.xml")
            Ejb3Configuration cfg = new Ejb3Configuration();
            factory = cfg.addProperties(properties).addAnnotatedClass(Activity.class)
                    .addAnnotatedClass(Annotation.class)
                    .addAnnotatedClass(App.class)
                    .addAnnotatedClass(Assignment.class)
                    .addAnnotatedClass(Component.class)
                    .addAnnotatedClass(CrowdTask.class)
                    .addAnnotatedClass(Heuristic.class)
                    .addAnnotatedClass(History.class)
                    .addAnnotatedClass(HIT.class)
                    .addAnnotatedClass(Interaction.class)
                    .addAnnotatedClass(Library.class)
                    .addAnnotatedClass(Permission.class)
                    .addAnnotatedClass(Provider.class)
                    .addAnnotatedClass(Receiver.class)
                    .addAnnotatedClass(Screenshot.class)
                    .addAnnotatedClass(Sequence.class)
                    .addAnnotatedClass(Service.class)
                    .addAnnotatedClass(Server.class)
                    .addAnnotatedClass(State.class)
                    .addAnnotatedClass(TaintLog.class)
                    .addAnnotatedClass(Traversal.class)
                    .buildEntityManagerFactory();
            factories.add(factory);
        }

        return factory;
    }
    
    public synchronized EntityManager getDefaultEntityManager() {
        if (defaultEntityManager == null || !defaultEntityManager.isOpen()) {
            defaultEntityManager = getEntityManager();
        }
        
        return defaultEntityManager;
    }
    
    public static void closeEntityManager(EntityManager em) {
        if (em != null) {
            try {
                em.close();
            } catch (Exception e) {
                System.out.println("Exception closing EntityManager");
                //e.printStackTrace();
            }
        }
        
        managers.remove(em);
        em = null;
    }
    
    /*
    private static void closeEntityManagerFactory(EntityManagerFactory emf) {
        if (emf != null && emf.isOpen()) {
            try {
                emf.close();
            } catch (Exception e) {
                System.out.println("Exception closing EntityManagerFactory");
                e.printStackTrace();
            }
        }
        
        factories.remove(emf);
        emf = null;
    }*/
    
    public synchronized static void closeAll() {
        System.out.println("GortEntityManager closing all managers and factories...");
        if (managers != null) {
            for (Iterator<EntityManager> i = managers.iterator(); i.hasNext(); ) {
                EntityManager em = i.next();
                
                if (em != null) {
                    try{
                        em.close();
                    } catch (Exception e) {
                        // ignore
                    }
                }
                
                i.remove();
            }
        }
        
        if (factories != null) {
            for (Iterator<EntityManagerFactory> i = factories.iterator(); i.hasNext(); ) {
                EntityManagerFactory emf = i.next();
                
                if (emf != null) {
                    try {
                        emf.close();
                    } catch (Exception e) {
                        // ignore
                    }
                }
                
                i.remove();
            }
        }
    }
    
    public void insertEntity(Object o) {
        EntityManager em = getEntityManager();
        insertEntity(em, o);
        closeEntityManager(em);
    }
    
    public void insertEntity(EntityManager em, Object o) {
        if (o == null) {
            return;
        }
        
        if (em == null) {
            em = getDefaultEntityManager();
        }
        
        try {
            em.getTransaction().begin();
            em.persist(o);
            em.getTransaction().commit();
        } catch (ConstraintViolationException cve) {
            System.out.println("Constration violation while adding " + o);
            cve.printStackTrace();
        }
    }
    
    public void insertApp(App app) {
        insertEntity(app);
    }
    
    public void removeEntity(EntityManager em, Object o) {
        if (o == null) {
            return;
        }
        
        if (em == null) {
            // careful. if not retreived from the default entity manager
            // this would cause problems
            em = getDefaultEntityManager();
        }
        
        em.getTransaction().begin();
        em.remove(o);
        em.getTransaction().commit();
    }
    
    public void updateEntity(Object o) {
        EntityManager em = getEntityManager();
        updateEntity(em, o);
        closeEntityManager(em);
    }
    
    public void updateEntity(EntityManager em, Object o) {
        if (o == null) {
            return;
        }
        
        if (em == null) {
            em = getDefaultEntityManager();
        }
        
        em.getTransaction().begin();
        em.merge(o);
        em.getTransaction().commit();
    }
    
    public void updateApp(App app) {
        updateEntity(app);
    }
    
    public <T> T select(EntityManager em, Class<T> type, Integer id) {
        if (type == null || id == null) {
            return null;
        }
        
        if (em == null) {
            em = getDefaultEntityManager();
        }
        
        return em.find(type, id);
    }
    
    public App selectApp(Integer id) {
        EntityManager em = getEntityManager();
        App retVal = selectApp(em, id);
        closeEntityManager(em);
        return retVal;
    }
    
    public App selectApp(EntityManager em, Integer id) {
        if (id == null) {
            return null;
        }
        
        if (em == null) {
            em = getDefaultEntityManager();
        }
        
        App retVal = em.find(App.class, id);
        
        return retVal;
    }
    
    public App selectApp(String apk) {
        EntityManager em = getEntityManager();
        App retVal = selectApp(em, apk);
        closeEntityManager(em);
        return retVal;
    }
    
    // does not close the entity manager
    public App selectApp(EntityManager em, String apk) {
        if (em == null) {
            return null;
        }
        
        if (apk == null || apk.isEmpty()) {
            return null;
        }
        
        System.out.println("Performing select query for apk: " + apk);
        
        Query query = em.createQuery("SELECT a FROM App a WHERE a.apk = :apk");
        query.setParameter("apk", apk);
        
        Object o = null;
        
        try {
            o = query.getSingleResult();
        } catch (NoResultException nre) {
            //ignore, objet remains as null
            //nre.printStackTrace();
        } catch (Exception e) {
            // ignore
        } finally {
            //closeEntityManager(em);
        }
        
        return (o != null) ? (App) o : null;
    }
    
    public List<CrowdTask> selectCrowdTask(EntityManager em, Integer traversalId,
            Integer startStateId, Integer endStateId) {
        if (traversalId == null) {
            return null;
        }
        
        if (startStateId == null || endStateId == null) {
            return null;
        }
        
        if (em == null) {
            em = getDefaultEntityManager();
        }
        
        Query query = em.createQuery("SELECT c FROM CrowdTask c WHERE " + 
                "c.traversal.id = :traversalId AND c.startState.id = :startStateId " + 
                "AND c.endState.id = :endStateId");
        query.setParameter("traversalId", traversalId);
        query.setParameter("startStateId", startStateId);
        query.setParameter("endStateId", endStateId);
        
        List<Object> results = query.getResultList();
        
        return (results != null && results.size() > 0) ? (List<CrowdTask>)(List<?>) results : null;
    }
    
    public List<CrowdTask> selectCrowdTask(EntityManager em, Integer traversalId,
            Integer startStateId, Integer endStateId, Integer startScreenshotId,
            Integer endScreenshotId) {
        if (traversalId == null) {
            return null;
        }
        
        if (startStateId == null || endStateId == null) {
            return null;
        }
        
        if (em == null) {
            em = getDefaultEntityManager();
        }
        
        Query query = em.createQuery("SELECT c FROM CrowdTask c WHERE " + 
                "c.traversal.id = :traversalId AND c.startState.id = :startStateId " + 
                "AND c.endState.id = :endStateId AND c.startScreenshot.id = :startScreenshotId " +
                "AND c.endScreenshot.id = :endScreenshotId");
        query.setParameter("traversalId", traversalId);
        query.setParameter("startStateId", startStateId);
        query.setParameter("endStateId", endStateId);
        query.setParameter("startScreenshotId", startScreenshotId);
        query.setParameter("endScreenshotId", endScreenshotId);
        
        Object o = null;
        
        List<Object> results = query.getResultList();
        return (results != null && results.size() > 0) ? (List<CrowdTask>)(List<?>) results : null;
    }
    
    public Heuristic selectHeuristic(Integer id) {
        EntityManager em = getEntityManager();
        Heuristic retVal = selectHeuristic(em, id);
        closeEntityManager(em);
        return retVal;
    }
    
    public Heuristic selectHeuristic(EntityManager em, Integer id) {
        return select(em, Heuristic.class, id);
    }
    
    public Heuristic selectHeuristic(EntityManager em, Integer appId, String clazz) {
        if (appId == null || appId.intValue() < 0) {
            return null;
        }
        
        if (clazz == null || clazz.isEmpty()) {
            return null;
        }
        
        if (em == null) {
            em = getDefaultEntityManager();
        }
        
        Query query = em.createQuery("SELECT h FROM Heuristic h WHERE h.clazz = :clazz AND h.app.id = :id");
        query.setParameter("clazz", clazz);
        query.setParameter("id", appId);
        
        Object o = null;
        
        try {
            o = query.getSingleResult();
        } catch (NoResultException nre) {
            // do nothing
        }
        
        return (o != null) ? (Heuristic) o : null;
    }
    
    public Heuristic selectHeuristic(EntityManager em, Integer appId, Integer traversalId, String clazz) {
        if (appId == null || appId.intValue() < 0) {
            return null;
        }
        
        if (clazz == null || clazz.isEmpty()) {
            return null;
        }
        
        if (traversalId == null || traversalId.intValue() < 0) {
            return null;
        }
        
        if (em == null) {
            em = getDefaultEntityManager();
        }
        
        Query query = em.createQuery("SELECT h FROM Heuristic h WHERE h.clazz = :clazz AND h.app.id = :appId AND h.traversal.id = :traversalId");
        query.setParameter("clazz", clazz);
        query.setParameter("appId", appId);
        query.setParameter("traversalId", traversalId);
        
        Object o = null;
        
        try {
            o = query.getSingleResult();
        } catch (NoResultException nre) {
            // do nothing
        }
        
        return (o != null) ? (Heuristic) o : null;
    }
    
    public Assignment selectAssignment(EntityManager em, String mturkHITId, String mturkAssignmentId) {
        if (mturkHITId == null || mturkAssignmentId == null) {
            return null;
        }
        
        if (em == null) {
            this.getDefaultEntityManager();
        }
        
        Query query = em.createQuery("SELECT a FROM Assignment a WHERE a.hit.hitId = :hitId AND a.assignmentId = :assignmentId");
        query.setParameter("hitId", mturkHITId);
        query.setParameter("assignmentId", mturkAssignmentId);
        
        Object o = null;
        
        try {
            o = query.getSingleResult();
        } catch (NoResultException nre) {
            // do nothing
        }
        
        return (o != null) ? (Assignment) o : null;
    }
    
    public List<Permission> selectPermission(String apk) {
        EntityManager em = getEntityManager();
        List<Permission> retVal = selectPermission(em, apk);
        closeEntityManager(em);
        return retVal;
    }
    
    public List<Permission> selectPermission(EntityManager em, String apk) {
        if (apk == null || apk.isEmpty()) {
            return null;
        }
        
        if (em == null) {
            em = getDefaultEntityManager();
        }
        
        App app = selectApp(em, apk);
        
        if (app == null) {
            return null;
        }
        
        List<Permission> retVal = app.getPermissions();
        
        // initiailize the values
        if (retVal == null) {
            return null;
        }
        
        Iterator iterator = retVal.iterator();
        
        while (iterator.hasNext()) {
            iterator.next();
        }
        
        return retVal;
    }
    
    public State selectState(EntityManager em, Integer id) {
        return select(em, State.class, id);
    }
    
    public Server selectServer(EntityManager em, Integer traversalId, String ip) {
        if (traversalId == null || traversalId.intValue() < 0) {
            return null;
        }
        
        if (ip == null || ip.isEmpty()) {
            return null;
        }
        
        if (em == null) {
            em = getDefaultEntityManager();
        }
        
        Query query = em.createQuery("SELECT s FROM Server s WHERE s.ip = :ip AND s.traversal.id = :id");
        query.setParameter("ip", ip);
        query.setParameter("id", traversalId);
        
        Object o = null;
        
        try {
            o = query.getSingleResult();
        } catch (NoResultException nre) {
            // do nothing
        }
        
        return (o != null) ? (Server) o : null;
    }
    
    public Traversal selectTraversal(String directoryName) {
        EntityManager em = getEntityManager();
        Traversal t = selectTraversal(em, directoryName);
        closeEntityManager(em);
        return t;
    }
    
    // does not close the entity manager
    public Traversal selectTraversal(EntityManager em, String directoryName) {
        if (em == null) {
            return null;
        }
        
        if (directoryName == null || directoryName.isEmpty()) {
            return null;
        }
        
        System.out.println("Performing select query for traversal: " + directoryName);
        
        Query query = em.createQuery("SELECT t FROM Traversal t WHERE t.directory = :directory");
        query.setParameter("directory", directoryName);
        
        Object o = null;
        
        try {
            o = query.getSingleResult();
        } catch (NoResultException nre) {
            //ignore, objet remains as null
            //nre.printStackTrace();
        } finally {
            //closeEntityManager(em);
        }
        
        return (o != null) ? (Traversal) o : null;
    }
    
}
