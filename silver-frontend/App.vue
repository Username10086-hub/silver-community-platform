<script>
export default {
  onLaunch() {
    // H5 环境下，清理之前可能在 localStorage 里残留的 userInfo（避免旧数据干扰）
    // #ifdef H5
    localStorage.removeItem('userInfo');
    // #endif
  }
}
</script>

<style>
/* App.vue - 全局样式 */
page {
  background-color: #f0f4f8;
  font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', Arial, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  padding: 20rpx;
  box-sizing: border-box;
}

/* 卡片容器 */
.card {
  background: #ffffff;
  border-radius: 32rpx;
  box-shadow: 0 8rpx 30rpx rgba(0, 0, 0, 0.05);
  padding: 30rpx;
  margin-bottom: 30rpx;
  transition: all 0.3s;
}

/* 标题 */
.page-title {
  font-size: 48rpx;
  font-weight: 700;
  color: #1a2332;
  margin-bottom: 30rpx;
  letter-spacing: 2rpx;
}
.section-title {
  font-size: 36rpx;
  font-weight: 600;
  color: #2d3a4a;
  margin-bottom: 20rpx;
}

/* 输入框 */
.input-group {
  margin-bottom: 28rpx;
}
.input-group label {
  display: block;
  font-size: 28rpx;
  color: #4a5a6e;
  margin-bottom: 10rpx;
  font-weight: 500;
}
.input-group input,
.input-group textarea,
.input-group picker {
  width: 100%;
  background: #f7f9fc;
  border-radius: 20rpx;
  padding: 22rpx 30rpx;
  border: 2rpx solid #e6edf4;
  font-size: 30rpx;
  color: #1a2332;
  box-sizing: border-box;
}
.input-group input:focus,
.input-group textarea:focus {
  border-color: #4f9ef7;
  outline: none;
  background: #ffffff;
}

/* 按钮组 */
.btn {
  border-radius: 50rpx;
  font-size: 32rpx;
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  padding: 0 40rpx;
  border: none;
  transition: all 0.2s;
  cursor: pointer;
  display: inline-block;
}
.btn-primary {
  background: linear-gradient(135deg, #4f9ef7, #3b7bd9);
  color: #fff;
}
.btn-success {
  background: linear-gradient(135deg, #52c41a, #3da50e);
  color: #fff;
}
.btn-warning {
  background: linear-gradient(135deg, #faad14, #d48806);
  color: #fff;
}
.btn-danger {
  background: linear-gradient(135deg, #ff4d4f, #d9363e);
  color: #fff;
}
.btn-outline {
  background: transparent;
  color: #4f9ef7;
  border: 2rpx solid #4f9ef7;
}
.btn-block {
  width: 100%;
  display: block;
}

/* 列表项 */
.list-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 2rpx solid #f0f4f8;
}
.list-item:last-child {
  border-bottom: none;
}
.list-item .info {
  flex: 1;
}
.list-item .info .name {
  font-size: 32rpx;
  font-weight: 500;
  color: #1a2332;
}
.list-item .info .desc {
  font-size: 26rpx;
  color: #6a7a8e;
  margin-top: 6rpx;
}
.list-item .status-tag {
  padding: 6rpx 18rpx;
  border-radius: 30rpx;
  font-size: 24rpx;
  color: #fff;
}
.status-tag.pending { background: #faad14; }
.status-tag.ongoing { background: #4f9ef7; }
.status-tag.done { background: #52c41a; }

/* 浮动按钮 */
.fab {
  position: fixed;
  bottom: 60rpx;
  right: 60rpx;
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #4f9ef7, #3b7bd9);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 60rpx;
  box-shadow: 0 10rpx 30rpx rgba(79, 158, 247, 0.4);
}

</style>