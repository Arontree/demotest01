package com.nowcoder.community.service;

import com.nowcoder.community.dao.LoginTicketMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.utils.CommunityConstant;
import com.nowcoder.community.utils.CommunityUtils;
import com.nowcoder.community.utils.MailClient;
import com.nowcoder.community.utils.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements CommunityConstant {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

//    @Autowired
//    private LoginTicketMapper loginTicketMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${community.path.domain}")
    private  String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    //登录优化，user信息获取
    public User findUserById(int id){

        /*return userMapper.selectById(id);（优化前）*/
        User user= getCache(id);
        if (user==null){
            user=initCache(id);
        }
            return user;
    }

    public Map<String,Object> register(User user){
        Map<String,Object> map=new HashMap<>();
        //空值处理,关键数值空值检测
        if(user==null){
            throw new IllegalArgumentException("参数不能为空");
        }
        if(StringUtils.isBlank(user.getUsername())){
            map.put("usernameMsg","账号不能为空");
        }
        if(StringUtils.isBlank(user.getPassword())){
            map.put("passwordMsg","密码不能为空");
        }
        if(StringUtils.isBlank(user.getPassword())){
            map.put("emailMsg","email不能为空");
        }

        //验证账号
        User u01 = userMapper.selectByName(user.getUsername());
        if(u01!=null){
            map.put("usernameMsg","该账号已存在");
            return map;
        }

        //验证邮箱
        u01=userMapper.selectByEmail(user.getEmail());
        if(u01!=null){
            map.put("emailMsg","该邮箱已被注册");
            return map;
    }
        //注册用户
        user.setSalt(CommunityUtils.generateUUId().substring(0,5));//随机字符串取六位
        user.setPassword(CommunityUtils.md5(user.getPassword()+user.getSalt()));
        user.setType(0);//默认为普通用户
        user.setActivationCode(CommunityUtils.generateUUId());
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png",new Random().nextInt(1000)));//random() 方法只能产生 double 类型的 0~1 的随机数;;Random 类提供的所有方法生成的随机数字都是均匀分布的
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        //发送激活邮件
        Context context=new Context();
        context.setVariable("email",user.getEmail());
        //http://localhost:8080/community/actiavtion/101/code
        String url=domain+contextPath+"/activation/"+user.getId()+"/"+user.getActivationCode();
        context.setVariable("url",url);
        String content=templateEngine.process("/mail/activation",context);
        mailClient.senMail(user.getEmail(),"激活账号",content);



        return map;
    }


    public int activation(int userId,String code){
        User user=userMapper.selectById(userId);
        if(user.getStatus()==1){
            return ACTIVATION_REPEAT;
        }else if(user.getActivationCode().equals(code)){
            //识别到激活账号激活码和数据库内存在的激活码一致，并且状态是未激活
            userMapper.updateStatus(userId, 1);
            clearCache(userId);
            return  ACTIVATION_SUCCESS;
        }else {
            return ACTIVATION_FAILURE;
        }


    }

    public Map<String,Object> login(String username,String password,int expiredSeconds){
        Map<String,Object> map=new HashMap<>();
        //空值处理
        if(StringUtils.isBlank(username)){
            map.put("usernameMsg", "账号不能为空!");
            return map;
        }
        if(StringUtils.isBlank(password)){
            map.put("passwordMsg", "密码不能为空!");
            return map;
        }

        //验证账号
        User user=userMapper.selectByName(username);
        if (user==null){
            map.put("usernameMsg", "该账号不存在!");
            return map;
        }

        //验证状态
        if(user.getStatus()==0){
            map.put("usernameMsg", "该账号未激活!");
            return map;
        }
        //验证密码
        password=CommunityUtils.md5(password+user.getSalt());
        if(!user.getPassword().equals(password)){
            map.put("passwordMsg", "密码不正确!");
            return map;
        }

        //生成登录凭证
        LoginTicket loginTicket=new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtils.generateUUId());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis()+expiredSeconds*1000));
       /* loginTicketMapper.insertLoginTicket(loginTicket);（登录优化前存在MySQL数据库内把登录凭证）*/
        //登录优化后存在redis内，但是也不清，因为还有用例如统计登录次数年终
        String redistKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(redistKey,loginTicket);


        map.put("ticket", loginTicket.getTicket());
        return map;
    }

    public void logout(String ticket){
        /*loginTicketMapper.updateStatus(ticket,1*/
        String redisKey=RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket=(LoginTicket) redisTemplate.opsForValue().get(redisKey);
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(redisKey,loginTicket);//把退出状态存回去
    }

    public LoginTicket findLoginTicket (String ticket){
        /*return loginTicketMapper.selectByTicket(ticket);*/
        String redisKey=RedisKeyUtil.getTicketKey(ticket);
        return (LoginTicket)redisTemplate.opsForValue().get(redisKey);
    }

    public int upadteHeader(int useId,String headerUrl){
        //update会返回更新对象得id
        int rows = userMapper.updateHeader(useId, headerUrl);
        clearCache(useId);
        return rows;
    }

    public User findUserByName(String username){
        return userMapper.selectByName(username);
    }

    //优化登录后的用户信息存储
    // 1。优先从缓存中取值
    private User getCache(int useId){
        String redisKey=RedisKeyUtil.getUserKey(useId);
        return (User)redisTemplate.opsForValue().get(redisKey);//这里要用转user,不然返回的是objec
    }
    // 2.取不到时初始化缓存数据
    private User initCache(int userId){
        User user = userMapper.selectById(userId);
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.opsForValue().set(redisKey,user,3600, TimeUnit.SECONDS);
        return user;
    }
    // 3.数据表更时清除缓存数据
    private void clearCache(int userId){
        String  redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(redisKey);
    }

    // 通过userId,获取用户权限
    public Collection<? extends GrantedAuthority> getAuthorities(int userId){
        User user = this.findUserById(userId);

        // GrantedAuthority 是 Spring Security 框架中的一个接口，用于表示用户的授权信息。
        // 它表示用户在系统中被授予的某个权限或角色。
        List<GrantedAuthority> list=new ArrayList<>();
        list.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
               switch (user.getType()){
                   case 1:
                       return AUTHORITY_ADMIN;
                   case 2:
                       return AUTHORITY_MODERATOR;
                   default:
                       return AUTHORITY_USER;
               }
            }
        });
        return list;
    }
}
