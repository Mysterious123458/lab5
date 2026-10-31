package com.vladt.lab5.service;

import com.vladt.lab5.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PasswordPolicy {

    private static final Pattern UPPER = Pattern.compile("[A-Z]");
    private static final Pattern LOWER = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");

    public void ensureValid(String password) {
        if (password == null || password.length() < 8
                || !UPPER.matcher(password).find()
                || !LOWER.matcher(password).find()
                || !DIGIT.matcher(password).find()) {
            throw new BadRequestException(
                    "Пароль должен содержать не менее 8 символов, включая строчные и прописные латинские буквы и цифры");
        }
    }
}
