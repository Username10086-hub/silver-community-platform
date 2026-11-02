<template>
  <view class="page-container">
    <view class="tabs">
      <view class="tab" :class="{active: type===0}" @click="switchType(0)">全部</view>
      <view class="tab" :class="{active: type===1}" @click="switchType(1)">科普</view>
      <view class="tab" :class="{active: type===2}" @click="switchType(2)">通知</view>
      <view class="tab" :class="{active: type===3}" @click="switchType(3)">公益</view>
    </view>
    <view v-for="item in newsList" :key="item.id" class="card" @click="goDetail(item.id)">
      <view class="news-header">
        <text class="title">{{ item.title }}</text>
        <text class="tag">{{ ['','科普','通知','公益'][item.type] }}</text>
      </view>
      <text class="summary">{{ item.content.length > 60 ? item.content.slice(0, 60) + '...' : item.content }}</text>
      <text class="time">{{ item.createTime }}</text>
    </view>
    <view v-if="newsList.length===0" class="empty">暂无新闻</view>
  </view>
</template>
<script>
import { request } from '@/utils/request.js'
export default {
  data() { return { type: 0, newsList: [] } },
  onLoad() { this.load() },
  methods: {
    switchType(t) { this.type = t; this.load() },
    async load() {
      const url = this.type === 0 ? '/order/news/list' : `/order/news/list?type=${this.type}`
      this.newsList = await request({ url })
    },
    goDetail(id) { uni.navigateTo({ url: `/pages/news/news-detail?id=${id}` }) }
  }
}
</script>
<style scoped>
.page-container { padding: 20rpx; }
.tabs { display: flex; gap: 16rpx; margin-bottom: 24rpx; }
.tab { flex: 1; text-align: center; padding: 20rpx 0; border-radius: 30rpx; background: #fff; font-size: 28rpx; color: #6a7a8e; }
.tab.active { background: #4A90D9; color: #fff; font-weight: 600; }
.card { background: #fff; border-radius: 20rpx; padding: 28rpx; margin-bottom: 20rpx; }
.news-header { display: flex; justify-content: space-between; margin-bottom: 12rpx; }
.title { font-size: 32rpx; font-weight: 600; flex: 1; color: #1a2332; }
.tag { font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 30rpx; background: #E8F4FD; color: #333; height: fit-content; }
.summary { display: block; font-size: 28rpx; color: #666; line-height: 1.7; }
.time { display: block; font-size: 22rpx; color: #999; margin-top: 12rpx; text-align: right; }
.empty { text-align: center; color: #999; padding: 60rpx; }
</style>