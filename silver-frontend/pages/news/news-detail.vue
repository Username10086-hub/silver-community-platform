<template>
  <view class="page-container" v-if="detail">
    <view class="card">
      <view class="title">{{ detail.title }}</view>
      <view class="meta">
        <text class="tag">{{ ['','科普','通知','公益'][detail.type] }}</text>
        <text class="time">{{ detail.createTime }}</text>
      </view>
      <view class="content">{{ detail.content }}</view>
    </view>
  </view>
</template>

<script>
import { request } from '@/utils/request.js'
export default {
  data() { return { detail: null } },
  onLoad(options) {
    request({ url: `/order/news/detail/${options.id}` }).then(res => this.detail = res);
  }
}
</script>

<style scoped lang="scss">
.meta { display: flex; gap: 20rpx; margin: 20rpx 0; }
.tag { background: #E8F4FD; padding: 4rpx 16rpx; border-radius: 30rpx; font-size: 22rpx; }
.time { color: $text-muted; font-size: 24rpx; }
.content { font-size: 30rpx; line-height: 1.8; color: $text-secondary; white-space: pre-wrap; }
</style>