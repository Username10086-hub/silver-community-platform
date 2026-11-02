<template>
  <view class="page-container">
    <view class="tab-header">
      <view class="tab-item" :class="{active:currentTab===0}" @click="currentTab=0">老人档案</view>
      <view class="tab-item" :class="{active:currentTab===1}" @click="currentTab=1">健康管理</view>
      <view class="tab-item" :class="{active:currentTab===2}" @click="currentTab=2">工单记录</view>
    </view>

    <view v-show="currentTab===0">
      <view class="card">
        <text class="title">📋 老人档案</text>
        <view class="info-item"><text>姓名</text><text>{{ elderInfo.realName || '--' }}</text></view>
        <view class="info-item"><text>年龄</text><text>{{ elderInfo.age || '--' }}</text></view>
        <view class="info-item"><text>性别</text><text>{{ elderInfo.gender === 1 ? '男' : (elderInfo.gender === 2 ? '女' : '--') }}</text></view>
        <view class="info-item"><text>电话</text><text>{{ elderInfo.phone || '--' }}</text></view>
        <view class="info-item"><text>住址</text><text>{{ elderInfo.address || '--' }}</text></view>
      </view>
      <view class="card" v-if="elderArchive">
        <view class="section-title">📋 养老基础档案</view>
        <view class="info-item"><text>居住状态</text><text>{{ livingMap[elderArchive.livingStatus] || '--' }}</text></view>
        <view class="info-item"><text>行动能力</text><text>{{ mobilityMap[elderArchive.mobility] || '--' }}</text></view>
        <view class="info-item"><text>慢性病史</text><text>{{ elderArchive.chronicDisease || '无' }}</text></view>
        <view class="info-item"><text>饮食禁忌</text><text>{{ elderArchive.dietaryRestriction || '无' }}</text></view>
        <view class="info-item"><text>紧急联系人</text><text>{{ elderArchive.emergencyContact || '--' }}</text></view>
      </view>
    </view>

    <view v-show="currentTab===1">
      <view class="card" v-for="h in healthList" :key="h.id">
        <text>血压 {{ h.bloodPressure || '--' }} | 血糖 {{ h.bloodSugar || '--' }} | 心率 {{ h.heartRate || '--' }}</text>
        <text class="time">{{ h.recordTime }}</text>
      </view>
      <view v-if="healthList.length < healthTotal" class="more-btn" @click="loadMoreHealth">查看更多 ></view>
      <view v-if="healthList.length===0" class="empty-tip">暂无健康记录</view>
    </view>

    <view v-show="currentTab===2">
      <!-- 状态分类 -->
      <scroll-view scroll-x class="order-tabs-scroll" show-scrollbar="false">
        <view class="order-tabs">
          <view class="order-tab" :class="{active: statusTab==='all'}" @click="statusTab='all'">全部</view>
          <view class="order-tab" :class="{active: statusTab==='audit'}" @click="statusTab='audit'">待审核</view>
          <view class="order-tab" :class="{active: statusTab==='waitTeam'}" @click="statusTab='waitTeam'">待组队</view>
          <view class="order-tab" :class="{active: statusTab==='ongoing'}" @click="statusTab='ongoing'">进行中</view>
          <view class="order-tab" :class="{active: statusTab==='verify'}" @click="statusTab='verify'">待验收</view>
          <view class="order-tab" :class="{active: statusTab==='done'}" @click="statusTab='done'">已完成</view>
          <view class="order-tab" :class="{active: statusTab==='cancel'}" @click="statusTab='cancel'">已取消</view>
          <view class="order-tab" :class="{active: statusTab==='reject'}" @click="statusTab='reject'">已拒绝</view>
        </view>
      </scroll-view>

      <view v-for="o in filteredOrders" :key="o.id" class="card">
        <view class="order-line">
          <text class="order-title">{{ ['陪护','助老帮扶','社区帮扶'][o.serviceType-1] }}</text>
          <text class="status-tag" :class="statusClass(o)">{{ statusText(o) }}</text>
        </view>
        <view class="desc">📍 {{ o.address || '未填' }}</view>
        <view class="desc" v-if="o.appointmentTime">⏰ {{ o.appointmentTime }}</view>
        <view class="desc">📝 {{ o.remark || '无' }}</view>
      </view>
      <view v-if="filteredOrders.length === 0" class="empty">暂无工单记录</view>
    </view>
  </view>
