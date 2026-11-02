<template>
  <view class="page-container">
    <view class="header-nav">
      <view class="brand">
        <text class="logo">🌱</text>
        <text class="title">银发无忧社区</text>
      </view>
      <view class="actions">
        <block v-if="!userInfo.id">
          <navigator url="/pages/login/login"><button class="btn-login" size="mini">登录</button></navigator>
          <navigator url="/pages/register/register"><button class="btn-register" size="mini">注册</button></navigator>
        </block>
        <view v-else class="user-greeting">
          <text class="greeting-text" @click="goToDashboard">👋 {{ userInfo.realName }}</text>
          <text class="role-tag">{{ roleMap[userInfo.role] }}</text>
          <text class="logout-btn" @click.stop="handleLogout">退出</text>
        </view>
      </view>
    </view>

    <view v-if="userInfo.id && userInfo.role === 1" class="sos-quick" @click="quickSOS">
      <text class="sos-icon">🆘</text>
      <text class="sos-text">一键紧急求助</text>
    </view>

    <view class="section-head">
      <text class="section-title">📰 社区资讯</text>
      <text class="more" @click="goMoreNews">更多 ></text>
    </view>

    <view v-for="item in newsList" :key="item.id" class="card" @click="goNewsDetail(item.id)">
      <view class="news-header">
        <text class="title">{{ item.title }}</text>
        <text class="tag" :style="{ background: newsBg(item.type) }">{{ ['','科普','通知','公益'][item.type] }}</text>
      </view>
      <text class="summary">{{ item.content.length > 50 ? item.content.slice(0, 50) + '...' : item.content }}</text>
      <view class="footer"><text class="time">{{ item.createTime }}</text></view>
    </view>

    <view class="section-head" style="margin-top: 40rpx;">
      <text class="section-title">❤️ 社区服务</text>
      <text class="more" @click="goMoreOrders">更多 ></text>
    </view>

    <scroll-view scroll-x class="filter-scroll" show-scrollbar="false">
      <view class="filter-bar">
        <view class="filter-tab" :class="{active: serviceType===0}" @click="serviceType=0">全部服务</view>
        <view class="filter-tab" :class="{active: serviceType===1}" @click="serviceType=1">陪护</view>
        <view class="filter-tab" :class="{active: serviceType===2}" @click="serviceType=2">助老帮扶</view>
        <view class="filter-tab" :class="{active: serviceType===3}" @click="serviceType=3">社区帮扶</view>
      </view>
    </scroll-view>

    <scroll-view v-if="userInfo.role === 2" scroll-x class="filter-scroll" show-scrollbar="false">
      <view class="filter-bar">
        <view class="filter-tab small" :class="{active: volFilter==='all'}" @click="volFilter='all'">全部</view>
        <view class="filter-tab small" :class="{active: volFilter==='mine'}" @click="volFilter='mine'">已接等待组队</view>
        <view class="filter-tab small" :class="{active: volFilter==='others'}" @click="volFilter='others'">未接</view>
      </view>
    </scroll-view>

    <view v-for="item in displayedOrders" :key="item.order.id" class="card" @click="goOrderDetail(item.order.id)">
      <view class="order-header">
        <text class="order-title">{{ ['陪护','助老帮扶','社区帮扶'][item.order.serviceType-1] }}</text>
        <view style="display:flex;gap:8rpx;align-items:center;">
          <text v-if="isMine(item)" class="tag mine-tag">我的</text>
          <text class="tag pending-tag">待接单</text>
        </view>
      </view>
      <view class="order-info">👤 {{ item.elderName }}</view>
      <view class="order-info">📍 {{ item.order.address || '未填写地址' }}</view>
      <view class="order-info" v-if="item.order.contactPhone">📞 {{ item.order.contactPhone }}</view>
      <view class="order-info" v-if="item.order.appointmentTime">⏰ {{ item.order.appointmentTime }}</view>
      <view class="order-info">👥 需要 {{ item.order.peopleCount }} 人，已接 {{ item.order.acceptedCount }} 人</view>
      <view class="order-info">📝 {{ item.order.remark || '无备注' }}</view>
    </view>
    <view v-if="displayedOrders.length === 0" class="empty">暂无服务需求</view>

    <view style="height: 60rpx;"></view>
  </view>
