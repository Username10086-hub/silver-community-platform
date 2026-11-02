<template>
  <view class="container">
    <view class="page-title">📊 管理员控制台</view>

    <scroll-view scroll-x class="tabs-scroll" show-scrollbar="false">
      <view class="tabs">
        <view class="tab" :class="{active: currentTab===0}" @click="currentTab=0">工单审核</view>
        <view class="tab" :class="{active: currentTab===1}" @click="currentTab=1">全部工单</view>
        <view class="tab" :class="{active: currentTab===2}" @click="currentTab=2">公告管理</view>
        <view class="tab" :class="{active: currentTab===3}" @click="currentTab=3">用户管理</view>
        <view class="tab" :class="{active: currentTab===4}" @click="currentTab=4">紧急求助</view>
        <view v-if="isSuper" class="tab" :class="{active: currentTab===5}" @click="currentTab=5">管理员管理</view>
        <view class="tab" :class="{active: currentTab===6}" @click="currentTab=6">个人中心</view>
      </view>
    </scroll-view>

    <!-- 1. 工单审核 -->
    <view v-show="currentTab===0">
      <view class="filter-bar">
        <view class="filter-tab" :class="{active: auditType===0}" @click="auditType=0">全部</view>
        <view class="filter-tab" :class="{active: auditType===1}" @click="auditType=1">陪护</view>
        <view class="filter-tab" :class="{active: auditType===2}" @click="auditType=2">助老帮扶</view>
        <view class="filter-tab" :class="{active: auditType===3}" @click="auditType=3">社区帮扶</view>
      </view>
      <scroll-view scroll-x class="filter-scroll" show-scrollbar="false">
        <view class="filter-bar">
          <view class="filter-tab small" :class="{active: auditStateFilter==='pending'}" @click="auditStateFilter='pending'">待审核</view>
          <view class="filter-tab small" :class="{active: auditStateFilter==='reviewing'}" @click="auditStateFilter='reviewing'">审核中</view>
          <view class="filter-tab small" :class="{active: auditStateFilter==='reviewed'}" @click="auditStateFilter='reviewed'">已审核</view>
          <view class="filter-tab small" :class="{active: auditStateFilter==='revoke'}" @click="auditStateFilter='revoke'">已撤销</view>
        </view>
      </scroll-view>

      <view v-for="item in filteredPendingAudit" :key="item.order.id" class="card" @click="goOrderDetail(item.order.id)">
        <view class="row">
          <text class="name">{{ ['陪护','助老帮扶','社区帮扶'][item.order.serviceType-1] }} - {{ item.elderName }}</text>
          <text class="status" :style="{color: auditStatusColor(item.order)}">{{ auditStatusText(item.order) }}</text>
        </view>
        <view class="desc big">📍 {{ item.order.address || '未填' }} · 👥 {{ item.order.peopleCount }}人</view>
        <view class="desc big" v-if="item.order.contactPhone">📞 {{ item.order.contactPhone }}</view>
        <view class="desc big" v-if="item.order.appointmentTime">⏰ {{ item.order.appointmentTime }}</view>
        <view class="desc big">📝 {{ item.order.remark || '无' }}</view>
        <view class="desc" style="color:#4A90D9;margin-top:12rpx;">点击查看详情 →</view>
      </view>
      <view v-if="filteredPendingAudit.length===0" class="empty">暂无对应工单</view>
    </view>

    <!-- 2. 全部工单 -->
    <view v-show="currentTab===1">
      <view class="total-card">
        <view class="total-title">📊 工单总计</view>
        <view class="total-grid">
          <view class="total-item" @click="switchAllFilter('all')">
            <text class="total-num">{{ allStats.total || 0 }}</text>
            <text class="total-label">全部</text>
          </view>
          <view class="total-item" @click="switchAllFilter('audit')">
            <text class="total-num" style="color:#4A90D9">{{ allStats.audit || 0 }}</text>
            <text class="total-label">待审核</text>
          </view>
          <view class="total-item" @click="switchAllFilter('reviewing')">
            <text class="total-num" style="color:#faad14">{{ allStats.reviewing || 0 }}</text>
            <text class="total-label">审核中</text>
          </view>
          <view class="total-item" @click="switchAllFilter('pending')">
            <text class="total-num" style="color:#faad14">{{ allStats.pending || 0 }}</text>
            <text class="total-label">待接单</text>
          </view>
          <view class="total-item" @click="switchAllFilter('ongoing')">
            <text class="total-num" style="color:#4A90D9">{{ allStats.ongoing || 0 }}</text>
            <text class="total-label">进行中</text>
          </view>
          <view class="total-item" @click="switchAllFilter('verify')">
            <text class="total-num" style="color:#faad14">{{ allStats.verify || 0 }}</text>
            <text class="total-label">待验收</text>
          </view>
          <view class="total-item" @click="switchAllFilter('done')">
            <text class="total-num" style="color:#52c41a">{{ allStats.done || 0 }}</text>
            <text class="total-label">已完成</text>
          </view>
          <view class="total-item" @click="switchAllFilter('reject')">
            <text class="total-num" style="color:#ff4d4f">{{ allStats.reject || 0 }}</text>
            <text class="total-label">已拒绝</text>
          </view>
          <view class="total-item" @click="switchAllFilter('cancel')">
            <text class="total-num" style="color:#999">{{ allStats.cancel || 0 }}</text>
            <text class="total-label">已取消</text>
          </view>
          <view class="total-item" @click="switchAllFilter('revoke')">
            <text class="total-num" style="color:#666">{{ allStats.revoke || 0 }}</text>
            <text class="total-label">已撤销</text>
          </view>
        </view>
      </view>

      <view class="filter-bar">
        <view class="filter-tab" :class="{active: orderType===0}" @click="orderType=0">全部</view>
        <view class="filter-tab" :class="{active: orderType===1}" @click="orderType=1">陪护</view>
        <view class="filter-tab" :class="{active: orderType===2}" @click="orderType=2">助老帮扶</view>
        <view class="filter-tab" :class="{active: orderType===3}" @click="orderType=3">社区帮扶</view>
      </view>

      <scroll-view scroll-x class="filter-scroll" show-scrollbar="false">
        <view class="filter-bar">
          <view class="filter-tab small" :class="{active: orderStatus==='all'}" @click="orderStatus='all'">全部状态</view>
          <view class="filter-tab small" :class="{active: orderStatus==='audit'}" @click="orderStatus='audit'">待审核</view>
          <view class="filter-tab small" :class="{active: orderStatus==='reviewing'}" @click="orderStatus='reviewing'">审核中</view>
          <view class="filter-tab small" :class="{active: orderStatus==='pending'}" @click="orderStatus='pending'">待接单</view>
          <view class="filter-tab small" :class="{active: orderStatus==='ongoing'}" @click="orderStatus='ongoing'">进行中</view>
          <view class="filter-tab small" :class="{active: orderStatus==='verify'}" @click="orderStatus='verify'">待验收</view>
          <view class="filter-tab small" :class="{active: orderStatus==='done'}" @click="orderStatus='done'">已完成</view>
          <view class="filter-tab small" :class="{active: orderStatus==='reject'}" @click="orderStatus='reject'">已拒绝</view>
          <view class="filter-tab small" :class="{active: orderStatus==='cancel'}" @click="orderStatus='cancel'">已取消</view>
          <view class="filter-tab small" :class="{active: orderStatus==='revoke'}" @click="orderStatus='revoke'">已撤销</view>
        </view>
      </scroll-view>

      <view v-for="item in filteredAllOrders" :key="item.order.id" class="card order-card" @click="goOrderDetail(item.order.id)">
        <view class="row">
          <text class="name">{{ ['陪护','助老帮扶','社区帮扶'][item.order.serviceType-1] }} - {{ item.elderName }}</text>
          <text class="status" :style="{color: statusColor(item.order)}">{{ orderStatusText(item.order) }}</text>
        </view>
        <view class="desc big">📍 {{ item.order.address || '未填' }} · 👥 {{ item.order.acceptedCount }}/{{ item.order.peopleCount }}</view>
        <view class="desc big" v-if="item.order.appointmentTime">⏰ {{ item.order.appointmentTime }}</view>
        <view class="desc big">审核：{{ auditStateText(item.order) }}</view>
        <view v-if="item.order.reviewerId" class="desc big">处理管理员ID：{{ item.order.reviewerId }}</view>
      </view>
      <view v-if="filteredAllOrders.length===0" class="empty">暂无工单</view>
    </view>

    <!-- 3. 公告管理 -->
    <view v-show="currentTab===2">
      <view class="filter-bar" style="flex-wrap:nowrap;">
        <view class="filter-tab small" :class="{active: newsFilter===0}" @click="newsFilter=0">全部</view>
        <view class="filter-tab small" :class="{active: newsFilter===1}" @click="newsFilter=1">科普</view>
        <view class="filter-tab small" :class="{active: newsFilter===2}" @click="newsFilter=2">通知</view>
        <view class="filter-tab small" :class="{active: newsFilter===3}" @click="newsFilter=3">公益</view>
        <view class="filter-tab small add-tab" @click="openNewsForm()">+ 新增</view>
      </view>
      <view v-for="n in filteredNews" :key="n.id" class="card news-card">
        <view class="name news-title">{{ n.title }}</view>
        <view class="desc">{{ ['','科普','通知','公益'][n.type] }} · {{ n.createTime }}</view>
        <view class="news-content">{{ n.content }}</view>
        <view class="actions">
          <button class="btn-mini btn-edit" @click="openNewsForm(n)">编辑</button>
          <button class="btn-mini btn-reject" @click="deleteNews(n.id)">删除</button>
        </view>
      </view>
    </view>

    <!-- 4. 用户管理（不含管理员） -->
    <view v-show="currentTab===3">
      <view class="filter-bar">
        <view class="filter-tab small" :class="{active: userFilter===0}" @click="filterUser(0)">全部</view>
        <view class="filter-tab small" :class="{active: userFilter===1}" @click="filterUser(1)">老人</view>
        <view class="filter-tab small" :class="{active: userFilter===2}" @click="filterUser(2)">志愿者</view>
        <view class="filter-tab small" :class="{active: userFilter===4}" @click="filterUser(4)">子女</view>
      </view>
      <view v-for="u in users" :key="u.id" class="card user-card" @click="goUserDetail(u.id)">
        <view class="user-head">
          <text class="name">{{ u.realName }}（{{ u.username }}）</text>
          <text class="status-tag" :class="{disabled: u.status === 0}">{{ u.status === 0 ? '已禁用' : '正常' }}</text>
        </view>
        <view class="desc">ID: {{ u.id }} · {{ roleMap[u.role] }} · {{ u.phone || '未填' }}</view>
        <view class="user-actions">
          <button class="btn-mini" :class="u.status === 0 ? 'btn-pass' : 'btn-warn'" @click.stop="toggleUser(u)">{{ u.status === 0 ? '启用' : '禁用' }}</button>
        </view>
      </view>
    </view>

    <!-- 5. 紧急求助 -->
    <view v-show="currentTab===4">
      <view v-for="(item, i) in alerts" :key="i" class="card">
        <view class="row">
          <text class="name">⚠️ {{ item.elderName }} 的求助</text>
          <text class="status-tag" :class="{disabled: item.alert.isRead === 1}">{{ item.alert.isRead === 1 ? '已读' : '未读' }}</text>
        </view>
        <view class="desc big">👴 老人电话：{{ item.elderPhone }}</view>
        <view class="desc big">👨‍👧 接收监护人：{{ item.guardianName }}（{{ item.guardianPhone }}）</view>
        <view class="desc big">📝 {{ item.alert.content }}</view>
        <view class="desc">⏰ {{ item.alert.createTime }}</view>
      </view>
      <view v-if="alerts.length===0" class="empty">暂无告警记录</view>
    </view>

    <!-- 6. 管理员管理 -->
    <view v-show="currentTab===5">
      <view class="filter-bar" style="justify-content:flex-end;">
        <view class="filter-tab small add-tab" @click="openAdminForm()">+ 新增管理员</view>
      </view>
      <view v-for="a in adminList" :key="a.id" class="card user-card" @click="viewAdminDetail(a)">
        <view class="user-head">
          <text class="name">{{ a.realName }}（{{ a.username }}）</text>
          <text class="status-tag" :class="{disabled: a.status === 0}">{{ a.status === 0 ? '已禁用' : '正常' }}</text>
        </view>
        <view class="desc">ID: {{ a.id }} · {{ a.phone || '未填' }} · 点击查看审核记录</view>
        <view class="user-actions">
          <button class="btn-mini" :class="a.status === 0 ? 'btn-pass' : 'btn-warn'" @click.stop="toggleAdmin(a)">{{ a.status === 0 ? '启用' : '禁用' }}</button>
          <button class="btn-mini btn-reject" @click.stop="delAdmin(a)">删除</button>
        </view>
      </view>
      <view v-if="adminList.length===0" class="empty">暂无其他管理员</view>
    </view>

    <!-- 7. 个人中心 -->
    <view v-show="currentTab===6">
      <view class="profile-card">
        <view class="avatar-circle">{{ avatarLetter }}</view>
        <view class="profile-info">
          <view class="name">{{ userInfo.realName }}</view>
          <view class="role-tag">{{ isSuper ? '超级管理员' : '管理员' }}</view>
          <view class="uid">ID：{{ userInfo.id }}</view>
        </view>
      </view>

      <view class="card">
        <view class="section-head">
          <text class="section-title">👤 基本信息</text>
          <text class="edit-btn" @click="openEditProfile">编辑</text>
        </view>
        <view class="info-item"><text>姓名</text><text>{{ userInfo.realName || '未填' }}</text></view>
        <view class="info-item"><text>用户ID</text><text>{{ userInfo.id }}</text></view>
        <view class="info-item"><text>用户名</text><text>{{ userInfo.username }}</text></view>
        <view class="info-item"><text>年龄</text><text>{{ userInfo.age || '未填' }}</text></view>
        <view class="info-item"><text>性别</text><text>{{ userInfo.gender === 1 ? '男' : (userInfo.gender === 2 ? '女' : '未填') }}</text></view>
        <view class="info-item"><text>手机</text><text>{{ userInfo.phone || '未填' }}</text></view>
        <view class="info-item"><text>角色</text><text>{{ isSuper ? '超级管理员' : '普通管理员' }}</text></view>
      </view>

      <button class="btn-logout" @click="logout">退出登录</button>
      <button class="btn-delete" @click="deleteAccount">注销账号</button>
    </view>

    <!-- 公告表单弹窗 -->
    <view v-if="showNewsForm" class="modal-mask" @click="showNewsForm=false">
      <view class="modal-box" @click.stop>
        <view class="modal-title">{{ newsForm.id ? '编辑' : '新增' }}公告</view>
        <view class="form-row"><text class="label">标题</text><input v-model="newsForm.title" /></view>
        <view class="form-row">
          <text class="label">类型</text>
          <picker mode="selector" :range="newsTypes" @change="e => newsForm.type = Number(e.detail.value) + 1">
            <view>{{ newsTypes[newsForm.type-1] || '请选择' }}</view>
          </picker>
        </view>
        <view class="form-row textarea-row">
          <text class="label">内容</text>
          <textarea v-model="newsForm.content" class="modal-textarea"></textarea>
        </view>
        <button class="btn-primary" @click="saveNews">保存</button>
        <button class="btn-cancel" @click="showNewsForm=false">取消</button>
      </view>
    </view>

    <!-- 新增管理员弹窗 -->
    <view v-if="showAdminForm" class="modal-mask" @click="showAdminForm=false">
      <view class="modal-box" @click.stop>
        <view class="modal-title">新增管理员</view>
        <view class="form-row"><text class="label">用户名</text><input v-model="adminForm.username" /></view>
        <view class="form-row"><text class="label">密码</text><input v-model="adminForm.password" /></view>
        <view class="form-row"><text class="label">姓名</text><input v-model="adminForm.realName" /></view>
        <view class="form-row"><text class="label">手机</text><input v-model="adminForm.phone" /></view>
        <button class="btn-primary" @click="saveAdmin">创建</button>
        <button class="btn-cancel" @click="showAdminForm=false">取消</button>
      </view>
    </view>

    <!-- 编辑个人信息弹窗 -->
    <view v-if="showProfileModal" class="modal-mask" @click="showProfileModal=false">
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
        <button class="btn-cancel" @click="showProfileModal=false">取消</button>
      </view>
    </view>

    <!-- 查看管理员审核记录 -->
    <view v-if="showAdminDetail" class="modal-mask" @click="showAdminDetail=false">
      <view class="modal-box" @click.stop>
        <view class="modal-title">{{ currentAdmin.realName }} 的审核记录</view>
        <view class="section-title" style="margin-top:20rpx;">审核中</view>
        <view v-if="reviewingOrders.length===0" class="empty">暂无</view>
        <view v-for="o in reviewingOrders" :key="o.id" class="cat-order">
          <text>{{ ['陪护','助老帮扶','社区帮扶'][o.serviceType-1] }} · {{ o.address || '未填' }}</text>
        </view>
        <view class="section-title" style="margin-top:20rpx;">已审核</view>
        <view v-if="reviewedOrders.length===0" class="empty">暂无</view>
        <view v-for="o in reviewedOrders" :key="o.id" class="cat-order">
          <text>{{ ['陪护','助老帮扶','社区帮扶'][o.serviceType-1] }} · {{ auditStateText(o) }}</text>
        </view>
        <button class="btn-cancel" @click="showAdminDetail=false">关闭</button>
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
      pendingAuditOrders: [],
      auditType: 0,
      auditStateFilter: 'pending',
      allOrders: [],
      allStats: {},
      orderType: 0,
      orderStatus: 'all',
      newsList: [],
      newsFilter: 0,
      users: [],
      userFilter: 0,
      alerts: [],
      adminList: [],
      showNewsForm: false,
      newsForm: { id: null, title: '', content: '', type: 1 },
      newsTypes: ['科普', '通知', '公益'],
      showAdminForm: false,
      adminForm: { username: '', password: '', realName: '', phone: '' },
      showAdminDetail: false,
      currentAdmin: {},
      reviewingOrders: [],
      reviewedOrders: [],
      showProfileModal: false,
      editProfile: {},
      roleMap: { 1: '老人', 2: '志愿者', 3: '管理员', 4: '子女', 5: '超级管理员' }
    }
  },
  computed: {
    isSuper() { return this.userInfo.role === 5 },
    avatarLetter() {
      const name = this.userInfo && this.userInfo.realName
      return name ? name.substring(0, 1) : '管'
    },
    filteredPendingAudit() {
      let list = this.pendingAuditOrders
      if (this.auditType !== 0) list = list.filter(i => i.order.serviceType === this.auditType)
      if (this.auditStateFilter === 'pending') {
        list = list.filter(i => !i.order.reviewingAdminId && (i.order.auditStatus === 0 || i.order.auditStatus === 3))
      } else if (this.auditStateFilter === 'reviewing') {
        list = list.filter(i => i.order.reviewingAdminId === this.userInfo.id && i.order.status !== 5)
      } else if (this.auditStateFilter === 'reviewed') {
        list = list.filter(i => i.order.reviewerId === this.userInfo.id && i.order.status !== 5)
      } else if (this.auditStateFilter === 'revoke') {
        list = list.filter(i => i.order.status === 5)
      }
      return list
    },
    filteredAllOrders() {
      return this.allOrders.filter(i => {
        if (this.orderType !== 0 && i.order.serviceType !== this.orderType) return false
        const s = this.orderStatus
        if (s === 'all') return true
        if (s === 'audit') return i.order.auditStatus === 0 && !i.order.reviewingAdminId && i.order.status !== 5
        if (s === 'reviewing') return i.order.auditStatus === 0 && i.order.reviewingAdminId && i.order.status !== 5
        if (s === 'pending') return i.order.auditStatus === 1 && i.order.status === 0
        if (s === 'ongoing') return i.order.auditStatus === 1 && i.order.status === 1
        if (s === 'verify') return i.order.auditStatus === 1 && i.order.status === 4
        if (s === 'done') return i.order.auditStatus === 1 && i.order.status === 2
        if (s === 'reject') return i.order.auditStatus === 2 && i.order.status !== 5
        if (s === 'cancel') return i.order.status === 3
        if (s === 'revoke') return i.order.status === 5
        return true
      })
    },
    filteredNews() {
      if (this.newsFilter === 0) return this.newsList
      return this.newsList.filter(n => n.type === this.newsFilter)
    }
  },
  onShow() {
    this.userInfo = getUser() || {}
    this.reloadAll()
  },
  methods: {
    reloadAll() {
      this.loadPendingAudit()
      this.loadAllOrders()
      this.loadAllStats()
      this.loadNews()
      this.loadUsers()
      this.loadAlerts()
      if (this.isSuper) this.loadAdminList()
    },
    async loadPendingAudit() {
      const p1 = await request({ url: '/order/list?auditStatus=0' })
      const p2 = await request({ url: '/order/list?auditStatus=3' })
      const p3 = await request({ url: '/order/list?auditStatus=1' })
      const p4 = await request({ url: '/order/list?auditStatus=2' })
      this.pendingAuditOrders = p1.concat(p2).concat(p3).concat(p4)
    },
    async loadAllOrders() { this.allOrders = await request({ url: '/order/list' }) },
    async loadAllStats() { try { this.allStats = await request({ url: '/order/stats/all' }) } catch (e) { this.allStats = {} } },
    async loadNews() { this.newsList = await request({ url: '/order/news/list' }) },
    async loadUsers() {
      const url = this.userFilter === 0 ? '/user/list' : `/user/list?role=${this.userFilter}`
      const list = await request({ url })
      this.users = (list || []).filter(u => u.role !== 3 && u.role !== 5)
    },
    async loadAlerts() { this.alerts = await request({ url: '/order/alert/all' }) },
    async loadAdminList() {
      try { this.adminList = await request({ url: `/user/adminList?adminId=${this.userInfo.id}` }) } catch (e) { this.adminList = [] }
    },
    filterUser(role) { this.userFilter = role; this.loadUsers() },
    switchAllFilter(s) {
      if (s === 'all') this.orderStatus = 'all'
      else this.orderStatus = s
      this.orderType = 0
    },
    goOrderDetail(id) { uni.navigateTo({ url: `/pages/order/order-detail?id=${id}` }) },
    auditStatusText(o) {
      if (o.status === 5) return '已撤销'
      if (o.reviewingAdminId) return `审核中(ID:${o.reviewingAdminId})`
      if (o.auditStatus === 3) return '再审核'
      if (o.auditStatus === 1) return '已通过'
      if (o.auditStatus === 2) return '已拒绝'
      return '待审核'
    },
    auditStatusColor(o) {
      if (o.status === 5) return '#666'
      if (o.reviewingAdminId) return '#faad14'
      if (o.auditStatus === 1) return '#52c41a'
      if (o.auditStatus === 2) return '#ff4d4f'
      return '#4A90D9'
    },
    auditStateText(o) {
      if (o.status === 5) return '已撤销'
      if (o.reviewingAdminId) return `审核中(ID:${o.reviewingAdminId})`
      if (o.auditStatus === 0) return '待审核'
      if (o.auditStatus === 1) return '已通过'
      if (o.auditStatus === 2) return '已拒绝'
      if (o.auditStatus === 3) return '再审核'
      return '未知'
    },
    orderStatusText(o) {
      if (o.status === 5) return '已撤销'
      if (o.status === 3) return '已取消'
      if (o.auditStatus === 0) return o.reviewingAdminId ? '审核中' : '待审核'
      if (o.auditStatus === 2) return '已拒绝'
      if (o.auditStatus === 3) return '再审核'
      return ['待接单', '进行中', '已完成', '已取消', '待验收'][o.status] || ''
    },
    statusColor(o) {
      if (o.status === 5) return '#666'
      if (o.status === 3) return '#999'
      if (o.auditStatus === 0) return o.reviewingAdminId ? '#faad14' : '#4A90D9'
      if (o.auditStatus === 2) return '#ff4d4f'
      if (o.status === 2) return '#52c41a'
      if (o.status === 4) return '#faad14'
      return '#4A90D9'
    },
    async toggleUser(u) {
      await request({ url: `/user/toggle/${u.id}`, method: 'PUT' })
      uni.showToast({ title: u.status === 0 ? '已启用' : '已禁用' })
      this.loadUsers()
    },
    goUserDetail(id) { uni.navigateTo({ url: `/pages/admin/user-detail?id=${id}` }) },
    openNewsForm(n) {
      this.newsForm = n ? { ...n } : { id: null, title: '', content: '', type: 1 }
      this.showNewsForm = true
    },
    async saveNews() {
      const url = this.newsForm.id ? '/order/news/update' : '/order/news/add'
      const method = this.newsForm.id ? 'PUT' : 'POST'
      await request({ url, method, data: this.newsForm, header: { 'Content-Type': 'application/json' } })
      uni.showToast({ title: '已保存' })
      this.showNewsForm = false
      this.loadNews()
    },
    deleteNews(id) {
      uni.showModal({
        title: '确认删除？',
        success: async (res) => {
          if (!res.confirm) return
          await request({ url: `/order/news/delete/${id}`, method: 'DELETE' })
          this.loadNews()
        }
      })
    },
    openAdminForm() {
      this.adminForm = { username: '', password: '', realName: '', phone: '' }
      this.showAdminForm = true
    },
    async saveAdmin() {
      const f = this.adminForm
      if (!f.username || !f.password) return uni.showToast({ title: '请填写完整', icon: 'none' })
      await request({
        url: `/user/addAdmin?adminId=${this.userInfo.id}&username=${f.username}&password=${f.password}&realName=${f.realName}&phone=${f.phone}`,
        method: 'POST'
      })
      uni.showToast({ title: '已创建' })
      this.showAdminForm = false
      this.loadAdminList()
    },
    async toggleAdmin(a) {
      await request({ url: `/user/toggleAdmin/${a.id}?adminId=${this.userInfo.id}`, method: 'PUT' })
      this.loadAdminList()
    },
    delAdmin(a) {
      uni.showModal({
        title: '确认删除？',
        success: async (res) => {
          if (!res.confirm) return
          await request({ url: `/user/delAdmin/${a.id}?adminId=${this.userInfo.id}`, method: 'DELETE' })
          this.loadAdminList()
        }
      })
    },
    viewAdminDetail(a) {
      this.currentAdmin = a
      this.showAdminDetail = true
      this.reviewingOrders = this.allOrders
        .filter(i => i.order.reviewingAdminId === a.id && i.order.auditStatus === 0)
        .map(i => i.order)
      this.reviewedOrders = this.allOrders
        .filter(i => i.order.reviewerId === a.id)
        .map(i => i.order)
    },
    // 个人中心
    openEditProfile() {
      this.editProfile = { ...this.userInfo }
      this.showProfileModal = true
    },
    onEditGenderChange(e) {
      this.editProfile.gender = Number(e.detail.value)
    },
    async saveProfile() {
      try {
        await request({
          url: '/user/update',
          method: 'PUT',
          data: this.editProfile,
          header: { 'Content-Type': 'application/json' }
        })
        Object.assign(this.userInfo, this.editProfile)
        setUser(this.userInfo)
        uni.showToast({ title: '已保存' })
        this.showProfileModal = false
      } catch (e) {
        uni.showToast({ title: '保存失败', icon: 'none' })
      }
    },
    logout() {
      uni.showModal({
        title: '提示', content: '确认退出登录吗？',
        success: (res) => {
          if (res.confirm) {
            clearUser()
            uni.reLaunch({ url: '/pages/index/index' })
          }
        }
      })
    },
    deleteAccount() {
      uni.showModal({
        title: '⚠️ 危险操作',
        content: '注销后账号及数据将被永久删除！',
        confirmColor: '#ff4d4f',
        success: async (res) => {
          if (!res.confirm) return
          try {
            await request({ url: `/user/delete/${this.userInfo.id}`, method: 'DELETE' })
            clearUser()
            uni.showToast({ title: '已注销' })
            setTimeout(() => uni.reLaunch({ url: '/pages/index/index' }), 800)
          } catch (e) {
            uni.showToast({ title: e.msg || '注销失败', icon: 'none' })
          }
        }
      })
    }
  }
}
</script>

