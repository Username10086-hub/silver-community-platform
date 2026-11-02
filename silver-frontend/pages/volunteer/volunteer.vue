<template>
  <view class="container">
    <view class="profile-card">
      <view class="avatar-circle">{{ avatarLetter }}</view>
      <view class="profile-info">
        <view class="name">{{ userInfo.realName }}</view>
        <view class="role-tag">志愿者</view>
        <view class="uid">ID：{{ userInfo.id }}</view>
      </view>
    </view>

    <view class="tabs">
      <view class="tab" :class="{active: currentTab===0}" @click="currentTab=0">我的工单</view>
      <view class="tab" :class="{active: currentTab===1}" @click="currentTab=1">个人中心</view>
    </view>

    <view v-show="currentTab===0">
      <view class="stats-card">
        <view class="stat-block">
          <text class="num" style="color:#94979c">{{ countByStatus(0) }}</text>
          <text class="label">待组队</text>
        </view>
        <view class="stat-block">
          <text class="num" style="color:#4A90D9">{{ countByStatus(1) }}</text>
          <text class="label">进行中</text>
        </view>
        <view class="stat-block">
          <text class="num" style="color:#faad14">{{ countByStatus(4) }}</text>
          <text class="label">待验收</text>
        </view>
        <view class="stat-block">
          <text class="num" style="color:#52c41a">{{ countByStatus(2) }}</text>
          <text class="label">已完成</text>
        </view>
      </view>

      <view class="tabs-inner">
        <view class="tab-inner" :class="{active: statusTab===5}" @click="statusTab=5">待组队</view>
        <view class="tab-inner" :class="{active: statusTab===1}" @click="statusTab=1">进行中</view>
        <view class="tab-inner" :class="{active: statusTab===4}" @click="statusTab=4">待验收</view>
        <view class="tab-inner" :class="{active: statusTab===2}" @click="statusTab=2">已完成</view>
        <view class="tab-inner" :class="{active: statusTab===3}" @click="statusTab=3">已取消</view>
      </view>

      <view class="card" v-for="order in filteredOrders" :key="order.id" @click="viewDetail(order.id)">
        <view class="order-line">
          <text class="order-title">{{ ['陪护','助老帮扶','社区帮扶'][order.serviceType-1] }}</text>
          <text class="status-tag" :class="statusClass(order.status)">{{ displayStatus(order) }}</text>
        </view>
        <view class="desc">发布者：{{ order.elderName || '加载中' }}</view>
        <view class="desc">📍 {{ order.address || '未填' }}</view>
        <view class="desc" v-if="order.appointmentTime">⏰ {{ order.appointmentTime }}</view>
        <view class="desc">👥 {{ order.acceptedCount }}/{{ order.peopleCount }}人</view>
        <view class="desc">📝 {{ order.remark || '无' }}</view>
        <view v-if="order.status===1 && !isMyPartDone(order.id)" class="actions">
          <button class="btn-sm btn-success" @click.stop="completeOrder(order.id)">完成我的部分</button>
        </view>
      </view>
      <view v-if="filteredOrders.length===0" class="empty-tip">暂无工单</view>
    </view>

    <view v-show="currentTab===1">
      <view class="card">
        <view class="section-head">
          <text class="section-title">👤 基本信息</text>
          <text class="edit-btn" @click="openEditProfile">编辑</text>
        </view>
        <view class="info-item"><text>姓名</text><text>{{ userInfo.realName }}</text></view>
        <view class="info-item"><text>用户ID</text><text>{{ userInfo.id }}</text></view>
        <view class="info-item"><text>用户名</text><text>{{ userInfo.username }}</text></view>
        <view class="info-item"><text>年龄</text><text>{{ userInfo.age || '未填' }}</text></view>
        <view class="info-item"><text>性别</text><text>{{ userInfo.gender === 1 ? '男' : (userInfo.gender === 2 ? '女' : '未填') }}</text></view>
        <view class="info-item"><text>手机</text><text>{{ userInfo.phone || '未填' }}</text></view>
      </view>
      <button class="btn-logout" @click="logout">退出登录</button>
      <button class="btn-delete" @click="deleteAccount">注销账号</button>
    </view>

    <view v-if="showProfileModal" class="modal-mask" @click="showProfileModal = false">
      <view class="modal-box" @click.stop>
        <view class="modal-title">编辑个人信息</view>
        <view class="form-row"><text class="label">姓名</text><input v-model="editProfile.realName" /></view>
        <view class="form-row"><text class="label">年龄</text><input v-model="editProfile.age" type="number" /></view>
        <view class="form-row">
          <text class="label">性别</text>
          <radio-group @change="e => editProfile.gender = Number(e.detail.value)" style="display:flex; gap:20rpx;">
            <label><radio value="1" :checked="editProfile.gender === 1" color="#4A90D9" />男</label>
            <label><radio value="2" :checked="editProfile.gender === 2" color="#4A90D9" />女</label>
          </radio-group>
        </view>
        <view class="form-row"><text class="label">手机</text><input v-model="editProfile.phone" /></view>
        <button class="btn-primary" @click="saveProfile">保存</button>
        <button class="btn-cancel" @click="showProfileModal = false">取消</button>
      </view>
    </view>
  </view>
