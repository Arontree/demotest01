package com.nowcoder.community.service;

import com.nowcoder.community.dao.AlphaDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Service
//@Scope("prototype")//改成多实例
public class AlphaService {
    @Autowired
    private AlphaDao alphaDao;
    public AlphaService(){
        System.out.println("构造一个");
    }
    @PostConstruct
    public void init(){
        System.out.println("chushihua");
    }
    @PreDestroy
    public void destroy(){
        System.out.println("销毁alpha");
    }

    public String find(){
       return alphaDao.select();
    }
}