</template>

<script>
import { request } from '@/utils/request.js'
import { getUser, clearUser } from '@/utils/storage.js'

export default {
  data() {
    return {
      userInfo: {},
      newsList: [],
      pendingOrders: [],
      myOrderIds: [],
      myElderIds: [],
      serviceType: 0,
      volFilter: 'all',
      roleMap: { 1: '👴 老人', 2: '🧑‍💼 志愿者', 3: '👨‍💼 管理员', 4: '👨‍👧 子女', 5: '👑 超级管理员' }
    }
  },
  computed: {
    displayedOrders() {
      let list = this.pendingOrders
      if (this.serviceType !== 0) {
        list = list.filter(i => i.order.serviceType === this.serviceType)
      }
      if (this.userInfo.role === 2) {
        if (this.volFilter === 'mine') return list.filter(i => this.myOrderIds.includes(i.order.id))
        else if (this.volFilter === 'others') return list.filter(i => !this.myOrderIds.includes(i.order.id))
      }
      return list
    }
  },
  onShow() {
    this.userInfo = getUser() || {}
    this.loadNews()
    this.loadPendingOrders()
    this.loadMyOrderIds()
    this.loadMyElderIds()
  },
  methods: {
    newsBg(type) { return type === 1 ? '#E8F4FD' : type === 2 ? '#FFF3E0' : '#FCE4EC' },
    async loadNews() { try { this.newsList = await request({ url: '/order/news/home' }) } catch (e) {} },
    async loadPendingOrders() { try { this.pendingOrders = await request({ url: '/order/home/pending' }) } catch (e) {} },
    async loadMyOrderIds() {
      if (this.userInfo.role !== 2 || !this.userInfo.id) return
      try {
        const list = await request({ url: `/order/list?volunteerId=${this.userInfo.id}` })
        this.myOrderIds = list.map(i => i.order.id)
      } catch (e) { this.myOrderIds = [] }
    },
    async loadMyElderIds() {
      if (this.userInfo.role !== 4 || !this.userInfo.id) return
      try {
        const list = await request({ url: `/user/getBindElderList?guardianId=${this.userInfo.id}` })
        this.myElderIds = list.map(e => e.id)
      } catch (e) { this.myElderIds = [] }
    },
    isMine(item) {
      if (!this.userInfo.id) return false
      if (this.userInfo.role === 3 || this.userInfo.role === 5 || this.userInfo.role === 2) return false
      if (this.userInfo.role === 1) return item.order.publisherId === this.userInfo.id || item.order.elderId === this.userInfo.id
      if (this.userInfo.role === 4) {
        if (item.order.publisherId === this.userInfo.id) return true
        return this.myElderIds.includes(item.order.elderId)
      }
      return false
    },
    goMoreNews() { uni.navigateTo({ url: '/pages/news/news-list' }) },
    goMoreOrders() { uni.navigateTo({ url: '/pages/order/order-list' }) },
    goNewsDetail(id) { uni.navigateTo({ url: `/pages/news/news-detail?id=${id}` }) },
    goOrderDetail(id) { uni.navigateTo({ url: `/pages/order/order-detail?id=${id}` }) },
    goToDashboard() {
      const map = { 1: '/pages/elder/elder', 2: '/pages/volunteer/volunteer', 3: '/pages/admin/admin', 4: '/pages/guardian/guardian', 5: '/pages/admin/admin' }
      if (map[this.userInfo.role]) uni.navigateTo({ url: map[this.userInfo.role] })
      else uni.showToast({ title: '未知角色', icon: 'none' })
    },
    handleLogout() {
      uni.showModal({
        title: '提示', content: '确认退出登录吗？',
        success: (res) => {
          if (res.confirm) {
            clearUser()
            this.userInfo = {}; this.myOrderIds = []; this.myElderIds = []
            uni.showToast({ title: '已退出' })
          }
        }
      })
    },
    async quickSOS() {
      try {
        const guardians = await request({ url: `/user/getGuardiansByElder?elderId=${this.userInfo.id}` })
        if (!guardians || guardians.length === 0) return uni.showToast({ title: '暂无绑定监护人', icon: 'none' })
        for (const g of guardians) {
          await request({ url: '/order/alert/send', method: 'POST', data: { elderId: this.userInfo.id, guardianId: g.id } })
        }
        uni.showToast({ title: `已通知 ${guardians.length} 位监护人`, icon: 'success', duration: 2000 })
      } catch (e) { uni.showToast({ title: '发送失败', icon: 'none' }) }
    }
  }
}
</script>

