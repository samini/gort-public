/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hibernate;

import javassist.util.proxy.ProxyFactory;
import org.openide.modules.ModuleInstall;

public class Installer extends ModuleInstall {

    @Override
    public void restored() {
        // Recommended by Hibernate chapter of The Definitive Guide to NetBeans Platform 7
        ProxyFactory.classLoaderProvider = new ProxyFactory.ClassLoaderProvider() {

            public ClassLoader get(ProxyFactory pf) {
                return Thread.currentThread().getContextClassLoader();
            }
        };
    }
}
