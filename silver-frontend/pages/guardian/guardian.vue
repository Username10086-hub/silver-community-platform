<template>
  <view class="container">
    <view class="profile-card">
      <view class="avatar-circle">{{ avatarLetter }}</view>
      <view class="profile-info">
        <view class="name">{{ userInfo.realName }}</view>
        <view class="role-tag">子女（监护人）</view>
        <view class="uid">ID：{{ userInfo.id }}</view>
      </view>
    </view>

    <view class="tabs">
      <view class="tab" :class="{active: currentTab===0}" @click="currentTab=0">绑定老人</view>
      <view class="tab" :class="{active: currentTab===1}" @click="currentTab=1">发布工单</view>
      <view class="tab" :class="{active: currentTab===2}" @click="currentTab=2">个人中心</view>
    </view>

    <view v-show="currentTab===0">
      <view v-if="elderList.length > 0">
        <view class="item" v-for="item in elderList" :key="item.id">
          <view class="item-info" @click="goDashboard(item.id)">
            <text class="name-text">{{ item.realName }}</text>
            <text class="desc-text">ID：{{ item.id }} | 电话：{{ item.phone || '未填' }}</text>
          </view>
          <text class="remove-btn" @click.stop="unbind(item.id)">解除</text>
        </view>
      </view>
      <view v-else class="empty-tip">暂无绑定的老人</view>
      <button class="btn-add" @click="goAddElder">+ 绑定新老人</button>
    </view>

    <view v-show="currentTab===1">
      <view class="card">
        <view class="form-row">
          <text class="label">选择老人</text>
          <picker mode="selector" :range="elderNameList" @change="onElderChange">
            <view class="picker-value">{{ selectedElderIndex >= 0 ? elderList[selectedElderIndex].realName : '请选择' }}</view>
          </picker>
        </view>
        <view class="form-row">
          <text class="label">服务类型</text>
          <picker mode="selector" :range="serviceTypes" @change="onTypeChange">
            <view class="picker-value">{{ serviceTypes[typeIndex] }}</view>
          </picker>
        </view>
        <view class="form-row"><text class="label">服务地址</text><input v-model="address" placeholder="请输入服务地址" /></view>
        <view class="form-row"><text class="label">联系电话</text><input v-model="contactPhone" type="number" placeholder="请输入联系电话" /></view>
        <view class="form-row"><text class="label">需要人数</text><input v-model="peopleCount" type="number" placeholder="1" /></view>
        <view class="form-row">
          <text class="label">预约日期</text>
          <picker mode="date" :start="todayStr" @change="onDateChange">
            <view class="picker-value">{{ appointmentDate || '请选择日期' }}</view>
          </picker>
        </view>
        <view class="form-row">
          <text class="label">预约时间</text>
          <picker mode="time" @change="onTimeChange">
            <view class="picker-value">{{ appointmentTime || '请选择时间' }}</view>
          </picker>
        </view>
        <view class="form-row textarea-row">
          <text class="label">需求描述</text>
          <textarea v-model="remark" placeholder="请描述老人的需求..." class="textarea-input"></textarea>
        </view>
        <button class="btn-primary" @click="publishForElder">为老人提交工单（等待审核）</button>
      </view>

      <view class="section-title">我代发布的工单</view>
      <scroll-view scroll-x class="order-tabs-scroll" show-scrollbar="false">
        <view class="order-tabs">
          <view class="order-tab" :class="{active: orderStatusTab==='all'}" @click="orderStatusTab='all'">全部</view>
          <view class="order-tab" :class="{active: orderStatusTab==='audit'}" @click="orderStatusTab='audit'">待审核</view>
          <view class="order-tab" :class="{active: orderStatusTab==='waitTeam'}" @click="orderStatusTab='waitTeam'">待组队</view>
          <view class="order-tab" :class="{active: orderStatusTab==='ongoing'}" @click="orderStatusTab='ongoing'">进行中</view>
          <view class="order-tab" :class="{active: orderStatusTab==='verify'}" @click="orderStatusTab='verify'">待验收</view>
          <view class="order-tab" :class="{active: orderStatusTab==='done'}" @click="orderStatusTab='done'">已完成</view>
          <view class="order-tab" :class="{active: orderStatusTab==='cancel'}" @click="orderStatusTab='cancel'">已取消</view>
          <view class="order-tab" :class="{active: orderStatusTab==='reject'}" @click="orderStatusTab='reject'">已拒绝</view>
          <view class="order-tab" :class="{active: orderStatusTab==='revoke'}" @click="orderStatusTab='revoke'">已撤销</view>
        </view>
      </scroll-view>

      <view v-for="o in filteredMyPublishedOrders" :key="o.id" class="card" @click="goOrderDetail(o.id)">
        <view class="order-line">
          <text class="order-title">{{ ['陪护','助老帮扶','社区帮扶'][o.serviceType-1] }}</text>
          <text class="status-tag" :class="statusClass(o)">{{ statusText(o) }}</text>
        </view>
        <text class="order-desc">老人：{{ o.elderName || ('ID:' + o.elderId) }} | 📍 {{ o.address || '未填' }} | 👥 {{ o.acceptedCount }}/{{ o.peopleCount }}</text>
        <text class="order-desc" v-if="o.appointmentTime">⏰ {{ o.appointmentTime }}</text>
        <text class="order-desc">📝 {{ o.remark }}</text>
      </view>
      <view v-if="filteredMyPublishedOrders.length===0" class="empty-tip">暂无代发布的工单</view>

      <view class="section-title" style="margin-top:40rpx;">👴 老人发布的工单</view>
      <scroll-view scroll-x class="order-tabs-scroll" show-scrollbar="false">
        <view class="order-tabs">
          <view class="order-tab" :class="{active: elderOrderStatusTab==='all'}" @click="elderOrderStatusTab='all'">全部</view>
          <view class="order-tab" :class="{active: elderOrderStatusTab==='audit'}" @click="elderOrderStatusTab='audit'">待审核</view>
          <view class="order-tab" :class="{active: elderOrderStatusTab==='waitTeam'}" @click="elderOrderStatusTab='waitTeam'">待组队</view>
          <view class="order-tab" :class="{active: elderOrderStatusTab==='ongoing'}" @click="elderOrderStatusTab='ongoing'">进行中</view>
          <view class="order-tab" :class="{active: elderOrderStatusTab==='verify'}" @click="elderOrderStatusTab='verify'">待验收</view>
          <view class="order-tab" :class="{active: elderOrderStatusTab==='done'}" @click="elderOrderStatusTab='done'">已完成</view>
          <view class="order-tab" :class="{active: elderOrderStatusTab==='cancel'}" @click="elderOrderStatusTab='cancel'">已取消</view>
          <view class="order-tab" :class="{active: elderOrderStatusTab==='revoke'}" @click="elderOrderStatusTab='revoke'">已撤销</view>
        </view>
      </scroll-view>

      <view v-for="o in filteredElderOrders" :key="o.id" class="card" @click="goOrderDetail(o.id)">
        <view class="order-line">
          <text class="order-title">{{ ['陪护','助老帮扶','社区帮扶'][o.serviceType-1] }}</text>
          <text class="status-tag" :class="statusClass(o)">{{ statusText(o) }}</text>
        </view>
        <text class="order-desc">老人：{{ o.elderName || ('ID:' + o.elderId) }} | 📍 {{ o.address || '未填' }} | 👥 {{ o.acceptedCount }}/{{ o.peopleCount }}</text>
        <text class="order-desc" v-if="o.appointmentTime">⏰ {{ o.appointmentTime }}</text>
        <text class="order-desc">📝 {{ o.remark }}</text>
      </view>
      <view v-if="filteredElderOrders.length===0" class="empty-tip">暂无老人自主发布的工单</view>
    </view>

    <view v-show="currentTab===2">
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
          <radio-group @change="onEditGenderChange" class="radio-inline">
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
      userInfo: {},
      currentTab: 0,
      elderList: [],
      selectedElderIndex: -1,
      serviceTypes: ['陪护', '助老帮扶', '社区帮扶'],
      typeIndex: 0,
      appointmentDate: '',
      appointmentTime: '',
      todayStr: new Date().toISOString().substring(0, 10),
      address: '',
      contactPhone: '',
      peopleCount: 1,
      remark: '',
      myPublishedOrders: [],
      elderOrders: [],
      orderStatusTab: 'all',
      elderOrderStatusTab: 'all',
      showProfileModal: false,
      editProfile: {},
      timer: null
    }
  },
  computed: {
    elderNameList() { return this.elderList.map(e => e.realName) },
    avatarLetter() {
      const name = this.userInfo && this.userInfo.realName
      return name ? name.substring(0, 1) : '?'
    },
    filteredMyPublishedOrders() { return this.filterByTab(this.myPublishedOrders, this.orderStatusTab) },
    filteredElderOrders() { return this.filterByTab(this.elderOrders, this.elderOrderStatusTab) }
  },
  onShow() {
    this.userInfo = getUser() || {}
    this.loadElders().then(() => this.loadElderOrders())
    this.loadMyPublishedOrders()
    this.startPolling()
  },
  onHide() { if (this.timer) { clearInterval(this.timer); this.timer = null } },
  methods: {
    filterByTab(list, tab) {
      if (tab === 'all') return list
      if (tab === 'audit') return list.filter(o => o.auditStatus === 0 || o.auditStatus === 3)
      if (tab === 'waitTeam') return list.filter(o => o.auditStatus === 1 && o.status === 0 && o.acceptedCount < o.peopleCount)
      if (tab === 'ongoing') return list.filter(o => o.auditStatus === 1 && o.status === 1)
      if (tab === 'verify') return list.filter(o => o.auditStatus === 1 && o.status === 4)
      if (tab === 'done') return list.filter(o => o.auditStatus === 1 && o.status === 2)
      if (tab === 'cancel') return list.filter(o => o.status === 3)
      if (tab === 'reject') return list.filter(o => o.auditStatus === 2)
      if (tab === 'revoke') return list.filter(o => o.status === 5)
      return list
    },
    async loadElders() {
      if (!this.userInfo.id) return
      try {
        const res = await request({ url: `/user/getBindElderList?guardianId=${this.userInfo.id}` })
        this.elderList = Array.isArray(res) ? res : []
      } catch (e) { console.log(e) }
    },
    async loadMyPublishedOrders() {
      try {
        const all = await request({ url: `/order/list?publisherId=${this.userInfo.id}` })
        this.myPublishedOrders = all.map(item => ({ ...item.order, elderName: item.elderName }))
      } catch (e) { console.log(e) }
    },
    async loadElderOrders() {
      try {
        const list = []
        for (const e of this.elderList) {
          const orders = await request({ url: `/order/list?publisherId=${e.id}` })
          list.push(...orders.map(i => ({ ...i.order, elderName: i.elderName })))
        }
        this.elderOrders = list
      } catch (e) { this.elderOrders = [] }
    },
    goAddElder() { uni.navigateTo({ url: '/pages/register/register?fromGuardian=1' }) },
    goDashboard(id) { uni.navigateTo({ url: `/pages/guardian/dashboard?id=${id}` }) },
    goOrderDetail(id) { uni.navigateTo({ url: `/pages/order/order-detail?id=${id}` }) },
    async unbind(elderId) {
      uni.showModal({
        title: '确认解除绑定？', content: '解除后无法查看该老人的信息',
        success: async (res) => {
          if (!res.confirm) return
          await request({ url: `/user/unbind?guardianId=${this.userInfo.id}&elderId=${elderId}`, method: 'DELETE' })
          uni.showToast({ title: '已解除' })
          this.loadElders(); this.loadMyPublishedOrders(); this.loadElderOrders()
        }
      })
    },
    onElderChange(e) { this.selectedElderIndex = Number(e.detail.value) },
    onTypeChange(e) { this.typeIndex = Number(e.detail.value) },
    onDateChange(e) { this.appointmentDate = e.detail.value },
    onTimeChange(e) { this.appointmentTime = e.detail.value },
    async publishForElder() {
      if (this.selectedElderIndex < 0) return uni.showToast({ title: '请选择老人', icon: 'none' })
      if (!this.remark.trim()) return uni.showToast({ title: '请填写需求', icon: 'none' })
      if (!this.appointmentDate || !this.appointmentTime) return uni.showToast({ title: '请选择预约日期和时间', icon: 'none' })
      const elder = this.elderList[this.selectedElderIndex]
      const apptTime = this.appointmentDate + ' ' + this.appointmentTime + ':00'
      try {
        await request({
          url: '/order/publish',
          method: 'POST',
          data: {
            elderId: elder.id,
            publisherId: this.userInfo.id,
            publisherRole: 4,
            serviceType: Number(this.typeIndex) + 1,
            address: this.address,
            contactPhone: this.contactPhone,
            peopleCount: Number(this.peopleCount) || 1,
            appointmentTime: apptTime,
            remark: this.remark
          }
        })
        uni.showToast({ title: '已提交' })
        this.remark = ''; this.address = ''; this.contactPhone = ''; this.peopleCount = 1
        this.appointmentDate = ''; this.appointmentTime = ''
        this.loadMyPublishedOrders()
      } catch (e) {
        uni.showToast({ title: e.msg || '提交失败', icon: 'none' })
      }
    },
    statusText(o) {
      if (o.status === 5) return '已撤销'
      if (o.status === 3) return '已取消'
      if (o.auditStatus === 0) return o.reviewingAdminId ? '审核中' : '待审核'
      if (o.auditStatus === 3) return '再审核'
      if (o.auditStatus === 2) return '已拒绝'
      if (o.status === 0 && o.acceptedCount < o.peopleCount) return '待组队'
      return ['待接单', '进行中', '已完成', '已取消', '待验收'][o.status] || ''
    },
    statusClass(o) {
      if (o.status === 5 || o.status === 3) return 'cancelled'
      if (o.auditStatus === 0 || o.auditStatus === 3) return 'audit'
      if (o.auditStatus === 2) return 'reject'
      if (o.status === 0 && o.acceptedCount < o.peopleCount) return 'verify'
      return ['pending', 'ongoing', 'done', 'cancelled', 'verify'][o.status] || ''
    },
    openEditProfile() { this.editProfile = { ...this.userInfo }; this.showProfileModal = true },
    onEditGenderChange(e) { this.editProfile.gender = Number(e.detail.value) },
    async saveProfile() {
      await request({ url: '/user/update', method: 'PUT', data: this.editProfile, header: { 'Content-Type': 'application/json' } })
      Object.assign(this.userInfo, this.editProfile)
      setUser(this.userInfo)
      uni.showToast({ title: '已保存' })
      this.showProfileModal = false
    },
    startPolling() {
      if (this.timer) clearInterval(this.timer)
      this.timer = setInterval(async () => {
        const user = getUser()
        if (!user || user.role !== 4) return
        try {
          const msgs = await request({ url: `/order/alert/unread?guardianId=${user.id}` })
          if (msgs && msgs.length > 0) {
            const m = msgs[0]
            let elderName = `ID:${m.elderId}`
            try {
              const elder = await request({ url: `/user/getInfo/${m.elderId}` })
              if (elder && elder.realName) elderName = elder.realName
            } catch (e) {}
            uni.showModal({
              title: '⚠️ 紧急求助',
              content: `您的家人「${elderName}」发来求助：${m.content}`,
              showCancel: false,
              confirmText: '我知道了',
              success: async () => {
                await request({ url: `/order/alert/read/${m.id}`, method: 'PUT' })
              }
            })
          }
        } catch (e) {}
      }, 3000)
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
.container { padding: 20rpx; }
.profile-card { display: flex; align-items: center; background: linear-gradient(135deg, #4A90D9, #3b7bd9); border-radius: 24rpx; padding: 40rpx; margin-bottom: 24rpx; color: #fff; }
.avatar-circle { width: 120rpx; height: 120rpx; border-radius: 50%; background: rgba(255,255,255,0.25); display: flex; align-items: center; justify-content: center; font-size: 52rpx; font-weight: bold; margin-right: 30rpx; }
.profile-info .name { font-size: 40rpx; font-weight: 700; }
.profile-info .role-tag { display: inline-block; background: rgba(255,255,255,0.25); padding: 4rpx 18rpx; border-radius: 30rpx; font-size: 24rpx; margin: 10rpx 0; }
.profile-info .uid { font-size: 24rpx; opacity: 0.9; }
.tabs { display: flex; background: #fff; border-radius: 50rpx; padding: 6rpx; margin-bottom: 24rpx; }
.tab { flex: 1; text-align: center; padding: 20rpx 0; border-radius: 50rpx; font-size: 28rpx; color: #6a7a8e; }
.tab.active { background: #4A90D9; color: #fff; font-weight: 600; }
.order-tabs-scroll { white-space: nowrap; margin-bottom: 20rpx; }
.order-tabs { display: inline-flex; gap: 12rpx; padding: 6rpx 0; }
.order-tab { display: inline-block; padding: 12rpx 26rpx; border-radius: 30rpx; background: #fff; font-size: 24rpx; color: #6a7a8e; }
.order-tab.active { background: #4A90D9; color: #fff; font-weight: 600; }
.item { display: flex; justify-content: space-between; align-items: center; padding: 24rpx; background: #fff; border-radius: 16rpx; margin-bottom: 16rpx; }
.item-info .name-text { font-size: 32rpx; font-weight: 500; color: #1a2332; }
.item-info .desc-text { display: block; font-size: 24rpx; color: #8A9AAE; margin-top: 8rpx; }
.remove-btn { color: #ff4d4f; font-size: 26rpx; padding: 10rpx 20rpx; border: 1px solid #ff4d4f; border-radius: 30rpx; }
.empty-tip { text-align: center; color: #999; margin: 40rpx 0; font-size: 26rpx; }
.btn-add { margin-top: 20rpx; background: #4A90D9; color: #fff; border-radius: 50rpx; padding: 24rpx; font-size: 30rpx; border: none; }
.btn-primary { width: 100%; background: #4A90D9; color: #fff; border: none; border-radius: 50rpx; padding: 22rpx; font-size: 30rpx; margin-top: 20rpx; }
.btn-cancel { width: 100%; background: #f0f4f8; color: #666; border: none; border-radius: 50rpx; padding: 22rpx; font-size: 30rpx; margin-top: 16rpx; }
.btn-logout { width: 100%; background: #f0f4f8; color: #4a5a6e; border: none; border-radius: 50rpx; padding: 22rpx; font-size: 30rpx; margin-bottom: 20rpx; }
.btn-delete { width: 100%; background: #fff; color: #ff4d4f; border: 1px solid #ff4d4f; border-radius: 50rpx; padding: 22rpx; font-size: 30rpx; }
.card { background: #fff; border-radius: 20rpx; padding: 24rpx; margin-bottom: 24rpx; }
.section-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16rpx; }
.section-title { font-size: 30rpx; font-weight: 600; color: #2d3a4a; margin-bottom: 16rpx; }
.edit-btn { font-size: 26rpx; color: #4A90D9; padding: 6rpx 20rpx; border: 1px solid #4A90D9; border-radius: 30rpx; }
.info-item { display: flex; justify-content: space-between; padding: 16rpx 0; font-size: 28rpx; color: #4a5a6e; border-bottom: 1rpx solid #f0f4f8; }
.info-item:last-child { border-bottom: none; }
.info-item text:first-child { color: #8A9AAE; }
.form-row { display: flex; align-items: center; min-height: 90rpx; border-bottom: 1rpx solid #f0f4f8; padding: 10rpx 0; }
.form-row:last-child { border-bottom: none; }
.form-row .label { width: 180rpx; flex-shrink: 0; font-size: 28rpx; color: #8A9AAE; }
.form-row input { flex: 1; height: 90rpx; line-height: 90rpx; font-size: 28rpx; color: #1a2332; background: transparent; border: none; }
.form-row picker { flex: 1; }
.picker-value { font-size: 28rpx; color: #1a2332; height: 90rpx; line-height: 90rpx; }
.textarea-row { align-items: flex-start; padding-top: 20rpx; }
.textarea-input { flex: 1; height: 160rpx; font-size: 28rpx; background: #f7f9fc; border-radius: 12rpx; padding: 16rpx; box-sizing: border-box; }
.radio-inline { display: flex; gap: 20rpx; font-size: 26rpx; color: #4a5a6e; }
.order-line { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12rpx; }
.order-title { font-size: 30rpx; font-weight: 500; }
.status-tag { padding: 6rpx 18rpx; border-radius: 30rpx; font-size: 24rpx; color: #fff; }
.status-tag.pending { background: #faad14; }
.status-tag.ongoing { background: #4f9ef7; }
.status-tag.done { background: #52c41a; }
.status-tag.cancelled { background: #999; }
.status-tag.verify { background: #faad14; }
.status-tag.audit { background: #4A90D9; }
.status-tag.reject { background: #ff4d4f; }
.order-desc { display: block; font-size: 26rpx; color: #6a7a8e; margin-top: 6rpx; }
.modal-mask { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 999; }
.modal-box { width: 640rpx; max-height: 80vh; overflow-y: auto; background: #fff; border-radius: 24rpx; padding: 40rpx; box-sizing: border-box; }
.modal-title { font-size: 34rpx; font-weight: 700; text-align: center; margin-bottom: 30rpx; }
</style>