<style scoped>
page { background-color: #f7f8fa; }
.page-container { min-height: 100vh; padding: 20rpx; }
.header-nav { display: flex; justify-content: space-between; align-items: center; padding: 24rpx 32rpx; background: #ffffff; border-radius: 24rpx; box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04); margin: 50rpx 0 24rpx; }
.brand { display: flex; align-items: center; }
.logo { font-size: 48rpx; margin-right: 16rpx; }
.title { font-size: 38rpx; font-weight: 700; color: #333; }
.actions { display: flex; align-items: center; gap: 20rpx; }
.btn-login { width: 140rpx; height: 60rpx; border: 2rpx solid #4A90D9; border-radius: 30rpx; color: #4A90D9; background: #fff; font-size: 26rpx; line-height: 60rpx; padding: 0; }
.btn-register { width: 140rpx; height: 60rpx; background: #4A90D9; color: #fff; border-radius: 30rpx; font-size: 26rpx; line-height: 60rpx; padding: 0; border: none; }
.user-greeting { display: flex; align-items: center; gap: 16rpx; background: #F0F7FF; padding: 10rpx 28rpx; border-radius: 30rpx; }
.greeting-text { font-size: 28rpx; color: #333; font-weight: 500; }
.role-tag { font-size: 24rpx; color: #0066cc; background: #fff; padding: 4rpx 18rpx; border-radius: 30rpx; }
.logout-btn { margin-left: 16rpx; padding: 6rpx 18rpx; background: #f0f2f5; border-radius: 30rpx; font-size: 22rpx; color: #666; }
.sos-quick { margin: 0 0 24rpx 0; padding: 36rpx; background: linear-gradient(135deg, #ff4d4f, #d9363e); border-radius: 24rpx; display: flex; align-items: center; justify-content: center; box-shadow: 0 12rpx 30rpx rgba(255, 77, 79, 0.35); }
.sos-icon { font-size: 52rpx; margin-right: 20rpx; }
.sos-text { color: #fff; font-size: 38rpx; font-weight: 700; letter-spacing: 4rpx; }
.section-head { display: flex; justify-content: space-between; align-items: center; margin: 24rpx 0 16rpx; padding: 0 8rpx; }
.section-title { font-size: 34rpx; font-weight: 700; color: #1a2332; }
.more { font-size: 26rpx; color: #4A90D9; }
.filter-scroll { white-space: nowrap; margin-bottom: 16rpx; }
.filter-bar { display: inline-flex; gap: 12rpx; padding: 6rpx 0; }
.filter-tab { display: inline-block; padding: 14rpx 36rpx; border-radius: 30rpx; background: #fff; font-size: 26rpx; color: #6a7a8e; }
.filter-tab.small { padding: 10rpx 24rpx; font-size: 24rpx; }
.filter-tab.active { background: #4A90D9; color: #fff; font-weight: 600; }
.card { background: #fff; border-radius: 20rpx; padding: 32rpx; margin-bottom: 24rpx; box-shadow: 0 3rpx 12rpx rgba(0,0,0,0.04); }
.news-header, .order-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 16rpx; }
.title, .order-title { font-size: 34rpx; font-weight: 600; color: #333; flex: 1; }
.tag { font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 30rpx; color: #333; }
.pending-tag { background: #FFF3E0; color: #d48806; }
.mine-tag { background: #FFE4E4; color: #ff4d4f; font-weight: 600; }
.summary { display: block; font-size: 28rpx; color: #666; line-height: 1.7; }
.footer { display: flex; justify-content: flex-end; margin-top: 12rpx; }
.time { font-size: 22rpx; color: #999; }
.order-info { font-size: 28rpx; color: #6a7a8e; margin-top: 8rpx; }
.empty { text-align: center; color: #999; padding: 60rpx 0; font-size: 28rpx; }
</style>