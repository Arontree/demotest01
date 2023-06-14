package com.nowcoder.community.service;

import com.nowcoder.community.dao.LoginTicketMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.utils.CommunityConstant;
import com.nowcoder.community.utils.CommunityUtils;
import com.nowcoder.community.utils.MailClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class UserService implements CommunityConstant {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private LoginTicketMapper loginTicketMapper;

    @Value("${community.path.domain}")
    private  String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    public User findUserById(int id){
        return userMapper.selectById(id);
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
        loginTicketMapper.insertLoginTicket(loginTicket);
        map.put("ticket", loginTicket.getTicket());
        return map;
    }

    public void logout(String ticket){
        loginTicketMapper.updateStatus(ticket,1);
    }

    public LoginTicket findLoginTicket (String ticket){
        return loginTicketMapper.selectByTicket(ticket);
    }

    public int upadteHeader(int useId,String headerUrl){//update会返回更新对象得id
        return userMapper.updateHeader(useId,headerUrl);
    }

    public User findUserByName(String username){
        return userMapper.selectByName(username);
    }
}
