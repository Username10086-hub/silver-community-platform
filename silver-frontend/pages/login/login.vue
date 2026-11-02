<template>
  <view class="login-page">
    <!-- 顶部装饰 -->
    <view class="header-bg"></view>

    <view class="login-card">
      <view class="brand">
        <text class="logo">🌱</text>
        <text class="app-name">银发无忧社区</text>
        <text class="slogan">公益养老 · 温暖同行</text>
      </view>

      <view class="form">
        <view class="input-item">
          <text class="icon">👤</text>
          <input v-model="account" placeholder="请输入用户名或用户ID" placeholder-class="ph" />
        </view>
        <view class="input-item">
          <text class="icon">🔒</text>
          <input v-model="password" type="password" placeholder="请输入密码" placeholder-class="ph" />
        </view>

        <button class="btn-login" @click="handleLogin">登录</button>

        <view class="footer-row">
          <text class="tip">还没有账号？</text>
          <navigator url="/pages/register/register" class="link">立即注册</navigator>
        </view>
      </view>
    </view>

    <view class="bottom-tip">银发无忧 · 让爱不再遥远</view>
  </view>
</template>

<script>
import { request } from '@/utils/request.js'
import { setUser } from '@/utils/storage.js'
export default {
  data() {
    return { account: '', password: '' }
  },
  methods: {
    async handleLogin() {
      if (!this.account || !this.password) {
        return uni.showToast({ title: '请输入账号和密码', icon: 'none' })
      }
      try {
        const user = await request({
          url: '/user/login',
          method: 'POST',
          data: { account: this.account, password: this.password }
        })
        setUser(user)
        uni.showToast({ title: '登录成功', icon: 'success' })
        setTimeout(() => uni.reLaunch({ url: '/pages/index/index' }), 800)
      } catch (e) {
        uni.showToast({ title: '账号或密码错误', icon: 'none' })
      }
    }
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #4A90D9 0%, #6AADF5 40%, #f0f4f8 100%);
  padding: 120rpx 40rpx 40rpx;
  box-sizing: border-box;
  position: relative;
}
.header-bg { position: absolute; top: 0; left: 0; right: 0; height: 300rpx; }

.login-card {
  background: #fff;
  border-radius: 32rpx;
  padding: 60rpx 40rpx;
  box-shadow: 0 20rpx 60rpx rgba(74, 144, 217, 0.15);
  position: relative;
  z-index: 2;
}
.brand { text-align: center; margin-bottom: 60rpx; }
.brand .logo { font-size: 100rpx; display: block; margin-bottom: 20rpx; }
.brand .app-name {
  font-size: 44rpx; font-weight: 700; color: #1a2332;
  display: block; letter-spacing: 2rpx;
}
.brand .slogan { font-size: 24rpx; color: #8A9AAE; display: block; margin-top: 12rpx; }

.form { margin-top: 20rpx; }
.input-item {
  display: flex; align-items: center;
  background: #f7f9fc; border-radius: 50rpx;
  padding: 0 30rpx; margin-bottom: 30rpx;
  border: 2rpx solid #e6edf4;
  height: 100rpx;
}
.input-item .icon { font-size: 36rpx; margin-right: 20rpx; }
.input-item input {
  flex: 1; height: 100rpx; font-size: 30rpx; color: #1a2332;
  background: transparent; border: none;
}
.ph { color: #b0bec5; }

.btn-login {
  width: 100%; height: 96rpx; line-height: 96rpx;
  background: linear-gradient(135deg, #4A90D9, #3b7bd9);
  color: #fff; border-radius: 50rpx;
  font-size: 34rpx; font-weight: 600; letter-spacing: 4rpx;
  border: none; margin-top: 20rpx;
  box-shadow: 0 10rpx 30rpx rgba(74, 144, 217, 0.35);
}
.btn-login:active { opacity: 0.85; }

.footer-row {
  display: flex; justify-content: center; align-items: center;
  margin-top: 40rpx; font-size: 26rpx;
}
.footer-row .tip { color: #8A9AAE; }
.footer-row .link { color: #4A90D9; font-weight: 600; margin-left: 10rpx; }

.bottom-tip {
  text-align: center; color: rgba(255,255,255,0.9);
  font-size: 24rpx; margin-top: 80rpx; letter-spacing: 2rpx;
}
</style>