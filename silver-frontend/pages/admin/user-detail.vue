<template>
  <view class="page-container" v-if="user">
    <!-- 基本信息 -->
    <view class="card">
      <view class="section-title">👤 基本信息</view>
      <view class="info-item"><text>ID</text><text>{{ user.id }}</text></view>
      <view class="info-item"><text>用户名</text><text>{{ user.username }}</text></view>
      <view class="info-item"><text>姓名</text><text>{{ user.realName }}</text></view>
      <view class="info-item"><text>角色</text><text>{{ roleMap[user.role] }}</text></view>
      <view class="info-item"><text>年龄</text><text>{{ user.age || '--' }}</text></view>
      <view class="info-item"><text>性别</text><text>{{ user.gender === 1 ? '男' : (user.gender === 2 ? '女' : '--') }}</text></view>
      <view class="info-item"><text>手机</text><text>{{ user.phone || '--' }}</text></view>
      <view class="info-item"><text>状态</text><text>{{ user.status === 0 ? '已禁用' : '正常' }}</text></view>
    </view>

    <!-- 老人专属：养老信息 -->
    <view class="card" v-if="user.role === 1 && elderInfo">
      <view class="section-title">📋 养老档案</view>
      <view class="info-item"><text>居住状态</text><text>{{ livingMap[elderInfo.livingStatus] || '--' }}</text></view>
      <view class="info-item"><text>行动能力</text><text>{{ mobilityMap[elderInfo.mobility] || '--' }}</text></view>
      <view class="info-item"><text>慢性病史</text><text>{{ elderInfo.chronicDisease || '无' }}</text></view>
      <view class="info-item"><text>饮食禁忌</text><text>{{ elderInfo.dietaryRestriction || '无' }}</text></view>
      <view class="info-item"><text>紧急联系人</text><text>{{ elderInfo.emergencyContact || '--' }}</text></view>
    </view>

    <!-- 工单统计 -->
    <view class="card">
      <view class="section-title">📊 工单总计</view>
      <view class="stats-grid">
        <view class="stat-item" @click="openCategoryList('all')">
          <text class="stat-num">{{ stats.total || 0 }}</text>
          <text class="stat-label">全部</text>
        </view>
        <template v-if="user.role === 1 || user.role === 4">
          <view class="stat-item" @click="openCategoryList('audit')">
            <text class="stat-num" style="color:#4A90D9">{{ stats.audit || 0 }}</text>
            <text class="stat-label">待审核</text>
          </view>
          <view class="stat-item" @click="openCategoryList('pending')">
            <text class="stat-num" style="color:#faad14">{{ stats.pending || 0 }}</text>
            <text class="stat-label">待接单</text>
          </view>
        </template>
        <view class="stat-item" @click="openCategoryList('ongoing')">
          <text class="stat-num" style="color:#4A90D9">{{ stats.ongoing || 0 }}</text>
          <text class="stat-label">进行中</text>
        </view>
        <view class="stat-item" @click="openCategoryList('verify')">
          <text class="stat-num" style="color:#faad14">{{ stats.verify || 0 }}</text>
          <text class="stat-label">待验收</text>
        </view>
        <view class="stat-item" @click="openCategoryList('done')">
          <text class="stat-num" style="color:#52c41a">{{ stats.done || 0 }}</text>
          <text class="stat-label">已完成</text>
        </view>
        <view class="stat-item" @click="openCategoryList('cancel')">
          <text class="stat-num" style="color:#999">{{ stats.cancel || 0 }}</text>
          <text class="stat-label">已取消</text>
        </view>
        <template v-if="user.role === 1 || user.role === 4">
          <view class="stat-item" @click="openCategoryList('reject')">
            <text class="stat-num" style="color:#ff4d4f">{{ stats.reject || 0 }}</text>
            <text class="stat-label">已拒绝</text>
          </view>
        </template>
      </view>

      <view class="divider"></view>

      <view class="section-title" style="margin-top:20rpx;">📋 全部工单</view>
      <view class="filter-bar" style="margin-bottom:20rpx;">
        <view class="filter-tab small" :class="{active: inlineCategory==='all'}" @click="switchInline('all')">全部</view>
        <template v-if="user.role === 1 || user.role === 4">
          <view class="filter-tab small" :class="{active: inlineCategory==='audit'}" @click="switchInline('audit')">待审核</view>
          <view class="filter-tab small" :class="{active: inlineCategory==='pending'}" @click="switchInline('pending')">待接单</view>
        </template>
        <view class="filter-tab small" :class="{active: inlineCategory==='ongoing'}" @click="switchInline('ongoing')">进行中</view>
        <view class="filter-tab small" :class="{active: inlineCategory==='verify'}" @click="switchInline('verify')">待验收</view>
        <view class="filter-tab small" :class="{active: inlineCategory==='done'}" @click="switchInline('done')">已完成</view>
        <view class="filter-tab small" :class="{active: inlineCategory==='cancel'}" @click="switchInline('cancel')">已取消</view>
        <template v-if="user.role === 1 || user.role === 4">
          <view class="filter-tab small" :class="{active: inlineCategory==='reject'}" @click="switchInline('reject')">已拒绝</view>
        </template>
      </view>

      <view v-if="inlineOrders.length===0" class="empty">暂无工单</view>
      <view v-for="item in inlineOrders" :key="item.order.id" class="inline-order" @click="goOrderDetail(item.order.id)">
        <view class="inline-row">
          <text class="inline-name">{{ ['陪护','助老帮扶','社区帮扶'][item.order.serviceType-1] }}</text>
          <text class="inline-status" :style="{color: inlineStatusColor(item.order)}">{{ orderStatusText(item.order) }}</text>
        </view>
        <view class="inline-desc">📍 {{ item.order.address || '未填' }} · 👥 {{ item.order.acceptedCount }}/{{ item.order.peopleCount }}</view>
        <view class="inline-desc" v-if="item.order.contactPhone">📞 {{ item.order.contactPhone }}</view>
        <view class="inline-desc" v-if="item.order.appointmentTime">⏰ {{ item.order.appointmentTime }}</view>
        <view class="inline-desc">📝 {{ item.order.remark || '无' }}</view>
        <view v-if="item.order.auditStatus === 2 && item.order.rejectReason" class="inline-reject">
          拒绝理由：{{ item.order.rejectReason }}
        </view>
      </view>
    </view>

    <!-- 老人：绑定的子女 -->
    <view class="card" v-if="user.role === 1 && guardians.length > 0">
      <view class="section-title">👨‍👧 绑定的监护人</view>
      <view v-for="g in guardians" :key="g.id" class="item-line">
        <view>
          <view class="name">{{ g.realName }}</view>
          <view class="desc">ID：{{ g.id }} | {{ g.phone }}</view>
        </view>
      </view>
    </view>

    <!-- 子女：绑定的老人 -->
    <view class="card" v-if="user.role === 4 && elders.length > 0">
      <view class="section-title">👴 绑定的老人</view>
      <view v-for="e in elders" :key="e.id" class="item-line">
        <view>
          <view class="name">{{ e.realName }}</view>
          <view class="desc">ID：{{ e.id }} | {{ e.phone }}</view>
        </view>
      </view>
    </view>

    <!-- 分类工单弹窗 -->
    <view v-if="showCategoryList" class="modal-mask" @click="showCategoryList=false">
      <view class="modal-box" @click.stop>
        <view class="modal-title">{{ categoryTitle }}</view>
        <scroll-view scroll-y class="category-scroll">
          <view v-if="categoryOrders.length===0" class="empty">暂无工单</view>
          <view v-for="item in categoryOrders" :key="item.order.id" class="cat-order" @click="goOrderDetail(item.order.id)">
            <view class="cat-row">
              <text class="cat-name">{{ ['陪护','助老帮扶','社区帮扶'][item.order.serviceType-1] }}</text>
              <text class="cat-status">{{ orderStatusText(item.order) }}</text>
            </view>
            <view class="cat-desc">📍 {{ item.order.address || '未填' }} · 👥 {{ item.order.acceptedCount }}/{{ item.order.peopleCount }}</view>
            <view class="cat-desc">📝 {{ item.order.remark || '无' }}</view>
            <view v-if="item.order.auditStatus === 2 && item.order.rejectReason" class="cat-reject">
              拒绝理由：{{ item.order.rejectReason }}
            </view>
          </view>
        </scroll-view>
        <button class="btn-cancel" @click="showCategoryList=false">关闭</button>
      </view>
    </view>

    <!-- 操作按钮 -->
    <view class="btn-wrap">
      <button class="btn-warn" @click="toggleUser">{{ user.status === 0 ? '启用该账号' : '禁用该账号' }}</button>
      <button class="btn-danger" @click="deleteUser">注销该账号</button>
    </view>
  </view>
