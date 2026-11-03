package com.dreampet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dreampet.common.BizException;
import com.dreampet.entity.User;
import com.dreampet.mapper.UserMapper;
import com.dreampet.util.TokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final TokenUtil tokenUtil;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public record AuthResult(String token, String username, Long userId) {
    }

    public AuthResult register(String username, String password) {
        validateUsername(username);
        if (password == null || password.length() < 6 || password.length() > 64) {
            throw new BizException("密码需要 6~64 位");
        }
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (exists > 0) {
            throw new BizException("这个名字已经被使用啦，换一个试试？");
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(encoder.encode(password));
        userMapper.insert(user);
        return new AuthResult(tokenUtil.create(user.getId(), username), username, user.getId());
    }

    public AuthResult login(String username, String password) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null || !encoder.matches(password == null ? "" : password, user.getPasswordHash())) {
            throw BizException.unauthorized("名字或密码不对哦，再试试？");
        }
        return new AuthResult(tokenUtil.create(user.getId(), user.getUsername()), user.getUsername(), user.getId());
    }

    /** 一键体验：创建随机游客账号（演示用，无密码） */
    public AuthResult guest() {
        String username = "小探险家" + (int) (Math.random() * 90000 + 10000);
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(encoder.encode(java.util.UUID.randomUUID().toString()));
        userMapper.insert(user);
        return new AuthResult(tokenUtil.create(user.getId(), username), username, user.getId());
    }

    public User me(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw BizException.unauthorized("用户不存在");
        return user;
    }

    private void validateUsername(String username) {
        if (username == null || !username.matches("^[a-zA-Z0-9_\\u4e00-\\u9fa5]{2,20}$")) {
            throw new BizException("名字需要 2~20 位，仅限中英文、数字和下划线");
        }
    }
}
