package com.murong.ecp.tools.fx.application;

import com.murong.ecp.tools.fx.enums.SuccessFailureEnum;
import com.murong.ecp.tools.fx.enums.UserStatusEnum;
import com.murong.ecp.tools.fx.infrastructure.msgcode.CrResult;
import com.murong.ecp.tools.fx.infrastructure.repository.dao.UserInfoDao;
import com.murong.ecp.tools.fx.infrastructure.repository.po.UserInfoPO;
import com.murong.ecp.tools.fx.infrastructure.rpc.LoginRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final UserInfoDao userInfoDao;

    public AuthController(UserInfoDao userInfoDao) {
        this.userInfoDao = userInfoDao;
    }

    @GetMapping("/api/health")
    public CrResult<String> health() {
        CrResult<String> result = CrResult.setSuccessFailure(SuccessFailureEnum.SUCCESS);
        result.setData("ok");
        result.setMsgInf("memory-service is running");
        return result;
    }

    @PostMapping("/api/auth/login")
    public CrResult<UserInfoPO> login(@RequestBody LoginRequest request) {
        if (request == null || StringUtils.isBlank(request.getUsername()) || StringUtils.isBlank(request.getPassword())) {
            CrResult<UserInfoPO> result = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
            result.setMsgInf("登录用户名称或密码不允许为空");
            return result;
        }
        UserInfoPO userInfoPO = userInfoDao.queryByUsername(request.getUsername());
        if (userInfoPO == null) {
            CrResult<UserInfoPO> result = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
            result.setMsgInf("用户不存在");
            return result;
        }
        if (!StringUtils.equals(request.getPassword(), userInfoPO.getPassword())) {
            CrResult<UserInfoPO> result = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
            result.setMsgInf("密码验证失败");
            return result;
        }
        if (!StringUtils.equals(userInfoPO.getStatus(), UserStatusEnum.ONLINE.getCode())) {
            CrResult<UserInfoPO> result = CrResult.setSuccessFailure(SuccessFailureEnum.FAILURE);
            result.setMsgInf("用户状态错误请联系管理员");
            return result;
        }
        CrResult<UserInfoPO> result = CrResult.setSuccessFailure(SuccessFailureEnum.SUCCESS);
        result.setData(userInfoPO);
        return result;
    }
}