</template>

<script>
import { request } from '@/utils/request.js'
export default {
  data() {
    return {
      userId: '',
      user: null,
      elderInfo: null,
      guardians: [],
      elders: [],
      stats: {},
      inlineCategory: 'all',
      inlineOrders: [],
      showCategoryList: false,
      categoryType: '',
      categoryOrders: [],
      roleMap: { 1: '老人', 2: '志愿者', 3: '管理员', 4: '子女' },
      livingMap: { 1: '独居', 2: '与子女同住', 3: '夫妻同住' },
      mobilityMap: { 1: '自理', 2: '半自理', 3: '失能' }
    }
  },
  computed: {
    categoryTitle() {
      const map = { all: '全部工单', audit: '待审核工单', pending: '待接单工单', ongoing: '进行中工单', verify: '待验收工单', done: '已完成工单', cancel: '已取消工单', reject: '已拒绝工单' }
      return map[this.categoryType] || '工单列表'
    }
  },
  onLoad(options) {
    this.userId = options.id
    this.load()
  },
  methods: {
    async load() {
      const res = await request({ url: `/user/detail/${this.userId}` })
      this.user = res.user
      this.elderInfo = res.elderInfo || null
      this.guardians = res.guardians || []
      this.elders = res.elders || []
      try { this.stats = await request({ url: `/order/stats/user/${this.userId}` }) } catch (e) { this.stats = {} }
      this.loadInlineOrders()
    },
    async loadInlineOrders() {
      try {
        const url = `/order/list/byUser?userId=${this.userId}&role=${this.user.role}&category=${this.inlineCategory}`
        this.inlineOrders = await request({ url })
      } catch (e) { this.inlineOrders = [] }
    },
    switchInline(c) { this.inlineCategory = c; this.loadInlineOrders() },
    async openCategoryList(type) {
      this.categoryType = type
      this.showCategoryList = true
      try {
        const url = `/order/list/byUser?userId=${this.userId}&role=${this.user.role}&category=${type}`
        this.categoryOrders = await request({ url })
      } catch (e) { this.categoryOrders = [] }
    },
    goOrderDetail(id) { uni.navigateTo({ url: `/pages/order/order-detail?id=${id}` }) },
    orderStatusText(o) {
      if (o.status === 3) return '已取消'
      if (o.auditStatus === 0) return '待审核'
      if (o.auditStatus === 2) return '已拒绝'
      return ['待接单', '进行中', '已完成', '已取消', '待验收'][o.status] || ''
    },
    inlineStatusColor(o) {
      if (o.status === 3) return '#999'
      if (o.auditStatus === 0) return '#4A90D9'
      if (o.auditStatus === 2) return '#ff4d4f'
      if (o.status === 2) return '#52c41a'
      if (o.status === 4) return '#faad14'
      return '#4A90D9'
    },
    async toggleUser() {
      await request({ url: `/user/toggle/${this.userId}`, method: 'PUT' })
      uni.showToast({ title: '已切换' })
      this.load()
    },
    deleteUser() {
      uni.showModal({
        title: '⚠️ 危险操作',
        content: '注销后账号及所有数据将被永久删除！',
        confirmColor: '#ff4d4f',
        success: async (res) => {
          if (!res.confirm) return
          await request({ url: `/user/delete/${this.userId}`, method: 'DELETE' })
          uni.showToast({ title: '已注销' })
          setTimeout(() => uni.navigateBack(), 800)
        }
      })
    }
  }
}
</script>