<style scoped>
.container { padding: 20rpx; }
.page-title { font-size: 40rpx; font-weight: 700; margin-bottom: 30rpx; }
.tabs-scroll { white-space: nowrap; margin-bottom: 24rpx; }
.tabs { display: inline-flex; gap: 12rpx; padding: 10rpx 0; }
.tab { display: inline-block; padding: 16rpx 32rpx; border-radius: 50rpx; background: #fff; font-size: 28rpx; color: #6a7a8e; }
.tab.active { background: #4A90D9; color: #fff; font-weight: 600; }

.filter-scroll { white-space: nowrap; margin-bottom: 16rpx; }
.filter-bar { display: flex; gap: 12rpx; margin-bottom: 20rpx; flex-wrap: wrap; align-items: center; }
.filter-tab { padding: 12rpx 28rpx; border-radius: 30rpx; font-size: 26rpx; background: #fff; color: #666; }
.filter-tab.small { padding: 10rpx 22rpx; font-size: 24rpx; }
.filter-tab.active { background: #4A90D9; color: #fff; font-weight: 600; }
.filter-tab.add-tab { background: #52c41a; color: #fff; }

.total-card { background: #fff; border-radius: 20rpx; padding: 28rpx; margin-bottom: 24rpx; }
.total-title { font-size: 32rpx; font-weight: 700; margin-bottom: 20rpx; }
.total-grid { display: flex; flex-wrap: wrap; }
.total-item { width: 20%; text-align: center; padding: 16rpx 0; }
.total-num { display: block; font-size: 38rpx; font-weight: 700; color: #4A90D9; }
.total-label { font-size: 22rpx; color: #8A9AAE; margin-top: 6rpx; }

.card { background: #fff; border-radius: 20rpx; padding: 28rpx; margin-bottom: 20rpx; }
.row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8rpx; }
.name { font-size: 34rpx; font-weight: 500; }
.status { font-size: 28rpx; font-weight: 500; }
.desc { font-size: 26rpx; color: #6a7a8e; margin-top: 6rpx; }
.desc.big { font-size: 30rpx; margin-top: 10rpx; }
.actions { margin-top: 16rpx; display: flex; gap: 12rpx; }

.btn-mini {
  padding: 8rpx 24rpx;
  border-radius: 30rpx;
  font-size: 22rpx;
  border: none;
  color: #fff;
  line-height: 1.4;
}
.btn-pass { background: #52c41a; }
.btn-reject { background: #ff4d4f; }
.btn-warn { background: #faad14; }
.btn-edit { background: #4A90D9; }

.btn-primary { width: 100%; background: #4A90D9; color: #fff; border: none; border-radius: 50rpx; padding: 22rpx; font-size: 30rpx; margin-top: 20rpx; }
.btn-cancel { width: 100%; background: #f0f4f8; color: #666; border: none; border-radius: 50rpx; padding: 22rpx; font-size: 30rpx; margin-top: 16rpx; }
.btn-logout { width: 100%; background: #f0f4f8; color: #4a5a6e; border: none; border-radius: 50rpx; padding: 22rpx; font-size: 30rpx; margin-bottom: 20rpx; }
.btn-delete { width: 100%; background: #fff; color: #ff4d4f; border: 1px solid #ff4d4f; border-radius: 50rpx; padding: 22rpx; font-size: 30rpx; }
.empty { text-align: center; color: #999; padding: 60rpx; }

.news-card { padding: 30rpx; }
.news-title { font-size: 34rpx; font-weight: 600; }
.news-content { display: block; margin-top: 16rpx; font-size: 30rpx; color: #4a5a6e; line-height: 1.6; }
.status-tag { font-size: 24rpx; padding: 6rpx 20rpx; border-radius: 30rpx; background: #e6ffed; color: #52c41a; flex-shrink: 0; }
.status-tag.disabled { background: #fff1f0; color: #ff4d4f; }

.user-card { padding: 28rpx; }
.user-head { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8rpx; }
.user-actions { display: flex; justify-content: flex-start; gap: 16rpx; margin-top: 20rpx; }

.section-title { font-size: 30rpx; font-weight: 600; color: #2d3a4a; margin: 20rpx 0 12rpx; }
.cat-order { padding: 16rpx; border: 1rpx solid #eef2f7; border-radius: 12rpx; margin-bottom: 12rpx; font-size: 26rpx; color: #4a5a6e; }

/* 个人中心 */
.profile-card {
  display: flex; align-items: center;
  background: linear-gradient(135deg, #4A90D9, #3b7bd9);
  border-radius: 24rpx; padding: 40rpx; margin-bottom: 24rpx; color: #fff;
}
.avatar-circle {
  width: 120rpx; height: 120rpx; border-radius: 50%;
  background: rgba(255,255,255,0.25); display: flex; align-items: center;
  justify-content: center; font-size: 52rpx; font-weight: bold; margin-right: 30rpx;
}
.profile-info .name { font-size: 40rpx; font-weight: 700; }
.profile-info .role-tag {
  display: inline-block; background: rgba(255,255,255,0.25);
  padding: 4rpx 18rpx; border-radius: 30rpx; font-size: 24rpx; margin: 10rpx 0;
}
.profile-info .uid { font-size: 24rpx; opacity: 0.9; }
.section-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16rpx; }
.edit-btn { font-size: 26rpx; color: #4A90D9; padding: 6rpx 20rpx; border: 1px solid #4A90D9; border-radius: 30rpx; }
.info-item { display: flex; justify-content: space-between; padding: 16rpx 0; font-size: 28rpx; color: #4a5a6e; border-bottom: 1rpx solid #f0f4f8; }
.info-item:last-child { border-bottom: none; }
.info-item text:first-child { color: #8A9AAE; }
.radio-inline { display: flex; gap: 20rpx; font-size: 26rpx; color: #4a5a6e; }

.modal-mask { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 999; }
.modal-box { width: 680rpx; max-height: 85vh; overflow-y: auto; background: #fff; border-radius: 24rpx; padding: 40rpx; box-sizing: border-box; }
.modal-title { font-size: 34rpx; font-weight: 700; text-align: center; margin-bottom: 30rpx; }
.form-row { display: flex; align-items: center; min-height: 90rpx; border-bottom: 1rpx solid #f0f4f8; padding: 10rpx 0; }
.form-row .label { width: 140rpx; flex-shrink: 0; font-size: 28rpx; color: #8A9AAE; }
.form-row input { flex: 1; height: 90rpx; line-height: 90rpx; font-size: 28rpx; border: none; background: transparent; }
.form-row picker { flex: 1; font-size: 28rpx; }
.textarea-row { align-items: flex-start; padding: 20rpx 0; }
.modal-textarea { flex: 1; min-height: 300rpx; font-size: 28rpx; background: #f7f9fc; border-radius: 12rpx; padding: 20rpx; box-sizing: border-box; border: 1px solid #e6edf4; }
</style>