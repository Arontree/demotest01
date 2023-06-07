package com.nowcoder.community.dao;

import org.springframework.stereotype.Repository;

@Repository("AlphaDaoHibe")
public class AlphaDaoHibe implements AlphaDao {

    @Override
    public String select() {
        return "Hello";
    }
}
