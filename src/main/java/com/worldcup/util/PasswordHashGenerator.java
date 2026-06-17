package com.worldcup.util;

import com.worldcup.security.PasswordService;

public class PasswordHashGenerator {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java com.worldcup.util.PasswordHashGenerator <password>");
            System.exit(2);
        }
        String pwd = args[0];
        String hash = PasswordService.hashPassword(pwd);
        System.out.println(hash);
    }
}
