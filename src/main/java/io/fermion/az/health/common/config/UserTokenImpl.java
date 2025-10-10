package io.fermion.az.health.common.config;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class UserTokenImpl implements UserToken {
  private String userId;
  private String token;
  private boolean valid = true;

  @Override
  public String getUserId() {
    return userId != null ? userId : "test-user-" + System.currentTimeMillis();
  }

  @Override
  public String getToken() {
    return token != null ? token : "test-token";
  }

  @Override
  public boolean isValid() {
    return valid;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public void setValid(boolean valid) {
    this.valid = valid;
  }
}