</template>

<script>
import { request } from '@/utils/request.js'
import { getUser, setUser, clearUser } from '@/utils/storage.js'

export default {
  data() {
    return {
      userInfo: {}, currentTab: 0, statusTab: 5,
      allOrders: [], doneOrderIds: [],
      showProfileModal: false, editProfile: {},
      notifyTimer: null
    }
  },
  computed: {
    avatarLetter() {
      const name = this.userInfo && this.userInfo.realName
      return name ? name.substring(0, 1) : '?'
    },
    filteredOrders() {
      if (this.statusTab === 1) return this.allOrders.filter(o => o.status === 1)
      if (this.statusTab === 5) return this.allOrders.filter(o => o.status === 0 && o.acceptedCount < o.peopleCount)
      return this.allOrders.filter(o => o.status === this.statusTab)
    }
  },
  onShow() {
    this.userInfo = getUser() || {}
    this.loadOrders()
    this.startNotifyPolling()
  },
  onHide() { if (this.notifyTimer) { clearInterval(this.notifyTimer); this.notifyTimer = null } },
  onUnload() { if (this.notifyTimer) { clearInterval(this.notifyTimer); this.notifyTimer = null } },
  methods: {
    async loadOrders() {
      try {
        const list = await request({ url: `/order/list?volunteerId=${this.userInfo.id}` })
        this.allOrders = list.map(item => ({ ...item.order, elderName: item.elderName }))
        this.doneOrderIds = []
        for (const o of this.allOrders) {
          try {
            const detail = await request({ url: `/order/detail/${o.id}` })
            const mine = (detail.volunteers || []).find(v => v.volunteerId === this.userInfo.id)
            if (mine && mine.status === 2) this.doneOrderIds.push(o.id)
          } catch (e) {}
        }
      } catch (e) { console.log(e) }
    },
    startNotifyPolling() {
      if (this.notifyTimer) clearInterval(this.notifyTimer)
      this.notifyTimer = setInterval(async () => {
        const user = getUser()
        if (!user || user.role !== 2) return
        try {
          const msgs = await request({ url: `/order/volunteer-notify/unread?volunteerId=${user.id}` })
          if (msgs && msgs.length > 0) {
            const m = msgs[0]
            const vn = m.notify
            uni.showModal({
              title: vn.type === 2 ? '⚠️ 工单被取消' : (vn.type === 1 ? '⚠️ 工单被修改' : '✅ 工单审核通过'),
              content: vn.content,
              showCancel: false,
              success: async () => {
                await request({ url: `/order/volunteer-notify/read/${vn.id}`, method: 'PUT' })
                this.loadOrders()
              }
            })
          }
        } catch (e) {}
      }, 3000)
    },
    isMyPartDone(orderId) { return this.doneOrderIds.includes(orderId) },
    countByStatus(s) {
      if (s === 0) return this.allOrders.filter(o => o.status === 0 && o.acceptedCount < o.peopleCount).length
      return this.allOrders.filter(o => o.status === s).length
    },
    displayStatus(order) {
      if (order.status === 0 && order.acceptedCount < order.peopleCount) return '待组队'
      return ['待接单','进行中','已完成','已取消','待验收'][order.status] || ''
    },
    statusClass(s) { return ['pending','ongoing','done','cancelled','verify'][s] || '' },
    async completeOrder(id) {
      await request({ url: `/order/complete/${id}?volunteerId=${this.userInfo.id}`, method: 'PUT' })
      uni.showToast({ title: '已提交，等待确认' })
      this.loadOrders()
    },
    viewDetail(id) { uni.navigateTo({ url: `/pages/order/order-detail?id=${id}` }) },
    openEditProfile() { this.editProfile = { ...this.userInfo }; this.showProfileModal = true },
    async saveProfile() {
      await request({ url: '/user/update', method: 'PUT', data: this.editProfile, header: { 'Content-Type': 'application/json' } })
      Object.assign(this.userInfo, this.editProfile)
      setUser(this.userInfo)
      uni.showToast({ title: '已保存' })
      this.showProfileModal = false
    },
    logout() { clearUser(); uni.reLaunch({ url: '/pages/index/index' }) },
    deleteAccount() {
      uni.showModal({
        title: '⚠️ 危险操作', content: '注销后账号及数据将被永久删除！', confirmColor: '#ff4d4f',
        success: async (res) => {
          if (!res.confirm) return
          await request({ url: `/user/delete/${this.userInfo.id}`, method: 'DELETE' })
          clearUser(); uni.showToast({ title: '已注销' })
          setTimeout(() => uni.reLaunch({ url: '/pages/index/index' }), 800)
        }
      })
    }
  }
}
</script>

