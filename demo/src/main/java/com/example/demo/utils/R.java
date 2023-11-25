package com.example.demo.utils;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * @author: zPlus
 * @date: 2023/11/29 10:38
 */
public class R {
    public static <T> ResponseEntity<T> badRequest(T msg){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg);
    }

    public static <T> ResponseEntity<T> notFound(T msg){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(msg);
    }

    public static <T> ResponseEntity<T> serverError(T msg){
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg);
    }

    public static <T> ResponseEntity<T> unAuthorized(T msg){
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(msg);
    }

    public static <T> ResponseEntity<T> forbidden(T msg){
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(msg);
    }

    public static <T> ResponseEntity<T> noContent(T msg){
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(msg);
    }

    public static <T> ResponseEntity<T> ok(T msg){
        return ResponseEntity.ok(msg);
    }

    public static <T> ResponseEntity<T> created(T msg){
        return ResponseEntity.status(HttpStatus.CREATED).body(msg);
    }
}
