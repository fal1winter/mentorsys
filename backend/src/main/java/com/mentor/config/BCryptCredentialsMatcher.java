package com.mentor.config;

import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.mindrot.jbcrypt.BCrypt;

/**
 * BCrypt Credentials Matcher for Shiro
 * 使用BCrypt算法进行密码匹配
 */
public class BCryptCredentialsMatcher implements CredentialsMatcher {

    @Override
    public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
        UsernamePasswordToken userToken = (UsernamePasswordToken) token;

        // Get the plain text password from the token
        String plainPassword = new String(userToken.getPassword());

        // Get the hashed password from the database
        String hashedPassword = (String) info.getCredentials();

        // Use BCrypt to compare
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}
