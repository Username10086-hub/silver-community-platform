<template>
  <view class="page-container">
    <view class="profile-header">
      <view class="avatar-circle">{{ avatarLetter }}</view>
      <view class="info">
        <text class="name">{{ userInfo.realName }}</text>
        <text class="role">老人</text>
        <text class="uid">ID：{{ userInfo.id }}</text>
      </view>
    </view>

    <view class="tabs">
      <view class="tab" :class="{active: currentTab===0}" @click="currentTab=0">发布工单</view>
      <view class="tab" :class="{active: currentTab===1}" @click="currentTab=1">健康管理</view>
      <view class="tab" :class="{active: currentTab===2}" @click="currentTab=2">个人中心</view>
    </view>

    <!-- 发布工单 -->
    <view v-show="currentTab===0">
      <view class="card">
        <view class="form-row">
          <text class="label">服务类型</text>
          <picker mode="selector" :range="types" @change="onTypeChange">
            <view class="picker-value">{{ types[typeIndex] }}</view>
          </picker>
        </view>
        <view class="form-row">
          <text class="label">服务地址</text>
          <input v-model="address" placeholder="请输入服务地址" />
        </view>
        <view class="form-row">
          <text class="label">联系电话</text>
          <input v-model="contactPhone" type="number" placeholder="请输入联系电话" />
        </view>
        <view class="form-row">
          <text class="label">需要人数</text>
          <input v-model="peopleCount" type="number" placeholder="1" />
        </view>
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
          <textarea v-model="remark" placeholder="请描述您的需求..." class="textarea-input"></textarea>
        </view>
        <button class="btn-primary" @click="publish">提交工单（等待审核）</button>
      </view>

      <view class="section-title">我的工单</view>
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

      <view v-for="o in filteredMyOrders" :key="o.id" class="card" @click="goOrderDetail(o.id)">
        <view class="order-line">
          <view style="display:flex;align-items:center;gap:8rpx;">
            <text class="order-title">{{ ['陪护','助老帮扶','社区帮扶'][o.serviceType-1] }}</text>
            <text v-if="o.publisherRole === 4" class="tag delegate-tag">代发布</text>
          </view>
          <text class="status-tag" :class="statusClass(o)">{{ statusText(o) }}</text>
        </view>
        <view class="desc">📍 {{ o.address || '未填' }} · 👥 {{ o.acceptedCount }}/{{ o.peopleCount }}人</view>
        <view class="desc" v-if="o.appointmentTime">⏰ {{ o.appointmentTime }}</view>
        <view class="desc">📝 {{ o.remark || '无' }}</view>
      </view>
      <view v-if="filteredMyOrders.length===0" class="empty-tip">暂无工单</view>
    </view>

    <!-- 健康管理 -->
    <view v-show="currentTab===1">
      <view class="card">
        <view class="form-row"><text class="label">血压</text><input v-model="health.bloodPressure" placeholder="120/80" /></view>
        <view class="form-row"><text class="label">血糖</text><input v-model="health.bloodSugar" placeholder="5.6" /></view>
        <view class="form-row"><text class="label">心率</text><input v-model="health.heartRate" type="number" placeholder="72" /></view>
        <button class="btn-primary" @click="submitHealth">提交健康数据</button>
      </view>
      <view class="card" v-for="h in healthList" :key="h.id">
        <text>血压 {{ h.bloodPressure || '--' }} | 血糖 {{ h.bloodSugar || '--' }} | 心率 {{ h.heartRate || '--' }}</text>
        <text class="time">{{ h.recordTime }}</text>
      </view>
      <view v-if="healthList.length < healthTotal" class="more-btn" @click="loadMoreHealth">查看更多 ></view>
      <view v-if="healthList.length===0" class="empty-tip">暂无健康记录</view>
    </view>

    <!-- 个人中心 -->
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
        <view class="info-item"><text>性别</text><text>{{ genderText(userInfo.gender) }}</text></view>
        <view class="info-item"><text>手机</text><text>{{ userInfo.phone || '未填' }}</text></view>
        <view class="info-item"><text>住址</text><text>{{ userInfo.address || '未填' }}</text></view>
      </view>

      <view class="card">
        <view class="section-head">
          <text class="section-title">📋 养老基础档案</text>
          <text class="edit-btn" @click="openEditArchive">编辑</text>
        </view>
        <view class="info-item"><text>居住状态</text><text>{{ livingMap[archive && archive.livingStatus] || '未填' }}</text></view>
        <view class="info-item"><text>行动能力</text><text>{{ mobilityMap[archive && archive.mobility] || '未填' }}</text></view>
        <view class="info-item"><text>慢性病史</text><text>{{ (archive && archive.chronicDisease) || '无' }}</text></view>
        <view class="info-item"><text>饮食禁忌</text><text>{{ (archive && archive.dietaryRestriction) || '无' }}</text></view>
        <view class="info-item"><text>紧急联系人</text><text>{{ (archive && archive.emergencyContact) || '未填' }}</text></view>
      </view>

      <view class="card">
        <view class="section-head">
          <text class="section-title">👨‍👧 我的监护人</text>
          <text class="edit-btn" @click="openBindModal">+ 添加</text>
        </view>
        <view v-if="guardians.length === 0" class="no-guardian">暂未绑定子女</view>
        <view v-for="g in guardians" :key="g.id" class="guardian-item">
          <view>
            <view class="g-name">{{ g.realName }}</view>
            <view class="g-desc">ID：{{ g.id }} | {{ g.phone }}</view>
          </view>
          <text class="remove-btn" @click="unbindGuardian(g.id)">解除</text>
        </view>
      </view>

      <button class="btn-logout" @click="logout">退出登录</button>
      <button class="btn-delete" @click="deleteAccount">注销账号</button>

      <view class="sos-container">
        <button class="btn-sos" @click="sendSOS">🆘 SOS 一键求助</button>
      </view>
    </view>

    <!-- 编辑基本信息 -->
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
        <view class="form-row"><text class="label">住址</text><input v-model="editProfile.address" /></view>
        <button class="btn-primary" @click="saveProfile">保存</button>
        <button class="btn-cancel" @click="showProfileModal = false">取消</button>
      </view>
    </view>

    <!-- 编辑档案 -->
    <view v-if="showArchiveModal" class="modal-mask" @click="showArchiveModal = false">
      <view class="modal-box" @click.stop>
        <view class="modal-title">编辑养老档案</view>
        <view class="form-row">
          <text class="label">居住状态</text>
          <radio-group @change="onEditLivingChange" class="radio-inline">
            <label><radio value="1" :checked="editArchive.livingStatus === 1" color="#4A90D9" />独居</label>
            <label><radio value="2" :checked="editArchive.livingStatus === 2" color="#4A90D9" />与子女</label>
            <label><radio value="3" :checked="editArchive.livingStatus === 3" color="#4A90D9" />夫妻</label>
          </radio-group>
        </view>
        <view class="form-row">
          <text class="label">行动能力</text>
          <radio-group @change="onEditMobilityChange" class="radio-inline">
            <label><radio value="1" :checked="editArchive.mobility === 1" color="#4A90D9" />自理</label>
            <label><radio value="2" :checked="editArchive.mobility === 2" color="#4A90D9" />半自理</label>
            <label><radio value="3" :checked="editArchive.mobility === 3" color="#4A90D9" />失能</label>
          </radio-group>
        </view>
        <view class="form-row"><text class="label">慢性病史</text><input v-model="editArchive.chronicDisease" /></view>
        <view class="form-row"><text class="label">饮食禁忌</text><input v-model="editArchive.dietaryRestriction" /></view>
        <view class="form-row"><text class="label">紧急联系人</text><input v-model="editArchive.emergencyContact" /></view>
        <button class="btn-primary" @click="saveArchive">保存</button>
        <button class="btn-cancel" @click="showArchiveModal = false">取消</button>
      </view>
    </view>

    <!-- 添加监护人 -->
    <view v-if="showBindModal" class="modal-mask" @click="showBindModal = false">
      <view class="modal-box" @click.stop>
        <view class="modal-title">添加监护人</view>
        <view class="form-row"><text class="label">子女姓名</text><input v-model="bindForm.guardianName" placeholder="请输入真实姓名" /></view>
        <view class="form-row"><text class="label">子女ID</text><input v-model="bindForm.guardianId" type="number" placeholder="请输入用户ID" /></view>
        <button class="btn-primary" @click="bindGuardian">确认绑定</button>
        <button class="btn-cancel" @click="showBindModal = false">取消</button>
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
      userInfo: {}, currentTab: 0,
      remark: '', types: ['陪护', '助老帮扶', '社区帮扶'], typeIndex: 0,
      address: '', contactPhone: '', peopleCount: 1,
      appointmentDate: '', appointmentTime: '',
      todayStr: new Date().toISOString().substring(0, 10),
      myOrders: [],
      orderStatusTab: 'all',
      health: { bloodPressure: '', bloodSugar: '', heartRate: '' }, healthList: [],
      healthLimit: 3, healthTotal: 0,
      guardians: [], archive: null,
      livingMap: { 1: '独居', 2: '与子女同住', 3: '夫妻同住' },
      mobilityMap: { 1: '自理', 2: '半自理', 3: '失能' },
      showProfileModal: false, showArchiveModal: false, showBindModal: false,
      editProfile: {}, editArchive: {}, bindForm: { guardianName: '', guardianId: '' }
    }
  },
  computed: {
    avatarLetter() {
      const name = this.userInfo && this.userInfo.realName
      return name ? name.substring(0, 1) : '?'
    },
    filteredMyOrders() {
      const t = this.orderStatusTab
      if (t === 'all') return this.myOrders
      if (t === 'audit') return this.myOrders.filter(o => o.auditStatus === 0 || o.auditStatus === 3)
      if (t === 'waitTeam') return this.myOrders.filter(o => o.auditStatus === 1 && o.status === 0 && o.acceptedCount < o.peopleCount)
      if (t === 'ongoing') return this.myOrders.filter(o => o.auditStatus === 1 && o.status === 1)
      if (t === 'verify') return this.myOrders.filter(o => o.auditStatus === 1 && o.status === 4)
      if (t === 'done') return this.myOrders.filter(o => o.auditStatus === 1 && o.status === 2)
      if (t === 'cancel') return this.myOrders.filter(o => o.status === 3)
      if (t === 'reject') return this.myOrders.filter(o => o.auditStatus === 2)
      if (t === 'revoke') return this.myOrders.filter(o => o.status === 5)
      return this.myOrders
    }
  },
  onShow() {
    this.userInfo = getUser() || {}
    if (this.userInfo.id) {
      this.loadOrders(); this.loadHealth(); this.loadGuardians(); this.loadArchive()
    }
  },
  methods: {
    genderText(g) {
      if (g === 1) return '男'
      if (g === 2) return '女'
      return '未填'
    },
    onTypeChange(e) { this.typeIndex = e.detail.value },
    onDateChange(e) { this.appointmentDate = e.detail.value },
    onTimeChange(e) { this.appointmentTime = e.detail.value },
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

    async publish() {
      if (!this.remark.trim()) return uni.showToast({ title: '请填写需求', icon: 'none' })
      if (!this.appointmentDate || !this.appointmentTime) return uni.showToast({ title: '请选择预约日期和时间', icon: 'none' })
      const apptTime = this.appointmentDate + ' ' + this.appointmentTime + ':00'
      try {
        await request({
          url: '/order/publish',
          method: 'POST',
          data: {
            elderId: this.userInfo.id,
            publisherId: this.userInfo.id,
            publisherRole: 1,
            serviceType: Number(this.typeIndex) + 1,
            address: this.address,
            contactPhone: this.contactPhone,
            peopleCount: Number(this.peopleCount) || 1,
            appointmentTime: apptTime,
            remark: this.remark
          }
        })
        uni.showToast({ title: '已提交，等待审核' })
        this.remark = ''; this.address = ''; this.contactPhone = ''; this.peopleCount = 1
        this.appointmentDate = ''; this.appointmentTime = ''
        this.loadOrders()
      } catch (e) {
        uni.showToast({ title: e.msg || '提交失败', icon: 'none' })
      }
    },
    async loadOrders() {
      try {
        const list = await request({ url: `/order/list?elderId=${this.userInfo.id}` })
        this.myOrders = list.map(item => item.order)
      } catch (e) { console.log(e) }
    },
    goOrderDetail(id) { uni.navigateTo({ url: `/pages/order/order-detail?id=${id}` }) },

    async submitHealth() {
      try {
        await request({ url: '/user/health/add', method: 'POST', data: { elderId: this.userInfo.id, ...this.health } })
        uni.showToast({ title: '上报成功' })
        this.health = { bloodPressure: '', bloodSugar: '', heartRate: '' }
        this.loadHealth()
      } catch (e) { uni.showToast({ title: '提交失败', icon: 'none' }) }
    },
    async loadHealth() {
      try {
        const list = await request({ url: `/user/health/list?elderId=${this.userInfo.id}&limit=${this.healthLimit}` })
        this.healthList = list || []
        const all = await request({ url: `/user/health/list?elderId=${this.userInfo.id}` })
        this.healthTotal = (all || []).length
      } catch (e) {}
    },
    loadMoreHealth() {
      this.healthLimit += 3
      this.loadHealth()
    },
    async loadGuardians() {
      try { this.guardians = await request({ url: `/user/getGuardiansByElder?elderId=${this.userInfo.id}` }) } catch (e) {}
    },
    async loadArchive() {
      try { this.archive = await request({ url: `/user/elderInfo/${this.userInfo.id}` }) } catch (e) { this.archive = null }
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

    openEditArchive() {
      this.editArchive = this.archive ? { ...this.archive } : { elderId: this.userInfo.id }
      this.showArchiveModal = true
    },
    onEditLivingChange(e) { this.editArchive.livingStatus = Number(e.detail.value) },
    onEditMobilityChange(e) { this.editArchive.mobility = Number(e.detail.value) },
    async saveArchive() {
      this.editArchive.elderId = this.userInfo.id
      await request({ url: '/user/updateElderInfo', method: 'PUT', data: this.editArchive, header: { 'Content-Type': 'application/json' } })
      uni.showToast({ title: '已保存' })
      this.showArchiveModal = false
      this.loadArchive()
    },

    openBindModal() { this.bindForm = { guardianName: '', guardianId: '' }; this.showBindModal = true },
    async bindGuardian() {
      const { guardianName, guardianId } = this.bindForm
      if (!guardianName || !guardianId) return uni.showToast({ title: '请填写完整', icon: 'none' })
      try {
        await request({
          url: '/user/bindGuardian',
          method: 'POST',
          data: { elderId: this.userInfo.id, guardianId: Number(guardianId), guardianName }
        })
        uni.showToast({ title: '绑定成功' })
        this.showBindModal = false
        this.loadGuardians()
      } catch (e) { uni.showToast({ title: '绑定失败', icon: 'none' }) }
    },
    unbindGuardian(guardianId) {
      uni.showModal({
        title: '确认解除？',
        success: async (res) => {
          if (!res.confirm) return
          await request({ url: `/user/unbind?guardianId=${guardianId}&elderId=${this.userInfo.id}`, method: 'DELETE' })
          uni.showToast({ title: '已解除' })
          this.loadGuardians()
        }
      })
    },

    async sendSOS() {
      if (this.guardians.length === 0) return uni.showToast({ title: '暂无绑定子女', icon: 'none' })
      uni.showModal({
        title: '一键求助',
        content: `将向 ${this.guardians.length} 位监护人发送求助`,
        success: async (res) => {
          if (!res.confirm) return
          try {
            for (const g of this.guardians) {
              await request({ url: '/order/alert/send', method: 'POST', data: { elderId: this.userInfo.id, guardianId: g.id } })
            }
            uni.showToast({ title: '求助已发送！', icon: 'success' })
          } catch (e) { uni.showToast({ title: '发送失败', icon: 'none' }) }
        }
      })
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
.page-container { padding: 20rpx; }
.profile-header {
  display: flex; align-items: center; padding: 30rpx;
  background: linear-gradient(135deg, #4f9ef7, #3b7bd9);
  border-radius: 24rpx; margin-bottom: 30rpx; color: #fff;
}
.avatar-circle {
  width: 120rpx; height: 120rpx; border-radius: 50%;
  background: rgba(255,255,255,0.25); display: flex; align-items: center;
  justify-content: center; font-size: 52rpx; font-weight: bold; margin-right: 30rpx;
}
.profile-header .info .name { font-size: 40rpx; font-weight: 700; }
.profile-header .info .role { font-size: 26rpx; opacity: 0.9; margin-left: 16rpx; }
.profile-header .info .uid { display: block; font-size: 24rpx; opacity: 0.85; margin-top: 8rpx; }
.tabs { display: flex; background: #fff; border-radius: 50rpx; padding: 6rpx; margin-bottom: 30rpx; }
.tab { flex: 1; text-align: center; padding: 20rpx 0; border-radius: 50rpx; font-size: 28rpx; color: #6a7a8e; }
.tab.active { background: #4f9ef7; color: #fff; font-weight: 600; }
.order-tabs-scroll { white-space: nowrap; margin-bottom: 20rpx; }
.order-tabs { display: inline-flex; gap: 12rpx; padding: 6rpx 0; }
.order-tab { display: inline-block; padding: 12rpx 26rpx; border-radius: 30rpx; background: #fff; font-size: 24rpx; color: #6a7a8e; }
.order-tab.active { background: #4A90D9; color: #fff; font-weight: 600; }
.card { background: #fff; border-radius: 20rpx; padding: 24rpx; margin-bottom: 24rpx; }
.section-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16rpx; }
.section-title { font-size: 30rpx; font-weight: 600; color: #2d3a4a; margin-bottom: 16rpx; }
.edit-btn { font-size: 26rpx; color: #4A90D9; padding: 6rpx 20rpx; border: 1px solid #4A90D9; border-radius: 30rpx; }
.info-item { display: flex; justify-content: space-between; padding: 16rpx 0; font-size: 28rpx; color: #4a5a6e; border-bottom: 1rpx solid #f0f4f8; }
.info-item:last-child { border-bottom: none; }
.info-item text:first-child { color: #8A9AAE; }
.no-guardian { color: #999; font-size: 26rpx; padding: 20rpx 0; text-align: center; }
.guardian-item { display: flex; justify-content: space-between; align-items: center; padding: 20rpx; border: 1rpx solid #e6edf4; border-radius: 12rpx; margin-bottom: 16rpx; }
.g-name { font-size: 30rpx; font-weight: 500; }
.g-desc { font-size: 24rpx; color: #8A9AAE; margin-top: 6rpx; }
.remove-btn { color: #ff4d4f; font-size: 24rpx; padding: 8rpx 20rpx; border: 1px solid #ff4d4f; border-radius: 30rpx; }
.order-line { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12rpx; }
.order-title { font-size: 30rpx; font-weight: 500; }
.tag { font-size: 20rpx; padding: 2rpx 12rpx; border-radius: 20rpx; }
.delegate-tag { background: #FFF3E0; color: #d48806; }
.status-tag { padding: 6rpx 18rpx; border-radius: 30rpx; font-size: 24rpx; color: #fff; }
.status-tag.pending { background: #faad14; }
.status-tag.ongoing { background: #4f9ef7; }
.status-tag.done { background: #52c41a; }
.status-tag.cancelled { background: #999; }
.status-tag.verify { background: #faad14; }
.status-tag.audit { background: #4A90D9; }
.status-tag.reject { background: #ff4d4f; }
.desc { font-size: 26rpx; color: #6a7a8e; margin-top: 6rpx; }
.form-row { display: flex; align-items: center; min-height: 90rpx; border-bottom: 1rpx solid #f0f4f8; padding: 10rpx 0; }
.form-row:last-child { border-bottom: none; }
.form-row .label { width: 180rpx; flex-shrink: 0; font-size: 28rpx; color: #8A9AAE; }
.form-row input { flex: 1; height: 90rpx; line-height: 90rpx; font-size: 28rpx; color: #1a2332; background: transparent; border: none; }
.form-row picker { flex: 1; }
.picker-value { font-size: 28rpx; color: #1a2332; height: 90rpx; line-height: 90rpx; }
.textarea-row { align-items: flex-start; padding-top: 20rpx; }
.textarea-input { flex: 1; height: 160rpx; font-size: 28rpx; background: #f7f9fc; border-radius: 12rpx; padding: 16rpx; box-sizing: border-box; }
.radio-inline { display: flex; gap: 20rpx; font-size: 26rpx; color: #4a5a6e; }
.btn-primary { width: 100%; background: #4A90D9; color: #fff; border: none; border-radius: 50rpx; padding: 22rpx; font-size: 30rpx; margin-top: 20rpx; }
.btn-cancel { width: 100%; background: #f0f4f8; color: #666; border: none; border-radius: 50rpx; padding: 22rpx; font-size: 30rpx; margin-top: 16rpx; }
.btn-logout { width: 100%; background: #f0f4f8; color: #4a5a6e; border: none; border-radius: 50rpx; padding: 22rpx; font-size: 30rpx; margin-bottom: 20rpx; }
.btn-delete { width: 100%; background: #fff; color: #ff4d4f; border: 1px solid #ff4d4f; border-radius: 50rpx; padding: 22rpx; font-size: 30rpx; }
.sos-container { margin-top: 40rpx; }
.btn-sos { width: 100%; height: 100rpx; background: linear-gradient(135deg, #ff4d4f, #d9363e); color: #fff; border-radius: 50rpx; font-size: 40rpx; font-weight: bold; box-shadow: 0 10rpx 30rpx rgba(255, 77, 79, 0.4); border: none; }
.modal-mask { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 999; }
.modal-box { width: 640rpx; max-height: 80vh; overflow-y: auto; background: #fff; border-radius: 24rpx; padding: 40rpx; box-sizing: border-box; }
.modal-title { font-size: 34rpx; font-weight: 700; text-align: center; margin-bottom: 30rpx; }
.time { display: block; font-size: 22rpx; color: #999; margin-top: 8rpx; }
.empty-tip { text-align: center; color: #999; margin: 40rpx 0; font-size: 26rpx; }
.more-btn { text-align: center; color: #4A90D9; font-size: 28rpx; padding: 20rpx; }
</style>