</template>

<script>
import { request } from '@/utils/request.js'
export default {
  data() {
    return {
      currentTab: 0,
      elderId: '',
      elderInfo: {},
      elderArchive: null,
      healthList: [],
      healthLimit: 3,
      healthTotal: 0,
      orderList: [],
      statusTab: 'all',
      livingMap: { 1: '独居', 2: '与子女同住', 3: '夫妻同住' },
      mobilityMap: { 1: '自理', 2: '半自理', 3: '失能' }
    }
  },
  computed: {
    filteredOrders() {
      const t = this.statusTab
      if (t === 'all') return this.orderList
      if (t === 'audit') return this.orderList.filter(o => o.auditStatus === 0 || o.auditStatus === 3)
      if (t === 'waitTeam') return this.orderList.filter(o => o.auditStatus === 1 && o.status === 0 && o.acceptedCount < o.peopleCount)
      if (t === 'ongoing') return this.orderList.filter(o => o.auditStatus === 1 && o.status === 1)
      if (t === 'verify') return this.orderList.filter(o => o.auditStatus === 1 && o.status === 4)
      if (t === 'done') return this.orderList.filter(o => o.auditStatus === 1 && o.status === 2)
      if (t === 'cancel') return this.orderList.filter(o => o.status === 3)
      if (t === 'reject') return this.orderList.filter(o => o.auditStatus === 2)
      return this.orderList
    }
  },
  onLoad(options) {
    this.elderId = options.id
    this.loadData()
  },
  methods: {
    async loadData() {
      try {
        const r = await request({ url: `/user/getInfo/${this.elderId}` })
        this.elderInfo = r || {}
      } catch (e) {
        try { this.elderInfo = await request({ url: `/user/info/${this.elderId}` }) || {} }
        catch (e2) { this.elderInfo = {} }
      }
      try { this.elderArchive = await request({ url: `/user/elderInfo/${this.elderId}` }) }
      catch (e) { this.elderArchive = null }
      this.loadHealth()
      try {
        const orders = await request({ url: `/order/list?elderId=${this.elderId}` })
        this.orderList = orders.map(i => i.order)
      } catch (e) { this.orderList = [] }
    },
    async loadHealth() {
      try {
        const list = await request({ url: `/user/health/list?elderId=${this.elderId}&limit=${this.healthLimit}` })
        this.healthList = list || []
        const all = await request({ url: `/user/health/list?elderId=${this.elderId}` })
        this.healthTotal = (all || []).length
      } catch (e) { this.healthList = [] }
    },
    loadMoreHealth() { this.healthLimit += 3; this.loadHealth() },
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
    }
  }
}
</script>

<style scoped>
.page-container { padding: 20rpx; }
.tab-header { display: flex; background: #fff; border-radius: 12rpx; margin-bottom: 20rpx; }
.tab-item { flex:1; text-align:center; padding:20rpx; font-size:28rpx; }
.tab-item.active { color:#007aff; font-weight:bold; border-bottom: 4rpx solid #007aff; }
.order-tabs-scroll { white-space: nowrap; margin-bottom: 20rpx; }
.order-tabs { display: inline-flex; gap: 12rpx; padding: 6rpx 0; }
.order-tab { display: inline-block; padding: 12rpx 26rpx; border-radius: 30rpx; background: #fff; font-size: 24rpx; color: #6a7a8e; }
.order-tab.active { background: #4A90D9; color: #fff; font-weight: 600; }
.card { background: #fff; border-radius: 20rpx; padding: 24rpx; margin-bottom: 20rpx; }
.info-item { display: flex; justify-content: space-between; padding: 12rpx 0; font-size: 28rpx; color: #4a5a6e; }
.info-item text:first-child { color: #8A9AAE; }
.section-title { font-size: 30rpx; font-weight: 600; color: #2d3a4a; margin: 20rpx 0; }
.time { display: block; font-size: 22rpx; color: #999; margin-top: 10rpx; }
.desc { display: block; font-size: 26rpx; color: #666; margin-top: 10rpx; }
.empty, .empty-tip { text-align: center; color: #999; padding: 30rpx; font-size: 26rpx; }
.title { font-size: 34rpx; font-weight: bold; }
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
.more-btn { text-align: center; color: #4A90D9; font-size: 28rpx; padding: 20rpx; }
</style>