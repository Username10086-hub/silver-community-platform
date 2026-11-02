<template>
  <view class="page-container">
    <view v-for="item in list" :key="item.order.id" class="card" @click="goDetail(item.order.id)">
      <view class="header">
        <text class="title">{{ ['陪护','助老帮扶','社区帮扶'][item.order.serviceType-1] }}</text>
        <text class="tag">待接单</text>
      </view>
      <view class="info">👤 老人：{{ item.elderName }}</view>
      <view class="info">📍 地址：{{ item.order.address || '未填' }}</view>
      <view class="info">👥 需要 {{ item.order.peopleCount }} 人，已接 {{ item.order.acceptedCount }} 人</view>
      <view class="info">📝 {{ item.order.remark || '无' }}</view>
    </view>
    <view v-if="list.length===0" class="empty">暂无待接单服务</view>
  </view>
</template>
<script>
import { request } from '@/utils/request.js'
export default {
  data() { return { list: [] } },
  onShow() { this.load() },
  methods: {
    async load() { this.list = await request({ url: '/order/home/pending' }) },
    goDetail(id) { uni.navigateTo({ url: `/pages/order/order-detail?id=${id}` }) }
  }
}
</script>
<style scoped>
.page-container { padding: 20rpx; }
.card { background: #fff; border-radius: 20rpx; padding: 28rpx; margin-bottom: 20rpx; }
.header { display: flex; justify-content: space-between; margin-bottom: 16rpx; }
.title { font-size: 32rpx; font-weight: 600; }
.tag { font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 30rpx; background: #FFF3E0; color: #d48806; }
.info { font-size: 26rpx; color: #6a7a8e; margin-top: 8rpx; }
.empty { text-align: center; color: #999; padding: 60rpx; }
</style>