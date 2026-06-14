package com.worldcup.security;

import jakarta.enterprise.context.ApplicationScoped;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.directory.Attributes;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Active Directory authentication provider for production.
 * This class authenticates users via LDAP.
 */
@ApplicationScoped
public class ActiveDirectoryAuthenticationProvider implements AuthenticationProvider {

    private static final Logger LOGGER = Logger.getLogger(ActiveDirectoryAuthenticationProvider.class.getName());

    // In a real app, these would come from properties/config
    private static final String LDAP_URL = "ldap://ad.company.com:389";
    private static final String LDAP_DOMAIN = "company.com";
    private static final String SEARCH_BASE = "dc=company,dc=com";

    @Override
    public AdUserDetails authenticate(String username, String password) {
        String principalName = username + "@" + LDAP_DOMAIN;

        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, LDAP_URL);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, principalName);
        env.put(Context.SECURITY_CREDENTIALS, password);

        try {
            // Attempt to connect and authenticate
            DirContext ctx = new InitialDirContext(env);

            // If authentication succeeds, fetch user details
            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            searchControls.setReturningAttributes(new String[]{"displayName", "mail", "employeeID", "sAMAccountName"});

            NamingEnumeration<SearchResult> results = ctx.search(SEARCH_BASE, "(sAMAccountName=" + username + ")", searchControls);

            if (results.hasMore()) {
                SearchResult result = results.next();
                Attributes attrs = result.getAttributes();

                String displayName = attrs.get("displayName") != null ? attrs.get("displayName").get().toString() : username;
                String mail = attrs.get("mail") != null ? attrs.get("mail").get().toString() : null;
                String employeeId = attrs.get("employeeID") != null ? attrs.get("employeeID").get().toString() : null;

                ctx.close();
                return new AdUserDetails(username, displayName, mail, employeeId);
            }

            ctx.close();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "LDAP Authentication failed for user " + username, e);
        }

        return null;
    }
}
