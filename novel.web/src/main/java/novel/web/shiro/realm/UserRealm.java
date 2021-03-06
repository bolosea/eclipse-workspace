package novel.web.shiro.realm;

import novel.web.entitys.User;
import novel.web.service.UserService;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

public class UserRealm extends AuthorizingRealm{

    @Autowired
    private UserService userService;
    /**
     * Authorization:授权
     * @param principalCollection
     * @return
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        User user  = (User)principalCollection.getPrimaryPrincipal();
        SimpleAuthorizationInfo simpleAuthorizationInfo = new SimpleAuthorizationInfo();
        Set<String> role = userService.getRoleByName(user.getName());
        simpleAuthorizationInfo.addRoles(role);
        System.out.println("role = " + role);
        Set<String> permission = userService.getPermissionByName(user.getName());
        simpleAuthorizationInfo.addStringPermissions(permission);
        System.out.println("permission = " + permission);
        return simpleAuthorizationInfo;
    }

    /**
     * Authentication:认证
     * @param token
     * @return
     * @throws AuthenticationException
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        String username = token.getPrincipal().toString();
        String password = new String ((char[])token.getCredentials());
        User user = userService.check(new User(username,password));
        if (user!=null){
            SecurityUtils.getSubject().getSession().setAttribute("user",user);
            return new SimpleAuthenticationInfo(user,user.getPassword(),getName());
        }
        return null;
    }
}
