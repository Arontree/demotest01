package com.nowcoder.community.utils;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.path.PathTrie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Component
public class SensitiveFilter {

    private static  final Logger logger= LoggerFactory.getLogger(SensitiveFilter.class);
    //替换符
    private static String REPLACEMENT="***";

    //根节点
    private TriNode rootNode=new TriNode();

    //初始化前缀树
    @PostConstruct
    public void init(){
        //用于获取资源文件的输入流，其中 "sensitive-words.txt" 是资源文件的名称。
        // 它会在类路径下查找该资源文件，并返回该文件的输入流。如果找不到该文件，则返回 null。
        //this代表当前对象，即调用该方法的对象。
        // 在这个方法中，this.getClass()返回的是当前对象的类，而getClassLoader()返回的是该类的类加载器
        //buffer是把输入流变成缓存流
        try(InputStream is=this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
            BufferedReader reader=new BufferedReader(new InputStreamReader(is));)
        {
            String keyword;
            while ((keyword=reader.readLine())!=null)
            {
                this.addKeyword(keyword);
            }
        } catch (Exception e) {
            logger.error("加载敏感词文件失败: " + e.getMessage());
        }
    }
    //将一个敏感词添加到前缀树中
    private void addKeyword(String keyword){
        TriNode tempNote=rootNode;
        for(int i=0;i<keyword.length();i++){
            char c=keyword.charAt(i);
            TriNode subNode=tempNote.getSubNode(c);
            if(subNode==null){
                //初始化子节点
                subNode=new TriNode();
                tempNote.addSubNode(c,subNode);
            }
            //指向子节点，进入下一轮循环
            tempNote=subNode;

            //设置结束标识,最后一轮循环的时候设置这个最后一个节点为结束标识
            if(i==keyword.length()-1){
                tempNote.setKeywordEnd(true);
            }
        }
    }

    /***
     * @description:
     * @param:  text待过滤的文本
     * @return:  过滤后的文本
     * @author
     * @date:
     */
    public String filter(String text){
        if(StringUtils.isBlank(text)){
            return null;
        }
        //指针1
        TriNode tempNode=rootNode;
        //指针2
        int begin =0;
        //指针3
        int position=0;
        //结果接受
        StringBuilder sb=new StringBuilder();

        while (position<text.length()){
            char c=text.charAt(position);
            //跳过符号
            if(isSymbol(c)){
                //若指针1处于根节点，将此符号计入结果，让指针2向下走一步
                if(tempNode==rootNode){
                    sb.append(c);
                    begin++;
                }
                //无论符号在开头或中间，指针3都向下走一步
                position++;
                continue;
            }
            //检查下级节点
            tempNode=tempNode.getSubNode(c);
            if(tempNode==null){
                //以begin开头的字符串不是敏感词
                sb.append(text.charAt(begin));
                //进入下一个位置
                position=++begin;
                //重新指向根节点
                tempNode=rootNode;
            }else if (tempNode.isKeywordEnd){
                //发现敏感词，将begin-position字符串替换掉
                sb.append(REPLACEMENT);
                //进入下一个位置
                begin=++position;
                //重新指向根节点
                tempNode=rootNode;
            }else{
                //检查下一个字符
                position++;
            }
        }
        //将最后一批字符计入结果
        sb.append(text.substring(begin));
        return sb.toString();
    }

    //判断是否为符号
    private boolean isSymbol(Character c){
        return !CharUtils.isAsciiAlphanumeric(c) && (c < 0x2E80 || c >0x9FFF);
    }

    //前缀树
    public class TriNode{
        //关键词结束标识
        private boolean isKeywordEnd=false;

        //子节点（key是下级字符，value是下级节点）
        private Map<Character, TriNode> subNodes=new HashMap<>();

        public boolean isKeywordEnd(){
            return isKeywordEnd;
        }

        public void setKeywordEnd(boolean keywordEnd){
            isKeywordEnd=keywordEnd;
        }

        //添加子节点
        public void addSubNode (Character c,TriNode node){
            subNodes.put(c,node);
        }

        //获取子节点
        public  TriNode getSubNode(Character c){
            return subNodes.get(c);
        }
    }


}