<style scoped>
.page-container { padding: 20rpx; }
.card { background: #fff; border-radius: 20rpx; padding: 28rpx; margin-bottom: 20rpx; }
.section-title { font-size: 32rpx; font-weight: 600; margin-bottom: 16rpx; color: #2d3a4a; }
.info-item { display: flex; justify-content: space-between; padding: 14rpx 0; font-size: 30rpx; color: #4a5a6e; border-bottom: 1rpx solid #f0f4f8; }
.info-item:last-child { border-bottom: none; }
.info-item text:first-child { color: #8A9AAE; }

.stats-grid { display: flex; flex-wrap: wrap; }
.stat-item { width: 33.33%; text-align: center; padding: 20rpx 0; }
.stat-num { display: block; font-size: 44rpx; font-weight: 700; color: #4A90D9; }
.stat-label { font-size: 24rpx; color: #8A9AAE; margin-top: 6rpx; }

.divider { height: 1rpx; background: #f0f4f8; margin: 20rpx 0; }
.filter-bar { display: flex; gap: 10rpx; flex-wrap: wrap; }
.filter-tab { padding: 10rpx 22rpx; border-radius: 30rpx; font-size: 24rpx; background: #f5f7fa; color: #666; }
.filter-tab.small { padding: 8rpx 20rpx; font-size: 22rpx; }
.filter-tab.active { background: #4A90D9; color: #fff; font-weight: 600; }
.inline-order { padding: 20rpx; border: 1rpx solid #eef2f7; border-radius: 12rpx; margin-bottom: 16rpx; }
.inline-row { display: flex; justify-content: space-between; }
.inline-name { font-size: 30rpx; font-weight: 500; }
.inline-status { font-size: 26rpx; font-weight: 500; }
.inline-desc { font-size: 26rpx; color: #6a7a8e; margin-top: 6rpx; }
.inline-reject { font-size: 26rpx; color: #ff4d4f; margin-top: 8rpx; }

.item-line { padding: 16rpx 0; border-bottom: 1rpx solid #f0f4f8; }
.item-line:last-child { border-bottom: none; }
.name { font-size: 32rpx; font-weight: 500; }
.desc { font-size: 26rpx; color: #8A9AAE; margin-top: 6rpx; }

.btn-wrap { margin-top: 30rpx; }
.btn-warn { width: 100%; background: #faad14; color: #fff; border: none; border-radius: 50rpx; padding: 22rpx; font-size: 30rpx; margin-bottom: 20rpx; }
.btn-danger { width: 100%; background: #fff; color: #ff4d4f; border: 1px solid #ff4d4f; border-radius: 50rpx; padding: 22rpx; font-size: 30rpx; }

.modal-mask { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 999; }
.modal-box { width: 680rpx; max-height: 80vh; background: #fff; border-radius: 24rpx; padding: 40rpx; box-sizing: border-box; display: flex; flex-direction: column; }
.modal-title { font-size: 34rpx; font-weight: 700; text-align: center; margin-bottom: 20rpx; }
.category-scroll { max-height: 800rpx; }
.cat-order { padding: 20rpx; border: 1rpx solid #eef2f7; border-radius: 12rpx; margin-bottom: 16rpx; }
.cat-row { display: flex; justify-content: space-between; }
.cat-name { font-size: 30rpx; font-weight: 500; }
.cat-status { font-size: 26rpx; color: #4A90D9; }
.cat-desc { font-size: 26rpx; color: #6a7a8e; margin-top: 6rpx; }
.cat-reject { font-size: 26rpx; color: #ff4d4f; margin-top: 8rpx; }
.btn-cancel { width: 100%; background: #f0f4f8; color: #666; border: none; border-radius: 50rpx; padding: 22rpx; font-size: 30rpx; margin-top: 20rpx; }
.empty { text-align: center; color: #999; padding: 60rpx; }
</style>