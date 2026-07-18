package com.urlshortener.url_shortener.util;

import org.springframework.stereotype.Component;

@Component
public class Base62Util {

    public static final String BASE62 = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    public static final int BASE=62;

    public String encode(long num)
    {
        StringBuilder sb=new StringBuilder();

        while(num>0)
        {
            sb.append(BASE62.charAt((int)num%62));
            num/=BASE;
        }

        //PAD to minimum 6 character
        while(sb.length()<6) {
            sb.append('a');
        }
        return sb.reverse().toString();
    }
    //Convert a Base62 shortCode back to a number
    public long decode(String shortCode)
    {
        long num=0;
        for(char c:shortCode.toCharArray())
        {
            num=num*BASE+BASE62.indexOf(c);
        }
        return num;
    }
}
