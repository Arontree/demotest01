package com.nowcoder.community;


import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class Servicetest {
    @Autowired
    private UserService userService;
    @Test
    public void testUserService(){
        User userById = userService.findUserById(101);
        System.out.println(userById);
    }
}
