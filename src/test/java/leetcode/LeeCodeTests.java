package leetcode;

import org.junit.Test;

import java.util.*;

public class LeeCodeTests {
    @Test
    public void leed()
    {
        String s="welcometocgb";
        String[] wordDict={"welcome", "to","cgb","www"};
        boolean result=true;
        // write code here
        if(s==null || wordDict.length==0){
            result=true;
        }
        int node=0;
        for(int i=0;i<wordDict.length;i++){
            int length=wordDict[i].length();
            int m=0;
            for(int j=node;j<length+node;j++){
                if(j>s.length()){
                    result=false;
                }
                if(s.charAt(j)==wordDict[i].charAt(m)){
                    System.out.println(s.charAt(j));
                    System.out.println(wordDict[i].charAt(m++));
                    continue;
                }else{
                    result=false;
                }

            }
            node+=length;
        }
        if(node!=s.length()){
            result=false;
        }
        result=true;
        System.out.println(result);
    }
}