<style scoped>
/* 与上一版相同，保留 */
.container { padding: 20rpx; }
.profile-card { display: flex; align-items: center; background: linear-gradient(135deg, #52c41a, #3da50e); border-radius: 24rpx; padding: 40rpx; margin-bottom: 24rpx; color: #fff; }
.avatar-circle { width: 120rpx; height: 120rpx; border-radius: 50%; background: rgba(255,255,255,0.25); display: flex; align-items: center; justify-content: center; font-size: 52rpx; font-weight: bold; margin-right: 30rpx; }
.profile-info .name { font-size: 40rpx; font-weight: 700; }
.profile-info .role-tag { display: inline-block; background: rgba(255,255,255,0.25); padding: 4rpx 18rpx; border-radius: 30rpx; font-size: 24rpx; margin: 10rpx 0; }
.profile-info .uid { font-size: 24rpx; opacity: 0.9; }
.tabs { display: flex; background: #fff; border-radius: 50rpx; padding: 6rpx; margin-bottom: 24rpx; }
.tab { flex: 1; text-align: center; padding: 20rpx 0; border-radius: 50rpx; font-size: 28rpx; color: #6a7a8e; }
.tab.active { background: #52c41a; color: #fff; font-weight: 600; }
.stats-card { display: flex; background: #fff; border-radius: 20rpx; padding: 24rpx 0; margin-bottom: 20rpx; }
.stat-block { flex: 1; text-align: center; }
.stat-block .num { display: block; font-size: 40rpx; font-weight: 700; }
.stat-block .label { font-size: 22rpx; color: #8A9AAE; margin-top: 6rpx; }
.tabs-inner { display: flex; gap: 10rpx; margin-bottom: 20rpx; flex-wrap: wrap; }
.tab-inner { flex: 1; min-width: 100rpx; text-align: center; padding: 14rpx 0; border-radius: 30rpx; background: #fff; font-size: 24rpx; color: #6a7a8e; }
.tab-inner.active { background: #4A90D9; color: #fff; font-weight: 600; }
.card { background: #fff; border-radius: 20rpx; padding: 24rpx; margin-bottom: 20rpx; }
.order-line { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12rpx; }
.order-title { font-size: 30rpx; font-weight: 500; }
.status-tag { padding: 6rpx 18rpx; border-radius: 30rpx; font-size: 24rpx; color: #fff; }
.status-tag.pending { background: #94979c; }
.status-tag.ongoing { background: #4f9ef7; }
.status-tag.verify { background: #faad14; }
.status-tag.done { background: #52c41a; }
.status-tag.cancelled { background: #999; }
.desc { font-size: 26rpx; color: #6a7a8e; margin-top: 6rpx; }
.actions { margin-top: 16rpx; display: flex; gap: 16rpx; flex-wrap: wrap; }
.btn-sm { padding: 12rpx 30rpx; border-radius: 30rpx; font-size: 26rpx; border: none; color: #fff; }
.btn-sm.btn-success { background: #52c41a; }
.empty-tip { text-align: center; color: #999; margin: 40rpx 0; font-size: 26rpx; }
.section-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16rpx; }
.section-title { font-size: 30rpx; font-weight: 600; color: #2d3a4a; }
.edit-btn { font-size: 26rpx; color: #52c41a; padding: 6rpx 20rpx; border: 1px solid #52c41a; border-radius: 30rpx; }
.info-item { display: flex; justify-content: space-between; padding: 16rpx 0; font-size: 28rpx; color: #4a5a6e; border-bottom: 1rpx solid #f0f4f8; }
.info-item:last-child { border-bottom: none; }
.info-item text:first-child { color: #8A9AAE; }
.btn-logout { width: 100%; background: #f0f4f8; color: #4a5a6e; border: none; border-radius: 50rpx; padding: 22rpx; font-size: 30rpx; margin-bottom: 20rpx; }
.btn-delete { width: 100%; background: #fff; color: #ff4d4f; border: 1px solid #ff4d4f; border-radius: 50rpx; padding: 22rpx; font-size: 30rpx; }
.btn-primary { width: 100%; background: #4A90D9; color: #fff; border: none; border-radius: 50rpx; padding: 22rpx; font-size: 30rpx; margin-top: 20rpx; }
.btn-cancel { width: 100%; background: #f0f4f8; color: #666; border: none; border-radius: 50rpx; padding: 22rpx; font-size: 30rpx; margin-top: 16rpx; }
.form-row { display: flex; align-items: center; min-height: 90rpx; border-bottom: 1rpx solid #f0f4f8; padding: 10rpx 0; }
.form-row .label { width: 180rpx; flex-shrink: 0; font-size: 28rpx; color: #8A9AAE; }
.form-row input { flex: 1; height: 90rpx; line-height: 90rpx; font-size: 28rpx; color: #1a2332; background: transparent; border: none; }
.modal-mask { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 999; }
.modal-box { width: 640rpx; max-height: 80vh; overflow-y: auto; background: #fff; border-radius: 24rpx; padding: 40rpx; box-sizing: border-box; }
.modal-title { font-size: 34rpx; font-weight: 700; text-align: center; margin-bottom: 30rpx; }
</style>