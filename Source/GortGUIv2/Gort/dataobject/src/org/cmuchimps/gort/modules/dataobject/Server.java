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

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

/**
 *
 * @author shahriyar
 */
@Entity
public class Server {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    private String ip;
    private String hostname;
    private String name;
    private String address;
    private String city;
    private String stateprov;
    private String country;
    private String postalCode;
    private String phone;
    private String email;
    
    @ManyToOne
    @JoinColumn(name="traversal_fk")
    private Traversal traversal;
    
    @ManyToMany(mappedBy="servers")
    @OrderBy
    private List<State> states;
    
    @OneToMany(mappedBy="server")
    @OrderBy
    private List<TaintLog> taintLogs;

    public Server() {
    }
    
    public Server(String ip) {
        this();
        this.ip = ip;
    }

    public Server(String ip, String hostname) {
        this(ip);
        this.hostname = hostname;
    }
    
    public Server(String ip, String hostname, String name, String address, String city, String stateprov, String country, String postalCode) {
        this(ip, hostname);
        this.name = name;
        this.address = address;
        this.city = city;
        this.stateprov = stateprov;
        this.country = country;
        this.postalCode = postalCode;
    }
    
    public Server(String ip, String hostname, String name, String address, String city, String stateprov, String country, String postalCode, String phone, String email) {
        this(ip, hostname, name, address, city, stateprov, country, postalCode);
        this.phone = phone;
        this.email = email;
    }

    public Integer getId() {
        return id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStateprov() {
        return stateprov;
    }

    public void setStateprov(String stateprov) {
        this.stateprov = stateprov;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public Traversal getTraversal() {
        return traversal;
    }

    public void setTraversal(Traversal traversal) {
        this.traversal = traversal;
    }

    public List<TaintLog> getTaintLogs() {
        return taintLogs;
    }

    public void setTaintLogs(List<TaintLog> taintLogs) {
        this.taintLogs = taintLogs;
    }
    
}
