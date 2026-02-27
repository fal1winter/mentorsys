package com.mentor.config;

import com.mentor.entity.Permission;
import com.mentor.entity.Role;
import com.mentor.entity.User;
import com.mentor.service.AuthService;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ByteSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Custom Realm for Shiro
 * 自定义Realm实现认证和授权
 */
public class CustomRealm extends AuthorizingRealm {

    @Autowired
    private AuthService authService;

    /**
     * Authorization - Get user roles and permissions
     * 授权 - 获取用户角色和权限
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        // Get username from principal
        String username = (String) principals.getPrimaryPrincipal();

        SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();

        try {
            // Get user by username
            User user = authService.getUserByUsername(username);
            if (user == null) {
                return authorizationInfo;
            }

            // Get user roles
            List<Role> roles = authService.getUserRoles(user.getId());
            Set<String> roleNames = roles.stream()
                    .map(Role::getRoleName)
                    .collect(Collectors.toSet());
            authorizationInfo.setRoles(roleNames);

            // Get user permissions
            List<Permission> permissions = authService.getUserPermissions(user.getId());
            Set<String> permissionNames = permissions.stream()
                    .map(Permission::getPermissionName)
                    .collect(Collectors.toSet());
            authorizationInfo.setStringPermissions(permissionNames);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return authorizationInfo;
    }

    /**
     * Authentication - Verify user credentials
     * 认证 - 验证用户凭证
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        UsernamePasswordToken usernamePasswordToken = (UsernamePasswordToken) token;
        String username = usernamePasswordToken.getUsername();

        try {
            // Get user by username
            User user = authService.getUserByUsername(username);
            if (user == null) {
                throw new UnknownAccountException("User not found: " + username);
            }

            // Check if user is enabled
            if (user.getStatus() == 0) {
                throw new LockedAccountException("User account is disabled: " + username);
            }

            // Return authentication info
            // Shiro will automatically compare the password with the credential
            SimpleAuthenticationInfo authenticationInfo = new SimpleAuthenticationInfo(
                    username,  // principal
                    user.getPassword(),  // credentials (hashed password)
                    ByteSource.Util.bytes(user.getSalt()),  // salt
                    getName()  // realm name
            );

            return authenticationInfo;

        } catch (Exception e) {
            throw new AuthenticationException("Authentication failed", e);
        }
    }
}
