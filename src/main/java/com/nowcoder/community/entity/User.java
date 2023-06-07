package com.nowcoder.community.entity;


import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.xml.crypto.Data;
import java.util.Date;

@lombok.Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private int id;
    private  String username;
    private String password;
    private  String salt;
    private  String email;
    private int type;
    private int status;
    private String activationCode;
    private String headerUrl;
    private Date createTime;

}
