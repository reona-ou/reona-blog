package com.reona.blog.exception;

import com.reona.blog.commcn.Constants;
import com.reona.blog.commcn.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    @ResponseBody
    public ResponseEntity<Result> handle(ServiceException se) {
        return ResponseEntity.status(toHttpStatus(se.getCode()))
                .body(Result.error(se.getCode(), se.getMessage()));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseBody
    public ResponseEntity<Result> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("唯一约束冲突: {}", e.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Result.error(Constants.CODE_600, "数据已存在，请勿重复提交"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseBody
    public ResponseEntity<Result> handleUnreadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(Result.error(Constants.CODE_400, "请求内容格式不正确"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseBody
    public ResponseEntity<Result> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest().body(Result.error(Constants.CODE_400, "参数格式不正确"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseBody
    public ResponseEntity<Result> handleMissingParameter(MissingServletRequestParameterException e) {
        return ResponseEntity.badRequest().body(Result.error(Constants.CODE_400, "缺少必要参数"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseBody
    public ResponseEntity<Result> handleNoResource(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.error(Constants.CODE_404, "接口不存在"));
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<Result> handleUnknown(Exception e) {
        log.error("未处理异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(Constants.CODE_500, "系统错误"));
    }

    private HttpStatus toHttpStatus(String code) {
        if (Constants.CODE_400.equals(code) || Constants.CODE_600.equals(code)) {
            return HttpStatus.BAD_REQUEST;
        }
        if (Constants.CODE_401.equals(code)) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (Constants.CODE_403.equals(code)) {
            return HttpStatus.FORBIDDEN;
        }
        if (Constants.CODE_404.equals(code)) {
            return HttpStatus.NOT_FOUND;
        }
        if (Constants.CODE_429.equals(code)) {
            return HttpStatus.TOO_MANY_REQUESTS;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
